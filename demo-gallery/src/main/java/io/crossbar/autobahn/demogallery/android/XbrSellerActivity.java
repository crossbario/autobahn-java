package io.crossbar.autobahn.demogallery.android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.basgeekball.awesomevalidation.AwesomeValidation;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import io.crossbar.autobahn.demogallery.R;
import io.crossbar.autobahn.utils.AuthUtil;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.PublishOptions;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import xbr.network.SimpleSeller;
import xbr.network.Util;
import xbr.network.eip712.MarketMemberLogin;

import static com.basgeekball.awesomevalidation.ValidationStyle.BASIC;

public class XbrSellerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String DELEGATE_ETH_KEY = "d99b5b29e6da2528bf458b26237a6cf8655a3e3276c1cdc0de1f98cefee81c01";
    private static final String MEMBER_ETH_KEY = "2eac15546def97adc6d69ca6e28eec831189baa2533e7910755d15403a0749e8";
    private static final String CS_KEY = "0db085a389c1216ad62b88b408e1d830abca9c9f9dad67eb8c8f8734fe7575eb";

    private SimpleSeller mSeller;
    private String mURI;
    private String mRealm;
    private Session mSession;

    private AwesomeValidation mValidator;
    private TextView mStatus;
    private EditText mFieldURI;
    private EditText mFieldRealm;
    private Button mButtonSell;
    private boolean mWasConnected;

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xbr_seller);
        mPrefs = getSharedPreferences("seller", 0);

        mRealm = mPrefs.getString("xbr_realm", "realm1");
        mURI = mPrefs.getString("xbr_uri", "ws://localhost:8070/ws");

        mFieldURI = findViewById(R.id.textFieldURI);
        mFieldRealm = findViewById(R.id.textFieldRealm);

        mFieldURI.setText(mURI);
        mFieldRealm.setText(mRealm);

        mStatus = findViewById(R.id.textViewStatus);
        mStatus.setText("Disconnected.");

        mButtonSell = findViewById(R.id.buttonSell);

        mValidator = new AwesomeValidation(BASIC);
        Pattern uriPattern = Pattern.compile("^wss?:\\/\\/\\w+(\\.\\w+)*(:[0-9]+)?\\/?(\\/[.\\w]*)*$");
        Pattern realmPattern = Pattern.compile("^(?!\\s*$).+");
        mValidator.addValidation(mFieldURI, uriPattern, "Invalid websocket URI");
        mValidator.addValidation(mFieldRealm, realmPattern, "Must provide valid router realm");

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonSell) {
            if (mWasConnected) {
                mSeller.stop().thenAccept(aVoid -> mSession.leave());
            } else if (mValidator.validate()) {
                mURI = mFieldURI.getText().toString();
                mRealm = mFieldRealm.getText().toString();
                sell();
            }
        }
    }

    public void sell() {
        Session session = new Session();
        session.addOnJoinListener(this::onJoin);
        session.addOnLeaveListener(this::onLeave);
        session.addOnDisconnectListener(this::onDisconnected);

        ECKeyPair keyPair = ECKeyPair.create(Numeric.hexStringToByteArray(MEMBER_ETH_KEY));
        String addressHex = Credentials.create(keyPair).getAddress();
        byte[] addressRaw = Numeric.hexStringToByteArray(addressHex);

        String pubkeyHex = CryptosignAuth.getPublicKey(AuthUtil.toBinary(CS_KEY));

        Map<String, Object> extras = new HashMap<>();
        extras.put("wallet_address", addressRaw);
        extras.put("pubkey", pubkeyHex);

        MarketMemberLogin.sign(
                keyPair, addressHex, pubkeyHex
        ).thenCompose(signature -> {
            extras.put("signature", signature);

            System.out.println(mURI + mRealm);
            CryptosignAuth auth = new CryptosignAuth("public", CS_KEY, extras);
            Client client = new Client(session, mURI, mRealm, auth);

            return client.connect();
        }).whenComplete((exitInfo, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                System.out.println("Exit...");
            }
        });
    }

    private void onJoin(Session session, SessionDetails details) {
        System.out.println("Joined...");
        mSession = session;

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("xbr_uri", mURI);
        editor.putString("xbr_realm", mRealm);
        editor.apply();

        mStatus.setText("Connected to server");
        mButtonSell.setText("Stop Selling");
        mWasConnected = true;
        AtomicReference<BigInteger> balance = new AtomicReference<>();

        byte[] apiID = new byte[16];
        String topic = "xbr.myapp.example";

        session.call(
                "xbr.marketmaker.get_config", Map.class
        ).thenCompose(config -> {
            String marketMaker = (String) config.get("marketmaker");
            mSeller = new SimpleSeller(marketMaker, DELEGATE_ETH_KEY);
            BigInteger price = Util.toXBR(1);
            int intervalSeconds = 10;
            mSeller.add(apiID, topic, price, intervalSeconds);
            return mSeller.start(session);
        }).thenCompose(bigInteger -> {
            balance.set(bigInteger);
            mStatus.setText("Seller is ready now");
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", "crossbario");
            payload.put("country", "DE");
            payload.put("level", "Crossbar is super cool!");
            return mSeller.wrap(apiID, topic, payload);
        }).thenCompose(enc -> {
            return session.publish(topic, new PublishOptions(true, true), enc.get("id"),
                    enc.get("serializer"), enc.get("ciphertext"));
        }).thenAccept(publication -> {
            System.out.println("BALANCE IS " + Util.toInt(balance.get()) + " XBR");
        }).exceptionally(throwable -> {
            mStatus.setText("Something went wrong");
            return null;
        });
    }

    private void onLeave(Session session, CloseDetails details) {
        mSession = null;
    }

    private void onDisconnected(Session session, boolean wasClean) {
        if (!mWasConnected) {
            mStatus.setText("Unable to connect.");
        } else {
            mStatus.setText("Disconnected.");
        }
        mButtonSell.setText("Start selling");
    }
}

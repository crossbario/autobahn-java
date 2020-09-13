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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.crossbar.autobahn.demogallery.R;
import io.crossbar.autobahn.utils.AuthUtil;
import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.auth.CryptosignAuth;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import xbr.network.SimpleBuyer;
import xbr.network.Util;
import xbr.network.eip712.MarketMemberLogin;

import static com.basgeekball.awesomevalidation.ValidationStyle.BASIC;

public class XbrBuyerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String DELEGATE_ETH_KEY = "77c5495fbb039eed474fc940f29955ed0531693cc9212911efd35dff0373153f";
    private static final String MEMBER_ETH_KEY = "2e114163041d2fb8d45f9251db259a68ee6bdbfd6d10fe1ae87c5c4bcd6ba491";
    private static final String CS_KEY = "dc88492fcff5470fcc76f21fa03f1752e0738e1e5cd56cd61fc280bac4d4c4d9";

    private Session mSession;
    private BigInteger mRemainingBalance;
    private SimpleBuyer mBuyer;
    private String mURI;
    private String mRealm;

    private AwesomeValidation mValidator;
    private TextView mStatus;
    private EditText mFieldURI;
    private EditText mFieldRealm;
    private Button mButtonBuy;
    private boolean mWasConnected;

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xbr_buyer);
        mPrefs = getSharedPreferences("buyer", 0);

        mRealm = mPrefs.getString("xbr_realm", "realm1");
        mURI = mPrefs.getString("xbr_uri", "ws://localhost:8070/ws");

        mFieldURI = findViewById(R.id.textFieldURI);
        mFieldRealm = findViewById(R.id.textFieldRealm);

        mFieldURI.setText(mURI);
        mFieldRealm.setText(mRealm);

        mStatus = findViewById(R.id.textViewStatus);
        mStatus.setText("Disconnected.");

        mButtonBuy = findViewById(R.id.buttonBuy);

        mValidator = new AwesomeValidation(BASIC);
        Pattern uriPattern = Pattern.compile("^wss?:\\/\\/\\w+(\\.\\w+)*(:[0-9]+)?\\/?(\\/[.\\w]*)*$");
        Pattern realmPattern = Pattern.compile("^(?!\\s*$).+");
        mValidator.addValidation(mFieldURI, uriPattern, "Invalid websocket URI");
        mValidator.addValidation(mFieldRealm, realmPattern, "Must provide valid router realm");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonBuy) {
            if (mWasConnected) {
                mBuyer.stop().thenAccept(aVoid -> mSession.leave());
            } else if (mValidator.validate()) {
                mURI = mFieldURI.getText().toString();
                mRealm = mFieldRealm.getText().toString();
                buy();
            }
        }
    }

    public void buy() {
        mSession = new Session();
        mSession.addOnJoinListener(this::onJoin);
        mSession.addOnLeaveListener(this::onLeave);
        mSession.addOnDisconnectListener(this::onDisconnected);

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

            CryptosignAuth auth = new CryptosignAuth("public", CS_KEY, extras);
            Client client = new Client(mSession, mURI, mRealm, auth);

            return client.connect();
        }).whenComplete((exitInfo, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                System.out.println("Exit...");
            }
        });
    }

    private void onDisconnected(Session session, boolean wasClean) {
        if (!mWasConnected) {
            mStatus.setText("Unable to connect.");
        } else {
            mStatus.setText("Disconnected.");
        }
        mWasConnected = false;
        mButtonBuy.setText("Start buying");
    }

    private void onLeave(Session session, CloseDetails closeDetails) {
    }

    private void onJoin(Session session, SessionDetails details) {
        System.out.println("Joined...");
        mWasConnected = true;

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("xbr_uri", mURI);
        editor.putString("xbr_realm", mRealm);
        editor.apply();

        session.call(
                "xbr.marketmaker.get_config", Map.class
        ).thenCompose(config -> {
            String marketMaker = (String) config.get("marketmaker");
            mBuyer = new SimpleBuyer(marketMaker, DELEGATE_ETH_KEY, Util.toXBR(50));
            return mBuyer.start(session, details.authid);
        }).thenCompose(balance -> {
            mRemainingBalance = balance;
            return session.subscribe("xbr.myapp.example", this::actuallyBuy);
        }).thenAccept(subscription -> {
            System.out.println("We are ready to buy.");
            mButtonBuy.setText("Stop buying");
            mStatus.setText("Ready");
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    private void actuallyBuy(List<Object> objects) {
        byte[] keyID = (byte[]) objects.get(0);
        String serializer = (String) objects.get(1);
        byte[] ciphertext = (byte[]) objects.get(2);
        mBuyer.unwrap(keyID, serializer, ciphertext).whenComplete((s, throwable) -> {
            System.out.println(s);
        });
    }
}

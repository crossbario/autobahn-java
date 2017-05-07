package de.tavendo.autobahn.demogallery;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button buttonWebSocketGallery = (Button) findViewById(R.id.button_websocket_gallery);
        Button buttonWAMPGallery = (Button) findViewById(R.id.button_wamp_gallery);
        buttonWebSocketGallery.setOnClickListener(this);
        buttonWAMPGallery.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_websocket_gallery:
                startActivity(new Intent(getApplicationContext(), EchoClientActivity.class));
                break;
            case R.id.button_wamp_gallery:
                // Need to open the WAMP demo gallery browser.
                break;
        }
    }
}

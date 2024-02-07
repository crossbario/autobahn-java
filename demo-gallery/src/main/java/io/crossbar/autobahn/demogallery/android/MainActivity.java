///////////////////////////////////////////////////////////////////////////////
//
//   AutobahnJava - http://crossbar.io/autobahn
//
//   Copyright (c) Crossbar.io Technologies GmbH and contributors
//
//   Licensed under the MIT License.
//   http://www.opensource.org/licenses/mit-license.php
//
///////////////////////////////////////////////////////////////////////////////

package io.crossbar.autobahn.demogallery.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import io.crossbar.autobahn.demogallery.R;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_test_suite_client) {
            startActivity(new Intent(getApplicationContext(), TestSuiteClientActivity.class));
        } else if (id == R.id.button_websocket_echo_client) {
            startActivity(new Intent(getApplicationContext(), EchoClientActivity.class));
        } else if (id == R.id.buttonXBRSeller) {
            startActivity(new Intent(getApplicationContext(), XbrSellerActivity.class));
        } else if (id == R.id.buttonXBRBuyer) {
            startActivity(new Intent(getApplicationContext(), XbrBuyerActivity.class));
        }
    }
}

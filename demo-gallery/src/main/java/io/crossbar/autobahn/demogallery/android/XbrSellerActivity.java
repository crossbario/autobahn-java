package io.crossbar.autobahn.demogallery.android;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.crossbar.autobahn.demogallery.R;
import io.crossbar.autobahn.demogallery.xbr.Seller;

public class XbrSellerActivity extends AppCompatActivity implements View.OnClickListener {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xbr_seller);
        Button buttonSell = findViewById(R.id.buttonSell);
        buttonSell.setEnabled(true);
    }

    @Override
    public void onClick(View v) {
        Seller seller = new Seller();
        seller.sell();
    }
}

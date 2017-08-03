package com.liuyuan.wifiserver;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUi();
    }

    private void initUi() {
        findViewById(R.id.btn_server).setOnClickListener(this);
        findViewById(R.id.btn_client).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_server:
                Intent intent = new Intent();
                intent.setClass(this, ServerMainActivity.class);
                this.startActivity(intent);
                break;
            case R.id.btn_client:
                Intent i = new Intent();
                i.setClass(this, ClientMainActivity.class);
                this.startActivity(i);
                break;
        }
    }
}

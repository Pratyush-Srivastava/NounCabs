package com.myhexaville.login;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.myhexaville.login.login.MainActivity;

public class SplashActivity extends AppCompatActivity {
    private static int splash_time=3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent homeintent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(homeintent);
                finish();
            }
        },splash_time);
    }
}

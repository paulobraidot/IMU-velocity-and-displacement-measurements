package com.example.wojcieth.program;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    public void btnStandalone(View v){
        Intent mMainActivity  = new Intent(this, MainActivityStandalone.class);
        startActivity(mMainActivity);
    }

    public void btnDual(View v){
        Intent mMainActivity  = new Intent(this, MainActivity.class);
        startActivity(mMainActivity);
    }
}
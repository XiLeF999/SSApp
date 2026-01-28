package com.sansan.apps;

import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.hardware.radio.RadioManager;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        RadioManager r = (RadioManager) getSystemService(Context.RADIO_SERVICE);
        Log.e("ww","AAAAAA:" + r);


    }
}
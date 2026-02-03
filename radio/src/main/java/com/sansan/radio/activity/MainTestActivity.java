package com.sansan.radio.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;

import com.sansan.radio.R;
import com.sansan.radio.fm.FmService;
import com.sansan.radio.fm.FmUtils;

public class MainTestActivity extends FragmentActivity {

    private int mCurrentStation = FmUtils.DEFAULT_STATION;

    private Button bt;

    private FmService mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_main);

        bindService(new Intent(MainTestActivity.this, FmService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

        bt = findViewById(R.id.bt);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {




                mService.powerUpAsync(93.5f);





            }
        });
    }



    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((FmService.ServiceBinder) service).getService();

            mCurrentStation = FmUtils.DEFAULT_STATION_50KHZ;
            mService.initService(mCurrentStation);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {

        }
    };



}

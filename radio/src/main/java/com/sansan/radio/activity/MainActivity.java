package com.sansan.radio.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.fragment.app.FragmentActivity;

import com.sansan.radio.R;
import com.sansan.radio.bean.StationInfo;
import com.sansan.radio.fm.FmListener;
import com.sansan.radio.fm.FmService;
import com.sansan.radio.fm.FmStation;
import com.sansan.radio.fm.FmUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

    private int mCurrentStation = FmUtils.DEFAULT_STATION;

    private MainHandler mHandler;

    private ImageView ivPlay;
    private ImageView ivSearch;

    private FmService mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MainHandler(getMainLooper(), this);

        bindService(new Intent(MainActivity.this, FmService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

        ivPlay = findViewById(R.id.iv_play);
        ivPlay.setOnClickListener(onClickListener);
        ivSearch = findViewById(R.id.iv_search);
        ivSearch.setOnClickListener(onClickListener);

    }



    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((FmService.ServiceBinder) service).getService();

            mCurrentStation = FmUtils.DEFAULT_STATION_50KHZ;
            mService.initService(mCurrentStation);

            mService.registerFmRadioListener(mFmRadioListener);


        }

        @Override
        public void onServiceDisconnected(ComponentName className) {

        }
    };




    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            switch (v.getId()){

                case R.id.iv_play:
                    mService.powerUpAsync(93.5f);
                    break;

                case R.id.iv_search:
                    mService.startScanAsync();
                    break;

            }



        }
    };









    private FmListener mFmRadioListener = new FmListener() {
        @Override
        public void onCallBack(Bundle bundle) {
            int flag = bundle.getInt(FmListener.CALLBACK_FLAG);
            if (flag == FmListener.MSGID_FM_EXIT) {
                mHandler.removeCallbacksAndMessages(null);
            }

            // remove tag message first, avoid too many same messages in queue.
            Message msg = mHandler.obtainMessage(flag);
            msg.setData(bundle);
            mHandler.removeMessages(flag);
            mHandler.sendMessage(msg);
        }
    };




    public class MainHandler extends Handler {
        WeakReference<MainActivity> mMainActivity;

        public MainHandler(Looper looper, MainActivity mainActivity) {
            super(looper);
            mMainActivity = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle;
            switch (msg.what) {
                case FmListener.MSGID_SCAN_FINISHED:

                    bundle = msg.getData();
                    // cancel scan happen
                    boolean isScan = bundle.getBoolean(FmListener.KEY_IS_SCAN);
                    int searchedNum = bundle.getInt(FmListener.KEY_STATION_NUM);

                    Log.e("ww","FM电台扫描完成:" + isScan + ";" + searchedNum);

                    Log.e("ww","FM电台扫描完成111:" + getData().size());

                    break;
                default:
                    break;
            }
        }
    }



    private List<StationInfo> getData() {
        List<StationInfo> stationInfoList = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = getContentResolver().query(
                    FmStation.Station.CONTENT_URI,
                    FmStation.COLUMNS,
                    null,
                    null,
                    FmStation.Station.FREQUENCY
            );

            if (cursor == null) {
                return stationInfoList;
            }

            // 预先获取列索引
            int freqIndex = cursor.getColumnIndex(FmStation.Station.FREQUENCY);
            int nameIndex = cursor.getColumnIndex(FmStation.Station.STATION_NAME);
            int rdsIndex = cursor.getColumnIndex(FmStation.Station.RADIO_TEXT);
            int favoriteIndex = cursor.getColumnIndex(FmStation.Station.IS_FAVORITE);

            // 确保索引有效（防御性编程）
            if (freqIndex == -1 || nameIndex == -1 || favoriteIndex == -1) {
                Log.e("ww", "缺少必要的列索引");
                return stationInfoList;
            }

            while (cursor.moveToNext()) {
                int frequency = cursor.getInt(freqIndex);
                String name = cursor.getString(nameIndex);

                // 处理可能为空的字段
                String rds = "";
                if (rdsIndex != -1 && !cursor.isNull(rdsIndex)) {
                    rds = cursor.getString(rdsIndex);
                }

                int isFavorite = cursor.getInt(favoriteIndex);

                StationInfo stationInfo = new StationInfo(frequency, name, rds, isFavorite);
                stationInfoList.add(stationInfo);
            }

        } catch (Exception e) {

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return stationInfoList;
    }



}

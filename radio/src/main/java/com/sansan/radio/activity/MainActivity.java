package com.sansan.radio.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.sansan.radio.R;
import com.sansan.radio.bean.StationInfo;
import com.sansan.radio.fm.FmListener;
import com.sansan.radio.fm.FmService;
import com.sansan.radio.fm.FmStation;
import com.sansan.radio.fm.FmUtils;
import com.sansan.radio.fragment.FavListFragment;
import com.sansan.radio.fragment.FmListFragment;
import com.sansan.radio.widget.RadioScaleView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

    private int mCurrentStation = FmUtils.DEFAULT_STATION;

    private MainActivityHandler mHandler;

    private float currentFrequency = 87.5f;
    private boolean animGoOrBack = true;

    private FmListFragment fmListFragment;
    private FavListFragment favListFragment;

    private RadioScaleView mRadioScaleView;
    private TextView tvFrequency;
    private ImageView ivPlay;
    private ImageView ivSearch;
    private TextView tvFmType;
    private TextView tvFavType;

    private FmService mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTransparentStatusBar();

        mHandler = new MainActivityHandler(getMainLooper(), this);

        bindService(new Intent(MainActivity.this, FmService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

        mRadioScaleView = findViewById(R.id.view_radio_scale);
        tvFrequency = findViewById(R.id.tv_frequency);
        ivPlay = findViewById(R.id.iv_play);
        ivPlay.setOnClickListener(onClickListener);
        ivSearch = findViewById(R.id.iv_search);
        ivSearch.setOnClickListener(onClickListener);
        tvFmType = findViewById(R.id.tv_fm_type);
        tvFmType.setOnClickListener(onClickListener);
        tvFavType = findViewById(R.id.tv_fav_type);
        tvFavType.setOnClickListener(onClickListener);

        tvFmType.setSelected(true);
        changeFragment(1);

    }


    /**
     * 设置状态栏沉浸式透明
     */
    private void setTransparentStatusBar() {
        Window window = getWindow();
        // 清除半透明标志
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // 允许绘制系统栏背景
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // 设置状态栏颜色为透明
        window.setStatusBarColor(Color.TRANSPARENT);

        // 设置布局延伸到状态栏
        View decorView = window.getDecorView();
        int systemUiVisibility = decorView.getSystemUiVisibility();
        systemUiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        systemUiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(systemUiVisibility);
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

                    if (mService.isScanning()) {

                        mService.stopScan();

                    } else {


                        mService.startScanAsync();

                        ivSearch.setSelected(true);
                        mHandler.removeMessages(5001);
                        mHandler.sendEmptyMessage(5001);


                    }

                    break;

                case R.id.tv_fm_type:
                    changeFragment(1);
                    break;

                case R.id.tv_fav_type:
                    changeFragment(2);
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




    public class MainActivityHandler extends Handler {
        WeakReference<MainActivity> mMainActivity;

        public MainActivityHandler(Looper looper, MainActivity mainActivity) {
            super(looper);
            mMainActivity = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle;
            switch (msg.what) {
                case FmListener.MSGID_SCAN_FINISHED:

                    bundle = msg.getData();
                    //正常扫描完成true,中途取消false
                    boolean isScan = bundle.getBoolean(FmListener.KEY_IS_SCAN);
                    int searchedNum = bundle.getInt(FmListener.KEY_STATION_NUM);

                    Log.e("ww","FM电台扫描结束:" + isScan + ";" + searchedNum);

                    Toast.makeText(MainActivity.this,"FM电台扫描个数:" + searchedNum,Toast.LENGTH_SHORT).show();

                    ivSearch.setSelected(false);
                    mHandler.removeMessages(5001);

                    if(fmListFragment != null){
                        fmListFragment.setRadioScanData(getData());
                    }


                    break;

                case 5001:   //更新扫描UI

                    currentFrequency = Math.round(currentFrequency * 10) / 10.0f;   //保留一位小数

                    mRadioScaleView.setFrequency(currentFrequency);

                    tvFrequency.setText(String.valueOf(currentFrequency));


                    if (animGoOrBack) {
                        currentFrequency += 0.1f;
                        if (currentFrequency > 108.5) {
                            animGoOrBack = false;
                        }
                    } else {
                        currentFrequency -= 0.1f;
                        if (currentFrequency < 87.5) {
                            animGoOrBack = true;
                        }
                    }

                    mHandler.sendEmptyMessageDelayed(5001,50);

                    break;

                default:
                    break;
            }
        }
    }


    /**
     * type:1-FM列表；2-搜藏喜欢的列表
     * 使用 add/hide/show 模式
     */
    private void changeFragment(int type){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // 隐藏所有Fragment
        if (fmListFragment != null && fmListFragment.isAdded()) {
            transaction.hide(fmListFragment);
        }
        if (favListFragment != null && favListFragment.isAdded()) {
            transaction.hide(favListFragment);
        }

        switch (type){
            case 1:

                tvFmType.setSelected(true);
                tvFavType.setSelected(false);

                if(fmListFragment != null){
                    if (fmListFragment.isAdded()) {
                        transaction.show(fmListFragment);
                    } else {
                        transaction.add(R.id.fragment_radio, fmListFragment, "radio_fm");
                    }
                }

                break;
            case 2:

                tvFmType.setSelected(false);
                tvFavType.setSelected(true);

                if(favListFragment != null){
                    if (favListFragment.isAdded()) {
                        transaction.show(favListFragment);
                    } else {
                        transaction.add(R.id.fragment_radio, favListFragment, "radio_fav");
                    }
                }

                break;

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

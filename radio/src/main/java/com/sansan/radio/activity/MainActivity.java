package com.sansan.radio.activity;

import android.content.ComponentName;
import android.content.ContentValues;
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

    private MainActivityHandler mHandler;

    private int currentStation = 875;   //用int计算，用float显示UI
    private boolean animGoOrBack = true;

    private boolean mIsServiceBinded = false;

    private boolean isPlaying = true;  //是否在播放

    private int currentType = 1;

    private FmListFragment fmListFragment;
    private FavListFragment favListFragment;

    private RadioScaleView mRadioScaleView;
    private TextView tvFrequency;
    private ImageView ivSub;
    private ImageView ivAdd;
    private ImageView ivFav;
    private ImageView ivPrevious;
    private ImageView ivNext;
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

        // Should start FM service first.
        //先开始在绑定，退出Activity，Service才会继续工作
        startService(new Intent(MainActivity.this, FmService.class));
        mIsServiceBinded = bindService(new Intent(MainActivity.this, FmService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

        fmListFragment = new FmListFragment();
        favListFragment = new FavListFragment();

        mRadioScaleView = findViewById(R.id.view_radio_scale);
        tvFrequency = findViewById(R.id.tv_frequency);
        ivSub = findViewById(R.id.iv_sub);
        ivSub.setOnClickListener(onClickListener);
        ivAdd = findViewById(R.id.iv_add);
        ivAdd.setOnClickListener(onClickListener);
        ivFav = findViewById(R.id.iv_fav);
        ivFav.setOnClickListener(onClickListener);
        ivPrevious = findViewById(R.id.iv_previous);
        ivPrevious.setOnClickListener(onClickListener);
        ivNext = findViewById(R.id.iv_next);
        ivNext.setOnClickListener(onClickListener);
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

        fmListFragment.setOnFmListFragmentListener(new FmListFragment.OnFmListFragmentListener() {
            @Override
            public boolean isPlaying() {
                return isPlaying;
            }

            @Override
            public void powerUpAsync(int station) {

                currentStation = station;

                ivFav.setSelected(FmStation.isFavoriteStation(MainActivity.this, currentStation));

                tuneStation();

            }
        });

        favListFragment.setOnFavListFragmentListener(new FavListFragment.OnFavListFragmentListener() {
            @Override
            public boolean isPlaying() {
                return isPlaying;
            }

            @Override
            public void powerUpAsync(int station) {

                currentStation = station;

                ivFav.setSelected(FmStation.isFavoriteStation(MainActivity.this, currentStation));

                tuneStation();

            }
        });

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

            mService.initService(FmUtils.DEFAULT_STATION_50KHZ);

            mService.registerFmRadioListener(mFmRadioListener);

            currentStation = FmStation.getCurrentStation(MainActivity.this);

            float currentFrequency = FmUtils.computeFrequency(currentStation);

            ivFav.setSelected(FmStation.isFavoriteStation(MainActivity.this, currentStation));

            mRadioScaleView.setFrequency(currentFrequency);
            tvFrequency.setText(String.valueOf(currentFrequency));
            powerUpFm();

        }

        @Override
        public void onServiceDisconnected(ComponentName className) {

        }
    };




    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            switch (v.getId()){

                case R.id.iv_sub:

                    currentStation -= 1;
                    if (currentStation < 875) {
                        currentStation = 1085;
                    }

                    ivFav.setSelected(FmStation.isFavoriteStation(MainActivity.this, currentStation));
                    tuneStation();

                    fmListFragment.updateStationList(currentStation);
                    favListFragment.updateStationList(currentStation);

                    break;

                case R.id.iv_add:

                    currentStation += 1;
                    if (currentStation > 1085) {
                        currentStation = 875;
                    }

                    ivFav.setSelected(FmStation.isFavoriteStation(MainActivity.this, currentStation));
                    tuneStation();

                    fmListFragment.updateStationList(currentStation);
                    favListFragment.updateStationList(currentStation);
                    break;

                case R.id.iv_fav:

                    if (FmStation.isFavoriteStation(MainActivity.this, currentStation)) {
                        FmStation.removeFromFavorite(MainActivity.this, currentStation);
                        ivFav.setSelected(false);
                    } else {
                        if (FmStation.isStationExist(MainActivity.this, currentStation)) {
                            FmStation.addToFavorite(MainActivity.this, currentStation);
                        } else {
                            ContentValues values = new ContentValues(2);
                            values.put(FmStation.Station.FREQUENCY, currentStation);
                            values.put(FmStation.Station.IS_FAVORITE, true);
                            FmStation.insertStationToDb(MainActivity.this, values);
                        }
                        ivFav.setSelected(true);
                    }

                    fmListFragment.updateStationList(currentStation);
                    favListFragment.updateStationList(currentStation);

                    break;

                case R.id.iv_play:

                    if (mService != null) {
                        if (mService.getPowerStatus() == FmService.POWER_UP) {
                            powerDownFm();

                            isPlaying = false;
                            fmListFragment.updatePlayAnim(false);
                            favListFragment.updatePlayAnim(false);
                        } else {
                            powerUpFm();

                            isPlaying = true;
                            fmListFragment.updatePlayAnim(true);
                            favListFragment.updatePlayAnim(true);
                        }
                    }

                    break;

                case R.id.iv_search:

                    if (mService.isScanning()) {

                        mService.stopScan();

                    } else {


                        mService.startScanAsync();

                        ivPlay.setSelected(false);
                        ivSearch.setSelected(true);
                        mHandler.removeMessages(5001);
                        mHandler.sendEmptyMessage(5001);


                    }

                    break;

                case R.id.iv_previous:
                    fmListFragment.updateStationItemInternal(false);
                    break;

                case R.id.iv_next:
                    fmListFragment.updateStationItemInternal(true);
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




    /**
     * Power up FM
     */
    private void powerUpFm() {
        ivPlay.setSelected(true);
        mService.powerUpAsync(FmUtils.computeFrequency(currentStation));
    }

    /**
     * Power down FM
     */
    private void powerDownFm() {
        ivPlay.setSelected(false);
        mService.powerDownAsync();
    }


    /**
     * 播放指定电台:用户手动输入频率，或在已开启收音机的情况下切换电台
     */
    private void tuneStation(){

        float currentFrequency = FmUtils.computeFrequency(currentStation);

        mRadioScaleView.setFrequency(currentFrequency);
        tvFrequency.setText(String.valueOf(currentFrequency));

        ivPlay.setSelected(true);

        mService.tuneStationAsync(currentFrequency);
    }

    /**
     * 扫描结束
     */
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

                    ivSearch.setSelected(false);
                    mHandler.removeMessages(5001);

                    currentStation = FmStation.getCurrentStation(MainActivity.this);

                    fmListFragment.updateStationList(currentStation);

                    tuneStation();

                    break;

                case 5001:   //更新扫描UI

                    float currentFrequency = FmUtils.computeFrequency(currentStation);

                    mRadioScaleView.setFrequency(currentFrequency);

                    tvFrequency.setText(String.valueOf(currentFrequency));


                    if (animGoOrBack) {
                        currentStation += 1;
                        if (currentStation > 1085) {
                            animGoOrBack = false;
                        }
                    } else {
                        currentStation -= 1;
                        if (currentStation < 875) {
                            animGoOrBack = true;
                        }
                    }

                    mHandler.sendEmptyMessageDelayed(5001,20);

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

        currentType = type;

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

        transaction.commitAllowingStateLoss();

    }





    @Override
    public void onDestroy() {
        // need to call this function because if doesn't do this,after
        // configuration change will have many instance and recording time
        // or playing time will not refresh
        // Remove all the handle message
        mHandler.removeCallbacksAndMessages(null);
        if (mService != null) {
            mService.unregisterFmRadioListener(mFmRadioListener);
        }
        if (mIsServiceBinded) {
            unbindService(mServiceConnection);
            mIsServiceBinded = false;
        }
        mFmRadioListener = null;
        super.onDestroy();
    }



}

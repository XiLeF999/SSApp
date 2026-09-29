package com.sansan.radio.activity;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.sansan.radio.R;
import com.sansan.radio.bean.StationInfo;
import com.sansan.radio.dialog.QuitDialog;
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

    private static final String TAG_FM_LIST = "radio_fm";
    private static final String TAG_FAV_LIST = "radio_fav";
    private static final String STATE_CURRENT_TYPE = "current_type";
    private static final int MSG_UPDATE_SCAN_UI = 5001;
    private static final int MSG_HIDE_SCAN_RESULT = 5003;

    private MainActivityHandler mHandler;

    private int currentStation = 87500;   //用int计算，用float显示UI。每次变化为100
    private boolean animGoOrBack = true;

    private boolean mIsServiceBinded = false;

    private boolean isPlaying = true;  //是否在播放

    private int currentType = 1;

    private float scanProgress = 0.0f;
    private boolean isKy1045 = false;

    private FrameLayout flSide;

    private FmListFragment fmListFragment;
    private FavListFragment favListFragment;

    private RadioScaleView mRadioScaleView;
    private TextView tvFrequency;
    private ImageView ivSub;
    private TextView tvScanProgress;
    private ImageView ivSearchDecorLeft;
    private ImageView ivSearchDecorRight;
    private ImageView ivAdd;
    private ImageView ivFav;
    private ImageView ivPrevious;
    private ImageView ivNext;
    private ImageView ivPlay;
    private ImageView ivSearch;
    private TextView tvFmType;
    private TextView tvFavType;
    private Typeface tabTypeface;

    private FmService mService = null;

    private QuitDialog quitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTransparentStatusBar();
        isKy1045 = getResources().getBoolean(R.bool.is_ky1045);

        mHandler = new MainActivityHandler(getMainLooper(), this);

        quitDialog = new QuitDialog(this);

        // Should start FM service first.
        //先开始在绑定，退出Activity，Service才会继续工作
        startService(new Intent(MainActivity.this, FmService.class));
        mIsServiceBinded = bindService(new Intent(MainActivity.this, FmService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

        Fragment restoredFm = getSupportFragmentManager()
                .findFragmentByTag(TAG_FM_LIST);
        Fragment restoredFav = getSupportFragmentManager()
                .findFragmentByTag(TAG_FAV_LIST);
        fmListFragment = restoredFm instanceof FmListFragment
                ? (FmListFragment) restoredFm : new FmListFragment();
        favListFragment = restoredFav instanceof FavListFragment
                ? (FavListFragment) restoredFav : new FavListFragment();
        if (savedInstanceState != null) {
            currentType = savedInstanceState.getInt(STATE_CURRENT_TYPE, 1);
        }

        flSide = findViewById(R.id.fl_side);
        mRadioScaleView = findViewById(R.id.view_radio_scale);
        tvFrequency = findViewById(R.id.tv_frequency);
        ivSub = findViewById(R.id.iv_sub);
        ivSub.setOnClickListener(onClickListener);
        tvScanProgress = findViewById(R.id.tv_scan_progress);
        if (isKy1045) {
            ivSearchDecorLeft = findOptionalImageView("iv_search_decor_left");
            ivSearchDecorRight = findOptionalImageView("iv_search_decor_right");
            updateSearchDecor(false);
        }
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
        tabTypeface = tvFmType.getTypeface();
        tvFmType.setOnClickListener(onClickListener);
        tvFavType = findViewById(R.id.tv_fav_type);
        tvFavType.setOnClickListener(onClickListener);

        changeFragment(currentType);

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


        mRadioScaleView.setOnFrequencyChangeListener(new RadioScaleView.OnFrequencyChangeListener() {
            @Override
            public void onFrequencyChanging(float frequency) {
                tvFrequency.setText(String.format("%.1f", frequency));
            }

            @Override
            public void onFrequencyChanged(float frequency) {
                currentStation = FmUtils.computeStation(frequency);
                ivFav.setSelected(FmStation.isFavoriteStation(MainActivity.this, currentStation));
                tuneStation();

                fmListFragment.updateStationList(currentStation);
                favListFragment.updateStationList(currentStation);
            }
        });


        quitDialog.setOnQuitDialogClickListener(new QuitDialog.OnQuitDialogClickListener() {
            @Override
            public void onQuit() {

                if (mService != null) {
                    mService.quitFm();           // 停止所有收音机操作，释放资源，停止服务
                }

                //解绑服务
                if (mIsServiceBinded) {
                    unbindService(mServiceConnection);
                    mIsServiceBinded = false;
                }

                //关闭所有 Activity
                finishAffinity();

            }

            @Override
            public void onGoBack() {
                finish();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        onConfigurationChanged(getResources().getConfiguration());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean isHalfScreen = newConfig.screenWidthDp == 640;
        flSide.setVisibility(isHalfScreen ? View.GONE : View.VISIBLE);
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

            int id = v.getId();

            if(id == R.id.iv_sub){
                currentStation -= 100;
                if (currentStation < 87500) {
                    currentStation = 108500;
                }

                ivFav.setSelected(FmStation.isFavoriteStation(MainActivity.this, currentStation));
                tuneStation();

                fmListFragment.updateStationList(currentStation);
                favListFragment.updateStationList(currentStation);
            } else if(id == R.id.iv_add){
                currentStation += 100;
                if (currentStation > 108500) {
                    currentStation = 87500;
                }

                ivFav.setSelected(FmStation.isFavoriteStation(MainActivity.this, currentStation));
                tuneStation();

                fmListFragment.updateStationList(currentStation);
                favListFragment.updateStationList(currentStation);
            } else if(id == R.id.iv_fav){
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
            } else if(id == R.id.iv_previous){
                if(currentType == 1){
                    fmListFragment.updateStationItemInternal(false);
                } else {
                    favListFragment.updateStationItemInternal(false);
                }
            } else if(id == R.id.iv_next){
                if(currentType == 1){
                    fmListFragment.updateStationItemInternal(true);
                } else {
                    favListFragment.updateStationItemInternal(true);
                }
            } else if(id == R.id.iv_play){
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
            } else if(id == R.id.iv_search){
                if (mService == null) {
                    return;
                }

                if (mService.isScanning()) {

                    mService.stopScan();

                } else {

                    mService.startScanAsync();
                    startScanAnimation();

                }
            } else if(id == R.id.tv_fm_type){
                changeFragment(1);
            } else if(id == R.id.tv_fav_type){
                changeFragment(2);
            }


        }
    };

    private void startScanAnimation() {
        scanProgress = 0.0f;
        ivPlay.setSelected(false);
        ivSearch.setSelected(true);
        mHandler.removeMessages(MSG_UPDATE_SCAN_UI);
        mHandler.removeMessages(MSG_HIDE_SCAN_RESULT);
        mHandler.sendEmptyMessage(MSG_UPDATE_SCAN_UI);

        isPlaying = false;
        fmListFragment.updatePlayAnim(false);
        favListFragment.updatePlayAnim(false);
        if (isKy1045) {
            updateSearchDecor(true);
            fmListFragment.setSearching(true);
        }
    }

    private ImageView findOptionalImageView(String resourceName) {
        int resourceId = getResources().getIdentifier(resourceName, "id", getPackageName());
        return resourceId == 0 ? null : findViewById(resourceId);
    }

    private void updateSearchDecor(boolean active) {
        if (!isKy1045 || ivSearchDecorLeft == null || ivSearchDecorRight == null) {
            return;
        }

        setOptionalDrawable(ivSearchDecorLeft,
                active ? "radio_decor_l_bg_on" : "radio_decor_l_bg_off");
        setOptionalDrawable(ivSearchDecorRight,
                active ? "radio_decor_r_bg_on" : "radio_decor_r_bg_off");
    }

    private void setOptionalDrawable(ImageView imageView, String resourceName) {
        int resourceId = getResources().getIdentifier(resourceName, "drawable", getPackageName());
        if (resourceId != 0) {
            imageView.setImageResource(resourceId);
        }
    }




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
        isPlaying = true;
        fmListFragment.updatePlayAnim(true);
        favListFragment.updatePlayAnim(true);

        mService.tuneStationAsync(currentFrequency,false);
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
                    mHandler.removeMessages(MSG_UPDATE_SCAN_UI);
                    scanProgress = 0.0f;

                    currentStation = FmStation.getCurrentStation(MainActivity.this);

                    fmListFragment.updateStationList(currentStation);

                    if (isKy1045 && isScan) {
                        updateSearchDecor(true);
                        fmListFragment.setSearching(false);
                        tvScanProgress.setText("电台搜索完成");
                        tvScanProgress.setVisibility(View.VISIBLE);
                        mHandler.sendEmptyMessageDelayed(MSG_HIDE_SCAN_RESULT, 1500L);
                    } else {
                        tvScanProgress.setVisibility(View.GONE);
                    }

                    isPlaying = true;
                    fmListFragment.updatePlayAnim(true);
                    favListFragment.updatePlayAnim(true);

                    tuneStation();

                    break;

                case MSG_UPDATE_SCAN_UI:   //更新扫描UI

                    float currentFrequency = FmUtils.computeFrequency(currentStation);

                    mRadioScaleView.setFrequency(currentFrequency);

                    tvFrequency.setText(String.valueOf(currentFrequency));

                    tvScanProgress.setVisibility(View.VISIBLE);
                    tvScanProgress.setText("电台搜索" + (int)scanProgress + "%");
                    scanProgress += 0.32f;
                    if(scanProgress > 99){
                        scanProgress = 99.0f;
                    }

                    if (animGoOrBack) {
                        currentStation += 100;
                        if (currentStation > 108500) {
                            animGoOrBack = false;
                        }
                    } else {
                        currentStation -= 100;
                        if (currentStation < 87500) {
                            animGoOrBack = true;
                        }
                    }

                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_SCAN_UI,30);

                    break;

                case MSG_HIDE_SCAN_RESULT:
                    tvScanProgress.setVisibility(View.GONE);
                    scanProgress = 0.0f;

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
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof FmListFragment && fragment != fmListFragment) {
                transaction.remove(fragment);
            } else if (fragment instanceof FavListFragment && fragment != favListFragment) {
                transaction.remove(fragment);
            }
        }
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
                tvFmType.setTypeface(tabTypeface, Typeface.BOLD);
                tvFavType.setTypeface(tabTypeface, Typeface.NORMAL);

                if(fmListFragment != null){
                    if (fmListFragment.isAdded()) {
                        transaction.show(fmListFragment);
                    } else {
                        transaction.add(R.id.fragment_radio, fmListFragment, TAG_FM_LIST);
                    }
                }

                break;
            case 2:

                tvFmType.setSelected(false);
                tvFavType.setSelected(true);
                tvFmType.setTypeface(tabTypeface, Typeface.NORMAL);
                tvFavType.setTypeface(tabTypeface, Typeface.BOLD);

                if(favListFragment != null){
                    if (favListFragment.isAdded()) {
                        transaction.show(favListFragment);
                    } else {
                        transaction.add(R.id.fragment_radio, favListFragment, TAG_FAV_LIST);
                    }
                }

                break;

        }

        transaction.commitAllowingStateLoss();

    }





    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_CURRENT_TYPE, currentType);
        super.onSaveInstanceState(outState);
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



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (quitDialog.isShowing()){
                quitDialog.dismiss();
            } else {
                quitDialog.show();
            }

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }



}

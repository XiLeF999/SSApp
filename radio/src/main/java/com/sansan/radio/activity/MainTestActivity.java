package com.sansan.radio.activity;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioSystem;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;

import com.android.fmradio.FmNative;
import com.sansan.radio.R;

public class MainTestActivity extends FragmentActivity {

    private AudioManager mAudioManager;

    private Button bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_main);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        bt = findViewById(R.id.bt);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play93_5MHz();
            }
        });
    }







    private void play93_5MHz() {

        // 检查音频模式，确保不在通话中
        if (mAudioManager.getMode() == AudioManager.MODE_IN_CALL ||
                mAudioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
            Log.e("ww", "系统处于通话模式，无法启动FM");
            return;
        }

        // 请求音频焦点
        int result = mAudioManager.requestAudioFocus(
                mAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e("ww", "获取音频焦点失败");
            return;
        } else {
            Log.e("ww", "获取音频焦点成功");
        }

        // 初始化FM设备
        if (!initFmDevice()) {
            Log.e("ww", "FM设备初始化失败");
            return;
        }

        // FM上电
        if (!powerUpFm(93.5f)) {
            Log.e("ww", "FM上电失败");
            return;
        }



        




        // 5. 调谐到93.5MHz并播放
        if (!tuneAndPlay(93.5f)) {
            Log.e("ww", "调谐播放失败");
            return;
        }

        Log.e("ww", "93.5MHz播放成功");


        // 添加调试信息
        int audioDevices = mAudioManager.getDevicesForStream(AudioManager.STREAM_MUSIC);
        Log.e("ww", "当前音频设备: " + audioDevices);
        Log.e("ww", "音频焦点状态: " + mAudioManager.isAudioFocusExclusive());


//        initAudioPathAndStartPlayback();



    }


//    private void initAudioPathAndStartPlayback() {
//        Log.d("ww", "初始化音频路径并开始播放");
//
//        // 创建AudioRecord和AudioTrack（基于FmService文档）
//        initAudioRecordSink();
//
//        // 根据音频输出设备创建音频路径
//        createAudioPatch();
//
//        // 开始音频渲染
//        startAudioPlayback();
//    }
//
//    private boolean initAudioRecordSink() {
//        Log.d("ww", "初始化音频录制和播放设备");
//        try {
//            // 创建AudioRecord从FM硬件读取音频
//            int bufferSize = AudioRecord.getMinBufferSize(44100,
//                    AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
//
//            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.FM_TUNER,
//                    44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
//
//            // 创建AudioTrack用于播放
//            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
//                    44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
//                    bufferSize, AudioTrack.MODE_STREAM);
//
//            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED &&
//                    audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
//                Log.d("ww", "音频设备初始化成功");
//                return true;
//            }
//        } catch (Exception e) {
//            Log.e("ww", "音频设备初始化失败: " + e.getMessage());
//        }
//        return false;
//    }
//
//    private void createAudioPatch() {
//        Log.d("ww", "创建音频路由");
//        // 根据当前音频输出设备设置音频路由
//        int device = mAudioManager.getDevicesForStream(AudioManager.STREAM_MUSIC);
//        Log.d("ww", "当前音频输出设备: " + device);
//
//        // 设置音频强制使用（耳机或扬声器）
//        if ((device & AudioDeviceInfo.TYPE_WIRED_HEADPHONES) != 0 ||
//                (device & AudioDeviceInfo.TYPE_WIRED_HEADSET) != 0) {
//            // 使用耳机
//            AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_NONE);
//            Log.d("ww", "音频路由设置为耳机");
//        } else {
//            // 使用扬声器
//            AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_SPEAKER);
//            Log.d("ww", "音频路由设置为扬声器");
//        }
//    }
//
//    private void startAudioPlayback() {
//        Log.d("ww", "开始音频播放");
//        // 这里需要实现音频数据的读取和播放逻辑
//        // 或者检查是否需要启动音频渲染线程
//    }




    private boolean initFmDevice() {
        // 打开FM设备驱动
        boolean success = FmNative.openDev();
        if (success) {
            Log.d("ww", "FM设备初始化成功");
        }
        return success;
    }

    private boolean powerUpFm(float frequency) {

        // 调用Native层上电
        boolean success = FmNative.powerUp(frequency);
        if (success) {
            Log.d("ww", "FM上电成功，频率：" + frequency);

            // 初始化音频路径
            initAudioPath();
        }
        return success;
    }

    private boolean tuneAndPlay(float frequency) {
        // 调谐到指定频率
        boolean tuneSuccess = FmNative.tune(frequency);
        if (!tuneSuccess) {
            Log.e("ww", "频率调谐失败");
            return false;
        }

        // 取消静音开始播放
        int muteSuccess = FmNative.setMute(false);
        Log.e("ww", "取消静音:" + muteSuccess);

        Log.d("ww", "播放频率：" + frequency + "MHz 成功");
        return true;
    }

    private void initAudioPath() {
        // 简化版音频路径初始化
        // 实际可能需要创建AudioRecord和AudioTrack
        Log.d("ww", "初始化音频路径");

        // 设置音频输出到扬声器（根据需求调整）
        AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_NONE);
    }


    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            // 永久失去焦点，停止播放
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // 暂时失去焦点，静音
                            FmNative.setMute(true);
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            // 重新获得焦点，取消静音
                            FmNative.setMute(false);
                            break;
                    }
                }
            };









}

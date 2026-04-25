/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sansan.radio.fm;

import static android.util.MathUtils.exp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;
import android.hardware.radio.RadioTuner;
import android.media.AudioDeviceInfo;
import android.media.AudioDevicePort;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioManager.OnAudioPortUpdateListener;
import android.media.AudioMixPort;
import android.media.AudioPatch;
import android.media.AudioPort;
import android.media.AudioPortConfig;
import android.media.AudioRecord;
import android.media.AudioSystem;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.sansan.radio.R;
import com.sansan.radio.fm.FmStation.Station;

import androidx.core.content.ContextCompat;

import com.sansan.radio.activity.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Background service to control FM or do background tasks.
 */
public class FmService extends Service implements FmRecorder.OnRecorderStateChangedListener {
    // Logging
    private static final String TAG = "[FmService]";

    // Broadcast messages from other sounder APP to FM service
    private static final String SOUND_POWER_DOWN_MSG = "com.android.music.musicservicecommand";
    private static final String FM_SEEK_PREVIOUS = "fmradio.seek.previous";
    private static final String FM_SEEK_NEXT = "fmradio.seek.next";
    private static final String FM_TURN_OFF = "fmradio.turnoff";
    private static final String CMDPAUSE = "pause";

    // HandlerThread Keys
    private static final String FM_FREQUENCY = "frequency";
    private static final String OPTION = "option";
    private static final String RECODING_FILE_NAME = "name";
    private static final String DIRECTION = "direction";

    
    private static final int AUDIO_FRAMES_TO_IGNORE_COUNT = 3;

    // Headset
    private static final int HEADSET_PLUG_IN = 1;

    // Notification id
    private static final int NOTIFICATION_ID = 1;

    // Set audio policy for FM
    // should check AUDIO_POLICY_FORCE_FOR_MEDIA in audio_policy.h
    private static final int FOR_PROPRIETARY = 1;
    // Forced Use value
    private int mForcedUseForMedia;

    // FM recorder
    FmRecorder mFmRecorder = null;
    private BroadcastReceiver mSdcardListener = null;
    private boolean isCancel = false;
    private int mRecordState = FmRecorder.STATE_INVALID;
    private int mRecorderErrorType = -1;
    // If eject record sdcard, should set Value false to not record.
    // Key is sdcard path(like "/storage/sdcard0"), V is to enable record or
    // not.
    private HashMap<String, Boolean> mSdcardStateMap = new HashMap<String, Boolean>();
    // record the listener list, will notify all listener in list
    private ArrayList<Record> mRecords = new ArrayList<Record>();
    // record sd card path when start recording
    private static String sRecordingSdcard = FmUtils.getDefaultStoragePath();

    // RDS
    // PS String
    private String mPsString = "";
    // RT String
    private String mRtTextString = "";
    // Notification target class name
    private String mTargetClassName = MainActivity.class.getName();

    // State variables
    // Record whether FM is in native scan state
    private boolean mIsNativeScanning = false;
    // Record whether FM is in scan thread
    private boolean mIsScanning = false;
    // Record whether FM is in seeking state
    private boolean mIsNativeSeeking = false;
    // Record whether FM is in native seek
    private boolean mIsSeeking = false;
    // Record whether searching progress is canceled
    private boolean mIsStopScanCalled = false;
    // Record whether is speaker used
    private boolean mIsSpeakerUsed = false;
    // Record Power Status
    private int mPowerStatus = POWER_DOWN;

    public static int POWER_UP = 0;
    public static int DURING_POWER_UP = 1;
    public static int POWER_DOWN = 2;
    private boolean mIsServiceInited = false;
    private boolean mIsDistanceExceed = false;
    private boolean mIsFmMainForeground = true;
    private boolean mIsFmFavoriteForeground = false;
    private boolean mIsFmRecordForeground = false;
    // Instance variables
    private Context mContext = null;
    private AudioManager mAudioManager = null;
    // Audio focus is held or not
    private boolean mIsAudioFocusHeld = false;
    // Focus transient lost
    private boolean mPausedByTransientLossOfFocus = false;
    private int mCurrentStation = FmUtils.DEFAULT_STATION;
    // Headset plug state (0:long antenna plug in, 1:long antenna plug out)
    private int mValueHeadSetPlug = 1;
    // For bind service
    private final IBinder mBinder = new ServiceBinder();
    // Broadcast to receive the external event
    private FmServiceBroadcastReceiver mBroadcastReceiver = null;
    // Async handler
    private FmRadioServiceHandler mFmServiceHandler;
    // Lock for lose audio focus and receive SOUND_POWER_DOWN_MSG
    // at the same time
    // while recording call stop recording not finished(status is still
    // RECORDING), but
    // SOUND_POWER_DOWN_MSG will exitFm(), if it is RECORDING will discard the
    // record.
    // 1. lose audio focus -> stop recording(lock) -> set to IDLE and show save
    // dialog
    // 2. exitFm() -> check the record status, discard it if it is recording
    // status(lock)
    // Add this lock the exitFm() while stopRecording()
    private Object mStopRecordingLock = new Object();
    // The listener for exit, should finish favorite when exit FM
    private static OnExitListener sExitListener = null;
    // The latest status for mute/unmute
    private boolean mIsMuted = false;

    private Object mRenderLock = new Object();
    private Object mRenderingLock = new Object();

    // Audio Patch
    private Object mAudioPatch = null;
    private Object mNotificationLock = new Object();

    private Notification.Builder mNotificationBuilder = null;
    private NotificationManager notificationManager = null;
    private NotificationChannel notificationChannel = null;

    private RadioManager mRadioManager;
    private RadioTuner mRadioTuner;

    private Object mSearchLock = new Object();
    private Object mLock = new Object();
    private List<RadioManager.ModuleProperties> mModules;

    private static final String FM_STOP_RECORDING = "fmradio.stop.recording";
    private static final String FM_ENTER_RECORD_SCREEN = "fmradio.enter.record.screen";
    private float mSeekFreqClbk = (float) 8750 / 100;
    private int[] mFreqList;
    private static String NOW_MODE = "now_mode";
    private boolean mValidStation = false;
    private boolean mSetFreqPending = false;
    private boolean mSearchStationPending = false;
    private boolean mStartFullStationPending = false;
    private boolean mSeekStationPending = false;

    private final String TUNE_STATION = "tuneStation";
    private final String SEEK_STATION = "seekStation";
    private final String START_STATION = "startScan";
    private final String START_FULL_STATION = "startFullScan";
    private final String channelId= "com.sansan.radio.fm";
    private final int pdFlag= 201326592;
    private final int intentFlag= 268435456;
    private boolean isEnableDenoise = false;

    private static final String IS_OPEN_SESSION = "IsSessionOpen";
    private static final int OPEN_SESSION = 1;
    private static final int CLOSE_SESSION = 0;

    private static int mStep = 100;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * class use to return service instance
     */
    public class ServiceBinder extends Binder {
        /**
         * get FM service instance
         *
         * @return service instance
         */
        public FmService getService() {
            return FmService.this;
        }
    }

    /**
     * Broadcast monitor external event, Other app want FM stop, Phone shut
     * down, screen state, headset state
     */
    private class FmServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String command = intent.getStringExtra("command");
            Log.d(TAG, "onReceive, action = " + action + " / command = " + command);
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                Log.d(TAG,"onReceive state: "+state +" ,FOR_PROPRIETARY: "+AudioSystem.getForceUse(FOR_PROPRIETARY));
                if (state == BluetoothAdapter.STATE_OFF) {
                    AudioSystem.setForceUse(FOR_PROPRIETARY, mForcedUseForMedia);
                }
            }

            // other app want FM stop, stop FM
            if ((SOUND_POWER_DOWN_MSG.equals(action) && CMDPAUSE.equals(command))) {
                exitFm();
                stopSelf();
                // phone shut down, so exit FM
            } else if (Intent.ACTION_SHUTDOWN.equals(action)) {
                /**
                 * here exitFm, system will send broadcast, system will shut
                 * down, so fm does not need call back to activity
                 */
                exitFm();
                // switch antenna when headset plug in or plug out
            } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                // switch antenna should not impact audio focus status
                mValueHeadSetPlug = (intent.getIntExtra("state", -1) == HEADSET_PLUG_IN) ? 0 : 1;
                switchAntennaAsync(mValueHeadSetPlug);

                // Avoid Service is killed,and receive headset plug in
                // broadcast again
                if (!mIsServiceInited) {
                    Log.d(TAG, "onReceive, mIsServiceInited is false");
                    return;
                }
                /*
                 * If ear phone insert and activity is
                 * foreground. power up FM automatic
                 */

                Log.d(TAG," mValueHeadSetPlug: "+mValueHeadSetPlug);

                if ((0 == mValueHeadSetPlug) && isActivityForeground()) {
                    powerUpAsync(FmUtils.computeFrequency(mCurrentStation));
                } else if (1 == mValueHeadSetPlug) {
                    mFmServiceHandler.removeMessages(FmListener.MSGID_SCAN_FINISHED);
                    mFmServiceHandler.removeMessages(FmListener.MSGID_SEEK_FINISHED);
                    mFmServiceHandler.removeMessages(FmListener.MSGID_TUNE_FINISHED);
                    mFmServiceHandler.removeMessages(
                            FmListener.MSGID_POWERDOWN_FINISHED);
                    mFmServiceHandler.removeMessages(
                            FmListener.MSGID_POWERUP_FINISHED);
                    focusChanged(AudioManager.AUDIOFOCUS_LOSS);

                    // Need check to switch to earphone mode for audio will
                    // change to AudioSystem.FORCE_NONE
                    setForceUse(false);

                    // Notify UI change to earphone mode, false means not speaker mode
                    Bundle bundle = new Bundle(2);
                    bundle.putInt(FmListener.CALLBACK_FLAG,
                            FmListener.LISTEN_SPEAKER_MODE_CHANGED);
                    bundle.putBoolean(FmListener.KEY_IS_SPEAKER_MODE, false);
                    notifyActivityStateChanged(bundle);
                }
            } else if (AudioManager.VOLUME_CHANGED_ACTION.equals(action)) {
                if (isScanning() || mIsSeeking || mIsMuted) {
                    return;
                }
                // change volume by FM
                if (intent.hasExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE)
                        && intent.hasExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE)) {
                    int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                    if (streamType == AudioManager.STREAM_MUSIC) {
                        int index = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        if (index != -1) {
                            Log.d(TAG, "streamType = " + streamType + ", volume = " + index);
                            setVolume(index);
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle sdcard mount/unmount event. 1. Update the sdcard state map 2. If
     * the recording sdcard is unmounted, need to stop and notify
     */
    private class SdcardListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // If eject record sdcard, should set this false to not
            // record.
            updateSdcardStateMap(intent);

            if (mFmRecorder == null) {
                Log.w(TAG, "SdcardListener.onReceive, mFmRecorder is null");
                return;
            }

            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_EJECT.equals(action) ||
                    Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
                // If not unmount recording sd card, do nothing;
                if (isRecordingCardUnmount(intent)) {
                    if (mFmRecorder.getState() == FmRecorder.STATE_RECORDING) {
                        onRecorderError(FmRecorder.ERROR_SDCARD_NOT_PRESENT);
                        mFmRecorder.discardRecording();
                    } else {
                        Bundle bundle = new Bundle(2);
                        bundle.putInt(FmListener.CALLBACK_FLAG,
                                FmListener.LISTEN_RECORDSTATE_CHANGED);
                        bundle.putInt(FmListener.KEY_RECORDING_STATE,
                                FmRecorder.STATE_IDLE);
                        notifyActivityStateChanged(bundle);
                    }
                }
            }
        }
    }

    /**
     * whether antenna available
     *
     * @return true, antenna available; false, antenna not available
     */
    public boolean isAntennaAvailable() {
        return true; //mAudioManager.isWiredHeadsetOn();
    }

    private void setForceUse(boolean isSpeaker) {
        /**
         * Original Android code:
         * mForcedUseForMedia = isSpeaker ? AudioSystem.FORCE_SPEAKER : AudioSystem.FORCE_NONE;
         * AudioSystem.setForceUse(FOR_PROPRIETARY, mForcedUseForMedia);
         */
        Log.d(TAG, "setForceUse() : isSpeaker = " + isSpeaker);
        setFmRoute(isSpeaker);
        mIsSpeakerUsed = isSpeaker;
        mForcedUseForMedia = isSpeaker ? AudioSystem.FORCE_SPEAKER : AudioSystem.FORCE_NONE;
        AudioSystem.setForceUse(FOR_PROPRIETARY, mForcedUseForMedia);

        FmUtils.setIsSpeakerPhoneOn(mContext, isSpeaker);
    }

    private void setFmRoute(boolean isSpeaker) {
        Log.d(TAG," setFmRoute isSpeaker =  "+isSpeaker + ",mPowerStatus: "+mPowerStatus);
        //There are only three models
        if (isSpeaker) {
            mAudioManager.setParameters("fm_route=fm_speaker");
        } else {
            //If a headset is inserted set to headset mode otherwise off
            if (isAntennaAvailable() && mPowerStatus == POWER_UP){
                mAudioManager.setParameters("fm_route=fm_headset");
            }else {
                mAudioManager.setParameters("fm_route=disabled");
            }
        }

    }

    /**
     * Set FM audio from speaker or not
     *
     * @param isSpeaker true if set FM audio from speaker
     */
    public void setSpeakerPhoneOn(boolean isSpeaker) {
        Log.d(TAG, "setSpeakerPhoneOn  isSpeaker = " + isSpeaker);
        /**
         *Original Android code: setForceUse(isSpeaker);
         */
        setMute(true);
        setForceUse(isSpeaker);
        setMute(false);
    }

    AudioDevicePort mAudioSource = null;
    AudioDevicePort mAudioSink = null;

    /**
     * power up FM, and make FM voice output from earphone
     *
     * @param frequency
     */
    public void powerUpAsync(float frequency) {
        Log.d(TAG, "powerUpAsync() : frequency = " + frequency);
        final int bundleSize = 1;
        mFmServiceHandler.removeMessages(FmListener.MSGID_POWERUP_FINISHED);
        mFmServiceHandler.removeMessages(FmListener.MSGID_POWERDOWN_FINISHED);
        Bundle bundle = new Bundle(bundleSize);
        bundle.putFloat(FM_FREQUENCY, frequency);
        Message msg = mFmServiceHandler.obtainMessage(FmListener.MSGID_POWERUP_FINISHED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    private boolean powerUp(float frequency) {
        Log.d(TAG, "powerUp() : mPowerStatus = " + mPowerStatus);
        if (mPowerStatus == POWER_UP) {
            return true;
        }
        if (!requestAudioFocus()) {
            // activity used for update powerdown menu
            mPowerStatus = POWER_DOWN;
            Log.d(TAG, "powerUp() : return POWER_DOWN");
            return false;
        }

        if  ((mAudioManager.getMode() == AudioSystem.MODE_IN_COMMUNICATION)||
             (mAudioManager.getMode() == AudioSystem.MODE_IN_CALL)) {

        //AudioManager takes time to setMode so we need to wait before checking modes again (maximum 1.5 sec)
            int i=0;
            do {
                try {
                    final int hundredMillisecond = 250;
                    Thread.sleep(hundredMillisecond);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            } while(((mAudioManager.getMode() == AudioSystem.MODE_IN_COMMUNICATION)||
                     (mAudioManager.getMode() == AudioSystem.MODE_IN_CALL))&&i<6);

            if((mAudioManager.getMode() == AudioSystem.MODE_IN_COMMUNICATION)||
                (mAudioManager.getMode() == AudioSystem.MODE_IN_CALL)){
                Log.d(TAG, "Audio is in communication mode, dont start FM");
                return false;
            }
        }

        mPowerStatus = DURING_POWER_UP;
        mPowerStatus = POWER_UP;

        //synchronized (mAudioPatchLock) {
            initAudioRecordSink();
            createAudioPatch();
            if (!requestAudioFocus()) {
                // activity used for update powerdown menu
                mPowerStatus = POWER_DOWN;
                Log.d(TAG, "release audio patch & stop audio track");
                releaseAudioPatch();
                stopAudioTrack();
                return false;
            }
        //}
        // need mute after power up
        setMute(true);

        if (!mIsRender) {
            releaseAudioPatch();
            startRender();
        }

        return (mPowerStatus == POWER_UP);
    }

    private boolean playFrequency(float frequency) {
        Log.d(TAG, "playFrequency() : frequency = " + frequency);
        mCurrentStation = FmUtils.computeStation(frequency);
        FmStation.setCurrentStation(mContext, mCurrentStation);
        updatePlayingNotification();

        if (mRecordState != FmRecorder.STATE_PLAYBACK) {
            enableFmAudio(true);
        }
        setForceUse(mIsSpeakerUsed);
        setMute(false);

        return (mPowerStatus == POWER_UP);
    }

    /**
     * power down FM
     */
    public void powerDownAsync() {
        // if power down Fm, should remove message first.
        // not remove all messages, because such as recorder message need
        // to execute after or before power down
        mFmServiceHandler.removeMessages(FmListener.MSGID_SCAN_FINISHED);
        mFmServiceHandler.removeMessages(FmListener.MSGID_SEEK_FINISHED);
        mFmServiceHandler.removeMessages(FmListener.MSGID_TUNE_FINISHED);
        mFmServiceHandler.removeMessages(FmListener.MSGID_POWERDOWN_FINISHED);
        mFmServiceHandler.removeMessages(FmListener.MSGID_POWERUP_FINISHED);
        mFmServiceHandler.sendEmptyMessage(FmListener.MSGID_POWERDOWN_FINISHED);
    }

    /**
     * Power down FM
     *
     * @return true if power down success
     */
    private boolean powerDown() {
        if (mPowerStatus == POWER_DOWN && !mQbPowerOff) {
            return true;
        }
        setMute(true);
        enableFmAudio(false);
        mPowerStatus = POWER_DOWN;

        synchronized (mNotificationLock) {
            removeNotification();
        }
        return true;
    }

    private void closedFm(){

        if (mRadioTuner != null){
            boolean isControl = mRadioTuner.hasControl();
            Log.d(TAG," closedFm: "+isControl);
            if (isControl){
                HashMap<String, String> map = new HashMap<String, String>();
                map.put(IS_OPEN_SESSION, String.valueOf(CLOSE_SESSION));
                mRadioTuner.setParameters(map);
                mRadioTuner.close();
            }else {
                Log.d(TAG,"fmServer mRadioTuner is closed");
            }
        }
    }


    public int getPowerStatus() {
        return mPowerStatus;
    }

    public void toggleFmDenoise() {
        if (mRadioTuner == null) {
            Log.d(TAG, "toggleFmDenoise: mRadioTuner == null return");
            return;
        }
        Log.d(TAG, "toggleFmDenoise: ");
        if (isEnableDenoise) {
            mAudioManager.setParameters("fm_denoise=disabled");
            isEnableDenoise = false;
        } else {
            mAudioManager.setParameters("fm_denoise=enable");
            isEnableDenoise = true;
        }
        updateFmDenoiseTitle();
    }

    public void updateFmDenoiseTitle() {
        Log.d(TAG, "updateFmDenoiseTitle: isEnableDenoise:"+isEnableDenoise);
        Bundle bundle = new Bundle(2);
        bundle.putInt(FmListener.CALLBACK_FLAG,
                FmListener.MSGID_DENOISE_CHANGED);
        bundle.putBoolean(FmListener.KEY_DENOISE_CHANGED, isEnableDenoise);
        notifyActivityStateChanged(bundle);
    }

    /**
     * Tune to a station
     *
     * @param frequency The frequency to tune
     * @return true, success; false, fail.
     */
    public void tuneStationAsync(float frequency, boolean direction) {
        Log.d(TAG, "tuneStationAsync() : frequency = " + frequency + ", direction = " + direction);
        mFmServiceHandler.removeMessages(FmListener.MSGID_TUNE_FINISHED);
        final int bundleSize = 1;
        Bundle bundle = new Bundle(bundleSize);
        bundle.putFloat(FM_FREQUENCY, frequency);
        bundle.putBoolean(DIRECTION, direction);
        Message msg = mFmServiceHandler.obtainMessage(FmListener.MSGID_TUNE_FINISHED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    private boolean tuneStation(boolean direction) {
        Log.d(TAG, "tuneStation(b) : mPowerStatus = " + mPowerStatus + ", direction = " + direction);
        if (mPowerStatus == POWER_UP) {
            NOW_MODE = TUNE_STATION;
            int step = -1;
            if (mRadioTuner != null) {
                synchronized (mLock) {
                    mSetFreqPending = true;

                    if (direction) {
                        step = mRadioTuner.step(RadioTuner.DIRECTION_UP, true);
                    } else {
                        step = mRadioTuner.step(RadioTuner.DIRECTION_DOWN, true);
                    }

                    Log.d(TAG, "tuneStation(b) : step = " + step + ", STATUS_OK = " + RadioManager.STATUS_OK);
                    if (step == RadioManager.STATUS_OK) {
                        try {
                            if (mSetFreqPending) {
                                mLock.wait(3000);
                            }
                            Log.d(TAG, "tuneStation(b) : mSetFreqPending = " + mSetFreqPending);
                            if (!mSetFreqPending) {
                                return true;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        NOW_MODE = "now_mode";
                        Log.w(TAG, "tuneStation(b) : step = " + step);
                        return false;
                    }
                }
            }

        }

        // if earphone is not insert, not power up
        if (!isAntennaAvailable()) {
            return false;
        }
        return false;
    }

    private boolean tuneStation(float frequency) {
        Log.d(TAG, "tuneStation() : mPowerStatus = " + mPowerStatus + ", frequency = " + frequency);
        if (mPowerStatus == POWER_UP) {
            NOW_MODE = TUNE_STATION;
            Log.d(TAG, "tuneStation(f) : frequency = " + frequency);
            int tune = -1;
            if (mRadioTuner != null) {
                if (frequency != 0) {
                    tune = mRadioTuner.tune(FmUtils.computeStation(frequency), 0);
                    playFrequency(frequency);
                }
                Log.d(TAG, "tuneStation(f) : tune = " + tune + ", STATUS_OK = " + RadioManager.STATUS_OK);
                if (tune == RadioManager.STATUS_OK) {
                    return true;
                } else {
                    NOW_MODE = "now_mode";
                    return false;
                }
            }

        }

        if (!isAntennaAvailable()) {
            return false;
        }
        return false;
    }

    /**
     * Seek station according frequency and direction
     *
     * @param frequency start frequency(100KHZ, 87.5)
     * @param isUp      direction(true, next station; false, previous station)
     * @return the frequency after seek
     */
    public void seekStationAsync(boolean direction) {
        Log.d(TAG,"seekStationAsync() : direction = " + direction);
        mFmServiceHandler.removeMessages(FmListener.MSGID_SEEK_FINISHED);
        final int bundleSize = 2;
        Bundle bundle = new Bundle(bundleSize);
        bundle.putBoolean(OPTION, direction);
        Message msg = mFmServiceHandler.obtainMessage(FmListener.MSGID_SEEK_FINISHED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    private boolean seekStation(boolean direction) {
        if (mPowerStatus != POWER_UP) {
            return false;
        }
        NOW_MODE = SEEK_STATION;

        if (mRadioTuner != null) {
            synchronized (mSearchLock) {
                mIsNativeSeeking = true;
                mIsStopScanCalled = false;
                int scan;
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("seek", "1");
                mRadioTuner.setParameters(map);
                if (direction) {
                    scan = mRadioTuner.scan(RadioTuner.DIRECTION_UP, true);
                } else {
                    scan = mRadioTuner.scan(RadioTuner.DIRECTION_DOWN, true);
                }

                if (scan == RadioManager.STATUS_OK) {
                    mIsNativeSeeking = false;
                    mSeekStationPending = true;
                    try {
                        mSearchLock.wait(20000);
                        if (!mSeekStationPending) {
                            Log.d(TAG, "seekStation has callback");
                        } else {
                            Log.d(TAG, "seekStation -> time out, no callback, cancel scan");
                            mRadioTuner.cancel();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }

    }

    public void setStepFrequency(int step) {
        mStep = step;
    }

    /**
     * Scan stations
     */
    public void startScanAsync() {
        mFmServiceHandler.removeMessages(FmListener.MSGID_SCAN_FINISHED);
        mFmServiceHandler.sendEmptyMessage(FmListener.MSGID_SCAN_FINISHED);
    }

    private int[] startScan() {
        Log.d(TAG, "startScan() : mIsStopScanCalled = " + mIsStopScanCalled);
        int[] stations = null;
        setMute(true);
        if (!mIsStopScanCalled) {
            mIsNativeScanning = true;
            stations = startFullScan();
            mIsNativeScanning = false;




            //wuhong test
            int[] newData = {88100, 93800, 96800, 98100, 101800, 104900};
            if(stations == null){
                stations = new int[0];
            }
            // 扩展数组
            stations = Arrays.copyOf(stations, stations.length + newData.length);
            // 添加新数据
            System.arraycopy(newData, 0, stations, stations.length - newData.length, newData.length);
            //wuhong test




        }
        if (mIsStopScanCalled) {
            // Received a message to power down FM, or interrupted by a phone
            // call. Do not return any stations. stationsInShort = null;
            // if cancel scan, return invalid station -100
            stations = new int[]{-100};
            mIsStopScanCalled = false;
        }

        return stations;
    }

    private int[] startFullScan() {
        Log.d(TAG, "startFullScan() : enter");
        NOW_MODE = START_FULL_STATION;
        int status = 0;
        if (mRadioTuner != null) {
            synchronized (mSearchLock) {
                HashMap<String, String> map = new HashMap<String, String>();
                if (NOW_MODE.equals(START_FULL_STATION)) {
                    map.put("seek", "2");
                    mRadioTuner.setParameters(map);
                } else {
                    map.put("seek", "1");
                }
                map.put("step", String.valueOf(mStep));
                Log.d(TAG,"startFullScan() : map = " + map.toString());
                mRadioTuner.setParameters(map);
                status = mRadioTuner.scan(RadioTuner.DIRECTION_UP, true);
                Log.d(TAG, "startFullScan() : status = " + status);
                if (status == RadioManager.STATUS_OK) {
                    mStartFullStationPending = true;
                    try {
                        if (mStartFullStationPending) {
                            Log.d(TAG, "startFullScan() : mSearchLock.wait before");
                            long waitTime = 30000;
                            if (mStep == 50) {
                                waitTime *= 2;
                            }
                            mSearchLock.wait(waitTime);
                            Log.d(TAG, "startFullScan() : mSearchLock.wait after");
                        }
                        Log.d(TAG, "startFullScan() : mStartFullStationPending = " + mStartFullStationPending
                            + ", mFreqList = " + mFreqList);
                        if (!mStartFullStationPending) {
                            //search result got from hardware
                        } else {
                            Log.d(TAG, "error startFullScan onSeekCompleteEvent didn't callback");
                            NOW_MODE = "now_mode";
                            mRadioTuner.cancel();
                        }
                    } catch (InterruptedException e) {
                        NOW_MODE = "now_mode";
                        Log.e(TAG, "Interrupted Exception in searchStation()");
                    }
                }
            }
        }
        Log.d(TAG, "startFullScan() : mFreqList = " + Arrays.toString(mFreqList));
        if (mFreqList == null || mFreqList.length == 0) {
            Log.d(TAG, "startFullScan() : new mFreqList");
            mFreqList = new int[]{-100};
        }
        NOW_MODE = "now_mode";
        return mFreqList;
    }

    public float searchStation(int direction) {
        int status;
        NOW_MODE = START_STATION;
        if (mRadioTuner != null) {
            synchronized (mSearchLock) {
                mValidStation = false;
                HashMap<String, String> map = new HashMap<String, String>();
                if (NOW_MODE.equals(START_FULL_STATION)) {
                    map.put("seek", "2");
                    mRadioTuner.setParameters(map);
                }else {
                    // scan single station
                    map.put("seek", "1");
                }
                mRadioTuner.setParameters(map);
                status = mRadioTuner.scan(direction, true);
                if (status == RadioManager.STATUS_OK) {
                    mSearchStationPending = true;
                    try {
                        if (mSearchStationPending) {
                            mSearchLock.wait(30000);
                        }
                        if (!mSearchStationPending) {
                            if (mValidStation) {
                                Log.d(TAG, "searchStation  freq = " + mSeekFreqClbk);
                                return mSeekFreqClbk;
                            } else {
                                return -mSeekFreqClbk;
                            }
                        } else {
                            Log.d(TAG, "error searchStation onSeekCompleteEvent didn't callback");
                            mRadioTuner.cancel();
                            return -mSeekFreqClbk;
                        }
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Interrupted Exception in searchStation()");
                    }
                }
            }
        }
        return -mSeekFreqClbk;
    }

    /**
     * Check FM Radio is in scan progress or not
     *
     * @return if in scan progress return true, otherwise return false.
     */
    public boolean isScanning() {
        return mIsScanning;
    }

    /**
     * Stop scan progress
     *
     * @return true if can stop scan, otherwise return false.
     */
    public boolean stopScan() {
        if (mPowerStatus != POWER_UP) {
            return false;
        }

        boolean bRet = false;
        mFmServiceHandler.removeMessages(FmListener.MSGID_SCAN_FINISHED);
        mFmServiceHandler.removeMessages(FmListener.MSGID_SEEK_FINISHED);
        if (mIsNativeScanning || mIsNativeSeeking) {
            mIsStopScanCalled = true;
            bRet = cancelSearch();
            Log.d(TAG, "stopScan result :" + bRet);
        }
        return bRet;
    }

    public boolean cancelSearch() {
        boolean result = false;
        int status = 1;
        if (mRadioTuner != null && mRadioTuner.hasControl()) {
            synchronized (mSearchLock) {
                Log.d(TAG, "cancelSearch ");
                status = mRadioTuner.cancel();
                isCancel = true;
                mSearchLock.notifyAll();
                if (status == RadioManager.STATUS_OK) {
                    mIsScanning = false;
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Check FM is in seek progress or not
     *
     * @return true if in seek progress, otherwise return false.
     */
    public boolean isSeeking() {
        return mIsNativeSeeking;
    }

    /**
     * Mute or unmute FM voice
     *
     * @param mute true for mute, false for unmute
     * @return (1, success ; other, failed)
     */
    public int setMute(boolean mute) {
        Log.d(TAG, "setMute() : mute = " + mute + ", mPowerStatus = " + mPowerStatus);
        if (mPowerStatus != POWER_UP) {
            Log.w(TAG, "setMute, FM is not powered up");
            setVolume(0);
            mAudioManager.setParameters("fm_route=disabled");
            if (isEnableDenoise) {
                mAudioManager.setParameters("fm_denoise=enable");
            } else {
                mAudioManager.setParameters("fm_denoise=disabled");
            }
            return -1;
        }
        int iRet = 0;
        /**
         * Original Android code:
         iRet = FmNative.setMute(mute);
         */
        Log.w(TAG, "setMute mute = " + mute);
        if (mute) {
            setVolume(0);
            mAudioManager.setParameters("fm_route=disabled");
        } else {
            if (mIsSpeakerUsed) {
                mAudioManager.setParameters("fm_route=fm_speaker");
            } else {
                mAudioManager.setParameters("fm_route=fm_headset");
            }
            if (isEnableDenoise) {
                mAudioManager.setParameters("fm_denoise=enable");
            } else {
                mAudioManager.setParameters("fm_denoise=disabled");
            }
            int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            setVolume(volume);
        }
        mIsMuted = mute;
        return iRet;
    }

    public void setVolume(int volume) {
        boolean isAntennaAvailable = isAntennaAvailable();
        Log.d(TAG, "setVolume FM_Volume = " + volume + ", isAntennaAvailable = " + isAntennaAvailable);
        //Do not set the volume if there is no earphone antenna
        if (isAntennaAvailable) {
            int convertToApVolume = convertToApVolume(volume);
            mAudioManager.setParameters("FM_VOLUME=" + convertToApVolume);
        }
    }

    private int convertToApVolume(int index){
        int volume = 0;
        float streamVolumeDb = 0L;
        if (mAudioManager != null) {
            int forceUse = AudioSystem.getForceUse(FOR_PROPRIETARY);
            if (forceUse == AudioSystem.FORCE_NONE) {
                streamVolumeDb = mAudioManager.getStreamVolumeDb(AudioManager.STREAM_MUSIC, index,
                        AudioDeviceInfo.TYPE_WIRED_HEADSET);
            }else if (forceUse == AudioSystem.FORCE_SPEAKER) {
                streamVolumeDb = mAudioManager.getStreamVolumeDb(AudioManager.STREAM_MUSIC, index,
                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
            }
            volume = (int) (DbToAmpl(streamVolumeDb) * 4096);
            Log.d(TAG, "convertToApVolume streamVolumeDb: " + streamVolumeDb + ", FmVolume: " + volume + ",forceUse: "+forceUse);
        }
        return volume;
    }

    private float DbToAmpl(float decibels)
    {
        final float VOLUME_MIN_DB = -758.0f;
        if (decibels <= VOLUME_MIN_DB) {
            return 0.0f;
        }
        return exp( decibels * 0.115129f); // exp( dB * ln(10) / 20 )
    }

    /**
     * Check the latest status is mute or not
     *
     * @return (true, mute ; false, unmute)
     */
    public boolean isMuted() {
        return mIsMuted;
    }

    /**
     * Check whether speaker used or not
     *
     * @return true if use speaker, otherwise return false
     */
    public boolean isSpeakerUsed() {
        return mIsSpeakerUsed;
    }

    /**
     * Initial service and current station
     *
     * @param iCurrentStation current station frequency
     */
    public void initService(int iCurrentStation) {
        mIsServiceInited = true;
        mCurrentStation = iCurrentStation;
        FmUtils.setIsServiceReleased(mContext, false);
    }

    /**
     * Check service is initialed or not
     *
     * @return true if initialed, otherwise return false
     */
    public boolean isServiceInited() {
        return mIsServiceInited;
    }

    /**
     * Get FM service current station frequency
     *
     * @return Current station frequency
     */
    public int getFrequency() {
        return mCurrentStation;
    }

    /**
     * Set FM service station frequency
     *
     * @param station Current station
     */
    public void setFrequency(int station) {
        mCurrentStation = station;
    }

    /**
     * resume FM audio
     */
    private void resumeFmAudio() {
        // If not check mIsAudioFocusHeld && power up, when scan canceled,
        // this will be resume first, then execute power down. it will cause
        // nosise.
        if (mIsAudioFocusHeld && (mPowerStatus == POWER_UP)) {
            enableFmAudio(true);
        }
    }

    /**
     * Switch antenna There are two types of antenna(long and short) If long
     * antenna(most is this type), must plug in earphone as antenna to receive
     * FM. If short antenna, means there is a short antenna if phone already,
     * can receive FM without earphone.
     *
     * @param antenna antenna (0, long antenna, 1 short antenna)
     * @return (0, success ; 1 failed ; 2 not support)
     */
    public void switchAntennaAsync(int antenna) {
        final int bundleSize = 1;
        mFmServiceHandler.removeMessages(FmListener.MSGID_SWITCH_ANTENNA);

        Bundle bundle = new Bundle(bundleSize);
        bundle.putInt(FmListener.SWITCH_ANTENNA_VALUE, antenna);
        Message msg = mFmServiceHandler.obtainMessage(FmListener.MSGID_SWITCH_ANTENNA);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    /**
     * Start recording
     */
    public void startRecordingAsync() {
        mFmServiceHandler.removeMessages(FmListener.MSGID_STARTRECORDING_FINISHED);
        mFmServiceHandler.sendEmptyMessage(FmListener.MSGID_STARTRECORDING_FINISHED);
    }

    private void startRecording() {
        sRecordingSdcard = FmUtils.getDefaultStoragePath();
        if (sRecordingSdcard == null || sRecordingSdcard.isEmpty()) {
            Log.d(TAG, "startRecording, may be no sdcard");
            onRecorderError(FmRecorder.ERROR_SDCARD_NOT_PRESENT);
            return;
        }

        if (mFmRecorder == null) {
            mFmRecorder = new FmRecorder();
            mFmRecorder.registerRecorderStateListener(FmService.this);
        }

        if (isSdcardReady(sRecordingSdcard)) {
            mFmRecorder.startRecording(mContext);
        } else {
            onRecorderError(FmRecorder.ERROR_SDCARD_NOT_PRESENT);
        }
    }

    private boolean isSdcardReady(String sdcardPath) {
        if (!mSdcardStateMap.isEmpty()) {
            if (mSdcardStateMap.get(sdcardPath) != null && !mSdcardStateMap.get(sdcardPath)) {
                Log.d(TAG, "isSdcardReady, return false");
                return false;
            }
        }
        return true;
    }

    /**
     * stop recording
     */
    public void stopRecordingAsync() {
        mFmServiceHandler.removeMessages(FmListener.MSGID_STOPRECORDING_FINISHED);
        mFmServiceHandler.sendEmptyMessage(FmListener.MSGID_STOPRECORDING_FINISHED);
    }

    private boolean stopRecording() {
        if (mFmRecorder == null) {
            Log.e(TAG, "stopRecording, called without a valid recorder!!");
            return false;
        }
        synchronized (mStopRecordingLock) {
            mFmRecorder.stopRecording();
        }
        return true;
    }

    /**
     * Save recording file according name or discard recording file if name is
     * null
     *
     * @param newName New recording file name
     */
    public void saveRecordingAsync(String newName) {
        mFmServiceHandler.removeMessages(FmListener.MSGID_SAVERECORDING_FINISHED);
        final int bundleSize = 1;
        Bundle bundle = new Bundle(bundleSize);
        bundle.putString(RECODING_FILE_NAME, newName);
        Message msg = mFmServiceHandler.obtainMessage(FmListener.MSGID_SAVERECORDING_FINISHED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    private void saveRecording(String newName) {
        if (mFmRecorder != null) {
            if (newName != null) {
                mFmRecorder.saveRecording(FmService.this, newName);
                return;
            }
            mFmRecorder.discardRecording();
        }
    }

    /**
     * Get record time
     *
     * @return Record time
     */
    public long getRecordTime() {
        if (mFmRecorder != null) {
            return mFmRecorder.getRecordTime();
        }
        return 0;
    }


    private void setRecordingMode(boolean isRecording) {
        if (mFmRecorder != null) {
            if (!isRecording) {
                if (mFmRecorder.getState() != FmRecorder.STATE_IDLE) {
                    mFmRecorder.stopRecording();
                }
                resumeFmAudio();
                setMute(false);
                return;
            }
            // reset recorder to unused status
            mFmRecorder.resetRecorder();
        }
    }

    /**
     * Get record state
     *
     * @return record state
     */
    public int getRecorderState() {
        if (null != mFmRecorder) {
            return mFmRecorder.getState();
        }
        return FmRecorder.STATE_INVALID;
    }

    /**
     * Get recording file name
     *
     * @return recording file name
     */
    public String getRecordingName() {
        if (null != mFmRecorder) {
            return mFmRecorder.getRecordFileName();
        }
        return null;
    }
    private Handler fmRadioHanndler;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        Log.d(TAG, "Create：" + FmUtils.isOpenFm(mContext));
        HandlerThread handlerTuner = new HandlerThread("radiotunrcallback");
        handlerTuner.start();
        fmRadioHanndler = new FmRadioServiceHandler(handlerTuner.getLooper());
        initRadioManager();
        if (FmUtils.isOpenFm(mContext)) {
            if (mRadioTuner != null && mRadioTuner.hasControl()) {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put(IS_OPEN_SESSION, String.valueOf(OPEN_SESSION));
                mRadioTuner.setParameters(map);
            }
        }
        sRecordingSdcard = FmUtils.getDefaultStoragePath();
        //ATC {
        //if (SystemProperties.getBoolean("ro.atc.aosp_enhancement", false)) {
            regiestPowerOnOffReceiver();
        //}
        //ATC }
        initAudioRecordSink();
        createRenderThread();
        registerFmBroadcastReceiver();
        registerSdcardReceiver();
        HandlerThread handlerThread = new HandlerThread("FmRadioServiceThread");
        handlerThread.start();
        mFmServiceHandler = new FmRadioServiceHandler(handlerThread.getLooper());
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mIsSpeakerUsed = FmUtils.isSpeakerPhoneOn(mContext);
        Log.d(TAG, "onCreate() : mIsSpeakerUsed = " + mIsSpeakerUsed);
        if (isAntennaAvailable()){
            setForceUse(mIsSpeakerUsed);
        }
        registerAudioPortUpdateListener();
        showPlayingNotification();
    }

    private void initRadioManager() {
        mModules = new ArrayList<>();
        if (mRadioManager == null) {
            mRadioManager = (RadioManager) mContext.getSystemService(Context.RADIO_SERVICE);
        }
        int status = mRadioManager.listModules(mModules);
        if (status != RadioManager.STATUS_OK) {
            Log.w(TAG, "Couldn't get radio module list: " + status);
            return;
        }
        if (mModules.size() == 0) {
            Log.i(TAG, "No radio modules on this device");
            return;
        }
        RadioManager.ModuleProperties module = mModules.get(0);
        mRadioTuner = mRadioManager.openTuner(module.getId(),
                null,
                true,
                mHardwareCallback,
                fmRadioHanndler);
        Log.i(TAG, "mRadioTuner Created successfully");
    }

    private void registerAudioPortUpdateListener() {
        if (mAudioPortUpdateListener == null) {
            mAudioPortUpdateListener = new FmOnAudioPortUpdateListener();
            mAudioManager.registerAudioPortUpdateListener(mAudioPortUpdateListener);
        }
    }

    private void unregisterAudioPortUpdateListener() {
        if (mAudioPortUpdateListener != null) {
            mAudioManager.unregisterAudioPortUpdateListener(mAudioPortUpdateListener);
            mAudioPortUpdateListener = null;
        }
    }

    private synchronized int createAudioPatch() {
        Log.d(TAG, "createAudioPatch");
        int status = AudioManager.SUCCESS;
        if (mAudioPatch != null) {
            Log.d(TAG, "createAudioPatch, mAudioPatch is not null, return");
            return status;
        }
        mAudioPatch = new Object();
        return status;
    }

    private FmOnAudioPortUpdateListener mAudioPortUpdateListener = null;

    private class FmOnAudioPortUpdateListener implements OnAudioPortUpdateListener {
        /**
         * Callback method called upon audio port list update.
         *
         * @param portList the updated list of audio ports
         */
        @Override
        public void onAudioPortListUpdate(AudioPort[] portList) {
            // Ingore audio port update
        }

        /**
         * Callback method called upon audio patch list update.
         *
         * @param patchList the updated list of audio patches
         */
        @Override
        public void onAudioPatchListUpdate(AudioPatch[] patchList) {

        }

        /**
         * Callback method called when the mediaserver dies
         */
        @Override
        public void onServiceDied() {
            enableFmAudio(false);
        }
    }

    private synchronized void releaseAudioPatch() {
        if (mAudioPatch != null) {
            Log.d(TAG, "releaseAudioPatch");
            mAudioPatch = null;
        }
        mAudioSource = null;
        mAudioSink = null;
    }

    private void registerFmBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SOUND_POWER_DOWN_MSG);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
        filter.addAction(AudioManager.STREAM_MUTE_CHANGED_ACTION);
        mBroadcastReceiver = new FmServiceBroadcastReceiver();
        registerReceiver(mBroadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void unregisterFmBroadcastReceiver() {
        if (null != mBroadcastReceiver) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy...");
        stopAudioFunc();
        unregisterFmBroadcastReceiver();
        unregisterSdcardListener();
        exitFm();
        if (null != mFmRecorder) {
            mFmRecorder = null;
        }
        //ATC {
        //if (SystemProperties.getBoolean("ro.atc.aosp_enhancement", false)) {
            unregiestPowerOnOffReceiver();
        //}
        //ATC }
        releaseAudioPatch();
//        unregisterAudioPortUpdateListener();
        closedFm();
        FmUtils.setIsServiceReleased(mContext, true);
        super.onDestroy();
    }

    /**
     * Exit FMRadio application
     */
    private void exitFm() {
        mIsAudioFocusHeld = false;
        // Stop FM recorder if it is working
        if (null != mFmRecorder) {
            synchronized (mStopRecordingLock) {
                int fmState = mFmRecorder.getState();
                if (FmRecorder.STATE_RECORDING == fmState) {
                    mFmRecorder.stopRecording();
                }
            }
        }

        // When exit, we set the audio path back to earphone.
        if (mIsNativeScanning || mIsNativeSeeking) {
            stopScan();
        }

        mFmServiceHandler.removeCallbacksAndMessages(null);

//        if (mIsSpeakerUsed) {
//            setForceUse(false);
//        }
        powerDown();

        Bundle bundle = new Bundle(1);
        bundle.putInt(FmListener.CALLBACK_FLAG, FmListener.MSGID_FM_EXIT);
        notifyActivityStateChanged(bundle);
        // Finish favorite when exit FM
        if (sExitListener != null) {
            sExitListener.onExit();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Change the notification string.
        if (mPowerStatus == POWER_UP) {
            showPlayingNotification();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            String action = intent.getAction();
            if (FM_SEEK_PREVIOUS.equals(action)) {
                seekStationAsync(false);
            } else if (FM_SEEK_NEXT.equals(action)) {
                seekStationAsync(true);
            } else if (FM_TURN_OFF.equals(action)) {
                powerDownAsync();
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * Set PS information
     *
     * @param ps The ps information
     */
    private void setPs(String ps) {
        if (0 != mPsString.compareTo(ps)) {
            mPsString = ps;
            Bundle bundle = new Bundle(3);
            bundle.putInt(FmListener.CALLBACK_FLAG, FmListener.LISTEN_PS_CHANGED);
            bundle.putString(FmListener.KEY_PS_INFO, mPsString);
            notifyActivityStateChanged(bundle);
        } // else New PS is the same as current
    }

    /**
     * Set RT information
     *
     * @param lrtText The RT information
     */
    private void setLRText(String lrtText) {
        if (0 != mRtTextString.compareTo(lrtText)) {
            mRtTextString = lrtText;
            Bundle bundle = new Bundle(3);
            bundle.putInt(FmListener.CALLBACK_FLAG, FmListener.LISTEN_RT_CHANGED);
            bundle.putString(FmListener.KEY_RT_INFO, mRtTextString);
            notifyActivityStateChanged(bundle);
        } // else New RT is the same as current
    }

    private synchronized void startRender() {
        Log.d(TAG, "startRender " + AudioSystem.getForceUse(FOR_PROPRIETARY));

        // need to create new audio record and audio play back track,
        // because input/output device may be changed.
        if (mAudioRecord != null) {
            synchronized (mAudioRecord) {
                if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    mAudioRecord.stop();
                }
                mAudioRecord.release();
                mAudioRecord = null;
           }
        }
        if (mAudioTrack != null) {
            stopAudioTrack();
        }

        if (initAudioRecordSink()) {
            mIsRender = true;
            synchronized (mRenderLock) {
                Log.i(TAG, "startRender: notifying for mRenderLock");
                mRenderLock.notify();
            }
        } else {
            Log.i(TAG, "initAudioRecordSink: fail");
        }
    }

    private synchronized void stopRender() {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "stopRender");
        }
        synchronized (mRenderingLock) {
        Log.i(TAG, "stopRender_processing, mIsRender = " + isRender());
            boolean localRender = isRender();
            mIsRender = false;
            if (localRender) {
                try {
                         long wait = 200;
                         Log.i(TAG, "stopRender: waiting for mRenderingLock");
                         mRenderingLock.wait(wait);
                } catch (InterruptedException e) {
                     Log.w(TAG, "stopRender, thread is interrupted");
                 }
                }
        }
    }

    private synchronized void createRenderThread() {
            Log.d(TAG, "createRenderThread");
        if (mRenderThread == null) {
            mRenderThread = new RenderThread();
            mRenderThread.start();
        }
    }

    private synchronized void exitRenderThread() {
            Log.d(TAG, "exitRenderThread");
        stopRender();
        mRenderThread.interrupt();
        mRenderThread = null;
    }

    private Thread mRenderThread = null;
    private AudioRecord mAudioRecord = null;
    private AudioTrack mAudioTrack = null;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORD_BUF_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            CHANNEL_CONFIG, AUDIO_FORMAT);
    private boolean mIsRender = false;

    /**
     * Open or close FM Radio audio
     *
     * @param enable true, open FM audio; false, close FM audio;
     */
    private synchronized void enableFmAudio(boolean enable) {
        if (enable) {
            if ((mPowerStatus != POWER_UP) || !mIsAudioFocusHeld) {
                Log.d(TAG, "enableFmAudio, current not available return.mIsAudioFocusHeld:"
                        + mIsAudioFocusHeld);
                mPowerStatus = POWER_DOWN;
                stopRender();
                stopAudioTrack();
                return;
            }
            startAudioTrack();
        } else {
            stopRender();
            stopAudioTrack();
        }
    }

    private synchronized boolean initAudioRecordSink() {

        // free if any previous audiorecord or audiotrack are present,
        if (mAudioRecord != null) {
            synchronized (mAudioRecord) {
                if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    mAudioRecord.stop();
                }
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }
        stopAudioTrack();

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.RADIO_TUNER,
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, RECORD_BUF_SIZE);
        if (mAudioRecord == null) {
            return false;
        }
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, RECORD_BUF_SIZE, AudioTrack.MODE_STREAM);
        Log.d(TAG, "initAudioRecordSink, mAudioRecord = " + mAudioRecord + ",mAudioTrack = "
              + mAudioTrack);

        if (mAudioTrack == null) {
            if (mAudioRecord != null) {
                mAudioRecord.release();
                mAudioRecord = null;
            }
            return false;
        }
        return true;
    }

    private void startAudioTrack() {
        Log.d(TAG, "startAudioTrack, mAudioTrack = " + mAudioTrack);
        if (mAudioTrack == null) {
            return;
        }
        synchronized (mAudioTrack) {
            if (mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED) {
                ArrayList<AudioPatch> patches = new ArrayList<AudioPatch>();
                mAudioManager.listAudioPatches(patches);
                mAudioTrack.play();
            }
        }
    }

    private void stopAudioTrack() {
        Log.d(TAG, "stopAudioTrack, mAudioTrack = " + mAudioTrack);
        if (mAudioTrack == null) {
            return;
        }
        try {
                synchronized (mAudioTrack) {
                    if (mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        mAudioTrack.stop();
                    }
                    mAudioTrack.release();
                    mAudioTrack = null;
                }
            } catch (IllegalStateException e) {
                Log.d(TAG, "stopAudioTrack, IllegalStateException");
            } catch (NullPointerException e) {
                Log.d(TAG, "stopAudioTrack, NullPointerException");
            }
    }

    class RenderThread extends Thread {
        private int mCurrentFrame = 0;
        private boolean isAudioFrameNeedIgnore() {
            return mCurrentFrame < AUDIO_FRAMES_TO_IGNORE_COUNT;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[RECORD_BUF_SIZE];
                Log.e(TAG, "RenderThread, interrupted = " + Thread.interrupted());
                while (!Thread.interrupted()) {
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(TAG, "RenderThread: run, isRender = " + isRender());
                    }
                    if (isRender()) {
                        // Speaker mode or BT a2dp mode will come here and keep reading and writing.
                        // If we want FM sound output from speaker or BT a2dp, we must record data
                        // to AudioRecrd and write data to AudioTrack.

                        if (mAudioRecord != null ) {
                            synchronized (mAudioRecord) {
                                if (mAudioRecord != null
                                    && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED
                                    && mAudioRecord.getRecordingState() ==
                                        AudioRecord.RECORDSTATE_STOPPED) {
                                    mAudioRecord.startRecording();
                                }
                            }
                        }
                        if (mAudioTrack != null ) {
                            synchronized (mAudioTrack) {
                                if (mAudioTrack != null
                                    && mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED
                                    && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED) {
                                    mAudioTrack.play();
                                }
                            }
                        }
                        int size = 0;
                        if (mAudioRecord != null) {
                            size = mAudioRecord.read(buffer, 0, RECORD_BUF_SIZE);
                        }
                        // check whether need to ignore first 3 frames audio data from AudioRecord
                        // to avoid pop noise.
                        if (isAudioFrameNeedIgnore()) {
                            mCurrentFrame += 1;
                            synchronized (mRenderingLock) {
                                Log.i(TAG, "RenderThread: notifying for mRenderingLock");
                                mRenderingLock.notify();
                            }
                            continue ;
                        }
                        if (size <= 0) {
                            Log.e(TAG, "RenderThread read data from AudioRecord "
                                    + "error size: " + size);
                            synchronized (mRenderingLock) {
                                Log.i(TAG, "RenderThread: notifying for mRenderingLock");
                                mRenderingLock.notify();
                            }
                            continue;
                        }
                        byte[] tmpBuf = new byte[size];
                        System.arraycopy(buffer, 0, tmpBuf, 0, size);
                        // Check again to avoid noises, because mIsRender may be changed
                        // while AudioRecord is reading.
                        if (isRender()) {
                            if (mAudioTrack != null )
                                mAudioTrack.write(tmpBuf, 0, tmpBuf.length);
                        }
                        synchronized (mRenderingLock) {
                            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                                Log.v(TAG, "RenderThread: notifying for mRenderingLock");
                            }
                            mRenderingLock.notify();
                        }
                    } else {
                        // Earphone mode will come here and wait.
                        mCurrentFrame = 0;
                        try {
                            if (mAudioTrack != null ) {
                                synchronized (mAudioTrack) {
                                    if (mAudioTrack != null &&
                                        mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING){
                                        mAudioTrack.stop();
                                    }
                                }
                            }

                            if (mAudioRecord != null &&
                                mAudioRecord.getRecordingState() ==
                                AudioRecord.RECORDSTATE_RECORDING) {
                                mAudioRecord.stop();
                            }
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "RenderThread.run, IllegalStateException");
                        } finally {
                            synchronized (mRenderLock) {
                                Log.i(TAG, "RenderThread: waiting for mRenderLock");
                                mRenderLock.wait();
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "RenderThread.run, thread is interrupted, need exit thread");
            } finally {
                if (mAudioRecord != null &&
                    (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)) {
                    mAudioRecord.stop();
                }

                if (mAudioTrack != null &&
                    (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)) {
                    mAudioTrack.stop();
                }
            }
        }
    }

    // A2dp or speaker mode should render
    public boolean isRender() {
        return (mIsRender && (mPowerStatus == POWER_UP) && mIsAudioFocusHeld);
    }

    // Make sure patches count will not be 0
    private boolean isPatchMixerToEarphone(ArrayList<AudioPatch> patches) {
        int deviceCount = 0;
        int deviceEarphoneCount = 0;
        for (AudioPatch patch : patches) {
            AudioPortConfig[] sources = patch.sources();
            AudioPortConfig[] sinks = patch.sinks();
            AudioPortConfig sourceConfig = sources[0];
            AudioPortConfig sinkConfig = sinks[0];
            AudioPort sourcePort = sourceConfig.port();
            AudioPort sinkPort = sinkConfig.port();
            Log.d(TAG, "isPatchMixerToEarphone " + sourcePort + " ====> " + sinkPort);
            if (sourcePort instanceof AudioMixPort && sinkPort instanceof AudioDevicePort) {
                deviceCount++;
                int type = ((AudioDevicePort) sinkPort).type();
                if (type == AudioSystem.DEVICE_OUT_WIRED_HEADSET ||
                        type == AudioSystem.DEVICE_OUT_WIRED_HEADPHONE) {
                    deviceEarphoneCount++;
                }
            }
        }
        if (deviceEarphoneCount == 1 && deviceCount == deviceEarphoneCount) {
            return true;
        }
        return false;
    }

    // Check whether the patch (mixer -> device) is removed by native.
    // If no patch (mixer -> device), return true.
    private boolean isPatchMixerToDeviceRemoved(ArrayList<AudioPatch> patches) {
        boolean noMixerToDevice = true;
        for (AudioPatch patch : patches) {
            AudioPortConfig[] sources = patch.sources();
            AudioPortConfig[] sinks = patch.sinks();
            AudioPortConfig sourceConfig = sources[0];
            AudioPortConfig sinkConfig = sinks[0];
            AudioPort sourcePort = sourceConfig.port();
            AudioPort sinkPort = sinkConfig.port();

            if (sourcePort instanceof AudioMixPort && sinkPort instanceof AudioDevicePort) {
                noMixerToDevice = false;
                break;
            }
        }
        return noMixerToDevice;
    }

    /**
     * Show notification
     */
    private void showPlayingNotification() {

        //wuhong 注释掉
//        String stationName = "";
//        String radioText = "";
//        String CHANNEL_ONE_ID = "com.asr.fmradio";
//        ContentResolver resolver = mContext.getContentResolver();
//        Cursor cursor = null;
//        try {
//            cursor = resolver.query(
//                    Station.CONTENT_URI,
//                    FmStation.COLUMNS,
//                    Station.FREQUENCY + "=?",
//                    new String[]{String.valueOf(mCurrentStation)},
//                    null);
//            if (cursor != null && cursor.moveToFirst()) {
//                // If the station name is not exist, show program service(PS) instead
//                stationName = cursor.getString(cursor.getColumnIndex(Station.STATION_NAME));
//                if (TextUtils.isEmpty(stationName)) {
//                    stationName = cursor.getString(cursor.getColumnIndex(Station.PROGRAM_SERVICE));
//                }
//                radioText = cursor.getString(cursor.getColumnIndex(Station.RADIO_TEXT));
//            }
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//
//        Intent aIntent = new Intent(Intent.ACTION_MAIN);
//        aIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//        aIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        aIntent.setClassName(getPackageName(), mTargetClassName);
//        PendingIntent pAIntent = PendingIntent.getActivity(mContext, 0, aIntent, PendingIntent.FLAG_IMMUTABLE);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
//
//            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
//                    "FmRadio", NotificationManager.IMPORTANCE_DEFAULT);
//            notificationChannel.setSound(null, null);
//            notificationManager.createNotificationChannel(notificationChannel);
//        }
//        synchronized (mNotificationLock) {
//            mNotificationBuilder = new Notification.Builder(mContext);
//            mNotificationBuilder.setSmallIcon(R.drawable.ic_launcher);
//            mNotificationBuilder.setShowWhen(false);
//            mNotificationBuilder.setAutoCancel(true);
//            Intent intent = new Intent(FM_SEEK_PREVIOUS);
//            intent.setClass(mContext, FmService.class);
//            PendingIntent pIntent = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
//            String title = getString(R.string.fm_prevstation_title);
//            mNotificationBuilder.addAction(R.drawable.btn_fm_prevstation, title, pIntent);
//            intent = new Intent(FM_TURN_OFF);
//            intent.setClass(mContext, FmService.class);
//            pIntent = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
//            title = getString(R.string.fm_stopplay_title);
//            mNotificationBuilder.addAction(R.drawable.btn_fm_rec_stop_enabled, title, pIntent);
//            intent = new Intent(FM_SEEK_NEXT);
//            intent.setClass(mContext, FmService.class);
//            pIntent = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
//            title = getString(R.string.fm_nextstation_title);
//            mNotificationBuilder.addAction(R.drawable.btn_fm_nextstation, title, pIntent);
//
//            mNotificationBuilder.setContentIntent(pAIntent);
//            Bitmap largeIcon = FmUtils.createNotificationLargeIcon(mContext,
//                    FmUtils.formatStation(mCurrentStation));
//            mNotificationBuilder.setLargeIcon(largeIcon);
//            // Show FM Radio if empty
//            if (TextUtils.isEmpty(stationName)) {
//                stationName = getString(R.string.app_name);
//            }
//            mNotificationBuilder.setContentTitle(stationName);
//            // If radio text is "" or null, we also need to update notification.
//            mNotificationBuilder.setContentText(radioText);
//            Log.d(TAG, "showPlayingNotification PS:" + stationName);
//
//            Notification n = mNotificationBuilder
//                    .setChannelId(CHANNEL_ONE_ID)
//                    .build();
//            notificationManager.notify(NOTIFICATION_ID, n);
//            n.flags &= ~Notification.FLAG_NO_CLEAR;
//            startForeground(NOTIFICATION_ID, n);
//
//            if (isActivityForeground() || mIsScanning || mPowerStatus != POWER_UP
//                    || (getRecorderState() == FmRecorder.STATE_RECORDING)) {
//                Log.w(TAG, "showPlayingNotification, do not show main notification.");
//                removeNotification();
//            }
//        }
    }

    public void updateRecordingNotification(long recordTime) {
        //wuhong 注释掉
//        Log.d(TAG, "service updateRecordingNotification ");
//        synchronized (mNotificationLock) {
//            if (this.mNotificationBuilder == null) {
//                Log.d(TAG, "service updateRecordingNotification .......");
//                Intent intent = new Intent(FM_STOP_RECORDING);
//                intent.setClass(this.mContext, FmRecordActivity.class);
//                intent.setFlags(intentFlag);
//                PendingIntent pendingIntent = PendingIntent.getActivity(this.mContext, 0, intent, pdFlag);
//                Bitmap largeIcon = FmUtils.createNotificationLargeIcon(this.mContext, FmUtils.formatStation(this.mCurrentStation));
//                this.mNotificationBuilder = new Notification.Builder(this, channelId)
//                        .setContentText(getText(R.string.record_notification_message))
//                        .setShowWhen(false).setAutoCancel(true).setSmallIcon(R.drawable.ic_launcher)
//                        .setLargeIcon(largeIcon)
//                        .addAction(R.drawable.ic_launcher, getText(R.string.stop_record), pendingIntent);
//                Intent cIntent = new Intent(FM_ENTER_RECORD_SCREEN);
//                cIntent.setClass(this.mContext, FmRecordActivity.class);
//                cIntent.setFlags(intentFlag);
//                PendingIntent contentPendingIntent = PendingIntent.getActivity(this.mContext, 0, cIntent, pdFlag);
//                this.mNotificationBuilder.setContentIntent(contentPendingIntent);
//            }
//            String time = FmUtils.timeFormat(recordTime);
//            this.mNotificationBuilder.setContentTitle(time);
//            showRecordingNotification(this.mNotificationBuilder.build());
//        }
    }

    /**
     * Show notification
     */
    public void showRecordingNotification(Notification notification) {
         startForeground(NOTIFICATION_ID, notification);
    }

    /**
     * Remove notification
     */
    public void removeNotification() {
        //ATC { if qb off or audio focus lost, don't go background.
        if (mQbPowerOff || !mIsAudioFocusHeld)
            return;
                /*&& SystemProperties.getBoolean("ro.atc.aosp_enhancement", false))
            return;*/
        //ATC }
        synchronized (mNotificationLock) {
            Log.d(TAG, "removeNotification ...");
            stopForeground(true);
            if(mNotificationBuilder != null) {
                mNotificationBuilder = null;
            }
        }
    }

    /**
     * Update notification
     */
    public void updatePlayingNotification() {
        if (mPowerStatus == POWER_UP) {
            Log.d(TAG," updatePlayingNotification:");
            showPlayingNotification();
        }
    }

    /**
     * Register sdcard listener for record
     */
    private void registerSdcardReceiver() {
        if (mSdcardListener == null) {
            mSdcardListener = new SdcardListener();
        }
        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("file");
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        registerReceiver(mSdcardListener, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void unregisterSdcardListener() {
        if (null != mSdcardListener) {
            unregisterReceiver(mSdcardListener);
        }
    }

    private void updateSdcardStateMap(Intent intent) {
        String action = intent.getAction();
        String sdcardPath = null;
        Uri mountPointUri = intent.getData();
        if (mountPointUri != null) {
            sdcardPath = mountPointUri.getPath();
            if (sdcardPath != null) {
                if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                    mSdcardStateMap.put(sdcardPath, false);
                } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
                    mSdcardStateMap.put(sdcardPath, false);
                } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                    mSdcardStateMap.put(sdcardPath, true);
                }
            }
        }
    }

    /**
     * Notify FM recorder state
     *
     * @param state The current FM recorder state
     */
    @Override
    public void onRecorderStateChanged(int state) {
        mRecordState = state;
        Bundle bundle = new Bundle(2);
        bundle.putInt(FmListener.CALLBACK_FLAG, FmListener.LISTEN_RECORDSTATE_CHANGED);
        bundle.putInt(FmListener.KEY_RECORDING_STATE, state);
        notifyActivityStateChanged(bundle);
    }

    /**
     * Notify FM recorder error message
     *
     * @param error The recorder error type
     */
    @Override
    public void onRecorderError(int error) {
        // if media server die, will not enable FM audio, and convert to
        // ERROR_PLAYER_INATERNAL, call back to activity showing toast.
        mRecorderErrorType = error;

        Bundle bundle = new Bundle(2);
        bundle.putInt(FmListener.CALLBACK_FLAG, FmListener.LISTEN_RECORDERROR);
        bundle.putInt(FmListener.KEY_RECORDING_ERROR_TYPE, mRecorderErrorType);
        notifyActivityStateChanged(bundle);
    }

    /**
     * Check and go next(play or show tips) after recorder file play
     * back finish.
     * Two cases:
     * 1. With headset  -> play FM
     * 2. Without headset -> show plug in earphone tips
     */
    private void checkState() {
        if (isHeadSetIn()) {
            // with headset
            if (mPowerStatus == POWER_UP) {
                resumeFmAudio();
                setMute(false);
            } else {
                powerUpAsync(FmUtils.computeFrequency(mCurrentStation));
            }
        } else {
            // without headset need show plug in earphone tips
            switchAntennaAsync(mValueHeadSetPlug);
        }
    }

    /**
     * Check the headset is plug in or plug out
     *
     * @return true for plug in; false for plug out
     */
    private boolean isHeadSetIn() {
        return (0 == mValueHeadSetPlug);
    }

    private void focusChanged(int focusState) {
        mIsAudioFocusHeld = false;
        if (mIsNativeScanning || mIsNativeSeeking) {
            // make stop scan from activity call to service.
            // notifyActivityStateChanged(FMRadioListener.LISTEN_SCAN_CANCELED);
            stopScan();
        }

        // using handler thread to update audio focus state
        updateAudioFocusAync(focusState);
    }

    /**
     * Request audio focus
     *
     * @return true, success; false, fail;
     */
    public boolean requestAudioFocus() {
        Log.d(TAG, "requestAudioFocus() : mIsAudioFocusHeld = " + mIsAudioFocusHeld);
        if (mIsAudioFocusHeld) {
            return true;
        }

        int audioFocus = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        Log.d(TAG, "requestAudioFocus() : audioFocus = " + audioFocus);
        mIsAudioFocusHeld = (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioFocus);

        if (FmUtils.getIsSpeakerModeOnFocusLost(mContext)) {
            setForceUse(true);
            FmUtils.setIsSpeakerModeOnFocusLost(mContext, false);
        }
        return mIsAudioFocusHeld;
    }

    /**
     * Abandon audio focus
     */
    public void abandonAudioFocus() {
        stopAudioFunc();
        mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
        mIsAudioFocusHeld = false;
        cancelSearch();
    }

    private void stopAudioFunc(){
        mAudioManager.setParameters("AudioFmPreStop=1");
        setMute(true);
        forceToHeadsetMode();
    }
    /**
     * Use to interact with other voice related app
     */
    private final OnAudioFocusChangeListener mAudioFocusChangeListener =
            new OnAudioFocusChangeListener() {
                /**
                 * Handle audio focus change ensure message FIFO
                 *
                 * @param focusChange audio focus change state
                 */
                @Override
                public void onAudioFocusChange(int focusChange) {
                    Log.d(TAG, "onAudioFocusChange " + focusChange);
                    //ATC {
                    if (mQbPowerOff) {
                        return;
                    }
                    //ATC }
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            synchronized (this) {
                                stopAudioFunc();
                                focusChanged(AudioManager.AUDIOFOCUS_LOSS);
                            }
                            break;

                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            synchronized (this) {
                                stopAudioFunc();
                                focusChanged(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);
                            }
                            break;

                        case AudioManager.AUDIOFOCUS_GAIN:
                            synchronized (this) {
                                updateAudioFocusAync(AudioManager.AUDIOFOCUS_GAIN);
                            }
                            break;

                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            synchronized (this) {
                                updateAudioFocusAync(
                                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
                            }
                            break;

                        default:
                            break;
                    }
                }
            };

    /**
     * Audio focus changed, will send message to handler thread. synchronized to
     * ensure one message can go in this method.
     *
     * @param focusState AudioManager state
     */
    private synchronized void updateAudioFocusAync(int focusState) {
        final int bundleSize = 1;
        Bundle bundle = new Bundle(bundleSize);
        bundle.putInt(FmListener.KEY_AUDIOFOCUS_CHANGED, focusState);
        Message msg = mFmServiceHandler.obtainMessage(FmListener.MSGID_AUDIOFOCUS_CHANGED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    /**
     * Audio focus changed, update FM focus state.
     *
     * @param focusState AudioManager state
     */
    private void updateAudioFocus(int focusState) {
        Log.d(TAG, "updateAudioFocus() : focusState = " + focusState);
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_LOSS:
                mPausedByTransientLossOfFocus = false;
                // play back audio will output with music audio
                // May be affect other recorder app, but the flow can not be
                // execute earlier,
                // It should ensure execute after start/stop record.
                if (mFmRecorder != null) {
                    int fmState = mFmRecorder.getState();
                    // only handle recorder state, not handle playback state
                    if (fmState == FmRecorder.STATE_RECORDING) {
                        mFmServiceHandler.removeMessages(
                                FmListener.MSGID_STARTRECORDING_FINISHED);
                        mFmServiceHandler.removeMessages(
                                FmListener.MSGID_STOPRECORDING_FINISHED);
                        stopRecording();
                    }
                }
                handlePowerDown();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mPowerStatus == POWER_UP) {
                    mPausedByTransientLossOfFocus = true;
                }
                // play back audio will output with music audio
                // May be affect other recorder app, but the flow can not be
                // execute earlier,
                // It should ensure execute after start/stop record.
                if (mFmRecorder != null) {
                    int fmState = mFmRecorder.getState();
                    if (fmState == FmRecorder.STATE_RECORDING) {
                        mFmServiceHandler.removeMessages(
                                FmListener.MSGID_STARTRECORDING_FINISHED);
                        mFmServiceHandler.removeMessages(
                                FmListener.MSGID_STOPRECORDING_FINISHED);
                        stopRecording();
                    }
                }
                handlePowerDown();
                break;

            case AudioManager.AUDIOFOCUS_GAIN:
                if (FmUtils.getIsSpeakerModeOnFocusLost(mContext)) {
                    setForceUse(true);
                    FmUtils.setIsSpeakerModeOnFocusLost(mContext, false);
                }
                if ((mPowerStatus != POWER_UP) && mPausedByTransientLossOfFocus) {
                    final int bundleSize = 1;
                    mFmServiceHandler.removeMessages(FmListener.MSGID_POWERUP_FINISHED);
                    mFmServiceHandler.removeMessages(FmListener.MSGID_POWERDOWN_FINISHED);
                    Bundle bundle = new Bundle(bundleSize);
                    bundle.putFloat(FM_FREQUENCY, FmUtils.computeFrequency(mCurrentStation));
                    handlePowerUp(bundle);
                }
                setMute(false);
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                setMute(true);
                break;

            default:
                break;
        }
    }

    private void forceToHeadsetMode() {
        if (mIsSpeakerUsed && isHeadSetIn()) {
            AudioSystem.setForceUse(FOR_PROPRIETARY, AudioSystem.FORCE_NONE);
            // save user's option to shared preferences.
            FmUtils.setIsSpeakerModeOnFocusLost(mContext, true);
        }
    }

    /**
     * FM Radio listener record
     */
    private static class Record {
        int mHashCode; // hash code
        FmListener mCallback; // call back
    }

    /**
     * Register FM Radio listener, activity get service state should call this
     * method register FM Radio listener
     *
     * @param callback FM Radio listener
     */
    public void registerFmRadioListener(FmListener callback) {
        synchronized (mRecords) {
            // register callback in AudioProfileService, if the callback is
            // exist, just replace the event.
            Record record = null;
            int hashCode = callback.hashCode();
            final int n = mRecords.size();
            for (int i = 0; i < n; i++) {
                record = mRecords.get(i);
                if (hashCode == record.mHashCode) {
                    return;
                }
            }
            record = new Record();
            record.mHashCode = hashCode;
            record.mCallback = callback;
            mRecords.add(record);
        }
    }

    /**
     * Call back from service to activity
     *
     * @param bundle The message to activity
     */
    private void notifyActivityStateChanged(Bundle bundle) {
        synchronized (mRecords) {
            if (!mRecords.isEmpty()) {
                Iterator<Record> iterator = mRecords.iterator();
                while (iterator.hasNext()) {
                    Record record = (Record) iterator.next();

                    FmListener listener = record.mCallback;

                    if (listener == null) {
                        iterator.remove();
                        return;
                    }

                    listener.onCallBack(bundle);
                }
            }
        }
    }

    /**
     * Call back from service to the current request activity
     * Scan need only notify FmFavoriteActivity if current is FmFavoriteActivity
     *
     * @param bundle The message to activity
     */
    private void notifyCurrentActivityStateChanged(Bundle bundle) {
        synchronized (mRecords) {
            if (!mRecords.isEmpty()) {
                Log.d(TAG, "notifyCurrentActivityStateChanged = " + mRecords.size());
                if (mRecords.size() > 0) {
                    Record record = mRecords.get(mRecords.size() - 1);
                    FmListener listener = record.mCallback;
                    if (listener == null) {
                        mRecords.remove(record);
                        return;
                    }
                    listener.onCallBack(bundle);
                }
            }
        }
    }

    /**
     * Unregister FM Radio listener
     *
     * @param callback FM Radio listener
     */
    public void unregisterFmRadioListener(FmListener callback) {
        remove(callback.hashCode());
    }

    /**
     * Remove call back according hash code
     *
     * @param hashCode The call back hash code
     */
    private void remove(int hashCode) {
        synchronized (mRecords) {
            Iterator<Record> iterator = mRecords.iterator();
            while (iterator.hasNext()) {
                Record record = (Record) iterator.next();
                if (record.mHashCode == hashCode) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Check recording sd card is unmount
     *
     * @param intent The unmount sd card intent
     * @return true or false indicate whether current recording sd card is
     * unmount or not
     */
    public boolean isRecordingCardUnmount(Intent intent) {
        String unmountSDCard = intent.getData().toString();
        Log.d(TAG, "unmount sd card file path: " + unmountSDCard);
        return unmountSDCard.equalsIgnoreCase("file://" + sRecordingSdcard) ? true : false;
    }

    private int[] updateStations(int[] stations) {
        Log.d(TAG, "updateStations() : stations = " + Arrays.toString(stations));
        int firstValidstation = mCurrentStation;

        int stationNum = 0;
        if (null != stations) {
            int searchedListSize = stations.length;
            if (mIsDistanceExceed) {
                FmStation.cleanSearchedStations(mContext);
                for (int j = 0; j < searchedListSize; j++) {
                    int freqSearched = stations[j];
                    if (FmUtils.isInvalidStation(freqSearched)) {
                        Log.d(TAG, "updateStations->invalidStation: " + freqSearched);
                        continue;
                    }
                    if (FmUtils.isValidStation(freqSearched) &&
                            !FmStation.isFavoriteStation(mContext, freqSearched)) {
                        FmStation.insertStationToDb(mContext, freqSearched, null);
                    }
                }
            } else {
                // get stations from db
                //wuhong 注释掉
//                stationNum = updateDBInLocation(stations);
                stationNum = updateSearchStationToDb(stations);
            }
        }

        Log.d(TAG, "updateStations() : firstValidstation = " + firstValidstation +
                ", stationNum = " + stationNum);
        return (new int[]{firstValidstation, stationNum});
    }

    /**
     * update DB, keep favorite and rds which is searched this time,
     * delete rds from db which is not searched this time.
     *
     * @param stations
     * @return number of valid searched stations
     */
    private int updateDBInLocation(int[] stations) {
        int stationNum = 0;
        int searchedListSize = stations.length;
        ArrayList<Integer> stationsInDB = new ArrayList<Integer>();
        Cursor cursor = null;
        try {
            // get non favorite stations
            cursor = mContext.getContentResolver().query(Station.CONTENT_URI,
                    new String[]{FmStation.Station.FREQUENCY},
                    FmStation.Station.IS_FAVORITE + "=0",
                    null, FmStation.Station.FREQUENCY);
            if ((null != cursor) && cursor.moveToFirst()) {

                do {
                    int freqInDB = cursor.getInt(cursor.getColumnIndex(
                            FmStation.Station.FREQUENCY));
                    stationsInDB.add(freqInDB);
                } while (cursor.moveToNext());

            } else {
                Log.d(TAG, "updateDBInLocation, insertSearchedStation cursor is null");
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        int listSizeInDB = stationsInDB.size();
        // delete station if db frequency is not in searched list
        for (int i = 0; i < listSizeInDB; i++) {
            int freqInDB = stationsInDB.get(i);
            for (int j = 0; j < searchedListSize; j++) {
                int freqSearched = stations[j];
                if (freqInDB == freqSearched) {
                    break;
                }
                if (j == (searchedListSize - 1) && freqInDB != freqSearched) {
                    // delete from db
                    FmStation.deleteStationInDb(mContext, freqInDB);
                }
            }
        }

        // add to db if station is not in db
        for (int j = 0; j < searchedListSize; j++) {
            int freqSearched = stations[j];
            if (FmUtils.isInvalidStation(freqSearched)) {
                continue;
            }
            if (FmUtils.isValidStation(freqSearched)) {
                stationNum++;
                if (!stationsInDB.contains(freqSearched)
                        && !FmStation.isFavoriteStation(mContext, freqSearched)) {
                    // insert to db
                    FmStation.insertStationToDb(mContext, freqSearched, "");
                }
            }
        }
        return stationNum;
    }







    /**
     * wuhong
     * 更新扫描出来的数据
     * 扫描出来的数据，在数据库中is_search字段应该为1
     */
    public int updateSearchStationToDb(int[] stations) {

        if (stations == null || stations.length == 0) {
            return 0;
        }

        //先删除所有没有被搜藏的数据
        String selection = FmStation.Station.IS_FAVORITE + "=0";
        mContext.getContentResolver().delete(FmStation.Station.CONTENT_URI,selection,null);

        for(int station : stations){
            ContentValues values = new ContentValues();
            values.put(Station.IS_SEARCH, 1);

            //尝试更新，如果这条数据在数据库中存在，把IS_SEARCH标记为1
            int rowsUpdated = mContext.getContentResolver().update(
                    Station.CONTENT_URI,
                    values,
                    Station.FREQUENCY + "=?",
                    new String[] { String.valueOf(station) });

            // 如果没有更新任何行，说明记录不存在，执行插入
            if (rowsUpdated == 0) {
                // 添加 frequency 到 values 中
                values.put(Station.FREQUENCY, station);
                mContext.getContentResolver().insert(Station.CONTENT_URI, values);
            }
        }

        return stations.length;

    }










    /**
     * The background handler
     */
    class FmRadioServiceHandler extends Handler {
        public FmRadioServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle;
            Log.d(TAG, "FmRadioServiceHandler() : msg.what = " + msg.what);
            switch (msg.what) {

                // power up
                case FmListener.MSGID_POWERUP_FINISHED:
                    bundle = msg.getData();
                    handlePowerUp(bundle);
                    break;

                // power down
                case FmListener.MSGID_POWERDOWN_FINISHED:
                    abandonAudioFocus();
                    handlePowerDown();
                    break;

                // fm exit
                case FmListener.MSGID_FM_EXIT:
                    powerDown();

                    bundle = new Bundle(1);
                    bundle.putInt(FmListener.CALLBACK_FLAG, FmListener.MSGID_FM_EXIT);
                    notifyActivityStateChanged(bundle);
                    // Finish favorite when exit FM
                    if (sExitListener != null) {
                        sExitListener.onExit();
                    }
                    break;

                // switch antenna
                case FmListener.MSGID_SWITCH_ANTENNA:
                    bundle = msg.getData();
                    int value = bundle.getInt(FmListener.SWITCH_ANTENNA_VALUE);

                    // if ear phone insert, need dismiss plugin earphone
                    // dialog
                    // if earphone plug out and it is not play recorder
                    // state, show plug dialog.
                    if (0 == value) {
                        // powerUpAsync(FMRadioUtils.computeFrequency(mCurrentStation));
                        bundle.putInt(FmListener.CALLBACK_FLAG,
                                FmListener.MSGID_SWITCH_ANTENNA);
                        bundle.putBoolean(FmListener.KEY_IS_SWITCH_ANTENNA, true);
                        notifyActivityStateChanged(bundle);
                    } else {
                        // ear phone plug out, and recorder state is not
                        // play recorder state,
                        // show dialog.
                        if (mRecordState != FmRecorder.STATE_PLAYBACK) {
                            bundle.putInt(FmListener.CALLBACK_FLAG,
                                    FmListener.MSGID_SWITCH_ANTENNA);
                            bundle.putBoolean(FmListener.KEY_IS_SWITCH_ANTENNA, false);
                            notifyActivityStateChanged(bundle);
                        }
                    }
                    break;

                // tune to station
                case FmListener.MSGID_TUNE_FINISHED:
                    bundle = msg.getData();
                    boolean direction = bundle.getBoolean(DIRECTION);
                    float tuneStation = bundle.getFloat(FM_FREQUENCY);
                    Log.d(TAG, "FmRadioServiceHandler().MSGID_TUNE_FINISHED : direction = " + direction
                        + ", tuneStation = " + tuneStation);
                    boolean isTune = false;
                    if (tuneStation != 0) {
                        isTune = tuneStation(tuneStation);
                    } else {
                        isTune = tuneStation(direction);
                    }

                    // if tune fail, pass current station to update ui
                    Log.d(TAG, "FmRadioServiceHandler().MSGID_TUNE_FINISHED : isTune = " + isTune
                        + ", tuneStation = " + tuneStation + ", mCurrentStation = " + mCurrentStation);
                    if (isTune) {
                        if (tuneStation != 0) {

                        } else {
                            tuneStation = FmUtils.computeFrequency(mCurrentStation);
                            playFrequency(tuneStation);
                        }
                    }

                    bundle = new Bundle(3);
                    bundle.putInt(FmListener.CALLBACK_FLAG,
                            FmListener.MSGID_TUNE_FINISHED);
                    bundle.putBoolean(FmListener.KEY_IS_TUNE, isTune);
                    bundle.putFloat(FmListener.KEY_TUNE_TO_STATION, tuneStation);
                    bundle.putBoolean(FmListener.KEY_DIRECTION, direction);

                    notifyActivityStateChanged(bundle);
                    break;

                // seek to station
                case FmListener.MSGID_SEEK_FINISHED:
                    bundle = msg.getData();
                    mIsSeeking = true;
                    setMute(true);

                    boolean seekStation = seekStation(bundle.getBoolean(OPTION));
                    Log.d(TAG, "FmRadioServiceHandler().MSGID_SEEK_FINISHED seekStation = " + seekStation);
                    float station = 0;
                    if (seekStation) {
                        station = FmUtils.computeFrequency((int) mCurrentStation);
                        playFrequency(station);
                        Log.d(TAG, "FmRadioServiceHandler().MSGID_SEEK_FINISHED seekStation Successful");
                    } else {
                        setMute(false);
                        FmStation.setCurrentStation(mContext, mCurrentStation);
                        station = FmUtils.computeFrequency((int) mCurrentStation);
                        Log.d(TAG, "FmRadioServiceHandler().MSGID_SEEK_FINISHED seekStation fail");
                    }
                    Log.d(TAG, "handleMessage seekStation:"+station);

                    bundle = new Bundle(2);
                    bundle.putInt(FmListener.CALLBACK_FLAG,
                            FmListener.MSGID_TUNE_FINISHED);
                    bundle.putBoolean(FmListener.KEY_IS_TUNE, seekStation);
                    bundle.putFloat(FmListener.KEY_TUNE_TO_STATION, station);
                    notifyActivityStateChanged(bundle);
                    mIsSeeking = false;
                    break;

                // start scan
                case FmListener.MSGID_SCAN_FINISHED:
                    Log.d(TAG, "FmRadioServiceHandler() : MSGID_SCAN_FINISHED");
                    int[] stations = null;
                    int[] result = null;
                    boolean isScan = true;
                    mIsScanning = true;
                    if (powerUp(FmUtils.DEFAULT_STATION_FLOAT)) {
                        stations = startScan();
                    }
                    // check whether cancel scan
                    Log.d(TAG, "FmRadioServiceHandler() : stations = " + Arrays.toString(stations));
                    if ((null != stations) && stations[0] == -100) {
                        Log.d(TAG, "FmRadioServiceHandler() : stations null");
                        isScan = false;
                        result = new int[]{
                                -1, 0
                        };
                    } else {
                        result = updateStations(stations);
                        tuneStation(FmUtils.computeFrequency(result[0]));
                    }

                    /*
                     * if there is stop command when scan, so it needs to mute
                     * fm avoid fm sound come out.
                     */
                    if (mIsAudioFocusHeld) {
                        setMute(false);
                    }

                    bundle = new Bundle(4);
                    bundle.putInt(FmListener.CALLBACK_FLAG,
                            FmListener.MSGID_SCAN_FINISHED);
                    //bundle.putInt(FmListener.KEY_TUNE_TO_STATION, scanTuneStation);
                    bundle.putInt(FmListener.KEY_STATION_NUM, result[1]);
                    bundle.putBoolean(FmListener.KEY_IS_SCAN, isScan);
                    mIsScanning = false;
                    // Only notify the newest request activity
                    notifyCurrentActivityStateChanged(bundle);
                    break;

                // audio focus changed
                case FmListener.MSGID_AUDIOFOCUS_CHANGED:
                    bundle = msg.getData();
                    int focusState = bundle.getInt(FmListener.KEY_AUDIOFOCUS_CHANGED);
                    updateAudioFocus(focusState);
                    break;

                case FmListener.MSGID_SET_MUTE_FINISHED:
                    bundle = msg.getData();
                    setMute(bundle.getBoolean(OPTION));
                    break;

                /********** recording **********/
                case FmListener.MSGID_STARTRECORDING_FINISHED:
                    startRecording();
                    break;

                case FmListener.MSGID_STOPRECORDING_FINISHED:
                    stopRecording();
                    break;

                case FmListener.MSGID_RECORD_MODE_CHANED:
                    bundle = msg.getData();
                    setRecordingMode(bundle.getBoolean(OPTION));
                    break;

                case FmListener.MSGID_SAVERECORDING_FINISHED:
                    bundle = msg.getData();
                    saveRecording(bundle.getString(RECODING_FILE_NAME));
                    break;

                default:
                    break;
            }
        }

    }

    /**
     * handle power down, execute power down and call back to activity.
     */
    private void handlePowerDown() {
        powerDown();
        Bundle bundle = new Bundle(1);
        bundle.putInt(FmListener.CALLBACK_FLAG, FmListener.MSGID_POWERDOWN_FINISHED);
        notifyActivityStateChanged(bundle);
    }

    /**
     * handle power up, execute power up and call back to activity.
     *
     * @param bundle power up frequency
     */
    private void handlePowerUp(Bundle bundle) {
        float curFrequency = bundle.getFloat(FM_FREQUENCY);
        Log.d(TAG, "handlePowerUp() : curFrequency = " + curFrequency);

        if (!isAntennaAvailable()) {
            Log.d(TAG, "handlePowerUp, earphone is not ready");
            bundle = new Bundle(2);
            bundle.putInt(FmListener.CALLBACK_FLAG, FmListener.MSGID_SWITCH_ANTENNA);
            bundle.putBoolean(FmListener.KEY_IS_SWITCH_ANTENNA, false);
            notifyActivityStateChanged(bundle);
            return;
        }

        boolean isPowerUp = powerUp(curFrequency);
        Log.d(TAG, "handlePowerUp() : isPowerUp = " + isPowerUp);
        if (isPowerUp) {
            boolean isFirstTimePlayFm = FmUtils.isFirstTimePlayFm(mContext);
            Log.d(TAG, "handlePowerUp() : isFirstTimePlayFm = " + isFirstTimePlayFm);
            if (isFirstTimePlayFm) {
                firstPlaying(curFrequency);
                FmUtils.setIsFirstTimePlayFm(mContext);
            } else {
                tuneStation(curFrequency);
            }
            mPausedByTransientLossOfFocus = false;
        }
        bundle = new Bundle(2);
        Log.d(TAG, "handlePowerUp() : mCurrentStation = " + mCurrentStation);
        bundle.putInt(FmListener.CALLBACK_FLAG, FmListener.MSGID_POWERUP_FINISHED);
        bundle.putInt(FmListener.KEY_TUNE_TO_STATION, mCurrentStation);
        notifyActivityStateChanged(bundle);
    }

    /**
     * check FM is foreground or background
     */
    public boolean isActivityForeground() {
        return (mIsFmMainForeground || mIsFmFavoriteForeground || mIsFmRecordForeground);
    }

    /**
     * mark FmMainActivity is foreground or not
     *
     * @param isForeground
     */
    public void setFmMainActivityForeground(boolean isForeground) {
        mIsFmMainForeground = isForeground;
        showPlayingNotification();
    }

    /**
     * mark FmRecordActivity activity is foreground or not
     *
     * @param isForeground
     */
    public void setFmRecordActivityForeground(boolean isForeground) {
        mIsFmRecordForeground = isForeground;
    }

    /**
     * Get the recording sdcard path when staring record
     *
     * @return sdcard path like "/storage/sdcard0"
     */
    public static String getRecordingSdcard() {
        return sRecordingSdcard;
    }

    /**
     * The listener interface for exit
     */
    public interface OnExitListener {
        /**
         * When Service finish, should notify FmFavoriteActivity to finish
         */
        void onExit();
    }

    /**
     * Register the listener for exit
     *
     * @param listener The listener want to know the exit event
     */
    public static void registerExitListener(OnExitListener listener) {
        sExitListener = listener;
    }

    /**
     * Unregister the listener for exit
     *
     * @param listener The listener want to know the exit event
     */
    public static void unregisterExitListener(OnExitListener listener) {
        sExitListener = null;
    }



    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (mPowerStatus != POWER_UP) {
            exitFm();
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }

    private void firstPlaying(float frequency) {
        Log.d(TAG, "firstPlaying() : mPowerStatus = " + mPowerStatus + ", frequency = " + frequency);
        if (mPowerStatus != POWER_UP) {
            Log.w(TAG, "firstPlaying, FM is not powered up");
            return;
        }
        float seekStation = searchStation(RadioManager.STATUS_OK);
        Log.d(TAG, "firstPlaying() : seekStation = " + seekStation);
        if (FmUtils.computeFrequency((int) seekStation) > 0){
            tuneStation(FmUtils.computeFrequency((int) seekStation));
        }else {
            tuneStation(frequency);
        }
    }

    /**
     * Set notification class name
     *
     * @param clsName The target class name of activity
     */
    public void setNotificationClsName(String clsName) {
        mTargetClassName = clsName;
    }

    public void quitFm() {
        abandonAudioFocus();
        if (mFmServiceHandler != null) {
            mFmServiceHandler.removeCallbacksAndMessages(null);
        }
        stopSelf();
    }

    private RadioTuner.Callback mHardwareCallback = new RadioTuner.Callback() {
        @Override
        public void onProgramInfoChanged(RadioManager.ProgramInfo info) {
            Objects.requireNonNull(info);

            Log.d(TAG, "mHardwareCallback() : isCancel = " + isCancel + ", mCurrentStation = " + mCurrentStation);
            if (isCancel){
                isCancel = false;
                mRadioTuner.tune(mCurrentStation, 0);
            }
            long station_value = info.getSelector().getPrimaryId().getValue();
            Log.d(TAG, "mHardwareCallback() : NOW_MODE = " + NOW_MODE + ", station_value = " + station_value);
            if (NOW_MODE.equals(TUNE_STATION)) {
                synchronized (mLock) {
                    //mCurrentStation = (int) info.getSelector().getPrimaryId().getValue();
                    int tmpCS = (int) info.getSelector().getPrimaryId().getValue();
                    mCurrentStation = tmpCS>0 ? tmpCS : mCurrentStation;
                    Log.d(TAG, "mHardwareCallback() : tmpCS = " + tmpCS + ", mCurrentStation = " + mCurrentStation);
                    mSetFreqPending = false;
                    mLock.notify();
                }
            } else if (NOW_MODE.equals(SEEK_STATION)) {
                synchronized (mSearchLock) {
                    mCurrentStation = (int) info.getSelector().getPrimaryId().getValue();
                    Log.d(TAG, "mHardwareCallback() : SEEK_STATION.mCurrentStation = " + mCurrentStation);
                    mSearchLock.notify();
                }
            } else if (NOW_MODE.equals(START_STATION)) {
                synchronized (mSearchLock) {
                    mSeekFreqClbk = info.getSelector().getPrimaryId().getValue();

                    if (mSeekFreqClbk != 0) {
                        mValidStation = true;
                    }
                    mSearchStationPending = false;
                    mSearchLock.notify();
                }
            } else if (NOW_MODE.equals(START_FULL_STATION)) {
                synchronized (mSearchLock) {
                    ProgramSelector.Identifier[] identifiers = info.getSelector().getSecondaryIds();
                    mFreqList = new int[identifiers.length];
                    for (int i = 0; i < identifiers.length; i++) {
                        mFreqList[i] = (int) identifiers[i].getValue();
                    }
                    Log.d(TAG, "mHardwareCallback() : mFreqList.length = " + mFreqList.length + ", mFreqList = " + Arrays.toString(mFreqList));
                    mStartFullStationPending = false;
                    mSearchLock.notify();
                }
            }
            NOW_MODE = "now_mode";
        }

        @Override
        public void onError(int status) {
            Log.d(TAG, "onError() : status = " + status);
        }

        @Override
        public void onControlChanged(boolean control) {

        }
    };

    /**
     * Determine if you have lost focus
     */
    public boolean isPausedByTransientLossOfFocus() {
        return mPausedByTransientLossOfFocus;
    }

    //ATC {
    public static final String ACTION_QB_ON = "autochips.intent.action.QB_POWERON";
    public static final String ACTION_QB_OFF = "autochips.intent.action.QB_POWEROFF";
    private int mLastPowerStatus = POWER_DOWN;
    private boolean mQbPowerOff = false;

    private void regiestPowerOnOffReceiver() {
        Log.i(TAG, "regiestPowerOnOffReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_QB_OFF);
        filter.addAction(ACTION_QB_ON);
        registerReceiver(mPowerOnOffReceiver, filter);
    }

    private void unregiestPowerOnOffReceiver() {
        Log.i(TAG, "unregiestPowerOnOffReceiver");
        unregisterReceiver(mPowerOnOffReceiver);
    }

    BroadcastReceiver mPowerOnOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "QB onReceive, action:" + action);
            if (ACTION_QB_OFF.equals(action)) {
                mQbPowerOff = true;
                Log.d(TAG, "QB onReceive, mIsAudioFocusHeld:" + mIsAudioFocusHeld
                        +", mPowerStatus = "+mPowerStatus);
                mLastPowerStatus = mPowerStatus;
                //Execute power down forcely, for power up may failed before.
                //if (mPowerStatus != POWER_DOWN) {
                powerDownAsync();
                //}
                //exitFm();//save recording file
                //TODO: if recorder playing
            } else if (ACTION_QB_ON.equals(action)) {

                Log.d(TAG, "QB onReceive, mIsAudioFocusHeld = "+mIsAudioFocusHeld
                        +", mLastPowerStatus = "+mLastPowerStatus);
                if (/*mIsAudioFocusHeld && */mLastPowerStatus != POWER_DOWN) {
                    powerUpAsync(FmUtils.computeFrequency(mCurrentStation));
                }
                mQbPowerOff = false;
            }
        }
    };
    //ATC }
}

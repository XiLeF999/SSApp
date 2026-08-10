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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View.MeasureSpec;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This class provider interface to compute station and frequency, get project
 * string
 */
public class FmUtils {
    private static final String TAG = "FmUtils";

    // FM station variables
    public static final int DEFAULT_STATION = 95000;
    public static final float DEFAULT_STATION_FLOAT = computeFrequency(DEFAULT_STATION);
    public static final int DEFAULT_STATION_50KHZ = 10000;
    // maximum station frequency
    public static final int HIGHEST_STATION = 108000;
    // minimum station frequency
    public static final int LOWEST_STATION = 87500;
    // station step
    private static final int STEP = 1;
    // convert rate
    private static final int CONVERT_RATE = 1000;

    // minimum storage space for record (512KB).
    // Need to check before starting recording and during recording to avoid
    // recording keeps going but there is no free space in sdcard.
    public static final long LOW_SPACE_THRESHOLD = 512 * 1024;
    // Different city may have different RDS information.
    // We define 100 miles (160934.4m) to distinguish the cities.
    public static final double LOCATION_DISTANCE_EXCEED = 160934.4;
    private static final String FM_LOCATION_LATITUDE = "fm_location_latitude";
    private static final String FM_LOCATION_LONGITUDE = "fm_location_longitude";
    private static final String FM_IS_FIRST_TIME_PLAY = "fm_is_first_time_play";
    private static final String IS_OPEN_FM = "is_open_fm";
    private static final String FM_IS_SPEAKER_MODE = "fm_is_speaker_mode";
    private static final String FM_IS_FIRST_ENTER_STATION_LIST = "fm_is_first_enter_station_list";
    private static final String IS_FM_MAIN_DESTROYED = "is_fm_main_destroyed";
    private static final String IS_FM_SERVICE_RELEASED = "is_service_released";
    private static final String IS_SPEAKER_PHONE_ON = "is_speaker_phone_on";

    private static final String IS_ENABLE_FM_DENOISE = "is_enable_fm_denoise";
    private static final int[] FILTER_STATIONS = {};
    // StorageManager For FM record
    private static StorageManager sStorageManager = null;

    public static float getHighestFrequency() {
        return HIGHEST_STATION;
    }

    public static final int FM_PERMISSIONS_REQUEST_CODE = 2016;

    /**
     * Whether the frequency is valid.
     *
     * @param station The FM station
     *
     * @return true if the frequency is in the valid scale, otherwise return
     *         false
     */
    public static boolean isValidStation(int station) {
        boolean isValid = (station >= LOWEST_STATION && station <= HIGHEST_STATION);
        return isValid;
    }

    /**
    *filter invalid station because hardware not support
    */
    public static boolean isInvalidStation(int station) {
        Log.d(TAG, "isInvalidStation: "+station);
        for (int i = 0; i < FILTER_STATIONS.length; i++) {
            if (station == FILTER_STATIONS[i]){
                return true;
            }
        }
        return false;
    }

    /**
     * Compute increase station frequency
     *
     * @param station The station frequency
     *
     * @return station The frequency after increased
     */
    public static int computeIncreaseStation(int station) {
        int result = station + STEP;
        if (result > HIGHEST_STATION) {
            result = LOWEST_STATION;
        }
        return result;
    }

    /**
     * Compute decrease station frequency
     *
     * @param station The station frequency
     *
     * @return station The frequency after decreased
     */
    public static int computeDecreaseStation(int station) {
        int result = station - STEP;
        if (result < LOWEST_STATION) {
            result = HIGHEST_STATION;
        }
        return result;
    }

    /**
     * Compute station value with given frequency
     *
     * @param frequency The station frequency
     *
     * @return station The result value
     */
    public static int computeStation(float frequency) {
        return (int) (frequency * CONVERT_RATE);
    }

    /**
     * Compute frequency value with given station
     *
     * @param station The station value
     *
     * @return station The frequency
     */
    public static float computeFrequency(int station) {
        return (float) station / CONVERT_RATE;
    }

    /**
     * According station to get frequency string
     *
     * @param station for 100KZ, range 875-1080
     *
     * @return string like 87.5
     */
    public static String formatStation(int station) {
        float frequency = (float) station / CONVERT_RATE;
//        DecimalFormat decimalFormat = new DecimalFormat("0.0");
//        return decimalFormat.format(frequency);
        return frequency + "";
    }

    public static String timeFormat(long millis) {
        long millis2 = millis / 1000;
        long hour = millis2 / 3600;
        long millis3 = millis2 % 3600;
        long min = millis3 / 60;
        long sec = millis3 % 60;
        return addPaddingForString(hour) + ":" + addPaddingForString(min) + ":" + addPaddingForString(sec);
    }
    private static String addPaddingForString(long time) {
        StringBuilder builder = new StringBuilder();
        if (time >= 0 && time < 10) {
            builder.append("0");
        }
        builder.append(time);
        return builder.toString();
    }
    /**
     * Get the phone storage path
     *
     * @return The phone storage path
     */
    public static String getDefaultStoragePath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    /**
     * Get the default storage state
     *
     * @return The default storage state
     */
    public static String getDefaultStorageState(Context context) {
        ensureStorageManager(context);
        String state = sStorageManager.getVolumeState(getDefaultStoragePath());
        return state;
    }

    private static void ensureStorageManager(Context context) {
        if (sStorageManager == null) {
            sStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        }
    }

    /**
     * Get the FM play list path
     *
     * @param context The context
     *
     * @return The FM play list path
     */
    public static String getPlaylistPath(Context context) {
        ensureStorageManager(context);
        String[] externalStoragePaths = sStorageManager.getVolumePaths();
        String path = externalStoragePaths[0] + "/Playlists/";
        return path;
    }

    /**
     * Check if has enough space for record
     *
     * @param recordingSdcard The recording sdcard path
     *
     * @return true if has enough space for record
     */
    public static boolean hasEnoughSpace(String recordingSdcard) {
        boolean ret = false;
        try {
            StatFs fs = new StatFs(recordingSdcard);
            long blocks = fs.getAvailableBlocks();
            long blockSize = fs.getBlockSize();
            long spaceLeft = blocks * blockSize;
            ret = spaceLeft > LOW_SPACE_THRESHOLD ? true : false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "hasEnoughSpace, sdcard may be unmounted:" + recordingSdcard);
        }
        return ret;
    }
    private static long lastClickTime;
    private static final int MIN_DELAY_TIME= 1000;
    public static boolean isFastRefresh() {
        boolean flag = true;
        long currentClickTime = System.currentTimeMillis();
        if ((currentClickTime - lastClickTime)  > MIN_DELAY_TIME) {
            flag = false;
        }
        lastClickTime = currentClickTime;
        return flag;
    }
    /**
     * Get the latest searched location
     * @return the list of latitude and longitude
     */
    public static double[] getLastSearchedLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String strLatitude = prefs.getString(FM_LOCATION_LATITUDE, "0.0");
        String strLongitude = prefs.getString(FM_LOCATION_LONGITUDE, "0.0");
        double latitude = Double.valueOf(strLatitude);
        double longitude = Double.valueOf(strLongitude);
        return new double[] { latitude, longitude };
    }

    /**
     * Set the last searched location
     */
    public static void setLastSearchedLocation(Context context, double latitude, double longitude) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        String strLatitude = Double.valueOf(latitude).toString();
        String strLongitude = Double.valueOf(longitude).toString();
        editor.putString(FM_LOCATION_LATITUDE, strLatitude);
        editor.putString(FM_LOCATION_LONGITUDE, strLongitude);
        editor.commit();
    }

    /**
     * check it is the first time to use Fm
     */
    public static boolean isFirstTimePlayFm(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstTime = prefs.getBoolean(FM_IS_FIRST_TIME_PLAY, true);
        return isFirstTime;
    }

    public static boolean isOpenFm(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstTime = prefs.getBoolean(IS_OPEN_FM, true);
        return isFirstTime;
    }

    public static void setOpenFm(Context context,Boolean isOpen){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(IS_OPEN_FM, isOpen);
        editor.commit();
    }
    public static boolean isEnableFmDenoise(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstTime = prefs.getBoolean(IS_ENABLE_FM_DENOISE, false);
        return isFirstTime;
    }

    public static void setEnableFmDenoise(Context context,Boolean isEnable){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(IS_ENABLE_FM_DENOISE, isEnable);
        editor.commit();
    }
    /**
     * Called when first time play FM.
     * @param context The context
     */
    public static void setIsFirstTimePlayFm(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(FM_IS_FIRST_TIME_PLAY, false);
        editor.commit();
    }

    /**
     * check it is the first time enter into station list page
     */
    public static boolean isFirstEnterStationList(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstEnter = prefs.getBoolean(FM_IS_FIRST_ENTER_STATION_LIST, true);
        if (isFirstEnter) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(FM_IS_FIRST_ENTER_STATION_LIST, false);
            editor.commit();
        }
        return isFirstEnter;
    }

    public static void setIsFmMainDestroyed(Context context, Boolean isReleased) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(IS_FM_MAIN_DESTROYED, isReleased);
        editor.commit();
    }

    public static boolean isFmMainDestroyed(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFmMainDestroyed = prefs.getBoolean(IS_FM_MAIN_DESTROYED, false);
        return isFmMainDestroyed;
    }

    public static void setIsServiceReleased(Context context, Boolean isReleased) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(IS_FM_SERVICE_RELEASED, isReleased);
        editor.commit();
    }

    public static boolean isServiceReleased(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isServiceReleased = prefs.getBoolean(IS_FM_SERVICE_RELEASED, false);
        return isServiceReleased;
    }

    public static void setIsSpeakerPhoneOn(Context context, Boolean isSpeaker) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(IS_SPEAKER_PHONE_ON, isSpeaker);
        editor.commit();
    }

    public static boolean isSpeakerPhoneOn(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isSpeakerPhoneOn = prefs.getBoolean(IS_SPEAKER_PHONE_ON, false);
        return isSpeakerPhoneOn;
    }

    /**
     * Create the notification large icon bitmap from layout
     * @param c The context
     * @param text The frequency text
     * @return The large icon bitmap with frequency text
     */
    //wuhong 注释掉
//    public static Bitmap createNotificationLargeIcon(Context c, String text) {
//        Resources res = c.getResources();
//        int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
//        int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
//        LinearLayout iconLayout = new LinearLayout(c);
//        iconLayout.setOrientation(LinearLayout.VERTICAL);
//        iconLayout.setBackgroundColor(c.getResources().getColor(R.color.theme_primary_color));
//        iconLayout.setDrawingCacheEnabled(true);
//        iconLayout.layout(0, 0, width, height);
//        TextView iconText = new TextView(c);
//        iconText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24.0f);
//        iconText.setTextColor(res.getColor(R.color.theme_title_color));
//        iconText.setText(text);
//        iconText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
//                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
//        int left = (int) ((width - iconText.getMeasuredWidth()) * 0.5);
//        int top = (int) ((height - iconText.getMeasuredHeight()) * 0.5);
//        iconText.layout(left, top, iconText.getMeasuredWidth() + left,
//                iconText.getMeasuredHeight() + top);
//        iconLayout.addView(iconText);
//        iconLayout.layout(0, 0, width, height);
//
//        iconLayout.buildDrawingCache();
//        Bitmap largeIcon = Bitmap.createBitmap(iconLayout.getDrawingCache());
//        iconLayout.destroyDrawingCache();
//        return largeIcon;
//    }

    /**
     * Get whether speaker mode is in use when audio focus lost.
     * @param context the Context
     * @return true for speaker mode, false for non speaker mode
     */
    public static boolean getIsSpeakerModeOnFocusLost(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getBoolean(FM_IS_SPEAKER_MODE, false);
    }

    /**
     * Set whether speaker mode is in use.
     * @param context the Context
     * @param isSpeaker speaker state
     */
    public static void setIsSpeakerModeOnFocusLost(Context context, boolean isSpeaker) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(FM_IS_SPEAKER_MODE, isSpeaker);
        editor.commit();
    }

    public static void setSelectAllChangedListener(CompoundButton toggle, final TextView promptTextView) {
        //wuhong 注释掉
//        setSelectAllChangedListener(toggle, promptTextView, R.string.select_all, R.string.cancel_select_all);
    }

    public static void setSelectAllChangedListener(CompoundButton toggle,final TextView promptTextView,
                                                   final int selectAllTextRes, final int removeAllTextRes) {
        if (toggle == null || promptTextView == null) {
            Log.e(TAG,"In function setSelectAllChangedListener,toggle = " + toggle + " ,promptTextView = "
                    + promptTextView);
            return;
        }
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    promptTextView.setText(removeAllTextRes);
                } else {
                    promptTextView.setText(selectAllTextRes);
                }
            }
        });
    }

    public static boolean hasStoragePermission(Context context) {
        if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    public static void updateStatusBarColor(Activity activity, boolean actionMode) {
        //wuhong 注释掉
//        if (actionMode) {
//            int cabStatusBarColor = activity.getResources().getColor(
//                    R.color.action_mode_statusbar_color);
//            activity.getWindow().setStatusBarColor(cabStatusBarColor);
//        } else {
//            int cabStatusBarColor = activity.getResources().getColor(
//                    R.color.theme_primarydark_color);
//            activity.getWindow().setStatusBarColor(cabStatusBarColor);
//        }
    }
}

package com.sansan.radio.activity;

import androidx.fragment.app.FragmentActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.sansan.radio.R;
import com.sansan.radio.widget.RadioScaleView;

public class MainActivity extends FragmentActivity {

    private RadioScaleView radioScaleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTransparentStatusBar();




        radioScaleView = findViewById(R.id.radioScaleView);






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



}
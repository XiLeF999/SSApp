package com.sansan.radio.widget;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class RadioScaleView extends View {

    // 频率范围 - 标准FM广播范围
    private float mMinFrequency = 87.5f;  // 最小频率
    private float mMaxFrequency = 108.0f; // 最大频率
    private float mCurrentFrequency = 97.5f; // 当前频率

    // 绘制工具
    private Paint mScalePaint;
    private Paint mTextPaint;
    private Paint mIndicatorPaint;

    // 尺寸
    private int mWidth, mHeight;
    private int mCenterX;

    // 刻度相关
    private static final float SMALL_SCALE_SPACING = 0.1f;  // 小刻度间隔
    private static final float LARGE_SCALE_SPACING = 1.0f;   // 大刻度间隔

    // 触摸相关
    private float mLastTouchX;
    private float mOffsetX = 0;  // 滚动偏移量

    public RadioScaleView(Context context) {
        this(context, null);
    }

    public RadioScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 初始化刻度线画笔
        mScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScalePaint.setColor(Color.parseColor("#666666"));
        mScalePaint.setStrokeWidth(2);

        // 初始化文字画笔
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(30);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        // 初始化指示线画笔
        mIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mIndicatorPaint.setColor(Color.RED);
        mIndicatorPaint.setStrokeWidth(4);

        // 初始偏移量，让当前频率在中间
        float centerFrequency = (mMinFrequency + mMaxFrequency) / 2; // 计算中心频率
        mOffsetX = (mCurrentFrequency - centerFrequency) * 100;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        mCenterX = w / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制背景
        canvas.drawColor(Color.WHITE);

        // 绘制所有刻度
        drawAllScales(canvas);

        // 绘制中心指示线
        drawIndicator(canvas);

        // 绘制当前频率
        drawCurrentFrequency(canvas);
    }

    private void drawAllScales(Canvas canvas) {
        // 每MHz对应的像素数
        float pixelsPerMHz = 100;
        float pixelsPerSmallScale = pixelsPerMHz * SMALL_SCALE_SPACING; // 每个小刻度的像素间距

        // 中心点对应的频率
        float centerFrequency = (mMinFrequency + mMaxFrequency) / 2; // 约为97.75MHz

        // 1. 首先绘制所有小刻度（从87.5开始，每0.1MHz一个）
        for (float freq = mMinFrequency; freq <= mMaxFrequency; freq += SMALL_SCALE_SPACING) {
            // 计算刻度位置
            float x = mCenterX + (freq - centerFrequency) * pixelsPerMHz - mOffsetX;

            // 只绘制在可见范围内的刻度
            if (x >= -10 && x <= mWidth + 10) {
                // 判断是否为整数MHz（大刻度位置）
                boolean isInteger = Math.abs(freq - Math.round(freq)) < 0.001f;

                if (isInteger) {
                    // 绘制大刻度线
                    mScalePaint.setStrokeWidth(3);
                    canvas.drawLine(x, mHeight - 100, x, mHeight - 50, mScalePaint);

                    // 绘制频率数字
                    canvas.drawText(String.format("%.0f", freq), x, mHeight - 120, mTextPaint);
                } else {
                    // 绘制小刻度线
                    mScalePaint.setStrokeWidth(1);
                    // 区分0.5的刻度（比普通小刻度长一些）
                    boolean isHalf = Math.abs(freq - Math.floor(freq) - 0.5) < 0.001f;
                    float endY = isHalf ? mHeight - 70 : mHeight - 80;
                    canvas.drawLine(x, mHeight - 100, x, endY, mScalePaint);
                }
            }
        }
    }

    private void drawIndicator(Canvas canvas) {
        // 绘制中心红色指示线
        canvas.drawLine(mCenterX, 0, mCenterX, mHeight, mIndicatorPaint);

        // 绘制三角形指示器
        Path triangle = new Path();
        triangle.moveTo(mCenterX - 20, mHeight - 100);
        triangle.lineTo(mCenterX + 20, mHeight - 100);
        triangle.lineTo(mCenterX, mHeight - 60);
        triangle.close();

        mIndicatorPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(triangle, mIndicatorPaint);
    }

    private void drawCurrentFrequency(Canvas canvas) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);

        String freqText = String.format("%.1f MHz", mCurrentFrequency);
        canvas.drawText(freqText, mCenterX, 100, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = event.getX();
                return true;

            case MotionEvent.ACTION_MOVE:
                float currentX = event.getX();
                float deltaX = currentX - mLastTouchX;

                // 更新偏移量
                mOffsetX -= deltaX;

                // 限制偏移范围
                float pixelsPerMHz = 100;
                float centerFrequency = (mMinFrequency + mMaxFrequency) / 2;
                float maxOffset = (mMaxFrequency - centerFrequency) * pixelsPerMHz;
                float minOffset = (mMinFrequency - centerFrequency) * pixelsPerMHz;
                mOffsetX = Math.max(minOffset, Math.min(maxOffset, mOffsetX));

                // 计算当前频率
                mCurrentFrequency = centerFrequency + mOffsetX / pixelsPerMHz;
                mCurrentFrequency = Math.max(mMinFrequency, Math.min(mMaxFrequency, mCurrentFrequency));

                mLastTouchX = currentX;
                invalidate();  // 重绘视图
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 对齐到最近的小刻度
                alignToNearestScale();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void alignToNearestScale() {
        // 对齐到最近的小刻度（0.1MHz）
        float alignedFreq = Math.round(mCurrentFrequency * 10) / 10.0f;
        alignedFreq = Math.max(mMinFrequency, Math.min(mMaxFrequency, alignedFreq));

        // 计算对应的偏移量
        float pixelsPerMHz = 100;
        float centerFrequency = (mMinFrequency + mMaxFrequency) / 2;
        mOffsetX = (alignedFreq - centerFrequency) * pixelsPerMHz;
        mCurrentFrequency = alignedFreq;

        invalidate();

        // 触发频率变化回调
        if (mOnFrequencyChangeListener != null) {
            mOnFrequencyChangeListener.onFrequencyChanged(mCurrentFrequency);
        }
    }

    // 设置频率
    public void setFrequency(float frequency) {
        mCurrentFrequency = Math.max(mMinFrequency, Math.min(mMaxFrequency, frequency));

        float pixelsPerMHz = 100;
        float centerFrequency = (mMinFrequency + mMaxFrequency) / 2;
        mOffsetX = (mCurrentFrequency - centerFrequency) * pixelsPerMHz;

        invalidate();
    }

    // 获取当前频率
    public float getCurrentFrequency() {
        return mCurrentFrequency;
    }

    // 设置频率范围
    public void setFrequencyRange(float min, float max) {
        mMinFrequency = min;
        mMaxFrequency = max;
        mCurrentFrequency = Math.max(min, Math.min(max, mCurrentFrequency));

        float pixelsPerMHz = 100;
        float centerFrequency = (mMinFrequency + mMaxFrequency) / 2;
        mOffsetX = (mCurrentFrequency - centerFrequency) * pixelsPerMHz;

        invalidate();
    }

    // 频率变化监听器接口
    public interface OnFrequencyChangeListener {
        void onFrequencyChanged(float frequency);
    }

    private OnFrequencyChangeListener mOnFrequencyChangeListener;

    public void setOnFrequencyChangeListener(OnFrequencyChangeListener listener) {
        mOnFrequencyChangeListener = listener;
    }
}
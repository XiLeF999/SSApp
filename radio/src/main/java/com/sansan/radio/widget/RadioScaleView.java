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
    private Paint mPointerPaint;

    // 尺寸
    private int mWidth, mHeight;
    private float mScaleStartX;  // 刻度开始位置
    private float mScaleEndX;    // 刻度结束位置
    private float mScaleLength;  // 刻度总长度

    // 刻度间隔
    private static final float SMALL_SCALE_SPACING = 0.5f;  // 小刻度间隔 0.5MHz

    // 指针相关
    private float mPointerX;  // 指针的水平位置
    private boolean mIsDragging = false;
    private float mPointerTouchOffset = 0;  // 触摸点与指针中心的偏移

    // 每MHz对应的像素数
    private float mPixelsPerMHz;

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

        // 初始化指针画笔
        mPointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointerPaint.setColor(Color.RED);
        mPointerPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;

        // 计算刻度区域
        int padding = 50;  // 左右边距
        mScaleStartX = padding;
        mScaleEndX = w - padding;
        mScaleLength = mScaleEndX - mScaleStartX;

        // 计算每MHz对应的像素数
        float frequencyRange = mMaxFrequency - mMinFrequency;
        mPixelsPerMHz = mScaleLength / frequencyRange;

        // 初始化指针位置
        updatePointerPosition();
    }

    private void updatePointerPosition() {
        // 根据当前频率计算指针位置
        mPointerX = mScaleStartX + (mCurrentFrequency - mMinFrequency) * mPixelsPerMHz;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制背景
        canvas.drawColor(Color.WHITE);

        // 绘制刻度背景区域
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#F5F5F5"));
        canvas.drawRect(mScaleStartX, mHeight - 150, mScaleEndX, mHeight - 20, bgPaint);

        // 绘制所有刻度
        drawAllScales(canvas);

        // 绘制指针
        drawPointer(canvas);

        // 绘制当前频率
        drawCurrentFrequency(canvas);
    }

    private void drawAllScales(Canvas canvas) {
        // 绘制刻度线
        for (float freq = mMinFrequency; freq <= mMaxFrequency; freq += SMALL_SCALE_SPACING) {
            float x = mScaleStartX + (freq - mMinFrequency) * mPixelsPerMHz;

            // 判断是否为整数MHz
            boolean isInteger = Math.abs(freq - Math.round(freq)) < 0.001f;

            if (isInteger) {
                int intFreq = Math.round(freq);

                if (intFreq % 2 == 0) {
                    // 每2MHz的大刻度
                    mScalePaint.setStrokeWidth(3);
                    mScalePaint.setColor(Color.parseColor("#333333"));
                    canvas.drawLine(x, mHeight - 100, x, mHeight - 50, mScalePaint);

                    // 绘制大刻度频率数字
                    mTextPaint.setTextSize(30);
                    mTextPaint.setColor(Color.BLACK);
                    canvas.drawText(String.format("%.0f", freq), x, mHeight - 120, mTextPaint);
                } else {
                    // 奇数MHz的刻度
                    mScalePaint.setStrokeWidth(2);
                    mScalePaint.setColor(Color.parseColor("#666666"));
                    canvas.drawLine(x, mHeight - 100, x, mHeight - 70, mScalePaint);

                    // 绘制小数字
                    mTextPaint.setTextSize(24);
                    mTextPaint.setColor(Color.parseColor("#666666"));
                    canvas.drawText(String.format("%.0f", freq), x, mHeight - 35, mTextPaint);
                }
            } else {
                // 0.5MHz的小刻度
                mScalePaint.setStrokeWidth(1);
                mScalePaint.setColor(Color.parseColor("#999999"));
                canvas.drawLine(x, mHeight - 100, x, mHeight - 80, mScalePaint);
            }
        }

//        // 绘制刻度两端的竖线
//        mScalePaint.setStrokeWidth(2);
//        mScalePaint.setColor(Color.parseColor("#333333"));
//        canvas.drawLine(mScaleStartX, mHeight - 100, mScaleStartX, mHeight - 50, mScalePaint);
//        canvas.drawLine(mScaleEndX, mHeight - 100, mScaleEndX, mHeight - 50, mScalePaint);
    }

    private void drawPointer(Canvas canvas) {
        // 绘制指针竖线
        mIndicatorPaint.setStrokeWidth(4);
        canvas.drawLine(mPointerX, 0, mPointerX, mHeight - 100, mIndicatorPaint);

        // 绘制指针三角形
        Path triangle = new Path();
        triangle.moveTo(mPointerX - 20, mHeight - 100);
        triangle.lineTo(mPointerX + 20, mHeight - 100);
        triangle.lineTo(mPointerX, mHeight - 60);
        triangle.close();

        canvas.drawPath(triangle, mPointerPaint);

        // 绘制指针顶部的圆点
        mPointerPaint.setColor(Color.RED);
        canvas.drawCircle(mPointerX, 20, 10, mPointerPaint);
    }

    private void drawCurrentFrequency(Canvas canvas) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        String freqText = String.format("%.1f MHz", mCurrentFrequency);
        canvas.drawText(freqText, mWidth / 2, 80, textPaint);

        // 在指针下方也显示当前频率
        textPaint.setTextSize(24);
        textPaint.setColor(Color.parseColor("#333333"));
        canvas.drawText(freqText, mPointerX, mHeight - 20, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 检查是否点击了指针区域
                float pointerRadius = 40;
                if (Math.abs(x - mPointerX) < pointerRadius && y > mHeight - 150) {
                    mIsDragging = true;
                    mPointerTouchOffset = x - mPointerX;
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    // 计算新的指针位置
                    float newX = x - mPointerTouchOffset;

                    // 限制指针在刻度范围内
                    newX = Math.max(mScaleStartX, Math.min(mScaleEndX, newX));

                    // 更新指针位置
                    mPointerX = newX;

                    // 计算对应的频率
                    mCurrentFrequency = mMinFrequency + (mPointerX - mScaleStartX) / mPixelsPerMHz;
                    mCurrentFrequency = Math.max(mMinFrequency, Math.min(mMaxFrequency, mCurrentFrequency));

                    invalidate();

                    // 实时回调频率变化
                    if (mOnFrequencyChangeListener != null) {
                        mOnFrequencyChangeListener.onFrequencyChanging(mCurrentFrequency);
                    }
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    mIsDragging = false;

                    // 对齐到最近的0.1MHz刻度
                    float alignedFreq = Math.round(mCurrentFrequency * 10) / 10.0f;
                    alignedFreq = Math.max(mMinFrequency, Math.min(mMaxFrequency, alignedFreq));

                    mCurrentFrequency = alignedFreq;
                    updatePointerPosition();
                    invalidate();

                    // 触发最终频率变化回调
                    if (mOnFrequencyChangeListener != null) {
                        mOnFrequencyChangeListener.onFrequencyChanged(mCurrentFrequency);
                    }
                    return true;
                }
                return false;
        }
        return super.onTouchEvent(event);
    }

    // 设置频率
    public void setFrequency(float frequency) {
        mCurrentFrequency = Math.max(mMinFrequency, Math.min(mMaxFrequency, frequency));
        updatePointerPosition();
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

        if (mScaleLength > 0) {
            float frequencyRange = mMaxFrequency - mMinFrequency;
            mPixelsPerMHz = mScaleLength / frequencyRange;
        }

        updatePointerPosition();
        invalidate();
    }

    // 频率变化监听器接口
    public interface OnFrequencyChangeListener {
        void onFrequencyChanging(float frequency);
        void onFrequencyChanged(float frequency);
    }

    private OnFrequencyChangeListener mOnFrequencyChangeListener;

    public void setOnFrequencyChangeListener(OnFrequencyChangeListener listener) {
        mOnFrequencyChangeListener = listener;
    }
}
package com.sansan.radio.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.sansan.radio.R;

import java.util.Locale;

public class RadioScaleView extends View {

    private static final float FREQUENCY_STEP = 0.1f;
    private static final float PIXELS_PER_MHZ = 140f;
    private static final float TOUCH_SLOP = 3f;
    private static final float CENTER_INDICATOR_WIDTH = 44f;
    private static final float CENTER_INDICATOR_HEIGHT = 90f;
    private static final float INTEGER_TICK_WIDTH = 6f;
    private static final float INTEGER_TICK_HEIGHT = 36f;
    private static final float HALF_TICK_HEIGHT = 24f;
    private static final float MINOR_TICK_WIDTH = 4f;
    private static final float MINOR_TICK_HEIGHT = 15f;
    private static final float TICK_BASELINE_GAP = 4f;
    private static final float SCALE_BACKGROUND_WIDTH = 500f;
    private static final float SCALE_BACKGROUND_HEIGHT = 82f;
    private static final float EDGE_MASK_WIDTH = 125f;
    private static final float EDGE_MASK_HEIGHT = 120f;

    private float mMinFrequency = 87.5f;
    private float mMaxFrequency = 108.5f;
    private float mCurrentFrequency = 98f;

    private final Paint mScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Bitmap mPointerBitmap;
    private Bitmap mScaleBackgroundBitmap;
    private Bitmap mLeftMaskBitmap;
    private Bitmap mRightMaskBitmap;

    private float mCenterX;
    private float mLastTouchX;
    private float mDownTouchX;
    private boolean mIsDragging;

    private OnFrequencyChangeListener mOnFrequencyChangeListener;

    public RadioScaleView(Context context) {
        this(context, null);
    }

    public RadioScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mScalePaint.setStrokeCap(Paint.Cap.ROUND);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mPointerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_radio_hand);
        mScaleBackgroundBitmap = loadOptionalBitmap("radio_rs_bg");
        mLeftMaskBitmap = loadOptionalBitmap("radio_hide_l_bg");
        mRightMaskBitmap = loadOptionalBitmap("radio_hide_r_bg");
        setClickable(true);
    }

    private Bitmap loadOptionalBitmap(String resourceName) {
        int resourceId = getResources().getIdentifier(resourceName, "drawable",
                getContext().getPackageName());
        return resourceId == 0 ? null : BitmapFactory.decodeResource(getResources(), resourceId);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawScaleBackground(canvas);
        drawScaleWindow(canvas);
        drawEdgeMasks(canvas);
        drawFixedPointer(canvas);
    }

    private void drawScaleBackground(Canvas canvas) {
        if (mScaleBackgroundBitmap == null) {
            return;
        }
        float left = mCenterX - SCALE_BACKGROUND_WIDTH / 2f;
        float bottom = getScaleBaseline() + 1f;
        RectF destination = new RectF(left, bottom - SCALE_BACKGROUND_HEIGHT,
                left + SCALE_BACKGROUND_WIDTH, bottom);
        canvas.drawBitmap(mScaleBackgroundBitmap, null, destination, mBitmapPaint);
    }

    private void drawEdgeMasks(Canvas canvas) {
        float top = (getHeight() - EDGE_MASK_HEIGHT) / 2f;
        if (mLeftMaskBitmap != null) {
            RectF leftDestination = new RectF(0f, top, EDGE_MASK_WIDTH,
                    top + EDGE_MASK_HEIGHT);
            canvas.drawBitmap(mLeftMaskBitmap, null, leftDestination, mBitmapPaint);
        }
        if (mRightMaskBitmap != null) {
            RectF rightDestination = new RectF(getWidth() - EDGE_MASK_WIDTH, top,
                    getWidth(), top + EDGE_MASK_HEIGHT);
            canvas.drawBitmap(mRightMaskBitmap, null, rightDestination, mBitmapPaint);
        }
    }

    private void drawScaleWindow(Canvas canvas) {
        float baseline = getScaleBaseline();
        float visibleRange = getWidth() / PIXELS_PER_MHZ / 2f + FREQUENCY_STEP;
        int firstStep = (int) Math.floor((mCurrentFrequency - visibleRange) / FREQUENCY_STEP);
        int lastStep = (int) Math.ceil((mCurrentFrequency + visibleRange) / FREQUENCY_STEP);

        for (int step = firstStep; step <= lastStep; step++) {
            float frequency = step * FREQUENCY_STEP;
            if (frequency < mMinFrequency - 0.001f || frequency > mMaxFrequency + 0.001f) {
                continue;
            }

            float x = mCenterX + (frequency - mCurrentFrequency) * PIXELS_PER_MHZ;
            boolean wholeMHz = isWholeMHz(step);
            boolean halfMHz = !wholeMHz && step % 5 == 0;
            float width = wholeMHz ? INTEGER_TICK_WIDTH : MINOR_TICK_WIDTH;
            float distanceInSteps = Math.abs(x - mCenterX)
                    / (PIXELS_PER_MHZ * FREQUENCY_STEP);
            float height = getTickHeight(distanceInSteps, wholeMHz, halfMHz);
            int edgeAlpha = getEdgeAlpha(x);

            mScalePaint.setColor(getResources().getColor(R.color._9, getContext().getTheme()));
            mScalePaint.setAlpha(edgeAlpha);
            mScalePaint.setStyle(Paint.Style.FILL);
            float tickBottom = baseline - TICK_BASELINE_GAP;
            RectF tickBounds = new RectF(x - width / 2f, tickBottom - height,
                    x + width / 2f, tickBottom);
            canvas.drawRoundRect(tickBounds, width / 2f, width / 2f, mScalePaint);

            if (wholeMHz) {
                mTextPaint.setColor(getResources().getColor(R.color.text_secondary, getContext().getTheme()));
                mTextPaint.setAlpha(edgeAlpha);
                mTextPaint.setTextSize(20f);
                canvas.drawText(String.format(Locale.US, "%.1f", frequency), x, getHeight() - 5f, mTextPaint);
            }
        }

    }

    private boolean isWholeMHz(int step) {
        return step % 10 == 0;
    }

    private float getTickHeight(float distanceInSteps, boolean wholeMHz, boolean halfMHz) {
        float centerHeight = INTEGER_TICK_HEIGHT
                - Math.min(4f, distanceInSteps) * 5.25f;
        float levelHeight = wholeMHz
                ? INTEGER_TICK_HEIGHT
                : (halfMHz ? HALF_TICK_HEIGHT : MINOR_TICK_HEIGHT);
        return Math.max(levelHeight, centerHeight);
    }

    private int getEdgeAlpha(float x) {
        float halfWidth = Math.max(1f, getWidth() / 2f);
        float progress = Math.min(1f, Math.abs(x - mCenterX) / halfWidth);
        return Math.round(255f - progress * 200f);
    }

    private void drawFixedPointer(Canvas canvas) {
        if (mPointerBitmap == null) {
            return;
        }
        float left = mCenterX - CENTER_INDICATOR_WIDTH / 2f;
        float bottom = getScaleBaseline() - TICK_BASELINE_GAP;
        RectF destination = new RectF(left, bottom - CENTER_INDICATOR_HEIGHT,
                left + CENTER_INDICATOR_WIDTH, bottom);
        canvas.drawBitmap(mPointerBitmap, null, destination, mBitmapPaint);
    }

    private float getScaleBaseline() {
        return getHeight() - 31f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownTouchX = event.getX();
                mLastTouchX = mDownTouchX;
                mIsDragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!mIsDragging) {
                    return false;
                }
                float deltaX = event.getX() - mLastTouchX;
                mLastTouchX = event.getX();
                updateFrequency(mCurrentFrequency - deltaX / PIXELS_PER_MHZ, true);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mIsDragging) {
                    return false;
                }
                mIsDragging = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                mCurrentFrequency = clamp(Math.round(mCurrentFrequency * 10f) / 10f);
                invalidate();
                if (Math.abs(event.getX() - mDownTouchX) <= TOUCH_SLOP) {
                    performClick();
                }
                if (mOnFrequencyChangeListener != null) {
                    mOnFrequencyChangeListener.onFrequencyChanged(mCurrentFrequency);
                }
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void updateFrequency(float frequency, boolean notifyChanging) {
        mCurrentFrequency = clamp(frequency);
        invalidate();
        if (notifyChanging && mOnFrequencyChangeListener != null) {
            mOnFrequencyChangeListener.onFrequencyChanging(mCurrentFrequency);
        }
    }

    private float clamp(float frequency) {
        return Math.max(mMinFrequency, Math.min(mMaxFrequency, frequency));
    }

    public void setFrequency(float frequency) {
        updateFrequency(frequency, false);
    }

    public float getCurrentFrequency() {
        return mCurrentFrequency;
    }

    public void setFrequencyRange(float min, float max) {
        mMinFrequency = min;
        mMaxFrequency = max;
        mCurrentFrequency = clamp(mCurrentFrequency);
        invalidate();
    }

    public void startScan() {
        invalidate();
    }

    public interface OnFrequencyChangeListener {
        void onFrequencyChanging(float frequency);

        void onFrequencyChanged(float frequency);
    }

    public void setOnFrequencyChangeListener(OnFrequencyChangeListener listener) {
        mOnFrequencyChangeListener = listener;
    }
}

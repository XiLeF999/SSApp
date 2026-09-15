package com.sansan.picture.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.github.chrisbanes.photoview.PhotoView;

public class InterceptablePhotoView extends PhotoView {

    private static final float SWIPE_DISTANCE = 100f;
    private static final float SCALE_TOLERANCE = 0.02f;

    private float startX;
    private float startY;
    private boolean verticalGestureHandled;
    private GestureDetector gestureDetector;
    private Runnable onSingleTap;
    private Runnable onLongPress;
    private Runnable onSwipeUp;
    private Runnable onSwipeDown;

    public InterceptablePhotoView(Context context) {
        super(context);
        initGestureDetector(context);
    }

    public InterceptablePhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initGestureDetector(context);
    }

    public InterceptablePhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initGestureDetector(context);
    }

    private void initGestureDetector(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent event) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent event) {
                if (onSingleTap != null) onSingleTap.run();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent event) {
                if (isAtMinimumScale() && onLongPress != null) onLongPress.run();
            }

        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                verticalGestureHandled = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                if (!verticalGestureHandled && isAtMinimumScale()) {
                    float dx = event.getX() - startX;
                    float dy = event.getY() - startY;
                    if (Math.abs(dy) > SWIPE_DISTANCE && Math.abs(dy) > Math.abs(dx) * 1.15f) {
                        verticalGestureHandled = true;
                        if (dy < 0) {
                            if (onSwipeUp != null) onSwipeUp.run();
                        } else if (onSwipeDown != null) {
                            onSwipeDown.run();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.dispatchTouchEvent(event);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                verticalGestureHandled = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_MOVE:
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.onTouchEvent(event);
    }

    private boolean isAtMinimumScale() {
        return getScale() <= getMinimumScale() + SCALE_TOLERANCE;
    }

    public void setOnSingleTapAction(Runnable action) {
        onSingleTap = action;
    }

    public void setOnLongPressAction(Runnable action) {
        onLongPress = action;
    }

    public void setOnSwipeUpAction(Runnable action) {
        onSwipeUp = action;
    }

    public void setOnSwipeDownAction(Runnable action) {
        onSwipeDown = action;
    }
}
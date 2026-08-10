package com.sansan.picture.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.github.chrisbanes.photoview.PhotoView;

public class InterceptablePhotoView extends PhotoView {

    private float mStartX;
    private float mStartY;
    private boolean mIsDragging;

    public InterceptablePhotoView(Context context) {
        super(context);
    }

    public InterceptablePhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InterceptablePhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartX = ev.getX();
                mStartY = ev.getY();
                mIsDragging = false;
                // 让父容器不要拦截后续事件
                getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = ev.getX() - mStartX;
                float dy = ev.getY() - mStartY;
                float absDx = Math.abs(dx);
                float absDy = Math.abs(dy);

                // 判断是否满足拖拽阈值（避免微小手势触发）
                if (absDx > 5 || absDy > 5) {
                    mIsDragging = true;
                    float scale = getScale();

                    // 关键逻辑：
                    // 1. 如果当前缩放级别 > 最小缩放，说明图片被放大了，应该由 PhotoView 处理
                    // 2. 如果已经缩放到最小，且是水平滑动，则交给 ViewPager2
                    if (scale > getMinimumScale()) {
                        // 图片放大状态：拦截事件，不让 ViewPager2 滑动
                        getParent().requestDisallowInterceptTouchEvent(true);
                    } else {
                        // 图片原始大小：判断滑动方向
                        if (absDx > absDy) {
                            // 水平滑动，且图片已缩放到最小 -> 交给 ViewPager2
                            getParent().requestDisallowInterceptTouchEvent(false);
                        } else {
                            // 垂直滑动 -> PhotoView 自己处理
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.onTouchEvent(ev);
    }
}
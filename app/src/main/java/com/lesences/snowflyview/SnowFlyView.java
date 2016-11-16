package com.lesences.snowflyview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author : lesences.
 */
public class SnowFlyView extends View {
    private final int msgWhat = 0x01;
    public static final long DEFAULTDURATION = 300L;
    /**
     * the distance of snow start falling to the view top
     */
    private int initToTop;
    /**
     * the distance of snow start falling to the view left
     */
    private int initToLeft;
    /**
     * the distance of snow start falling to the view bottom
     */
    private int initToBottom;
    /**
     * the distance of snow start falling to the view right
     */
    private int initToRight;
    private float minScale;
    private float maxScale;
    private float xSpeed;
    private float ySpeed;
    private int snowCount;
    private long snowDuration;
    private List<Snow> snowList;
    private BitmapDrawable snowBitmap;
    private Matrix mtx = new Matrix();
    private ValueAnimator animator;
    private Random xRandom = new Random();
    private Random yRandom = new Random();
    private boolean isDelyStop;
    private boolean sendMsgable;
    /**
     * the range of snow start falling in the x direction
     */
    private float xWidth;
    /**
     * the range of snow start falling in the y direction
     */
    private float yHeight;


    public SnowFlyView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SnowFlyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context, attrs);
        init();
    }

    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SnowFlyView);
        int initTo = attributes.getDimensionPixelSize(R.styleable.SnowFlyView_snow_initTo, 0);
        initToTop = attributes.getDimensionPixelSize(R.styleable.SnowFlyView_snow_initToTop, 0);
        initToLeft = attributes.getDimensionPixelSize(R.styleable.SnowFlyView_snow_initToLeft, 0);
        initToBottom = attributes.getDimensionPixelSize(R.styleable.SnowFlyView_snow_initToBottom, 0);
        initToRight = attributes.getDimensionPixelSize(R.styleable.SnowFlyView_snow_initToRight, 0);
        minScale = attributes.getFloat(R.styleable.SnowFlyView_snow_minScale, 1.0f);
        maxScale = attributes.getFloat(R.styleable.SnowFlyView_snow_maxScale, 1.0f);
        xSpeed = attributes.getFloat(R.styleable.SnowFlyView_snow_xSpeed, 0.0f);
        ySpeed = attributes.getFloat(R.styleable.SnowFlyView_snow_ySpeed, 100.0f);
        snowCount = attributes.getInt(R.styleable.SnowFlyView_snow_count, 20);
        snowDuration = attributes.getInt(R.styleable.SnowFlyView_snow_duration, 0);
        snowBitmap = (BitmapDrawable) attributes.getDrawable(R.styleable.SnowFlyView_snow_bitmap);

        if (0 != initTo)
            initToTop = initToLeft = initToBottom = initToRight = initTo;
        if (minScale <= 0.0f || minScale > maxScale)
            throw new IllegalArgumentException("The minScale is illegal");
        sendMsgable = snowDuration > DEFAULTDURATION;
        attributes.recycle();
    }

    private void init() {
        /**
         * close software/hardware
         */
        setLayerType(View.LAYER_TYPE_NONE, null);
        snowList = new ArrayList<>(snowCount);
        animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(DEFAULTDURATION);
        animator.addUpdateListener(new animatorUpdateListenerImp());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        xWidth = getWidth() - initToLeft - initToRight;
        yHeight = getHeight() - initToTop - initToBottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < snowList.size(); i++) {
            Snow snow = snowList.get(i);
            mtx.setTranslate(-snow.bpWidth / 2, -snow.bpHeight / 2);
            mtx.postTranslate(snow.bpWidth / 2 + snow.x, snow.bpHeight / 2 + snow.y);
            canvas.drawBitmap(snow.snowBitmap, mtx, null);
        }
    }

    /**
     * init snowList
     */
    private void initSnows() {
        if (null == snowBitmap) return;
        snowList.clear();
        for (int i = 0; i < snowCount; i++) {
            Snow snow = new Snow(xSpeed, ySpeed, snowBitmap.getBitmap());
            snowList.add(snow);
        }
    }

    /**
     * stop animation dely
     */
    public void stopAnimationDely() {
        removeMessages();
        this.isDelyStop = true;
    }

    /**
     * stop animation and clear snowList
     */
    public void stopAnimationNow() {
        removeMessages();
        snowList.clear();
        invalidate();
        animator.cancel();
    }

    /**
     * start animation
     */
    public void startAnimation() {
        this.isDelyStop = false;
        if (animator.isRunning())
            animator.cancel();
        if (sendMsgable) {
            removeMessages();
            handler.sendEmptyMessageDelayed(msgWhat, snowDuration);
        }
        initSnows();
        animator.start();
    }

    /**
     * set the duration of animation
     */
    public void setSnowDuration(long snowDuration) {
        this.snowDuration = snowDuration;
        sendMsgable = snowDuration > DEFAULTDURATION;
    }

    /**
     * back the state of animation
     */
    public boolean isRunning() {
        return animator.isRunning();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeMessages();
        if (animator.isRunning())
            animator.cancel();
        super.onDetachedFromWindow();

    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == msgWhat)
                isDelyStop = true;
        }
    };

    private void removeMessages() {
        if (handler.hasMessages(msgWhat))
            handler.removeMessages(msgWhat);
    }


    private class animatorUpdateListenerImp implements ValueAnimator.AnimatorUpdateListener {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            for (int i = 0; i < snowList.size(); i++) {
                Snow snow = snowList.get(i);
                snow.x += snow.xSpeed;
                snow.y += snow.ySpeed;
                if (snow.x < -snow.bpWidth || snow.x > getWidth()) {
                    /**
                     *  the snow falling to the sides
                     */
                    if (isDelyStop)
                        snowList.remove(i);
                    else {
                        snow.x = randomX(snow.bpWidth);
                        snow.y = randomY(snow.bpHeight);
                    }
                } else if (snow.y > getHeight()) {
                    /**
                     * the snow falling to the bottom
                     */
                    if (isDelyStop)
                        snowList.remove(i);
                    else {
                        snow.x = randomX(snow.bpWidth);
                        snow.y = 0 - snow.bpHeight;
                    }
                }
            }
            /**
             * to prevent the animator running empty
             */
            if (snowList.size() <= 0 && animator.isRunning())
                animator.cancel();
            invalidate();
        }
    }

    class Snow {
        private float x;
        private float y;
        private float xSpeed;
        private float ySpeed;
        private int bpHeight;
        private int bpWidth;
        private Bitmap snowBitmap;
        private float BASESPEED = 100.0f;

        Snow(float xSpeed, float ySpeed, Bitmap snowBitmap) {
            float tempScale = minScale + (float) (Math.random() * (maxScale - minScale));
            this.bpHeight = (int) (snowBitmap.getHeight() * tempScale);
            this.bpWidth = (int) (snowBitmap.getWidth() * tempScale);
            this.x = randomX(bpWidth);
            this.y = randomY(bpHeight);
            /**
             * xDirection > 0 right falling
             * xDirection < 0 left falling
             * xDirection = 0 vertical falling
             */
            float xDirection = 1.0f - (float) (Math.random() * 2.0f);
            this.xSpeed = xSpeed * xDirection / BASESPEED;
            this.ySpeed = (ySpeed + ySpeed * (float) Math.random()) / BASESPEED;
            this.snowBitmap = Bitmap.createScaledBitmap(snowBitmap, bpWidth, bpHeight, true);
        }
    }

    /**
     * the x coordinate
     */
    private float randomX(int bpWidth) {
        return initToLeft + xRandom.nextFloat() * (xWidth - bpWidth);
    }

    /**
     * the y coordinate
     */
    private float randomY(int bpHeight) {
        return 0 - (initToTop + yRandom.nextFloat() * (yHeight - bpHeight));
    }
}

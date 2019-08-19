package com.example.canvasdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

public class SplashView extends View {

    //旋转圆的画笔
    private Paint mPaint;
    //属性动画
    private ValueAnimator mValueAnimator;
    //扩散圆的画笔
    private Paint mHolePaint;

    //背景色
    private int mBackgroundColor = Color.WHITE;
    private int[] mCircleColors;

    //表示旋转圆的中心坐标
    private float mCenterX;
    private float mCenterY;
    //表示斜对角线长度的一半，扩散圆最大半径
    private float mDistance;

    //6个小球的半径
    private float mCircleRadius = 18;
    //旋转大圆的半径
    private float mRotateRedius = 90;
    //当前大圆的半径
    private float mCurrentRotateRadius = mRotateRedius;
    //表示旋转动画的时长
    private int mRotateDuration = 1200;
    //当前大圆的旋转角度
    private float mCurrentRotateAngle =  0F;
    //扩散圆的半径
    private float mCurrentHoleRadius = 0F;

    public SplashView(Context context) {
        this(context, null);
    }

    public SplashView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mHolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHolePaint.setStyle(Paint.Style.STROKE);
        mHolePaint.setColor(mBackgroundColor);

        mCircleColors = context.getResources().getIntArray(R.array.splash_circle_colors);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mState == null) {
            mState = new RotateState();
        }
        mState.drawState(canvas);
    }

    private SplashState mState;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w * 1f / 2;
        mCenterY = h * 1f / 2;
        mDistance = (float) (Math.hypot(w,h) / 2);
    }

    private abstract class SplashState {
        abstract void drawState(Canvas canvas);
    }

    //1.旋转
    private class RotateState extends SplashState {

        private RotateState() {
            //动画时的变化范围
            mValueAnimator = ValueAnimator.ofFloat(0, (float) (Math.PI * 2));
            //设置循环次数,设置为INFINITE表示无限循环
            mValueAnimator.setRepeatCount(1);
            //设置动画时长，单位是毫秒
            mValueAnimator.setDuration(mRotateDuration);
            //设置插值器
            mValueAnimator.setInterpolator(new LinearInterpolator());
            mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentRotateAngle = (float) animation.getAnimatedValue();
                    invalidate();//会使得onDraw方法重新执行--- 刷新
                }
            });
            mValueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mState = new MerginState();
                }
            });
            mValueAnimator.start();

        }

        @Override
        void drawState(Canvas canvas) {
            //绘制背景
            drawBackground(canvas);
            //绘制6个小球
            drawCircles(canvas);

        }
    }

    private void drawCircles(Canvas canvas) {
        //两个圆之间的角度  pi = 180°
        float rotateAngle = (float) (Math.PI * 2 / mCircleColors.length);
        for (int i = 0; i < mCircleColors.length; i++) {
            // x = r * cos(a) + centX;
            // y = r * sin(a) + centY;
            float angle = i * rotateAngle + mCurrentRotateAngle;
            //小圆x坐标
            float cx = (float) (Math.cos(angle) * mCurrentRotateRadius + mCenterX);
            //小圆y坐标
            float cy = (float) (Math.sin(angle) * mCurrentRotateRadius + mCenterY);
            mPaint.setColor(mCircleColors[i]);
            canvas.drawCircle(cx, cy, mCircleRadius, mPaint);
        }
    }

    private void drawBackground(Canvas canvas) {
        if (mCurrentHoleRadius > 0){
            //绘制空心圆
            float strokeWidth = mDistance - mCurrentHoleRadius;
            float radius = strokeWidth / 2 + mCurrentHoleRadius;
            //Set the width for stroking. 设置描边的宽度（也就是控制画笔的粗细）
            mHolePaint.setStrokeWidth(strokeWidth);
            canvas.drawCircle(mCenterX,mCenterY,radius,mHolePaint);
        }else {
            canvas.drawColor(mBackgroundColor);
        }

    }

    //2.扩散聚合
    private class MerginState extends SplashState{

        public MerginState() {
            mValueAnimator = ValueAnimator.ofFloat(mCircleRadius,mRotateRedius);
            mValueAnimator.setDuration(mRotateDuration);
            mValueAnimator.setInterpolator(new OvershootInterpolator(10f));
            mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentRotateRadius = (float) animation.getAnimatedValue();
                    invalidate();//会使得onDraw方法重新执行--- 刷新
                }
            });
            mValueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mState = new ExpandState();
                }
            });
            mValueAnimator.reverse();//反向执行

        }

        @Override
        void drawState(Canvas canvas) {
            drawBackground(canvas);
            drawCircles(canvas);
        }
    }

    //3.水波纹
    private class ExpandState extends SplashState{

        public ExpandState() {
            mValueAnimator = ValueAnimator.ofFloat(mCircleRadius,mDistance);
            mValueAnimator.setDuration(mRotateDuration);
            mValueAnimator.setInterpolator(new LinearInterpolator());
            mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentHoleRadius = (float) animation.getAnimatedValue();
                    invalidate();//会使得onDraw方法重新执行--- 刷新
                }
            });

            mValueAnimator.start();

        }

        @Override
        void drawState(Canvas canvas) {
            drawBackground(canvas);
        }
    }

}

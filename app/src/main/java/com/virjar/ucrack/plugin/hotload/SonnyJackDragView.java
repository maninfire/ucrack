package com.virjar.ucrack.plugin.hotload;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.virjar.ucrack.host.CircleImageView;
import com.virjar.xposed_extention.SingletonXC_MethodHook;
import com.virjar.xposed_extention.XposedReflectUtil;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * 可拖动的悬浮按钮
 * Created by linqs on 2017/12/21.
 * <p>
 * https://github.com/linqssonny/DragView
 */
public class SonnyJackDragView implements View.OnTouchListener {

    private static Set<Class> hookedClass = Sets.newConcurrentHashSet();
    private static ResumeHookCallBack resumeHookCallBack = new ResumeHookCallBack();
    private static Map<Activity, SonnyJackDragView> dragViewMap = Maps.newHashMap();

    private static class ResumeHookCallBack extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            super.afterHookedMethod(param);
            Object hasCalled = param.getObjectExtra("hasCalled");
            if (hasCalled != null) {
                return;
            }
            param.setObjectExtra("hasCalled", "true");
            final Activity activity = (Activity) param.thisObject;

            SonnyJackDragView sonnyJackDragView = dragViewMap.get(activity);
            if (sonnyJackDragView != null) {
                nowPanel = sonnyJackDragView;
                return;
            }

            CircleImageView imageView = new CircleImageView(activity);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(activity, "点击了...", Toast.LENGTH_SHORT).show();
                }
            });
            sonnyJackDragView = new SonnyJackDragView.Builder()
                    .setActivity(activity)
                    .setDefaultLeft(30)
                    .setSize(120)
                    .setView(imageView)
                    .build();
            dragViewMap.put(activity, sonnyJackDragView);
            nowPanel = sonnyJackDragView;
        }
    }

    static void enableDragViewPanel() {
        XposedBridge.hookAllConstructors(Activity.class, new SingletonXC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object activity = param.thisObject;
                if (activity == null) {
                    return;
                }
                XposedBridge.log("hook class weijia " + activity.getClass());
                Class<?> aClass = activity.getClass();
                if (hookedClass.contains(aClass)) {
                    return;
                }
                hookedClass.add(aClass);
                //避免重复hook，所以使用唯一对象，因为xposed内部是一个set来存储钩子函数，唯一对象可以消重
                //XposedHelpers.findAndHookMethod(aClass, "onResume", resumeHookCallBack);
                XposedReflectUtil.findAndHookMethodWithSupperClass(aClass, "onResume", resumeHookCallBack);
            }
        });
    }

    private static SonnyJackDragView nowPanel = null;

    private Builder mBuilder;

    private int mStatusBarHeight, mScreenWidth, mScreenHeight;

    //手指按下位置
    private int mStartX, mStartY, mLastX, mLastY;
    private boolean mTouchResult = false;

    private SonnyJackDragView(SonnyJackDragView.Builder builder) {
        mBuilder = builder;
        initDragView();
    }

    public View getDragView() {
        return mBuilder.view;
    }

    public Activity getActivity() {
        return mBuilder.activity;
    }


    private void initDragView() {
        if (null == getActivity()) {
            throw new NullPointerException("the activity is null");
        }
        if (null == mBuilder.view) {
            throw new NullPointerException("the dragView is null");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && mBuilder.activity.isDestroyed()) {
            return;
        }

        //屏幕宽高
        WindowManager windowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        if (null != windowManager) {
            DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
            mScreenWidth = displayMetrics.widthPixels;
            mScreenHeight = displayMetrics.heightPixels;
        }

        //状态栏高度
        Rect frame = new Rect();
        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        mStatusBarHeight = frame.top;
        if (mStatusBarHeight <= 0) {
            try {
                Class<?> c = Class.forName("com.android.internal.R$dimen");
                Object obj = c.newInstance();
                Field field = c.getField("status_bar_height");
                int x = Integer.parseInt(field.get(obj).toString());
                mStatusBarHeight = getActivity().getResources().getDimensionPixelSize(x);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        if (mBuilder.defaultTop < 0) {
            mBuilder.defaultTop = mScreenHeight / 2;
        }

        int left = mBuilder.defaultLeft;
        FrameLayout.LayoutParams layoutParams = createLayoutParams(left, mStatusBarHeight + mBuilder.defaultTop, 0, 0);
        FrameLayout rootLayout = (FrameLayout) getActivity().getWindow().getDecorView();
        rootLayout.addView(getDragView(), layoutParams);
        getDragView().setOnTouchListener(this);
    }

    private static SonnyJackDragView createDragView(SonnyJackDragView.Builder builder) {
        if (null == builder) {
            throw new NullPointerException("the param builder is null when execute method createDragView");
        }
        if (null == builder.activity) {
            throw new NullPointerException("the activity is null");
        }
        if (null == builder.view) {
            throw new NullPointerException("the view is null");
        }
        return new SonnyJackDragView(builder);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchResult = false;
                mStartX = mLastX = (int) event.getRawX();
                mStartY = mLastY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                int left, top, right, bottom;
                int dx = (int) event.getRawX() - mLastX;
                int dy = (int) event.getRawY() - mLastY;
                left = v.getLeft() + dx;
                if (left < 0) {
                    left = 0;
                }
                right = left + v.getWidth();
                if (right > mScreenWidth) {
                    right = mScreenWidth;
                    left = right - v.getWidth();
                }
                top = v.getTop() + dy;
                if (top < mStatusBarHeight + 2) {
                    top = mStatusBarHeight + 2;
                }
                bottom = top + v.getHeight();
                if (bottom > mScreenHeight) {
                    bottom = mScreenHeight;
                    top = bottom - v.getHeight();
                }
                v.layout(left, top, right, bottom);
                mLastX = (int) event.getRawX();
                mLastY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
                //这里需设置LayoutParams，不然按home后回再到页面等view会回到原来的地方
                v.setLayoutParams(createLayoutParams(v.getLeft(), v.getTop(), 0, 0));
                float endX = event.getRawX();
                float endY = event.getRawY();
                if (Math.abs(endX - mStartX) > 5 || Math.abs(endY - mStartY) > 5) {
                    //防止点击的时候稍微有点移动点击事件被拦截了
                    mTouchResult = true;
                }

                if (mTouchResult &&
                        (Math.abs(mScreenWidth - endX) / mScreenWidth < 0.3
                                || endX / mScreenWidth < 0.3)) {
                    //屏幕边沿
                    moveNearEdge();
                }
                break;
        }
        return mTouchResult;
    }

    /**
     * 移至最近的边沿
     */
    private void moveNearEdge() {
        int left = getDragView().getLeft();
        int lastX;
        if (left + getDragView().getWidth() / 2 <= mScreenWidth / 2) {
            lastX = 0;
        } else {
            lastX = mScreenWidth - getDragView().getWidth();
        }
        ValueAnimator valueAnimator = ValueAnimator.ofInt(left, lastX);
        valueAnimator.setDuration(1000);
        valueAnimator.setRepeatCount(0);
        valueAnimator.setInterpolator(new BounceInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int left = (int) animation.getAnimatedValue();
                getDragView().setLayoutParams(createLayoutParams(left, getDragView().getTop(), 0, 0));
            }
        });
        valueAnimator.start();
    }

    private FrameLayout.LayoutParams createLayoutParams(int left, int top, int right, int bottom) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(mBuilder.size, mBuilder.size);
        layoutParams.setMargins(left, top, right, bottom);
        return layoutParams;
    }

    public static class Builder {
        private Activity activity;
        private int size = FrameLayout.LayoutParams.WRAP_CONTENT;
        private int defaultTop = -1;
        private int defaultLeft = 0;
        private View view;

        public Builder setActivity(Activity activity) {
            this.activity = activity;
            return this;
        }

        public Builder setSize(int size) {
            this.size = size;
            return this;
        }

        public Builder setDefaultTop(int top) {
            this.defaultTop = top;
            return this;
        }

        public Builder setDefaultLeft(int left) {
            this.defaultLeft = left;
            return this;
        }

        public Builder setView(View view) {
            this.view = view;
            return this;
        }

        public SonnyJackDragView build() {
            return createDragView(this);
        }
    }
}

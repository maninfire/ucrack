package com.virjar.ucrack.plugin;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Message;
import android.util.Log;

import com.virjar.xposed_extention.ClassLoadMonitor;
import com.virjar.xposed_extention.SharedObject;
import com.virjar.xposed_extention.SingletonXC_MethodHook;

import org.apache.commons.lang3.StringUtils;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by virjar on 2018/4/19.<br>
 * 爱加密反调试的各种策略统一拆解
 */

public class IJiaMiPrevent {

    /**
     * 阻止爱加密检测xposed后自杀,请注意一个爱加密的app再启动的时候，会产生两个进程（package相同，processName不同）。
     */
    public static void preventXposedTest() {
        ClassLoadMonitor.addClassLoadMonitor("com.ijm.antihook.IjmReceiver"
                , new ClassLoadMonitor.OnClassLoader() {
                    @Override
                    public void onClassLoad(Class clazz) {
                        //Log.i("weijia", "find ijm framwork: " + LogUtil.getTrack());
                        XposedBridge.hookAllMethods(clazz, "onReceive"
                                , new SingletonXC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        Log.e("weijia", "ijm receive message");
                                        param.setResult(null);
                                    }
                                });
                    }
                });
    }

    /**
     * 阻止爱加密检测xposed后自杀，这个方案不靠谱。他是通过拦截事件循环的方式，将会导致app失去对界面的响应（业务activity已经压栈，失去焦点）
     */
    @SuppressLint("PrivateApi")
    public static void preventXposedTest2() {
        try {
            Class<?> activityThreadHandler = SharedObject.loadPackageParam.classLoader.loadClass("android.app.ActivityThread$H");
            XposedHelpers.findAndHookMethod(activityThreadHandler, "handleMessage", Message.class, new SingletonXC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Message message = (Message) param.args[0];
                    //android.app.ActivityThread.H#LAUNCH_ACTIVITY
                    if (message.what != 100) {
                        return;
                    }
                    //android.app.ActivityThread.ActivityClientRecord
                    Object activityClientRecord = message.obj;

                    Intent intent = (Intent) XposedHelpers.getObjectField(activityClientRecord, "intent");
                    ComponentName component = intent.getComponent();
                    if (component != null && StringUtils.equalsIgnoreCase(component.getClassName(), "com.ijm.antihook.IjmActivity")) {
                        param.setResult(null);
                        return;
                    }
                    ActivityInfo activityInfo = (ActivityInfo) XposedHelpers.getObjectField(activityClientRecord, "activityInfo");
                    if (activityInfo != null && StringUtils.equalsIgnoreCase(activityInfo.targetActivity, "com.ijm.antihook.IjmActivity")) {
                        param.setResult(null);
                    }
                }
            });
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

package com.virjar.ucrack.plugin;

import android.os.Process;
import android.util.Log;

import com.virjar.ucrack.plugin.hotload.PluginNativeLibLoader;
import com.virjar.xposed_extention.SingletonXC_MethodHook;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by virjar on 2018/5/15.<br>
 * 监控程序退出
 */

public class ExitMonitor {
    private static final String TAG = "ExistMonitor";

    public static void monitorAppExit() {
        //todo nothing
    }

    static {
        PluginNativeLibLoader.makeSureNativeLibLoaded();
        monitorAppExitInternal();
    }

    private static void monitorAppExitInternal() {
        XposedHelpers.findAndHookMethod(Process.class, "killProcess", int.class, new SingletonXC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //Process.killProcess(Process.myPid());
                int myPid = Process.myPid();
                if (((int) param.args[0]) == myPid) {
                    Log.w(TAG, "app killSelf：" + LogUtil.getTrack());
                    return;
                }
                Log.i(TAG, "app kill process:" + param.args[0] + "   trace:" + LogUtil.getTrack());
            }
        });
        XposedHelpers.findAndHookMethod(Runtime.class, "exit", int.class, new SingletonXC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //Runtime.getRuntime().exit(-1);
                Log.w(TAG, "app killSelf：" + LogUtil.getTrack());
            }
        });

        XposedHelpers.findAndHookMethod(System.class, "exit", int.class, new SingletonXC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.w(TAG, "app killSelf：" + LogUtil.getTrack());
            }
        });

        //TODO native monitor && interrupt monitor
        monitorAppExitNative();
    }

    private static native void monitorAppExitNative();
}

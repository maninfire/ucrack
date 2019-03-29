package com.virjar.ucrack.plugin;

import android.util.Log;

import com.virjar.xposed_extention.ClassLoadMonitor;
import com.virjar.xposed_extention.SingletonXC_MethodHook;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by virjar on 2018/8/4.
 */

public class AliPayHook {
    public static void monitorAliPaySDKHook() {
        //do nothing
    }

    static {
        monitorAliPaySDKHookInternal();
    }

    private static void monitorAliPaySDKHookInternal() {
        ClassLoadMonitor.addClassLoadMonitor("com.alipay.sdk.app.PayTask", new ClassLoadMonitor.OnClassLoader() {
            @Override
            public void onClassLoad(Class clazz) {
                XposedHelpers.findAndHookMethod(clazz, "payV2", String.class, boolean.class, new SingletonXC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.i("alipay_sdk_hook", "发起支付宝付款，调用堆栈为：" + LogUtil.getTrack());
                        Log.i("alipay_sdk_hook", "支付宝支付报文:" + param.args[0]);
                    }
                });
            }
        });
    }
}

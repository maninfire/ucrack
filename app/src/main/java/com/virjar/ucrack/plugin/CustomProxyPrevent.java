package com.virjar.ucrack.plugin;

import android.util.Log;


import com.virjar.xposed_extention.SingletonXC_MethodHook;

import org.apache.commons.lang3.StringUtils;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by virjar on 2018/4/25.<br>
 * 阻止修改系统代理导致无法抓包
 */

public class CustomProxyPrevent {
    static {
        preventInternal();
    }

    public static void prevent() {
    }

    private static void preventInternal() {
        XposedHelpers.findAndHookMethod(System.class, "setProperty",
                String.class, String.class, new SingletonXC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        if (!StringUtils.startsWith(key, "http")) {
                            return;
                        }
                        if (StringUtils.equals(key, "http.proxyHost")
                                || StringUtils.equals(key, "http.proxyPort")
                                || StringUtils.equals(key, "https.proxyHost")
                                || StringUtils.equals(key, "https.proxyPort")) {
                            Log.i("CustomProxyPrevent", "key:" + key + "  value:" + param.args[1]);
                            param.setResult(null);
                        }
                    }
                });
    }
}

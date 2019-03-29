package com.virjar.ucrack.plugin;

import android.os.Build;

import com.virjar.xposed_extention.SingletonXC_MethodHook;
import com.virjar.xposed_extention.XposedReflectUtil;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by virjar on 2017/12/7.
 */
public class WebViewDebuggerController {

    public synchronized static void enableDebug(final ClassLoader classLoader, final String packageName) {

        //hook需要放在webview创建之后，因为webview内部会持有context，在构造前context为null，导致空指针发生
        Class<?> webViewClass = XposedHelpers.findClass("android.webkit.WebView", classLoader);
        XposedBridge.hookAllConstructors(webViewClass, new SingletonXC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    XposedBridge.log("开启浏览器调试...");
                    if (Build.VERSION.SDK_INT >= 19) {
                        //启用浏览器调试，可以在Android里面调试webview的代码
                        Class<?> webViewClass = XposedHelpers.findClass("android.webkit.WebView", classLoader);
                        XposedHelpers.callStaticMethod(webViewClass, "setWebContentsDebuggingEnabled", Boolean.TRUE);
                        XposedReflectUtil.findAndHookOneMethod(webViewClass, "setWebContentsDebuggingEnabled", new SingletonXC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.args[0] = Boolean.TRUE;
                                //make sure webview debug disable in the future
                            }
                        });
                    }
                } catch (Exception e) {
                    XposedBridge.log("浏览器调试开启失败");
                    XposedBridge.log(e);
                }
            }
        });
    }
}

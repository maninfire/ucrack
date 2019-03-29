package com.virjar.ucrack.plugin.socket;

import com.google.common.collect.Sets;
import com.virjar.ucrack.plugin.LogUtil;
import com.virjar.xposed_extention.SingletonXC_MethodHook;
import com.virjar.xposed_extention.XposedReflectUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Set;

import de.robv.android.xposed.XposedBridge;

/**
 * Created by virjar on 2018/4/26.<br>
 */

public class SocketMonitor {
    public static void startMonitor() {
    }

    static {
        startMonitorInternal();
    }

    private static Set<Class> monitoredClass = Sets.newConcurrentHashSet();

    private static void startMonitorInternal() {
        LogUtil.startRecord();
        //to find all class that extend java.net.Socket
        XposedBridge.hookAllConstructors(Socket.class, new SingletonXC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Class<?> theSocketClass = param.thisObject.getClass();
                if (monitoredClass.contains(theSocketClass)) {
                    return;
                }
                synchronized (SocketMonitor.class) {
                    if (monitoredClass.contains(theSocketClass)) {
                        return;
                    }
                    monitorSocketClass(theSocketClass);
                    monitoredClass.add(theSocketClass);
                }
            }
        });
    }

    private static void monitorSocketClass(Class socketClass) {
        //monitor socket input,


        XposedReflectUtil.findAndHookOneMethod(socketClass, "getInputStream", new SingletonXC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                InputStream inputStream = (InputStream) param.getResult();
                if (inputStream instanceof InputStreamWrapper) {
                    return;
                }
                param.setResult(new InputStreamWrapper(inputStream, (Socket) param.thisObject));
            }
        });

        //monitor socket output,
        XposedReflectUtil.findAndHookOneMethod(socketClass, "getOutputStream", new SingletonXC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                OutputStream outputStream = (OutputStream) param.getResult();
                if (outputStream instanceof OutputStreamWrapper) {
                    return;
                }
                param.setResult(new OutputStreamWrapper(outputStream, (Socket) param.thisObject));
            }
        });
    }
}

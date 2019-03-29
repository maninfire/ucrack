package com.virjar.ucrack.plugin;

import com.virjar.ucrack.plugin.hotload.PluginNativeLibLoader;

/**
 * Created by virjar on 2018/6/1.<br>在虚拟机层面拦截UncaughtException
 */

public class UncaughtExceptionInterceptor {
    static {
        PluginNativeLibLoader.makeSureNativeLibLoaded();
        interceptNative();
    }

    /**
     * 如果一个线程异常退出（指线程执行完成，没有对她进行exception trycatch），此时不管是否挂载了UncaughtExceptionHandler，
     * 我们都进行一次异常堆栈打印，因为java层的UncaughtExceptionHandler可能由于将该异常吞并，这个时候可能只知道闪退，不知道为何闪退<br>
     * native的实现方案是，拦截线程执行结束处理函数，在UncaughtExceptionHandler发生作用之前，输出线程环境中存在的异常（如果存在异常）
     */
    private static native void interceptNative();


//    private static class InnerUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
//
//        @Override
//        public void uncaughtException(Thread t, Throwable e) {
//            //先打印日志,主要是虚拟机可能不打印日志,这样真的蛋疼了
//            Log.e("weijia", "uncaught Exception for thread:" + t.getName(), e);
//            t.getUncaughtExceptionHandler().uncaughtException(t, e);
//        }
//    }

    // private static InnerUncaughtExceptionHandler innerUncaughtExceptionHandler = new InnerUncaughtExceptionHandler();

    /**
     * 这个功能暂时处于无法使用的状态
     */
    public synchronized static void registerInterceptor() {
        //TODO noting
//        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();
//        if (uncaughtExceptionHandler == innerUncaughtExceptionHandler) {
//            return;
//        }
//        Log.e("weijia", "replace UncaughtExceptionHandler");
//        Thread.currentThread().setUncaughtExceptionHandler(innerUncaughtExceptionHandler);
    }

}

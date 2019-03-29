package com.virjar.ucrack.plugin;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.virjar.xposed_extention.SingletonXC_MethodHook;
import com.virjar.xposed_extention.XposedReflectUtil;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by virjar on 2018/7/5.
 * <br>
 * 不使用继承方案，这可能导致类型转换异常
 */

public class ThreadPoolHookV2 {
    //每个线程，绑定一个记录堆栈的Throwable；  注意，千万不要使用InheritableThreadLocal
    private static ThreadLocal<Throwable> stackTraceThreadLocal = new ThreadLocal<>();
    private static Map<Object, Throwable> throwableMap = Maps.newConcurrentMap();

    static {
        monitorInternal();
    }

    public static void monitorThreadPool() {
        //do nothing
    }


    public static Throwable stackTraceChain() {
        Throwable submitEntry = stackTraceThreadLocal.get();
        if (submitEntry == null) {
            return new Throwable();
        }
        return new Throwable(submitEntry);
    }


    private static void monitorInternal() {
        //线程池，runnable会被复用
        XposedHelpers.findAndHookMethod(ThreadPoolExecutor.class, "execute", Runnable.class, new SingletonXC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Runnable target = (Runnable) param.args[0];
                Throwable parentStackTrace = stackTraceThreadLocal.get();
                Throwable theStackTrace;
                if (parentStackTrace != null) {
                    theStackTrace = new Throwable("parent submit task stack entry", parentStackTrace);
                } else {
                    theStackTrace = new Throwable("parent submit task stack entry");
                }
                throwableMap.put(target, theStackTrace);
                registryRunnableMonitory(target);
            }
        });

        //创建线程的方式
        Method threadInitMethod = null;
        try {
            threadInitMethod = Thread.class.getDeclaredMethod("init", ThreadGroup.class, Runnable.class, String.class, long.class);
            //threadInitMethod = XposedHelpers.findMethodExactIfExists(Thread.class, "init", ThreadGroup.class, Runnable.class, String.class, long.class);
        } catch (Exception e) {
            //ignore
        }
        if (threadInitMethod == null) {
            try {
                threadInitMethod = Thread.class.getDeclaredMethod("create", ThreadGroup.class, Runnable.class, String.class, long.class);
                //threadInitMethod = XposedHelpers.findMethodExactIfExists(Thread.class, "init", ThreadGroup.class, Runnable.class, String.class, long.class);
            } catch (Exception e) {
                //ignore
            }
        }
        if (threadInitMethod != null) {
            monitorThreadInitMethod(threadInitMethod);
        }
    }

    private static void monitorThreadInitMethod(Method threadInitMethod) {
        XposedBridge.hookMethod(threadInitMethod, new SingletonXC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Runnable target = (Runnable) param.args[1];
                //传递了runnable的方式创建线程
                Throwable parentStackTrace = stackTraceThreadLocal.get();
                Throwable theStackTrace;
                if (parentStackTrace != null) {
                    theStackTrace = new Throwable("parent submit task stack entry", parentStackTrace);
                } else {
                    theStackTrace = new Throwable("parent submit task stack entry");
                }
                if (target != null) {
                    throwableMap.put(target, theStackTrace);
                    registryRunnableMonitory(target);
                } else if (param.thisObject instanceof Runnable) {
                    throwableMap.put(param.thisObject, theStackTrace);
                    registryRunnableMonitory((Runnable) param.thisObject);
                }
            }
        });
    }


    private static Set<Class<? extends Runnable>> monitoredRunnable = Sets.newConcurrentHashSet();

    private static void registryRunnableMonitory(Runnable runnable) {
        Class<? extends Runnable> runnableClass = runnable.getClass();
        if (monitoredRunnable.contains(runnableClass)) {
            return;
        }
        synchronized (ThreadPoolHookV2.class) {
            XposedReflectUtil.findAndHookMethodWithSupperClass(runnableClass, "run", new SingletonXC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    //所有线程，运行之前，设置堆栈追踪的线程变量
                    Throwable throwable = throwableMap.get(param.thisObject);
                    if (throwable == null) {
                        return;
                    }
                    stackTraceThreadLocal.set(throwable);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Throwable throwable = throwableMap.get(param.thisObject);
                    if (throwable == null) {
                        return;
                    }
                    throwableMap.remove(param.thisObject);
                    stackTraceThreadLocal.remove();
                }
            });
            monitoredRunnable.add(runnableClass);
        }
    }

}

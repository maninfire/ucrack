package com.virjar.ucrack.plugin.socrack;

import com.virjar.ucrack.plugin.hotload.PluginNativeLibLoader;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by virjar on 2018/5/17.<br>
 * so相关操作
 */
public class SoInfoHelper {
    static {
        PluginNativeLibLoader.makeSureNativeLibLoaded();
    }

    /**
     * 将会得到当前加载的所有lib库
     *
     * @return 所有lib库的名字，不包含路径
     */
    public static native List<String> allLoadLibs();

    private static AtomicBoolean hasNativeFunctionRegsted = new AtomicBoolean(false);

    public static void monitorRegisterNatives() {
        if (hasNativeFunctionRegsted.compareAndSet(false, true)) {
            monitorRegisterNativesNative();
        }
    }

    public static ByteBuffer dumpSo(Method theNativeMethod) {
        int nativeFlag = theNativeMethod.getModifiers() & Modifier.NATIVE;
        if (nativeFlag == 0) {
            throw new IllegalStateException("the method " + theNativeMethod + " is not a native method");
        }
        return dumpSoNative(theNativeMethod.getDeclaringClass(), theNativeMethod.getName(), getSignature(theNativeMethod));
    }

    private static native ByteBuffer dumpSoNative(Class<?> declaringClass, String methodName, String Signature);

    /**
     * 监控所有jni注册method方法的,将会输出日志
     */
    private static native void monitorRegisterNativesNative();

    /**
     * 传递一个method，得到该method对应的so地址,
     * 在使用该方法之前，请确保该method的native层函数被执行过，如果该方法没有被触发调用，那么方法挂在地址将会是虚拟机内部的方法寻找函数
     * 由于虚拟机参数检查流程复杂，我无法强制进行native初始化代码的调用
     *
     * @param theNativeMethod Method对象，该method必须是native
     * @return so的文件地址, 如果加壳了，该地址可能无法解析出来
     */
    public static String soLocation(Method theNativeMethod) {
        int nativeFlag = theNativeMethod.getModifiers() & Modifier.NATIVE;
        if (nativeFlag == 0) {
            throw new IllegalStateException("the method " + theNativeMethod + " is not a native method");
        }
        return soLocationNative(theNativeMethod.getDeclaringClass(), theNativeMethod.getName(), getSignature(theNativeMethod));
    }

    private static native String soLocationNative(Class<?> declaringClass, String methodName, String Signature);

    private static String getSignature(Method method) {
        StringBuilder result = new StringBuilder();

        result.append('(');
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (Class<?> parameterType : parameterTypes) {
            result.append(Types.getSignature(parameterType));
        }
        result.append(')');
        result.append(Types.getSignature(method.getReturnType()));

        return result.toString();
    }
}

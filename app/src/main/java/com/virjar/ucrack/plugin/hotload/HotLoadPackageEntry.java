package com.virjar.ucrack.plugin.hotload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Process;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.virjar.ucrack.BuildConfig;
import com.virjar.ucrack.plugin.AliPayHook;
import com.virjar.ucrack.plugin.ExitMonitor;
import com.virjar.ucrack.plugin.IJiaMiPrevent;
import com.virjar.ucrack.plugin.JustTrustMe;
import com.virjar.ucrack.plugin.LogUtil;
import com.virjar.ucrack.plugin.SharePreferenceConfigHolder;
import com.virjar.ucrack.plugin.ThreadPoolHookV2;
import com.virjar.ucrack.plugin.ToolConstant;
import com.virjar.ucrack.plugin.WebViewDebuggerController;
import com.virjar.ucrack.plugin.droidsword.DroidSword;
import com.virjar.ucrack.plugin.socket.SocketMonitor;
import com.virjar.ucrack.plugin.socrack.SoInfoHelper;
import com.virjar.xposed_extention.ClassScanner;
import com.virjar.xposed_extention.SharedObject;
import com.virjar.xposed_extention.XposedExtensionInstaller;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import javax.annotation.Nullable;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by virjar on 2017/12/21.<br/>插件热加载器
 */

public class HotLoadPackageEntry {
    private static final String TAG = "HotPluginLoader";
    //private static MyServer server = new MyServer();

    //这里需要通过反射调用，HotLoadPackageEntry的entry的全路径不允许改变（包括方法签名），方法签名是xposed回调和热加载器的桥梁，需要满足调用接口规范
    //但是这个类的其他地方是可以修改的，因为这个代码已经是在最新插件apk的类加载器里面执行了
    @SuppressWarnings("unused")
    public static boolean entry(ClassLoader masterClassLoader, ClassLoader pluginClassLoader, Context context, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (StringUtils.equalsIgnoreCase(loadPackageParam.packageName, BuildConfig.APPLICATION_ID)) {
            return false;
        }

        //将一批有用的对象放置到静态区域，方便使用
        SharedObject.context = context;
        SharedObject.loadPackageParam = loadPackageParam;
//        SharedObject.masterClassLoader = masterClassLoader;
//        SharedObject.pluginClassLoader = pluginClassLoader;

        XSharedPreferences xSharedPreferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, ToolConstant.configPrefix + loadPackageParam.packageName);

        Log.i(loadPackageParam.processName, "processNameFilterFlag");
        //PID=`logcat -d -s "com.air.sz" | tail -n 1 | sed 's/.*( *[0−9]∗).*/\1/'` && logcat -v time | grep --color $PID
        Log.i(TAG, "package: " + loadPackageParam.packageName + " has bean hooked");

        //如果总开关没有勾选则默认为false，直接退出
        if (!SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.SWITCH, xSharedPreferences)) {
            return false;
        }

        XposedExtensionInstaller.initComponent();

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.UNPACK_VERSION_1, xSharedPreferences)) {
            //一代可脱壳方案
            DumpDexController.registryDump1(loadPackageParam);
        }

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.UNPACK_VERSION_3, xSharedPreferences)) {
            //Log.i(TAG, "third generation unpack");
            //三代壳，自动脱壳
            DumpDexController.registerDump3(loadPackageParam);
        }

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.LOG, xSharedPreferences)) {
            //开启日志
            //LogUtil.start(loadPackageParam.packageName, loadPackageParam.processName);
            LogUtil.startRecord();
        }

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.NETDATA_PRINT, xSharedPreferences)) {
            //开启网络请求堆栈输出（便于爆破）
//            NetDataPrinter.hook(null, true);
            //新版本的网络监控,还未完成，先使用老版本功能
            SocketMonitor.startMonitor();
        }

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.WEB_VIEW, xSharedPreferences)) {
            //开启webview调试 （便于分析h5实现的加解密）https://www.cnblogs.com/wmhuang/p/7396150.html
            Log.i(TAG, "打开了webView功能");
            WebViewDebuggerController.enableDebug(masterClassLoader, loadPackageParam.packageName);
        }

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.D_SWORD, xSharedPreferences)) {
            Log.i(TAG, "打开了DroidSword功能");
            //开启DroidSword
            DroidSword.startDroidSword();
        }

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.TRUST_ME, xSharedPreferences)) {
            //解决 certificate pinning
            //see https://github.com/moxie0/AndroidPinning
            JustTrustMe.trustAllCertificate();
        }

        //ShowDeviceInfo.showInfo();

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.MONITOR_STACK, xSharedPreferences)) {
            //监控所有异步任务的堆栈，彻底解决通过堆栈定位代码，遇到异步无法定位的尴尬
            // ThreadPoolHook.monitorThreadPool();
            ThreadPoolHookV2.monitorThreadPool();
        }

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.KILL_SELF, xSharedPreferences)) {
            ExitMonitor.monitorAppExit();
        }

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.ALIAPY_STACK_TRACE, xSharedPreferences)) {
            //支付宝发生付款的时候，打印付款逻辑堆栈
            AliPayHook.monitorAliPaySDKHook();
        }

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.NATIVE_FUNTION_REGISTERY, xSharedPreferences)) {
            SoInfoHelper.monitorRegisterNatives();
        }

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.IJIAMI_PREVENT, xSharedPreferences)) {
            IJiaMiPrevent.preventXposedTest();
        }

        if (SharePreferenceConfigHolder.getCheckWithFunc(SharePreferenceConfigHolder.SonnyJackDragView, xSharedPreferences)) {
            SonnyJackDragView.enableDragViewPanel();
        }

//        XposedHelpers.findAndHookMethod(Debug.class, "startMethodTracingDdms", int.class, int.class, boolean.class, int.class, new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                int size = (Integer) param.args[0];
//                if (size == 8388608) {
//                    //https://github.com/panhongwei/XposedXDebug/blob/master/XposedXDebug/src/com/example/xposedxdebug/Hook.java
//                    param.args[0] = size * 10;
//                }
//            }
//        });

        //执行所有自定义的回调钩子函数
        List<XposedHotLoadCallBack> allCallBack = findAllCallBackV2();
        for (XposedHotLoadCallBack xposedHotLoadCallBack : allCallBack) {
            if (xposedHotLoadCallBack == null) {
                continue;
            }
            try {
                XposedBridge.log("执行回调: " + xposedHotLoadCallBack.getClass());
                xposedHotLoadCallBack.onXposedHotLoad();
            } catch (Exception e) {
                XposedBridge.log(e);
            }
        }

        //exitIfMasterReInstall(context);
        return true;
    }

    private static void exitIfMasterReInstall(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        // intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (StringUtils.isBlank(action)) {
                    return;
                }
                String packageName = getPackageName(intent);
                if (packageName == null)
                    return;
                if (!StringUtils.equalsIgnoreCase(packageName, BuildConfig.APPLICATION_ID)) {
                    return;
                }
                Log.i(TAG, "插件代码重新安装，重启宿主，加载最新代码逻辑");

                //自杀后，自然有其他守护进程拉起，无需考虑死后重启问题
                Process.killProcess(Process.myPid());
                System.exit(0);
            }

            private String getPackageName(Intent intent) {
                Uri uri = intent.getData();
                return (uri != null) ? uri.getSchemeSpecificPart() : null;
            }

        }, intentFilter);
    }

    @SuppressWarnings("unchecked")
    private static List<XposedHotLoadCallBack> findAllCallBackV2() {
        ClassScanner.SubClassVisitor<XposedHotLoadCallBack> subClassVisitor = new ClassScanner.SubClassVisitor(true, XposedHotLoadCallBack.class);
        ClassScanner.scan(subClassVisitor, ToolConstant.appHookSupperPackage);
        return Lists.newArrayList(Iterables.filter(Lists.transform(subClassVisitor.getSubClass(), new Function<Class<? extends XposedHotLoadCallBack>, XposedHotLoadCallBack>() {
            @Nullable
            @Override
            public XposedHotLoadCallBack apply(Class<? extends XposedHotLoadCallBack> input) {
                try {
                    return input.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    Log.e("weijia", "failed to load create plugin", e);
                }
                return null;
            }
        }), new Predicate<XposedHotLoadCallBack>() {
            @Override
            public boolean apply(@Nullable XposedHotLoadCallBack input) {
                return input != null && input.needHook(SharedObject.loadPackageParam);
            }
        }));
    }
}

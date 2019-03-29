package com.virjar.ucrack.plugin.hotload;

import android.app.Activity;
import android.util.Log;

import com.virjar.ucrack.BuildConfig;
import com.virjar.ucrack.plugin.unpack.Dumper;
import com.virjar.xposed_extention.SharedObject;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author lei.X
 * @date 2018/8/6 上午9:56
 */
public class DumpDexController {
public static String tag="dumpdex";
    public static void registryDump1(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (StringUtils.equals(loadPackageParam.packageName, BuildConfig.APPLICATION_ID)) {
            return;//不hook自身
        }
        XposedBridge.log("对" + loadPackageParam.packageName + "进行脱壳dump处理");
        File file = new File(SharedObject.context.getFilesDir(), "dumpSmali/dump_version1");
        if (!file.exists()) {
            if (!file.mkdirs()) {
                XposedBridge.log("脱壳文件夹创建失败");
                return;
            }
        }
        PluginNativeLibLoader.makeSureNativeLibLoaded();
        Dumper.dumpVersion1();
    }

    public static void registerDump3(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (StringUtils.equals(loadPackageParam.packageName, BuildConfig.APPLICATION_ID)) {
            return;//不hook自身
        }
        XposedBridge.log("对" + loadPackageParam.packageName + "进行脱壳dump处理");
        PluginNativeLibLoader.makeSureNativeLibLoaded();
//        XposedBridge.hookAllConstructors(Activity.class, new SingletonXC_MethodHook() {
//
//
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                Class<?> aClass = param.thisObject.getClass();
//                if (aClass.getClassLoader().equals(Activity.class.getClassLoader())) {
//                    return;
//                }
//                throw new UnsupportedOperationException("开源版本不支持三代壳脱壳方案");
//            }
//        });
        Log.i(tag,"registerDumps3");
        XposedHelpers.findAndHookConstructor(Activity.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object activity = param.thisObject;
                if (activity == null) {
                    return;
                }
                XposedBridge.log("hook class " + activity.getClass());
                Log.i(tag,"compare name:"+activity.getClass().getName());
//                if (StringUtils.equalsIgnoreCase(activity.getClass().getName(), "com.xxx.xxx.MainActivity")) {
//                    Log.i(tag,"dump begin:");
//                    com.virjar.xposedhooktool.unshell.Dumper.dumpDex(activity);
//                }else if(StringUtils.equalsIgnoreCase(activity.getClass().getName(), "com.xxx.xxx.xxx.MainActivity")){
//                    Log.i(tag,"dump next begin:");
//                    com.virjar.xposedhooktool.unshell.Dumper.dumpDex(activity);
//                }
                if(activity.getClass().getName().indexOf("com")!=-1 && activity.getClass().getName().indexOf("MainActivity")!=-1){
                    Log.i(tag,"dump begin:"+activity.getClass().getName());
                    com.virjar.xposedhooktool.unshell.Dumper.dumpDex(activity);
                }
            }
        });

        //PluginNativeLibLoader.makeSureNativeLibLoaded();
        //Dumper.dumpVersion3();
    }
}

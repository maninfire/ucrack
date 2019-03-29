package com.virjar.ucrack.plugin.droidsword;


import com.virjar.xposed_extention.SharedObject;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by virjar on 2017/12/23.<br/>
 * 把fragment的信息打印出来
 */
class FragmentHooker {
    private static class FragmentResumeHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            ActivityHooker.sFragments.add(param.thisObject.getClass().getName());
        }
    }

    private static FragmentResumeHook fragmentResumeHook = new FragmentResumeHook();

    static void hookFragment() {
        Class<?> fragmentClass;
        try {
            fragmentClass = SharedObject.loadPackageParam.classLoader.loadClass("android.support.v4.app.Fragment");
        } catch (ClassNotFoundException e) {
            return;
        }
        //hook constructor会导致app卡机，问题带排查。另外看源码onResume必须call supper，所以不需要考虑fragment onResume不call Supper的问题
        XposedHelpers.findAndHookMethod(fragmentClass, "onResume", fragmentResumeHook);
    }
}

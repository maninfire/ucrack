package com.virjar.ucrack.plugin;

import android.content.SharedPreferences;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * @author lei.X
 * @date 2018/8/7 下午5:40
 */
public class SharePreferenceConfigHolder {

    public static final String UNPACK_VERSION_1 = "unpack_version_1";
    public static final String UNPACK_VERSION_3 = "unpack_version_3";
    public static final String LOG = "log";
    public static final String NETDATA_PRINT = "netDataPrint";
    public static final String WEB_VIEW = "webView";
    public static final String D_SWORD = "dsword";
    public static final String SWITCH = "switch";
    public static final String TRUST_ME = "trustme";
    public static final String MONITOR_STACK = "monitor";

    public static final String IJIAMI_PREVENT = "ijiami_prevent";
    public static final String KILL_SELF = "kill_self";
    public static final String ALIAPY_STACK_TRACE = "alipay_stack_trace";
    public static final String NATIVE_FUNTION_REGISTERY = "native_function_registry";

    public static final String SonnyJackDragView = "sonny_jack_DragView";

    private static Map<String, Boolean> defaultValue = Maps.newHashMap();

    static {
        defaultValue.put(SWITCH, false);
        defaultValue.put(UNPACK_VERSION_1, false);
        defaultValue.put(UNPACK_VERSION_3, false);
        defaultValue.put(LOG, true);
        defaultValue.put(NETDATA_PRINT, true);
        defaultValue.put(WEB_VIEW, false);
        defaultValue.put(D_SWORD, true);
        defaultValue.put(TRUST_ME, true);
        defaultValue.put(MONITOR_STACK, true);

        defaultValue.put(IJIAMI_PREVENT, false);
        defaultValue.put(KILL_SELF, true);
        defaultValue.put(ALIAPY_STACK_TRACE, false);
        defaultValue.put(NATIVE_FUNTION_REGISTERY, false);
        defaultValue.put(SonnyJackDragView, true);
    }


    public static boolean getCheckWithFunc(String funcStr, SharedPreferences sharedPreferences) {
        Boolean theDefaultValue = defaultValue.get(funcStr);
        if (theDefaultValue == null) {
            theDefaultValue = false;
        }
        return sharedPreferences.getBoolean(funcStr, theDefaultValue);
    }


}

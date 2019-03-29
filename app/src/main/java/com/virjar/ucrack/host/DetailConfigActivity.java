package com.virjar.ucrack.host;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.virjar.ucrack.R;
import com.virjar.ucrack.plugin.SharePreferenceConfigHolder;
import com.virjar.ucrack.plugin.ToolConstant;


public class DetailConfigActivity extends AppCompatActivity {

    private static final String TAG = "xulei.xu_INFO";

    private SharedPreferences sharedPreferences = null;


    @SuppressLint("WorldReadableFiles")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        Intent intent = getIntent();
        String appName = intent.getStringExtra(ToolConstant.appName);
        String appPackage = intent.getStringExtra(ToolConstant.appPackage);
        this.setTitle(appName + "插件开关");
        sharedPreferences = getSharedPreferences(ToolConstant.configPrefix + appPackage, MODE_WORLD_READABLE);

//        try {
//
//            Object xSharedPreferences = ClassLoader.getSystemClassLoader()
//                    .loadClass("de.robv.android.xposed.XSharedPreferences")
//                    .getConstructor(String.class, String.class).newInstance(BuildConfig.APPLICATION_ID, ToolConstant.configPrefix + appPackage);
//            ReflectUtil.callMethod(xSharedPreferences, "makeWorldReadable");
//            sharedPreferences = (SharedPreferences) xSharedPreferences;
//        } catch (Throwable throwable) {
//            //ignore
//        }
//        if (sharedPreferences == null) {
//            sharedPreferences = getSharedPreferences(ToolConstant.configPrefix + appPackage, MODE_WORLD_READABLE);
//            // todo 这里不成功的话，说明是高版本的手机，并且改版本中没有xposed环境
//        }

        Log.i(TAG, "appName:{}" + appName + "--appPackage:{}" + appPackage);
        buildCheckBoxLister();
    }


    private void bindCheckBox(Integer checkBoxId, final String switchName) {
        CheckBox switchFLag = (CheckBox) findViewById(checkBoxId);
        switchFLag.setChecked(SharePreferenceConfigHolder.getCheckWithFunc(switchName, sharedPreferences));
        switchFLag.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sharedPreferences.edit().putBoolean(switchName, isChecked).apply();
            }
        });

    }


    //构建checkBox响应
    private void buildCheckBoxLister() {
        bindCheckBox(R.id.ckb_switch, SharePreferenceConfigHolder.SWITCH);
        bindCheckBox(R.id.ckb_unpack_1, SharePreferenceConfigHolder.UNPACK_VERSION_1);
        bindCheckBox(R.id.ckb_unpack_3, SharePreferenceConfigHolder.UNPACK_VERSION_3);
        bindCheckBox(R.id.ckb_log, SharePreferenceConfigHolder.LOG);
        bindCheckBox(R.id.ckb_netDataPrint, SharePreferenceConfigHolder.NETDATA_PRINT);
        bindCheckBox(R.id.ckb_webview, SharePreferenceConfigHolder.WEB_VIEW);
        bindCheckBox(R.id.ckb_dsword, SharePreferenceConfigHolder.D_SWORD);
        bindCheckBox(R.id.ckb_trust, SharePreferenceConfigHolder.TRUST_ME);
        bindCheckBox(R.id.monitorThread, SharePreferenceConfigHolder.MONITOR_STACK);
        bindCheckBox(R.id.ijiami_prevent, SharePreferenceConfigHolder.IJIAMI_PREVENT);
        bindCheckBox(R.id.self_kill_stacktrace, SharePreferenceConfigHolder.KILL_SELF);
        bindCheckBox(R.id.alipay_stack_trace, SharePreferenceConfigHolder.ALIAPY_STACK_TRACE);
        bindCheckBox(R.id.native_function_register, SharePreferenceConfigHolder.NATIVE_FUNTION_REGISTERY);
        bindCheckBox(R.id.sonny_jack_drag_view, SharePreferenceConfigHolder.SonnyJackDragView);
    }


}

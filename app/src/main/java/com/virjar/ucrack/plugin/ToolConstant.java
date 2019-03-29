package com.virjar.ucrack.plugin;

/**
 * @author lei.X
 * @date 2018/8/6 上午11:17
 */
public interface ToolConstant {
    String configPrefix = "configPrefix";
    String appName = "appName";
    String appPackage = "appPackage";
    //热加载入口，不能修改其定义
    String hotloadPluginEntry = "com.virjar.ucrack.plugin.hotload.HotLoadPackageEntry";
    //所有插件写到这个包下面，即可被框架自动识别
    String appHookSupperPackage = "com.virjar.ucrack.apphook";
}

package com.example.xing.myxposedmodule;

import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.widget.TextView;

import com.example.xing.myxposedmodule.hooks.AOSPSystemUIHook;
import com.example.xing.myxposedmodule.hooks.PhoneStatusBarHook;
import com.example.xing.myxposedmodule.hooks.PhoneWindowManagerHook;
import com.example.xing.myxposedmodule.hooks.PointerEventDispatcherHooks;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.log;

/**
 * Created by jiaxing on 10/13/16.
 */

public class NoBarNav implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {

    private final String TAG = "NoBarNav";

    private static String MODULE_PATH;
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log("Hideable Nav Bar: Version = " + BuildConfig.VERSION_CODE);
        MODULE_PATH = startupParam.modulePath;
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        switch (lpparam.packageName) {
            case PACKAGE_SYSTEMUI:
                try {
                    Log.d(TAG, "AOSPSystemUIHook.doHook");
                    AOSPSystemUIHook.doHook(lpparam.classLoader);
                } catch (Exception e) {
                    log(TAG + e);
                }
                try {
                    Log.d(TAG, "PhoneStatusBarHook.doHook");
                    PhoneStatusBarHook.doHook(lpparam.classLoader);
                } catch (Exception e) {
                    log(TAG + e);
                }
                break;
            case "android":
                try {
                    PointerEventDispatcherHooks.doHook(lpparam.classLoader);
                } catch (Exception e) {
                    log(TAG + e);
                }
                try {
                    Log.d(TAG, "PhoneWindowManagerHook.doHook");
                    PhoneWindowManagerHook.doHook(lpparam.classLoader);
                } catch (Exception e) {
                    log(TAG + e);
                }
                break;

            case BuildConfig.APPLICATION_ID:
                try {
//                    XposedHelpers.findAndHookMethod(SettingsActivity.class.getName(), lpparam.classLoader,
//                            "activatedModuleVersion", XC_MethodReplacement.returnConstant(BuildConfig.VERSION_CODE));
                } catch (Exception e) {
                    log(TAG + e);
                }
                break;
        }
    }

}

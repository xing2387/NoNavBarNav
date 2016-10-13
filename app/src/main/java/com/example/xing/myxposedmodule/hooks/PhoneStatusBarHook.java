package com.example.xing.myxposedmodule.hooks;

import android.graphics.Point;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.example.xing.myxposedmodule.PublicVlue;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

import static com.example.xing.myxposedmodule.hooks.PhoneWindowManagerHook.sGesturesListener;

/**
 * Created by jiaxing on 10/13/16.
 */

public class PhoneStatusBarHook {

    private static final String TAG = "PhoneStatusBarHook";

    private static Object phoneStatusBar;
    private static WindowManager windowManager;
    private static ViewGroup navigationBarView;

    public static void doHook(ClassLoader classLoader) {

        final Class<?> CLASS_PHONE_STATUS_BAR = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", classLoader);
        XposedHelpers.findAndHookMethod(CLASS_PHONE_STATUS_BAR, "repositionNavigationBar", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Log.d(TAG, "after repositionNavigationBar");
                if (phoneStatusBar == null) {
                    phoneStatusBar = param.thisObject;
                }
                if (windowManager == null) {
                    windowManager = (WindowManager) XposedHelpers.getObjectField(
                            phoneStatusBar, "mWindowManager");
                    Point point = new Point();
                    windowManager.getDefaultDisplay().getSize(point);
                }
                if (navigationBarView == null) {
                    navigationBarView = (ViewGroup) XposedHelpers.getObjectField(
                            phoneStatusBar, "mNavigationBarView");
                }
//                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) navigationBarView.getLayoutParams();
//                layoutParams.height = 0;
//                layoutParams.width = 0;
//                navigationBarView.setLayoutParams(layoutParams);
//                navigationBarView.invalidate();
                if (navigationBarView != null) {
                    PhoneWindowManagerHook.sendNavBarHideIntent(navigationBarView.getContext());
                }
            }
        });

    }

}

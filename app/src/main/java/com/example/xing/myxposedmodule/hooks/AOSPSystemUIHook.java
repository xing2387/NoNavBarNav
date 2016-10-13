package com.example.xing.myxposedmodule.hooks;

import android.content.res.Resources;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by jiaxing on 10/13/16.
 */

public class AOSPSystemUIHook {

    private static final String TAG = "AOSPSystemUIHook";
    private static ViewGroup sNavBar;

    public static void doHook(ClassLoader classLoader) {
        final Class<?> CLASS_NAVIGATION_BAR_VIEW = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NavigationBarView", classLoader);
        XposedHelpers.findAndHookMethod(CLASS_NAVIGATION_BAR_VIEW, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                sNavBar = (ViewGroup) param.thisObject;
//                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) sNavBar.getLayoutParams();
//                layoutParams.height = 0;
//                layoutParams.width = 0;
//                sNavBar.setLayoutParams(layoutParams);

                sNavBar.setVisibility(View.GONE);

            }
        });
    }


}

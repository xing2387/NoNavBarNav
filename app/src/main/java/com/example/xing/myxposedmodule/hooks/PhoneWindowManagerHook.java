package com.example.xing.myxposedmodule.hooks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.example.xing.myxposedmodule.BuildConfig;
import com.example.xing.myxposedmodule.PublicVlue;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedBridge.log;

/**
 * Created by jiaxing on 10/13/16.
 */

public class PhoneWindowManagerHook {

    public static final String ACTION_HIDE_NAV_BAR = "com.example.xing.myxposedmodule.hooks.PhoneWindowManagerHooks.action.HIDE_NAV_BAR";
    private static final String TAG = "PhoneWindowManagerHook";
    private static Object sPhoneWindowManager;
    public static GesturesListener sGesturesListener;
    private static Context sContext;
    private static Handler sHandler;


    private static BroadcastReceiver sHideNavBarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_HIDE_NAV_BAR:
                    hideNavBar();
                    break;
            }
        }
    };

    public static void doHook(ClassLoader classLoader) {
        final XSharedPreferences preferences = new XSharedPreferences(BuildConfig.APPLICATION_ID);

        final String CLASS_PHONE_WINDOW_MANAGER = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ?
                "com.android.internal.policy.impl.PhoneWindowManager" :
                "com.android.server.policy.PhoneWindowManager";
        final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";
        final String CLASS_WINDOW_MANAGER_FUNCS = "android.view.WindowManagerPolicy.WindowManagerFuncs";

        XposedHelpers.findAndHookMethod(CLASS_PHONE_WINDOW_MANAGER, classLoader, "init",
                Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        log(TAG + XposedHelpers.getObjectField(param.thisObject, "mWindowManagerFuncs").getClass().getName());
                        sPhoneWindowManager = param.thisObject;
                        sContext = (Context) XposedHelpers.getObjectField(sPhoneWindowManager, "mContext");
                        sHandler = (Handler) XposedHelpers.getObjectField(sPhoneWindowManager, "mHandler");
                        Resources res = sContext.getResources();
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(ACTION_HIDE_NAV_BAR);
                        sContext.registerReceiver(sHideNavBarReceiver, intentFilter);


                        sGesturesListener = new GesturesListener(sContext, new GesturesListener.Callbacks() {
                            @Override
                            public void onSwipeFromTop(MotionEvent event) {
                                Log.e(TAG, "onSwipeFromTop");
                            }

                            @Override
                            public void onSwipeFromBottom(MotionEvent event) {
                                if (XposedHelpers.getBooleanField(sPhoneWindowManager, "mNavigationBarOnBottom")) {
                                    Log.d(TAG, "onSwipeFromBottom x,y = " + event.getX() + "," + event.getY());
                                    float x = event.getX();
                                    if (x > PublicVlue.ScreenWidth - PublicVlue.ScreenWidth / 10 - 80) {
                                        injectKey(KeyEvent.KEYCODE_MENU);
                                    } else if (x > PublicVlue.ScreenWidth - PublicVlue.ScreenWidth / 3) {
                                        injectKey(KeyEvent.KEYCODE_BACK);
                                    } else if (x > PublicVlue.ScreenWidth / 5 * 2) {
                                        injectKey(KeyEvent.KEYCODE_HOME);
                                    } else if (x > PublicVlue.ScreenWidth / 5) {
                                        injectKey(KeyEvent.KEYCODE_APP_SWITCH);
                                    }
                                    event.setSource(0);
                                }
                            }

                            @Override
                            public void onSwipeFromRight(MotionEvent event) {
                                if (!XposedHelpers.getBooleanField(sPhoneWindowManager, "mNavigationBarOnBottom")) {
                                    Log.d(TAG, "onSwipeFromRight x,y = " + event.getX() + "," + event.getY());
                                    float y = event.getY();
                                    if (y < PublicVlue.ScreenHeight / 10 + 80) {
                                        injectKey(KeyEvent.KEYCODE_MENU);
                                    } else if (y < PublicVlue.ScreenHeight / 3) {
                                        injectKey(KeyEvent.KEYCODE_BACK);
                                    } else if (y < PublicVlue.ScreenHeight - PublicVlue.ScreenHeight / 5 * 2) {
                                        injectKey(KeyEvent.KEYCODE_HOME);
                                    } else if (y  < PublicVlue.ScreenHeight - PublicVlue.ScreenHeight / 5) {
                                        injectKey(KeyEvent.KEYCODE_APP_SWITCH);
                                    }
                                    event.setSource(0);
                                }
                            }

                            @Override
                            public void onDebug() {
                            }
                        });
                    }
                });
//        XposedHelpers.findAndHookMethod(CLASS_PHONE_WINDOW_MANAGER, classLoader, "setInitialDisplaySize",
//                Display.class, int.class, int.class, int.class, new XC_MethodHook() {
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        super.afterHookedMethod(param);
//                        hideNavBar();
//                    }
//                });

        final String CLASS_WINDOW_STATE = "android.view.WindowManagerPolicy$WindowState";
        XposedHelpers.findAndHookMethod(CLASS_PHONE_WINDOW_MANAGER, classLoader, "layoutWindowLw", CLASS_WINDOW_STATE, CLASS_WINDOW_STATE, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                PublicVlue.ScreenWidth = XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenWidth");
                PublicVlue.ScreenHeight = XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenHeight");
            }
        });
    }

    public static void injectKey(final int keyCode) {
        if (sHandler == null) return;
        Log.d(TAG, "injectKey " + keyCode);
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final long eventTime = SystemClock.uptimeMillis();
                    final InputManager inputManager = (InputManager)
                            sContext.getSystemService(Context.INPUT_SERVICE);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent",
                            new KeyEvent(eventTime - 50, eventTime - 50, KeyEvent.ACTION_DOWN,
                                    keyCode, 0), 0);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent",
                            new KeyEvent(eventTime - 50, eventTime - 25, KeyEvent.ACTION_UP,
                                    keyCode, 0), 0);
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            }
        });
    }

    private static void setNavBarDimensions(int wp, int hp, int hl) {
        Log.d(TAG, "PhoneWindowManagerHook.setNavBarDimensions(0, 0, 0)");
        int[] navigationBarWidthForRotation = (int[]) XposedHelpers.getObjectField(
                sPhoneWindowManager, "mNavigationBarWidthForRotation");
        int[] navigationBarHeightForRotation = (int[]) XposedHelpers.getObjectField(
                sPhoneWindowManager, "mNavigationBarHeightForRotation");
        final int portraitRotation = XposedHelpers.getIntField(sPhoneWindowManager, "mPortraitRotation");
        final int upsideDownRotation = XposedHelpers.getIntField(sPhoneWindowManager, "mUpsideDownRotation");
        final int landscapeRotation = XposedHelpers.getIntField(sPhoneWindowManager, "mLandscapeRotation");
        final int seascapeRotation = XposedHelpers.getIntField(sPhoneWindowManager, "mSeascapeRotation");
        if (navigationBarHeightForRotation[portraitRotation] == hp && navigationBarHeightForRotation[landscapeRotation] == hl
                && navigationBarWidthForRotation[portraitRotation] == wp && navigationBarWidthForRotation[landscapeRotation] == wp)
            return;

        navigationBarHeightForRotation[portraitRotation] =
                navigationBarHeightForRotation[upsideDownRotation] =
                        hp;
        navigationBarHeightForRotation[landscapeRotation] =
                navigationBarHeightForRotation[seascapeRotation] =
                        hl;

        navigationBarWidthForRotation[portraitRotation] =
                navigationBarWidthForRotation[upsideDownRotation] =
                        navigationBarWidthForRotation[landscapeRotation] =
                                navigationBarWidthForRotation[seascapeRotation] =
                                        wp;
        XposedHelpers.callMethod(sPhoneWindowManager, "updateRotation", false);
    }


    private static boolean isNavBarShowing() {
        int[] navigationBarWidthForRotation = (int[]) XposedHelpers.getObjectField(
                sPhoneWindowManager, "mNavigationBarWidthForRotation");

        final int portraitRotation = XposedHelpers.getIntField(sPhoneWindowManager, "mPortraitRotation");
        final int upsideDownRotation = XposedHelpers.getIntField(sPhoneWindowManager, "mUpsideDownRotation");
        final int landscapeRotation = XposedHelpers.getIntField(sPhoneWindowManager, "mLandscapeRotation");
        final int seascapeRotation = XposedHelpers.getIntField(sPhoneWindowManager, "mSeascapeRotation");

        return navigationBarWidthForRotation[portraitRotation] ==
                navigationBarWidthForRotation[upsideDownRotation] &&
                navigationBarWidthForRotation[landscapeRotation] ==
                        navigationBarWidthForRotation[seascapeRotation] &&
                navigationBarWidthForRotation[portraitRotation] ==
                        navigationBarWidthForRotation[landscapeRotation] &&
                navigationBarWidthForRotation[portraitRotation] == 0;

    }

    private static void hideNavBar() {
        setNavBarDimensions(0, 0, 0);
    }

    public static void sendNavBarHideIntent(Context context) {
        Intent intent = new Intent(PhoneWindowManagerHook.ACTION_HIDE_NAV_BAR);
        context.sendBroadcast(intent);
    }
}

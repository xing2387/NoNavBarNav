/*
 * Copyright 2015-2016 Alex Zhang aka. ztc1997
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.example.xing.myxposedmodule.hooks;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.MotionEvent;

import com.example.xing.myxposedmodule.PublicVlue;

import static de.robv.android.xposed.XposedBridge.log;
import static com.example.xing.myxposedmodule.BuildConfig.DEBUG;

public class GesturesListener {
    private static final String TAG = GesturesListener.class.getSimpleName() + ": ";
    private static final long SWIPE_TIMEOUT_MS = 500;
    private static final int MAX_TRACKED_POINTERS = 32;  // max per input system
    private static final int UNTRACKED_POINTER = -1;

    private static final int SWIPE_NONE = 0;
    private static final int SWIPE_FROM_TOP = 1;
    private static final int SWIPE_FROM_BOTTOM = 2;
    private static final int SWIPE_FROM_RIGHT = 3;

    private final int mSwipeStartThreshold;
    private final int mSwipeDistanceThreshold;
    private final Callbacks mCallbacks;
    private final int[] mDownPointerId = new int[MAX_TRACKED_POINTERS];
    private final float[] mDownX = new float[MAX_TRACKED_POINTERS];
    private final float[] mDownY = new float[MAX_TRACKED_POINTERS];
    private final long[] mDownTime = new long[MAX_TRACKED_POINTERS];

    //    int screenHeight;
//    int screenWidth;
    private int mDownPointers;
    private boolean mSwipeFireable;
    private boolean mDebugFireable;

    public GesturesListener(Context context, Callbacks callbacks) {
        mCallbacks = checkNull("callbacks", callbacks);
        Resources resources = checkNull("context", context).getResources();
        mSwipeStartThreshold = resources.getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android"));
        mSwipeDistanceThreshold = mSwipeStartThreshold;
        if (DEBUG) log(TAG + "mSwipeStartThreshold=" + mSwipeStartThreshold
                + " mSwipeDistanceThreshold=" + mSwipeDistanceThreshold);
    }

    private static <T> T checkNull(String name, T arg) {
        if (arg == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return arg;
    }

    public void onPointerEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "onPointerEvent ACTION_DOWN on x,y = " + event.getX() + "," + event.getY());
                mSwipeFireable = true;
                mDebugFireable = true;
                mDownPointers = 0;
                captureDown(event, 0);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                captureDown(event, event.getActionIndex());
                if (mDebugFireable) {
                    mDebugFireable = event.getPointerCount() < 5;
                    if (!mDebugFireable) {
                        if (DEBUG) log(TAG + "Firing debug");
                        mCallbacks.onDebug();
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mSwipeFireable) {
                    final int swipe = detectSwipe(event);
                    mSwipeFireable = swipe == SWIPE_NONE;
                    if (swipe == SWIPE_FROM_TOP) {
                        if (DEBUG) log(TAG + "Firing onSwipeFromTop");
                        mCallbacks.onSwipeFromTop(event);
                    } else if (swipe == SWIPE_FROM_BOTTOM) {
                        if (DEBUG) log(TAG + "Firing onSwipeFromBottom");
                        mCallbacks.onSwipeFromBottom(event);
                    } else if (swipe == SWIPE_FROM_RIGHT) {
                        if (DEBUG) log(TAG + "Firing onSwipeFromRight");
                        mCallbacks.onSwipeFromRight(event);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mSwipeFireable = false;
                mDebugFireable = false;
                break;
            default:
                if (DEBUG) log(TAG + "Ignoring " + event);
        }
    }

    private void captureDown(MotionEvent event, int pointerIndex) {
        final int pointerId = event.getPointerId(pointerIndex);
        final int i = findIndex(pointerId);
        if (DEBUG) log(TAG + "pointer " + pointerId +
                " down pointerIndex=" + pointerIndex + " trackingIndex=" + i);
        if (i != UNTRACKED_POINTER) {
            mDownX[i] = event.getX(pointerIndex);
            mDownY[i] = event.getY(pointerIndex);
            mDownTime[i] = event.getEventTime();
            if (DEBUG) log(TAG + "pointer " + pointerId +
                    " down x=" + mDownX[i] + " y=" + mDownY[i]);
        }
    }

    private int findIndex(int pointerId) {
        for (int i = 0; i < mDownPointers; i++) {
            if (mDownPointerId[i] == pointerId) {
                return i;
            }
        }
        if (mDownPointers == MAX_TRACKED_POINTERS || pointerId == MotionEvent.INVALID_POINTER_ID) {
            return UNTRACKED_POINTER;
        }
        mDownPointerId[mDownPointers++] = pointerId;
        return mDownPointers - 1;
    }

    private int detectSwipe(MotionEvent move) {
        final int historySize = move.getHistorySize();
        final int pointerCount = move.getPointerCount();
        for (int p = 0; p < pointerCount; p++) {
            final int pointerId = move.getPointerId(p);
            final int i = findIndex(pointerId);
            if (i != UNTRACKED_POINTER) {
                for (int h = 0; h < historySize; h++) {
                    final long time = move.getHistoricalEventTime(h);
                    final float x = move.getHistoricalX(p, h);
                    final float y = move.getHistoricalY(p, h);
                    final int swipe = detectSwipe(i, time, x, y);
                    if (swipe != SWIPE_NONE) {
                        return swipe;
                    }
                }
                final int swipe = detectSwipe(i, move.getEventTime(), move.getX(p), move.getY(p));
                if (swipe != SWIPE_NONE) {
                    return swipe;
                }
            }
        }
        return SWIPE_NONE;
    }

    private int detectSwipe(int i, long time, float x, float y) {
        final float fromX = mDownX[i];
        final float fromY = mDownY[i];
        final long elapsed = time - mDownTime[i];
        if (DEBUG) log(TAG + "pointer " + mDownPointerId[i]
                + " moved (" + fromX + "->" + x + "," + fromY + "->" + y + ") in " + elapsed);
        if (fromY <= mSwipeStartThreshold
                && y > fromY + mSwipeDistanceThreshold
                && elapsed < SWIPE_TIMEOUT_MS) {
            return SWIPE_FROM_TOP;
        }
        if (fromY >= PublicVlue.ScreenHeight - mSwipeStartThreshold
                && y < fromY - mSwipeDistanceThreshold
                && elapsed < SWIPE_TIMEOUT_MS) {
            return SWIPE_FROM_BOTTOM;
        }
        if (fromX >= PublicVlue.ScreenWidth - mSwipeStartThreshold
                && x < fromX - mSwipeDistanceThreshold
                && elapsed < SWIPE_TIMEOUT_MS) {
            return SWIPE_FROM_RIGHT;
        }
        return SWIPE_NONE;
    }

    interface Callbacks {
        void onSwipeFromTop(MotionEvent event);

        void onSwipeFromBottom(MotionEvent event);

        void onSwipeFromRight(MotionEvent event);

        void onDebug();
    }
}
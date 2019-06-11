package com.cyberpunk.ble.beat.connect;

import android.app.Application;

public class BluefruitApplication extends Application {
    // Log
    private final static String TAG = BluefruitApplication.class.getSimpleName();

    // Data
    private static boolean mIsActivityVisible;

    // region Detect app in background: https://stackoverflow.com/questions/3667022/checking-if-an-android-application-is-running-in-the-background

    public static boolean isActivityVisible() {
        return mIsActivityVisible;
    }

    public static void activityResumed() {
        mIsActivityVisible = true;
    }

    public static void activityPaused() {
        mIsActivityVisible = false;
    }

    // endregion
}

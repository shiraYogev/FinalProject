package com.example.finalprojectappraisal;

import android.app.Application;
import android.util.Log;

public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((t, e) ->
                Log.e("AppCrash", "Uncaught exception in " + t.getName(), e));
    }
}

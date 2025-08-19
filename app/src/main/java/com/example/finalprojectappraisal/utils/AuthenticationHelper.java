package com.example.finalprojectappraisal.utils;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.example.finalprojectappraisal.activity.AppraiserSetupActivity;
import com.example.finalprojectappraisal.activity.MainActivity;
import com.google.firebase.auth.FirebaseUser;

/**
 * Helper class to manage authentication flow and determine next activity
 */
public class AuthenticationHelper {

    private static final String TAG = "AuthHelper";

    /**
     * Determines where to navigate after successful authentication
     * @param activity Current activity
     * @param user Authenticated Firebase user
     * @param isFirstTimeLogin Whether this is the first time login/registration
     */
    public static void handlePostAuthNavigation(Activity activity, FirebaseUser user,
                                                boolean isFirstTimeLogin) {
        if (user == null) {
            Log.e(TAG, "User is null, cannot navigate");
            return;
        }

        if (isFirstTimeLogin) {
            // For first time users, always go to setup
            navigateToAppraiserSetup(activity);
        } else {
            // For existing users, check if setup was completed
            AppraiserSetupActivity.checkAppraiserSetupStatus(user, isCompleted -> {
                if (isCompleted) {
                    navigateToMain(activity);
                } else {
                    navigateToAppraiserSetup(activity);
                }
            });
        }
    }

    /**
     * Navigate to appraiser setup activity
     */
    private static void navigateToAppraiserSetup(Activity activity) {
        Intent intent = new Intent(activity, AppraiserSetupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    /**
     * Navigate to main activity
     */
    private static void navigateToMain(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
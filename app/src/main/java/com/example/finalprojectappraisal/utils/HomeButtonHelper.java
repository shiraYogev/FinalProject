package com.example.finalprojectappraisal.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.HomePageActivity;

/**
 * עוזר לחיווט כפתור הבית (btn_home_header) בכל מסכי אשף יצירת הפרויקט.
 * לחיצה מציגה אישור ואז מנווטת חזרה לדף הבית.
 */
public final class HomeButtonHelper {

    private HomeButtonHelper() {
        throw new AssertionError("Cannot instantiate helper class");
    }

    /** מחבר את כפתור הבית שבכותרת המסך לניווט חזרה לדף הבית. */
    public static void setup(Activity activity) {
        View homeButton = activity.findViewById(R.id.btn_home_header);
        if (homeButton != null) {
            homeButton.setOnClickListener(v -> confirmAndGoHome(activity));
        }
    }

    private static void confirmAndGoHome(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("חזרה לדף הבית")
                .setMessage("האם לחזור לדף הבית? שינויים שלא נשמרו במסך זה עלולים לאבד.")
                .setPositiveButton("חזור לדף הבית", (dialog, which) -> goHome(activity))
                .setNegativeButton("ביטול", null)
                .show();
    }

    private static void goHome(Activity activity) {
        Intent intent = new Intent(activity, HomePageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.finish();
    }
}

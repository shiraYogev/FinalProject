package com.example.finalprojectappraisal.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.HomePageActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // חשוב: ה-Theme של האקטיביטי כבר Theme.App.Splash (ב-Manifest)
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // ראה סעיף 4

        // השהייה קצרה לויזואליות (אפשר 0 אם לא רוצים בכלל)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // כאן לא מפנים למסך "השלמת שמאי" – הולכים ישר למסך הבית
            Intent i = new Intent(SplashActivity.this, HomePageActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }, 400);
    }
}

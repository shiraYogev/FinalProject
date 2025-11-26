package com.example.finalprojectappraisal.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
// ✅ יש לייבא את MainActivity
import com.example.finalprojectappraisal.activity.MainActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // השהייה קצרה
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // ⬇️ השינוי כאן: הפנייה ל-MainActivity במקום HomePageActivity/LoginActivity
            Intent i = new Intent(SplashActivity.this, MainActivity.class);

            // הדגלים האלה נכונים כדי למנוע חזרה למסך הפתיחה
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }, 400); // 400 מילישניות
    }
}
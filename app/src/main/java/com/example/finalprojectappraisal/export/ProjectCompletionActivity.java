package com.example.finalprojectappraisal.export;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.HomePageActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * מסך סיום פרויקט - מבצע ייצוא אוטומטי ל-JSON ואז מנווט לדף הבית.
 * נקרא מה-UploadTabuActivity במקום finishProjectCreation() ישירות.
 */
public class ProjectCompletionActivity extends AppCompatActivity {

    private static final String TAG = "ProjectCompletion";
    private static final long MIN_DISPLAY_TIME_MS = 2000; // זמן מינימום להצגת המסך (2 שניות)
    private static final long AUTO_NAVIGATE_DELAY_MS = 3000; // זמן אוטומטי לניווט (3 שניות)

    public static final String EXTRA_PROJECT_ID = "project_id";

    private TextView tvStatus;
    private TextView tvSubtitle;
    private ProgressBar progressBar;
    private MaterialButton btnContinue;
    private MaterialCardView successCard;

    private ProjectJsonExporter exporter;
    private String projectId;
    private boolean exportCompleted = false;
    private long startTime;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_completion);

        // קבלת projectId
        projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        if (projectId == null || projectId.isEmpty()) {
            Toast.makeText(this, "שגיאה: חסר מזהה פרויקט", Toast.LENGTH_SHORT).show();
            navigateToHome();
            return;
        }

        initViews();
        startExport();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        progressBar = findViewById(R.id.progress_bar);
        btnContinue = findViewById(R.id.btn_continue);
        successCard = findViewById(R.id.success_card);

        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> navigateToHome());
        }
    }

    private void startExport() {
        startTime = System.currentTimeMillis();

        // הצגת סטטוס התחלתי
        showProgressState();

        // התחלת ייצוא
        exporter = new ProjectJsonExporter();
        exporter.exportProject(projectId, new ProjectJsonExporter.ExportCallback() {
            @Override
            public void onSuccess(String projectId) {
                exportCompleted = true;
                long elapsed = System.currentTimeMillis() - startTime;
                long remainingDelay = Math.max(0, MIN_DISPLAY_TIME_MS - elapsed);

                handler.postDelayed(() -> showSuccessState(), remainingDelay);
                handler.postDelayed(() -> navigateToHome(), AUTO_NAVIGATE_DELAY_MS);
            }

            @Override
            public void onError(String errorMessage) {
                exportCompleted = true;
                // גם אם הייצוא נכשל, עדיין מציגים הצלחה וממשיכים
                // הייצוא יוכל לרוץ שוב בפעם הבאה
                long elapsed = System.currentTimeMillis() - startTime;
                long remainingDelay = Math.max(0, MIN_DISPLAY_TIME_MS - elapsed);

                handler.postDelayed(() -> showPartialSuccessState(errorMessage), remainingDelay);
                handler.postDelayed(() -> navigateToHome(), AUTO_NAVIGATE_DELAY_MS + 2000);
            }
        });
    }

    private void showProgressState() {
        tvStatus.setText("מייצא נתונים...");
        tvSubtitle.setText("מכין את הפרויקט להעברה למערכת החיצונית");
        progressBar.setVisibility(View.VISIBLE);
        if (successCard != null) {
            successCard.setVisibility(View.GONE);
        }
        if (btnContinue != null) {
            btnContinue.setVisibility(View.GONE);
        }
    }

    private void showSuccessState() {
        tvStatus.setText("הפרויקט נוצר בהצלחה!");
        tvSubtitle.setText("הנתונים יוצאו למערכת החיצונית ונשמרו בהצלחה");
        progressBar.setVisibility(View.GONE);

        if (successCard != null) {
            successCard.setVisibility(View.VISIBLE);
            successCard.setCardBackgroundColor(getResources().getColor(R.color.accent_green_light, null));
        }

        if (btnContinue != null) {
            btnContinue.setVisibility(View.VISIBLE);
            btnContinue.setText("המשך לדף הבית");
        }
    }

    private void showPartialSuccessState(String error) {
        tvStatus.setText("הפרויקט נוצר בהצלחה");
        tvSubtitle.setText("הפרויקט נשמר, אך הייצוא למערכת החיצונית נכשל. ניתן לנסות שוב מאוחר יותר.");
        progressBar.setVisibility(View.GONE);

        if (successCard != null) {
            successCard.setVisibility(View.VISIBLE);
            successCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_orange_light, null));
        }

        if (btnContinue != null) {
            btnContinue.setVisibility(View.VISIBLE);
            btnContinue.setText("המשך בכל זאת");
        }

        // הצגת הודעת שגיאה בלוג (לא למשתמש)
        android.util.Log.w(TAG, "Export failed but continuing: " + error);
    }

    private void navigateToHome() {
        // מניעת כפילות - אם כבר עוזבים
        if (isFinishing()) {
            return;
        }

        Intent intent = new Intent(this, HomePageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // חסימת כפתור חזור - המשתמש חייב לחכות או ללחוץ המשך
        if (exportCompleted) {
            navigateToHome();
        } else {
            Toast.makeText(this, "נא להמתין לסיום הייצוא", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ביטול תזמונים אם האקטיביטי נהרס
        handler.removeCallbacksAndMessages(null);
    }
}

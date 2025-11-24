package com.example.finalprojectappraisal.activity.newProject;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.*;
import com.example.finalprojectappraisal.activity.newProject.bank.BankDetailsActivity;
import com.example.finalprojectappraisal.activity.newProject.bank.EditBankDetailsActivity;
import com.example.finalprojectappraisal.activity.newProject.client.ClientDetailsActivity;
import com.example.finalprojectappraisal.activity.newProject.images.UploadImagesActivity;
import com.example.finalprojectappraisal.activity.newProject.presenter.PresenterDetailsActivity;
import com.example.finalprojectappraisal.activity.newProject.property.activity.ApartmentDetailsActivity;
import com.example.finalprojectappraisal.activity.newProject.property.activity.PropertyDescriptionActivity;
import com.example.finalprojectappraisal.activity.newProject.property.activity.PropertyDetailsActivity;
import com.google.android.material.card.MaterialCardView;
import java.util.HashMap;
import java.util.Map;
import com.example.finalprojectappraisal.activity.HomePageActivity;
import android.widget.Toast;

/**
 * Helper class לניהול Progress Stepper
 * מנהל את הניווט, העיצוב והמעברים בין 9 שלבי יצירת הפרויקט
 */
public class ProgressStepperHelper {

    // הגדרת כל 9 השלבים - לפי הסדר הנכון!
    public static final int STEP_CLIENT_DETAILS = 1;        // פרטי לקוח
    public static final int STEP_IMAGE_UPLOAD = 2;          // העלאת תמונות נכס
    public static final int STEP_APARTMENT_DETAILS = 3;     // פרטי הדירה
    public static final int STEP_PROPERTY_DETAILS = 4;      // פרטי הנכס
    public static final int STEP_PROPERTY_SUMMARY = 5;      // תיאור הנכס
    public static final int STEP_BANK_DETAILS = 6;          // פרטי בנק
    public static final int STEP_BANK_EDIT = 7;             // עריכת פרטי הבנק
    public static final int STEP_APPRAISER_DETAILS = 8;     // פרטי שמאי ומוסר
    public static final int STEP_TABU_UPLOAD = 9;           // תמונת טאבו

    private final Context context;
    private final int currentStep;
    private final View rootView;

    // ⭐️ שינוי 1: הוספת משתנה לאחסון ה-projectId
    private final String projectId;

    // צבעים
    private int colorCompleted;
    private int colorCurrent;
    private int colorInactive;
    private int textColorCompleted;
    private int textColorCurrent;
    private int textColorInactive;

    // מפות סטטיות
    private static final Map<Integer, String> STEP_NAMES = new HashMap<>();
    private static final Map<Integer, Class<?>> STEP_ACTIVITIES = new HashMap<>();

    static {
        // מפה של שמות השלבים
        STEP_NAMES.put(STEP_CLIENT_DETAILS, "פרטי לקוח");
        STEP_NAMES.put(STEP_IMAGE_UPLOAD, "תמונות נכס");
        STEP_NAMES.put(STEP_APARTMENT_DETAILS, "פרטי הדירה");
        STEP_NAMES.put(STEP_PROPERTY_DETAILS, "פרטי הנכס");
        STEP_NAMES.put(STEP_PROPERTY_SUMMARY, "תיאור הנכס");
        STEP_NAMES.put(STEP_BANK_DETAILS, "פרטי בנק");
        STEP_NAMES.put(STEP_BANK_EDIT, "עריכת בנק");
        STEP_NAMES.put(STEP_APPRAISER_DETAILS, "שמאי ומוסר");
        STEP_NAMES.put(STEP_TABU_UPLOAD, "תמונת טאבו");

        // מפה של Activity classes - **עדכני את השמות בהתאם לפרויקט שלך!**
        STEP_ACTIVITIES.put(STEP_CLIENT_DETAILS, ClientDetailsActivity.class);
        STEP_ACTIVITIES.put(STEP_IMAGE_UPLOAD, UploadImagesActivity.class);
        STEP_ACTIVITIES.put(STEP_APARTMENT_DETAILS, ApartmentDetailsActivity.class);
        STEP_ACTIVITIES.put(STEP_PROPERTY_DETAILS, PropertyDetailsActivity.class);
        STEP_ACTIVITIES.put(STEP_PROPERTY_SUMMARY, PropertyDescriptionActivity.class);
        STEP_ACTIVITIES.put(STEP_BANK_DETAILS, BankDetailsActivity.class);
        STEP_ACTIVITIES.put(STEP_BANK_EDIT, EditBankDetailsActivity.class);
        STEP_ACTIVITIES.put(STEP_APPRAISER_DETAILS, PresenterDetailsActivity.class);
        STEP_ACTIVITIES.put(STEP_TABU_UPLOAD, UploadTabuActivity.class);
    }

    /**
     * Constructor
     */
    public ProgressStepperHelper(Context context, int currentStep, View rootView, String projectId) {
        this.context = context;
        this.currentStep = currentStep;
        this.rootView = rootView;
        this.projectId = projectId;

        // אתחול צבעים
        this.colorCompleted = ContextCompat.getColor(context, R.color.accent_green_light);
        this.colorCurrent = ContextCompat.getColor(context, android.R.color.white);
        this.colorInactive = ContextCompat.getColor(context, R.color.step_inactive);
        this.textColorCompleted = ContextCompat.getColor(context, android.R.color.white);
        this.textColorCurrent = ContextCompat.getColor(context, R.color.primary_blue);
        this.textColorInactive = ContextCompat.getColor(context, android.R.color.white);
    }

    /**
     * מאתחל את הסטפר
     */
    public void initialize() {
        updateStepperUI();
        setupClickListeners();
    }

    /**
     * מעדכן את הUI של כל השלבים
     */
    private void updateStepperUI() {
        for (int step = 1; step <= 9; step++) {
            MaterialCardView stepCard = rootView.findViewById(getStepCardId(step));
            TextView stepNumber = rootView.findViewById(getStepNumberId(step));
            View lineView = (step < 9) ? rootView.findViewById(getLineId(step, step + 1)) : null;

            if (stepCard == null || stepNumber == null) continue;

            if (step < currentStep) {
                // שלב שהושלם - ירוק עם ✓
                stepCard.setCardBackgroundColor(colorCompleted);
                stepNumber.setText("✓");
                stepNumber.setTextColor(textColorCompleted);
                stepCard.setCardElevation(4f);
                if (lineView != null) {
                    lineView.setBackgroundColor(colorCompleted);
                }

                // החזר לגודל רגיל
                stepCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start();

            } else if (step == currentStep) {
                // שלב נוכחי - לבן עם מספר כחול, מוגדל
                stepCard.setCardBackgroundColor(colorCurrent);
                stepNumber.setText(String.valueOf(step));
                stepNumber.setTextColor(textColorCurrent);
                stepCard.setCardElevation(6f);

                // הגדלה קלה
                stepCard.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(300)
                        .start();

            } else {
                // שלב עתידי - אפור שקוף
                stepCard.setCardBackgroundColor(colorInactive);
                stepNumber.setText(String.valueOf(step));
                stepNumber.setTextColor(textColorInactive);
                stepCard.setCardElevation(2f);
                if (lineView != null) {
                    lineView.setBackgroundColor(colorInactive);
                }

                // גודל רגיל
                stepCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start();
            }
        }
    }

    /**
     * מגדיר listeners לכל השלבים
     */
    private void setupClickListeners() {
        for (int step = 1; step <= 9; step++) {
            final int targetStep = step;
            MaterialCardView stepCard = rootView.findViewById(getStepCardId(step));
            if (stepCard == null) continue;

            if (step < currentStep) {
                // שלבים שהושלמו - ניתן לחזור אליהם
                stepCard.setClickable(true);
                stepCard.setFocusable(true);
                stepCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        navigateToStep(targetStep);
                    }
                });

            } else if (step == currentStep) {
                // שלב נוכחי - לא פעיל
                stepCard.setClickable(false);

            } else {
                // שלבים עתידיים - לא ניתן ללחיצה
                stepCard.setClickable(false);
                stepCard.setFocusable(false);
            }
        }
    }

    /**
     * מנווט לשלב מסוים
     */
    private void navigateToStep(int targetStep) {
        Class<?> activityClass = STEP_ACTIVITIES.get(targetStep);
        if (activityClass == null) return;

        Intent intent = new Intent(context, activityClass);

        // ⭐️ העברת ה-projectId ל-Activity הבא
        if (this.projectId != null && !this.projectId.isEmpty()) {
            intent.putExtra("projectId", this.projectId);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);

        // אנימציית מעבר
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (targetStep < currentStep) {
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        }
    }

    /**
     * פונקציות עזר למציאת IDs
     */
    private int getStepCardId(int step) {
        return context.getResources().getIdentifier("step_" + step, "id", context.getPackageName());
    }

    private int getStepNumberId(int step) {
        return context.getResources().getIdentifier("step_" + step + "_number", "id", context.getPackageName());
    }

    private int getLineId(int fromStep, int toStep) {
        return context.getResources().getIdentifier("line_" + fromStep + "_" + toStep, "id", context.getPackageName());
    }

    /**
     * מעבר לשלב הבא
     */
    public void moveToNextStep() {
        if (currentStep < 9) {
            navigateToStep(currentStep + 1);
        }
    }

    /**
     * חזרה לשלב הקודם
     */
    public void moveToPreviousStep() {
        if (currentStep > 1) {
            navigateToStep(currentStep - 1);
        }
    }

    /**
     * קבלת אחוז ההתקדמות (0-100)
     */
    public int getProgressPercentage() {
        return ((currentStep - 1) * 100) / 8;  // 9 שלבים = 8 מעברים
    }

    /**
     * קבלת שם השלב הנוכחי
     */
    public String getCurrentStepName() {
        String name = STEP_NAMES.get(currentStep);
        return name != null ? name : "";
    }

    /**
     * בדיקה האם זה השלב האחרון
     */
    public boolean isLastStep() {
        return currentStep == 9;
    }

    /**
     * בדיקה האם זה השלב הראשון
     */
    public boolean isFirstStep() {
        return currentStep == 1;
    }

    /**
     * מסיים את תהליך יצירת הפרויקט כולו ומנווט למסך הבית.
     * קורא רק בשלב האחרון (UploadTabuActivity).
     */
    public void finishProjectCreation() {
        if (context instanceof Activity) {
            // מציג הודעת הצלחה
            Toast.makeText(context, "✅ יצירת הפרויקט הושלמה בהצלחה!", Toast.LENGTH_LONG).show();

            // יצירת Intent למסך הבית (HomePageActivity)
            Intent intent = new Intent(context, HomePageActivity.class);

            // הדגלים הללו חיוניים: הם סוגרים את כל הפעילויות של הסטפר שנמצאות מעל ה-HomePage
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);

            // סוגר את הפעילות הנוכחית (UploadTabuActivity)
            ((Activity) context).finish();

        }
    }
}
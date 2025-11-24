package com.example.finalprojectappraisal.activity.newProject.presenter;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.ProgressStepperHelper;
import com.example.finalprojectappraisal.activity.newProject.presenter.veiwmodel.PresenterDetailsViewModel;
import com.example.finalprojectappraisal.database.auth.AuthRepository;
import com.example.finalprojectappraisal.utils.FieldValidators;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

/**
 * שלב 8: פרטי שמאי ומוסר
 */
public class PresenterDetailsActivity extends AppCompatActivity {

    // שדות טופס
    private TextInputEditText etAppraiserName, etAppraisalDate, etAppraiserRole,
            etNameOfPresenter, etIdOfPresenter, etTypeOfPresenterId,
            etRoleOfPresenter, etHolderStatus;

    // כפתורים
    private MaterialButton btnSave, btnFinish;

    // ✅ תיקון: שימוש ב-MaterialCardView במקום MaterialButton
    private MaterialCardView backButtonCard;

    // ViewModel ו-ProgressStepper
    private PresenterDetailsViewModel vm;
    private ProgressStepperHelper progressHelper;
    private String projectId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_details);

        // קבלת projectId
        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null || projectId.isEmpty()) {
            toast("שגיאה: חסר מזהה פרויקט");
            finish();
            return;
        }

        // אתחול ViewModel
        vm = new ViewModelProvider(this).get(PresenterDetailsViewModel.class);

        // אתחול Views
        bindViews();

        // ✅ אתחול ProgressStepper ו-Listeners
        setupProgressStepper();
        setupListeners();

        // אתחול ViewModel עם userId לאיסוף נתונים אוטומטי
        String uid = AuthRepository.getInstance().getCurrentUserId();
        vm.init(projectId, uid);

        // Observers
        setupObservers();
    }

    /**
     * אתחול כל ה-Views
     */
    private void bindViews() {
        // שדות שמאי
        etAppraiserName = findViewById(R.id.et_appraiser_name);
        etAppraisalDate = findViewById(R.id.et_appraisal_date);
        etAppraiserRole = findViewById(R.id.et_appraiser_role);

        // שדות מוסר
        etNameOfPresenter = findViewById(R.id.et_name_of_presenter);
        etIdOfPresenter = findViewById(R.id.et_id_of_presenter);
        etTypeOfPresenterId = findViewById(R.id.et_type_of_presenter_id);
        etRoleOfPresenter = findViewById(R.id.et_role_of_presenter);
        etHolderStatus = findViewById(R.id.et_holder_status);

        // כפתורים
        btnSave = findViewById(R.id.btn_save);
        btnFinish = findViewById(R.id.btn_finish);

        // ✅ תיקון: חיפוש MaterialCardView במקום MaterialButton
        backButtonCard = findViewById(R.id.back_button_card);
    }

    /**
     * ✅ אתחול ProgressStepper
     */
    private void setupProgressStepper() {
        progressHelper = new ProgressStepperHelper(
                this,
                ProgressStepperHelper.STEP_APPRAISER_DETAILS, // שלב 8
                findViewById(android.R.id.content),
                projectId
        );
        progressHelper.initialize();
    }

    /**
     * ✅ הגדרת Listeners לכל הכפתורים
     */
    private void setupListeners() {
        // ✅ תיקון: listener על MaterialCardView
        if (backButtonCard != null) {
            backButtonCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    progressHelper.moveToPreviousStep(); // → חזרה לשלב 7
                }
            });
        } else {
            Log.w("PresenterDetailsActivity", "back_button_card not found in layout");
        }

        // כפתור שמירה (ללא מעבר)
        if (btnSave != null) {
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    save(false);
                }
            });
        }

        // כפתור סיום (שמירה + מעבר לשלב 9)
        if (btnFinish != null) {
            btnFinish.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    save(true);
                }
            });
        }
    }

    /**
     * הגדרת Observers ל-ViewModel
     */
    private void setupObservers() {
        // Observer לטופס - ממלא את השדות אם יש נתונים קיימים
        vm.getForm().observe(this, f -> {
            if (f != null) {
                putIfHas(etAppraiserName, f.appraiserName);
                putIfHas(etAppraisalDate, f.appraisalDate);
                putIfHas(etAppraiserRole, f.appraiserRole);
                putIfHas(etNameOfPresenter, f.nameOfPresenter);
                putIfHas(etIdOfPresenter, f.idOfPresenter);
                putIfHas(etTypeOfPresenterId, f.typeOfPresenterId);
                putIfHas(etRoleOfPresenter, f.roleOfPresenter);
                putIfHas(etHolderStatus, f.holderStatus);
            }
        });

        // Observer לשגיאות
        vm.getError().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                toast(msg);
            }
        });
    }

    /**
     * שמירת הנתונים
     * @param moveToNextStep האם לעבור לשלב הבא אחרי שמירה מוצלחת
     */
    private void save(boolean moveToNextStep) {
        if (projectId == null || projectId.isEmpty()) {
            toast("שגיאה: חסר מזהה פרויקט");
            return;
        }

        // קריאת נתונים מהטופס
        PresenterDetailsViewModel.Form f = new PresenterDetailsViewModel.Form();
        f.appraiserName = str(etAppraiserName);
        f.appraisalDate = str(etAppraisalDate);
        f.appraiserRole = str(etAppraiserRole);
        f.nameOfPresenter = str(etNameOfPresenter);
        f.idOfPresenter = str(etIdOfPresenter);
        f.typeOfPresenterId = str(etTypeOfPresenterId);
        f.roleOfPresenter = str(etRoleOfPresenter);
        f.holderStatus = str(etHolderStatus);

        // ✅ ולידציה
        if (isEmpty(f.appraiserName)) {
            toast("יש למלא שם שמאי");
            etAppraiserName.requestFocus();
            return;
        }

        if (!FieldValidators.isValidDate(f.appraisalDate)) {
            toast("תאריך שומה לא תקין (DD/MM/YYYY)");
            etAppraisalDate.requestFocus();
            return;
        }

        if (isEmpty(f.nameOfPresenter)) {
            toast("יש למלא שם מוסר");
            etNameOfPresenter.requestFocus();
            return;
        }

        if (!FieldValidators.isNineDigits(f.idOfPresenter) ||
                !FieldValidators.isValidIsraeliID(f.idOfPresenter)) {
            toast("ת.ז. מוסר חייבת להיות 9 ספרות תקינות");
            etIdOfPresenter.requestFocus();
            return;
        }

        // ✅ שמירה ל-Firebase דרך ViewModel
        vm.save(f).observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                toast("הנתונים נשמרו בהצלחה!");

                if (moveToNextStep) {
                    // ✅ מעבר לשלב 9 (טאבו - השלב האחרון)
                    progressHelper.moveToNextStep();
                    finish();
                }
            } else {
                toast("שגיאה בשמירת הנתונים");
            }
        });
    }

    // ===== פונקציות עזר =====

    /**
     * בדיקה האם מחרוזת ריקה
     */
    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * בדיקה האם שדה מלא
     */
    private boolean isFilled(TextInputEditText et) {
        return et != null && et.getText() != null && et.getText().length() > 0;
    }

    /**
     * מילוי שדה רק אם יש ערך ואם השדה ריק
     */
    private void putIfHas(TextInputEditText et, String v) {
        if (et != null && v != null && !v.isEmpty() && !isFilled(et)) {
            et.setText(v);
        }
    }

    /**
     * קריאת ערך מ-EditText
     */
    private String str(TextInputEditText et) {
        if (et == null || et.getText() == null) {
            return "";
        }
        return et.getText().toString().trim();
    }

    /**
     * הצגת Toast
     */
    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
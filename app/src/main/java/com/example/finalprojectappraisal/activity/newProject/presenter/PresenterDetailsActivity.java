package com.example.finalprojectappraisal.activity.newProject.presenter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.ProgressStepperHelper;
import com.example.finalprojectappraisal.activity.newProject.presenter.veiwmodel.PresenterDetailsViewModel;
import com.example.finalprojectappraisal.export.ProjectCompletionActivity;
import com.example.finalprojectappraisal.database.auth.AuthRepository;
import com.example.finalprojectappraisal.utils.FieldValidators;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * שלב 6 (האחרון): פרטי שמאי ומוסר
 */
public class PresenterDetailsActivity extends AppCompatActivity {

    // שדות טופס
    private TextInputEditText etAppraiserName, etAppraisalDate,
            etNameOfPresenter, etIdOfPresenter, etTypeOfPresenterId,
            etRoleOfPresenter, etHolderStatus;

    // ✅ שינוי ל-AutoCompleteTextView עבור הרשימה הנפתחת
    private AutoCompleteTextView etAppraiserRole;

    // כפתורים
    private MaterialButton btnSave, btnFinish;
    private MaterialCardView backButtonCard;

    // ViewModel ו-ProgressStepper
    private PresenterDetailsViewModel vm;
    private ProgressStepperHelper progressHelper;
    private String projectId;
    private FirebaseFirestore db;

    // ✅ רשימת התפקידים
    private static final String[] ROLES = new String[] {
            "שמאי מקרקעין / שמאית מקרקעין",
            "בוגר התמחות / בוגרת התמחות",
            "מתמחה"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_details);

        db = FirebaseFirestore.getInstance();

        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null || projectId.isEmpty()) {
            toast("שגיאה: חסר מזהה פרויקט");
            finish();
            return;
        }

        vm = new ViewModelProvider(this).get(PresenterDetailsViewModel.class);

        bindViews();
        setupDropdown();      // ✅ הגדרת הרשימה
        setupNameListener();  // ✅ הגדרת מאזין לשם המבקר
        setupProgressStepper();
        setupListeners();

        String uid = AuthRepository.getInstance().getCurrentUserId();
        vm.init(projectId, uid);

        setupObservers();
    }

    private void bindViews() {
        etAppraiserName = findViewById(R.id.et_appraiser_name);
        etAppraisalDate = findViewById(R.id.et_appraisal_date);

        // ✅ שימי לב שזה מקושר ל-ID ב-XML שהפכת ל-AutoCompleteTextView
        etAppraiserRole = findViewById(R.id.et_appraiser_role);

        etNameOfPresenter = findViewById(R.id.et_name_of_presenter);
        etIdOfPresenter = findViewById(R.id.et_id_of_presenter);
        etTypeOfPresenterId = findViewById(R.id.et_type_of_presenter_id);
        etRoleOfPresenter = findViewById(R.id.et_role_of_presenter);
        etHolderStatus = findViewById(R.id.et_holder_status);

        btnSave = findViewById(R.id.btn_save);
        btnFinish = findViewById(R.id.btn_finish);
        backButtonCard = findViewById(R.id.back_button_card);
    }

    // ✅ פונקציה להגדרת הרשימה הנפתחת
    private void setupDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                ROLES
        );
        etAppraiserRole.setAdapter(adapter);
        etAppraiserRole.setOnClickListener(v -> etAppraiserRole.showDropDown());
    }

    // ✅ מאזין לשם המבקר - שליפת תפקיד כשיוצאים מהשדה
    private void setupNameListener() {
        etAppraiserName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String name = etAppraiserName.getText().toString().trim();
                if (!name.isEmpty()) {
                    fetchRoleByAppraiserName(name);
                }
            }
        });
    }

    // ✅ שליפה מהאוסף הכללי לפי שם
    private void fetchRoleByAppraiserName(String name) {
        db.collection("appraisers_registry").document(name).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String savedRole = document.getString("role");
                        // ממלא אוטומטית רק אם השדה כרגע ריק
                        if (savedRole != null && etAppraiserRole.getText().toString().isEmpty()) {
                            etAppraiserRole.setText(savedRole, false);
                            toast("תפקיד של " + name + " זוהה אוטומטית");
                        }
                    }
                });
    }

    private void setupProgressStepper() {
        progressHelper = new ProgressStepperHelper(
                this,
                ProgressStepperHelper.STEP_APPRAISER_DETAILS,
                findViewById(android.R.id.content),
                projectId
        );
        progressHelper.initialize();
    }

    private void setupListeners() {
        if (backButtonCard != null) {
            backButtonCard.setOnClickListener(v -> progressHelper.moveToPreviousStep());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> save(false));
        }

        if (btnFinish != null) {
            btnFinish.setOnClickListener(v -> save(true));
        }
    }

    private void setupObservers() {
        vm.getForm().observe(this, f -> {
            if (f != null) {
                putIfHas(etAppraiserName, f.appraiserName);
                putIfHas(etAppraisalDate, f.appraisalDate);

                // מילוי ה-Dropdown
                if (f.appraiserRole != null && !f.appraiserRole.isEmpty()) {
                    etAppraiserRole.setText(f.appraiserRole, false);
                }

                putIfHas(etNameOfPresenter, f.nameOfPresenter);
                putIfHas(etIdOfPresenter, f.idOfPresenter);
                putIfHas(etTypeOfPresenterId, f.typeOfPresenterId);
                putIfHas(etRoleOfPresenter, f.roleOfPresenter);
                putIfHas(etHolderStatus, f.holderStatus);
            }
        });

        vm.getError().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                toast(msg);
            }
        });
    }

    private void save(boolean moveToNextStep) {
        if (projectId == null || projectId.isEmpty()) {
            toast("שגיאה: חסר מזהה פרויקט");
            return;
        }

        PresenterDetailsViewModel.Form f = new PresenterDetailsViewModel.Form();
        f.appraiserName = str(etAppraiserName);
        f.appraisalDate = str(etAppraisalDate);
        f.appraiserRole = etAppraiserRole.getText().toString().trim(); // ✅ לוקח מה-Dropdown
        f.nameOfPresenter = str(etNameOfPresenter);
        f.idOfPresenter = str(etIdOfPresenter);
        f.typeOfPresenterId = str(etTypeOfPresenterId);
        f.roleOfPresenter = str(etRoleOfPresenter);
        f.holderStatus = str(etHolderStatus);

        // הולכים רק על ולידציה רכה: אם המשתמש הזין ערך – נוודא שהוא תקין.
        if (!isEmpty(f.appraisalDate) && !FieldValidators.isValidDate(f.appraisalDate)) {
            toast("תאריך שומה לא תקין (DD/MM/YYYY)");
            etAppraisalDate.requestFocus();
            return;
        }

        if (!isEmpty(f.idOfPresenter)) {
            boolean isNineDigits = FieldValidators.isNineDigits(f.idOfPresenter);
            boolean isValidId    = FieldValidators.isValidIsraeliID(f.idOfPresenter);
            if (!isNineDigits || !isValidId) {
                toast("ת.ז. מוסר חייבת להיות 9 ספרות תקינות");
                etIdOfPresenter.requestFocus();
                return;
            }
        }

        // שמירה ל-Firebase דרך ViewModel
        vm.save(f).observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {

                // ✅ שמירה ל"מרשם" המבקרים הכללי לשימוש עתידי
                saveAppraiserToRegistry(f.appraiserName, f.appraiserRole);

                toast("הנתונים נשמרו בהצלחה!");

                if (moveToNextStep) {
                    // שלב אחרון – מעבר למסך סיום וייצוא ה-JSON
                    Intent intent = new Intent(this, ProjectCompletionActivity.class);
                    intent.putExtra(ProjectCompletionActivity.EXTRA_PROJECT_ID, projectId);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            } else {
                toast("שגיאה בשמירת הנתונים");
            }
        });
    }

    // ✅ פונקציה לשמירת הקשר בין השם לתפקיד באוסף נפרד
    private void saveAppraiserToRegistry(String name, String role) {
        Map<String, Object> data = new HashMap<>();
        data.put("role", role);
        db.collection("appraisers_registry").document(name)
                .set(data, SetOptions.merge());
    }

    // ===== פונקציות עזר =====

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isFilled(TextInputEditText et) {
        return et != null && et.getText() != null && et.getText().length() > 0;
    }

    private void putIfHas(TextInputEditText et, String v) {
        if (et != null && v != null && !v.isEmpty() && !isFilled(et)) {
            et.setText(v);
        }
    }

    private String str(TextInputEditText et) {
        if (et == null || et.getText() == null) {
            return "";
        }
        return et.getText().toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
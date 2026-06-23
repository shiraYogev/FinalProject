package com.example.finalprojectappraisal.activity.newProject.presenter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.vertexai.FirebaseVertexAI;
import com.google.firebase.vertexai.java.GenerativeModelFutures;
import com.google.firebase.vertexai.type.Content;
import com.google.firebase.vertexai.type.GenerateContentResponse;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.utils.HomeButtonHelper;
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
    private MaterialButton btnSave, btnFinish, btnScanId;
    private MaterialCardView backButtonCard;

    // סריקת ת.ז.
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri photoUri;
    private static final Executor SCAN_EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

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

        HomeButtonHelper.setup(this);

        db = FirebaseFirestore.getInstance();

        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null || projectId.isEmpty()) {
            toast("שגיאה: חסר מזהה פרויקט");
            finish();
            return;
        }

        vm = new ViewModelProvider(this).get(PresenterDetailsViewModel.class);

        registerCameraLauncher();
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
        btnScanId = findViewById(R.id.btn_scan_id);
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

        if (btnScanId != null) {
            btnScanId.setOnClickListener(v -> requestCameraThenLaunch());
        }
    }

    private void registerCameraLauncher() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (Boolean.TRUE.equals(granted)) {
                        launchCamera();
                    } else {
                        toast("אי אפשר לפתוח מצלמה ללא הרשאה");
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (Boolean.TRUE.equals(success) && photoUri != null) {
                        try (InputStream in = getContentResolver().openInputStream(photoUri)) {
                            Bitmap bmp = in != null ? BitmapFactory.decodeStream(in) : null;
                            if (bmp != null) {
                                scanIdCard(bmp);
                            } else {
                                toast("שגיאה בטעינת התמונה");
                            }
                        } catch (Exception e) {
                            toast("שגיאה בטעינת התמונה");
                        }
                    }
                }
        );
    }

    private void requestCameraThenLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File img = File.createTempFile("id_scan_", ".jpg", dir);
            photoUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    img
            );
            takePictureLauncher.launch(photoUri);
        } catch (Exception e) {
            toast("לא ניתן לפתוח מצלמה");
        }
    }

    private void scanIdCard(Bitmap bitmap) {
        if (btnScanId != null) {
            btnScanId.setEnabled(false);
            btnScanId.setText("סורק...");
        }

        SCAN_EXEC.execute(() -> {
            try {
                GenerativeModelFutures model = GenerativeModelFutures.from(
                        FirebaseVertexAI.getInstance().generativeModel("gemini-2.5-flash")
                );
                Content.Builder cb = new Content.Builder();
                cb.addImage(bitmap);
                cb.addText(com.example.finalprojectappraisal.classifer.gemini.GeminiPrompts.ID_CARD_SCAN_PROMPT);
                GenerateContentResponse response = model.generateContent(cb.build()).get();
                String raw = response.getText() != null ? response.getText().trim() : "";

                // נקה Markdown אם קיים
                raw = raw.replace("```json", "").replace("```", "").trim();

                JSONObject json = new JSONObject(raw);
                String fullName = json.isNull("fullName")  ? "" : json.optString("fullName",  "");
                String idNumber = json.isNull("idNumber")  ? "" : json.optString("idNumber",  "");
                String idType   = json.isNull("idType")    ? "" : json.optString("idType",    "");

                MAIN_HANDLER.post(() -> populateIdFields(fullName, idNumber, idType));

            } catch (Exception e) {
                Log.e("PresenterScan", "ID scan failed", e);
                MAIN_HANDLER.post(() -> toast("סריקת ת.ז. נכשלה: " + e.getMessage()));
            } finally {
                MAIN_HANDLER.post(() -> {
                    if (btnScanId != null) {
                        btnScanId.setEnabled(true);
                        btnScanId.setText("צלם תעודת זהות מציג");
                    }
                });
            }
        });
    }

    private void populateIdFields(String fullName, String idNumber, String idType) {
        if (!fullName.isEmpty() && etNameOfPresenter != null) {
            etNameOfPresenter.setText(fullName);
        }
        if (!idNumber.isEmpty() && etIdOfPresenter != null) {
            etIdOfPresenter.setText(idNumber);
        }
        if (!idType.isEmpty() && etTypeOfPresenterId != null) {
            etTypeOfPresenterId.setText(idType);
        }
        if (!fullName.isEmpty() || !idNumber.isEmpty()) {
            toast("פרטי המציג מולאו אוטומטית מהת.ז.");
        } else {
            toast("לא זוהו פרטים בתמונה – נסה שוב");
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

        // ולידציה רכה: אם המשתמש הזין ערך – נוודא שהוא תקין.
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
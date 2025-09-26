package com.example.finalprojectappraisal.activity.newProject.presenter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.HomePageActivity;
import com.example.finalprojectappraisal.utils.FieldValidators;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Unified screen for Appraiser + Presenter (מוסר)
 * Saves:
 *  - Project root (flat): appraiser_name, appraisal_date, appraiser_role
 *  - Project nested: presenter_details.{name_of_presenter,id_of_presenter,type_of_presenter_id,role_of_presenter,holder_status}
 * Autofill:
 *  - appraiser_name/appraiser_role from appraisers/{uid} if missing
 *  - appraisal_date default from project's creationDate (long millis) if exists, otherwise today
 * Extras:
 *  - Finish button navigates back to HomePageActivity and clears back-stack.
 */
public class PresenterDetailsActivity extends AppCompatActivity {

    private TextInputEditText etAppraiserName, etAppraisalDate, etAppraiserRole,
            etNameOfPresenter, etIdOfPresenter, etTypeOfPresenterId, etRoleOfPresenter, etHolderStatus;

    private FirebaseFirestore db;
    private String projectId;

    private final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_details);

        db = FirebaseFirestore.getInstance();
        projectId = getIntent().getStringExtra("projectId"); // חובה להעביר מהמסך הקודם

        bindViews();

        MaterialButton btnSave   = findViewById(R.id.btn_save);
        MaterialButton btnFinish = findViewById(R.id.btn_finish);

        btnSave.setOnClickListener(v -> save(false));
        btnFinish.setOnClickListener(v -> save(true)); // שומר ואז חוזר לדף הבית

        autofill();
    }

    private void bindViews() {
        etAppraiserName     = findViewById(R.id.et_appraiser_name);
        etAppraisalDate     = findViewById(R.id.et_appraisal_date);
        etAppraiserRole     = findViewById(R.id.et_appraiser_role);

        etNameOfPresenter   = findViewById(R.id.et_name_of_presenter);
        etIdOfPresenter     = findViewById(R.id.et_id_of_presenter);
        etTypeOfPresenterId = findViewById(R.id.et_type_of_presenter_id);
        etRoleOfPresenter   = findViewById(R.id.et_role_of_presenter);
        etHolderStatus      = findViewById(R.id.et_holder_status);
    }

    private void autofill() {
        if (projectId == null || projectId.isEmpty()) {
            toast("חסר projectId"); return;
        }

        db.collection("projects").document(projectId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Existing values
                        putIfHas(etAppraiserName, doc.getString("appraiser_name"));
                        putIfHas(etAppraisalDate, doc.getString("appraisal_date"));
                        putIfHas(etAppraiserRole, doc.getString("appraiser_role"));

                        Map<String, Object> presenter = safeMap(doc.get("presenter_details"));
                        if (presenter != null) {
                            putIfHas(etNameOfPresenter,   asString(presenter.get("name_of_presenter")));
                            putIfHas(etIdOfPresenter,     asString(presenter.get("id_of_presenter")));
                            putIfHas(etTypeOfPresenterId, asString(presenter.get("type_of_presenter_id")));
                            putIfHas(etRoleOfPresenter,   asString(presenter.get("role_of_presenter")));
                            putIfHas(etHolderStatus,      asString(presenter.get("holder_status")));
                        }

                        // Default appraisal_date from creationDate (millis)
                        if (!isFilled(etAppraisalDate)) {
                            Long createdMs = doc.getLong("creationDate");
                            String createdAsDate = formatMillisToDate(createdMs);
                            if (createdAsDate != null) {
                                etAppraisalDate.setText(createdAsDate);
                            } else {
                                etAppraisalDate.setText(DATE_FMT.format(new Date()));
                            }
                        }
                    } else {
                        // No doc — default today
                        if (!isFilled(etAppraisalDate)) {
                            etAppraisalDate.setText(DATE_FMT.format(new Date()));
                        }
                    }

                    // Fill appraiser defaults from profile if missing
                    autoFromAppraiserProfileIfMissing();
                })
                .addOnFailureListener(e -> {
                    if (!isFilled(etAppraisalDate)) {
                        etAppraisalDate.setText(DATE_FMT.format(new Date()));
                    }
                    autoFromAppraiserProfileIfMissing();
                });
    }

    private void autoFromAppraiserProfileIfMissing() {
        boolean needName = !isFilled(etAppraiserName);
        boolean needRole = !isFilled(etAppraiserRole);
        if (!needName && !needRole) return;

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            db.collection("appraisers").document(uid).get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.exists()) return;
                        if (needName) {
                            String name = snap.getString("fullName");
                            if (name != null && !isFilled(etAppraiserName)) etAppraiserName.setText(name);
                        }
                        if (needRole) {
                            String role = snap.getString("role");
                            if (role != null && !isFilled(etAppraiserRole)) etAppraiserRole.setText(role);
                        }
                    });
        }
    }

    /** saveAndMaybeFinish: אם finishAfterSave=true נחזור לדף הבית לאחר שמירה מוצלחת */
    private void save(boolean finishAfterSave) {
        if (projectId == null || projectId.isEmpty()) {
            toast("חסר projectId"); return;
        }

        // ------- Read form -------
        String appraiserName = str(etAppraiserName);
        String appraisalDate = str(etAppraisalDate);
        String appraiserRole = str(etAppraiserRole);

        String namePresenter  = str(etNameOfPresenter);
        String idPresenter    = str(etIdOfPresenter);
        String idType         = str(etTypeOfPresenterId);
        String rolePresenter  = str(etRoleOfPresenter);
        String holder         = str(etHolderStatus);

        // ------- Validation -------
        if (appraiserName.isEmpty()) { toast("חסר שם שמאי"); return; }
        if (!FieldValidators.isValidDate(appraisalDate)) { toast("תאריך שומה לא תקין (DD/MM/YYYY)"); return; }
        if (namePresenter.isEmpty()) { toast("חסר שם מוסר"); return; }
        if (!FieldValidators.isNineDigits(idPresenter) || !FieldValidators.isValidIsraeliID(idPresenter)) {
            toast("ת.ז. מוסר חייבת להיות 9 ספרות עם ספרת ביקורת תקינה"); return;
        }

        // ------- Build write maps -------
        Map<String, Object> projectFlat = new HashMap<>();
        projectFlat.put("appraiser_name", appraiserName);
        projectFlat.put("appraisal_date", appraisalDate);
        projectFlat.put("appraiser_role", appraiserRole);

        Map<String, Object> presenterNested = new HashMap<>();
        presenterNested.put("name_of_presenter",   namePresenter);
        presenterNested.put("id_of_presenter",     idPresenter);
        presenterNested.put("type_of_presenter_id",idType);
        presenterNested.put("role_of_presenter",   rolePresenter);
        presenterNested.put("holder_status",       holder);

        Map<String, Object> write = new HashMap<>(projectFlat);
        write.put("presenter_details", presenterNested);

        // ------- Write to Firestore -------
        DocumentReference ref = db.collection("projects").document(projectId);
        ref.set(write, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    toast("נשמר!");
                    if (finishAfterSave) {
                        navigateHome();
                    }
                })
                .addOnFailureListener(e -> toast("שגיאה: " + e.getMessage()));
    }

    private void navigateHome() {
        // מחזיר לדף הבית ומנקה את ה-back stack כדי שהמשתמש לא יחזור לפה בטעות
        Intent intent = new Intent(this, HomePageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // מסיים את האקטיביטי הנוכחי
    }

    // ===== Helpers =====
    private boolean isFilled(TextInputEditText et) {
        return et.getText() != null && et.getText().length() > 0;
    }

    private void putIfHas(TextInputEditText et, String v) {
        if (v != null && !isFilled(et)) et.setText(v);
    }

    private String str(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object o) {
        if (o instanceof Map) return (Map<String, Object>) o;
        return null;
    }

    private String formatMillisToDate(Long ms) {
        if (ms == null) return null;
        return DATE_FMT.format(new Date(ms));
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
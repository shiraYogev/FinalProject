/**
 * Summary:
 * UI-only Activity for the unified Appraiser + Presenter screen.
 * Delegates data loading/autofill/saving to PresenterDetailsViewModel.
 * Retrieves current user id from AuthRepository when relevant.
 *
 * Notes:
 * - Validation stays here (UI concern).
 * - No direct Firestore calls in this Activity.
 */

package com.example.finalprojectappraisal.activity.newProject.presenter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.HomePageActivity;
import com.example.finalprojectappraisal.activity.newProject.UploadTabuActivity;
import com.example.finalprojectappraisal.activity.newProject.presenter.veiwmodel.PresenterDetailsViewModel;
import com.example.finalprojectappraisal.database.auth.AuthRepository;
import com.example.finalprojectappraisal.utils.FieldValidators;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class PresenterDetailsActivity extends AppCompatActivity {

    private TextInputEditText etAppraiserName, etAppraisalDate, etAppraiserRole,
            etNameOfPresenter, etIdOfPresenter, etTypeOfPresenterId, etRoleOfPresenter, etHolderStatus;

    private PresenterDetailsViewModel vm;
    private String projectId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_details);

        vm = new ViewModelProvider(this).get(PresenterDetailsViewModel.class);

        projectId = getIntent().getStringExtra("projectId"); // must be provided
        bindViews();

        MaterialButton btnSave   = findViewById(R.id.btn_save);
        MaterialButton btnFinish = findViewById(R.id.btn_finish);

        btnSave.setOnClickListener(v -> save(false));
        btnFinish.setOnClickListener(v -> save(true));

        // Provide current userId for profile autofill via AuthRepository
        String uid = AuthRepository.getInstance().getCurrentUserId();
        vm.init(projectId, uid);

        // Observe form → populate fields (one-way binding; keep user's edits)
        vm.getForm().observe(this, f -> {
            putIfHas(etAppraiserName, f.appraiserName);
            putIfHas(etAppraisalDate, f.appraisalDate);
            putIfHas(etAppraiserRole, f.appraiserRole);

            putIfHas(etNameOfPresenter,   f.nameOfPresenter);
            putIfHas(etIdOfPresenter,     f.idOfPresenter);
            putIfHas(etTypeOfPresenterId, f.typeOfPresenterId);
            putIfHas(etRoleOfPresenter,   f.roleOfPresenter);
            putIfHas(etHolderStatus,      f.holderStatus);
        });

        vm.getError().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) toast(msg);
        });
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

    /** saveAndMaybeFinish: if finishAfterSave=true we navigate to Home on success */
    private void save(boolean finishAfterSave) {
        if (projectId == null || projectId.isEmpty()) {
            toast("חסר projectId");
            return;
        }

        // --- Read form from UI ---
        PresenterDetailsViewModel.Form f = new PresenterDetailsViewModel.Form();
        f.appraiserName    = str(etAppraiserName);
        f.appraisalDate    = str(etAppraisalDate);
        f.appraiserRole    = str(etAppraiserRole);

        f.nameOfPresenter   = str(etNameOfPresenter);
        f.idOfPresenter     = str(etIdOfPresenter);
        f.typeOfPresenterId = str(etTypeOfPresenterId);
        f.roleOfPresenter   = str(etRoleOfPresenter);
        f.holderStatus      = str(etHolderStatus);

        // --- Validate (UI-level validation) ---
        if (isEmpty(f.appraiserName))          { toast("חסר שם שמאי"); return; }
        if (!FieldValidators.isValidDate(f.appraisalDate)) {
            toast("תאריך שומה לא תקין (DD/MM/YYYY)"); return;
        }
        if (isEmpty(f.nameOfPresenter))        { toast("חסר שם מוסר"); return; }
        if (!FieldValidators.isNineDigits(f.idOfPresenter)
                || !FieldValidators.isValidIsraeliID(f.idOfPresenter)) {
            toast("ת.ז. מוסר חייבת להיות 9 ספרות עם ספרת ביקורת תקינה"); return;
        }

        // --- Persist via ViewModel/Repository ---
        vm.save(f).observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) {
                toast("נשמר!");
                if (finishAfterSave) navigateHome();
            }
        });
    }

    private void navigateHome() {
        // במקום ללכת ישירות ל-Home, נעבור למסך הטאבו
        Intent intent = new Intent(this, UploadTabuActivity.class);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
        finish();
    }

    // ===== UI helpers =====
    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }
    private boolean isFilled(TextInputEditText et) {
        return et.getText() != null && et.getText().length() > 0;
    }
    private void putIfHas(TextInputEditText et, String v) {
        if (v != null && !isFilled(et)) et.setText(v);
    }
    private String str(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}

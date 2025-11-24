package com.example.finalprojectappraisal.activity.newProject.bank;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.ProgressStepperHelper;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.BankDetails;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditBankDetailsActivity extends AppCompatActivity {

    // שדות בסיסיים
    private TextInputEditText editBankName, editBranchName, editBranchEmail, editBankerName;
    private TextInputEditText editDocumentDateGre, editDocumentDateHe, editAppraisalFinalDate;
    private TextInputEditText editLoanNumber, editTypeOfLoan, editValuationNumber, editPurposeOfLoan;
    private TextInputEditText editLoanerName, editLoanerId, editIdentityOfCustomer;
    private TextInputEditText editLotNumber, editMainParcel, editSubParcel, editShortAddress;
    private TextInputEditText editPage1Header;

    private LinearProgressIndicator progressIndicator;
    private Button btnSave, btnCancel;
    private MaterialCardView backButtonCard;

    private String projectId;

    // ✅ הוספת ProgressStepperHelper
    private ProgressStepperHelper progressHelper;

    private FirebaseFirestore db;
    private static final String TAG = "EditBankDetailsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_bank_details);

        initViews();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        if (intent != null) {
            projectId = intent.getStringExtra("projectId");

            if (projectId != null && !projectId.isEmpty()) {
                loadBankDetailsFromFirebase(projectId);
            } else {
                Toast.makeText(this, "Project ID is missing", Toast.LENGTH_LONG).show();
                showLoading(false);
            }
        } else {
            showLoading(false);
        }

        // ✅ אתחול ה-Stepper
        setupProgressStepper();
        setupListeners();

        showLoading(true);
    }

    private void initViews() {
        editBankName = findViewById(R.id.editBankName);
        editBranchName = findViewById(R.id.editBranchName);
        editBranchEmail = findViewById(R.id.editBranchEmail);
        editBankerName = findViewById(R.id.editBankerName);
        editDocumentDateGre = findViewById(R.id.editDocumentDateGre);
        editDocumentDateHe = findViewById(R.id.editDocumentDateHe);
        editAppraisalFinalDate = findViewById(R.id.editAppraisalFinalDate);
        editLoanNumber = findViewById(R.id.editLoanNumber);
        editTypeOfLoan = findViewById(R.id.editTypeOfLoan);
        editValuationNumber = findViewById(R.id.editValuationNumber);
        editPurposeOfLoan = findViewById(R.id.editPurposeOfLoan);
        editLoanerName = findViewById(R.id.editLoanerName);
        editLoanerId = findViewById(R.id.editLoanerId);
        editIdentityOfCustomer = findViewById(R.id.editIdentityOfCustomer);
        editLotNumber = findViewById(R.id.editLotNumber);
        editMainParcel = findViewById(R.id.editMainParcel);
        editSubParcel = findViewById(R.id.editSubParcel);
        editShortAddress = findViewById(R.id.editShortAddress);
        editPage1Header = findViewById(R.id.editPage1Header);

        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        progressIndicator = findViewById(R.id.progressIndicator);
        backButtonCard = findViewById(R.id.back_button_card);
    }

    // ✅ פונקציה חדשה
    private void setupProgressStepper() {
        progressHelper = new ProgressStepperHelper(
                this,
                ProgressStepperHelper.STEP_BANK_EDIT, // שלב 7 - עריכת בנק
                findViewById(android.R.id.content),
                projectId
        );
        progressHelper.initialize();
    }

    // ✅ פונקציה חדשה
    private void setupListeners() {
        // כפתור חזור
        if (backButtonCard != null) {
            backButtonCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    progressHelper.moveToPreviousStep(); // → חזרה לשלב 6
                }
            });
        }

        // כפתור שמירה
        btnSave.setOnClickListener(v -> saveBankDetails());

        // כפתור ביטול
        btnCancel.setOnClickListener(v -> {
            progressHelper.moveToPreviousStep(); // → חזרה לשלב 6
        });
    }

    private void displayBankDetails(BankDetails bankDetails) {
        if (bankDetails != null) {
            setTextSafely(editBankName, bankDetails.getBankName());
            setTextSafely(editBranchName, bankDetails.getBranchName());
            setTextSafely(editBranchEmail, bankDetails.getBranchEmail());
            setTextSafely(editBankerName, bankDetails.getBankerName());
            setTextSafely(editDocumentDateGre, bankDetails.getDocumentDateGre());
            setTextSafely(editDocumentDateHe, bankDetails.getDocumentDateHe());
            setTextSafely(editAppraisalFinalDate, bankDetails.getAppraisalFinalDate());
            setTextSafely(editLoanNumber, bankDetails.getLoanNumber());
            setTextSafely(editTypeOfLoan, bankDetails.getTypeOfLoan());
            setTextSafely(editValuationNumber, bankDetails.getValuationNumber());
            setTextSafely(editPurposeOfLoan, bankDetails.getPurposeOfLoan());
            setTextSafely(editLoanerName, bankDetails.getLoanerName());
            setTextSafely(editLoanerId, bankDetails.getLoanerId());
            setTextSafely(editIdentityOfCustomer, bankDetails.getIdentityOfCustomer());
            setTextSafely(editLotNumber, bankDetails.getLotNumber());
            setTextSafely(editMainParcel, bankDetails.getMainParcel());
            setTextSafely(editSubParcel, bankDetails.getSubParcel());
            setTextSafely(editShortAddress, bankDetails.getShortAddress());
            setTextSafely(editPage1Header, bankDetails.getPage1Header());
        }
    }

    private void setTextSafely(TextInputEditText editText, String value) {
        if (editText != null) {
            editText.setText(value != null ? value : "");
        }
    }

    private String getTextSafely(TextInputEditText editText) {
        if (editText != null && editText.getText() != null) {
            return editText.getText().toString().trim();
        }
        return null;
    }

    // ✅ תיקון: שמירה ומעבר לשלב הבא
    private void saveBankDetails() {
        BankDetails updatedBankDetails = new BankDetails();

        updatedBankDetails.setBankName(getTextSafely(editBankName));
        updatedBankDetails.setBranchName(getTextSafely(editBranchName));
        updatedBankDetails.setBranchEmail(getTextSafely(editBranchEmail));
        updatedBankDetails.setBankerName(getTextSafely(editBankerName));
        updatedBankDetails.setDocumentDateGre(getTextSafely(editDocumentDateGre));
        updatedBankDetails.setDocumentDateHe(getTextSafely(editDocumentDateHe));
        updatedBankDetails.setAppraisalFinalDate(getTextSafely(editAppraisalFinalDate));
        updatedBankDetails.setLoanNumber(getTextSafely(editLoanNumber));
        updatedBankDetails.setTypeOfLoan(getTextSafely(editTypeOfLoan));
        updatedBankDetails.setValuationNumber(getTextSafely(editValuationNumber));
        updatedBankDetails.setPurposeOfLoan(getTextSafely(editPurposeOfLoan));
        updatedBankDetails.setLoanerName(getTextSafely(editLoanerName));
        updatedBankDetails.setLoanerId(getTextSafely(editLoanerId));
        updatedBankDetails.setIdentityOfCustomer(getTextSafely(editIdentityOfCustomer));
        updatedBankDetails.setLotNumber(getTextSafely(editLotNumber));
        updatedBankDetails.setMainParcel(getTextSafely(editMainParcel));
        updatedBankDetails.setSubParcel(getTextSafely(editSubParcel));
        updatedBankDetails.setShortAddress(getTextSafely(editShortAddress));
        updatedBankDetails.setPage1Header(getTextSafely(editPage1Header));

        // ✅ שמירה ל-Firebase ומעבר לשלב הבא
        showLoading(true);

        ProjectRepository.getInstance().saveBankDetailsToProject(projectId, updatedBankDetails, task -> {
            runOnUiThread(() -> {
                showLoading(false);

                if (task.isSuccessful()) {
                    Log.d(TAG, "Bank details saved successfully");
                    Toast.makeText(this, "פרטי הבנק נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();

                    // ✅ מעבר לשלב 8 (שמאי ומוסר)
                    progressHelper.moveToNextStep();
                    finish(); // ✅ סוגרים את שלב 7

                } else {
                    Log.e(TAG, "Failed to save bank details", task.getException());
                    Toast.makeText(this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressIndicator.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);
            btnCancel.setEnabled(false);
        } else {
            progressIndicator.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            btnCancel.setEnabled(true);
        }
    }

    private void loadBankDetailsFromFirebase(String projectId) {
        DocumentReference docRef = db.collection("projects").document(projectId);

        docRef.get().addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists() && document.contains("bankDetails")) {
                    BankDetails bankDetails = document.get("bankDetails", BankDetails.class);

                    if (bankDetails != null) {
                        displayBankDetails(bankDetails);
                        Log.d(TAG, "Bank Details loaded successfully");
                    } else {
                        Log.e(TAG, "Failed to parse bankDetails");
                        Toast.makeText(this, "Error loading bank details", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d(TAG, "No bank details found, starting with empty fields");
                }
            } else {
                Log.e(TAG, "Error getting document", task.getException());
                Toast.makeText(this, "Failed to load details", Toast.LENGTH_LONG).show();
            }
        });
    }
}
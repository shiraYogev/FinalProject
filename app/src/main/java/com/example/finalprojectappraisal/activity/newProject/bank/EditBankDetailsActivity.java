package com.example.finalprojectappraisal.activity.newProject.bank;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.BankDetails;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

public class EditBankDetailsActivity extends AppCompatActivity {

    // שדות בסיסיים
    private TextInputEditText editBankName, editBranchName, editBranchEmail, editBankerName;

    // שדות תאריכים
    private TextInputEditText editDocumentDateGre, editDocumentDateHe, editAppraisalFinalDate;

    // שדות הלוואה
    private TextInputEditText editLoanNumber, editTypeOfLoan, editValuationNumber, editPurposeOfLoan;

    // שדות לווה
    private TextInputEditText editLoanerName, editLoanerId, editIdentityOfCustomer;

    // שדות נכס
    private TextInputEditText editLotNumber, editMainParcel, editSubParcel, editShortAddress;

    // שדות נוספים
    private TextInputEditText editPage1Header;

    private LinearProgressIndicator progressIndicator;
    private Button btnSave, btnCancel;
    private String pdfUriString, fileName, projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_bank_details);

        // אתחול רכיבי ה-UI
        initViews();

        // קבלת הנתונים מה-Intent
        Intent intent = getIntent();
        if (intent != null) {
            String bankDetailsJson = intent.getStringExtra("bankDetails");
            projectId = intent.getStringExtra("projectId");
            pdfUriString = intent.getStringExtra("pdfUriString");
            fileName = intent.getStringExtra("fileName");

            if (bankDetailsJson != null) {
                Gson gson = new Gson();
                BankDetails bankDetails = gson.fromJson(bankDetailsJson, BankDetails.class);
                displayBankDetails(bankDetails);
            }
        }

        // הגדרת מאזין לכפתור השמירה
        btnSave.setOnClickListener(v -> saveBankDetails());

        // הגדרת מאזין לכפתור הביטול
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private void initViews() {
        // שדות בסיסיים
        editBankName = findViewById(R.id.editBankName);
        editBranchName = findViewById(R.id.editBranchName);
        editBranchEmail = findViewById(R.id.editBranchEmail);
        editBankerName = findViewById(R.id.editBankerName);

        // שדות תאריכים
        editDocumentDateGre = findViewById(R.id.editDocumentDateGre);
        editDocumentDateHe = findViewById(R.id.editDocumentDateHe);
        editAppraisalFinalDate = findViewById(R.id.editAppraisalFinalDate);

        // שדות הלוואה
        editLoanNumber = findViewById(R.id.editLoanNumber);
        editTypeOfLoan = findViewById(R.id.editTypeOfLoan);
        editValuationNumber = findViewById(R.id.editValuationNumber);
        editPurposeOfLoan = findViewById(R.id.editPurposeOfLoan);

        // שדות לווה
        editLoanerName = findViewById(R.id.editLoanerName);
        editLoanerId = findViewById(R.id.editLoanerId);
        editIdentityOfCustomer = findViewById(R.id.editIdentityOfCustomer);

        // שדות נכס
        editLotNumber = findViewById(R.id.editLotNumber);
        editMainParcel = findViewById(R.id.editMainParcel);
        editSubParcel = findViewById(R.id.editSubParcel);
        editShortAddress = findViewById(R.id.editShortAddress);

        // שדות נוספים
        editPage1Header = findViewById(R.id.editPage1Header);

        // כפתורים
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        progressIndicator = findViewById(R.id.progressIndicator);
    }

    private void displayBankDetails(BankDetails bankDetails) {
        if (bankDetails != null) {
            // שדות בסיסיים
            setTextSafely(editBankName, bankDetails.getBankName());
            setTextSafely(editBranchName, bankDetails.getBranchName());
            setTextSafely(editBranchEmail, bankDetails.getBranchEmail());
            setTextSafely(editBankerName, bankDetails.getBankerName());

            // שדות תאריכים
            setTextSafely(editDocumentDateGre, bankDetails.getDocumentDateGre());
            setTextSafely(editDocumentDateHe, bankDetails.getDocumentDateHe());
            setTextSafely(editAppraisalFinalDate, bankDetails.getAppraisalFinalDate());

            // שדות הלוואה
            setTextSafely(editLoanNumber, bankDetails.getLoanNumber());
            setTextSafely(editTypeOfLoan, bankDetails.getTypeOfLoan());
            setTextSafely(editValuationNumber, bankDetails.getValuationNumber());
            setTextSafely(editPurposeOfLoan, bankDetails.getPurposeOfLoan());

            // שדות לווה
            setTextSafely(editLoanerName, bankDetails.getLoanerName());
            setTextSafely(editLoanerId, bankDetails.getLoanerId());
            setTextSafely(editIdentityOfCustomer, bankDetails.getIdentityOfCustomer());

            // שדות נכס
            setTextSafely(editLotNumber, bankDetails.getLotNumber());
            setTextSafely(editMainParcel, bankDetails.getMainParcel());
            setTextSafely(editSubParcel, bankDetails.getSubParcel());
            setTextSafely(editShortAddress, bankDetails.getShortAddress());

            // שדות נוספים
            setTextSafely(editPage1Header, bankDetails.getPage1Header());
        }
    }

    /**
     * פונקציה עזר להגדרת טקסט בצורה בטוחה (מטפלת ב-null)
     */
    private void setTextSafely(TextInputEditText editText, String value) {
        if (editText != null) {
            editText.setText(value != null ? value : "");
        }
    }

    /**
     * פונקציה עזר לקריאת טקסט בצורה בטוחה
     */
    private String getTextSafely(TextInputEditText editText) {
        if (editText != null && editText.getText() != null) {
            return editText.getText().toString().trim();
        }
        return null;
    }

    private void saveBankDetails() {
        // יצירת אובייקט BankDetails חדש מהנתונים המעודכנים
        BankDetails updatedBankDetails = new BankDetails();

        // שדות בסיסיים
        updatedBankDetails.setBankName(getTextSafely(editBankName));
        updatedBankDetails.setBranchName(getTextSafely(editBranchName));
        updatedBankDetails.setBranchEmail(getTextSafely(editBranchEmail));
        updatedBankDetails.setBankerName(getTextSafely(editBankerName));

        // שדות תאריכים
        updatedBankDetails.setDocumentDateGre(getTextSafely(editDocumentDateGre));
        updatedBankDetails.setDocumentDateHe(getTextSafely(editDocumentDateHe));
        updatedBankDetails.setAppraisalFinalDate(getTextSafely(editAppraisalFinalDate));

        // שדות הלוואה
        updatedBankDetails.setLoanNumber(getTextSafely(editLoanNumber));
        updatedBankDetails.setTypeOfLoan(getTextSafely(editTypeOfLoan));
        updatedBankDetails.setValuationNumber(getTextSafely(editValuationNumber));
        updatedBankDetails.setPurposeOfLoan(getTextSafely(editPurposeOfLoan));

        // שדות לווה
        updatedBankDetails.setLoanerName(getTextSafely(editLoanerName));
        updatedBankDetails.setLoanerId(getTextSafely(editLoanerId));
        updatedBankDetails.setIdentityOfCustomer(getTextSafely(editIdentityOfCustomer));

        // שדות נכס
        updatedBankDetails.setLotNumber(getTextSafely(editLotNumber));
        updatedBankDetails.setMainParcel(getTextSafely(editMainParcel));
        updatedBankDetails.setSubParcel(getTextSafely(editSubParcel));
        updatedBankDetails.setShortAddress(getTextSafely(editShortAddress));

        // שדות נוספים
        updatedBankDetails.setPage1Header(getTextSafely(editPage1Header));

        // החזרת הנתונים למסך הקודם
        Intent resultIntent = new Intent();
        resultIntent.putExtra("updatedBankDetails", new Gson().toJson(updatedBankDetails));
        resultIntent.putExtra("pdfUriString", pdfUriString);
        resultIntent.putExtra("fileName", fileName);

        setResult(RESULT_OK, resultIntent);
        finish();
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
}.
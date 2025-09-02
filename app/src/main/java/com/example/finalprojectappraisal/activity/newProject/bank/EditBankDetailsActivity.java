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

    private TextInputEditText editBankName, editBranchName, editLoanerName, editLoanNumber;
    private LinearProgressIndicator progressIndicator;
    private Button btnSave, btnCancel;
    private String pdfUriString, fileName, projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_bank_details); // השם של קובץ ה-XML שלך

        // אתחול רכיבי ה-UI
        editBankName = findViewById(R.id.editBankName);
        editBranchName = findViewById(R.id.editBranchName);
        editLoanerName = findViewById(R.id.editLoanerName);
        editLoanNumber = findViewById(R.id.editLoanNumber);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        progressIndicator = findViewById(R.id.progressIndicator);

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
                // מילוי השדות בנתונים שחולצו
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

    private void displayBankDetails(BankDetails bankDetails) {
        if (bankDetails != null) {
            editBankName.setText(bankDetails.getBankName());
            editBranchName.setText(bankDetails.getBranchName());
            editLoanerName.setText(bankDetails.getLoanerName());
            editLoanNumber.setText(bankDetails.getLoanNumber());
            // TODO: מלא שדות נוספים מה-BankDetails (למשל, תאריכים וכו')
            // editDocumentDateGre.setText(...);
            // ...
        }
    }

    private void saveBankDetails() {
        showLoading(true);

        // יצירת אובייקט BankDetails חדש מהנתונים המעודכנים מהשדות
        BankDetails updatedBankDetails = new BankDetails();
        updatedBankDetails.setBankName(editBankName.getText().toString());
        updatedBankDetails.setBranchName(editBranchName.getText().toString());
        updatedBankDetails.setLoanerName(editLoanerName.getText().toString());
        updatedBankDetails.setLoanNumber(editLoanNumber.getText().toString());
        // TODO: קח את הערכים משאר השדות ומלא אותם ב-updatedBankDetails
        // updatedBankDetails.setDocumentDateGre(editDocumentDateGre.getText().toString());
        // ...

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
}
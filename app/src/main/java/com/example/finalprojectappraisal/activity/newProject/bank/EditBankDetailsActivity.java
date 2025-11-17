package com.example.finalprojectappraisal.activity.newProject.bank;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.BankDetails;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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

    // Firebase
    private FirebaseFirestore db;
    private static final String TAG = "EditBankDetailsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_bank_details);

        // אתחול רכיבי ה-UI
        initViews();
        db = FirebaseFirestore.getInstance();

        // קבלת הנתונים מה-Intent
        Intent intent = getIntent();
        if (intent != null) {
            //String bankDetailsJson = intent.getStringExtra("bankDetails");
            projectId = intent.getStringExtra("projectId");
            pdfUriString = intent.getStringExtra("pdfUriString");
            fileName = intent.getStringExtra("fileName");

            // ודא ש-projectId קיים לפני הטעינה
            if (projectId != null && !projectId.isEmpty()) {
                loadBankDetailsFromFirebase(projectId);
            } else {
                // אם אין projectId, אפשר להציג שדות ריקים
                Toast.makeText(this, "Project ID is missing. Starting with empty details.", Toast.LENGTH_LONG).show();
                showLoading(false); // הפסק את הלואדינג אם אין מזהה פרויקט
            }

            //if (bankDetailsJson != null) {
            //    Gson gson = new Gson();
            //    BankDetails bankDetails = gson.fromJson(bankDetailsJson, BankDetails.class);
            //    displayBankDetails(bankDetails);
            //}

        } else {
            showLoading(false);
        }

        // הגדרת מאזין לכפתור השמירה
        btnSave.setOnClickListener(v -> saveBankDetails());

        // הגדרת מאזין לכפתור הביטול
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        showLoading(true); // הצג לואדינג בזמן טעינת הנתונים
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

        // ** <<< השינוי מתחיל כאן >>> **
        if (projectId != null && !projectId.isEmpty()) {
            showLoading(true); // 1. מציג לואדינג

            // 2. שולח את האובייקט המעודכן ל-Firebase
            db.collection("projects").document(projectId)
                    .update("bankDetails", updatedBankDetails)

                    // 3. אם השמירה הצליחה:
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Details Saved.", Toast.LENGTH_SHORT).show();
                        // 4. מחזיר את התוצאה למסך הקודם
                        returnResultToPreviousScreen(updatedBankDetails);
                    })

                    // 5. אם השמירה נכשלה:
                    .addOnFailureListener(e -> {
                        showLoading(false); // 6. מכבה לואדינג ומציג שגיאה
                        Toast.makeText(this, "Error saving details to database.", Toast.LENGTH_LONG).show();
                    });
        } else {
            // ... טיפול במקרה שאין projectId ...
            returnResultToPreviousScreen(updatedBankDetails);
        }

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

    /**
     * טוען את פרטי הבנק מ-Firebase ומציג אותם ב-UI.
     * @param projectId מזהה הפרויקט ב-Firestore.
     */
    private void loadBankDetailsFromFirebase(String projectId) {
        DocumentReference docRef = db.collection("projects").document(projectId);

        docRef.get().addOnCompleteListener(task -> {
            showLoading(false); // הפסק את הלואדינג לאחר סיום המשימה
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists() && document.contains("bankDetails")) {

                    // ⭐️ התיקון המרכזי: קוראים ישירות לאובייקט BankDetails
                    // בלי ניסיון להמיר ל-String או להשתמש ב-GSON.
                    BankDetails bankDetails = document.get("bankDetails", BankDetails.class);

                    if (bankDetails != null) {
                        displayBankDetails(bankDetails);
                        Log.d(TAG, "Bank Details loaded successfully using Firebase Model Mapping.");
                    } else {
                        Log.e(TAG, "BankDetails field exists but failed to parse into BankDetails object.");
                        Toast.makeText(this, "Error loading bank details.", Toast.LENGTH_LONG).show();
                    }

                } else {
                    Log.d(TAG, "No bank details found for this project. Starting with empty fields.");
                    // אם המסמך קיים אך אין שדה bankDetails, נשארים עם שדות ריקים
                }
            } else {
                Log.e(TAG, "Error getting project document from Firebase: ", task.getException());
                Toast.makeText(this, "Failed to load details from database.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // שינוי קטן: כעת מקבל BankDetails במקום JSON String
    private void returnResultToPreviousScreen(BankDetails updatedBankDetails) {
        // ממירים ל-JSON רק כדי להחזיר את הנתונים למסך הקודם
        Intent resultIntent = new Intent();
        resultIntent.putExtra("updatedBankDetails", new Gson().toJson(updatedBankDetails));
        resultIntent.putExtra("pdfUriString", pdfUriString);
        resultIntent.putExtra("fileName", fileName);

        setResult(RESULT_OK, resultIntent);
        showLoading(false); // וודא שהלואדינג כבה לפני סיום
        finish();
    }
}
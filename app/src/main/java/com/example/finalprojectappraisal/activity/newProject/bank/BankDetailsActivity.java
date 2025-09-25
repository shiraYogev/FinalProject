package com.example.finalprojectappraisal.activity.newProject.bank;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.HomePageActivity;
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.model.BankDetails;
import com.example.finalprojectappraisal.classifer.gemini.GeminiBankExtractor;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

public class BankDetailsActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private Button uploadPdfButton;
    private ProgressBar progressBar;
    private String projectId;
    private static final int REVIEW_REQUEST_CODE = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bank_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // קבלת מזהה הפרויקט מה-Intent
        String testProjectId = "k5yiSucvz4Mi9ifEifUa";
        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null || projectId.trim().isEmpty()) {
            projectId = testProjectId;
        }

        // איתחול ה-UI elements
        uploadPdfButton = findViewById(R.id.uploadPdfButton);
        progressBar = findViewById(R.id.progressBar);

        // הגדרת המאזין לכפתור
        uploadPdfButton.setOnClickListener(v -> {
            Log.d("BankDetailsActivity", "Upload button clicked!");
            Toast.makeText(this, "כפתור נלחץ", Toast.LENGTH_SHORT).show();
            openFileChooser();
        });
    }

    private void openFileChooser() {
        Log.d("BankDetailsActivity", "openFileChooser called");

        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");

            String[] mimeTypes = {"application/pdf", "application/vnd.pdf"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

            startActivityForResult(intent, PICK_PDF_REQUEST);
            Log.d("BankDetailsActivity", "File chooser intent sent with ACTION_OPEN_DOCUMENT");

        } catch (Exception e) {
            Log.e("BankDetailsActivity", "ACTION_OPEN_DOCUMENT failed, trying fallback", e);

            try {
                Intent fallbackIntent = new Intent(Intent.ACTION_GET_CONTENT);
                fallbackIntent.setType("application/pdf");
                fallbackIntent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(fallbackIntent, "בחר קובץ PDF"), PICK_PDF_REQUEST);
                Log.d("BankDetailsActivity", "Fallback file chooser intent sent");

            } catch (Exception e2) {
                Log.e("BankDetailsActivity", "Both file chooser methods failed", e2);
                Toast.makeText(this, "שגיאה בפתיחת בורר הקבצים", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("BankDetailsActivity", "onActivityResult called - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri pdfUri = data.getData();
            String fileName = getFileName(pdfUri);
            // מעביר לחילוץ נתונים ואז למסך העריכה
            extractDataAndUpload(pdfUri, fileName);

        } else if (requestCode == REVIEW_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                // הנתונים אושרו ע"י המשתמש במסך העריכה, כעת אפשר לשמור
                String updatedBankDetailsJson = data.getStringExtra("updatedBankDetails");
                Uri pdfUri = Uri.parse(data.getStringExtra("pdfUriString"));
                String fileName = data.getStringExtra("fileName");

                Gson gson = new Gson();
                BankDetails updatedBankDetails = gson.fromJson(updatedBankDetailsJson, BankDetails.class);

                // כעת נבצע את השמירה בפועל
                uploadPdfAndSaveData(pdfUri, fileName, updatedBankDetails);

            } else if (resultCode == RESULT_CANCELED) {
                // המשתמש ביטל את האישור, נחזור למסך הקודם
                resetUI();
                Toast.makeText(this, "השמירה בוטלה על ידי המשתמש.", Toast.LENGTH_SHORT).show();
            }

        } else {
            Log.w("BankDetailsActivity", "File selection failed or cancelled");
            Toast.makeText(this, "בחירת הקובץ בוטלה או נכשלה", Toast.LENGTH_SHORT).show();
        }
    }

    private void extractDataAndUpload(Uri pdfUri, String fileName) {
        Log.d("BankDetailsActivity", "Starting data extraction and upload process");

        progressBar.setVisibility(View.VISIBLE);
        uploadPdfButton.setEnabled(false);

        Toast.makeText(this, "מחלץ נתונים מהמסמך...", Toast.LENGTH_LONG).show();

        // חילוץ נתונים מה-PDF באמצעות Gemini
        GeminiBankExtractor.extractBankDetailsFromPDF(this, pdfUri, new GeminiBankExtractor.BankDataExtractionCallback() {
            @Override
            public void onSuccess(BankDetails bankDetails) {
                runOnUiThread(() -> {
                    Log.d("BankDetailsActivity", "Data extraction successful");
                    Toast.makeText(BankDetailsActivity.this, "נתונים חולצו. נא לבדוק ולאשר.", Toast.LENGTH_SHORT).show();
                    // שלב חדש: העברת הנתונים למסך הביקורת
                    showEditActivity(bankDetails, pdfUri, fileName);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("BankDetailsActivity", "Data extraction failed: " + error);
                    Toast.makeText(BankDetailsActivity.this, "שגיאה בחילוץ נתונים: " + error + " מעלה קובץ בלבד.", Toast.LENGTH_LONG).show();
                    // במקרה של שגיאה, נציע למשתמש להעלות בלי חילוץ נתונים
                    uploadPdfOnly(pdfUri, fileName);
                });
            }
        });
    }
    private void showEditActivity(BankDetails bankDetails, Uri pdfUri, String fileName) {
        Intent intent = new Intent(this, EditBankDetailsActivity.class);
        // שימוש ב-Gson כדי להעביר את האובייקט כ-JSON
        Gson gson = new Gson();
        intent.putExtra("bankDetails", gson.toJson(bankDetails));
        intent.putExtra("projectId", projectId);
        intent.putExtra("pdfUriString", pdfUri.toString());
        intent.putExtra("fileName", fileName);
        startActivityForResult(intent, REVIEW_REQUEST_CODE);
    }
    private void uploadPdfAndSaveData(Uri pdfUri, String fileName, BankDetails bankDetails) {
        Log.d("BankDetailsActivity", "Uploading PDF and saving extracted data");

        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "שגיאה: מזהה פרויקט לא תקין", Toast.LENGTH_LONG).show();
            Log.e("BankDetailsActivity", "Invalid project ID: " + projectId);
            resetUI();
            return;
        }

        String sanitizedProjectId = projectId.replaceAll("[^a-zA-Z0-9_-]", "_");
        Log.d("BankDetailsActivity", "Sanitized project ID: " + sanitizedProjectId);

        // בדיקה שהפרויקט קיים
        ProjectRepository.getInstance().getProject(projectId, new OnCompleteListener<com.google.firebase.firestore.DocumentSnapshot>() {
            @Override
            public void onComplete(com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    Log.d("BankDetailsActivity", "Project exists, proceeding with upload and data save");
                    performUploadAndSave(pdfUri, fileName, sanitizedProjectId, bankDetails);
                } else {
                    Log.e("BankDetailsActivity", "Project does not exist: " + projectId);
                    Log.w("BankDetailsActivity", "Project not found in Firestore, trying upload anyway");
                    performUploadAndSave(pdfUri, fileName, sanitizedProjectId, bankDetails);
                }
            }
        });
    }

    private void performUploadAndSave(Uri pdfUri, String fileName, String sanitizedProjectId, BankDetails bankDetails) {
        Log.d("BankDetailsActivity", "Performing upload and saving bank data");

        // העלאת הקובץ ל-Firebase Storage
        ProjectRepository.getInstance().addPdfToProject(projectId, pdfUri, fileName, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(com.google.android.gms.tasks.Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d("BankDetailsActivity", "PDF upload successful, now saving bank details");

                    // שמירת נתוני הבנק ל-Firestore
                    saveBankDetailsToFirestore(bankDetails);
                } else {
                    Log.e("BankDetailsActivity", "PDF upload failed", task.getException());
                    handleUploadError(task.getException());
                }
            }
        });
    }

    private void saveBankDetailsToFirestore(BankDetails bankDetails) {
        Log.d("BankDetailsActivity", "Saving bank details to Firestore for project: " + projectId);
        // הצגת ProgressBar בזמן השמירה
        progressBar.setVisibility(View.VISIBLE);


        // שמירת הנתונים ל-Firestore תחת הפרויקט
        ProjectRepository.getInstance().saveBankDetailsToProject(projectId, bankDetails, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(com.google.android.gms.tasks.Task<Void> task) {
                runOnUiThread(() -> {
                    resetUI();

                    if (task.isSuccessful()) {
                        Log.d("BankDetailsActivity", "Bank details saved successfully");
                        Toast.makeText(BankDetailsActivity.this, "המסמך והנתונים נשמרו בהצלחה!", Toast.LENGTH_LONG).show();

                        // אפשר לעבור לעמוד הבא או לחזור
                        Intent homeIntent = new Intent(BankDetailsActivity.this, HomePageActivity.class);
                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(homeIntent);
                        finish(); // סגירת BankDetailsActivity
                    } else {
                        Log.e("BankDetailsActivity", "Failed to save bank details", task.getException());
                        Toast.makeText(BankDetailsActivity.this, "הקובץ הועלה אך שמירת הנתונים נכשלה", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void uploadPdfOnly(Uri pdfUri, String fileName) {
        Log.d("BankDetailsActivity", "Uploading PDF without data extraction");

        Toast.makeText(this, "מעלה קובץ בלי חילוץ נתונים...", Toast.LENGTH_SHORT).show();

        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "שגיאה: מזהה פרויקט לא תקין", Toast.LENGTH_LONG).show();
            resetUI();
            return;
        }

        String sanitizedProjectId = projectId.replaceAll("[^a-zA-Z0-9_-]", "_");

        ProjectRepository.getInstance().addPdfToProject(projectId, pdfUri, fileName, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(com.google.android.gms.tasks.Task<Void> task) {
                runOnUiThread(() -> {
                    resetUI();

                    if (task.isSuccessful()) {
                        Log.d("BankDetailsActivity", "PDF upload successful (without data extraction)");
                        Toast.makeText(BankDetailsActivity.this, "הקובץ הועלה בהצלחה", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("BankDetailsActivity", "PDF upload failed", task.getException());
                        handleUploadError(task.getException());
                    }
                });
            }
        });
    }

    private void handleUploadError(Exception exception) {
        String errorMessage = exception != null ? exception.getMessage() : "שגיאה לא ידועה";

        if (exception instanceof com.google.firebase.storage.StorageException) {
            com.google.firebase.storage.StorageException storageException =
                    (com.google.firebase.storage.StorageException) exception;
            Log.e("BankDetailsActivity", "Storage error code: " + storageException.getErrorCode());
            Log.e("BankDetailsActivity", "HTTP result code: " + storageException.getHttpResultCode());

            if (storageException.getHttpResultCode() == 404) {
                Toast.makeText(this, "שגיאת הרשאות Firebase Storage. אנא בדוק את Security Rules",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "שגיאה בהעלאת המסמך: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "שגיאה בהעלאת המסמך: " + errorMessage, Toast.LENGTH_LONG).show();
        }

        Log.i("BankDetailsActivity", "Upload failed. Check Firebase Storage Rules and project configuration.");
    }

    private void showDataExtractionSuccess(BankDetails bankDetails) {
        // כאן אפשר להציג דיאלוג עם הנתונים שחולצו
        Log.d("BankDetailsActivity", "Extracted bank details summary:");
        Log.d("BankDetailsActivity", "Bank: " + bankDetails.getBankName());
        Log.d("BankDetailsActivity", "Branch: " + bankDetails.getBranchName());
        Log.d("BankDetailsActivity", "Loaner: " + bankDetails.getLoanerName());
        Log.d("BankDetailsActivity", "Loan Number: " + bankDetails.getLoanNumber());

        // אפשר להוסיף כאן Intent לעמוד הבא או דיאלוג עם סיכום הנתונים
    }

    private void resetUI() {
        progressBar.setVisibility(View.GONE);
        uploadPdfButton.setEnabled(true);
    }

    private String getFileName(Uri uri) {
        String fileName = "document.pdf";

        try {
            String uriString = uri.toString();
            if (uriString.contains("/")) {
                String[] parts = uriString.split("/");
                String lastPart = parts[parts.length - 1];
                if (lastPart.contains(".pdf")) {
                    fileName = lastPart;
                }
            }

            if (fileName.equals("document.pdf")) {
                fileName = "bank_document_" + System.currentTimeMillis() + ".pdf";
            }
        } catch (Exception e) {
            Log.e("BankDetailsActivity", "Error getting filename", e);
            fileName = "bank_document_" + System.currentTimeMillis() + ".pdf";
        }

        Log.d("BankDetailsActivity", "Generated filename: " + fileName);
        return fileName;
    }
}
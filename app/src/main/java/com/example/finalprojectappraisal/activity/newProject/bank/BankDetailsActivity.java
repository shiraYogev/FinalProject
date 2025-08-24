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
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class BankDetailsActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private Button uploadPdfButton;
    private ProgressBar progressBar;
    private String projectId;

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

        // הגדרת המאזין לכפתור - ללא הרשאות!
        uploadPdfButton.setOnClickListener(v -> {
            Log.d("BankDetailsActivity", "Upload button clicked!");
            Toast.makeText(this, "כפתור נלחץ", Toast.LENGTH_SHORT).show();
            openFileChooser();
        });
    }

    private void openFileChooser() {
        Log.d("BankDetailsActivity", "openFileChooser called");

        try {
            // נסה ראשית עם ACTION_OPEN_DOCUMENT
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");

            // הוסף סוגי MIME נוספים למקרה שצריך
            String[] mimeTypes = {"application/pdf", "application/vnd.pdf"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

            startActivityForResult(intent, PICK_PDF_REQUEST);
            Log.d("BankDetailsActivity", "File chooser intent sent with ACTION_OPEN_DOCUMENT");

        } catch (Exception e) {
            Log.e("BankDetailsActivity", "ACTION_OPEN_DOCUMENT failed, trying fallback", e);

            try {
                // Fallback ל-ACTION_GET_CONTENT
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
            Log.d("BankDetailsActivity", "Selected PDF URI: " + pdfUri.toString());

            // קבלת שם הקובץ מה-URI
            String fileName = getFileName(pdfUri);

            uploadPdfToFirebase(pdfUri, fileName);
        } else {
            Log.w("BankDetailsActivity", "File selection failed or cancelled");
            Toast.makeText(this, "בחירת הקובץ בוטלה או נכשלה", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String fileName = "document.pdf"; // ברירת מחדל

        try {
            // ננסה לקבל את שם הקובץ מה-URI
            String uriString = uri.toString();
            if (uriString.contains("/")) {
                String[] parts = uriString.split("/");
                String lastPart = parts[parts.length - 1];
                if (lastPart.contains(".pdf")) {
                    fileName = lastPart;
                }
            }

            // אם לא הצלחנו, ניצור שם עם timestamp
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

    private void uploadPdfToFirebase(Uri pdfUri, String fileName) {
        Log.d("BankDetailsActivity", "Starting upload - ProjectID: " + projectId + ", URI: " + pdfUri + ", filename: " + fileName);

        // בדיקה שה-projectId תקין
        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "שגיאה: מזהה פרויקט לא תקין", Toast.LENGTH_LONG).show();
            Log.e("BankDetailsActivity", "Invalid project ID: " + projectId);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        uploadPdfButton.setEnabled(false);

        // יצירת project ID מועדכן ללא הרווחים/תווים מיוחדים
        String sanitizedProjectId = projectId.replaceAll("[^a-zA-Z0-9_-]", "_");
        Log.d("BankDetailsActivity", "Sanitized project ID: " + sanitizedProjectId);

        // בדיקה שהפרויקט קיים לפני העלאה
        ProjectRepository.getInstance().getProject(projectId, new OnCompleteListener<com.google.firebase.firestore.DocumentSnapshot>() {
            @Override
            public void onComplete(com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    Log.d("BankDetailsActivity", "Project exists, proceeding with upload");
                    // הפרויקט קיים, נמשיך עם ההעלאה
                    performUpload(pdfUri, fileName, sanitizedProjectId);
                } else {
                    Log.e("BankDetailsActivity", "Project does not exist: " + projectId);
                    progressBar.setVisibility(View.GONE);
                    uploadPdfButton.setEnabled(true);

                    // נסה בכל זאת להעלות - אולי הפרויקט קיים ב-Storage אבל לא ב-Firestore
                    Log.w("BankDetailsActivity", "Project not found in Firestore, trying upload anyway");
                    performUpload(pdfUri, fileName, sanitizedProjectId);
                }
            }
        });
    }

    private void performUpload(Uri pdfUri, String fileName, String sanitizedProjectId) {
        Log.d("BankDetailsActivity", "Performing upload with sanitized ID: " + sanitizedProjectId);

        // נשתמש ב-projectId המקורי ל-Firestore וב-sanitized ל-Storage
        ProjectRepository.getInstance().addPdfToProject(projectId, pdfUri, fileName, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(com.google.android.gms.tasks.Task<Void> task) {
                progressBar.setVisibility(View.GONE);
                uploadPdfButton.setEnabled(true);

                if (task.isSuccessful()) {
                    Log.d("BankDetailsActivity", "Upload successful");
                    Toast.makeText(BankDetailsActivity.this, "המסמך הועלה בהצלחה", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("BankDetailsActivity", "Upload failed", task.getException());
                    String errorMessage = task.getException() != null ? task.getException().getMessage() : "שגיאה לא ידועה";

                    // הוספת מידע נוסף לדיבוג
                    if (task.getException() instanceof com.google.firebase.storage.StorageException) {
                        com.google.firebase.storage.StorageException storageException = (com.google.firebase.storage.StorageException) task.getException();
                        Log.e("BankDetailsActivity", "Storage error code: " + storageException.getErrorCode());
                        Log.e("BankDetailsActivity", "HTTP result code: " + storageException.getHttpResultCode());

                        // המלצה למשתמש לגבי Firebase Rules
                        if (storageException.getHttpResultCode() == 404) {
                            Toast.makeText(BankDetailsActivity.this, "שגיאת הרשאות Firebase Storage. אנא בדוק את Security Rules", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(BankDetailsActivity.this, "שגיאה בהעלאת המסמך: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(BankDetailsActivity.this, "שגיאה בהעלאת המסמך: " + errorMessage, Toast.LENGTH_LONG).show();
                    }

                    // הצעת פתרון חלופי
                    Log.i("BankDetailsActivity", "Upload failed. Check Firebase Storage Rules and project configuration.");
                }
            }
        });
    }
}
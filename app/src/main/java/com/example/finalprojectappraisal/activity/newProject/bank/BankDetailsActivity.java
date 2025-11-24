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
import com.example.finalprojectappraisal.activity.newProject.ProgressStepperHelper;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.databinding.ActivityBankDetailsBinding;
import com.example.finalprojectappraisal.model.BankDetails;
import com.example.finalprojectappraisal.classifer.gemini.GeminiBankExtractor;
import com.google.android.gms.tasks.OnCompleteListener;

public class BankDetailsActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private static final int REVIEW_REQUEST_CODE = 2;

    private ActivityBankDetailsBinding binding;
    private ProgressStepperHelper progressHelper;

    private Button uploadPdfButton;
    private Button editManuallyButton;
    private ProgressBar progressBar;
    private String projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityBankDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String testProjectId = "k5yiSucvz4Mi9ifEifUa";
        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null || projectId.trim().isEmpty()) {
            projectId = testProjectId;
        }

        uploadPdfButton   = binding.uploadPdfButton;
        editManuallyButton = binding.editManuallyButton;
        progressBar       = binding.progressBar;

        setupProgressStepper();
        setupListeners();

        uploadPdfButton.setOnClickListener(v -> {
            Log.d("BankDetailsActivity", "Upload button clicked!");
            Toast.makeText(this, "כפתור נלחץ", Toast.LENGTH_SHORT).show();
            openFileChooser();
        });
    }

    private void setupProgressStepper() {
        // ✅ שלב 6 - פרטי בנק (העלאת PDF)
        progressHelper = new ProgressStepperHelper(
                this,
                ProgressStepperHelper.STEP_BANK_DETAILS,
                binding.getRoot(),
                projectId
        );
        progressHelper.initialize();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressHelper.moveToPreviousStep();
            }
        });

        // ✅ תיקון: עריכה ידנית עוברת ישירות לשלב 7 (EditBankDetailsActivity)
        binding.editManuallyButton.setOnClickListener(v -> {
            // פתיחת מסך עריכה ללא PDF
            Intent intent = new Intent(this, EditBankDetailsActivity.class);
            intent.putExtra("projectId", projectId);
            intent.putExtra("pdfUriString", "");
            intent.putExtra("fileName", "");
            // ⚠️ לא startActivityForResult! פשוט startActivity ו-finish
            startActivity(intent);
            finish(); // ✅ סוגרים את שלב 6
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
            Log.d("BankDetailsActivity", "File chooser intent sent");
        } catch (Exception e) {
            Log.e("BankDetailsActivity", "Error opening file chooser", e);
            Toast.makeText(this, "שגיאה בפתיחת בורר הקבצים", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri pdfUri = data.getData();
            String fileName = getFileName(pdfUri);
            extractDataAndUpload(pdfUri, fileName);
        } else {
            Log.w("BankDetailsActivity", "File selection failed or cancelled");
            Toast.makeText(this, "בחירת הקובץ בוטלה או נכשלה", Toast.LENGTH_SHORT).show();
        }
    }

    private void extractDataAndUpload(Uri pdfUri, String fileName) {
        Log.d("BankDetailsActivity", "Starting data extraction");
        progressBar.setVisibility(View.VISIBLE);
        uploadPdfButton.setEnabled(false);
        Toast.makeText(this, "מחלץ נתונים מהמסמך...", Toast.LENGTH_LONG).show();

        GeminiBankExtractor.extractBankDetailsFromPDF(this, pdfUri, new GeminiBankExtractor.BankDataExtractionCallback() {
            @Override
            public void onSuccess(BankDetails bankDetails) {
                runOnUiThread(() -> {
                    Log.d("BankDetailsActivity", "Extraction successful");
                    Toast.makeText(BankDetailsActivity.this, "נתונים חולצו בהצלחה", Toast.LENGTH_SHORT).show();

                    // ✅ שומר את הנתונים ועובר למסך עריכה (שלב 7)
                    saveAndMoveToEdit(bankDetails, pdfUri, fileName);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("BankDetailsActivity", "Extraction failed: " + error);
                    Toast.makeText(BankDetailsActivity.this, "שגיאה בחילוץ: " + error, Toast.LENGTH_LONG).show();

                    // ✅ גם במקרה של שגיאה, עובר לעריכה עם נתונים ריקים
                    saveAndMoveToEdit(new BankDetails(), pdfUri, fileName);
                });
            }
        });
    }

    // ✅ פונקציה חדשה: שומרת את הנתונים ועוברת לשלב 7
    private void saveAndMoveToEdit(BankDetails bankDetails, Uri pdfUri, String fileName) {
        // שמירה ל-Firebase
        ProjectRepository.getInstance().saveBankDetailsToProject(projectId, bankDetails, task -> {
            runOnUiThread(() -> {
                resetUI();

                if (task.isSuccessful()) {
                    Log.d("BankDetailsActivity", "Data saved successfully");

                    // ✅ מעבר לשלב 7 (EditBankDetailsActivity)
                    Intent intent = new Intent(this, EditBankDetailsActivity.class);
                    intent.putExtra("projectId", projectId);
                    intent.putExtra("pdfUriString", pdfUri.toString());
                    intent.putExtra("fileName", fileName);
                    startActivity(intent);
                    finish(); // ✅ סוגרים את שלב 6

                } else {
                    Log.e("BankDetailsActivity", "Failed to save", task.getException());
                    Toast.makeText(this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void resetUI() {
        progressBar.setVisibility(View.GONE);
        uploadPdfButton.setEnabled(true);
        if (editManuallyButton != null) editManuallyButton.setEnabled(true);
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
        return fileName;
    }
}
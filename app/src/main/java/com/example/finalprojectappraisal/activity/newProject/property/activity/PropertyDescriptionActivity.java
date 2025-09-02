package com.example.finalprojectappraisal.activity.newProject.property.activity;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.utils.RepresentativePicker;
import com.example.finalprojectappraisal.classifer.gemini.GeminiSummaryParser;
import com.example.finalprojectappraisal.classifer.gemini.GeminiSummaryService;
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.model.Image;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * מסך "תיאור כללי של הנכס":
 *  - maintenance (שיפוצים/תחזוקה)
 *  - apartment_includes (מה כלול בדירה)
 *  - property_summary (תיאור כללי, עם AI)
 */
public class PropertyDescriptionActivity extends AppCompatActivity {

    private String projectId;

    private EditText edtMaintenance;
    private EditText edtIncludes;
    private EditText edtSummary;

    private View progress;
    private Button btnSave, btnSkip;
    private Button btnAiSummary;        // only AI button
    private Button btnAiRegenerate;     // optional
    private Button btnAiRefine;         // optional

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_description);

        projectId = getIntent() != null ? getIntent().getStringExtra("projectId") : null;

        progress       = findViewById(R.id.progress);
        edtMaintenance = findViewById(R.id.edtMaintenance);
        edtIncludes    = findViewById(R.id.edtApartmentIncludes);
        edtSummary     = findViewById(R.id.edtPropertySummary);

        btnSave        = findViewById(R.id.btnSaveSummary);
        btnSkip        = findViewById(R.id.btnSkip);
        btnAiSummary   = findViewById(R.id.btnAiSummary);
        btnAiRegenerate= findViewById(R.id.btnAiRegenerate);
        btnAiRefine    = findViewById(R.id.btnAiRefine);

        btnSave.setOnClickListener(v -> saveFields());
        btnSkip.setOnClickListener(v -> goToNextPage());

        btnAiSummary.setOnClickListener(v -> runSummary(null));
        if (btnAiRegenerate != null) btnAiRegenerate.setOnClickListener(v -> runSummary(null));
        if (btnAiRefine != null) btnAiRefine.setOnClickListener(v -> {
            String hint = askForRefineHint();
            runSummary(hint);
        });
    }

    private void runSummary(@Nullable String refineHint) {
        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "חסר projectId", Toast.LENGTH_SHORT).show();
            return;
        }
        setBusy(true);

        ProjectRepository.getInstance().getImagesForProject(projectId, task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                setBusy(false);
                Toast.makeText(this, "שגיאה בשליפת תמונות", Toast.LENGTH_LONG).show();
                return;
            }

            List<Image> all = task.getResult();
            if (all == null) all = new ArrayList<>();

            // ↓↓ FIX: compute once, then assign to a final variable (no re-assignment later)
            List<Uri> tmpUris = RepresentativePicker.pickUris(all, 6);
            final List<Uri> representativeUris = (tmpUris != null) ? tmpUris : java.util.Collections.emptyList();

            if (representativeUris.isEmpty()) {
                Toast.makeText(this, "לא נמצאו תמונות מייצגות — נוציא תקציר משדות בלבד", Toast.LENGTH_SHORT).show();
            }

            ProjectRepository.getInstance().getProjectFieldsForSummary(projectId, fieldsTask -> {
                Map<String, Object> structured = (fieldsTask.isSuccessful() ? fieldsTask.getResult() : null);
                if (structured == null) structured = new HashMap<>();
                if (refineHint != null && !refineHint.trim().isEmpty()) {
                    structured.put("refine_hint", refineHint.trim());
                }

                GeminiSummaryService.generatePropertySummary(
                        this,
                        representativeUris, // ← captured safely (final)
                        structured,
                        new GeminiSummaryService.SummaryCallback() {
                            @Override public void onSuccess(GeminiSummaryParser.Result result, String rawJson) {
                                setBusy(false);
                                edtSummary.setText(result.toMultiline());
                                Toast.makeText(PropertyDescriptionActivity.this, "טיוטה מ-AI מולאה (3 משפטים)", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onError(String message) {
                                setBusy(false);
                                Toast.makeText(PropertyDescriptionActivity.this, "כשל ביצירת תקציר: " + message, Toast.LENGTH_LONG).show();
                            }
                        }
                );
            });
        });
    }


    private void saveFields() {
        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "חסר projectId", Toast.LENGTH_SHORT).show();
            return;
        }
        String maintenance = safeText(edtMaintenance);
        String includes    = safeText(edtIncludes);
        String summary     = safeText(edtSummary);

        Map<String, Object> updates = new HashMap<>();
        if (!maintenance.isEmpty()) updates.put("maintenance", maintenance);
        if (!includes.isEmpty())    updates.put("apartment_includes", includes);
        if (!summary.isEmpty())     updates.put("property_summary", summary);

        btnSave.setEnabled(false);
        progress.setVisibility(View.VISIBLE);

        ProjectRepository.getInstance().savePropertyDetails(projectId, updates, task -> {
            btnSave.setEnabled(true);
            progress.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Toast.makeText(this, "נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                goToNextPage();
            } else {
                Exception e = task.getException();
                Toast.makeText(this, "שמירה נכשלה: " + (e != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (btnAiSummary != null) btnAiSummary.setEnabled(!busy);
        if (btnAiRegenerate != null) btnAiRegenerate.setEnabled(!busy);
        if (btnAiRefine != null) btnAiRefine.setEnabled(!busy);
        if (btnSave != null) btnSave.setEnabled(!busy);
    }

    private void goToNextPage() {
        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "חסר projectId", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, com.example.finalprojectappraisal.activity.newProject.bank.BankDetailsActivity.class);
        i.putExtra("projectId", projectId);
        startActivity(i);
        // אפקט מעבר עדין (לא חובה)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        finish(); // סוגר את המסך הנוכחי בזרימה הליניארית
    }


    private String askForRefineHint() {
        // TODO: show a tiny dialog and return its text
        return "";
    }
}

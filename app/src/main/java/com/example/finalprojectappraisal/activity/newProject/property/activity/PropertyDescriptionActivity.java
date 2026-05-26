// file: app/src/main/java/com/example/finalprojectappraisal/activity/newProject/property/activity/PropertyDescriptionActivity.java
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
import com.example.finalprojectappraisal.activity.newProject.ProgressStepperHelper; // ⬅️ ייבוא נדרש
import com.example.finalprojectappraisal.databinding.ActivityPropertyDescriptionBinding; // ⬅️ ייבוא ה-Binding המתאים לשם ה-Activity (הנחתי שהשם הוא: ActivityPropertyDescriptionBinding)
import com.example.finalprojectappraisal.utils.RepresentativePicker;
import com.example.finalprojectappraisal.classifer.gemini.GeminiSummaryParser;
import com.example.finalprojectappraisal.classifer.gemini.GeminiSummaryService;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Image;
import com.example.finalprojectappraisal.model.Project;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * מסך "תיאור כללי של הנכס":
 * - apartment_includes (מה כלול בדירה)
 * - apartment_renovations (שיפוצים)
 * - property_summary (תיאור כללי, עם AI)
 *
 * טוען ערכים קיימים מ-property_details (עם נפילה לשורש אם צריך),
 * ושומר חזרה ל-property_details.
 */
public class PropertyDescriptionActivity extends AppCompatActivity {

    private static final String TAG = "PropertyDescriptionAct";

    private String projectId;

    // ⬅️ משתנים חדשים ל-Binding ול-Stepper
    private ActivityPropertyDescriptionBinding binding;
    private ProgressStepperHelper progressHelper;

    // NOTE: edtMaintenance משמש כאן בתור שדה "שיפוצים" בפועל
    private EditText edtMaintenance;
    private EditText edtIncludes;
    private EditText edtSummary;

    private View progress;
    private Button btnSave, btnSkip;
    private Button btnAiSummary;
    private Button btnAiRegenerate;
    private Button btnAiRefine;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ⬅️ שימוש ב-Binding
        binding = ActivityPropertyDescriptionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        projectId = getIntent() != null ? getIntent().getStringExtra("projectId") : null;

        // ⬅️ גישה לרכיבים דרך Binding
        progress       = binding.progress;
        edtMaintenance = binding.edtMaintenance;        // כאן נכתוב את השיפוצים
        edtIncludes    = binding.edtApartmentIncludes;  // כאן "מה כלול בדירה"
        edtSummary     = binding.edtPropertySummary;

        btnSave        = binding.btnSaveSummary;
        btnSkip        = binding.btnSkip;
        btnAiSummary   = binding.btnAiSummary;
        btnAiRegenerate= binding.btnAiRegenerate;
        btnAiRefine    = binding.btnAiRefine;

        // ⬅️ הוספת ה-Stepper וה-Listeners הכלליים
        setupProgressStepper();
        setupListeners();

        // ה-Listeners שהיו קיימים
        btnSave.setOnClickListener(v -> saveFields());
        // btnSkip.setOnClickListener(v -> goToNextPage()); // יטופל ב-setupListeners

        btnAiSummary.setOnClickListener(v -> runSummary(null));
        if (btnAiRegenerate != null) btnAiRegenerate.setOnClickListener(v -> runSummary(null));
        if (btnAiRefine != null) btnAiRefine.setOnClickListener(v -> {
            String hint = askForRefineHint();
            runSummary(hint);
        });

        // ← טעינת ערכים קיימים מה-DB למסך
        loadExistingFields();
    }

    // ⬅️ הטמעת ProgressStepperHelper
    private void setupProgressStepper() {
        progressHelper = new ProgressStepperHelper(
                this,
                ProgressStepperHelper.STEP_PROPERTY_SUMMARY, // שלב 5 (תיאור הנכס)
                binding.getRoot(),
                projectId
        );
        progressHelper.initialize();
    }

    // ⬅️ הגדרת כפתורי ניווט גנריים
    private void setupListeners() {
        // ניווט אחורה (כפתור חזור)
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressHelper.moveToPreviousStep(); // → חזרה לשלב 4
            }
        });

        // ניווט קדימה (כפתור דילוג)
        binding.btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // דילוג בלי שמירה, אבל מעבר לשלב הבא דרך הסטפר
                goToNextPage(); // קורא ל-progressHelper.moveToNextStep()
            }
        });

        // כפתור שמירה: מטופל ב-saveFields() שקורא ל-goToNextPage() אם הצליח
    }


    /** שליפת הערכים למסך מתוך property_details, עם נפילה לשורש (בעיקר לשמירה על תאימות לאחור). */
    private void loadExistingFields() {
        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "חסר projectId", Toast.LENGTH_SHORT).show();
            return;
        }
        setBusy(true);

        ProjectRepository.getInstance().getProject(projectId, task -> {
            setBusy(false);

            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                Toast.makeText(this, "לא נמצא פרויקט", Toast.LENGTH_LONG).show();
                return;
            }
            DocumentSnapshot snap = task.getResult();

            // מקור אמת מועדף: property_details.*
            @SuppressWarnings("unchecked")
            Map<String, Object> pd = (Map<String, Object>) snap.get("property_details");

            // נפילה לשורש (למקרה של נתונים היסטוריים)
            Project p = null;
            try { p = snap.toObject(Project.class); } catch (Exception ignore) {}

            // מה כלול בדירה – שדה עצמאי
            String includes = fromPd(pd, "apartment_includes", /* rootFallback */ null);

            // שיפוצים – שדה עצמאי; backward-compat: אם ריק ו-includes מכיל סמן ישן
            String renovations = fromPd(pd, "apartment_renovations", /* rootFallback */ null);
            if ((renovations == null || renovations.isEmpty()) && includes != null && !includes.isEmpty()) {
                IncludesParts parts = parseApartmentIncludes(includes);
                if (!parts.renovations.isEmpty()) {
                    renovations = parts.renovations;
                    includes    = parts.includes;
                }
            }

            // property_summary כרגיל
            String summary     = fromPd(pd, "property_summary", /* rootFallback */ null);
            if ((summary == null || summary.isEmpty()) && p != null) {
                // summary = p.getPropertySummary(); // אם יש getter בשורש
            }

            // מילוי בשדות המסך
            edtMaintenance.setText(safe(renovations));
            edtIncludes.setText(safe(includes));
            edtSummary.setText(safe(summary));
        });
    }

    // ===================== AI Summary =====================

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
                        representativeUris,
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

    // ===================== Save =====================

    private void saveFields() {
        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "חסר projectId לשמירה", Toast.LENGTH_SHORT).show();
            return;
        }

        // edtMaintenance = שיפוצים, edtIncludes = מה כלול בדירה
        String renovations = safeText(edtMaintenance);
        String includes    = safeText(edtIncludes);
        String summary     = safeText(edtSummary);

        Map<String, Object> updates = new HashMap<>();

        // שמירה נפרדת לשני השדות
        updates.put("apartment_includes", includes);
        updates.put("apartment_renovations", renovations);

        // לא נוגעים יותר בשדה maintenance כאן – הוא מנוהל ב-PropertyDetailsActivity

        if (!summary.isEmpty()) {
            updates.put("property_summary", summary);
        }

        btnSave.setEnabled(false);
        progress.setVisibility(View.VISIBLE);

        // נשמר תחת property_details
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

    // ===================== Helpers =====================

    private String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private String safe(String s) { return s == null ? "" : s; }

    private void setBusy(boolean busy) {
        if (progress != null) progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        // ⬅️ גישה לרכיבים דרך Binding
        if (binding.btnAiSummary != null) binding.btnAiSummary.setEnabled(!busy);
        if (binding.btnAiRegenerate != null) binding.btnAiRegenerate.setEnabled(!busy);
        if (binding.btnAiRefine != null) binding.btnAiRefine.setEnabled(!busy);
        if (binding.btnSaveSummary != null) binding.btnSaveSummary.setEnabled(!busy);
        if (binding.btnSkip != null) binding.btnSkip.setEnabled(!busy);
    }

    // ⬅️ התיקון: שימוש ב-progressHelper למעבר לשלב הבא
    private void goToNextPage() {
        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "חסר projectId", Toast.LENGTH_SHORT).show();
            return;
        }
        // מעבר גנרי לשלב 6 (BankDetailsActivity)
        progressHelper.moveToNextStep();
        // ה-progressHelper אחראי כעת על ה-finish() וה-Intent המפורש
    }

    private String askForRefineHint() {
        // TODO: show a tiny dialog and return its text
        return "";
    }

    /** קורא ערך מ-property_details ואם אין—מחזיר fallback מהשורש/פרמטר */
    private String fromPd(Map<String, Object> pd, String key, String rootFallback) {
        if (pd != null && pd.containsKey(key) && pd.get(key) != null) {
            Object v = pd.get(key);
            return v == null ? "" : String.valueOf(v);
        }
        return rootFallback == null ? "" : rootFallback;
    }

    // ===== backward-compat: פירוק שדה apartment_includes ישן שהכיל גם שיפוצים =====

    /** מבנה פנימי לצורך backward-compat בלבד. */
    private static class IncludesParts {
        String includes = "";
        String renovations = "";
    }

    /** מפרק את הפורמט הישן "<כולל בדירה> | שיפוצים: <טקסט>" לצורך נתונים היסטוריים בלבד. */
    private IncludesParts parseApartmentIncludes(String combined) {
        IncludesParts res = new IncludesParts();
        if (combined == null) return res;

        String s = combined.trim();
        if (s.isEmpty()) return res;

        final String marker = "| שיפוצים:";
        final String markerStart = "שיפוצים:";

        int idx = s.indexOf(marker);
        if (idx >= 0) {
            res.includes = s.substring(0, idx).trim();
            res.renovations = s.substring(idx + marker.length()).trim();
            return res;
        }

        if (s.startsWith(markerStart)) {
            res.includes = "";
            res.renovations = s.substring(markerStart.length()).trim();
            return res;
        }

        res.includes = s;
        res.renovations = "";
        return res;
    }
}
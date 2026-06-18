// file: app/src/main/java/com/example/finalprojectappraisal/activity/newProject/ApartmentDetailsActivity.java
package com.example.finalprojectappraisal.activity.newProject.property.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.utils.HomeButtonHelper;
import com.example.finalprojectappraisal.activity.newProject.ProgressStepperHelper;
import com.example.finalprojectappraisal.adapter.ApartmentDetailsAdapter;
import com.example.finalprojectappraisal.classifer.gemini.GeminiJsonParser;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.databinding.ActivityApartmentDetailsBinding;
import com.example.finalprojectappraisal.model.Project;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.finalprojectappraisal.activity.newProject.property.common.utils.Choices;
import com.example.finalprojectappraisal.activity.newProject.property.common.utils.Formatters;

import com.example.finalprojectappraisal.activity.newProject.property.common.forms.FormItems.ListItem;
import com.example.finalprojectappraisal.activity.newProject.property.common.forms.FormItems.SectionItem;
import com.example.finalprojectappraisal.activity.newProject.property.common.forms.FormItems.FieldItem;

import com.example.finalprojectappraisal.activity.newProject.property.common.dialogs.FormDialogs;

import com.example.finalprojectappraisal.activity.newProject.property.common.state.ApartmentEditableState;
import com.example.finalprojectappraisal.activity.newProject.property.common.mappers.ApartmentDetailsMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * מסך עריכת פרטי דירה:
 * - מציג את כל מה שסיווגנו + שדות ידניים (חניה/מעלית/מחסן/דלתות פנים, מטבח, חדר רחצה)
 * - עריכה מתבצעת בלחיצה → דיאלוג בחירה → שמירה לפיירסטור (field-by-field).
 *
 * Note:
 * hasParking / hasStorage are pre-filled from the Project (including flags set by images step).
 */
public class ApartmentDetailsActivity extends AppCompatActivity implements ApartmentDetailsAdapter.FieldClickListener {

    public static final String EXTRA_PROJECT_ID = "projectId";

    private RecyclerView recycler;
    private ApartmentDetailsAdapter adapter;

    private ProjectRepository repo;
    private String projectId;

    // UI state for all editable apartment fields
    private ApartmentEditableState state;

    // Virtual keys for UI-only split fields (combined into one Firestore field):
    // flooring type / size
    private static final String VKEY_FLOORING_TYPE = "__ui_flooring_type";
    private static final String VKEY_FLOORING_SIZE = "__ui_flooring_size";

    private ActivityApartmentDetailsBinding binding;
    private ProgressStepperHelper progressHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityApartmentDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        HomeButtonHelper.setup(this);

        projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "חסר projectId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        repo = ProjectRepository.getInstance();

        // RecyclerView setup
        recycler = binding.recyclerEdit;
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ApartmentDetailsAdapter(new ArrayList<>(), this);
        recycler.setAdapter(adapter);

        loadProject();

        // "Save and continue" button
        Button btnSaveAll = binding.btnSaveAll;
        btnSaveAll.setText("שמור והמשך");
        btnSaveAll.setOnClickListener(v -> saveApartmentDetailsAndNext());

        setupProgressStepper();
        setupListeners();
    }

    private void setupProgressStepper() {
        progressHelper = new ProgressStepperHelper(
                this,
                ProgressStepperHelper.STEP_APARTMENT_DETAILS,
                binding.getRoot(),
                projectId
        );
        progressHelper.initialize();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> {
            // Back to step 2 (images)
            progressHelper.moveToPreviousStep();
        });
    }

    private void loadProject() {
        repo.getProject(projectId, task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                Toast.makeText(this, "לא נמצא פרויקט", Toast.LENGTH_LONG).show();
                return;
            }
            DocumentSnapshot snap = task.getResult();
            @SuppressWarnings("unchecked")
            Map<String, Object> pd = (Map<String, Object>) snap.get("property_details");
            Project p = snap.toObject(Project.class);
            if (p == null) {
                Toast.makeText(this, "שגיאה בקריאת פרויקט", Toast.LENGTH_LONG).show();
                return;
            }

            // Build editable state from project:
            // includes fields from Gemini + flags like hasParking / hasStorageRoom
            state = ApartmentEditableState.fromProject(p);

            if (state != null) {
                boolean missingElevator = state.hasElevator == null
                        || state.hasElevator.trim().isEmpty()
                        || Formatters.EM_DASH.equals(state.hasElevator.trim());
                if (missingElevator && pd != null) {
                    Object elevatorObj = pd.get(GeminiJsonParser.FirestoreKeys.HAS_ELEVATOR);
                    if (elevatorObj instanceof String) {
                        String elevator = ((String) elevatorObj).trim();
                        if (!elevator.isEmpty()) {
                            state.hasElevator = elevator;
                        }
                    }
                }
            }

            bindList();
        });
    }

    private void bindList() {
        List<ListItem> items = new ArrayList<>();

        // ===== דלת כניסה =====
        items.add(SectionItem.of("דלת כניסה"));
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.ENTRANCE_DOOR_CONDITION,
                "סוג דלת כניסה",
                Formatters.safe(state.entranceDoorCondition)
        ));

        // ===== סלון =====
        items.add(SectionItem.of("סלון"));
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.WINDOW_TYPE,
                "חלונות",
                Formatters.safe(state.windowType)
        ));
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.HAS_CENTRAL_HEATING,
                "הסקה מרכזית/קמין",
                Formatters.boolText(state.hasCentralHeating)
        ));

        // ✅ סורגים – מציג את הטקסט ("מלא"/"חלקי"/"אין")
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.HAS_BARS,
                "סורגים",
                Formatters.safe(state.hasBars)
        ));

        // ✅ מיזוג אוויר – "מלא"/"חלקי"/"אין"
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.HAS_AIR_CONDITIONING,
                "מיזוג אוויר",
                Formatters.safe(state.hasAirConditioning)
        ));

        // ריצוף – שני שדות UI נפרדים (type / size) → יישמרו יחד בשדה אחד
        items.add(FieldItem.of(
                VKEY_FLOORING_TYPE,
                "ריצוף - סוג",
                Formatters.safe(state.flooringType)
        ));
        items.add(FieldItem.of(
                VKEY_FLOORING_SIZE,
                "ריצוף - מידה",
                Formatters.safe(state.flooringSize)
        ));

        // ===== חדר שינה =====
        items.add(SectionItem.of("חדר שינה"));
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.INTERIOR_DOOR_CONDITION,
                "דלתות פנים",
                Formatters.safe(state.interiorDoorCondition)
        ));

        // ===== מטבח =====
        items.add(SectionItem.of("מטבח"));
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.KITCHEN_CONDITION,
                "מצב מטבח",
                Formatters.safe(state.kitchenCondition)
        ));

        // ===== חדר רחצה =====
        items.add(SectionItem.of("חדר רחצה"));
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.BATHROOM_FIXTURES,
                "כלים סניטריים",
                Formatters.safe(state.bathroomFixtures)
        ));

        // ===== מתקנים כלליים =====
        items.add(SectionItem.of("מתקנים כלליים"));

        // מעלית – תצוגה טקסטואלית (String: "אין", "יש (1)", "יש (2)", ...)
        String elevatorDisplay =
                (state.hasElevator != null && !state.hasElevator.trim().isEmpty())
                        ? state.hasElevator
                        : "אין";

        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.HAS_ELEVATOR,
                "מעלית",
                elevatorDisplay
        ));

        // ✅ חניה – מתבסס על state.hasParking (Boolean)
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.HAS_PARKING,
                "חניה",
                Formatters.boolText(state.hasParking) // will show "כן"/"לא"/"" according to flag
        ));

        // ✅ מחסן – מתבסס על state.hasStorage (Boolean, mapped from hasStorageRoom project field)
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.HAS_STORAGE,
                "מחסן",
                Formatters.boolText(state.hasStorage)
        ));

        adapter.submit(items);
    }

    // Save all current state as a batch and go to next step
    private void saveApartmentDetailsAndNext() {
        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "חסר projectId לשמירה", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = ApartmentDetailsMapper.toUpdates(state);

        if (updates.isEmpty()) {
            goToNextScreen();
            return;
        }

        View progress = findViewById(R.id.progress);
        if (progress != null) progress.setVisibility(View.VISIBLE);

        ProjectRepository.getInstance().saveApartmentDetails(projectId, updates, task -> {
            if (progress != null) progress.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                Toast.makeText(this, "נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                goToNextScreen();
            } else {
                Exception e = task.getException();
                Toast.makeText(this, "שמירה נכשלה: " +
                        (e != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Use ProgressStepperHelper to navigate forward
    private void goToNextScreen() {
        progressHelper.moveToNextStep();
        // no finish() needed; stepper controls navigation
    }

    // ================== FieldClickListener ==================

    @Override
    public void onFieldClicked(FieldItem item) {
        String key = item.key;

        // מעלית – free text choice from ELEVATOR_OPTIONS
        if (GeminiJsonParser.FirestoreKeys.HAS_ELEVATOR.equals(key)) {
            FormDialogs.showSingleChoice(
                    this,
                    item.title,
                    Choices.ELEVATOR_OPTIONS, // ["אין","יש (1)","יש (2)","יש (3)","יש (4)"]
                    item.value,
                    selection -> {
                        state.hasElevator = selection;

                        Map<String, Object> update = new HashMap<>();
                        update.put(GeminiJsonParser.FirestoreKeys.HAS_ELEVATOR, selection);
                        saveAndRefresh(update);
                    }
            );
            return;
        }

        // ✅ סורגים – "מלא/חלקי/אין"
        if (GeminiJsonParser.FirestoreKeys.HAS_BARS.equals(key)) {
            FormDialogs.showSingleChoice(
                    this,
                    item.title,
                    Choices.BARS_LEVELS,
                    item.value,
                    selection -> {
                        state.hasBars = selection;
                        Map<String, Object> update = new HashMap<>();
                        update.put(GeminiJsonParser.FirestoreKeys.HAS_BARS, selection);
                        saveAndRefresh(update);
                    }
            );
            return;
        }

        // בוליאנים פשוטים: חניה / מחסן / הסקה
        if (GeminiJsonParser.FirestoreKeys.HAS_PARKING.equals(key)
                || GeminiJsonParser.FirestoreKeys.HAS_STORAGE.equals(key)
                || GeminiJsonParser.FirestoreKeys.HAS_CENTRAL_HEATING.equals(key)) {

            FormDialogs.showYesNo(
                    this,
                    item.title,
                    Formatters.YES.equals(item.value),
                    selectedYes -> {
                        Map<String, Object> update = new HashMap<>();
                        boolean val = selectedYes;

                        if (GeminiJsonParser.FirestoreKeys.HAS_PARKING.equals(key)) {
                            state.hasParking = val;
                        }
                        if (GeminiJsonParser.FirestoreKeys.HAS_STORAGE.equals(key)) {
                            state.hasStorage = val;
                        }
                        if (GeminiJsonParser.FirestoreKeys.HAS_CENTRAL_HEATING.equals(key)) {
                            state.hasCentralHeating = val;
                        }

                        update.put(key, val);
                        saveAndRefresh(update);
                    }
            );
            return;
        }

        // ✅ מיזוג אוויר – "מלא/חלקי/אין"
        if (GeminiJsonParser.FirestoreKeys.HAS_AIR_CONDITIONING.equals(key)) {
            FormDialogs.showSingleChoice(
                    this,
                    item.title,
                    Choices.AIR_CONDITIONING_LEVELS,
                    item.value,
                    selection -> {
                        state.hasAirConditioning = selection;
                        Map<String, Object> update = new HashMap<>();
                        update.put(GeminiJsonParser.FirestoreKeys.HAS_AIR_CONDITIONING, selection);
                        saveAndRefresh(update);
                    }
            );
            return;
        }

        // ✅ ריצוף – שני שדות וירטואליים שמעדכנים שדה Firestore אחד
        if (VKEY_FLOORING_TYPE.equals(key)) {
            FormDialogs.showSingleChoice(
                    this,
                    item.title,
                    Choices.FLOORING_TYPES,
                    item.value,
                    selection -> {
                        state.flooringType = selection;
                        commitFlooringCombined();
                    }
            );
            return;
        }
        if (VKEY_FLOORING_SIZE.equals(key)) {
            FormDialogs.showSingleChoice(
                    this,
                    item.title,
                    Choices.FLOORING_SIZES,
                    item.value,
                    selection -> {
                        state.flooringSize = selection;
                        commitFlooringCombined();
                    }
            );
            return;
        }

        // ✅ דלתות פנים
        if (GeminiJsonParser.FirestoreKeys.INTERIOR_DOOR_CONDITION.equals(key)) {
            FormDialogs.showSingleChoice(
                    this,
                    item.title,
                    Choices.INTERIOR_DOOR_TYPES,
                    item.value,
                    selection -> {
                        state.interiorDoorCondition = selection;
                        Map<String, Object> update = new HashMap<>();
                        update.put(GeminiJsonParser.FirestoreKeys.INTERIOR_DOOR_CONDITION, selection);
                        saveAndRefresh(update);
                    }
            );
            return;
        }

        // ✅ חדר רחצה
        if (GeminiJsonParser.FirestoreKeys.BATHROOM_FIXTURES.equals(key)) {
            FormDialogs.showSingleChoice(
                    this,
                    item.title,
                    Choices.BATHROOM_FIXTURES,
                    item.value,
                    selection -> {
                        state.bathroomFixtures = selection;
                        Map<String, Object> update = new HashMap<>();
                        update.put(GeminiJsonParser.FirestoreKeys.BATHROOM_FIXTURES, selection);
                        saveAndRefresh(update);
                    }
            );
            return;
        }

        // ✅ חלונות
        if (GeminiJsonParser.FirestoreKeys.WINDOW_TYPE.equals(key)) {
            FormDialogs.showSingleChoice(
                    this,
                    item.title,
                    Choices.WINDOW_TYPES,
                    item.value,
                    selection -> {
                        state.windowType = selection;
                        Map<String, Object> update = new HashMap<>();
                        update.put(GeminiJsonParser.FirestoreKeys.WINDOW_TYPE, selection);
                        saveAndRefresh(update);
                    }
            );
            return;
        }

        // ✅ דלת כניסה
        if (GeminiJsonParser.FirestoreKeys.ENTRANCE_DOOR_CONDITION.equals(key)) {
            FormDialogs.showSingleChoice(
                    this,
                    item.title,
                    Choices.ENTRANCE_DOOR_TYPES,
                    item.value,
                    selection -> {
                        state.entranceDoorCondition = selection;
                        Map<String, Object> update = new HashMap<>();
                        update.put(GeminiJsonParser.FirestoreKeys.ENTRANCE_DOOR_CONDITION, selection);
                        saveAndRefresh(update);
                    }
            );
            return;
        }

        // ✅ מטבח – ארונות + משטח עבודה → שדה משולב
        if (GeminiJsonParser.FirestoreKeys.KITCHEN_CONDITION.equals(key)) {

            // שלב 1: בחירת ארונות
            FormDialogs.showSingleChoice(
                    this,
                    "ארונות מטבח",
                    Choices.KITCHEN_CABINETS,
                    null,
                    selectedCabinets -> {

                        // שלב 2: בחירת משטח עבודה
                        FormDialogs.showSingleChoice(
                                this,
                                "משטח עבודה",
                                Choices.KITCHEN_WORKTOPS,
                                null,
                                selectedWorktop -> {
                                    String combined =
                                            "ארונות: " + selectedCabinets + ", משטח עבודה: " + selectedWorktop;
                                    state.kitchenCondition = combined;

                                    Map<String, Object> update = new HashMap<>();
                                    update.put(GeminiJsonParser.FirestoreKeys.KITCHEN_CONDITION, combined);
                                    saveAndRefresh(update);
                                }
                        );
                    }
            );
            return;
        }

        // Default fallback
        Toast.makeText(this, "עריכה מתקדמת לשדה זה תתווסף בהמשך 😊", Toast.LENGTH_SHORT).show();
    }

    private void commitFlooringCombined() {
        String combined = Formatters.combineFlooring(state.flooringType, state.flooringSize);
        Map<String, Object> update = new HashMap<>();
        update.put(GeminiJsonParser.FirestoreKeys.FLOORING_TYPE, combined);
        saveAndRefresh(update);
    }

    private void saveAndRefresh(Map<String, Object> updates) {
        if (updates == null || updates.isEmpty()) {
            bindList();
            return;
        }
        repo.updateMultipleFields(projectId, updates, task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(
                        this,
                        "שמירה נכשלה: " +
                                (task.getException() != null ? task.getException().getMessage() : "שגיאה"),
                        Toast.LENGTH_LONG
                ).show();
            }
            bindList();
        });
    }
}

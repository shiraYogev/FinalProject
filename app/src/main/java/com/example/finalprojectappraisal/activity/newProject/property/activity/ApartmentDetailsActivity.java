// file: app/src/main/java/com/example/finalprojectappraisal/activity/newProject/property/activity/ApartmentDetailsActivity.java
package com.example.finalprojectappraisal.activity.newProject.property.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.adapter.ApartmentDetailsAdapter;
import com.example.finalprojectappraisal.classifer.gemini.GeminiJsonParser;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Project;
import com.google.firebase.firestore.DocumentSnapshot;

import com.example.finalprojectappraisal.activity.newProject.property.common.utils.Choices;
import com.example.finalprojectappraisal.activity.newProject.property.common.utils.Formatters;
import com.example.finalprojectappraisal.activity.newProject.property.common.utils.OtherOptionFieldHelper;

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
 * Apartment details edit screen:
 * Shows AI-classified fields + manual fields (parking/elevator/storage/interior doors).
 * Editing done via dialogs → field-by-field save to Firestore.
 */
public class ApartmentDetailsActivity extends AppCompatActivity implements ApartmentDetailsAdapter.FieldClickListener {

    public static final String EXTRA_PROJECT_ID = "projectId";

    private RecyclerView recycler;
    private ApartmentDetailsAdapter adapter;

    private ProjectRepository repo;
    private String projectId;

    // Editable in-memory state (handles multi-field logic like flooring type+size)
    private ApartmentEditableState state;

    // Virtual keys for UI-only fields (not stored as is in Firestore)
    private static final String VKEY_FLOORING_TYPE = "__ui_flooring_type";
    private static final String VKEY_FLOORING_SIZE = "__ui_flooring_size";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apartment_details);

        projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "חסר projectId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        repo = ProjectRepository.getInstance();

        recycler = findViewById(R.id.recyclerEdit);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ApartmentDetailsAdapter(new ArrayList<>(), this);
        recycler.setAdapter(adapter);

        loadProject();

        Button btnSaveAll = findViewById(R.id.btnSaveAll);
        btnSaveAll.setText("שמור והמשך");
        btnSaveAll.setOnClickListener(v -> saveApartmentDetailsAndNext());
    }

    private void loadProject() {
        repo.getProject(projectId, task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                Toast.makeText(this, "לא נמצא פרויקט", Toast.LENGTH_LONG).show();
                return;
            }
            DocumentSnapshot snap = task.getResult();
            Project p = snap.toObject(Project.class);
            if (p == null) {
                Toast.makeText(this, "שגיאה בקריאת פרויקט", Toast.LENGTH_LONG).show();
                return;
            }
            state = ApartmentEditableState.fromProject(p);
            bindList();
        });
    }

    private void bindList() {
        List<ListItem> items = new ArrayList<>();

        // ===== Entrance door =====
        items.add(SectionItem.of("דלת כניסה"));
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.ENTRANCE_DOOR_CONDITION,
                "סוג דלת כניסה",
                Formatters.safe(state.entranceDoorCondition)
        ));

        // ===== Living room =====
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

        // Bars – stored as text ("מלא"/"חלקי"/"אין")
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.HAS_BARS,
                "סורגים",
                Formatters.safe(state.hasBars)
        ));

        // Air conditioning – "מלא"/"חלקי"/"אין"
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.HAS_AIR_CONDITIONING,
                "מיזוג אוויר",
                Formatters.safe(state.hasAirConditioning)
        ));

        // Flooring – split into two UI fields, combined to one Firestore string
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

        // ===== Bedroom =====
        items.add(SectionItem.of("חדר שינה"));
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.INTERIOR_DOOR_CONDITION,
                "דלתות פנים",
                Formatters.safe(state.interiorDoorCondition)
        ));

        // ===== Kitchen =====
        items.add(SectionItem.of("מטבח"));
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.KITCHEN_CONDITION,
                "מצב מטבח (טקסט משולב)",
                Formatters.safe(state.kitchenCondition)
        ));

        // ===== Bathroom =====
        items.add(SectionItem.of("חדר רחצה"));
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.BATHROOM_FIXTURES,
                "כלים סניטריים",
                Formatters.safe(state.bathroomFixtures)
        ));

        // ===== General facilities =====
        items.add(SectionItem.of("מתקנים כלליים"));

        // Elevator – boolean in DB, text in UI
        String elevatorDisplay = state.hasElevator ? "יש (1)" : "אין";
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.HAS_ELEVATOR,
                "מעלית",
                elevatorDisplay
        ));

        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.HAS_PARKING,
                "חניה",
                Formatters.boolText(state.hasParking)
        ));
        items.add(FieldItem.of(
                GeminiJsonParser.FirestoreKeys.HAS_STORAGE,
                "מחסן",
                Formatters.boolText(state.hasStorage)
        ));

        adapter.submit(items);
    }

    // Save apartment details and go to next screen
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

        android.view.View progress = findViewById(R.id.progress);
        if (progress != null) progress.setVisibility(android.view.View.VISIBLE);

        ProjectRepository.getInstance().saveApartmentDetails(projectId, updates, task -> {
            if (progress != null) progress.setVisibility(android.view.View.GONE);

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

    private void goToNextScreen() {
        Intent i = new Intent(this, PropertyDetailsActivity.class);
        i.putExtra("projectId", projectId);
        startActivity(i);
    }

    // ================== FieldClickListener ==================

    @Override
    public void onFieldClicked(FieldItem item) {
        String key = item.key;

        // Elevator – dialog: אין / יש (1-4) (bool in DB)
        if (GeminiJsonParser.FirestoreKeys.HAS_ELEVATOR.equals(key)) {
            FormDialogs.showSingleChoice(
                    this,
                    item.title,
                    Choices.ELEVATOR_OPTIONS,
                    item.value,
                    selection -> {
                        boolean val = !"אין".equals(selection);
                        state.hasElevator = val;

                        Map<String, Object> update = new HashMap<>();
                        update.put(GeminiJsonParser.FirestoreKeys.HAS_ELEVATOR, val);
                        saveAndRefresh(update);
                    }
            );
            return;
        }

        // Bars – text: "מלא/חלקי/אין"
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

        // Simple booleans: parking / storage / central heating
        if (GeminiJsonParser.FirestoreKeys.HAS_PARKING.equals(key)
                || GeminiJsonParser.FirestoreKeys.HAS_STORAGE.equals(key)
                || GeminiJsonParser.FirestoreKeys.HAS_CENTRAL_HEATING.equals(key)) {

            FormDialogs.showYesNo(this, item.title, Formatters.YES.equals(item.value), selectedYes -> {
                Map<String, Object> update = new HashMap<>();
                boolean val = selectedYes;

                if (GeminiJsonParser.FirestoreKeys.HAS_PARKING.equals(key))  state.hasParking  = val;
                if (GeminiJsonParser.FirestoreKeys.HAS_STORAGE.equals(key))  state.hasStorage  = val;
                if (GeminiJsonParser.FirestoreKeys.HAS_CENTRAL_HEATING.equals(key)) state.hasCentralHeating = val;

                update.put(key, val);
                saveAndRefresh(update);
            });
            return;
        }

        // Air conditioning – "מלא/חלקי/אין"
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
                    });
            return;
        }

        // Flooring – type (has "אחר")
        if (VKEY_FLOORING_TYPE.equals(key)) {
            FormDialogs.showSingleChoice(
                    this,
                    item.title,
                    Choices.FLOORING_TYPES,
                    item.value,
                    selection -> OtherOptionFieldHelper.handleSelection(
                            this,
                            item.title,
                            selection,
                            state.flooringType,
                            newVal -> {
                                state.flooringType = newVal;
                                commitFlooringCombined();
                            }
                    )
            );
            return;
        }

        // Flooring – size (no "אחר")
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

        // Interior doors – INTERIOR_DOOR_TYPES has "אחר"
        if (GeminiJsonParser.FirestoreKeys.INTERIOR_DOOR_CONDITION.equals(key)) {
            FormDialogs.showSingleChoice(
                    this,
                    item.title,
                    Choices.INTERIOR_DOOR_TYPES,
                    item.value,
                    selection -> OtherOptionFieldHelper.handleSelection(
                            this,
                            item.title,
                            selection,
                            state.interiorDoorCondition,
                            newVal -> {
                                state.interiorDoorCondition = newVal;
                                Map<String, Object> update = new HashMap<>();
                                update.put(GeminiJsonParser.FirestoreKeys.INTERIOR_DOOR_CONDITION, newVal);
                                saveAndRefresh(update);
                            }
                    )
            );
            return;
        }

        // Windows
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

        // Entrance door
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

        Toast.makeText(this, "עריכה מתקדמת למטבח תתווסף בהמשך 😊", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "שמירה נכשלה: " +
                        (task.getException() != null ? task.getException().getMessage() : "שגיאה"), Toast.LENGTH_LONG).show();
            }
            bindList();
        });
    }
}

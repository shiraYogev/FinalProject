// file: app/src/main/java/com/example/finalprojectappraisal/activity/newProject/ApartmentDetailsActivity.java
package com.example.finalprojectappraisal.activity.newProject.property;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.adapter.ApartmentDetailsAdapter;
import com.example.finalprojectappraisal.classifer.gemini.GeminiJsonParser;
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.model.Project;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.finalprojectappraisal.activity.newProject.property.common.utils.Choices;
import com.example.finalprojectappraisal.activity.newProject.property.common.utils.Formatters;

import com.example.finalprojectappraisal.activity.newProject.property.common.forms.FormItems.ListItem;
import com.example.finalprojectappraisal.activity.newProject.property.common.forms.FormItems.SectionItem;
import com.example.finalprojectappraisal.activity.newProject.property.common.forms.FormItems.FieldItem;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * מסך עריכת פרטי דירה: מציג את כל מה שסיווגנו + שדות ידניים (חניה/מעלית/מחסן/דלתות פנים)
 * עריכה מתבצעת בלחיצה → דיאלוג בחירה → שמירה לפיירסטור (field-by-field).
 */
public class ApartmentDetailsActivity extends AppCompatActivity implements ApartmentDetailsAdapter.FieldClickListener {

    public static final String EXTRA_PROJECT_ID = "projectId";

    private RecyclerView recycler;
    private ApartmentDetailsAdapter adapter;

    private ProjectRepository repo;
    private String projectId;

    // מצב עריכה לוקאלי כדי לטפל בתלויות (למשל ריצוף סוג+מידה)
    private EditableState state;

    // שמות השדות שאינם בתוך GeminiJsonParser.FirestoreKeys
    private static final String KEY_HAS_ELEVATOR = "has_elevator";
    private static final String KEY_HAS_PARKING  = "has_parking";
    private static final String KEY_HAS_STORAGE  = "has_storage";

    // מפתחות "וירטואליים" למסך (לא נשמרים ישירות): ריצוף סוג/מידה נפרדים
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
            state = EditableState.fromProject(p);
            bindList();
        });
    }

    private void bindList() {
        List<ListItem> items = new ArrayList<>();

        // ===== דלת כניסה =====
        items.add(SectionItem.of("דלת כניסה"));
        items.add(FieldItem.of(GeminiJsonParser.FirestoreKeys.ENTRANCE_DOOR_CONDITION, "סוג דלת כניסה", Formatters.safe(state.entranceDoorCondition)));

        // ===== סלון =====
        items.add(SectionItem.of("סלון"));
        items.add(FieldItem.of(GeminiJsonParser.FirestoreKeys.WINDOW_TYPE, "חלונות", Formatters.safe(state.windowType)));
        items.add(FieldItem.of(GeminiJsonParser.FirestoreKeys.HAS_CENTRAL_HEATING, "הסקה מרכזית/קמין", Formatters.boolText(state.hasCentralHeating)));
        items.add(FieldItem.of(GeminiJsonParser.FirestoreKeys.HAS_BARS, "סורגים", Formatters.boolText(state.hasBars)));
        // מיזוג – String "כן"/"לא"
        items.add(FieldItem.of(GeminiJsonParser.FirestoreKeys.HAS_AIR_CONDITIONING, "מיזוג אוויר", Formatters.safe(state.hasAirConditioning)));

        // ריצוף (מוצג כשני שדות נפרדים לעריכה; נשמר לשדה יחיד משולב)
        items.add(FieldItem.of(VKEY_FLOORING_TYPE, "ריצוף - סוג", Formatters.safe(state.flooringType)));
        items.add(FieldItem.of(VKEY_FLOORING_SIZE, "ריצוף - מידה", Formatters.safe(state.flooringSize)));

        // ===== חדר שינה =====
        items.add(SectionItem.of("חדר שינה"));
        items.add(FieldItem.of(GeminiJsonParser.FirestoreKeys.INTERIOR_DOOR_CONDITION, "דלתות פנים", Formatters.safe(state.interiorDoorCondition)));

        // ===== מטבח =====
        items.add(SectionItem.of("מטבח"));
        items.add(FieldItem.of(GeminiJsonParser.FirestoreKeys.KITCHEN_CONDITION, "מצב מטבח (טקסט משולב)", Formatters.safe(state.kitchenCondition)));

        // ===== חדר רחצה =====
        items.add(SectionItem.of("חדר רחצה"));
        items.add(FieldItem.of(GeminiJsonParser.FirestoreKeys.BATHROOM_FIXTURES, "כלים סניטריים", Formatters.safe(state.bathroomFixtures)));

        // ===== מתקנים כלליים =====
        items.add(SectionItem.of("מתקנים כלליים"));
        items.add(FieldItem.of(KEY_HAS_ELEVATOR, "מעלית", Formatters.boolText(state.hasElevator)));
        items.add(FieldItem.of(KEY_HAS_PARKING, "חניה", Formatters.boolText(state.hasParking)));
        items.add(FieldItem.of(KEY_HAS_STORAGE, "מחסן", Formatters.boolText(state.hasStorage)));

        adapter.submit(items);
    }

    // שמירה מרוכזת של כל פרטי הדירה ואז מעבר למסך הבא
    private void saveApartmentDetailsAndNext() {
        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "חסר projectId לשמירה", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        if (state != null) {
            // שדות טקסטואליים
            putIfNotEmpty(updates, GeminiJsonParser.FirestoreKeys.ENTRANCE_DOOR_CONDITION, state.entranceDoorCondition);
            putIfNotEmpty(updates, GeminiJsonParser.FirestoreKeys.WINDOW_TYPE,              state.windowType);
            putIfNotEmpty(updates, GeminiJsonParser.FirestoreKeys.HAS_AIR_CONDITIONING,     state.hasAirConditioning);
            putIfNotEmpty(updates, GeminiJsonParser.FirestoreKeys.INTERIOR_DOOR_CONDITION,  state.interiorDoorCondition);
            putIfNotEmpty(updates, GeminiJsonParser.FirestoreKeys.KITCHEN_CONDITION,        state.kitchenCondition);
            putIfNotEmpty(updates, GeminiJsonParser.FirestoreKeys.BATHROOM_FIXTURES,        state.bathroomFixtures);

            // ריצוף – טקסט משולב מסוג+מידה
            String flooringCombined = Formatters.combineFlooring(state.flooringType, state.flooringSize);
            if (flooringCombined != null && !flooringCombined.trim().isEmpty()) {
                updates.put(GeminiJsonParser.FirestoreKeys.FLOORING_TYPE, flooringCombined);
            }

            // בוליאנים
            updates.put(GeminiJsonParser.FirestoreKeys.HAS_CENTRAL_HEATING, state.hasCentralHeating);
            updates.put(GeminiJsonParser.FirestoreKeys.HAS_BARS,            state.hasBars);
            updates.put(KEY_HAS_ELEVATOR,                                    state.hasElevator);
            updates.put(KEY_HAS_PARKING,                                     state.hasParking);
            updates.put(KEY_HAS_STORAGE,                                     state.hasStorage);
        }

        if (updates.isEmpty()) {
            goToNextScreen();
            return;
        }

        // אם יש לך ProgressBar עם @id/progress אפשר להציג/להסתיר:
        // (ודאי שמיובא android.view.View)
        android.view.View progress = findViewById(R.id.progress);
        if (progress != null) progress.setVisibility(android.view.View.VISIBLE);

        ProjectRepository.getInstance().saveApartmentDetails(projectId, updates, task -> {
            if (progress != null) progress.setVisibility(android.view.View.GONE);

            if (task.isSuccessful()) {
                Toast.makeText(this, "נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                goToNextScreen();
            } else {
                Exception e = task.getException();
                Toast.makeText(this, "שמירה נכשלה: " + (e != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToNextScreen() {
        // אם יש לך PropertyDescriptionActivity – זה היעד הטבעי:
        Intent i = new Intent(this, PropertyDetailsActivity.class);
        i.putExtra("projectId", projectId);
        startActivity(i);
    }

    // עוזר קטן לסינון מחרוזות ריקות/מקף תצוגה
    private void putIfNotEmpty(Map<String, Object> map, String key, String value) {
        if (value == null) return;
        String v = value.trim();
        if (!v.isEmpty() && !"—".equals(v)) {
            map.put(key, v);
        }
    }


    // ================== FieldClickListener ==================

    @Override
    public void onFieldClicked(FieldItem item) {
        String key = item.key;

        // בוליאנים פשוטים
        if (KEY_HAS_ELEVATOR.equals(key) || KEY_HAS_PARKING.equals(key) || KEY_HAS_STORAGE.equals(key)
                || GeminiJsonParser.FirestoreKeys.HAS_CENTRAL_HEATING.equals(key)
                || GeminiJsonParser.FirestoreKeys.HAS_BARS.equals(key)) {
            showYesNoDialog(item, selectedYes -> {
                Map<String, Object> update = new HashMap<>();
                boolean val = selectedYes;
                // עדכון state + שמירה
                if (KEY_HAS_ELEVATOR.equals(key)) state.hasElevator = val;
                if (KEY_HAS_PARKING.equals(key))  state.hasParking  = val;
                if (KEY_HAS_STORAGE.equals(key))  state.hasStorage  = val;
                if (GeminiJsonParser.FirestoreKeys.HAS_CENTRAL_HEATING.equals(key)) state.hasCentralHeating = val;
                if (GeminiJsonParser.FirestoreKeys.HAS_BARS.equals(key)) state.hasBars = val;

                update.put(key, val);
                saveAndRefresh(update);
            });
            return;
        }

        // מיזוג אוויר – נשמר כמחרוזת "כן"/"לא"
        if (GeminiJsonParser.FirestoreKeys.HAS_AIR_CONDITIONING.equals(key)) {
            showStringOptionsDialog(item, Arrays.asList(Formatters.YES, Formatters.NO), selection -> {
                state.hasAirConditioning = selection;
                Map<String, Object> update = new HashMap<>();
                update.put(GeminiJsonParser.FirestoreKeys.HAS_AIR_CONDITIONING, selection);
                saveAndRefresh(update);
            });
            return;
        }

        // ריצוף – שני שדות וירטואליים שמעדכנים שדה Firestore אחד
        if (VKEY_FLOORING_TYPE.equals(key)) {
            showStringOptionsDialog(item, Choices.FLOORING_TYPES, selection -> {
                state.flooringType = selection;
                commitFlooringCombined();
            });
            return;
        }
        if (VKEY_FLOORING_SIZE.equals(key)) {
            showStringOptionsDialog(item, Choices.FLOORING_SIZES, selection -> {
                state.flooringSize = selection;
                commitFlooringCombined();
            });
            return;
        }

        // דלתות פנים
        if (GeminiJsonParser.FirestoreKeys.INTERIOR_DOOR_CONDITION.equals(key)) {
            showStringOptionsDialog(item, Choices.INTERIOR_DOOR_TYPES, selection -> {
                state.interiorDoorCondition = selection;
                Map<String, Object> update = new HashMap<>();
                update.put(GeminiJsonParser.FirestoreKeys.INTERIOR_DOOR_CONDITION, selection);
                saveAndRefresh(update);
            });
            return;
        }

        // חלונות
        if (GeminiJsonParser.FirestoreKeys.WINDOW_TYPE.equals(key)) {
            showStringOptionsDialog(item, Choices.WINDOW_TYPES, selection -> {
                state.windowType = selection;
                Map<String, Object> update = new HashMap<>();
                update.put(GeminiJsonParser.FirestoreKeys.WINDOW_TYPE, selection);
                saveAndRefresh(update);
            });
            return;
        }

        // דלת כניסה
        if (GeminiJsonParser.FirestoreKeys.ENTRANCE_DOOR_CONDITION.equals(key)) {
            showStringOptionsDialog(item, Choices.ENTRANCE_DOOR_TYPES, selection -> {
                state.entranceDoorCondition = selection;
                Map<String, Object> update = new HashMap<>();
                update.put(GeminiJsonParser.FirestoreKeys.ENTRANCE_DOOR_CONDITION, selection);
                saveAndRefresh(update);
            });
            return;
        }

        // מטבח (תיאור משולב) – כאן אפשר לפתוח Dialog חכם שיבחר Cabinets + Worktop ויבנה טקסט
        // כרגע: שדה טקסט חופשי/תצוגה בלבד. אם תרצי, נוסיף בהמשך דיאלוג משולב כמו בריצוף.
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
            // מרעננים את הרשימה (ע"פ ה־state הלוקאלי)
            bindList();
        });
    }

    // ======= UI helpers =======

    private void showYesNoDialog(FieldItem item, YesNoListener listener) {
        String[] choices = new String[]{Formatters.YES, Formatters.NO};
        int checked = Formatters.YES.equals(item.value) ? 0 : 1;
        new AlertDialog.Builder(this)
                .setTitle(item.title)
                .setSingleChoiceItems(choices, checked, null)
                .setPositiveButton("בחר/י", (dialog, which) -> {
                    int sel = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    listener.onPicked(sel == 0);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showStringOptionsDialog(FieldItem item, List<String> options, StringPickListener listener) {
        int checked = Math.max(0, options.indexOf(item.value));
        new AlertDialog.Builder(this)
                .setTitle(item.title)
                .setSingleChoiceItems(options.toArray(new String[0]), checked, null)
                .setPositiveButton("בחר/י", (dialog, which) -> {
                    int sel = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (sel >= 0 && sel < options.size()) {
                        listener.onPicked(options.get(sel));
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }


    // ======= Callbacks =======
    interface YesNoListener { void onPicked(boolean selectedYes); }
    interface StringPickListener { void onPicked(String selection); }

    // ======= Editable State =======
    static class EditableState {
        // Entrance
        String entranceDoorCondition;

        // Living room
        String windowType;
        boolean hasCentralHeating;
        boolean hasBars;
        String hasAirConditioning; // "כן"/"לא"
        String flooringType;       // UI field
        String flooringSize;       // UI field

        // Bedroom
        String interiorDoorCondition;

        // Kitchen (combined)
        String kitchenCondition;

        // Bathroom
        String bathroomFixtures;

        // Facilities
        boolean hasElevator;
        boolean hasParking;
        boolean hasStorage;

        static EditableState fromProject(Project p) {
            EditableState s = new EditableState();
            s.entranceDoorCondition = p.getEntranceDoorCondition();

            s.windowType = p.getWindowType();
            s.hasCentralHeating = p.isHasCentralHeating();
            s.hasBars = p.isHasBars();
            s.hasAirConditioning = p.isHasAirConditioning(); // במודל זה String

            // פירוק "ריצוף: סוג (מידה)"
            String flooring = p.getFlooringType();
            if (flooring != null) {
                // ננסה לחלץ סוג ומידה
                // דוגמא: "פורצלן גרניט (60X60)"
                int i = flooring.lastIndexOf('(');
                int j = flooring.lastIndexOf(')');
                if (i > 0 && j > i) {
                    s.flooringType = flooring.substring(0, i).trim();
                    s.flooringSize = flooring.substring(i + 1, j).trim();
                } else {
                    s.flooringType = flooring.trim();
                    s.flooringSize = null;
                }
            }

            s.interiorDoorCondition = p.getInteriorDoorCondition();
            s.kitchenCondition = p.getKitchenCondition();
            s.bathroomFixtures = p.getBathroomFixtures();

            s.hasElevator = p.isHasElevator();
            s.hasParking = p.isHasParking();
            s.hasStorage = p.isHasStorageRoom();
            return s;
        }
    }
}

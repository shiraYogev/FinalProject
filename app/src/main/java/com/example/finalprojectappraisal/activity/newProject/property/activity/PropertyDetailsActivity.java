// file: app/src/main/java/com/example/finalprojectappraisal/activity/newProject/property/activity/PropertyDetailsActivity.java
package com.example.finalprojectappraisal.activity.newProject.property.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.adapter.PropertyDetailsAdapter;
import com.example.finalprojectappraisal.activity.newProject.property.activity.PropertyDetailsActivity.FieldItem;
import com.example.finalprojectappraisal.activity.newProject.property.activity.PropertyDetailsActivity.ListItem;
import com.example.finalprojectappraisal.activity.newProject.property.activity.PropertyDetailsActivity.SectionItem;
import com.example.finalprojectappraisal.activity.newProject.property.common.utils.Choices;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Project;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Property Details with dialog-based editing, loading values from Firestore (+rich logs). */
public class PropertyDetailsActivity extends AppCompatActivity implements PropertyDetailsAdapter.FieldClickListener {

    private static final String TAG = "PropertyDetailsAct";

    private RecyclerView recycler;
    private String projectId;

    private final List<ListItem> items = new ArrayList<>();
    private PropertyDetailsAdapter adapter;
    private ProjectRepository repo;

    // ---------- small log helpers ----------
    private void logVal(String key, Object val) {
        Log.d(TAG, "bind: " + key + " = " + String.valueOf(val));
    }

    private void debugDumpProject(Project p) {
        if (p == null) { Log.w(TAG, "debugDumpProject: project == null"); return; }
        Log.d(TAG, "projectId=" + p.getProjectId()
                + " | location=" + p.getLocation()
                + " | buildingType=" + p.getBuildingType()
                + " | buildingCondition=" + p.getBuildingCondition()
                + " | floors=" + p.getNumberOfFloors()
                + " | buildingEntry=" + p.getBuildingEntry()
                + " | buildingNumber=" + p.getBuildingNumber()
                + " | zoneNumber=" + p.getZoneNumber()
                + " | cityPlanNum=" + p.getBuildingCityPlanNumber()
                + " | aptNumber=" + p.getApartmentNumber()
                + " | floorNumber=" + p.getFloorNumber()
                + " | rooms=" + p.getNumberOfRooms()
                + " | regArea=" + p.getRegisteredArea()
                + " | grossArea=" + p.getGrossArea()
                + " | airDir=" + p.getAirDirection());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_details);
        Log.d(TAG, "onCreate: started");

        recycler = findViewById(R.id.recyclerDetails);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        projectId = getIntent() != null ? getIntent().getStringExtra("projectId") : null;
        Log.d(TAG, "onCreate: received projectId=" + projectId);

        if (projectId == null || projectId.trim().isEmpty()) {
            Log.e(TAG, "onCreate: missing projectId");
            Toast.makeText(this, "חסר projectId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        repo = ProjectRepository.getInstance();
        loadProject();

        findViewById(R.id.btnSaveAll).setOnClickListener(v -> {
            Log.d(TAG, "btnSaveAll clicked");
            saveToDb();
        });
    }

    private void loadProject() {
        android.view.View progress = findViewById(R.id.progress);
        if (progress != null) progress.setVisibility(android.view.View.VISIBLE);

        repo.getProject(projectId, task -> {
            if (progress != null) progress.setVisibility(android.view.View.GONE);

            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                Toast.makeText(this, "לא נמצא פרויקט", Toast.LENGTH_LONG).show();
                return;
            }

            DocumentSnapshot snap = task.getResult();
            // מקור אמת מועדף
            @SuppressWarnings("unchecked")
            Map<String, Object> pd = (Map<String, Object>) snap.get("property_details");

            // אופציונלי: שימוש ב-Project ל-fallback לשורש
            Project p = null;
            try { p = snap.toObject(Project.class); } catch (Exception ignored) {}

            items.clear();

            // ===== פרטים כלליים / בניין =====
            items.add(new SectionItem("פרטים כלליים על הנכס/בניין"));
            items.add(FieldItem.single(
                    "property_location", "מיקום הנכס",
                    fromPd(pd, "property_location", p != null ? p.getLocation() : null),
                    Choices.PROPERTY_LOCATIONS, true
            ));
            items.add(FieldItem.single(
                    "building_type", "סוג הבניין",
                    fromPd(pd, "building_type", p != null ? p.getBuildingType() : null),
                    Choices.BUILDING_TYPES, true
            ));
            items.add(FieldItem.physicalCondition(
                    "physical_condition", "מצב הבניין",
                    fromPd(pd, "physical_condition", p != null ? p.getBuildingCondition() : null),
                    Choices.PHYSICAL_CONDITION_OPTIONS
            ));
            items.add(FieldItem.single(
                    "external_cladding", "חיפוי חיצוני",
                    fromPd(pd, "external_cladding", p != null ? p.getExternalCladding() : null),
                    Choices.EXTERNAL_CLADDING_OPTIONS, true
            ));
            items.add(FieldItem.floorsComposite(
                    "number_of_floors", "מספר קומות",
                    fromPd(pd, "number_of_floors", p != null ? p.getNumberOfFloors() : null),
                    Choices.FLOORS_REFERENCE_LEVELS, true
            ));

            items.add(FieldItem.text(
                    "building_entry", "כניסה/מבואה",
                    fromPd(pd, "building_entry", p != null ? p.getBuildingEntry() : null)
            ));
            items.add(FieldItem.text(
                    "building_number", "מספר הבניין",
                    fromPd(pd, "building_number", p != null ? p.getBuildingNumber() : null)
            ));
            items.add(FieldItem.text(
                    "zone_number", "מספר אזור",
                    fromPd(pd, "zone_number", p != null ? p.getZoneNumber() : null)
            ));
            items.add(FieldItem.text(
                    "building_city_plan_number", "מספר תכנית עירונית (תב\"ע)",
                    fromPd(pd, "building_city_plan_number", p != null ? p.getBuildingCityPlanNumber() : null)
            ));

            // ===== פרטי הדירה =====
            items.add(new SectionItem("פרטי הדירה"));
            items.add(FieldItem.text(
                    "apartment_number_municipal_form", "מס' דירה (טופס עירייה)",
                    fromPd(pd, "apartment_number_municipal_form", p != null ? p.getApartmentNumber() : null)
            ));
            items.add(FieldItem.signedInt(
                    "apartment_story", "מספר הקומה",
                    fromPd(pd, "apartment_story", p != null ? p.getFloorNumber() : null),
                    -2, 60
            ));
            items.add(FieldItem.single(
                    "number_of_rooms", "מספר חדרים",
                    fromPd(pd, "number_of_rooms", p != null ? p.getNumberOfRooms() : null),
                    Choices.ROOMS_OPTIONS, false
            ));

            // ===== מדדים =====
            items.add(new SectionItem("מדדים (שטחים)"));
            items.add(FieldItem.decimalMeters(
                    "registered_apartment_area", "שטח רשום (מ\"ר)",
                    stripMr(fromPd(pd, "registered_apartment_area", p != null ? p.getRegisteredArea() : null))
            ));
            items.add(FieldItem.decimalMeters(
                    "gross_apartment_area", "שטח ברוטו (מ\"ר)",
                    stripMr(fromPd(pd, "gross_apartment_area", p != null ? p.getGrossArea() : null))
            ));

            // ===== כיווני אוויר =====
            items.add(new SectionItem("כיווני אוויר"));
            items.add(FieldItem.multi(
                    "apartment_directions", "כיווני אוויר",
                    listFromPd(pd, "apartment_directions", p != null ? p.getAirDirection() : null),
                    Choices.AIR_DIRECTIONS
            ));

            if (adapter == null) {
                adapter = new PropertyDetailsAdapter(items, this);
                recycler.setAdapter(adapter);
            } else {
                adapter.submit(items);
            }
        });
    }

    // Helpers
    private String fromPd(Map<String, Object> pd, String key, String rootVal) {
        if (pd != null && pd.containsKey(key) && pd.get(key) != null) {
            return String.valueOf(pd.get(key));
        }
        return rootVal == null ? "" : rootVal;
    }
    @SuppressWarnings("unchecked")
    private List<String> listFromPd(Map<String, Object> pd, String key, List<String> rootList) {
        if (pd != null && pd.containsKey(key) && pd.get(key) instanceof List) {
            return new ArrayList<>((List<String>) pd.get(key));
        }
        return rootList == null ? new ArrayList<>() : new ArrayList<>(rootList);
    }
    private String stripMr(String s) {
        if (s == null) return "";
        return s.replace("מ\"ר","").trim();
    }


    // ====== בניית האייטמים למסך (כותרות + שדות) ======
    private void buildList(Project p) {
        Log.d(TAG, "buildList: start");

        items.add(new SectionItem("פרטים כלליים על הנכס/בניין"));

        logVal("property_location", p.getLocation());
        items.add(FieldItem.single("property_location", "מיקום הנכס",
                or(p.getLocation()), Choices.PROPERTY_LOCATIONS, true));

        logVal("building_type", p.getBuildingType());
        items.add(FieldItem.single("building_type", "סוג הבניין",
                or(p.getBuildingType()), Choices.BUILDING_TYPES, true));

        logVal("physical_condition", p.getBuildingCondition());
        items.add(FieldItem.physicalCondition("physical_condition", "מצב הבניין",
                or(p.getBuildingCondition()), Choices.PHYSICAL_CONDITION_OPTIONS));

        logVal("external_cladding", p.getExternalCladding());
        items.add(FieldItem.single("external_cladding", "חיפוי חיצוני",
                or(p.getExternalCladding()), Choices.EXTERNAL_CLADDING_OPTIONS, true));

        logVal("number_of_floors", p.getNumberOfFloors());
        items.add(FieldItem.floorsComposite("number_of_floors", "מספר קומות",
                or(p.getNumberOfFloors()), Choices.FLOORS_REFERENCE_LEVELS, true));

        logVal("building_entry", p.getBuildingEntry());
        items.add(FieldItem.text("building_entry", "כניסה/מבואה", or(p.getBuildingEntry())));

        logVal("building_number", p.getBuildingNumber());
        items.add(FieldItem.text("building_number", "מספר הבניין", or(p.getBuildingNumber())));

        logVal("zone_number", p.getZoneNumber());
        items.add(FieldItem.text("zone_number", "מספר אזור", or(p.getZoneNumber())));

        logVal("building_city_plan_number", p.getBuildingCityPlanNumber());
        items.add(FieldItem.text("building_city_plan_number", "מספר תכנית עירונית (תב\"ע)", or(p.getBuildingCityPlanNumber())));

        items.add(new SectionItem("פרטי הדירה"));

        logVal("apartment_number_municipal_form", p.getApartmentNumber());
        items.add(FieldItem.text("apartment_number_municipal_form", "מס' דירה (טופס עירייה)", or(p.getApartmentNumber())));

        logVal("apartment_story", p.getFloorNumber());
        items.add(FieldItem.signedInt("apartment_story", "מספר הקומה", or(p.getFloorNumber()), -2, 60));

        logVal("number_of_rooms", p.getNumberOfRooms());
        items.add(FieldItem.single("number_of_rooms", "מספר חדרים", or(p.getNumberOfRooms()), Choices.ROOMS_OPTIONS, false));

        items.add(new SectionItem("מדדים (שטחים)"));

        logVal("registered_apartment_area", p.getRegisteredArea());
        items.add(FieldItem.decimalMeters("registered_apartment_area", "שטח רשום (מ\"ר)", or(p.getRegisteredArea())));

        logVal("gross_apartment_area", p.getGrossArea());
        items.add(FieldItem.decimalMeters("gross_apartment_area", "שטח ברוטו (מ\"ר)", or(p.getGrossArea())));

        items.add(new SectionItem("כיווני אוויר"));

        logVal("apartment_directions", p.getAirDirection());
        items.add(FieldItem.multi("apartment_directions", "כיווני אוויר",
                listOrEmpty(p.getAirDirection()), Choices.AIR_DIRECTIONS));

        Log.d(TAG, "buildList: end");
    }

    private String or(String s) { return s == null ? "" : s; }
    private List<String> listOrEmpty(List<String> l) { return l == null ? new ArrayList<>() : l; }

    // ====== פתיחת דיאלוגים לפי סוג ======
    @Override
    public void onFieldClicked(FieldItem f, int position) {
        Log.d(TAG, "onFieldClicked: key=" + f.key + ", kind=" + f.kind + ", currentValue=" + f.value + ", multi=" + f.multiValue);
        switch (f.kind) {
            case SINGLE:               openSingleChoice(f, position); break;
            case MULTI:                openMultiChoice(f, position); break;
            case SIGNED_INT:           openSignedInt(f, position);   break;
            case DECIMAL_M2:           openDecimalMeters(f, position); break;
            case TEXT:                 openText(f, position);        break;
            case PHYSICAL_WITH_YEARS:  openPhysicalCondition(f, position); break;
            case FLOORS_COMPOSITE:     openFloorsComposite(f, position); break;
        }
    }

    private void openSingleChoice(FieldItem f, int pos) {
        final List<String> opts = f.options != null ? f.options : new ArrayList<>();
        CharSequence[] itemsArr = opts.toArray(new CharSequence[0]);

        new AlertDialog.Builder(this)
                .setTitle(f.title)
                .setItems(itemsArr, (d, which) -> {
                    String choice = opts.get(which);
                    if (f.allowOther && "אחר".equals(choice)) {
                        promptText("פרט/י", f.value, text -> updateValue(f, text, pos));
                    } else {
                        updateValue(f, choice, pos);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void openMultiChoice(FieldItem f, int pos) {
        final List<String> opts = f.options != null ? f.options : new ArrayList<>();
        boolean[] checked = new boolean[opts.size()];
        for (int i = 0; i < opts.size(); i++) {
            checked[i] = f.multiValue.contains(opts.get(i));
        }
        new AlertDialog.Builder(this)
                .setTitle(f.title)
                .setMultiChoiceItems(opts.toArray(new CharSequence[0]), checked,
                        (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("אישור", (dialog, which) -> {
                    List<String> sel = new ArrayList<>();
                    for (int i = 0; i < opts.size(); i++) if (checked[i]) sel.add(opts.get(i));
                    Log.d(TAG, "openMultiChoice: key=" + f.key + " selected=" + sel);
                    f.multiValue = sel;
                    adapter.notifyItemChanged(pos);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void openSignedInt(FieldItem f, int pos) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setText(f.value);

        new AlertDialog.Builder(this)
                .setTitle(f.title)
                .setView(input)
                .setPositiveButton("אישור", (d, w) -> {
                    String s = input.getText().toString().trim();
                    if (s.isEmpty()) { updateValue(f, "", pos); return; }
                    try {
                        int v = Integer.parseInt(s);
                        if (v < f.min) v = f.min;
                        if (v > f.max) v = f.max;
                        updateValue(f, String.valueOf(v), pos);
                    } catch (NumberFormatException ignored) { }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void openDecimalMeters(FieldItem f, int pos) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(f.value.replace("מ\"ר","").trim());

        new AlertDialog.Builder(this)
                .setTitle(f.title)
                .setView(input)
                .setPositiveButton("אישור", (d, w) -> {
                    String s = input.getText().toString().trim();
                    Log.d(TAG, "openDecimalMeters: key=" + f.key + " entered=" + s);
                    updateValue(f, s, pos); // הסיומת תתווסף בשמירה
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void openText(FieldItem f, int pos) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(80) });
        input.setText(f.value);

        new AlertDialog.Builder(this)
                .setTitle(f.title)
                .setView(input)
                .setPositiveButton("אישור", (d, w) -> updateValue(f, input.getText().toString().trim(), pos))
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void openPhysicalCondition(FieldItem f, int pos) {
        final List<String> opts = f.options != null ? f.options : new ArrayList<>();
        CharSequence[] itemsArr = opts.toArray(new CharSequence[0]);

        new AlertDialog.Builder(this)
                .setTitle(f.title)
                .setItems(itemsArr, (d, which) -> {
                    String choice = opts.get(which);
                    if ("בבניה".equals(choice)) {
                        EditText years = new EditText(this);
                        years.setInputType(InputType.TYPE_CLASS_NUMBER);
                        years.setHint("אומדן שנים");
                        new AlertDialog.Builder(this)
                                .setTitle("אומדן שנים")
                                .setView(years)
                                .setPositiveButton("אישור", (d2, w) -> {
                                    String y = years.getText().toString().trim();
                                    String composed = y.isEmpty() ? "בבניה" : "בבניה (" + y + " שנים – אומדן)";
                                    updateValue(f, composed, pos);
                                })
                                .setNegativeButton("ביטול", (d2, w) -> updateValue(f, "בבניה", pos))
                                .show();
                    } else {
                        updateValue(f, choice, pos);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void openFloorsComposite(FieldItem f, int pos) {
        // שלב 1: מספר קומות
        final String[] nums = new String[60];
        for (int i = 0; i < 60; i++) nums[i] = String.valueOf(i + 1);

        new AlertDialog.Builder(this)
                .setTitle("מספר קומות")
                .setItems(nums, (d, i) -> {
                    int count = i + 1;
                    // שלב 2: מעל מה
                    final List<String> opts = f.options != null ? f.options : new ArrayList<>();
                    CharSequence[] itemsArr = opts.toArray(new CharSequence[0]);
                    new AlertDialog.Builder(this)
                            .setTitle("מעל איזו קומה?")
                            .setItems(itemsArr, (d2, which) -> {
                                String level = opts.get(which);
                                if (f.allowOther && "אחר".equals(level)) {
                                    promptText("פרט/י", "", other -> {
                                        String composed = count + " קומות – מעל: " + other;
                                        updateValue(f, composed, pos);
                                    });
                                } else {
                                    String composed = count + " קומות – " + level;
                                    updateValue(f, composed, pos);
                                }
                            })
                            .setNegativeButton("ביטול", null)
                            .show();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void promptText(String title, String initial, TextConsumer onOk) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(initial == null ? "" : initial);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("אישור", (d, w) -> onOk.accept(input.getText().toString().trim()))
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void updateValue(FieldItem f, String newVal, int pos) {
        Log.d(TAG, "updateValue: key=" + f.key + " old=" + f.value + " new=" + newVal + " pos=" + pos);
        f.value = newVal == null ? "" : newVal;
        adapter.notifyItemChanged(pos);
    }

    private void goToNextScreen() {
        Log.d(TAG, "goToNextScreen: navigating to PropertyDescriptionActivity");
        Intent intent = new Intent(this, PropertyDescriptionActivity.class);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }

    // ====== שמירה ל-DB (עם לוגים) ======
    private void saveToDb() {
        if (projectId == null || projectId.trim().isEmpty()) {
            Toast.makeText(this, "חסר projectId לשמירה", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        for (ListItem li : items) {
            if (!(li instanceof FieldItem)) continue;
            FieldItem f = (FieldItem) li;

            switch (f.kind) {
                case MULTI:
                    updates.put(f.key, new ArrayList<>(f.multiValue));
                    break;
                case DECIMAL_M2:
                    String meters = (f.value == null) ? "" : f.value.trim();
                    if (!meters.isEmpty()) updates.put(f.key, meters + " מ\"ר");
                    break;
                default:
                    String val = (f.value == null) ? "" : f.value.trim();
                    if (!val.isEmpty()) updates.put(f.key, val);
            }
        }

        // <<< שינוי כאן: שמירה מרוכזת ל-property_details >>>
        ProjectRepository.getInstance().savePropertyDetails(projectId, updates, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                goToNextScreen();
                finish();
            } else {
                Exception e = task.getException();
                Toast.makeText(this, "שמירה נכשלה: " + (e != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show();
            }
        });
    }


    // ====== מודלי אייטמים פנימיים ======
    public static abstract class ListItem {}

    public static class SectionItem extends ListItem {
        public final String title;
        public SectionItem(String t){ this.title = t; }
    }

    public enum Kind { SINGLE, MULTI, SIGNED_INT, DECIMAL_M2, TEXT, PHYSICAL_WITH_YEARS, FLOORS_COMPOSITE }

    public static class FieldItem extends ListItem {
        public final String key;
        public final String title;
        public Kind kind;

        public String value = "";
        public List<String> multiValue = new ArrayList<>();
        public List<String> options;
        public boolean allowOther = false;
        public int min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;

        public static FieldItem single(String key, String title, String value, List<String> options, boolean allowOther) {
            FieldItem f = new FieldItem(key, title, Kind.SINGLE);
            f.value = value; f.options = options; f.allowOther = allowOther; return f;
        }
        public static FieldItem multi(String key, String title, List<String> value, List<String> options) {
            FieldItem f = new FieldItem(key, title, Kind.MULTI);
            if (value != null) f.multiValue = value; f.options = options; return f;
        }
        public static FieldItem signedInt(String key, String title, String value, int min, int max) {
            FieldItem f = new FieldItem(key, title, Kind.SIGNED_INT);
            f.value = value; f.min = min; f.max = max; return f;
        }
        public static FieldItem decimalMeters(String key, String title, String value) {
            FieldItem f = new FieldItem(key, title, Kind.DECIMAL_M2);
            f.value = value; return f;
        }
        public static FieldItem text(String key, String title, String value) {
            FieldItem f = new FieldItem(key, title, Kind.TEXT);
            f.value = value; return f;
        }
        public static FieldItem physicalCondition(String key, String title, String value, List<String> options) {
            FieldItem f = new FieldItem(key, title, Kind.PHYSICAL_WITH_YEARS);
            f.value = value; f.options = options; return f;
        }
        public static FieldItem floorsComposite(String key, String title, String value, List<String> refLevels, boolean allowOther) {
            FieldItem f = new FieldItem(key, title, Kind.FLOORS_COMPOSITE);
            f.value = value; f.options = refLevels; f.allowOther = allowOther; return f;
        }

        private FieldItem(String key, String title, Kind kind) {
            this.key = key; this.title = title; this.kind = kind;
        }
    }

    interface TextConsumer { void accept(String s); }
}

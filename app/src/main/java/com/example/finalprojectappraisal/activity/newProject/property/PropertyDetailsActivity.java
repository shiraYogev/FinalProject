package com.example.finalprojectappraisal.activity.newProject.property;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.adapter.PropertyDetailsAdapter;
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.activity.newProject.property.common.utils.Choices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Property Details with dialog-based editing, reusing existing item XMLs. */
public class PropertyDetailsActivity extends AppCompatActivity implements PropertyDetailsAdapter.FieldClickListener {

    private RecyclerView recycler;
    private String projectId;

    private final List<ListItem> items = new ArrayList<>();
    private PropertyDetailsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_details); // כבר יש לך את זה

        recycler = findViewById(R.id.recyclerDetails);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        projectId = getIntent() != null ? getIntent().getStringExtra("projectId") : null;

        // TODO: שליפה אמיתית של הפרויקט (כאן רק סטאבים ריקים/ברירת מחדל)
        Project p = new Project();

        buildList(p);
        adapter = new PropertyDetailsAdapter(items, this);
        recycler.setAdapter(adapter);

        findViewById(R.id.btnSaveAll).setOnClickListener(v -> saveToDb());
    }

    // ====== בניית האייטמים למסך (כותרות + שדות) ======
    private void buildList(Project p) {
        items.clear();

        items.add(new SectionItem("פרטים כלליים על הנכס/בניין"));
        items.add(FieldItem.single("property_location", "מיקום הנכס",
                or(p.getLocation()), Choices.PROPERTY_LOCATIONS, true));
        items.add(FieldItem.single("building_type", "סוג הבניין",
                or(p.getBuildingType()), Choices.BUILDING_TYPES, true));
        items.add(FieldItem.physicalCondition("physical_condition", "מצב הבניין",
                or(p.getBuildingCondition()), Choices.PHYSICAL_CONDITION_OPTIONS));
        items.add(FieldItem.floorsComposite("number_of_floors", "מספר קומות",
                or(p.getNumberOfFloors()), Choices.FLOORS_REFERENCE_LEVELS, true));

        items.add(FieldItem.text("building_entry", "כניסה/מבואה", ""));
        items.add(FieldItem.text("building_number", "מספר הבניין", ""));
        items.add(FieldItem.text("zone_number", "מספר אזור", ""));
        items.add(FieldItem.text("building_city_plan_number", "מספר תכנית עירונית (תב\"ע)", ""));

        items.add(new SectionItem("פרטי הדירה"));
        items.add(FieldItem.text("apartment_number(municipal_form)", "מס' דירה (טופס עירייה)",
                or(p.getApartmentNumber())));
        items.add(FieldItem.signedInt("apartment_story", "מספר הקומה",
                or(p.getFloorNumber()), -2, 60));
        items.add(FieldItem.single("number_of_rooms", "מספר חדרים",
                or(p.getNumberOfRooms()), Choices.ROOMS_OPTIONS, false));

        items.add(new SectionItem("מדדים (שטחים)"));
        items.add(FieldItem.decimalMeters("registered_apartment_area", "שטח רשום (מ\"ר)",
                or(p.getRegisteredArea())));
        items.add(FieldItem.decimalMeters("gross_apartment_area", "שטח ברוטו (מ\"ר)",
                or(p.getGrossArea())));

        items.add(new SectionItem("כיווני אוויר"));
        items.add(FieldItem.multi("apartment_directions", "כיווני אוויר",
                listOrNull(p.getAirDirection()), Choices.AIR_DIRECTIONS));
    }

    private String or(String s) { return s == null ? "" : s; }
    private List<String> listOrNull(List<String> l) { return l == null ? new ArrayList<>() : l; }

    // ====== פתיחת דיאלוגים לפי סוג ======
    @Override
    public void onFieldClicked(FieldItem f, int position) {
        switch (f.kind) {
            case SINGLE:
                openSingleChoice(f, position);
                break;
            case MULTI:
                openMultiChoice(f, position);
                break;
            case SIGNED_INT:
                openSignedInt(f, position);
                break;
            case DECIMAL_M2:
                openDecimalMeters(f, position);
                break;
            case TEXT:
                openText(f, position);
                break;
            case PHYSICAL_WITH_YEARS:
                openPhysicalCondition(f, position);
                break;
            case FLOORS_COMPOSITE:
                openFloorsComposite(f, position);
                break;
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
        f.value = newVal == null ? "" : newVal;
        adapter.notifyItemChanged(pos);
    }

    // ====== שמירה ל-DB ======
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
                    // נשמר כ-List<String> (למשל לכיווני אוויר)
                    updates.put(f.key, new ArrayList<>(f.multiValue));
                    break;

                case DECIMAL_M2:
                    // שדות במ"ר – מוסיפים סיומת בשמירה לפי המפרט שלך
                    String meters = (f.value == null) ? "" : f.value.trim();
                    if (!meters.isEmpty()) {
                        updates.put(f.key, meters + " מ\"ר");
                    }
                    break;

                default:
                    // שדות טקסט/מספר רגילים
                    String val = (f.value == null) ? "" : f.value.trim();
                    if (!val.isEmpty()) {
                        updates.put(f.key, val);
                    }
            }
        }

        // שמירה דרך ה-Repository עם ולידציה מובנית ל-Property Details
        ProjectRepository.getInstance().savePropertyDetails(projectId, updates, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                // finish(); // אם את רוצה לסגור את המסך אחרי שמירה
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

        public String value = "";                 // לשדות רגילים
        public List<String> multiValue = new ArrayList<>(); // לשדות רב-בחירה
        public List<String> options;              // לאופציות
        public boolean allowOther = false;        // "אחר"
        public int min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;

        // Factories
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

// file: app/src/main/java/com/example/finalprojectappraisal/activity/newProject/images/UploadImagesActivity.java
package com.example.finalprojectappraisal.activity.newProject.images;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.BuildConfig;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.HomePageActivity;
import com.example.finalprojectappraisal.utils.HomeButtonHelper;
import com.example.finalprojectappraisal.activity.newProject.ProgressStepperHelper;
import com.example.finalprojectappraisal.export.ProjectCompletionActivity;
import com.example.finalprojectappraisal.activity.newProject.images.viewmodel.UploadImagesViewModel;
import com.example.finalprojectappraisal.adapter.ImageCategoriesAdapter;
import com.example.finalprojectappraisal.adapter.MiniCardAdapter;
import com.example.finalprojectappraisal.classifer.ImageCategorySection;
import com.example.finalprojectappraisal.classifer.gemini.EnhancedGeminiHelper;
import com.example.finalprojectappraisal.classifer.gemini.GeminiPrompts;
import com.example.finalprojectappraisal.databinding.ActivityUploadImagesBinding;
import com.example.finalprojectappraisal.adapter.ImagePagerAdapter;
import com.example.finalprojectappraisal.model.Floor;
import com.example.finalprojectappraisal.model.FloorType;
import com.example.finalprojectappraisal.model.Image;
import com.example.finalprojectappraisal.model.Unit;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * שלב 2: העלאת תמונות נכס
 */
public class UploadImagesActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQUEST_CAMERA_PERMISSION = 201;

    private static final String TAG = "UploadImagesActivity";

    public static final String EXTRA_PROJECT_ID = "projectId";

    private static final String DEBUG_PROJECT_ID = "1d1W6owgTtkdnTb7Kb8H";

    private UploadImagesViewModel vm;
    private ActivityUploadImagesBinding binding;
    private ProgressStepperHelper progressHelper;

    private java.util.List<ImageCategorySection> categories;
    private ImageCategoriesAdapter categoriesAdapter;

    private ImageCategorySection pendingSection;
    private Unit pendingUnit;
    private Floor pendingFloor;
    private Uri pendingCameraUri;

    private int nextBedroomIndex = 2;

    // רשימת היחידות של הנכס (ברירת מחדל: יחידה ראשית עם קומה ראשית)
    private final List<Unit> units = new ArrayList<>();

    // Bottom sheet של פירוט קטגוריה לפי יחידה+קומה
    private BottomSheetDialog currentSheet;
    private ImageCategorySection currentSheetSection;
    private Unit currentSheetUnit;
    private Floor currentSheetFloor;
    private ImagePagerAdapter currentSheetPager;
    private List<Image> currentSheetImages;
    private TextView currentSheetDesc;

    private String projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadImagesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        HomeButtonHelper.setup(this);

        projectId = resolveProjectId();
        if (projectId == null) return;

        vm = new ViewModelProvider(this).get(UploadImagesViewModel.class);
        vm.init(projectId);

        setupCategories();
        setupRecyclerView();
        setupObservers();
        setupProgressStepper();
        setupListeners();
    }

    private String resolveProjectId() {
        Intent intent = getIntent();
        String fromIntent = intent.getStringExtra(EXTRA_PROJECT_ID);

        if (fromIntent != null && !fromIntent.isEmpty()) {
            Log.d(TAG, "Received projectId from Intent: " + fromIntent);
            return fromIntent;
        }

        if (BuildConfig.DEBUG) {
            Log.w(TAG, "No projectId in Intent → using DEBUG_PROJECT_ID=" + DEBUG_PROJECT_ID);
            return DEBUG_PROJECT_ID;
        }

        toast("לא נמצא מזהה פרויקט");
        finish();
        return null;
    }

    private void setupCategories() {
        categories = new java.util.ArrayList<>();

        // ============================================================
        // קבוצת חוץ (לפי המפרט)
        // ============================================================
        categories.add(new ImageCategorySection("חזית", Image.Category.EXTERIOR, GeminiPrompts.EXTERIOR_CLADDING_PROMPT));
        categories.add(new ImageCategorySection("סביבת הנכס / נוף", Image.Category.VIEW, "זהה את הנוף מהדירה..."));
        categories.add(new ImageCategorySection("גג בניין", Image.Category.ROOF, null));
        categories.add(new ImageCategorySection("גג משותף", Image.Category.SHARED_ROOF, null));
        categories.add(new ImageCategorySection("חדר מדרגות", Image.Category.STAIRWELL, null));
        categories.add(new ImageCategorySection("מעלית", Image.Category.ELEVATOR, GeminiPrompts.ELEVATOR_PROMPT));
        categories.add(new ImageCategorySection("לובי בניין", Image.Category.LOBBY, null));
        categories.add(new ImageCategorySection("כניסה לבניין", Image.Category.ENTRANCE, null));
        categories.add(new ImageCategorySection("חניה", Image.Category.PARKING, null));
        categories.add(new ImageCategorySection("מחסן", Image.Category.STORAGE, null));
        categories.add(new ImageCategorySection("חצר / גינה", Image.Category.YARD, null));
        categories.add(new ImageCategorySection("שביל / דרך גישה", Image.Category.ACCESS_PATH, null));
        categories.add(new ImageCategorySection("מתקנים חיצוניים", Image.Category.EXTERNAL_FACILITIES, null));

        // ============================================================
        // קבוצת פנים (לפי המפרט)
        // ============================================================
        categories.add(new ImageCategorySection("דלת כניסה", Image.Category.ENTRANCE_DOOR, GeminiPrompts.ENTRANCE_DOOR_PROMPT));
        categories.add(new ImageCategorySection("דלתות פנים ומשקופים", Image.Category.INTERIOR_DOORS, null));
        categories.add(new ImageCategorySection("פרוזדור", Image.Category.HALLWAY, GeminiPrompts.INTERIOR_DOORS_PROMPT));
        categories.add(new ImageCategorySection("ריצוף", Image.Category.FLOORING, null));
        categories.add(new ImageCategorySection("תקרה / הנמכת תקרה", Image.Category.CEILING, null));
        categories.add(new ImageCategorySection("מיזוג אוויר", Image.Category.AIR_CONDITIONING, null));
        categories.add(new ImageCategorySection("חיפוי פנים", Image.Category.INTERIOR_CLADDING, null));
        categories.add(new ImageCategorySection("ארונות", Image.Category.CABINETS, null));
        categories.add(new ImageCategorySection("מצב פנים וגמרים", Image.Category.INTERIOR_FINISH, null));
        categories.add(new ImageCategorySection("סלון", Image.Category.LIVING_ROOM, GeminiPrompts.LIVING_ROOM_PROMPT));
        categories.add(new ImageCategorySection("מטבח", Image.Category.KITCHEN, GeminiPrompts.KITCHEN_PROMPT));
        categories.add(new ImageCategorySection("פינת אוכל", Image.Category.DINING_AREA, null));
        categories.add(new ImageCategorySection("מזווה", Image.Category.PANTRY, null));

        // ✅ חדר שינה הורים (BEDROOM + MASTER)
        categories.add(new ImageCategorySection(
                "חדר שינה הורים",
                Image.Category.BEDROOM,
                GeminiPrompts.BEDROOM_PROMPT,
                Image.Subcategory.MASTER,
                0
        ));

        // ✅ חדר שינה 1 רגיל (BEDROOM + NONE + index=1)
        categories.add(new ImageCategorySection(
                "חדר שינה 1",
                Image.Category.BEDROOM,
                GeminiPrompts.BEDROOM_PROMPT,
                Image.Subcategory.NONE,
                1
        ));

        // ✅ שירותי הורים (BATHROOM + MASTER)
        categories.add(new ImageCategorySection(
                "שירותי הורים",
                Image.Category.BATHROOM,
                GeminiPrompts.BATHROOM_PROMPT,
                Image.Subcategory.MASTER,
                0
        ));

        // ✅ שירותי אורחים (BATHROOM + GUEST)
        categories.add(new ImageCategorySection(
                "שירותי אורחים",
                Image.Category.BATHROOM,
                GeminiPrompts.BATHROOM_PROMPT,
                Image.Subcategory.GUEST,
                0
        ));

        // ✅ חדר רחצה רגיל (BATHROOM + NONE)
        categories.add(new ImageCategorySection(
                "חדר רחצה",
                Image.Category.BATHROOM,
                GeminiPrompts.BATHROOM_PROMPT,
                Image.Subcategory.NONE,
                0
        ));

        categories.add(new ImageCategorySection("שירותים", Image.Category.TOILET, null));
        categories.add(new ImageCategorySection("מרפסת", Image.Category.BALCONY, null));
        categories.add(new ImageCategorySection("ממ\"ד", Image.Category.SAFE_ROOM, null));
        categories.add(new ImageCategorySection("ליקויים", Image.Category.DEFECTS, null));

        categories.add(new ImageCategorySection("אחר", Image.Category.OTHER, null));

        assignGroups();

        if (units.isEmpty()) units.add(Unit.main());
    }

    /** מסמן לכל section האם הוא שייך לקבוצת חוץ או פנים (לאקורדיון). */
    private void assignGroups() {
        java.util.EnumSet<Image.Category> exterior = java.util.EnumSet.of(
                Image.Category.EXTERIOR, Image.Category.VIEW, Image.Category.ROOF,
                Image.Category.SHARED_ROOF, Image.Category.STAIRWELL, Image.Category.ELEVATOR,
                Image.Category.LOBBY, Image.Category.ENTRANCE, Image.Category.PARKING,
                Image.Category.STORAGE, Image.Category.YARD, Image.Category.GARDEN,
                Image.Category.ACCESS_PATH, Image.Category.EXTERNAL_FACILITIES
        );
        for (ImageCategorySection s : categories) {
            s.group = exterior.contains(s.category)
                    ? ImageCategorySection.Group.EXTERIOR
                    : ImageCategorySection.Group.INTERIOR;
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerCategories = binding.recyclerCategories;
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));

        categoriesAdapter = new ImageCategoriesAdapter(
                categories,
                units,
                this,
                new ImageCategoriesAdapter.Listener() {
                    @Override
                    public void onMiniCardClick(ImageCategorySection section, Unit unit, Floor floor) {
                        showCategoryBottomSheet(section, unit, floor);
                    }

                    @Override
                    public void onAddFloor(Unit unit) {
                        showAddFloorDialog(unit);
                    }

                    @Override
                    public void onAddUnit() {
                        addUnit();
                    }

                    @Override
                    public void onRemoveUnit(Unit unit) {
                        removeUnit(unit);
                    }
                }
        );

        recyclerCategories.setAdapter(categoriesAdapter);
    }

    private void setupObservers() {
        vm.getExisting().observe(this, list -> {
            for (ImageCategorySection s : categories) {
                s.images.clear();
            }

            if (list == null || list.isEmpty()) {
                categoriesAdapter.reload();
                return;
            }

            // ✅ Ensure dynamic bedroom sections exist for previously saved bedroomIndex (2,3,4...)
            ensureDynamicBedroomSections(list);

            // ✅ Distribute each image to the correct section (not only by category)
            for (Image img : list) {
                if (img == null || img.getCategory() == null) continue;
                ImageCategorySection sec = findSectionForImage(img);
                if (sec != null) sec.images.add(img);
            }

            ensureUnitsAndFloorsFromImages(list);
            categoriesAdapter.reload();
        });

        vm.getLastSavedImage().observe(this, img -> {
            if (img == null || img.getCategory() == null) return;

            // ✅ if it's bedroom#2 and the section wasn't created for some reason, create it
            if (img.getCategory() == Image.Category.BEDROOM &&
                    img.getSubCategory() == Image.Subcategory.NONE &&
                    img.getBedroomIndex() >= 2 &&
                    !hasBedroomNumberedSection(img.getBedroomIndex())) {
                addBedroomSectionWithIndex(img.getBedroomIndex());
            }

            ImageCategorySection sec = findSectionForImage(img);
            if (sec == null) return;

            if (!categories.contains(sec)) return;

            Image target = null;

            for (int i = 0; i < sec.images.size(); i++) {
                Image it = sec.images.get(i);

                if (img.getId() != null && it.getId() != null && img.getId().equals(it.getId())) {
                    target = it;
                    break;
                }

                if (img.getLocalUri() != null && img.getLocalUri().equals(it.getLocalUri())) {
                    target = it;
                    break;
                }
            }

            if (target == null) {
                target = img;
                sec.images.add(target);
            } else {
                target.setId(img.getId());
                target.setUrl(img.getUrl());
                target.setProjectId(img.getProjectId());
                target.setDescription(img.getDescription());

                // ✅ keep identity fields in sync
                target.setSubCategory(img.getSubCategory());
                target.setBedroomIndex(img.getBedroomIndex());
            }

            refreshUi();

            if (sec.category == Image.Category.OTHER) {
                if (target.getDescription() == null || target.getDescription().trim().isEmpty()) {
                    target.setDescription("תמונה נוספת (ללא סיווג אוטומטי)");
                }
                refreshUi();
                toast("התמונה נשמרה (קטגוריה: אחר)");
                return;
            }

            classifyImage(target, sec);
        });

        vm.getLastDeleteOk().observe(this, ok -> {
            if (ok == null) return;
            if (!ok) toast("מחיקת תמונה נכשלה");
        });
    }

    private void setupProgressStepper() {
        progressHelper = new ProgressStepperHelper(
                this,
                ProgressStepperHelper.STEP_IMAGE_UPLOAD,
                binding.getRoot(),
                projectId
        );
        progressHelper.initialize();
    }

    private void setupListeners() {
        MaterialCardView backButtonCard = binding.backButtonCard;
        if (backButtonCard != null) {
            backButtonCard.setOnClickListener(v -> progressHelper.moveToPreviousStep());
        }

        binding.btnSaveAndContinue.setOnClickListener(v -> {
            progressHelper.moveToNextStep();
            finish();
        });
    }

    /**
     * 🏠 מעבר לדף הבית עם ייצוא אוטומטי של JSON
     */
    private void navigateToHomeWithExport() {
        String currentProjectId = projectId;
        if (currentProjectId != null) {
            Intent intent = new Intent(this, ProjectCompletionActivity.class);
            intent.putExtra(ProjectCompletionActivity.EXTRA_PROJECT_ID, currentProjectId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(this, HomePageActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void onImageClicked(@NonNull ImageCategorySection section,
                                @NonNull Image image,
                                int sectionIndex,
                                int imageIndex) {
        Log.d(TAG, "Image Clicked: " + image.getId() + " in section: " + section.title);
    }

    private void addNewBedroomSection() {
        int idx = nextBedroomIndex;
        addBedroomSectionWithIndex(idx);
        nextBedroomIndex++;
        toast("חדר שינה " + idx + " נוסף");
    }

    private void addBedroomSectionWithIndex(int idx) {
        String title = "חדר שינה " + idx;

        ImageCategorySection newSection = new ImageCategorySection(
                title,
                Image.Category.BEDROOM,
                GeminiPrompts.BEDROOM_PROMPT,
                Image.Subcategory.NONE,
                idx
        );

        int insertPos = findInsertPosAfterLastBedroom();
        categories.add(insertPos, newSection);

        if (categoriesAdapter != null) {
            categoriesAdapter.reload();
        }
    }

    private int findInsertPosAfterLastBedroom() {
        int last = -1;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).category == Image.Category.BEDROOM) last = i;
        }
        return (last == -1) ? 0 : (last + 1);
    }

    private boolean hasBedroomNumberedSection(int idx) {
        for (ImageCategorySection s : categories) {
            if (s.category == Image.Category.BEDROOM &&
                    s.subCategory == Image.Subcategory.NONE &&
                    s.bedroomIndex == idx) return true;
        }
        return false;
    }

    private void ensureDynamicBedroomSections(java.util.List<Image> list) {
        int maxIdx = 1;
        Set<Integer> needed = new HashSet<>();

        for (Image img : list) {
            if (img == null || img.getCategory() != Image.Category.BEDROOM) continue;
            if (img.getSubCategory() == Image.Subcategory.MASTER) continue;

            int idx = img.getBedroomIndex();
            if (idx <= 0) idx = 1;

            maxIdx = Math.max(maxIdx, idx);
            if (idx >= 2) needed.add(idx);
        }

        for (int idx : needed) {
            if (!hasBedroomNumberedSection(idx)) {
                // add without toast
                addBedroomSectionWithIndex(idx);
            }
        }

        nextBedroomIndex = Math.max(nextBedroomIndex, maxIdx + 1);
    }

    // ===========================
    // Gemini classification
    // ===========================

    private void classifyImage(Image img, ImageCategorySection sec) {
        if (sec.prompt == null) {
            img.setDescription("תמונה נוספה (" + sec.title + ")");
            refreshUi();
            return;
        }

        EnhancedGeminiHelper.classifyImageAndSave(
                this,
                Uri.parse(img.getLocalUri() != null ? img.getLocalUri() : img.getUrl()),
                projectId,
                img,
                sec.prompt,
                new EnhancedGeminiHelper.EnhancedClassificationCallback() {
                    @Override
                    public void onResult(String raw, Map<String, String> parsed) {
                        runOnUiThread(() -> {
                            String summary = buildClassificationSummary(img.getCategory(), parsed);
                            img.setDescription((summary == null || summary.trim().isEmpty()) ? "סיווג הושלם" : summary);
                            refreshUi();
                            showClassificationResult(img.getCategory(), parsed);
                        });
                    }

                    @Override
                    public void onError(String e) {
                        runOnUiThread(() -> {
                            img.setDescription("שגיאה בסיווג: " + e);
                            refreshUi();
                            toast("שגיאה בסיווג: " + e);
                        });
                    }

                    @Override
                    public void onSavedToDatabase() {
                        runOnUiThread(() -> toast("התמונה נשמרה והפרויקט עודכן!"));
                    }
                }
        );
    }

    private String buildClassificationSummary(Image.Category category, Map<String, String> parsedDisplayKv) {
        if (parsedDisplayKv == null || parsedDisplayKv.isEmpty()) return "";

        StringBuilder message = new StringBuilder();
        switch (category) {
            case KITCHEN:
                appendIf(message, "ארונות", parsedDisplayKv.get("ארונות"));
                appendIf(message, "משטח", parsedDisplayKv.get("משטח עבודה"));
                break;
            case ENTRANCE_DOOR:
                appendIf(message, "מספר דירה", parsedDisplayKv.get("מספר דירה"));
                appendIf(message, "דלת", parsedDisplayKv.get("סוג דלת"));
                break;
            case ELEVATOR:
                appendIf(message, "מעליות", parsedDisplayKv.get("מספר מעליות"));
                appendIf(message, "קומות", parsedDisplayKv.get("מספר קומות בבניין"));
                break;
            case HALLWAY:
                appendIf(message, "דלתות פנים", parsedDisplayKv.get("דלתות פנים"));
                break;
            case LIVING_ROOM:
            case BEDROOM:
                appendIf(message, "ריצוף", parsedDisplayKv.get("ריצוף"));
                appendIf(message, "מיזוג", parsedDisplayKv.get("מיזוג אוויר"));
                appendIf(message, "חלונות", parsedDisplayKv.get("חלונות"));
                appendIf(message, "סורגים", parsedDisplayKv.get("סורגים"));
                break;
            default:
                for (Map.Entry<String, String> e : parsedDisplayKv.entrySet()) {
                    if (message.length() > 0) message.append("\n");
                    message.append(e.getKey()).append(": ").append(e.getValue());
                }
        }
        return message.toString();
    }

    private void showClassificationResult(Image.Category category, Map<String, String> parsedDisplayKv) {
        String message = buildClassificationSummary(category, parsedDisplayKv);
        toast((message == null || message.trim().isEmpty()) ? "סיווג הושלם" : message);
    }

    // ===========================
    // Image picking
    // ===========================

    private void showImageSourceDialog() {
        CharSequence[] options = new CharSequence[]{"גלריה", "מצלמה"};
        new AlertDialog.Builder(this)
                .setTitle("בחירת מקור תמונה")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openGallery();
                    else openCameraWithPermissionCheck();
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void openCameraWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Appraisal_" + System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        pendingCameraUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );

        if (pendingCameraUri == null) {
            toast("שגיאה ביצירת URI לתמונה");
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
        cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            toast("לא נמצאה אפליקציית מצלמה במכשיר");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                toast("אי אפשר לפתוח מצלמה ללא הרשאה");
            }
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == REQUEST_IMAGE_PICK) {
            if (data == null || data.getData() == null) return;
            handleNewImageFromUri(data.getData());
        } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (pendingCameraUri == null) {
                toast("שגיאה בקבלת התמונה מהמצלמה");
                return;
            }
            handleNewImageFromUri(pendingCameraUri);
        }
    }

    private void handleNewImageFromUri(Uri imageUri) {
        final ImageCategorySection pickedSection = pendingSection;
        if (pickedSection == null) {
            toast("לא נבחרה קטגוריה");
            return;
        }

        final Floor pickedFloor = pendingFloor;
        final String floorKey = (pickedFloor != null) ? pickedFloor.key() : Floor.DEFAULT_KEY;
        final Unit pickedUnit = pendingUnit;
        final String unitId = (pickedUnit != null) ? pickedUnit.key() : Unit.DEFAULT_KEY;

        final Image temp = new Image();
        temp.setProjectId(projectId);
        temp.setCategory(pickedSection.category);
        temp.setLocalUri(imageUri.toString());
        temp.setUrl(imageUri.toString());

        // ✅ NEW: keep section identity already in the temp object (UI will stay consistent)
        temp.setSubCategory(pickedSection.subCategory);
        temp.setBedroomIndex(pickedSection.bedroomIndex);
        temp.setFloor(floorKey);
        temp.setUnitId(unitId);

        pickedSection.images.add(temp);
        refreshUi();

        // ✅ NEW: pass identity to VM so it persists in Firestore
        vm.uploadAndSave(
                imageUri,
                pickedSection.category,
                pickedSection.prompt,
                pickedSection.subCategory,
                pickedSection.bedroomIndex,
                floorKey,
                unitId
        );
    }

    // ===========================
    // Bottom Sheet (פירוט קטגוריה לפי קומה)
    // ===========================

    private void showCategoryBottomSheet(ImageCategorySection section, @Nullable Unit unit, @Nullable Floor floor) {
        currentSheetSection = section;
        currentSheetUnit = unit;
        currentSheetFloor = floor;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.item_image_category_section, null);
        dialog.setContentView(content);

        TextView title = content.findViewById(R.id.txtCategoryTitle);
        ViewPager2 pager = content.findViewById(R.id.viewPagerImages);
        currentSheetDesc = content.findViewById(R.id.txtImageDescription);
        Button btnAdd = content.findViewById(R.id.btnAddImage);
        Button btnAddBedroom = content.findViewById(R.id.btnAddBedroom);

        String suffix = "";
        if (unit != null && !unit.isMain()) suffix += " · " + unit.label();
        if (floor != null) suffix += " · " + floor.label();
        title.setText(section.title + suffix);

        currentSheetImages = new ArrayList<>(MiniCardAdapter.imagesFor(section, unit, floor));

        currentSheetPager = new ImagePagerAdapter(currentSheetImages, new ImagePagerAdapter.OnImageActionListener() {
            @Override
            public void onDelete(int position) {
                if (position < 0 || position >= currentSheetImages.size()) return;
                deleteImageFromSection(section, currentSheetImages.get(position));
            }

            @Override
            public void onDescriptionChanged(int position, String newText) {
                if (position >= 0 && position < currentSheetImages.size()) {
                    currentSheetImages.get(position).setDescription(newText);
                }
            }

            @Override
            public void onImageClick(int position) { }
        });
        pager.setAdapter(currentSheetPager);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int pos) { updateSheetDescription(pos); }
        });
        updateSheetDescription(0);

        btnAdd.setOnClickListener(v -> {
            pendingSection = section;
            pendingUnit = unit;
            pendingFloor = floor;
            showImageSourceDialog();
        });

        boolean isNumberedBedroom = section.category == Image.Category.BEDROOM
                && section.subCategory == Image.Subcategory.NONE
                && section.bedroomIndex >= 1;
        if (isNumberedBedroom) {
            btnAddBedroom.setVisibility(View.VISIBLE);
            btnAddBedroom.setOnClickListener(v -> {
                addNewBedroomSection();
                dialog.dismiss();
            });
        } else {
            btnAddBedroom.setVisibility(View.GONE);
        }

        currentSheet = dialog;
        dialog.setOnDismissListener(d -> {
            if (currentSheet == d) {
                currentSheet = null;
                currentSheetSection = null;
                currentSheetUnit = null;
                currentSheetFloor = null;
                currentSheetPager = null;
                currentSheetImages = null;
                currentSheetDesc = null;
            }
        });
        dialog.show();
    }

    private void updateSheetDescription(int pos) {
        if (currentSheetDesc == null || currentSheetImages == null) return;
        if (pos < 0 || pos >= currentSheetImages.size()) {
            currentSheetDesc.setText("");
            currentSheetDesc.setVisibility(View.GONE);
            return;
        }
        String d = currentSheetImages.get(pos).getDescription();
        if (d == null || d.trim().isEmpty()) {
            currentSheetDesc.setText("");
            currentSheetDesc.setVisibility(View.GONE);
        } else {
            currentSheetDesc.setVisibility(View.VISIBLE);
            currentSheetDesc.setText(d);
        }
    }

    /** מרענן את הרשימה הראשית + ה-Bottom Sheet הפתוח (אם יש). */
    private void refreshUi() {
        if (categoriesAdapter != null) categoriesAdapter.reload();
        refreshOpenSheet();
    }

    private void refreshOpenSheet() {
        if (currentSheet == null || !currentSheet.isShowing()) return;
        if (currentSheetSection == null || currentSheetImages == null || currentSheetPager == null) return;
        currentSheetImages.clear();
        currentSheetImages.addAll(MiniCardAdapter.imagesFor(currentSheetSection, currentSheetUnit, currentSheetFloor));
        currentSheetPager.notifyDataSetChanged();
        updateSheetDescription(0);
    }

    private void deleteImageFromSection(ImageCategorySection section, Image image) {
        section.images.remove(image);
        refreshUi();

        vm.getLastDeleteOk().removeObservers(this);
        vm.getLastDeleteOk().observe(this, ok -> {
            if (Boolean.FALSE.equals(ok)) {
                section.images.add(image);
                refreshUi();
            }
        });

        vm.deleteImage(image);
    }

    // ===========================
    // Units & Floors
    // ===========================

    private void showAddFloorDialog(Unit unit) {
        final FloorType[] types = {
                FloorType.BASEMENT, FloorType.GROUND, FloorType.FLOOR, FloorType.ATTIC, FloorType.ROOF
        };
        CharSequence[] labels = new CharSequence[types.length];
        for (int i = 0; i < types.length; i++) labels[i] = types[i].label;

        new AlertDialog.Builder(this)
                .setTitle("הוספת קומה · " + unit.label())
                .setItems(labels, (d, which) -> addFloorOfType(unit, types[which]))
                .show();
    }

    private void addFloorOfType(Unit unit, FloorType type) {
        int index = 0;
        if (type == FloorType.FLOOR) {
            int max = 0;
            for (Floor f : unit.floors) if (f.type == FloorType.FLOOR) max = Math.max(max, f.index);
            index = max + 1;
        }
        Floor nf = new Floor(type, index);
        for (Floor f : unit.floors) {
            if (f.key().equals(nf.key())) {
                toast("הקומה כבר קיימת");
                return;
            }
        }
        unit.floors.add(nf);
        categoriesAdapter.reload();
        toast(nf.label() + " נוספה");
    }

    /** מוסיף יחידה חדשה (דירה מחולקת). */
    private void addUnit() {
        int max = 0;
        for (Unit u : units) max = Math.max(max, u.index);
        Unit nu = Unit.numbered(max + 1);
        units.add(nu);
        categoriesAdapter.reload();
        toast(nu.label() + " נוספה");
    }

    /** מסיר יחידה (לא את הראשית). אם יש בה תמונות — מבקש אישור. */
    private void removeUnit(Unit unit) {
        if (unit == null || unit.isMain()) return;
        int count = 0;
        for (ImageCategorySection s : categories) {
            for (Image img : s.images) {
                if (img != null && unit.key().equals(img.getUnitId())) count++;
            }
        }
        if (count > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("הסרת " + unit.label())
                    .setMessage("ביחידה זו יש " + count + " תמונות. להסיר בכל זאת? התמונות יימחקו.")
                    .setPositiveButton("הסר", (d, w) -> doRemoveUnit(unit))
                    .setNegativeButton("ביטול", null)
                    .show();
        } else {
            doRemoveUnit(unit);
        }
    }

    private void doRemoveUnit(Unit unit) {
        for (ImageCategorySection s : categories) {
            java.util.Iterator<Image> it = s.images.iterator();
            while (it.hasNext()) {
                Image img = it.next();
                if (img != null && unit.key().equals(img.getUnitId())) {
                    it.remove();
                    vm.deleteImage(img);
                }
            }
        }
        units.remove(unit);
        categoriesAdapter.reload();
        toast(unit.label() + " הוסרה");
    }

    /** מוודא שכל יחידה/קומה שמופיעה בתמונות קיימת. */
    private void ensureUnitsAndFloorsFromImages(java.util.List<Image> list) {
        if (list == null) return;
        for (Image img : list) {
            if (img == null) continue;
            String unitKey = img.getUnitId();
            if (unitKey == null || unitKey.trim().isEmpty()) unitKey = Unit.DEFAULT_KEY;
            Unit unit = findOrAddUnit(unitKey);

            String floorKey = img.getFloor();
            if (floorKey == null || floorKey.trim().isEmpty()) continue;
            boolean exists = false;
            for (Floor f : unit.floors) {
                if (f.key().equals(floorKey)) { exists = true; break; }
            }
            if (!exists) unit.floors.add(Floor.fromKey(floorKey));
        }
    }

    private Unit findOrAddUnit(String key) {
        for (Unit u : units) if (u.key().equals(key)) return u;
        Unit u = Unit.fromKey(key);
        units.add(u);
        return u;
    }

    // ===========================
    // Delete
    // ===========================

    private void onDeleteImageClicked(
            ImageCategorySection section,
            Image image,
            int sectionIndex,
            int imageIndex
    ) {
        Image removed = section.images.remove(imageIndex);
        refreshUi();

        vm.getLastDeleteOk().removeObservers(this);
        vm.getLastDeleteOk().observe(this, ok -> {
            if (Boolean.FALSE.equals(ok)) {
                section.images.add(imageIndex, removed);
                refreshUi();
            }
        });

        vm.deleteImage(image);
    }

    // ===========================
    // Helpers: section mapping
    // ===========================

    private ImageCategorySection findSectionForImage(@NonNull Image img) {
        Image.Category c = img.getCategory();
        if (c == null) return null;

        if (c == Image.Category.BEDROOM) {
            Image.Subcategory sc = img.getSubCategory();
            if (sc == Image.Subcategory.MASTER) {
                return findSectionExact(Image.Category.BEDROOM, Image.Subcategory.MASTER, 0);
            }
            int idx = img.getBedroomIndex();
            if (idx <= 0) idx = 1;
            ImageCategorySection byIdx = findSectionExact(Image.Category.BEDROOM, Image.Subcategory.NONE, idx);
            if (byIdx != null) return byIdx;
            return findSectionExact(Image.Category.BEDROOM, Image.Subcategory.NONE, 1);
        }

        if (c == Image.Category.BATHROOM) {
            Image.Subcategory sc = img.getSubCategory();
            if (sc == Image.Subcategory.MASTER) return findSectionExact(Image.Category.BATHROOM, Image.Subcategory.MASTER, 0);
            if (sc == Image.Subcategory.GUEST)  return findSectionExact(Image.Category.BATHROOM, Image.Subcategory.GUEST, 0);
            return findSectionExact(Image.Category.BATHROOM, Image.Subcategory.NONE, 0);
        }

        // Unique categories
        for (ImageCategorySection s : categories) {
            if (s.category == c) return s;
        }
        return null;
    }

    private ImageCategorySection findSectionExact(Image.Category cat, Image.Subcategory sc, int bedroomIndex) {
        for (ImageCategorySection s : categories) {
            if (s.category != cat) continue;
            if (s.subCategory != sc) continue;
            if (cat == Image.Category.BEDROOM && s.bedroomIndex != bedroomIndex) continue;
            return s;
        }
        return null;
    }

    private void appendIf(StringBuilder sb, String key, @Nullable String val) {
        if (val == null || val.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append("\n");
        sb.append(key).append(": ").append(val);
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}

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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.ProgressStepperHelper;
import com.example.finalprojectappraisal.activity.newProject.images.viewmodel.UploadImagesViewModel;
import com.example.finalprojectappraisal.activity.newProject.property.activity.ApartmentDetailsActivity;
import com.example.finalprojectappraisal.adapter.ImageCategoriesAdapter;
import com.example.finalprojectappraisal.classifer.ImageCategorySection;
import com.example.finalprojectappraisal.classifer.gemini.EnhancedGeminiHelper;
import com.example.finalprojectappraisal.classifer.gemini.GeminiPrompts;
import com.example.finalprojectappraisal.databinding.ActivityUploadImagesBinding;
import com.example.finalprojectappraisal.model.Image;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UploadImagesActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQUEST_CAMERA_PERMISSION = 201;

    private UploadImagesViewModel vm;

    private List<ImageCategorySection> categories;
    private ImageCategoriesAdapter categoriesAdapter;
    private ImageCategorySection pendingSection; // section chosen before picker/camera
    private Uri pendingCameraUri;                // Uri where camera will save image
    private String projectId;

    private ActivityUploadImagesBinding binding;
    private ProgressStepperHelper progressHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding =ActivityUploadImagesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null) {
            toast("לא נמצא מזהה פרויקט");
            finish();
            return;
        }

        vm = new ViewModelProvider(this).get(UploadImagesViewModel.class);
        vm.init(projectId);

        // Categories, including "Other" without Gemini prompt
        categories = Arrays.asList(
                new ImageCategorySection("דלת כניסה", Image.Category.ENTRANCE_DOOR, GeminiPrompts.ENTRANCE_DOOR_PROMPT),
                new ImageCategorySection("מטבח", Image.Category.KITCHEN, GeminiPrompts.KITCHEN_PROMPT),
                new ImageCategorySection("סלון", Image.Category.LIVING_ROOM, GeminiPrompts.LIVING_ROOM_PROMPT),
                new ImageCategorySection("חזית", Image.Category.EXTERIOR, "זהה מצב חזית הבית..."),
                new ImageCategorySection("חדר רחצה", Image.Category.BATHROOM, GeminiPrompts.BATHROOM_PROMPT),
                new ImageCategorySection("חדר שינה", Image.Category.BEDROOM, GeminiPrompts.BEDROOM_PROMPT),
                new ImageCategorySection("נוף", Image.Category.VIEW, "זהה את הנוף מהדירה..."),
                new ImageCategorySection("אחר", Image.Category.OTHER, null) // Free images, no auto classification
        );

        // --- תיקון: גישה ל-RecyclerView דרך Binding ---
        RecyclerView recyclerCategories = binding.recyclerCategories;
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        categoriesAdapter = new ImageCategoriesAdapter(
                categories,
                this,
                section -> {
                    // open source chooser instead of gallery directly
                    pendingSection = section;
                    showImageSourceDialog();
                },
                this::onDeleteImageClicked
        );
        recyclerCategories.setAdapter(categoriesAdapter);

        // Load existing images and place them in sections
        vm.getExisting().observe(this, list -> {
            // Clear old images to avoid duplicates on reload
            for (ImageCategorySection s : categories) {
                s.images.clear();
            }

            if (list == null || list.isEmpty()) {
                categoriesAdapter.notifyDataSetChanged();
                return;
            }

            java.util.Map<Image.Category, ImageCategorySection> byCat = new java.util.HashMap<>();
            for (ImageCategorySection s : categories) byCat.put(s.category, s);
            for (Image img : list) {
                if (img == null || img.getCategory() == null) continue;
                ImageCategorySection sec = byCat.get(img.getCategory());
                if (sec != null) sec.images.add(img);
            }
            categoriesAdapter.notifyDataSetChanged();
        });

        // When VM finishes upload+save, update UI and (if needed) trigger classification
        vm.getLastSavedImage().observe(this, img -> {
            if (img == null) return;

            ImageCategorySection sec = findSection(img.getCategory());
            if (sec == null) return;

            // Replace temp (content://) with final https if exists
            for (int i = 0; i < sec.images.size(); i++) {
                Image it = sec.images.get(i);
                if (img.getId().equals(it.getId())) {
                    sec.images.set(i, img);
                    categoriesAdapter.notifyImageChanged(categories.indexOf(sec));
                    break;
                }
            }

            // "Other" category – no Gemini, only store simple description
            if (sec.category == Image.Category.OTHER) {
                img.setDescription("תמונה נוספת (ללא סיווג אוטומטי)");
                categoriesAdapter.notifyImageChanged(categories.indexOf(sec));
                toast("התמונה נשמרה (קטגוריה: אחר)");
                return;
            }

            // Trigger classification (updates image document internally)
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
                                // Build human-readable summary and put it into description
                                String summary = buildClassificationSummary(img.getCategory(), parsed);
                                if (summary == null || summary.trim().isEmpty()) {
                                    img.setDescription("סיווג הושלם");
                                } else {
                                    img.setDescription(summary);
                                }
                                categoriesAdapter.notifyImageChanged(categories.indexOf(sec));
                                // Optional toast with the same summary
                                showClassificationResult(img.getCategory(), parsed);
                            });
                        }

                        @Override
                        public void onError(String e) {
                            runOnUiThread(() -> {
                                img.setDescription("שגיאה בסיווג: " + e);
                                categoriesAdapter.notifyImageChanged(categories.indexOf(sec));
                                toast("שגיאה בסיווג: " + e);
                            });
                        }

                        @Override
                        public void onSavedToDatabase() {
                            runOnUiThread(() ->
                                    toast("התמונה נשמרה והפרויקט עודכן!")
                            );
                        }
                    }
            );
        });

        vm.getLastDeleteOk().observe(this, ok -> {
            if (ok == null) return;
            if (!ok) toast("מחיקת תמונה נכשלה או ניקוי מערך נכשל");
        });

        // --- הושאר ה-Listener הנכון והמלא (עם בדיקות) ---
        Button btnSaveAndContinue = binding.btnSaveAndContinue;
        btnSaveAndContinue.setOnClickListener(v -> {
            boolean hasImages = false;
            for (ImageCategorySection s : categories) {
                if (!s.images.isEmpty()) {
                    hasImages = true;
                    break;
                }
            }
            if (!hasImages) {
                toast("יש להעלות לפחות תמונה אחת");
                return;
            }
            try {
                // ניווט לשלב הבא והעברת projectId
                Intent intent = new Intent(this, ApartmentDetailsActivity.class);
                intent.putExtra("projectId", projectId);
                startActivity(intent);
                finish();

                // הערה: progressHelper.moveToNextStep() לא שימושי כאן כי צריך להעביר projectId
                // וצריך לקרוא ל-finish()

            } catch (Exception e) {
                toast("שגיאה במעבר: " + e.getMessage());
                Log.e("UploadImagesActivity", "Navigation error", e);
            }
        });
        // -----------------------------------------------------------------

        setupProgressStepper();
        setupListeners();
    }

    // =======================================================
    // --- הועבר לתוך הקלאס לצורך קומפילציה תקינה ---
    // =======================================================

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
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressHelper.moveToPreviousStep(); // → חזרה לשלב 1
            }
        });

    }

    // ============================
    // Build classification summary
    // ============================
    private String buildClassificationSummary(Image.Category category,
                                              Map<String, String> parsedDisplayKv) {
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
            case LIVING_ROOM:
            case BEDROOM:
                appendIf(message, "ריצוף", parsedDisplayKv.get("ריצוף"));
                appendIf(message, "מיזוג", parsedDisplayKv.get("מיזוג אוויר"));
                appendIf(message, "חלונות", parsedDisplayKv.get("חלונות"));
                appendIf(message, "סורגים", parsedDisplayKv.get("סורגים"));
                appendIf(message, "מידת ריצוף", parsedDisplayKv.get("מידת ריצוף"));
                appendIf(message, "דלתות פנים", parsedDisplayKv.get("דלתות פנים"));
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
        if (message == null || message.trim().isEmpty()) {
            toast("סיווג הושלם");
        } else {
            toast(message);
        }
    }

    // ==================================
    // בחירה בין מצלמה לגלריה (MediaStore)
    // ==================================

    private void showImageSourceDialog() {
        CharSequence[] options = new CharSequence[]{"גלריה", "מצלמה"};
        new AlertDialog.Builder(this)
                .setTitle("בחירת מקור תמונה")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openGallery();
                    } else {
                        openCameraWithPermissionCheck();
                    }
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

    /**
     * Opens camera using MediaStore Uri (no FileProvider).
     */
    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Appraisal_" + System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        pendingCameraUri = getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

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
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                toast("אי אפשר לפתוח מצלמה ללא הרשאה");
            }
        }
    }

    // =======================
    // onActivityResult
    // =======================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == REQUEST_IMAGE_PICK) {
            if (data == null || data.getData() == null) return;
            Uri imageUri = data.getData();
            handleNewImageFromUri(imageUri);
        } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (pendingCameraUri == null) {
                toast("שגיאה בקבלת התמונה מהמצלמה");
                return;
            }
            handleNewImageFromUri(pendingCameraUri);
        }
    }

    /**
     * Shared logic for new image (from gallery or camera).
     */
    private void handleNewImageFromUri(Uri imageUri) {
        final ImageCategorySection pickedSection = pendingSection;
        if (pickedSection == null) {
            toast("לא נבחרה קטגוריה");
            return;
        }

        // Temporary image for UI
        final Image temp = new Image();
        temp.setProjectId(projectId);
        temp.setCategory(pickedSection.category);
        temp.setLocalUri(imageUri.toString());
        temp.setUrl(imageUri.toString()); // Glide can handle content://

        pickedSection.images.add(temp);
        int sectionIndex = categories.indexOf(pickedSection);
        if (sectionIndex != -1) categoriesAdapter.notifyImageChanged(sectionIndex);

        // Upload + save via ViewModel
        vm.uploadAndSave(imageUri, pickedSection.category, pickedSection.prompt);
    }

    // =======================
    // Delete
    // =======================

    private void onDeleteImageClicked(ImageCategorySection section, Image image, int sectionIndex, int imageIndex) {
        // Optimistic UI removal
        Image removed = section.images.remove(imageIndex);
        categoriesAdapter.notifyImageChanged(sectionIndex);

        vm.getLastDeleteOk().removeObservers(this);
        vm.getLastDeleteOk().observe(this, ok -> {
            if (Boolean.FALSE.equals(ok)) {
                section.images.add(imageIndex, removed);
                categoriesAdapter.notifyImageChanged(sectionIndex);
            }
        });

        vm.deleteImage(image);
    }

    private ImageCategorySection findSection(Image.Category c) {
        for (ImageCategorySection s : categories) if (s.category == c) return s;
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

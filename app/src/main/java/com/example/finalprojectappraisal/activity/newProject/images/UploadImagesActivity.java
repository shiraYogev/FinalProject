/**
 * Summary:
 * UI-only Activity for uploading and managing project images.
 * Delegates storage/DB work to UploadImagesViewModel. Handles:
 * - Recycler sections & immediate UI rendering
 * - Image picker intent
 * - Post-save classification via EnhancedGeminiHelper
 */

package com.example.finalprojectappraisal.activity.newProject.images;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.images.viewmodel.UploadImagesViewModel;
import com.example.finalprojectappraisal.activity.newProject.property.activity.ApartmentDetailsActivity;
import com.example.finalprojectappraisal.adapter.ImageCategoriesAdapter;
import com.example.finalprojectappraisal.classifer.ImageCategorySection;
import com.example.finalprojectappraisal.classifer.gemini.EnhancedGeminiHelper;
import com.example.finalprojectappraisal.classifer.gemini.GeminiPrompts;
import com.example.finalprojectappraisal.model.Image;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UploadImagesActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 101;

    private UploadImagesViewModel vm;

    private List<ImageCategorySection> categories;
    private ImageCategoriesAdapter categoriesAdapter;
    private ImageCategorySection pendingSection; // section chosen before picker
    private String projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_images);

        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null) {
            toast("לא נמצא מזהה פרויקט");
            finish();
            return;
        }

        vm = new ViewModelProvider(this).get(UploadImagesViewModel.class);
        vm.init(projectId);

        categories = Arrays.asList(
                new ImageCategorySection("דלת כניסה", Image.Category.ENTRANCE_DOOR, GeminiPrompts.ENTRANCE_DOOR_PROMPT),
                new ImageCategorySection("מטבח", Image.Category.KITCHEN, GeminiPrompts.KITCHEN_PROMPT),
                new ImageCategorySection("סלון", Image.Category.LIVING_ROOM, GeminiPrompts.LIVING_ROOM_PROMPT),
                new ImageCategorySection("חזית", Image.Category.EXTERIOR, "זהה מצב חזית הבית..."),
                new ImageCategorySection("חדר רחצה", Image.Category.BATHROOM, GeminiPrompts.BATHROOM_PROMPT),
                new ImageCategorySection("חדר שינה", Image.Category.BEDROOM, GeminiPrompts.BEDROOM_PROMPT),
                new ImageCategorySection("נוף", Image.Category.VIEW, "זהה את הנוף מהדירה...")
        );

        RecyclerView recyclerCategories = findViewById(R.id.recyclerCategories);
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        categoriesAdapter = new ImageCategoriesAdapter(
                categories,
                this,
                section -> {
                    pendingSection = section;
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, REQUEST_IMAGE_PICK);
                },
                this::onDeleteImageClicked
        );
        recyclerCategories.setAdapter(categoriesAdapter);

        // Observe existing images loaded by VM and inject them into sections
        vm.getExisting().observe(this, list -> {
            if (list == null || list.isEmpty()) return;
            java.util.Map<Image.Category, ImageCategorySection> byCat = new java.util.HashMap<>();
            for (ImageCategorySection s : categories) byCat.put(s.category, s);
            for (Image img : list) {
                if (img == null || img.getCategory() == null) continue;
                ImageCategorySection sec = byCat.get(img.getCategory());
                if (sec != null) sec.images.add(img);
            }
            categoriesAdapter.notifyDataSetChanged();
        });

        // When the VM finishes upload+save, we update UI and trigger classification
        vm.getLastSavedImage().observe(this, img -> {
            if (img == null) return;
            // Find the section and update its UI
            ImageCategorySection sec = findSection(img.getCategory());
            if (sec != null) {
                // Replace temp (content://) with final https if exists
                for (int i = 0; i < sec.images.size(); i++) {
                    Image it = sec.images.get(i);
                    if (img.getId().equals(it.getId())) {
                        sec.images.set(i, img);
                        categoriesAdapter.notifyImageChanged(categories.indexOf(sec));
                        break;
                    }
                }
                // Trigger classification (updates the image document internally)
                EnhancedGeminiHelper.classifyImageAndSave(
                        this,
                        Uri.parse(img.getLocalUri() != null ? img.getLocalUri() : img.getUrl()),
                        projectId,
                        img,
                        sec.prompt,
                        new EnhancedGeminiHelper.EnhancedClassificationCallback() {
                            @Override public void onResult(String raw, Map<String, String> parsed) {
                                runOnUiThread(() -> {
                                    img.setDescription("סיווג הושלם");
                                    categoriesAdapter.notifyImageChanged(categories.indexOf(sec));
                                    showClassificationResult(img.getCategory(), parsed);
                                });
                            }
                            @Override public void onError(String e) {
                                runOnUiThread(() -> {
                                    img.setDescription("שגיאה בסיווג: " + e);
                                    categoriesAdapter.notifyImageChanged(categories.indexOf(sec));
                                    toast("שגיאה בסיווג: " + e);
                                });
                            }
                            @Override public void onSavedToDatabase() {
                                runOnUiThread(() ->
                                        toast("התמונה נשמרה והפרויקט עודכן!")
                                );
                            }
                        }
                );
            }
        });

        vm.getLastDeleteOk().observe(this, ok -> {
            if (ok == null) return;
            if (!ok) toast("מחיקת תמונה נכשלה או ניקוי מערך נכשל");
        });

        Button btnSaveAndContinue = findViewById(R.id.btnSaveAndContinue);
        btnSaveAndContinue.setOnClickListener(v -> {
            boolean hasImages = false;
            for (ImageCategorySection s : categories) {
                if (!s.images.isEmpty()) { hasImages = true; break; }
            }
            if (!hasImages) {
                toast("יש להעלות לפחות תמונה אחת");
                return;
            }
            try {
                Intent intent = new Intent(this, ApartmentDetailsActivity.class);
                intent.putExtra("projectId", projectId);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                toast("שגיאה במעבר: " + e.getMessage());
                Log.e("UploadImagesActivity", "Navigation error", e);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMAGE_PICK || resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;

        Uri imageUri = data.getData();
        final ImageCategorySection pickedSection = pendingSection;
        if (pickedSection == null) {
            toast("לא נבחרה קטגוריה");
            return;
        }

        // Create a temporary Image (for immediate UI) with content://
        final Image temp = new Image();
        temp.setProjectId(projectId);
        temp.setCategory(pickedSection.category);
        temp.setLocalUri(imageUri.toString());
        temp.setUrl(imageUri.toString()); // Glide can render content://

        pickedSection.images.add(temp);
        int sectionIndex = categories.indexOf(pickedSection);
        if (sectionIndex != -1) categoriesAdapter.notifyImageChanged(sectionIndex);

        // Delegate upload+save to VM; it will emit final Image via LiveData
        vm.uploadAndSave(imageUri, pickedSection.category, pickedSection.prompt);
    }

    private void onDeleteImageClicked(ImageCategorySection section, Image image, int sectionIndex, int imageIndex) {
        // Optimistic UI removal
        Image removed = section.images.remove(imageIndex);
        categoriesAdapter.notifyImageChanged(sectionIndex);

        vm.getLastDeleteOk().removeObservers(this);
        vm.getLastDeleteOk().observe(this, ok -> {
            // If failed → rollback
            if (Boolean.FALSE.equals(ok)) {
                section.images.add(imageIndex, removed);
                categoriesAdapter.notifyImageChanged(sectionIndex);
            }
        });

        vm.deleteImage(image);
    }

    private @Nullable ImageCategorySection findSection(Image.Category c) {
        for (ImageCategorySection s : categories) if (s.category == c) return s;
        return null;
    }

    private void showClassificationResult(Image.Category category, Map<String, String> parsedDisplayKv) {
        if (parsedDisplayKv == null || parsedDisplayKv.isEmpty()) {
            toast("סיווג הושלם");
            return;
        }
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
                break;
            default:
                for (Map.Entry<String, String> e : parsedDisplayKv.entrySet()) {
                    if (message.length() > 0) message.append("\n");
                    message.append(e.getKey()).append(": ").append(e.getValue());
                }
        }
        if (message.length() > 0) toast(message.toString());
    }

    private void appendIf(StringBuilder sb, String key, @Nullable String val) {
        if (val == null || val.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append("\n");
        sb.append(key).append(": ").append(val);
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}

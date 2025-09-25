// file: app/src/main/java/com/example/finalprojectappraisal/activity/newProject/UploadImagesActivity.java
package com.example.finalprojectappraisal.activity.newProject.images;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.property.activity.ApartmentDetailsActivity;
import com.example.finalprojectappraisal.adapter.ImageCategoriesAdapter;
import com.example.finalprojectappraisal.classifer.ImageCategorySection;
import com.example.finalprojectappraisal.classifer.gemini.EnhancedGeminiHelper;
import com.example.finalprojectappraisal.classifer.gemini.GeminiPrompts;
import com.example.finalprojectappraisal.model.Image;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.database.constants.FirestoreConstants;

public class UploadImagesActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 101;

    private List<ImageCategorySection> categories;
    private ImageCategoriesAdapter categoriesAdapter;
    private ImageCategorySection pendingSection; // נשמרת בזמן לחיצה על כפתור "הוסף תמונה" בסקשן
    private String projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_images);

        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null) {
            Toast.makeText(this, "לא נמצא מזהה פרויקט", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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
                    pendingSection = section; // לזכור איזה סקשן מבקש תמונה
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, REQUEST_IMAGE_PICK);
                },
                // ⬅️ קולבק למחיקת תמונה מסקשן
                (section, image, sectionIndex, imageIndex) -> {
                    onDeleteImageClicked(section, image, sectionIndex, imageIndex);
                }
        );
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategories.setAdapter(categoriesAdapter);

        loadExistingImages(projectId);

        Button btnSaveAndContinue = findViewById(R.id.btnSaveAndContinue);
        btnSaveAndContinue.setOnClickListener(v -> {
            boolean hasImages = false;
            for (ImageCategorySection s : categories) {
                if (!s.images.isEmpty()) { hasImages = true; break; }
            }
            if (!hasImages) {
                Toast.makeText(this, "יש להעלות לפחות תמונה אחת", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Intent intent = new Intent(UploadImagesActivity.this, ApartmentDetailsActivity.class);
                intent.putExtra("projectId", projectId);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "שגיאה במעבר: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("UploadImagesActivity", "שגיאה במעבר אקטיביטי", e);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            // ננעל את הסקשן שנבחר ברגע זה (כדי לא להתבלבל אם המשתמש עובר סקשן מהר)
            final ImageCategorySection pickedSection = pendingSection;
            if (pickedSection == null) {
                Toast.makeText(this, "לא נבחרה קטגוריה", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1) יצירת Image ועדכון UI מיידי (טוענים מה- content:// כפליס-הולדר עד שיגיע downloadUrl)
            final Image img = new Image();
            img.setUrl(imageUri.toString());       // תצוגה מיידית עם Glide (content://)
            img.setLocalUri(imageUri.toString());  // נשמור גם בשדה יעודי
            img.setProjectId(projectId);
            img.setCategory(pickedSection.category);
            img.setDescription("מעלה לענן ומסווג...");

            pickedSection.images.add(img);
            int sectionIndex = categories.indexOf(pickedSection);
            if (sectionIndex != -1) {
                categoriesAdapter.notifyImageChanged(sectionIndex);
            }

            // 2) העלאה ל-Storage → קבלת downloadUrl → שמירה ב-DB → ואז סיווג
            ProjectRepository.getInstance().uploadImageToStorage(projectId, imageUri, img.getId(), task1 -> {
                if (!task1.isSuccessful() || task1.getResult() == null) {
                    runOnUiThread(() -> {
                        img.setDescription("שגיאה בהעלאה: " + (task1.getException() != null ? task1.getException().getMessage() : ""));
                        int idx = categories.indexOf(pickedSection);
                        if (idx != -1) categoriesAdapter.notifyImageChanged(idx);
                        Toast.makeText(UploadImagesActivity.this, "שגיאה בהעלאת תמונה ל-Storage", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                String downloadUrl = task1.getResult();
                img.setUrl(downloadUrl); // מעכשיו נציג תמיד https
                img.setStoragePath(ProjectRepository.getInstance().buildImageStoragePath(projectId, img.getId()));
                img.setDescription("שומר למסד...");

                // שמירה ראשונית למסד כדי שתהיה רשומה גם לפני הסיווג
                ProjectRepository.getInstance().addImageToProject(projectId, img, task2 -> {
                    runOnUiThread(() -> {
                        int idx = categories.indexOf(pickedSection);
                        if (idx != -1) categoriesAdapter.notifyImageChanged(idx);

                        if (!task2.isSuccessful()) {
                            Toast.makeText(UploadImagesActivity.this, "שגיאה בשמירה ל-DB", Toast.LENGTH_LONG).show();
                        } else {
                            // עדכון מערך תמונות בפרויקט (images/main) עם ה-downloadUrl
                            upsertPropertyImagesArray(pickedSection.category, img.getUrl());
                        }
                    });

                    // 3) סיווג → יעדכן את המסמך של התמונה (אותו id) עם התוצאות
                    EnhancedGeminiHelper.classifyImageAndSave(
                            UploadImagesActivity.this,
                            imageUri,
                            projectId,
                            img,
                            pickedSection.prompt,
                            new EnhancedGeminiHelper.EnhancedClassificationCallback() {

                                @Override
                                public void onResult(String rawResult, Map<String, String> parsedDisplayKv) {
                                    runOnUiThread(() -> {
                                        img.setDescription("סיווג הושלם");
                                        int idx = categories.indexOf(pickedSection);
                                        if (idx != -1) {
                                            categoriesAdapter.notifyImageChanged(idx);
                                        }
                                        showClassificationResult(pickedSection.category, parsedDisplayKv);
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() -> {
                                        img.setDescription("שגיאה בסיווג: " + error);
                                        int idx = categories.indexOf(pickedSection);
                                        if (idx != -1) {
                                            categoriesAdapter.notifyImageChanged(idx);
                                        }
                                        Toast.makeText(UploadImagesActivity.this, "שגיאה בסיווג: " + error, Toast.LENGTH_LONG).show();
                                    });
                                }

                                @Override
                                public void onSavedToDatabase() {
                                    runOnUiThread(() -> {
                                        Toast.makeText(UploadImagesActivity.this, "התמונה נשמרה והפרויקט עודכן!", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                    );
                });
            });
        }
    }

    private void onDeleteImageClicked(ImageCategorySection section, Image image, int sectionIndex, int imageIndex) {
        // 1) הסרה אופטימיסטית מה-UI
        Image removed = section.images.remove(imageIndex);
        categoriesAdapter.notifyImageChanged(sectionIndex);

        // 2) מחיקה מה-DB לפי imageId
        String imageId = image.getId();
        if (imageId == null || imageId.trim().isEmpty()) {
            // אין id → נחזיר לתצוגה ונדווח. ודאי ש-EnhancedGeminiHelper מגדיר image.setId(...)
            section.images.add(imageIndex, removed);
            categoriesAdapter.notifyImageChanged(sectionIndex);
            Toast.makeText(this, "אי אפשר למחוק: חסר imageId", Toast.LENGTH_SHORT).show();
            Log.e("UploadImagesActivity", "Missing imageId on delete for url=" + image.getUrl());
            return;
        }

        ProjectRepository.getInstance().deleteImageFromProject(
                projectId,
                imageId,
                task -> {
                    if (!task.isSuccessful()) {
                        // 3) כשל ב-DB → Rollback ל-UI
                        section.images.add(imageIndex, removed);
                        categoriesAdapter.notifyImageChanged(sectionIndex);
                        Toast.makeText(this, "מחיקת תמונה מה-DB נכשלה", Toast.LENGTH_SHORT).show();
                        Log.e("UploadImagesActivity", "DB delete failed", task.getException());
                        return;
                    }

                    // 4) ניקוי מהמֶערך propertyImages אם הוא עדיין מצביע ל-path הזה
                    removeFromPropertyImagesArrayIfNeeded(image);
                }
        );
    }

    private void removeFromPropertyImagesArrayIfNeeded(@NonNull Image image) {
        final String name = arrayNameForCategory(image.getCategory());
        final String path = image.getUrl();
        if (name == null || path == null || path.trim().isEmpty()) return;

        ProjectRepository.getInstance().removePropertyImageIfMatches(
                projectId, name, path,
                t -> {
                    if (!t.isSuccessful()) {
                        Log.e("UploadImagesActivity",
                                "removePropertyImageIfMatches failed for " + name, t.getException());
                    } else {
                        Log.d("UploadImagesActivity",
                                "removePropertyImageIfMatches OK for " + name);
                    }
                }
        );
    }

    private void upsertPropertyImagesArray(Image.Category category, String uriString) {
        if (uriString == null || uriString.trim().isEmpty()) return;

        final String name = arrayNameForCategory(category);
        if (name == null) return;

        ProjectRepository.getInstance().upsertPropertyImage(
                projectId,
                name,
                uriString,
                task -> {
                    if (!task.isSuccessful()) {
                        Log.e("UploadImagesActivity", "upsertPropertyImage failed: " + name, task.getException());
                    } else {
                        Log.d("UploadImagesActivity", "upsertPropertyImage OK: " + name + " -> " + uriString);
                    }
                }
        );
    }

    private @Nullable String arrayNameForCategory(Image.Category category) {
        switch (category) {
            case EXTERIOR:
                return FirestoreConstants.FIELD_FRONT_IMAGE;    // "front_image"
            case LIVING_ROOM:
            case BEDROOM:
            case KITCHEN:
            case BATHROOM:
                return FirestoreConstants.FIELD_INTERIOR_IMAGE; // "interior_image"
            // כשתוסיפי קטגוריית טאבו:
            // case TABU_CROP: return FirestoreConstants.FIELD_TABU_CROP_IMAGE;
            default:
                return null;
        }
    }

    private void loadExistingImages(@NonNull String projectId) {
        ProjectRepository.getInstance().getImagesForProject(projectId, task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "שגיאה בטעינת תמונות קיימות", Toast.LENGTH_SHORT).show();
                return;
            }
            List<Image> existing = task.getResult();
            if (existing == null || existing.isEmpty()) {
                return; // אין מה להוסיף
            }

            // נבנה מיפוי קטגוריה -> סקשן
            java.util.Map<Image.Category, ImageCategorySection> byCat = new java.util.HashMap<>();
            for (ImageCategorySection s : categories) {
                byCat.put(s.category, s);
            }

            // נפזר את התמונות לסקשנים הנכונים
            for (Image img : existing) {
                if (img == null) continue;

                // לוודא שיש projectId
                if (img.getProjectId() == null) img.setProjectId(projectId);

                // אם ה־Image מגיע מ־DB עם מחרוזת קטגוריה – ודאי שה־Image(Map) שלך ממפה ל־enum.
                Image.Category cat = img.getCategory();
                if (cat == null) continue; // אם אין קטגוריה – אין לנו איפה להציג

                ImageCategorySection sec = byCat.get(cat);
                if (sec != null) {
                    // downloadUrl/https (Glide תומך גם ב-content:// בתצוגה)
                    sec.images.add(img);
                }
            }

            // עדכון UI – אם יש לך פונקציה מדויקת (notifyImageChanged) השתמשי בה; אחרת notifyDataSetChanged
            try {
                categoriesAdapter.notifyDataSetChanged();
            } catch (Throwable t) {
                for (int i = 0; i < categories.size(); i++) {
                    categoriesAdapter.notifyItemChanged(i);
                }
            }
        });
    }

    /**
     * מציג טוסט מסכם (מפה ידידותית בעברית מגיעה מה-Parser).
     */
    private void showClassificationResult(Image.Category category, Map<String, String> parsedDisplayKv) {
        if (parsedDisplayKv == null || parsedDisplayKv.isEmpty()) {
            Toast.makeText(this, "סיווג הושלם", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder message = new StringBuilder();

        switch (category) {
            case KITCHEN:
                if (parsedDisplayKv.containsKey("ארונות")) {
                    message.append("ארונות: ").append(parsedDisplayKv.get("ארונות"));
                }
                if (parsedDisplayKv.containsKey("משטח עבודה")) {
                    if (message.length() > 0) message.append("\n");
                    message.append("משטח: ").append(parsedDisplayKv.get("משטח עבודה"));
                }
                break;

            case ENTRANCE_DOOR:
                if (parsedDisplayKv.containsKey("מספר דירה")) {
                    message.append("מספר דירה: ").append(parsedDisplayKv.get("מספר דירה"));
                }
                if (parsedDisplayKv.containsKey("סוג דלת")) {
                    if (message.length() > 0) message.append("\n");
                    message.append("דלת: ").append(parsedDisplayKv.get("סוג דלת"));
                }
                break;

            case LIVING_ROOM:
            case BEDROOM:
                if (parsedDisplayKv.containsKey("ריצוף")) {
                    message.append("ריצוף: ").append(parsedDisplayKv.get("ריצוף"));
                }
                if (parsedDisplayKv.containsKey("מיזוג אוויר")) {
                    if (message.length() > 0) message.append("\n");
                    message.append("מיזוג: ").append(parsedDisplayKv.get("מיזוג אוויר"));
                }
                if (parsedDisplayKv.containsKey("חלונות")) {
                    if (message.length() > 0) message.append("\n");
                    message.append("חלונות: ").append(parsedDisplayKv.get("חלונות"));
                }
                if (parsedDisplayKv.containsKey("סורגים")) {
                    if (message.length() > 0) message.append("\n");
                    message.append("סורגים: ").append(parsedDisplayKv.get("סורגים"));
                }
                if (parsedDisplayKv.containsKey("מידת ריצוף")) {
                    if (message.length() > 0) message.append("\n");
                    message.append("מידת ריצוף: ").append(parsedDisplayKv.get("מידת ריצוף"));
                }
                break;

            default:
                // לקטגוריות אחרות נדפיס את כל מה שיש, שורה-שורה
                for (Map.Entry<String, String> e : parsedDisplayKv.entrySet()) {
                    if (message.length() > 0) message.append("\n");
                    message.append(e.getKey()).append(": ").append(e.getValue());
                }
        }

        if (message.length() > 0) {
            Toast.makeText(this, message.toString(), Toast.LENGTH_LONG).show();
        }
    }
}

// file: app/src/main/java/com/example/finalprojectappraisal/activity/newProject/UploadImagesActivity.java
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.property.ApartmentDetailsActivity;
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
                }
        );
        recyclerCategories.setAdapter(categoriesAdapter);

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

            // 1) יצירת Image ועדכון UI מיידי
            Image img = new Image();
            img.setUrl(imageUri.toString());
            img.setProjectId(projectId);
            img.setCategory(pickedSection.category);
            img.setDescription("מעבד תמונה...");

            pickedSection.images.add(img);
            int sectionIndex = categories.indexOf(pickedSection);
            if (sectionIndex != -1) {
                categoriesAdapter.notifyImageChanged(sectionIndex); // התאימי לשם המתודה באדפטר שלך
            }

            // 2) סיווג → פריסה → שמירה: ה-Helper כבר עושה הכל כולל שמירה ועדכון פרויקט
            EnhancedGeminiHelper.classifyImageAndSave(
                    this,
                    imageUri,
                    projectId,
                    img,
                    pickedSection.prompt, // אפשר גם בלי להעביר, שה-Helper יבחר לפי קטגוריה
                    new EnhancedGeminiHelper.EnhancedClassificationCallback() {

                        @Override
                        public void onResult(String rawResult, Map<String, String> parsedDisplayKv) {
                            runOnUiThread(() -> {
                                // רענון קלף הסקשן לאחר עיבוד
                                int idx = categories.indexOf(pickedSection);
                                if (idx != -1) {
                                    categoriesAdapter.notifyImageChanged(idx);
                                }
                                // הצגת תקציר נוח לפי קטגוריה
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
                            runOnUiThread(() ->
                                    Toast.makeText(UploadImagesActivity.this, "התמונה נשמרה והפרויקט עודכן!", Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
            );
        }
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

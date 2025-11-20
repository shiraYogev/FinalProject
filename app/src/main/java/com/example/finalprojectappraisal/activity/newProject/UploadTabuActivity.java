package com.example.finalprojectappraisal.activity.newProject;

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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.HomePageActivity;
import com.example.finalprojectappraisal.activity.newProject.images.UploadImagesActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Map;

public class UploadTabuActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQUEST_CAMERA_PERMISSION = 201;

    private String projectId;
    private Uri pendingCameraUri;
    private Uri currentTabuUri;

    private ImageView tabuImageView;
    private View placeholderLayout;
    private FloatingActionButton btnDeleteImage;
    private TextView processingStatus;
    private MaterialButton btnSaveAndContinue;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_tabu);

        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null) {
            toast("לא נמצא מזהה פרויקט");
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();
        setupListeners();
        loadExistingTabu();
    }

    private void initViews() {
        tabuImageView = findViewById(R.id.tabu_image);
        placeholderLayout = findViewById(R.id.placeholder_layout);
        btnDeleteImage = findViewById(R.id.btn_delete_image);
        processingStatus = findViewById(R.id.processing_status);
        btnSaveAndContinue = findViewById(R.id.btnSaveAndContinue);

        MaterialCardView backButtonCard = findViewById(R.id.back_button_card);
        MaterialButton btnGallery = findViewById(R.id.btn_gallery);
        MaterialButton btnCamera = findViewById(R.id.btn_camera);

        backButtonCard.setOnClickListener(v -> onBackPressed());
        btnGallery.setOnClickListener(v -> openGallery());
        btnCamera.setOnClickListener(v -> openCameraWithPermissionCheck());
    }

    private void setupListeners() {
        MaterialCardView imageContainer = findViewById(R.id.image_container);
        imageContainer.setOnClickListener(v -> {
            if (currentTabuUri == null) {
                showImageSourceDialog();
            }
        });

        btnDeleteImage.setOnClickListener(v -> deleteTabuImage());

        btnSaveAndContinue.setOnClickListener(v -> {
            if (currentTabuUri == null) {
                toast("יש להעלות תמונת טאבו לפני המשך");
                return;
            }
            navigateToNextScreen();
        });
    }

    private void showImageSourceDialog() {
        CharSequence[] options = new CharSequence[]{"גלריה", "מצלמה"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
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

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Tabu_" + System.currentTimeMillis());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == REQUEST_IMAGE_PICK) {
            if (data == null || data.getData() == null) return;
            Uri imageUri = data.getData();
            handleNewTabuImage(imageUri);
        } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (pendingCameraUri == null) {
                toast("שגיאה בקבלת התמונה מהמצלמה");
                return;
            }
            handleNewTabuImage(pendingCameraUri);
        }
    }

    private void handleNewTabuImage(Uri imageUri) {
        currentTabuUri = imageUri;
        displayTabuImage(imageUri);
        uploadTabuToFirebase(imageUri);
    }

    private void displayTabuImage(Uri imageUri) {
        placeholderLayout.setVisibility(View.GONE);
        tabuImageView.setVisibility(View.VISIBLE);
        btnDeleteImage.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .into(tabuImageView);
    }

    private void uploadTabuToFirebase(Uri imageUri) {
        processingStatus.setVisibility(View.VISIBLE);
        processingStatus.setText("מעלה תמונה...");

        String fileName = "tabu_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference()
                .child("projects")
                .child(projectId)
                .child("tabu")
                .child(fileName);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        saveTabuUrlToFirestore(downloadUri.toString());
                    }).addOnFailureListener(e -> {
                        processingStatus.setText("שגיאה בקבלת URL");
                        toast("שגיאה: " + e.getMessage());
                    });
                })
                .addOnFailureListener(e -> {
                    processingStatus.setText("שגיאה בהעלאה");
                    toast("שגיאה בהעלאה: " + e.getMessage());
                    Log.e("UploadTabu", "Upload failed", e);
                });
    }

    private void saveTabuUrlToFirestore(String downloadUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("tabu_crop_image", downloadUrl);

        db.collection("projects")
                .document(projectId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    processingStatus.setText("✓ התמונה נשמרה בהצלחה");
                    toast("תמונת הטאבו נשמרה");
                })
                .addOnFailureListener(e -> {
                    processingStatus.setText("שגיאה בשמירה");
                    toast("שגיאה בשמירה: " + e.getMessage());
                    Log.e("UploadTabu", "Firestore save failed", e);
                });
    }

    private void deleteTabuImage() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("מחיקת תמונה")
                .setMessage("האם אתה בטוח שברצונך למחוק את תמונת הטאבו?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    currentTabuUri = null;
                    tabuImageView.setVisibility(View.GONE);
                    btnDeleteImage.setVisibility(View.GONE);
                    placeholderLayout.setVisibility(View.VISIBLE);
                    processingStatus.setVisibility(View.GONE);

                    // Remove from Firestore
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("tabu_crop_image", null);

                    db.collection("projects")
                            .document(projectId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> toast("תמונת הטאבו נמחקה"))
                            .addOnFailureListener(e -> toast("שגיאה במחיקה: " + e.getMessage()));
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void loadExistingTabu() {
        db.collection("projects")
                .document(projectId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String tabuUrl = documentSnapshot.getString("tabu_crop_image");
                        if (tabuUrl != null && !tabuUrl.isEmpty()) {
                            currentTabuUri = Uri.parse(tabuUrl);
                            displayTabuImage(currentTabuUri);
                            processingStatus.setVisibility(View.VISIBLE);
                            processingStatus.setText("✓ קיימת תמונת טאבו");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UploadTabu", "Failed to load existing tabu", e);
                });
    }

    private void navigateToNextScreen() {
        try {
            // במקום UploadImagesActivity, נעבור ל-HomePageActivity
            Intent intent = new Intent(this, HomePageActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            toast("שגיאה במעבר: " + e.getMessage());
            Log.e("UploadTabu", "Navigation error", e);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
package com.example.finalprojectappraisal.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.Appraiser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AppraiserSetupActivity extends AppCompatActivity {

    private static final String TAG = "AppraiserSetup";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private TextInputEditText etFirstName, etLastName, etPhoneNumber;
    private MaterialButton btnSaveAppraiserInfo;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appraiser_setup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        // **תיקון: קוד אתחול ה-UI חייב לבוא לפני השימוש בו**
        initViews();

        checkIfSetupIsComplete();
    }

    private void checkIfSetupIsComplete() {
        showProgress(true); // זה בטוח כי initViews כבר נקרא
        db.collection("appraisers").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getBoolean("setupCompleted") != null && documentSnapshot.getBoolean("setupCompleted")) {
                        // הנתונים קיימים וההגדרה הושלמה - נווט למסך הראשי
                        navigateToHomePageActivity();
                    } else {
                        // הנתונים חסרים - הצג את המסך למילוי פרטים
                        showProgress(false);
                        setupClickListeners();
                        populateFieldsFromUserProfile();
                    }
                })
                .addOnFailureListener(e -> {
                    // שגיאה בקריאה מה-Firestore, הצג את מסך ההגדרה כדי למנוע קריסה
                    Log.e(TAG, "Error checking setup status", e);
                    showProgress(false);
                    setupClickListeners();
                    populateFieldsFromUserProfile();
                });
    }

    private void initViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnSaveAppraiserInfo = findViewById(R.id.btnSaveAppraiserInfo);
        progressBar = findViewById(R.id.progressBar);
    }

    private void populateFieldsFromUserProfile() {
        if (currentUser.getDisplayName() != null) {
            String displayName = currentUser.getDisplayName();
            String[] nameParts = displayName.split(" ", 2);
            if (nameParts.length > 0) {
                etFirstName.setText(nameParts[0]);
            }
            if (nameParts.length > 1) {
                etLastName.setText(nameParts[1]);
            }
        }
    }


    private void setupClickListeners() {
        btnSaveAppraiserInfo.setOnClickListener(v -> saveAppraiserInfo());
    }

    private void saveAppraiserInfo() {
        if (!validateInput()) {
            return;
        }

        showProgress(true);

        String email = currentUser.getEmail();
        String userId = currentUser.getUid();

        db.collection("appraisers").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Appraiser.AccessPermission permission;
                    if (documentSnapshot.exists() && documentSnapshot.contains("accessPermissions")) {
                        String roleStr = documentSnapshot.getString("accessPermissions");
                        try {
                            permission = Appraiser.AccessPermission.valueOf(roleStr);
                        } catch (IllegalArgumentException e) {
                            permission = Appraiser.AccessPermission.USER;
                        }
                    } else {
                        permission = Appraiser.AccessPermission.USER;
                    }
                    saveUserInfoToFirestore(userId, email, permission);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check existing user permissions. Saving as USER.", e);
                    saveUserInfoToFirestore(userId, email, Appraiser.AccessPermission.USER);
                });
    }

    private void saveUserInfoToFirestore(String userId, String email, Appraiser.AccessPermission permission) {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String fullName = firstName + " " + lastName;
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        Map<String, Object> appraiserData = new HashMap<>();
        appraiserData.put("appraiserId", userId);
        appraiserData.put("firstName", firstName);
        appraiserData.put("lastName", lastName);
        appraiserData.put("fullName", fullName);
        appraiserData.put("email", email);
        appraiserData.put("phoneNumber", phoneNumber);
        appraiserData.put("accessPermissions", permission.name());
        appraiserData.put("activeProjects", new ArrayList<>());
        appraiserData.put("appraisalHistory", new ArrayList<>());
        appraiserData.put("createdAt", System.currentTimeMillis());
        appraiserData.put("setupCompleted", true);

        db.collection("appraisers")
                .document(userId)
                .set(appraiserData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Appraiser info saved successfully");
                    showProgress(false);

                    String message = (permission == Appraiser.AccessPermission.ADMIN || permission == Appraiser.AccessPermission.SUPER_ADMIN) ?
                            "הפרטים נשמרו בהצלחה! הינך מוגדר כמנהל מערכת" :
                            "הפרטים נשמרו בהצלחה";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                    navigateToHomePageActivity();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving appraiser info", e);
                    showProgress(false);
                    Toast.makeText(this, "שגיאה בשמירת הפרטים: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInput() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("שם פרטי נדרש");
            etFirstName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("שם משפחה נדרש");
            etLastName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            etPhoneNumber.setError("מספר טלפון נדרש");
            etPhoneNumber.requestFocus();
            return false;
        }

        if (phoneNumber.length() < 9) {
            etPhoneNumber.setError("מספר טלפון לא תקין");
            etPhoneNumber.requestFocus();
            return false;
        }

        return true;
    }

    private void showProgress(boolean show) {
        // **תיקון: בדיקה לוודא שהאובייקט לא null לפני השימוש**
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnSaveAppraiserInfo != null) {
            btnSaveAppraiserInfo.setEnabled(!show);
        }
    }

    private void navigateToHomePageActivity() {
        Intent intent = new Intent(this, HomePageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // מפעיל את הפעולה הרגילה של כפתור החזרה
        // זה יסגור את האקטיביטי הנוכחי ויחזור למסך הקודם
        super.onBackPressed();
    }
}
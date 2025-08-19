package com.example.finalprojectappraisal.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.Appraiser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class  AppraiserSetupActivity extends AppCompatActivity {

    private static final String TAG = "AppraiserSetup";
    private static final String PREFS_NAME = "AppraiserPrefs";
    private static final String KEY_SETUP_COMPLETED = "setup_completed_";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Views
    private TextInputEditText etFirstName, etLastName, etPhoneNumber;
    private MaterialButton btnSaveAppraiserInfo, btnSkipForNow;
    private ProgressBar progressBar;

    // Admin emails list - configure these emails as admins
    private static final String[] ADMIN_EMAILS = {
            "admin@yourcompany.com",
            "manager@yourcompany.com"
            // Add more admin emails here
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appraiser_setup);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // User not authenticated, redirect to login
            //startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Check if setup was already completed
        if (isSetupAlreadyCompleted()) {
            navigateToMainActivity();
            return;
        }

        initViews();
        setupClickListeners();

    }

    private void initViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnSaveAppraiserInfo = findViewById(R.id.btnSaveAppraiserInfo);
        btnSkipForNow = findViewById(R.id.btnSkipForNow);
        progressBar = findViewById(R.id.progressBar);

        // Pre-fill names if available from Firebase Auth
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
        btnSkipForNow.setOnClickListener(v -> skipSetup());
    }

    private void saveAppraiserInfo() {
        if (!validateInput()) {
            return;
        }

        showProgress(true);

        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String fullName = firstName + " " + lastName;
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String email = currentUser.getEmail();
        String userId = currentUser.getUid();

        // Determine access permission based on email
        Appraiser.AccessPermission permission = isAdminEmail(email) ?
                Appraiser.AccessPermission.ADMIN : Appraiser.AccessPermission.USER;

        // Create appraiser data map for Firestore
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

        // Save to Firestore
        db.collection("appraisers")
                .document(userId)
                .set(appraiserData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Appraiser info saved successfully");
                    showProgress(false);
                    markSetupAsCompleted();

                    String message = permission == Appraiser.AccessPermission.ADMIN ?
                            "הפרטים נשמרו בהצלחה! הינך מוגדר כמנהל מערכת" :
                            "הפרטים נשמרו בהצלחה";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving appraiser info", e);
                    showProgress(false);
                    Toast.makeText(this, "שגיאה בשמירת הפרטים: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
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

    /**
     * Check if the given email is in the admin emails list
     */
    private boolean isAdminEmail(String email) {
        if (email == null) return false;

        for (String adminEmail : ADMIN_EMAILS) {
            if (adminEmail.equalsIgnoreCase(email.trim())) {
                return true;
            }
        }
        return false;
    }

    private void skipSetup() {
        // User chose to skip setup for now
        Toast.makeText(this, "ניתן להשלים את הפרטים מההגדרות", Toast.LENGTH_LONG).show();
        navigateToMainActivity();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveAppraiserInfo.setEnabled(!show);
        btnSkipForNow.setEnabled(!show);
    }

    private boolean isSetupAlreadyCompleted() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_SETUP_COMPLETED + currentUser.getUid(), false);
    }

    private void markSetupAsCompleted() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED + currentUser.getUid(), true).apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // Method to check if appraiser info exists in Firestore (alternative approach)
    public static void checkAppraiserSetupStatus(FirebaseUser user,
                                                 OnSetupStatusChecked callback) {
        if (user == null) {
            callback.onStatusChecked(false);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("appraisers")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean exists = documentSnapshot.exists() &&
                            documentSnapshot.getBoolean("setupCompleted") == Boolean.TRUE;
                    callback.onStatusChecked(exists);
                })
                .addOnFailureListener(e -> callback.onStatusChecked(false));
    }

    public interface OnSetupStatusChecked {
        void onStatusChecked(boolean isCompleted);
    }
}
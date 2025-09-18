package com.example.finalprojectappraisal.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName;
    private Button btnEditProfile, btnContactUs, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        loadUserProfile();
        setupClickListeners();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnContactUs = findViewById(R.id.btnContactUs);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadUserProfile() {
        // Here you would load user data from Firebase Firestore
        // For simplicity, we'll use the display name from Firebase Auth
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String displayName = auth.getCurrentUser().getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvUserName.setText(displayName);
            } else {
                tvUserName.setText(auth.getCurrentUser().getEmail());
            }
        }
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnContactUs.setOnClickListener(v -> {
            // Implement 'Contact Us' functionality here, e.g., opening a support email
            // For example:
            // Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "support@yourcompany.com", null));
            // startActivity(Intent.createChooser(emailIntent, "Send email..."));
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
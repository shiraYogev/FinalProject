package com.example.finalprojectappraisal.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.activitiesAdmin.AdminListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private Button adminActionsButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI component
        adminActionsButton = findViewById(R.id.btnAdminActions);

        // Check if the current user is an admin
        checkIfAdmin();
    }

    private void checkIfAdmin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not logged in, hide the button
            adminActionsButton.setVisibility(Button.GONE);
            return;
        }

        String userEmail = currentUser.getEmail();
        if (userEmail == null) {
            adminActionsButton.setVisibility(Button.GONE);
            return;
        }

        DocumentReference adminRef = db.collection("admins").document(userEmail);

        adminRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // User is an admin, show the button and set up the click listener
                adminActionsButton.setVisibility(Button.VISIBLE);
                adminActionsButton.setOnClickListener(v -> {
                    // Navigate to the AdminListActivity
                    Intent intent = new Intent(SettingsActivity.this, AdminListActivity.class);
                    startActivity(intent);
                });
            } else {
                // User is not an admin, hide the button
                adminActionsButton.setVisibility(Button.GONE);
            }
        }).addOnFailureListener(e -> {
            // Handle any errors in fetching the data
            Toast.makeText(this, "שגיאה בבדיקת הרשאות", Toast.LENGTH_SHORT).show();
            adminActionsButton.setVisibility(Button.GONE);
        });
    }
}
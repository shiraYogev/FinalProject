package com.example.finalprojectappraisal.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etFirstName, etLastName, etPhoneNumber;
    private MaterialButton btnSave;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "התחבר מחדש כדי לעדכן פרטים", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadExistingData();
        setupClickListener();
    }

    private void initViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnSave = findViewById(R.id.btnSave);
    }

    private void loadExistingData() {
        db.collection("appraisers")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etFirstName.setText(documentSnapshot.getString("firstName"));
                        etLastName.setText(documentSnapshot.getString("lastName"));
                        etPhoneNumber.setText(documentSnapshot.getString("phoneNumber"));
                    } else {
                        Toast.makeText(this, "לא נמצאו נתונים קודמים. אנא מלא את הפרטים.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בטעינת הנתונים.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListener() {
        btnSave.setOnClickListener(v -> saveUpdatedData());
    }

    private void saveUpdatedData() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "יש למלא את כל השדות.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("phoneNumber", phoneNumber);
        updates.put("fullName", firstName + " " + lastName);

        db.collection("appraisers")
                .document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "הפרטים עודכנו בהצלחה.", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to the previous screen
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
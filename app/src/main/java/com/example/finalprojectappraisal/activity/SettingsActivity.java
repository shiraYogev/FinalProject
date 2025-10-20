package com.example.finalprojectappraisal.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.activitiesAdmin.AdminListActivity;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Appraiser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnCompleteListener;

public class SettingsActivity extends AppCompatActivity {

    private Button adminActionsButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        adminActionsButton = findViewById(R.id.btnAdminActions);

        checkIfAdmin();
    }

    private void checkIfAdmin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            adminActionsButton.setVisibility(View.GONE);
            return;
        }

        String userId = currentUser.getUid();

        // קריאה למתודה getUserPermissions שלך
        ProjectRepository.getInstance().getUserPermissions(userId, new OnCompleteListener<Appraiser.AccessPermission>() {
            @Override
            public void onComplete(Task<Appraiser.AccessPermission> task) {
                if (task.isSuccessful()) {
                    Appraiser.AccessPermission permissions = task.getResult();
                    if (permissions == Appraiser.AccessPermission.ADMIN || permissions == Appraiser.AccessPermission.SUPER_ADMIN) {
                        adminActionsButton.setVisibility(View.VISIBLE);
                        adminActionsButton.setOnClickListener(v -> {
                            Intent intent = new Intent(SettingsActivity.this, AdminListActivity.class);
                            startActivity(intent);
                        });
                    } else {
                        adminActionsButton.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(SettingsActivity.this, "שגיאה בבדיקת הרשאות", Toast.LENGTH_SHORT).show();
                    adminActionsButton.setVisibility(View.GONE);
                }
            }
        });
    }
}
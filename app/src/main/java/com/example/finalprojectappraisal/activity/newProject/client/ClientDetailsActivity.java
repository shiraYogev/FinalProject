package com.example.finalprojectappraisal.activity.newProject.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.images.UploadImagesActivity;
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Project;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;              // ✨
import com.google.firebase.firestore.FirebaseFirestore;      // ✨

public class ClientDetailsActivity extends AppCompatActivity {

    private EditText clientIdEditText;
    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText phoneNumberEditText;
    private EditText fullAddressEditText;
    private Button saveClientButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_details);

        clientIdEditText = findViewById(R.id.clientIdEditText);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        fullAddressEditText = findViewById(R.id.fullAddressEditText);
        saveClientButton = findViewById(R.id.saveClientButton);

        saveClientButton.setOnClickListener(v -> saveClientAndProject());
    }

    private void saveClientAndProject() {
        String clientId   = clientIdEditText.getText().toString().trim();
        String fullName   = fullNameEditText.getText().toString().trim();
        String email      = emailEditText.getText().toString().trim();
        String phoneNumber= phoneNumberEditText.getText().toString().trim();
        String fullAddress= fullAddressEditText.getText().toString().trim();

        if (clientId.isEmpty() || fullName.isEmpty() || fullAddress.isEmpty()) {
            Toast.makeText(this, "יש למלא לפחות תעודת זהות, שם מלא וכתובת", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת אובייקט Client
        Client client = new Client(clientId, fullName, email, phoneNumber, null);

        // יצירת אובייקט Project
        Project project = new Project();
        project.setClient(client);
        project.setFullAddress(fullAddress);

        // מזהה השמאי (UID)
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        project.setAppraiserId(uid);

        // שמירה ל-Firebase עם מזהה אוטומטי
        ProjectRepository.getInstance().createNewProject(project, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ClientDetailsActivity.this, "הפרויקט נשמר בהצלחה!", Toast.LENGTH_SHORT).show();

                // ה-ID שנוצר לפרויקט
                String projectId = project.getProjectId();

                // ✨ הוספה למערך activeProjects של השמאי
                FirebaseFirestore.getInstance()
                        .collection("appraisers")
                        .document(uid)
                        .update("activeProjects", FieldValue.arrayUnion(projectId))
                        .addOnSuccessListener(v -> {
                            // אופציונלי: לוג/טוסט
                            // Toast.makeText(this, "נוסף לרשימת הפרויקטים הפעילים", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "שגיאה בעדכון activeProjects: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });

                // מעבר למסך הבא
                Intent intent = new Intent(ClientDetailsActivity.this, UploadImagesActivity.class);
                intent.putExtra("projectId", projectId);
                startActivity(intent);
                finish();
            } else {
                Exception exception = task.getException();
                String error = (exception != null) ? exception.getMessage() : "שגיאה לא ידועה";
                Toast.makeText(ClientDetailsActivity.this, "שגיאה בשמירת הפרויקט: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}

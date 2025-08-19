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

/**
 * ClientDetailsActivity manages the user interface for entering and updating client information in the appraisal system.
 * It allows users to input a client's ID, name, email, phone number, and property address.
 * After saving the data, it creates a new Project and stores it in Firebase under the address.
 */

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
        String clientId = clientIdEditText.getText().toString().trim();
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        String fullAddress = fullAddressEditText.getText().toString().trim();

        if (clientId.isEmpty() || fullName.isEmpty() || fullAddress.isEmpty()) {
            Toast.makeText(this, "יש למלא לפחות תעודת זהות, שם מלא וכתובת", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת אובייקט Client
        Client client = new Client(clientId, fullName, email, phoneNumber,null);

        // יצירת אובייקט Project עם פרטי הלקוח והכתובת
        Project project = new Project(); // משתמש בבנאי ברירת המחדל שמכניס גם זמנים וסטטוס
        project.setClient(client);
        project.setFullAddress(fullAddress);

        // הוספת מזהה השמאי (המשתמש הנוכחי)
        //Appraiser appraiser = getAppraiserFromDatabase(); // מחזיר את כל האובייקט עם כל השדות מלאים
        //project.setAppraiser(appraiser);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        project.setAppraiserId(uid);



        // ניתן להוסיף כאן עוד שדות אם תרצי למסך (location, buildingType וכו')

        // שמירה ל-Firebase עם מזהה אוטומטי
        ProjectRepository.getInstance().createNewProject(project, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ClientDetailsActivity.this, "הפרויקט נשמר בהצלחה!", Toast.LENGTH_SHORT).show();

                // קבלת ה-id שג'ונרטנו (אם תרצי להעביר אותו הלאה)
                String projectId = project.getProjectId();

                // מעבר לפעילות הבאה
                Intent intent = new Intent(ClientDetailsActivity.this, UploadImagesActivity.class);
                intent.putExtra("projectId", projectId); // להעביר projectId הלאה
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
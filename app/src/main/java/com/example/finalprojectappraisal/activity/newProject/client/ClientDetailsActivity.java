package com.example.finalprojectappraisal.activity.newProject.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.images.UploadImagesActivity;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Project;

import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

// ייבוא ספריית Google Places
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.gms.common.api.Status;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClientDetailsActivity extends AppCompatActivity {

    private EditText clientIdEditText;
    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText phoneNumberEditText;
    private EditText fullAddressEditText;
    private Button saveClientButton;

    private @Nullable String projectId; // null = create mode, not null = edit mode

    // קבוע לבקשת Intent של Autocomplete
    private static final int AUTOCOMPLETE_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_details);

        clientIdEditText    = findViewById(R.id.clientIdEditText);
        fullNameEditText    = findViewById(R.id.fullNameEditText);
        emailEditText       = findViewById(R.id.emailEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        fullAddressEditText = findViewById(R.id.fullAddressEditText);
        saveClientButton    = findViewById(R.id.saveClientButton);

        // 1. אתחול Google Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        // 2. הגדרת לחיצה על שדה הכתובת להפעלת Autocomplete
        // שדה זה הוגדר כ-focusable="false" ב-XML כדי לאפשר את הלחיצה
        fullAddressEditText.setOnClickListener(v -> startAutocompleteIntent());

        // אם נכנסנו מעריכת פרויקט – יהיה projectId
        projectId = getIntent().getStringExtra("projectId");

        if (projectId != null && !projectId.trim().isEmpty()) {
            setTitle("עריכת פרטי לקוח");
            loadExistingClient(projectId);
        } else {
            setTitle("יצירת פרויקט חדש - פרטי לקוח");
        }

        saveClientButton.setOnClickListener(v -> {
            if (projectId != null && !projectId.trim().isEmpty()) {
                updateClientForExistingProject(projectId);
            } else {
                createProjectWithClient();
            }
        });
    }

    /**
     * מפעיל את ה-Intent המובנה של Autocomplete של גוגל
     */
    private void startAutocompleteIntent() {
        // רכיבי הנתונים שאנחנו מבקשים מה-API.
        // עדיף לבקש רק את השדות הנחוצים כדי לחסוך בעלויות.
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS, // הכתובת המלאה
                Place.Field.LAT_LNG // אם נדרש מיקום גיאוגרפי
        );

        // יצירת Intent של Autocomplete
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                // הגבלת החיפוש לישראל בלבד ('IL')
                .setCountries(Collections.singletonList("IL"))
                .build(this);

        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    /**
     * מטפל בתוצאה החוזרת מה-Autocomplete Intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // הצלחה: המשתמש בחר מקום
                Place place = Autocomplete.getPlaceFromIntent(data);

                String address = place.getAddress();
                if (address != null) {
                    // ממלאים את שדה הכתובת בכתובת המלאה שנבחרה
                    fullAddressEditText.setText(address);
                }

                Toast.makeText(this, "הכתובת נבחרה בהצלחה", Toast.LENGTH_SHORT).show();

            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // שגיאה: כשל ב-API או בחיבור
                Status status = Autocomplete.getStatusFromIntent(data);
                if (status != null && status.getStatusMessage() != null) {
                    Toast.makeText(this, "שגיאה בבחירת כתובת: " + status.getStatusMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "שגיאה לא ידועה בבחירת כתובת", Toast.LENGTH_LONG).show();
                }
            } else if (resultCode == RESULT_CANCELED) {
                // המשתמש ביטל את הפעולה
                // אין צורך בהודעה, פשוט ממשיכים
            }
        }
    }


    /** מצב עריכה: טוען את הפרויקט וממלא את פרטי הלקוח במסך */
    private void loadExistingClient(String projectId) {
        ProjectRepository.getInstance().getProject(projectId, task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                Toast.makeText(this, "לא נמצאו פרטי פרויקט", Toast.LENGTH_LONG).show();
                return;
            }
            Project p = task.getResult().toObject(Project.class);
            if (p == null) return;

            // ממלאים שדות—אם חסר, משאירים ריק
            Client c = p.getClient();
            if (c != null) {
                if (c.getClientId() != null)        clientIdEditText.setText(c.getClientId());
                if (c.getFullName() != null)        fullNameEditText.setText(c.getFullName());
                if (c.getEmail() != null)           emailEditText.setText(c.getEmail());
                if (c.getPhoneNumber() != null)     phoneNumberEditText.setText(c.getPhoneNumber());
            }
            if (p.getFullAddress() != null)         fullAddressEditText.setText(p.getFullAddress());
        });
    }

    /** מצב עריכה: מעדכן את פרטי הלקוח בפרויקט קיים */
    private void updateClientForExistingProject(String projectId) {
        String clientId     = clientIdEditText.getText().toString().trim();
        String fullName     = fullNameEditText.getText().toString().trim();
        String email        = emailEditText.getText().toString().trim();
        String phoneNumber  = phoneNumberEditText.getText().toString().trim();
        String fullAddress  = fullAddressEditText.getText().toString().trim();

        if (clientId.isEmpty() || fullName.isEmpty()) {
            Toast.makeText(this, "יש למלא לפחות תעודת זהות ושם מלא", Toast.LENGTH_SHORT).show();
            return;
        }

        Client client = new Client(clientId, fullName, email, phoneNumber, null);

        // שמירת פרטי לקוח
        ProjectRepository.getInstance().saveClientDetails(projectId, client, task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "שמירת פרטי הלקוח נכשלה", Toast.LENGTH_LONG).show();
                return;
            }

            // אם כתובת עודכנה – נעדכן גם אותה
            if (!fullAddress.isEmpty()) {
                java.util.Map<String, Object> fields = new java.util.HashMap<>();
                fields.put("fullAddress", fullAddress);
                ProjectRepository.getInstance().updateMultipleFields(projectId, fields, t2 -> {
                    // לא קריטי אם נכשל – נתקדם
                    Toast.makeText(this, "פרטי הלקוח נשמרו", Toast.LENGTH_SHORT).show();
                    finish(); // חוזרים לרשימת הפרויקטים
                });
            } else {
                Toast.makeText(this, "פרטי הלקוח נשמרו", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /** מצב יצירה: יוצר פרויקט חדש (ההתנהגות הישנה שלך) */
    private void createProjectWithClient() {
        String clientId     = clientIdEditText.getText().toString().trim();
        String fullName     = fullNameEditText.getText().toString().trim();
        String email        = emailEditText.getText().toString().trim();
        String phoneNumber  = phoneNumberEditText.getText().toString().trim();
        String fullAddress  = fullAddressEditText.getText().toString().trim();

        if (clientId.isEmpty() || fullName.isEmpty() || fullAddress.isEmpty()) {
            Toast.makeText(this, "יש למלא לפחות תעודת זהות, שם מלא וכתובת", Toast.LENGTH_SHORT).show();
            return;
        }

        Client client = new Client(clientId, fullName, email, phoneNumber, null);
        Project project = new Project();
        project.setClient(client);
        project.setFullAddress(fullAddress);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) project.setAppraiserId(uid);

        ProjectRepository.getInstance().createNewProject(project, task -> {
            if (!task.isSuccessful()) {
                String error = task.getException() != null ? task.getException().getMessage() : "שגיאה לא ידועה";
                Toast.makeText(this, "שגיאה בשמירת הפרויקט: " + error, Toast.LENGTH_LONG).show();
                return;
            }

            String newProjectId = project.getProjectId();

            // עידכון רשימת projects של השמאי (לא חובה להמשך העריכה)
            if (uid != null) {
                FirebaseFirestore.getInstance()
                        .collection("appraisers")
                        .document(uid)
                        .update("activeProjects", FieldValue.arrayUnion(newProjectId));
            }

            Toast.makeText(this, "הפרויקט נשמר בהצלחה!", Toast.LENGTH_SHORT).show();

            // מעבר למסך הבא בזרימה שלך
            Intent intent = new Intent(ClientDetailsActivity.this, UploadImagesActivity.class);
            intent.putExtra("projectId", newProjectId);
            startActivity(intent);
            finish();
        });
    }
}
package com.example.finalprojectappraisal.activity.newProject.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.HomePageActivity;
import com.example.finalprojectappraisal.activity.newProject.ProgressStepperHelper;
import com.example.finalprojectappraisal.activity.newProject.images.UploadImagesActivity;
import com.example.finalprojectappraisal.export.ProjectCompletionActivity;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.databinding.ActivityClientDetailsBinding;
import com.example.finalprojectappraisal.model.Client;
import com.example.finalprojectappraisal.model.Project;

import com.google.android.gms.maps.model.LatLng;
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
import java.util.Locale;
import com.example.finalprojectappraisal.BuildConfig;

public class ClientDetailsActivity extends AppCompatActivity {

    private EditText caseNumberEditText;
    private EditText clientIdEditText;
    private EditText firstNameEditText; // מפוצל
    private EditText lastNameEditText;  // מפוצל
    private EditText emailEditText;
    private EditText phoneNumberEditText;
    private EditText fullAddressEditText;
    private Button saveClientButton;

    private ProgressStepperHelper progressHelper;

    private ActivityClientDetailsBinding binding;

    private @Nullable String projectId; // null = create mode, not null = edit mode

    // קבוע לבקשת Intent של Autocomplete
    private static final int AUTOCOMPLETE_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- תיקון 1: שימוש נכון ב-Binding להגדרת Layout ורכיבים ---
        binding = ActivityClientDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        caseNumberEditText  = binding.caseNumberEditText;
        clientIdEditText    = binding.clientIdEditText;
        firstNameEditText   = binding.firstNameEditText;
        lastNameEditText    = binding.lastNameEditText;
        emailEditText       = binding.emailEditText;
        phoneNumberEditText = binding.phoneNumberEditText;
        fullAddressEditText = binding.fullAddressEditText;
        saveClientButton    = binding.saveClientButton;
        // -----------------------------------------------------------

        // 1. אתחול Google Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.GOOGLE_MAPS_API_KEY, Locale.forLanguageTag("he"));
        }
        // 2. הגדרת לחיצה על שדה הכתובת להפעלת Autocomplete
        // שדה זה הוגדר כ-focusable="false" ב-XML כדי לאפשר את הלחיצה
        fullAddressEditText.setOnClickListener(v -> startAutocompleteIntent());

        // אם נכנסנו מעריכת פרויקט – יהיה projectId
        projectId = getIntent().getStringExtra("projectId");

        if (projectId != null && !projectId.trim().isEmpty()) {
            setTitle("עריכת פרטי לקוח");
            caseNumberEditText.setEnabled(false);
            loadExistingClient(projectId);
        } else {
            setTitle("יצירת פרויקט חדש - פרטי לקוח");
        }


        setupProgressStepper();
        setupListeners();
    }

    private void setupProgressStepper() {
        // מציאת Root View נכונה עבור ProgressStepperHelper
        // אם משתמשים ב-Binding, Root View היא binding.getRoot()
        progressHelper = new ProgressStepperHelper(
                this,
                ProgressStepperHelper.STEP_CLIENT_DETAILS, // שלב 1 - פרטי לקוח
                binding.getRoot(),
                projectId
        );
        progressHelper.initialize();
    }

    private void setupListeners() {
        // 🏠 כפתור בית - חזרה לדף הבית עם ייצוא JSON אוטומטי
        binding.btnHomeHeader.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToHomeWithExport();
            }
        });

        // כפתור חזור - אם זה שלב 1, חזור למסך הקודם
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (progressHelper.isFirstStep()) {
                    finish(); // חזור למסך ראשי
                } else {
                    progressHelper.moveToPreviousStep();
                }
            }
        });

        // כפתור שמירה והמשך - מאחד את הלוגיקה ליצירה/עדכון ומעבר לשלב הבא
        binding.saveClientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    saveAndContinue();
                }
            }
        });
    }

    /**
     * 🏠 מעבר לדף הבית עם ייצוא אוטומטי של JSON
     */
    private void navigateToHomeWithExport() {
        // שמירת הנתונים הנוכחיים אם יש מה לשמור
        if (projectId != null) {
            // מעבר למסך סיום שמבצע ייצוא JSON ואז לדף הבית
            Intent intent = new Intent(this, ProjectCompletionActivity.class);
            intent.putExtra(ProjectCompletionActivity.EXTRA_PROJECT_ID, projectId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            // אם אין projectId, פשוט חזור לבית בלי ייצוא
            Intent intent = new Intent(this, HomePageActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    /**
     * פונקציה לבדיקת תקינות הקלט.
     * הערה: נדרש להוספה כחלק מהתיקון.
     */
    private boolean validateInputs() {
        String caseNumber = caseNumberEditText.getText().toString().trim();
        String fullAddress = fullAddressEditText.getText().toString().trim();

        if (projectId == null && caseNumber.isEmpty()) {
            caseNumberEditText.setError("חובה למלא מספר תיק");
            return false;
        }
        if (fullAddress.isEmpty()) {
            fullAddressEditText.setError("חובה למלא כתובת");
            return false;
        }
        return true;
    }

    /**
     * מאחדת את הלוגיקה של יצירה או עדכון, ושומרת את הנתונים, ואז עוברת לשלב הבא.
     */
    private void saveAndContinue() {
        if (projectId != null && !projectId.trim().isEmpty()) {
            // מצב עריכה
            updateClientForExistingProjectAndContinue(projectId);
        } else {
            // מצב יצירה
            createProjectWithClientAndContinue();
        }
    }


    /**
     * מפעיל את ה-Intent המובנה של Autocomplete של גוגל
     */
    private void startAutocompleteIntent() {
        // רכיבי הנתונים שאנחנו מבקשים מה-API.
        // עדיף לבקש רק את השדות הנחוצים כדי לחסוך בעלויות.
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,           // היה NAME
                Place.Field.FORMATTED_ADDRESS,       // היה ADDRESS
                Place.Field.LOCATION                 // היה LAT_LNG
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

                String address = place.getFormattedAddress();
                if (address != null) {
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
            caseNumberEditText.setText(projectId);
            Client c = p.getClient();
            if (c != null) {
                if (c.getClientId() != null)        clientIdEditText.setText(c.getClientId());
                if (c.getFirstName() != null)       firstNameEditText.setText(c.getFirstName());
                if (c.getLastName() != null)        lastNameEditText.setText(c.getLastName());
                if (c.getEmail() != null)           emailEditText.setText(c.getEmail());
                if (c.getPhoneNumber() != null)     phoneNumberEditText.setText(c.getPhoneNumber());
            }
            if (p.getFullAddress() != null)         fullAddressEditText.setText(p.getFullAddress());
        });
    }

    /** מצב עריכה: מעדכן את פרטי הלקוח בפרויקט קיים וממשיך לשלב הבא */
    private void updateClientForExistingProjectAndContinue(String projectId) {
        String clientId     = clientIdEditText.getText().toString().trim();
        String firstName    = firstNameEditText.getText().toString().trim();
        String lastName     = lastNameEditText.getText().toString().trim();
        String email        = emailEditText.getText().toString().trim();
        String phoneNumber  = phoneNumberEditText.getText().toString().trim();
        String fullAddress  = fullAddressEditText.getText().toString().trim();

        String combinedFullName = firstName + " " + lastName;

        Client client = new Client(clientId, combinedFullName, email, phoneNumber, null);
        client.setFirstName(firstName);
        client.setLastName(lastName);

        ProjectRepository.getInstance().saveClientDetails(projectId, client, task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "שמירת פרטי הלקוח נכשלה", Toast.LENGTH_LONG).show();
                return;
            }

            java.util.Map<String, Object> fields = new java.util.HashMap<>();
            if (!fullAddress.isEmpty()) {
                fields.put("fullAddress", fullAddress);
            }
            if (!fields.isEmpty()) {
                ProjectRepository.getInstance().updateMultipleFields(projectId, fields, t2 -> {
                    Toast.makeText(this, "פרטי הלקוח נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
                    progressHelper.moveToNextStep();
                });
            } else {
                Toast.makeText(this, "פרטי הלקוח נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
                progressHelper.moveToNextStep();
            }
        });
    }

    /**
     * הערה: הפונקציה המקורית updateClientForExistingProject הוסרה כי היא קראה ל-finish() במקום להמשיך.
     * הלוגיקה המעודכנת נמצאת ב-updateClientForExistingProjectAndContinue.
     */


    /** מצב יצירה: יוצר פרויקט חדש וממשיך לשלב הבא בזרימה */
    private void createProjectWithClientAndContinue() {
        String caseNumber   = caseNumberEditText.getText().toString().trim();
        String clientId     = clientIdEditText.getText().toString().trim();
        String firstName    = firstNameEditText.getText().toString().trim();
        String lastName     = lastNameEditText.getText().toString().trim();
        String combinedFullName = firstName + " " + lastName;
        String email        = emailEditText.getText().toString().trim();
        String phoneNumber  = phoneNumberEditText.getText().toString().trim();
        String fullAddress  = fullAddressEditText.getText().toString().trim();

        FirebaseFirestore.getInstance().collection("projects").document(caseNumber).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        caseNumberEditText.setError("מספר תיק זה כבר קיים במערכת");
                        Toast.makeText(this, "מספר תיק " + caseNumber + " כבר תפוס", Toast.LENGTH_LONG).show();
                        return;
                    }
                    doCreateProject(caseNumber, clientId, combinedFullName, firstName, lastName, email, phoneNumber, fullAddress);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בבדיקת מספר תיק", Toast.LENGTH_LONG).show());
    }

    private void doCreateProject(String caseNumber, String clientId, String combinedFullName,
                                 String firstName, String lastName, String email,
                                 String phoneNumber, String fullAddress) {
        Client client = new Client(clientId, combinedFullName, email, phoneNumber, null);
        client.setFirstName(firstName);
        client.setLastName(lastName);

        Project project = new Project(caseNumber);
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

            // מעבר למסך הבא בזרימה שלך (שלב 2)
            Intent intent = new Intent(ClientDetailsActivity.this, UploadImagesActivity.class);
            intent.putExtra("projectId", newProjectId);
            startActivity(intent);
            finish();

        });
    }
}
package com.example.finalprojectappraisal.activity.activitiesAdmin;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.admin.AdminAdapter;
import com.example.finalprojectappraisal.model.Appraiser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminListActivity extends AppCompatActivity {
    private static final String TAG = "AdminListActivity";
    private RecyclerView adminRecyclerView;
    private AdminAdapter adminAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        adminRecyclerView = findViewById(R.id.adminRecyclerView);
        adminRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adminAdapter = new AdminAdapter(new ArrayList<>(), this::removeAdmin);
        adminRecyclerView.setAdapter(adminAdapter);

        loadAdmins();
    }

    private void loadAdmins() {

        db.collection("appraisers")
                .whereEqualTo("accessPermissions", "ADMIN")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Appraiser> adminList = new ArrayList<>();
                        task.getResult().forEach(document -> {
                            String appraiserId = document.getId();
                            Appraiser appraiser = document.toObject(Appraiser.class);
                            appraiser.setAppraiserId(appraiserId); // נוודא שה-ID מוגדר
                            // נוודא שזה באמת מנהל לפני שנוסיף אותו לרשימה
                            if (appraiser.isAdmin()) {
                                adminList.add(appraiser);
                            }
                        });
                        adminAdapter.updateAdmins(adminList);
                    } else {
                        Log.e(TAG, "Failed to load admins", task.getException());
                        Toast.makeText(this, "שגיאה בטעינת הרשימה", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeAdmin(String email) {
        // נחפש את המשתמש לפי המייל כדי להשיג את ה-UID שלו
        db.collection("appraisers")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // קיבלנו את המסמך של המנהל שרוצים להסיר
                        String appraiserId = task.getResult().getDocuments().get(0).getId();
                        Appraiser appraiser = task.getResult().getDocuments().get(0).toObject(Appraiser.class);

                        // נבדוק אם זה המנהל הראשי
                        if (appraiser.isSuperAdmin()) {
                            Toast.makeText(this, "לא ניתן להסיר את המנהל הראשי", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // נעדכן את ההרשאה של המנהל ל"USER"
                        db.collection("appraisers").document(appraiserId)
                                .update("accessPermissions", Appraiser.AccessPermission.USER.toString())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "המנהל הוסר בהצלחה", Toast.LENGTH_SHORT).show();
                                    loadAdmins(); // רענון הרשימה
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error removing admin: " + e.getMessage(), e);
                                    Toast.makeText(this, "שגיאה בהסרת המנהל: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "המנהל לא נמצא", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
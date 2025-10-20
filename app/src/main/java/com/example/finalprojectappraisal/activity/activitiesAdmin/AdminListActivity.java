package com.example.finalprojectappraisal.activity.activitiesAdmin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.admin.AdminAdapter;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Appraiser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import android.app.AlertDialog;

// ייבוא קריטי עבור OnCompleteListener ו-Task:
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnCompleteListener;


public class AdminListActivity extends AppCompatActivity {
    private static final String TAG = "AdminListActivity";
    private RecyclerView adminRecyclerView;
    private AdminAdapter adminAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Button btnAddAdmin;

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

        btnAddAdmin = findViewById(R.id.btnAddAdmin);
        btnAddAdmin.setOnClickListener(v -> showAppraisersList());

        checkCurrentUserPermissions();
        loadAdmins();
    }

    private void checkCurrentUserPermissions() {
        if (mAuth.getCurrentUser() != null) {
            String currentUserId = mAuth.getCurrentUser().getUid();
            // **תיקון כאן: שימוש ב-OnCompleteListener אנונימי כדי להתאים לחתימת המתודה ב-ProjectRepository**
            ProjectRepository.getInstance().getUserPermissions(currentUserId, new OnCompleteListener<Appraiser.AccessPermission>() {
                @Override
                public void onComplete(Task<Appraiser.AccessPermission> task) {
                    if (task.isSuccessful()) {
                        // ניגשים לתוצאה מתוך ה-Task
                        Appraiser.AccessPermission permissions = task.getResult();
                        if (permissions == Appraiser.AccessPermission.SUPER_ADMIN) {
                            btnAddAdmin.setVisibility(View.VISIBLE);
                        } else {
                            btnAddAdmin.setVisibility(View.GONE);
                        }
                    } else {
                        Log.e(TAG, "Failed to get user permissions: " + task.getException());
                        btnAddAdmin.setVisibility(View.GONE); // במקרה של שגיאה, נסתר את הכפתור
                    }
                }
            });
        }
    }

    private void loadAdmins() {
        db.collection("appraisers")
                .whereEqualTo("accessPermissions", Appraiser.AccessPermission.ADMIN.toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Appraiser> adminList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Appraiser appraiser = document.toObject(Appraiser.class);
                            appraiser.setAppraiserId(document.getId());
                            adminList.add(appraiser);
                        }
                        adminAdapter.updateAdmins(adminList);
                    } else {
                        Log.e(TAG, "Failed to load admins", task.getException());
                        Toast.makeText(this, "שגיאה בטעינת הרשימה", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeAdmin(String email) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "יש להתחבר כדי לבצע פעולה זו", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("appraisers")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String appraiserId = task.getResult().getDocuments().get(0).getId();
                        Appraiser appraiser = task.getResult().getDocuments().get(0).toObject(Appraiser.class);

                        if (appraiserId.equals(currentUserId)) {
                            Toast.makeText(this, "אינך יכול להסיר את עצמך מניהול", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (appraiser.getAccessPermissions() == Appraiser.AccessPermission.SUPER_ADMIN) {
                            Toast.makeText(this, "לא ניתן להסיר את המנהל הראשי", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        db.collection("appraisers").document(appraiserId)
                                .update("accessPermissions", Appraiser.AccessPermission.USER.toString())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "המנהל הוסר בהצלחה", Toast.LENGTH_SHORT).show();
                                    loadAdmins();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error removing admin: " + e.getMessage(), e);
                                    Toast.makeText(this, "שגיאה בהסרת המנהל", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "המנהל לא נמצא", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAppraisersList() {
        db.collection("appraisers")
                .whereEqualTo("accessPermissions", Appraiser.AccessPermission.USER.toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Appraiser> usersList = task.getResult().toObjects(Appraiser.class);

                        AlertDialog.Builder builder = new AlertDialog.Builder(AdminListActivity.this);
                        builder.setTitle("בחר שמאי למנות כמנהל");

                        final String[] userEmails = usersList.stream()
                                .map(Appraiser::getEmail)
                                .toArray(String[]::new);

                        builder.setItems(userEmails, (dialog, which) -> {
                            String selectedEmail = userEmails[which];
                            makeAdmin(selectedEmail);
                        });

                        builder.show();
                    } else {
                        Toast.makeText(this, "שגיאה בטעינת רשימת השמאים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void makeAdmin(String email) {
        db.collection("appraisers")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String appraiserId = task.getResult().getDocuments().get(0).getId();

                        db.collection("appraisers").document(appraiserId)
                                .update("accessPermissions", Appraiser.AccessPermission.ADMIN.toString())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "השמאי מונה למנהל בהצלחה", Toast.LENGTH_SHORT).show();
                                    loadAdmins();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "שגיאה במינוי מנהל", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }
}
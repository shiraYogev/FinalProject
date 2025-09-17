package com.example.finalprojectappraisal.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.images.UploadImagesActivity;
import com.example.finalprojectappraisal.adapter.ProjectsAdapter;
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.model.Project;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import java.util.ArrayList;
import java.util.List;

public class MyProjectsActivity extends AppCompatActivity {

    private ProjectsAdapter adapter;
    private final List<Project> allProjects = new ArrayList<>();
    private EditText searchBar;
    private TextView txtEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_projects);

        RecyclerView recyclerView = findViewById(R.id.recyclerMyProjects);
        searchBar = findViewById(R.id.searchBar);
        txtEmpty  = findViewById(R.id.txtEmpty);

        adapter = new ProjectsAdapter(allProjects, new ProjectsAdapter.ProjectActionListener() {
            @Override
            public void onEdit(Project project) {
                Intent intent = new Intent(MyProjectsActivity.this, UploadImagesActivity.class);
                intent.putExtra("projectId", project.getProjectId());
                startActivity(intent);
            }

            @Override
            public void onImages(Project project) {
                // TODO: intent to ImagesActivity
            }

            @Override
            public void onReport(Project project) {
                // TODO: דו"ח PDF
            }

            @Override
            public void onDelete(Project project) {
                ProjectRepository.getInstance().deleteProject(project.getProjectId(), task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MyProjectsActivity.this, "הפרויקט נמחק", Toast.LENGTH_SHORT).show();
                        loadProjects();
                    } else {
                        Toast.makeText(MyProjectsActivity.this, "מחיקה נכשלה", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadProjects();

        searchBar.addTextChangedListener(new android.text.TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProjects(s == null ? "" : s.toString());
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(android.text.Editable s) {}
        });

        findViewById(R.id.btnNewProject).setOnClickListener(v -> {
            // TODO: Intent למסך יצירת פרויקט
        });
    }

    private void loadProjects() {
        String appraiserIdStr = getCurrentAppraiserIdString();
        if (appraiserIdStr == null || appraiserIdStr.trim().isEmpty()) {
            Toast.makeText(this, "לא נמצא appraiserId למשתמש הנוכחי", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ שליפה לפי activeProjects
        ProjectRepository.getInstance().loadActiveProjectsForAppraiser(appraiserIdStr);

        ProjectRepository.getInstance().getAllProjects().observe(this, projects -> {
            allProjects.clear();
            if (projects != null) allProjects.addAll(projects);
            adapter.updateData(allProjects);
            Log.d("UIUpdate", "Loaded " + allProjects.size() + " active projects for appraiser " + appraiserIdStr);
            updateEmptyState();
        });
    }


    private String getCurrentAppraiserIdString() {
        //  אם מזהה השמאי הוא פשוט ה-UID של FirebaseAuth (String) ---
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getUid() != null) {
            return user.getUid(); // <-- אם זה ה-appraiserId אצלך
        }
        return null; // אם לא נמצא
    }


    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        if (txtEmpty != null) {
            txtEmpty.setVisibility(isEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }

    private void filterProjects(String query) {
        String q = query.trim().toLowerCase();
        List<Project> filtered = new ArrayList<>();
        for (Project p : allProjects) {
            String address = safe(p.getFullAddress());
            String client  = p.getClient() != null ? safe(p.getClient().getFullName()) : "";
            String status  = safe(p.getProjectStatus());
            String note    = safe(p.getNote()); // *** חיפוש גם בהערה ***

            if (address.contains(q) || client.contains(q) || status.contains(q) || note.contains(q)) {
                filtered.add(p);
            }
        }
        adapter.updateData(filtered);
        updateEmptyState();
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase();
    }
}

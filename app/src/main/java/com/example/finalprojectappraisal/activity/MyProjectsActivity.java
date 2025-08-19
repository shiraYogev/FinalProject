package com.example.finalprojectappraisal.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.images.UploadImagesActivity;
import com.example.finalprojectappraisal.adapter.ProjectsAdapter;
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.model.Project;

import java.util.ArrayList;
import java.util.List;

public class MyProjectsActivity extends AppCompatActivity {

    private ProjectsAdapter adapter;
    private List<Project> allProjects = new ArrayList<>();
    private EditText searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_projects);

        RecyclerView recyclerView = findViewById(R.id.recyclerMyProjects);
        searchBar = findViewById(R.id.searchBar);

        adapter = new ProjectsAdapter(allProjects, new ProjectsAdapter.ProjectActionListener() {
            @Override
            public void onEdit(Project project) {
                // מעבר למסך עריכה
                Intent intent = new Intent(MyProjectsActivity.this, UploadImagesActivity.class);
                intent.putExtra("projectId", project.getProjectId());
                startActivity(intent);
            }

            @Override
            public void onImages(Project project) {
                // מעבר למסך התמונות
                // intent to ImagesActivity, וכו'
            }

            @Override
            public void onReport(Project project) {
                // דו"ח PDF, intent או פעולה אחרת
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

        // טען מה-DB
        loadProjects();

        // חיפוש בזמן אמת (לא חובה, רק דוגמה בסיסית)
        searchBar.addTextChangedListener(new android.text.TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProjects(s.toString());
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void afterTextChanged(android.text.Editable s) { }
        });

        // כפתור פרויקט חדש
        findViewById(R.id.btnNewProject).setOnClickListener(v -> {
            // Intent למסך יצירת פרויקט
        });
    }

    private void loadProjects() {
        // שליפת כל הפרויקטים (ללא סינון לפי שמאי)
        ProjectRepository.getInstance().loadAllProjects();

        // תצפית על LiveData, מתעדכן ברגע שהנתונים נשלפים
        ProjectRepository.getInstance().getAllProjects().observe(this, projects -> {
            if (projects != null) {
                allProjects.clear();
                allProjects.addAll(projects);
                adapter.updateData(allProjects);
                Log.d("UIUpdate", "Loaded " + projects.size() + " projects to UI");
            } else {
                Log.d("UIUpdate", "Projects LiveData is null");
            }
        });
    }


    private void filterProjects(String query) {
        List<Project> filtered = new ArrayList<>();
        for (Project p : allProjects) {
            if ((p.getFullAddress() != null && p.getFullAddress().contains(query)) ||
                    (p.getClient() != null && p.getClient().getFullName() != null && p.getClient().getFullName().contains(query)) ||
                    (p.getProjectStatus() != null && p.getProjectStatus().contains(query))) {
                filtered.add(p);
            }
        }
        adapter.updateData(filtered);
    }
}

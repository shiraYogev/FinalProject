package com.example.finalprojectappraisal.activity.AllProjects;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.adapter.ProjectsReadOnlyAdapter;
import com.example.finalprojectappraisal.database.ProjectRepository;
import com.example.finalprojectappraisal.model.Project;

import java.util.ArrayList;
import java.util.List;

public class AllProjectsViewActivity extends AppCompatActivity implements ProjectsReadOnlyAdapter.ViewClickListener {

    private RecyclerView rv;
    private ProgressBar progress;
    private TextView empty;
    private ProjectsReadOnlyAdapter adapter;
    private final ProjectRepository repo = ProjectRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_projects_view);
        setTitle("כל הפרויקטים (צפייה)");

        rv = findViewById(R.id.rvProjects);
        progress = findViewById(R.id.progress);
        empty = findViewById(R.id.txtEmpty);

        adapter = new ProjectsReadOnlyAdapter(new ArrayList<>(), this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        progress.setVisibility(View.VISIBLE);
        repo.loadAllProjectsWithListener(); // <-- שנה לשם המתודה הנכון
        repo.getAllProjects().observe(this, this::render);
        repo.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                empty.setText("שגיאה בטעינה: " + msg);
                progress.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void render(List<Project> items) {
        progress.setVisibility(View.GONE);
        if (items == null || items.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            adapter.submit(new ArrayList<>());
        } else {
            empty.setVisibility(View.GONE);
            adapter.submit(items);
        }
    }

    @Override
    public void onView(Project p) {
        Intent i = new Intent(this, ProjectPreviewActivity.class);
        i.putExtra("projectId", p.getProjectId());
        startActivity(i);
    }
}

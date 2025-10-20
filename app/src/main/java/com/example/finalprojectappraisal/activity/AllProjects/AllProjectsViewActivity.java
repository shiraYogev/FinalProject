/**
 * Summary:
 * Activity that displays a read-only list of projects. It delegates data loading
 * decisions to AllProjectsViewModel and supplies the current userId via AuthRepository.
 * No direct DB logic here.
 *
 * Notes:
 * - Observes VM projects LiveData (a Mediator that mirrors repository list).
 * - Admins see all projects; non-admins see only their active projects.
 */

package com.example.finalprojectappraisal.activity.AllProjects;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.AllProjects.viewmodel.AllProjectsViewModel;
import com.example.finalprojectappraisal.adapter.ProjectsReadOnlyAdapter;
import com.example.finalprojectappraisal.database.auth.AuthRepository;
import com.example.finalprojectappraisal.database.repository.ProjectRepository;
import com.example.finalprojectappraisal.model.Project;

import java.util.ArrayList;
import java.util.List;

public class AllProjectsViewActivity extends AppCompatActivity implements ProjectsReadOnlyAdapter.ViewClickListener {

    private RecyclerView rv;
    private ProgressBar progress;
    private TextView empty;

    private ProjectsReadOnlyAdapter adapter;
    private AllProjectsViewModel vm;

    private String currentUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_projects_view);

        // Status bar overlay height handling
        View overlay = findViewById(R.id.status_bar_overlay);
        if (overlay != null) {
            ViewCompat.setOnApplyWindowInsetsListener(overlay, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                overlay.getLayoutParams().height = top;
                overlay.requestLayout();
                return insets;
            });
        }

        setTitle("כל הפרויקטים (צפייה)");

        // Bind views
        rv = findViewById(R.id.rvProjects);
        progress = findViewById(R.id.progress);
        empty = findViewById(R.id.txtEmpty);

        // Adapter
        adapter = new ProjectsReadOnlyAdapter(new ArrayList<>(), this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // ViewModel
        vm = new ViewModelProvider(this).get(AllProjectsViewModel.class);

        // Observe loading
        vm.getIsLoading().observe(this, isLoading -> {
            progress.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        // Observe error (optional)
        vm.getError().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                empty.setText("שגיאה בטעינה: " + msg);
                empty.setVisibility(View.VISIBLE);
            }
        });

        // Observe projects (from VM MediatorLiveData)
        vm.getProjects().observe(this, this::render);

        // Current user via AuthRepository → initial load
        currentUserId = AuthRepository.getInstance().getCurrentUserId();
        vm.loadForUser(currentUserId);

        // React to real-time auth changes
        AuthRepository.getInstance().getUserIdLive().observe(this, uid -> {
            boolean changed = (uid == null && currentUserId != null) || (uid != null && !uid.equals(currentUserId));
            if (changed) {
                currentUserId = uid;
                ProjectRepository.getInstance().stopListening(); // reset live query
                vm.loadForUser(currentUserId);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure we stop Firestore listeners when leaving the screen
        ProjectRepository.getInstance().stopListening();
    }
}

package com.example.finalprojectappraisal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.Project;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProjectsReadOnlyAdapter extends RecyclerView.Adapter<ProjectsReadOnlyAdapter.Holder> {

    public interface ViewClickListener {
        void onView(Project p);
    }

    private final List<Project> data = new ArrayList<>();
    private final ViewClickListener listener;

    public ProjectsReadOnlyAdapter(List<Project> init, ViewClickListener listener) {
        if (init != null) data.addAll(init);
        this.listener = listener;
    }

    public void submit(List<Project> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_readonly, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        Project p = data.get(pos);
        String title = safe(p.getFullAddress(), "ללא כתובת");
        String status = safe(p.getProjectStatus(), "ללא סטטוס");
        String client = (p.getClient() != null && p.getClient().getFullName() != null)
                ? p.getClient().getFullName() : "לקוח לא ידוע";

        Long tsMillis = null;
        try {
            // prefer model helper if exists, else from Date
            if (p.getLastUpdateDate() != null) tsMillis = p.getLastUpdateDate().getTime();
            // if you have getLastUpdateDateMillis(): tsMillis = p.getLastUpdateDateMillis();
        } catch (Exception ignore) {}

        String last = (tsMillis != null)
                ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(tsMillis))
                : "—";

        h.txtTitle.setText(title);
        h.txtClient.setText("לקוח: " + client);
        h.txtStatus.setText("סטטוס: " + status);
        h.txtUpdated.setText("עודכן: " + last);

        h.btnView.setOnClickListener(v -> {
            if (listener != null) listener.onView(p);
        });
    }

    private static String safe(String s, String def) { return (s == null || s.trim().isEmpty()) ? def : s; }

    @Override
    public int getItemCount() { return data.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        CardView card;
        TextView txtTitle, txtClient, txtStatus, txtUpdated;
        Button btnView;
        Holder(@NonNull View v) {
            super(v);
            card = v.findViewById(R.id.card);
            txtTitle = v.findViewById(R.id.txtTitle);
            txtClient = v.findViewById(R.id.txtClient);
            txtStatus = v.findViewById(R.id.txtStatus);
            txtUpdated = v.findViewById(R.id.txtUpdated);
            btnView = v.findViewById(R.id.btnView);
        }
    }
}

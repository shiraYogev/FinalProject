package com.example.finalprojectappraisal.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.Appraiser;

import java.util.List;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminViewHolder> {

    private final List<Appraiser> adminList;
    private final RemoveAdminListener removeAdminListener;

    public AdminAdapter(List<Appraiser> adminList, RemoveAdminListener removeAdminListener) {
        this.adminList = adminList;
        this.removeAdminListener = removeAdminListener;
    }

    public interface RemoveAdminListener {
        void onRemove(String email);
    }

    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin, parent, false);
        return new AdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        Appraiser appraiser = adminList.get(position);
        holder.emailTextView.setText(appraiser.getEmail());

        if (appraiser.isSuperAdmin()) {
            holder.removeButton.setVisibility(View.GONE);
        } else {
            holder.removeButton.setVisibility(View.VISIBLE);
            holder.removeButton.setOnClickListener(v -> removeAdminListener.onRemove(appraiser.getEmail()));
        }
    }

    @Override
    public int getItemCount() {
        return adminList.size();
    }

    public void updateAdmins(List<Appraiser> admins) {
        this.adminList.clear();
        this.adminList.addAll(admins);
        notifyDataSetChanged();
    }

    static class AdminViewHolder extends RecyclerView.ViewHolder {
        TextView emailTextView;
        Button removeButton;

        AdminViewHolder(@NonNull View itemView) {
            super(itemView);
            emailTextView = itemView.findViewById(R.id.adminEmailTextView);
            removeButton = itemView.findViewById(R.id.removeAdminButton);
        }
    }
}
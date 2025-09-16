package com.example.finalprojectappraisal.admin;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.model.Admin;

import java.util.List;

/**
 * Adapter class for displaying the list of admin users in a RecyclerView.
 * It also provides functionality to remove an admin from the list.
 */
public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminViewHolder> {

    private final List<com.example.finalprojectappraisal.model.Admin> adminList;
    private final RemoveAdminListener removeAdminListener;

    /**
     * Constructor to initialize the adapter with a list of admins and a listener for removing an admin.
     *
     * @param adminList List of admin objects to be displayed in the RecyclerView.
     * @param removeAdminListener Listener to handle the admin removal action.
     */
    public AdminAdapter(List<com.example.finalprojectappraisal.model.Admin> adminList, RemoveAdminListener removeAdminListener) {
        this.adminList = adminList;
        this.removeAdminListener = removeAdminListener;
    }

    /**
     * Interface for removing an admin.
     */
    public interface RemoveAdminListener {
        void onRemove(String email);
    }

    /**
     * Inflates the layout for a single admin item in the RecyclerView.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The type of the view.
     * @return A new instance of AdminViewHolder containing the inflated view.
     */
    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin, parent, false);
        return new AdminViewHolder(view);
    }

    /**
     * Binds the admin data to the view elements in each item.
     * Hides the "Remove" button for super admins.
     *
     * @param holder The ViewHolder for the current item.
     * @param position The position of the current item in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        Admin admin = adminList.get(position);
        holder.emailTextView.setText(admin.getEmail());

        // Hide "Remove" button for super admin
        if (admin.isSuperAdmin()) {
            holder.removeButton.setVisibility(View.GONE);  // Super admin cannot be removed
        } else {
            holder.removeButton.setVisibility(View.VISIBLE);  // Regular admin can be removed
            holder.removeButton.setOnClickListener(v -> removeAdminListener.onRemove(admin.getEmail()));
        }
    }

    /**
     * Returns the total number of items in the admin list.
     *
     * @return The size of the admin list.
     */
    @Override
    public int getItemCount() {
        return adminList.size();
    }

    /**
     * Updates the admin list with a new list of admins and notifies the adapter of the change.
     *
     * @param admins The new list of admins.
     */
    public void updateAdmins(List<com.example.finalprojectappraisal.model.Admin> admins) {
        this.adminList.clear();
        this.adminList.addAll(admins);
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class that represents the view for a single admin item.
     */
    static class AdminViewHolder extends RecyclerView.ViewHolder {
        TextView emailTextView;
        Button removeButton;

        /**
         * Constructor to initialize the view elements for a single admin item.
         *
         * @param itemView The view for the individual item.
         */
        AdminViewHolder(@NonNull View itemView) {
            super(itemView);
            emailTextView = itemView.findViewById(R.id.adminEmailTextView);
            removeButton = itemView.findViewById(R.id.removeAdminButton);
        }
    }
}
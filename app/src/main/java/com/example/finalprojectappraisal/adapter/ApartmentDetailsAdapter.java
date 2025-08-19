package com.example.finalprojectappraisal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.property.common.forms.FormItems.FieldItem;
import com.example.finalprojectappraisal.activity.newProject.property.common.forms.FormItems.ListItem;
import com.example.finalprojectappraisal.activity.newProject.property.common.forms.FormItems.SectionItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter פשוט עם שני סוגי תצוגה: Section ו-Field.
 * Field לחיץ ופותח דיאלוג עריכה דרך ה-Listener.
 */
public class ApartmentDetailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface FieldClickListener {
        void onFieldClicked(FieldItem item);
    }

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_FIELD   = 1;

    private final List<ListItem> items = new ArrayList<>();
    private final FieldClickListener listener;

    public ApartmentDetailsAdapter(List<ListItem> data, FieldClickListener listener) {
        if (data != null) items.addAll(data);
        this.listener = listener;
    }

    public void submit(List<ListItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ListItem li = items.get(position);
        if (li instanceof SectionItem) return TYPE_SECTION;
        return TYPE_FIELD;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SECTION) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_section_header, parent, false);
            return new SectionVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_field_row, parent, false);
            return new FieldVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        if (getItemViewType(position) == TYPE_SECTION) {
            SectionItem si = (SectionItem) items.get(position);
            SectionVH vh = (SectionVH) h;
            vh.txt.setText(si.title);
        } else {
            FieldItem fi = (FieldItem) items.get(position);
            FieldVH vh = (FieldVH) h;
            vh.title.setText(fi.title);
            vh.value.setText(fi.value);
            vh.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFieldClicked(fi);
            });
            if (vh.edit != null) {
                vh.edit.setOnClickListener(v -> {
                    if (listener != null) listener.onFieldClicked(fi);
                });
            }
        }
    }

    static class SectionVH extends RecyclerView.ViewHolder {
        final TextView txt;
        SectionVH(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.txtSection);
        }
    }

    static class FieldVH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView value;
        final ImageView edit;
        FieldVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txtTitle);
            value = itemView.findViewById(R.id.txtValue);
            ImageView e = null;
            try { e = itemView.findViewById(R.id.imgEdit); } catch (Throwable ignored) {}
            edit = e;
        }
    }
}
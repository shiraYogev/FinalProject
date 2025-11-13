package com.example.finalprojectappraisal.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.newProject.property.activity.PropertyDetailsActivity;
import com.example.finalprojectappraisal.activity.newProject.property.activity.PropertyDetailsActivity.FieldItem;
import com.example.finalprojectappraisal.activity.newProject.property.activity.PropertyDetailsActivity.ListItem;
import com.example.finalprojectappraisal.activity.newProject.property.activity.PropertyDetailsActivity.SectionItem;

import java.util.ArrayList;
import java.util.List;

public class PropertyDetailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface FieldClickListener {
        void onFieldClicked(FieldItem item, int position);
    }

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_FIELD   = 1;

    private final List<ListItem> items = new ArrayList<>();
    private final FieldClickListener listener;

    public PropertyDetailsAdapter(List<ListItem> data, FieldClickListener listener) {
        if (data != null) items.addAll(data);
        this.listener = listener;
    }

    // מאפשר להחליף את כל התוכן לאחר טעינה מה-DB
    public void submit(List<ListItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) {
        return (items.get(position) instanceof SectionItem) ? TYPE_SECTION : TYPE_FIELD;
    }
    @Override public int getItemCount() { return items.size(); }

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
            ((SectionVH) h).txt.setText(si.title);
        } else {
            FieldItem fi = (FieldItem) items.get(position);
            FieldVH vh = (FieldVH) h;

            vh.title.setText(fi.title);

            String display = (fi.kind == PropertyDetailsActivity.Kind.MULTI)
                    ? (fi.multiValue == null || fi.multiValue.isEmpty()
                    ? "—"
                    : TextUtils.join(", ", fi.multiValue))
                    : (fi.value == null || fi.value.isEmpty() ? "—" : fi.value);
            vh.value.setText(display);

            View.OnClickListener click = v -> {
                int p = h.getAdapterPosition();
                if (p != RecyclerView.NO_POSITION && listener != null) {
                    ListItem li = items.get(p);
                    if (li instanceof FieldItem) listener.onFieldClicked((FieldItem) li, p);
                }
            };
            vh.itemView.setOnClickListener(click);
            if (vh.edit != null) vh.edit.setOnClickListener(click);
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
            int editRes = itemView.getResources()
                    .getIdentifier("imgEdit", "id", itemView.getContext().getPackageName());
            if (editRes != 0) e = itemView.findViewById(editRes);
            edit = e;
        }
    }
}

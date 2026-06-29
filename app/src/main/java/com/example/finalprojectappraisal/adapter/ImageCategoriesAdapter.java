// file: app/src/main/java/com/example/finalprojectappraisal/adapter/ImageCategoriesAdapter.java
package com.example.finalprojectappraisal.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.classifer.ImageCategorySection;
import com.example.finalprojectappraisal.model.Floor;
import com.example.finalprojectappraisal.model.Unit;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Property images accordion:
 *  - Group header (exterior / interior) - expandable
 *  - Inside "interior": a floor header per floor - expandable
 *  - Inside each group/floor: a horizontal carousel of mini-cards (categories)
 *
 * Tapping a mini-card is handled by the Activity (bottom sheet with the full card).
 */
public class ImageCategoriesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onMiniCardClick(@NonNull ImageCategorySection section, @Nullable Unit unit, @Nullable Floor floor);
        void onAddFloor(@NonNull Unit unit);
        void onAddUnit();
        void onRemoveUnit(@NonNull Unit unit);
    }

    private static final int TYPE_GROUP_HEADER = 0;
    private static final int TYPE_UNIT_HEADER = 1;
    private static final int TYPE_FLOOR_HEADER = 2;
    private static final int TYPE_CAROUSEL = 3;
    private static final int TYPE_ADD_FLOOR = 4;
    private static final int TYPE_ADD_UNIT = 5;

    private final List<ImageCategorySection> categories;
    private final List<Unit> units;
    private final Context context;
    private final Listener listener;

    private final EnumMap<ImageCategorySection.Group, Boolean> groupExpanded =
            new EnumMap<>(ImageCategorySection.Group.class);
    private final Map<String, Boolean> unitExpanded = new HashMap<>();
    private final Map<String, Boolean> floorExpanded = new HashMap<>();

    private final List<Row> rows = new ArrayList<>();

    private static final class Row {
        final int type;
        final ImageCategorySection.Group group;
        final Unit unit;
        final Floor floor;
        Row(int type, ImageCategorySection.Group group, Unit unit, Floor floor) {
            this.type = type; this.group = group; this.unit = unit; this.floor = floor;
        }
    }

    public ImageCategoriesAdapter(@NonNull List<ImageCategorySection> categories,
                                  @NonNull List<Unit> units,
                                  @NonNull Context context,
                                  @NonNull Listener listener) {
        this.categories = categories;
        this.units = units;
        this.context = context;
        this.listener = listener;
        groupExpanded.put(ImageCategorySection.Group.EXTERIOR, true);
        groupExpanded.put(ImageCategorySection.Group.INTERIOR, true);
        rebuildRows();
    }

    // ===== rows =====
    private void rebuildRows() {
        rows.clear();

        if (hasGroup(ImageCategorySection.Group.EXTERIOR)) {
            rows.add(new Row(TYPE_GROUP_HEADER, ImageCategorySection.Group.EXTERIOR, null, null));
            if (isGroupExpanded(ImageCategorySection.Group.EXTERIOR)) {
                rows.add(new Row(TYPE_CAROUSEL, ImageCategorySection.Group.EXTERIOR, null, null));
            }
        }

        if (hasGroup(ImageCategorySection.Group.INTERIOR)) {
            rows.add(new Row(TYPE_GROUP_HEADER, ImageCategorySection.Group.INTERIOR, null, null));
            if (isGroupExpanded(ImageCategorySection.Group.INTERIOR)) {
                boolean divided = units.size() > 1;
                for (Unit u : units) {
                    if (divided) {
                        rows.add(new Row(TYPE_UNIT_HEADER, ImageCategorySection.Group.INTERIOR, u, null));
                        if (!isUnitExpanded(u)) continue;
                    }
                    for (Floor f : u.floors) {
                        rows.add(new Row(TYPE_FLOOR_HEADER, ImageCategorySection.Group.INTERIOR, u, f));
                        if (isFloorExpanded(u, f)) {
                            rows.add(new Row(TYPE_CAROUSEL, ImageCategorySection.Group.INTERIOR, u, f));
                        }
                    }
                    rows.add(new Row(TYPE_ADD_FLOOR, ImageCategorySection.Group.INTERIOR, u, null));
                }
                rows.add(new Row(TYPE_ADD_UNIT, ImageCategorySection.Group.INTERIOR, null, null));
            }
        }
    }

    private boolean hasGroup(ImageCategorySection.Group g) {
        for (ImageCategorySection s : categories) if (s.group == g) return true;
        return false;
    }

    private boolean isGroupExpanded(ImageCategorySection.Group g) {
        return Boolean.TRUE.equals(groupExpanded.get(g));
    }

    private boolean isUnitExpanded(Unit u) {
        Boolean b = unitExpanded.get(u.key());
        return b == null || b; // default expanded
    }

    private boolean isFloorExpanded(Unit u, Floor f) {
        Boolean b = floorExpanded.get(u.key() + "/" + f.key());
        return b == null || b; // default expanded
    }

    private List<ImageCategorySection> sectionsForGroup(ImageCategorySection.Group g) {
        List<ImageCategorySection> list = new ArrayList<>();
        for (ImageCategorySection s : categories) if (s.group == g) list.add(s);
        return list;
    }

    @Override public int getItemViewType(int position) { return rows.get(position).type; }
    @Override public int getItemCount() { return rows.size(); }

    /** Rebuild grouped rows and refresh everything. */
    public void reload() {
        rebuildRows();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(context);
        switch (viewType) {
            case TYPE_GROUP_HEADER:
                return new GroupHeaderVH(inf.inflate(R.layout.item_image_group_header, parent, false));
            case TYPE_UNIT_HEADER:
                return new UnitHeaderVH(inf.inflate(R.layout.item_unit_header, parent, false));
            case TYPE_FLOOR_HEADER:
                return new FloorHeaderVH(inf.inflate(R.layout.item_floor_header, parent, false));
            case TYPE_ADD_FLOOR:
                return new AddFloorVH(inf.inflate(R.layout.item_add_floor, parent, false));
            case TYPE_ADD_UNIT:
                return new AddUnitVH(inf.inflate(R.layout.item_add_unit, parent, false));
            case TYPE_CAROUSEL:
            default:
                return new CarouselVH(inf.inflate(R.layout.item_floor_carousel, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder vh, int position) {
        Row row = rows.get(position);
        switch (row.type) {
            case TYPE_GROUP_HEADER:
                bindGroupHeader((GroupHeaderVH) vh, row.group);
                break;
            case TYPE_UNIT_HEADER:
                bindUnitHeader((UnitHeaderVH) vh, row.unit);
                break;
            case TYPE_FLOOR_HEADER:
                bindFloorHeader((FloorHeaderVH) vh, row.unit, row.floor);
                break;
            case TYPE_ADD_FLOOR:
                ((AddFloorVH) vh).btn.setOnClickListener(v -> listener.onAddFloor(row.unit));
                break;
            case TYPE_ADD_UNIT:
                ((AddUnitVH) vh).btn.setOnClickListener(v -> listener.onAddUnit());
                break;
            case TYPE_CAROUSEL:
            default:
                bindCarousel((CarouselVH) vh, row.group, row.unit, row.floor);
                break;
        }
    }

    private void bindGroupHeader(GroupHeaderVH h, ImageCategorySection.Group group) {
        boolean isExt = group == ImageCategorySection.Group.EXTERIOR;
        h.title.setText(isExt ? "תמונות חוץ" : "תמונות פנים");

        int total = 0, withImages = 0;
        for (ImageCategorySection s : categories) {
            if (s.group != group) continue;
            total++;
            if (!s.images.isEmpty()) withImages++;
        }
        h.count.setText(withImages + "/" + total);

        boolean exp = isGroupExpanded(group);
        h.chevron.setText(exp ? "\u02C5" : "\u02C4");
        h.itemView.setOnClickListener(v -> {
            groupExpanded.put(group, !isGroupExpanded(group));
            reload();
        });
    }

    private void bindUnitHeader(UnitHeaderVH h, Unit unit) {
        h.title.setText(unit.label());

        int total = 0, withImages = 0;
        for (ImageCategorySection s : categories) {
            if (s.group != ImageCategorySection.Group.INTERIOR) continue;
            total++;
            if (!MiniCardAdapter.imagesFor(s, unit, null).isEmpty()) withImages++;
        }
        h.count.setText(withImages + "/" + total);

        h.remove.setVisibility(unit.isMain() ? View.GONE : View.VISIBLE);
        h.remove.setOnClickListener(v -> listener.onRemoveUnit(unit));

        boolean exp = isUnitExpanded(unit);
        h.chevron.setText(exp ? "\u02C5" : "\u02C4");
        h.itemView.setOnClickListener(v -> {
            unitExpanded.put(unit.key(), !isUnitExpanded(unit));
            reload();
        });
    }

    private void bindFloorHeader(FloorHeaderVH h, Unit unit, Floor floor) {
        h.title.setText(floor.label());

        int total = 0, withImages = 0;
        for (ImageCategorySection s : categories) {
            if (s.group != ImageCategorySection.Group.INTERIOR) continue;
            total++;
            if (!MiniCardAdapter.imagesFor(s, unit, floor).isEmpty()) withImages++;
        }
        h.count.setText(withImages + "/" + total);

        boolean exp = isFloorExpanded(unit, floor);
        h.chevron.setText(exp ? "\u02C5" : "\u02C4");
        h.itemView.setOnClickListener(v -> {
            floorExpanded.put(unit.key() + "/" + floor.key(), !isFloorExpanded(unit, floor));
            reload();
        });
    }

    private void bindCarousel(CarouselVH h, ImageCategorySection.Group group, Unit unit, Floor floor) {
        List<ImageCategorySection> secs = sectionsForGroup(group);
        h.recycler.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        h.recycler.setAdapter(new MiniCardAdapter(secs, unit, floor, listener::onMiniCardClick));
    }

    // ===== ViewHolders =====
    static class GroupHeaderVH extends RecyclerView.ViewHolder {
        TextView title, count, chevron;
        GroupHeaderVH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.txtGroupTitle);
            count = v.findViewById(R.id.txtGroupCount);
            chevron = v.findViewById(R.id.txtGroupChevron);
        }
    }

    static class UnitHeaderVH extends RecyclerView.ViewHolder {
        TextView title, count, chevron, remove;
        UnitHeaderVH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.txtUnitTitle);
            count = v.findViewById(R.id.txtUnitCount);
            chevron = v.findViewById(R.id.txtUnitChevron);
            remove = v.findViewById(R.id.btnRemoveUnit);
        }
    }

    static class FloorHeaderVH extends RecyclerView.ViewHolder {
        TextView title, count, chevron;
        FloorHeaderVH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.txtFloorTitle);
            count = v.findViewById(R.id.txtFloorCount);
            chevron = v.findViewById(R.id.txtFloorChevron);
        }
    }

    static class CarouselVH extends RecyclerView.ViewHolder {
        RecyclerView recycler;
        CarouselVH(@NonNull View v) {
            super(v);
            recycler = (RecyclerView) v; // root view IS the RecyclerView
        }
    }

    static class AddFloorVH extends RecyclerView.ViewHolder {
        TextView btn;
        AddFloorVH(@NonNull View v) {
            super(v);
            btn = (TextView) v;
        }
    }

    static class AddUnitVH extends RecyclerView.ViewHolder {
        TextView btn;
        AddUnitVH(@NonNull View v) {
            super(v);
            btn = (TextView) v;
        }
    }
}

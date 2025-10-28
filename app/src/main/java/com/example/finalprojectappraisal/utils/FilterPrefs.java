package com.example.finalprojectappraisal.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilterPrefs {
    private static final String PREF = "project_filters_pref";
    private static final String KEY  = "last_filter";
    private static final Gson GSON = new Gson();

    // Save/load the whole filter as before
    public static void save(Context ctx, ProjectFilter f) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().putString(KEY, GSON.toJson(f)).apply();
    }

    public static ProjectFilter load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String json = sp.getString(KEY, null);
        if (json == null) return new ProjectFilter();
        try {
            return GSON.fromJson(json, ProjectFilter.class);
        } catch (Exception e) {
            return new ProjectFilter();
        }
    }

    // ---------- NEW: statuses helpers (robust, JSON-level) ----------

    /** Overwrite only the "statuses" array in saved JSON (keep all other fields as-is). */
    public static void saveStatuses(Context ctx, List<String> statuses) {
        if (statuses == null) statuses = new ArrayList<>();
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String json = sp.getString(KEY, null);

        JsonObject obj;
        if (json == null || json.trim().isEmpty()) {
            // start from an empty object; other fields will use defaults on load()
            obj = new JsonObject();
        } else {
            try {
                obj = JsonParser.parseString(json).getAsJsonObject();
            } catch (Throwable t) {
                obj = new JsonObject();
            }
        }

        JsonArray arr = new JsonArray();
        for (String s : statuses) {
            if (s != null) arr.add(s);
        }
        obj.add("statuses", arr); // <-- force write the array

        sp.edit().putString(KEY, obj.toString()).apply();
    }

    /** Save a single status into the statuses array (overwrite). */
    public static void saveSingleStatus(Context ctx, String status) {
        saveStatuses(ctx, Collections.singletonList(status == null ? "" : status));
    }

    /** Clear statuses array. */
    public static void clearStatuses(Context ctx) {
        saveStatuses(ctx, new ArrayList<>());
    }
}

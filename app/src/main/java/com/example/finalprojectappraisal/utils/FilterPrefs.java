package com.example.finalprojectappraisal.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilter;
import com.google.gson.Gson;

public class FilterPrefs {
    private static final String PREF = "project_filters_pref";
    private static final String KEY  = "last_filter";
    private static final Gson GSON = new Gson();

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
}

package com.example.finalprojectappraisal.activity.newProject.property.common.forms;

public final class FormItems {
    private FormItems() {}

    public interface ListItem {}

    public static final class SectionItem implements ListItem {
        public final String title;
        private SectionItem(String t) { this.title = t; }
        public static SectionItem of(String t) { return new SectionItem(t); }
    }

    public static final class FieldItem implements ListItem {
        public final String key;    // Firestore key (או וירטואלי)
        public final String title;  // תווית
        public final String value;  // ערך להצגה
        private FieldItem(String key, String title, String value) {
            this.key = key; this.title = title; this.value = value;
        }
        public static FieldItem of(String key, String title, String value) {
            return new FieldItem(key, title, value);
        }
    }
}

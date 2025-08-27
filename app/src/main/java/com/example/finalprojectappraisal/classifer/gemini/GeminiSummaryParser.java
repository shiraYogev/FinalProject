package com.example.finalprojectappraisal.classifer.gemini;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

public final class GeminiSummaryParser {
    private GeminiSummaryParser() {}

    public static final class Result {
        public final String[] sentences; // 3 sentences
        public final double confidence;
        public Result(String[] sentences, double confidence) {
            this.sentences = sentences;
            this.confidence = confidence;
        }
        public String toMultiline() {
            // Join as three short lines; you can also join with ". " if you prefer one paragraph
            return String.join("\n", sentences);
        }
    }

    /** Parse and validate the JSON-only response from Gemini. */
    public static Result parse(String raw) throws JSONException {
        String json = stripCodeFences(raw);
        JSONObject obj = new JSONObject(json);

        if (!obj.has("property_summary")) {
            throw new JSONException("missing property_summary");
        }
        JSONArray arr = obj.getJSONArray("property_summary");
        if (arr.length() != 3) {
            throw new JSONException("property_summary must contain exactly 3 sentences");
        }
        String[] out = new String[3];
        for (int i = 0; i < 3; i++) {
            String s = arr.optString(i, "").trim();
            s = sanitizeSentence(s);
            if (s.isEmpty()) throw new JSONException("empty sentence at index " + i);
            out[i] = capLength(s, 120); // keep it short-ish; adjust as needed
        }
        double conf = obj.optDouble("confidence", 0.0);
        return new Result(out, conf);
    }

    private static String stripCodeFences(String t) {
        if (t == null) return "";
        String s = t.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```[a-zA-Z]*\\s*", "");
            int last = s.lastIndexOf("```");
            if (last >= 0) s = s.substring(0, last).trim();
        }
        return s;
    }

    // Remove emojis and URLs; very light check for Hebrew presence
    private static final Pattern URL = Pattern.compile("(https?://|www\\.)\\S+");
    private static final Pattern EMOJI = Pattern.compile("[\\p{So}\\p{Cn}]+");

    private static String sanitizeSentence(String s) {
        s = URL.matcher(s).replaceAll("");
        s = EMOJI.matcher(s).replaceAll("");
        s = s.replaceAll("\\s{2,}", " ").trim();
        return s;
    }

    private static String capLength(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)).trim() + "…";
    }
}
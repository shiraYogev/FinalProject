package com.example.finalprojectappraisal.utils;

import android.text.TextUtils;
import java.util.regex.*;
/**
 * The Validator class is used to validate user input data. It checks if the data, such as email, password,
 * or phone number, meets the required format or criteria.
 *
 * Using this class ensures consistent validation logic across the app and keeps the code clean and organized.
 */


public class FieldValidators {
    // DD/MM/YYYY (format only)
    private static final Pattern DATE_PATTERN =
            Pattern.compile("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/([0-9]{4})$");

    public static boolean isValidDate(String s) {
        return s != null && DATE_PATTERN.matcher(s.trim()).matches();
    }

    public static boolean isNineDigits(String s) {
        return s != null && s.matches("^\\d{9}$");
    }

    // Israeli ID checksum (mod 10)
    public static boolean isValidIsraeliID(String id) {
        if (id == null) return false;
        String s = id.trim();
        if (!s.matches("^\\d{5,9}$")) return false; // allow <=9 then pad
        while (s.length() < 9) s = "0" + s;
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int c = s.charAt(i) - '0';
            int inc = c * ((i % 2) + 1);
            if (inc > 9) inc -= 9;
            sum += inc;
        }
        return sum % 10 == 0;
    }
}
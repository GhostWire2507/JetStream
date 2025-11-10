package com.jetstream.utils;

/**
 * Static input validator utility.
 */
public class Validator {

    public static boolean isNotEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Add more validation helpers as needed
}

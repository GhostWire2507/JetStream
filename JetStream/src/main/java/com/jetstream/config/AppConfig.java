package com.jetstream.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration from config.properties.
 */
public class AppConfig {
    private static Properties props = new Properties();

    public static void load() {
        try (InputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        } catch (Exception e) {
            System.err.println("Could not load config.properties: " + e.getMessage());
            // continue with defaults
        }
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}

package com.jetstream.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration from config.properties in classpath (src/main/resources).
 */
public class AppConfig {
    private static Properties props = new Properties();

    public static void load() {
        try {
            // Load from classpath (src/main/resources/config.properties)
            InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("config.properties");
            if (in == null) {
                System.err.println("ERROR: Could not find config.properties in classpath");
                System.err.println("Make sure config.properties exists in src/main/resources/");
                return;
            }
            props.load(in);
            in.close();
            System.out.println("✓ Configuration loaded successfully from classpath");
        } catch (Exception e) {
            System.err.println("✗ Could not load config.properties: " + e.getMessage());
            e.printStackTrace();
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

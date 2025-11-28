package com.jetstream.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Simple test to verify Render PostgreSQL connection
 * Run this to test your connection before using in the application
 */
public class RenderConnectionTest {

    public static void main(String[] args) {
        System.out.println("=== Testing Render PostgreSQL Connection ===\n");

        try {
            // Load config from direct file path
            Properties prop = new Properties();
            String configPath = "c:/Users/dell/IdeaProjects/JetStream/config.properties";
            
            if (Files.exists(Paths.get(configPath))) {
                prop.load(Files.newInputStream(Paths.get(configPath)));
                System.out.println("✓ Loaded config from: " + configPath);
            } else {
                System.err.println("ERROR: config.properties not found at: " + configPath);
                return;
            }

            String url = prop.getProperty("db.url");
            String user = prop.getProperty("db.user");
            String password = prop.getProperty("db.password");

            System.out.println("\nConfiguration loaded:");
            System.out.println("  URL: " + url);
            System.out.println("  User: " + user);
            System.out.println("  Password: " + (password != null && !password.isEmpty() ? "***" : "EMPTY"));
            System.out.println();

            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");
            System.out.println("✓ PostgreSQL driver loaded");

            // Test connection
            System.out.println("\nAttempting connection...");
            Connection conn = DriverManager.getConnection(url, user, password);

            if (conn != null && !conn.isClosed()) {
                System.out.println("✓ Connection successful!\n");

                // Test query
                System.out.println("Testing query...");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1 as test");
                if (rs.next()) {
                    System.out.println("✓ Query successful! Test value: " + rs.getInt("test"));
                }
                rs.close();
                stmt.close();

                System.out.println("\n✓✓✓ All tests passed! Your Render database is ready to use. ✓✓✓");
                conn.close();
            } else {
                System.err.println("✗ Connection failed!");
            }

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

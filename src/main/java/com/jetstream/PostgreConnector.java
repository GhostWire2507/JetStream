package com.jetstream.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

public class PostgreConnector {

    private static Connection connection = null;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;

    public static Connection getConnection() {
        // Check if existing connection is still valid
        if (connection != null) {
            try {
                if (!connection.isClosed() && connection.isValid(5)) {
                    return connection;
                }
            } catch (SQLException e) {
                System.err.println("Existing connection is invalid: " + e.getMessage());
                connection = null;
            }
        }

        // Try to establish new connection with retries
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                System.out.println("PostgreSQL connection attempt " + attempt + " of " + MAX_RETRIES + "...");

                // Load properties from classpath (src/main/resources/config.properties)
                Properties prop = new Properties();
                InputStream input = PostgreConnector.class.getClassLoader().getResourceAsStream("config.properties");
                if (input == null) {
                    System.err.println("ERROR: Unable to find config.properties in classpath");
                    return null;
                }
                prop.load(input);

                // Get properties
                String url = prop.getProperty("db.url");
                String user = prop.getProperty("db.user");
                String password = prop.getProperty("db.password");

                // Validate properties
                if (url == null || user == null || password == null) {
                    System.err.println("ERROR: Missing database configuration in config.properties");
                    System.err.println("  db.url: " + (url != null ? "OK" : "MISSING"));
                    System.err.println("  db.user: " + (user != null ? "OK" : "MISSING"));
                    System.err.println("  db.password: " + (password != null ? "OK" : "MISSING"));
                    return null;
                }

                System.out.println("Connecting to PostgreSQL at: " + url.replaceAll("\\?.*", ""));

                // Load PostgreSQL driver explicitly
                try {
                    Class.forName("org.postgresql.Driver");
                } catch (ClassNotFoundException e) {
                    System.err.println("ERROR: PostgreSQL JDBC driver not found!");
                    System.err.println("Make sure postgresql dependency is in pom.xml");
                    return null;
                }

                // Establish connection
                connection = DriverManager.getConnection(url, user, password);
                System.out.println("✓ PostgreSQL connection established successfully!");
                input.close();
                return connection;

            } catch (SQLException e) {
                System.err.println("SQL Exception on attempt " + attempt + ": " + e.getMessage());
                System.err.println("  SQLState: " + e.getSQLState());
                System.err.println("  Error Code: " + e.getErrorCode());

                if (attempt < MAX_RETRIES) {
                    System.out.println("Retrying in " + RETRY_DELAY_MS + "ms...");
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.err.println("Failed to connect after " + MAX_RETRIES + " attempts");
                    System.err.println("\nTroubleshooting tips:");
                    System.err.println("1. Check if the Render PostgreSQL hostname is correct");
                    System.err.println("2. Verify your IP is whitelisted in Render dashboard");
                    System.err.println("3. Ensure the database is running and accessible");
                    System.err.println("4. Try different region URLs in config.properties");
                }
            } catch (IOException e) {
                System.err.println("IO Exception: " + e.getMessage());
                return null;
            }
        }

        return null;
    }

    // Method to close connection
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("PostgreSQL connection closed.");
                connection = null;
            } catch (SQLException e) {
                System.err.println("Failed to close PostgreSQL connection: " + e.getMessage());
            }
        }
    }
}

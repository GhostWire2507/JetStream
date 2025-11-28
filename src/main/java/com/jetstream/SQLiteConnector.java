package com.jetstream.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SQLite connector for backup database functionality
 */
public class SQLiteConnector {

    private static Connection connection = null;
    private static final int MAX_RETRIES = 10;
    private static final int RETRY_DELAY_MS = 2000;

    public static Connection getConnection() {
        closeConnection(); // ensure previous connection properly closed before opening anew

        // Try to establish new connection with retries
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                System.out.println("SQLite connection attempt " + attempt + " of " + MAX_RETRIES + "...");

                // Load properties from classpath (src/main/resources/config.properties)
                Properties prop = new Properties();
                InputStream input = SQLiteConnector.class.getClassLoader().getResourceAsStream("config.properties");
                if (input == null) {
                    System.err.println("ERROR: Unable to find config.properties in classpath");
                    return null;
                }
                prop.load(input);

                // Get SQLite properties

                String url = prop.getProperty("sqlite.url");
                if (url == null || url.isEmpty()) {
                    String dbPath = prop.getProperty("db.sqlite.path");
                    if (dbPath != null && !dbPath.isEmpty()) {
                        url = "jdbc:sqlite:" + dbPath;
                    }
                }
                String user = prop.getProperty("sqlite.user");
                String password = prop.getProperty("sqlite.password");

                // Validate properties
                if (url == null) {
                    System.err.println("ERROR: Missing SQLite URL in config.properties");
                    return null;
                }

                // Extract database file path from URL (jdbc:sqlite:filename.db)
                String dbPath = url.substring("jdbc:sqlite:".length());
                int qidx = dbPath.indexOf('?');
                String dbFileOnly = (qidx >= 0) ? dbPath.substring(0, qidx) : dbPath;
                Path dbFilePath = Paths.get(dbFileOnly);

                // Ensure the directory exists
                try {
                    Files.createDirectories(dbFilePath.getParent());
                } catch (Exception e) {
                    System.err.println("Warning: Could not create SQLite database directory: " + e.getMessage());
                }

                System.out.println("Connecting to SQLite at: " + dbFileOnly);

                // Load SQLite driver explicitly
                try {
                    Class.forName("org.sqlite.JDBC");
                } catch (ClassNotFoundException e) {
                    System.err.println("ERROR: SQLite JDBC driver not found!");
                    System.err.println("Make sure sqlite-jdbc dependency is in pom.xml");
                    return null;
                }

                // Ensure JDBC URL contains useful params to reduce locking issues
                // NOTE: For sqlite-jdbc on Windows, query parameters must be passed
                // using the URI form (file:...) otherwise the driver treats the
                // entire string (including '?') as a filename which breaks.
                String baseFilePart = dbFileOnly; // absolute file path without params

                // collect any existing params from the configured URL
                String existingParams = "";
                int origQ = url.indexOf('?');
                if (origQ >= 0 && origQ + 1 < url.length()) {
                    existingParams = url.substring(origQ + 1);
                }

                StringBuilder paramsBuilder = new StringBuilder();
                if (existingParams != null && !existingParams.isEmpty()) {
                    paramsBuilder.append(existingParams);
                }
                if (paramsBuilder.indexOf("busy_timeout=") < 0) {
                    if (paramsBuilder.length() > 0) paramsBuilder.append('&');
                    paramsBuilder.append("busy_timeout=30000");
                }
                if (paramsBuilder.toString().toLowerCase().indexOf("timeout=") < 0) {
                    if (paramsBuilder.length() > 0) paramsBuilder.append('&');
                    paramsBuilder.append("timeout=30000");
                }
                if (paramsBuilder.toString().toLowerCase().indexOf("journal_mode") < 0) {
                    if (paramsBuilder.length() > 0) paramsBuilder.append('&');
                    paramsBuilder.append("journal_mode=WAL");
                }
                if (paramsBuilder.toString().toLowerCase().indexOf("cache=") < 0) {
                    if (paramsBuilder.length() > 0) paramsBuilder.append('&');
                    paramsBuilder.append("cache=shared");
                }

                String finalParams = paramsBuilder.toString();

                // Build the final JDBC URL. Use the URI form (file:) so sqlite-jdbc
                // correctly parses query parameters on Windows instead of mixing them
                // into the filename.
                String urlWithParams;
                if (finalParams.isEmpty()) {
                    // no params, use simple jdbc:sqlite:FILE
                    urlWithParams = "jdbc:sqlite:" + baseFilePart;
                } else {
                    // use URI form
                    // avoid double 'file:' if user already supplied it
                    String filePrefix = baseFilePart.startsWith("file:") ? "" : "file:";
                    urlWithParams = "jdbc:sqlite:" + filePrefix + baseFilePart + "?" + finalParams;
                }

                // Log final JDBC URL (file part only) to help debugging locking/timeouts
                System.out.println("Using JDBC URL: " + urlWithParams);

                // Establish connection (SQLite doesn't use username/password typically)
                if (user != null && !user.trim().isEmpty() && password != null && !password.trim().isEmpty()) {
                    connection = DriverManager.getConnection(urlWithParams, user, password);
                } else {
                    connection = DriverManager.getConnection(urlWithParams);
                }

                // Enable foreign keys and other SQLite optimizations
                // Apply PRAGMA settings using try-with-resources so statements close quickly
                try (java.sql.Statement pragma = connection.createStatement()) {
                    try {
                        pragma.execute("PRAGMA busy_timeout = 30000;");
                    } catch (SQLException ignored) {}
                    try { pragma.execute("PRAGMA foreign_keys = ON;"); } catch (SQLException ignored) {}
                    try { pragma.execute("PRAGMA journal_mode = WAL;"); } catch (SQLException ignored) {}
                    try { pragma.execute("PRAGMA synchronous = NORMAL;"); } catch (SQLException ignored) {}
                }

                System.out.println("✓ SQLite connection established successfully!");
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
                    System.err.println("1. Check if the SQLite database file path is writable");
                    System.err.println("2. Ensure sqlite-jdbc dependency is in pom.xml");
                    System.err.println("3. Verify config.properties has correct sqlite.url");
                }
            } catch (IOException e) {
                System.err.println("IO Exception: " + e.getMessage());
                return null;
            }
        }

        return null;
    }

    /**
     * Close the SQLite connection
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("SQLite connection closed.");
                connection = null;
            } catch (SQLException e) {
                System.err.println("Failed to close SQLite connection: " + e.getMessage());
            }
        }
    }

    /**
     * Check if SQLite database file exists
     */
    public static boolean databaseExists() {
        try {
            Properties prop = new Properties();
            InputStream input = SQLiteConnector.class.getClassLoader().getResourceAsStream("config.properties");
            if (input == null) return false;

            prop.load(input);
            String url = prop.getProperty("sqlite.url");
            input.close();

            if (url == null) return false;

            String dbPath = url.replace("jdbc:sqlite:", "");
            if (dbPath.startsWith("file:")) dbPath = dbPath.substring("file:".length());
            return Files.exists(Paths.get(dbPath));
        } catch (Exception e) {
            return false;
        }
    }
}

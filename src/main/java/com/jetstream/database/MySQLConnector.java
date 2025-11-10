package com.jetstream.database;

import com.jetstream.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLConnector {

    private static Connection conn;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;

    public static void connect() throws Exception {
        String url = AppConfig.get("mysql.url");
        String user = AppConfig.get("mysql.user");
        String pass = AppConfig.get("mysql.password");

        if (url == null || user == null) {
            throw new Exception("MySQL config missing in config.properties");
        }

        // Try to connect with retries
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                System.out.println("MySQL connection attempt " + attempt + " of " + MAX_RETRIES + "...");
                System.out.println("Connecting to MySQL at: " + url.replaceAll("\\?.*", ""));

                // Load MySQL driver explicitly
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    System.err.println("ERROR: MySQL JDBC driver not found!");
                    System.err.println("Make sure mysql-connector-j dependency is in pom.xml");
                    throw new Exception("MySQL driver not found", e);
                }

                conn = DriverManager.getConnection(url, user, pass);
                System.out.println("✓ MySQL connection established successfully!");
                return;

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
                    System.err.println("Failed to connect to MySQL after " + MAX_RETRIES + " attempts");
                    System.err.println("\nTroubleshooting tips:");
                    System.err.println("1. Check if MySQL server is running locally");
                    System.err.println("2. Verify username and password in config.properties");
                    System.err.println("3. Ensure database 'jetstream' exists");
                    System.err.println("4. Check MySQL is listening on port 3306");
                    throw new Exception("Failed to connect to MySQL", e);
                }
            }
        }
    }

    public static Connection getConnection() {
        // Validate connection before returning
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(5)) {
                    return conn;
                }
            } catch (SQLException e) {
                System.err.println("MySQL connection is invalid: " + e.getMessage());
                conn = null;
            }
        }
        return conn;
    }

    public static void close() {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("MySQL connection closed.");
                conn = null;
            } catch (SQLException e) {
                System.err.println("Failed to close MySQL connection: " + e.getMessage());
            }
        }
    }
}

package com.jetstream.database;

import com.jetstream.application.HelloApplication;
import com.jetstream.config.AppConfig;

import java.sql.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Database manager supporting dual DB storage (MySQL + PostgreSQL)
 */
public class DatabaseConnection {

    private static Connection mysqlConn;
    private static Connection pgConn;

    private static final Logger logger = Logger.getLogger("JetStreamLog");

    static {
        try {
            FileHandler fh = new FileHandler("jetstream.log", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
        } catch (Exception ignored) {}
    }

    public static void init() {
        System.out.println("=== Initializing Database Connections ===");

        // Try MySQL connection
        try {
            MySQLConnector.connect();
            mysqlConn = MySQLConnector.getConnection();
            if (mysqlConn != null) {
                logger.info("✓ Connected to MySQL.");
                System.out.println("✓ MySQL connection successful");
            } else {
                logger.warning("MySQL connection is null");
                System.err.println("⚠ MySQL connection failed - connection is null");
            }
        } catch (Exception e) {
            logger.warning("MySQL connection failed: " + e.getMessage());
            System.err.println("⚠ MySQL connection failed: " + e.getMessage());
            System.err.println("  Continuing with PostgreSQL only...");
        }

        // Try PostgreSQL connection
        try {
            pgConn = PostgreConnector.getConnection();
            if (pgConn != null) {
                logger.info("✓ Connected to PostgreSQL.");
                System.out.println("✓ PostgreSQL connection successful");
            } else {
                logger.severe("PostgreSQL connection is null");
                System.err.println("✗ PostgreSQL connection failed - connection is null");
            }
        } catch (Exception e) {
            logger.severe("PostgreSQL connection failed: " + e.getMessage());
            System.err.println("✗ PostgreSQL connection failed: " + e.getMessage());
        }

        // Check if at least one connection succeeded
        if (mysqlConn == null && pgConn == null) {
            String errorMsg = "Failed to connect to both MySQL and PostgreSQL databases.\n" +
                            "Please check your database configuration and network connectivity.";
            logger.severe("DB init failed: No database connections available");
            System.err.println("\n✗✗✗ CRITICAL: No database connections available ✗✗✗\n");
            HelloApplication.showAlert("Database Error", errorMsg);
        } else {
            System.out.println("=== Database Initialization Complete ===\n");
        }
    }

    public static Connection getMySQL() { return mysqlConn; }
    public static Connection getPostgres() { return pgConn; }

    /**
     * Execute SELECT query - tries PostgreSQL first, falls back to MySQL
     */
    public static ResultSet executeQuery(String sql) {
        Connection conn = pgConn != null ? pgConn : mysqlConn;

        if (conn == null) {
            logger.severe("Query failed: No database connection available");
            System.err.println("✗ Cannot execute query: No database connection");
            HelloApplication.showAlert("Database Error", "No database connection available");
            return null;
        }

        try {
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            logger.severe("Query failed: " + e.getMessage());
            System.err.println("✗ Query failed: " + e.getMessage());
            System.err.println("  SQL: " + sql);
            HelloApplication.showAlert("Database Error", "Query failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Execute INSERT/UPDATE/DELETE - tries to update both databases if available
     */
    public static int executeUpdate(String sql) {
        if (mysqlConn == null && pgConn == null) {
            logger.severe("Update failed: No database connection available");
            System.err.println("✗ Cannot execute update: No database connection");
            HelloApplication.showAlert("Database Error", "No database connection available");
            return -1;
        }

        int affected = -1;
        boolean success = false;

        // Try MySQL first if available
        if (mysqlConn != null) {
            try {
                Statement stmtMySQL = mysqlConn.createStatement();
                affected = stmtMySQL.executeUpdate(sql);
                success = true;
                System.out.println("✓ MySQL update successful: " + affected + " rows affected");
            } catch (SQLException e) {
                logger.warning("MySQL update failed: " + e.getMessage());
                System.err.println("⚠ MySQL update failed: " + e.getMessage());
            }
        }

        // Try PostgreSQL if available
        if (pgConn != null) {
            try {
                Statement stmtPG = pgConn.createStatement();
                int pgAffected = stmtPG.executeUpdate(sql);
                if (affected == -1) affected = pgAffected;
                success = true;
                System.out.println("✓ PostgreSQL update successful: " + pgAffected + " rows affected");
            } catch (SQLException e) {
                logger.warning("PostgreSQL update failed: " + e.getMessage());
                System.err.println("⚠ PostgreSQL update failed: " + e.getMessage());
            }
        }

        if (!success) {
            logger.severe("Update failed on all databases");
            System.err.println("✗ Update failed: " + sql);
            HelloApplication.showAlert("Database Error", "Update failed on all databases");
            return -1;
        }

        return affected;
    }

    public static void close() {
        try {
            if (mysqlConn != null) mysqlConn.close();
            if (pgConn != null) pgConn.close();
            logger.info("DB connections closed.");
        } catch (SQLException e) {
            logger.severe("Error closing DB: " + e.getMessage());
        }
    }
}

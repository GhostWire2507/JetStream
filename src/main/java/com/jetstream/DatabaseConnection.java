package com.jetstream.database;

import com.jetstream.application.HelloApplication;

import java.sql.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Database manager with dual-write support for SQLite (primary) and PostgreSQL (secondary).
 */
public class DatabaseConnection {
    private static Connection dbConn;
    private static final Logger logger = Logger.getLogger("JetStreamLog");
    private static final String DB_ERROR = "Database Error";

    static {
        try {
            FileHandler fh = new FileHandler("jetstream.log", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
        } catch (Exception ignored) {}
    }

    public static void init() {
        logger.info("=== Initializing Database Connection ===");
        java.util.Properties prop = new java.util.Properties();
        try (java.io.InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.severe("Unable to find config.properties in classpath");
                HelloApplication.showAlert(DB_ERROR, "Missing config.properties");
                return;
            }
            prop.load(input);
            String dbType = prop.getProperty("db.type", "sqlite").trim().toLowerCase();
            if ("sqlite".equals(dbType)) {
                dbConn = com.jetstream.database.SQLiteConnector.getConnection();
                if (dbConn != null) {
                    logger.info("\u2713 Connected to SQLite database (primary)");
                } else {
                    String errorMsg = "Failed to connect to SQLite database.\nPlease check your SQLite file path.";
                    logger.severe("Database connection failed");
                    HelloApplication.showAlert(DB_ERROR, errorMsg);
                }
            } else if ("postgres".equals(dbType)) {
                dbConn = com.jetstream.database.PostgreConnector.getConnection();
                if (dbConn != null) {
                    logger.info("\u2713 Connected to PostgreSQL database");
                } else {
                    String errorMsg = "Failed to connect to PostgreSQL database.\nPlease check your connection credentials.";
                    logger.severe("Database connection failed");
                    HelloApplication.showAlert(DB_ERROR, errorMsg);
                }
            } else {
                logger.severe(String.format("Unknown db.type in config.properties: %s", dbType));
                HelloApplication.showAlert(DB_ERROR, "Unknown db.type: " + dbType);
            }

            // Initialize PostgreSQL dual-write if enabled
            PostgreSQLConfig.init();
            if (PostgreSQLConfig.isEnabled()) {
                logger.info("\u2713 PostgreSQL dual-write enabled");
            }
        } catch (Exception e) {
            logger.severe("Connection error: " + e.getMessage());
            HelloApplication.showAlert(DB_ERROR, "Connection error: " + e.getMessage());
        }
    }

    public static Connection getConnection() {
        return dbConn;
    }

    public static boolean isAvailable() {
        return dbConn != null;
    }

    public static ResultSet executeQuery(String sql) {
        if (dbConn == null) {
            logger.severe("Query failed: No database connection");
            HelloApplication.showAlert(DB_ERROR, "No database connection available");
            return null;
        }

        try {
            Statement stmt = dbConn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            logger.severe("Query failed: " + e.getMessage());
            HelloApplication.showAlert(DB_ERROR, "Query failed: " + e.getMessage());
            return null;
        }
    }

    public static int executeUpdate(String sql) {
        if (dbConn == null) {
            logger.severe("Update failed: No database connection");
            HelloApplication.showAlert(DB_ERROR, "No database connection available");
            return -1;
        }

        try {
            Statement stmt = dbConn.createStatement();
            int affected = stmt.executeUpdate(sql);
            logger.info(String.format("Update successful: %d rows affected", affected));
            return affected;
        } catch (SQLException e) {
            logger.severe("Update failed: " + e.getMessage());
            HelloApplication.showAlert(DB_ERROR, "Update failed: " + e.getMessage());
            return -1;
        }
    }

    public static void close() {
        try {
            if (dbConn != null && !dbConn.isClosed()) {
                dbConn.close();
                dbConn = null;
                logger.info("SQLite connection closed");
            }
        } catch (SQLException e) {
            logger.severe("Error closing SQLite connection: " + e.getMessage());
        }

        // Close PostgreSQL connection pool
        PostgreSQLConfig.close();
        PostgreSQLService.shutdown();
        DatabaseSyncService.shutdown();
        logger.info("All database connections closed");
    }

    /**
     * Check if dual-write mode is active
     */
    public static boolean isDualWriteEnabled() {
        return PostgreSQLConfig.isEnabled();
    }

    /**
     * Trigger a sync from SQLite to PostgreSQL
     */
    public static DatabaseSyncService.SyncResult syncToPostgreSQL() {
        return DatabaseSyncService.syncAllData();
    }
}

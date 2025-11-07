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
        try {
            MySQLConnector.connect();
            mysqlConn = MySQLConnector.getConnection();
            logger.info("Connected to MySQL.");


            pgConn = PostgreConnector.getConnection();
            logger.info("Connected to PostgreSQL.");
        } catch (Exception e) {
            logger.severe("DB init failed: " + e.getMessage());
            HelloApplication.showAlert("Database Error", "Could not initialize databases: " + e.getMessage());
        }
    }

    public static Connection getMySQL() { return mysqlConn; }
    public static Connection getPostgres() { return pgConn; }

    /**
     * Execute SELECT on MySQL (you can change to Postgres if needed)
     */
    public static ResultSet executeQuery(String sql) {
        try {
            Statement stmt = mysqlConn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            logger.severe("Query failed: " + e.getMessage());
            HelloApplication.showAlert("Database Error", e.getMessage());
            return null;
        }
    }

    /**
     * Execute INSERT/UPDATE/DELETE on BOTH databases
     */
    public static int executeUpdate(String sql) {
        int affected = -1;

        try {
            Statement stmtMySQL = mysqlConn.createStatement();
            affected = stmtMySQL.executeUpdate(sql);

            Statement stmtPG = pgConn.createStatement();
            stmtPG.executeUpdate(sql);

        } catch (SQLException e) {
            logger.severe("Update failed: " + e.getMessage());
            HelloApplication.showAlert("Database Error", e.getMessage());
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

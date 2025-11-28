package com.jetstream.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Dual Write Service - Coordinates writes to both SQLite (primary) and PostgreSQL (secondary).
 * 
 * Design principles:
 * - SQLite is always the source of truth
 * - PostgreSQL writes are best-effort and non-blocking
 * - If PostgreSQL fails, log error but continue with SQLite success
 * - Supports async mode to avoid blocking UI
 */
public class DualWriteService {

    private static final Logger logger = Logger.getLogger(DualWriteService.class.getName());

    /**
     * Execute an update on both databases.
     * SQLite is primary (must succeed), PostgreSQL is secondary (best-effort).
     * 
     * @param sql The SQL statement to execute
     * @return Number of rows affected in SQLite, or -1 on failure
     */
    public static int executeUpdate(String sql) {
        // First, write to SQLite (primary)
        Connection sqliteConn = DatabaseConnection.getConnection();
        if (sqliteConn == null) {
            logger.severe("SQLite not available for update");
            return -1;
        }

        int sqliteResult;
        try {
            Statement stmt = sqliteConn.createStatement();
            sqliteResult = stmt.executeUpdate(sql);
            logger.fine("SQLite update successful: " + sqliteResult + " rows");
        } catch (SQLException e) {
            logger.severe("SQLite update failed: " + e.getMessage());
            return -1;
        }

        // Then, write to PostgreSQL (secondary, non-blocking if async enabled)
        if (PostgreSQLConfig.isEnabled()) {
            writeToPostgreSQL(sql);
        }

        return sqliteResult;
    }

    /**
     * Execute a prepared statement update on both databases.
     * 
     * @param sql The SQL statement with placeholders
     * @param params The parameters to bind
     * @return Number of rows affected in SQLite, or -1 on failure
     */
    public static int executeUpdatePrepared(String sql, Object... params) {
        // First, write to SQLite (primary)
        Connection sqliteConn = DatabaseConnection.getConnection();
        if (sqliteConn == null) {
            logger.severe("SQLite not available for prepared update");
            return -1;
        }

        int sqliteResult;
        try (PreparedStatement stmt = sqliteConn.prepareStatement(sql)) {
            setParameters(stmt, params);
            sqliteResult = stmt.executeUpdate();
            logger.fine("SQLite prepared update successful: " + sqliteResult + " rows");
        } catch (SQLException e) {
            logger.severe("SQLite prepared update failed: " + e.getMessage());
            return -1;
        }

        // Then, write to PostgreSQL (secondary)
        if (PostgreSQLConfig.isEnabled()) {
            writeToPostgreSQLPrepared(sql, params);
        }

        return sqliteResult;
    }

    /**
     * Execute a query on SQLite (reads always go to primary)
     */
    public static ResultSet executeQuery(String sql) {
        return DatabaseConnection.executeQuery(sql);
    }

    /**
     * Execute a prepared query on SQLite
     */
    public static ResultSet executeQueryPrepared(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        setParameters(stmt, params);
        return stmt.executeQuery();
    }

    /**
     * Write to PostgreSQL - async or sync based on config
     */
    private static void writeToPostgreSQL(String sql) {
        if (PostgreSQLConfig.isAsyncWritesEnabled()) {
            PostgreSQLService.executeUpdateAsync(sql)
                .thenAccept(result -> {
                    if (result < 0) {
                        logger.warning("PostgreSQL async write failed for: " + truncateSql(sql));
                    }
                });
        } else {
            int result = PostgreSQLService.executeUpdate(sql);
            if (result < 0) {
                logger.warning("PostgreSQL sync write failed for: " + truncateSql(sql));
            }
        }
    }

    /**
     * Write to PostgreSQL with prepared statement - async or sync based on config
     */
    private static void writeToPostgreSQLPrepared(String sql, Object... params) {
        if (PostgreSQLConfig.isAsyncWritesEnabled()) {
            PostgreSQLService.executeUpdatePreparedAsync(sql, params)
                .thenAccept(result -> {
                    if (result < 0) {
                        logger.warning("PostgreSQL async prepared write failed for: " + truncateSql(sql));
                    }
                });
        } else {
            int result = PostgreSQLService.executeUpdatePrepared(sql, params);
            if (result < 0) {
                logger.warning("PostgreSQL sync prepared write failed for: " + truncateSql(sql));
            }
        }
    }

    /**
     * Helper to set prepared statement parameters
     */
    private static void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    /**
     * Truncate SQL for logging (avoid logging full statements)
     */
    private static String truncateSql(String sql) {
        return sql.length() > 100 ? sql.substring(0, 100) + "..." : sql;
    }

    /**
     * Check if dual-write mode is active
     */
    public static boolean isDualWriteActive() {
        return PostgreSQLConfig.isEnabled();
    }
}


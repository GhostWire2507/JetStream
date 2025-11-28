package com.jetstream.services;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.database.PostgreSQLConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service for dual-write operations to both SQLite (primary) and PostgreSQL (secondary)
 * Ensures data consistency across both databases
 */
public class DualWriteService {

    private static final Logger logger = Logger.getLogger(DualWriteService.class.getName());

    private static DualWriteService instance;

    private DualWriteService() {
        logger.info("Dual-write service initialized. PostgreSQL enabled: " + PostgreSQLConfig.isEnabled());
    }

    public static DualWriteService getInstance() {
        if (instance == null) {
            instance = new DualWriteService();
        }
        return instance;
    }

    /**
     * Execute a write operation on both databases
     * Primary: SQLite (must succeed)
     * Secondary: PostgreSQL (failure logged but doesn't fail the operation)
     */
    public boolean executeDualWrite(String sqliteQuery, String postgresQuery, Object... params) {
        return executeDualWrite(sqliteQuery, postgresQuery, false, params);
    }

    /**
     * Execute a write operation on both databases with async option
     */
    public boolean executeDualWrite(String sqliteQuery, String postgresQuery, boolean asyncPostgres, Object... params) {
        // Always execute on SQLite first (primary database)
        boolean sqliteSuccess = executeOnSQLite(sqliteQuery, params);

        if (!sqliteSuccess) {
            logger.severe("Primary SQLite operation failed. Aborting dual-write.");
            return false;
        }

        // Execute on PostgreSQL if enabled
        if (PostgreSQLConfig.isEnabled() && postgresQuery != null) {
            if (asyncPostgres) {
                // Execute asynchronously to avoid blocking UI
                CompletableFuture.runAsync(() -> executeOnPostgreSQL(postgresQuery, params));
            } else {
                // Execute synchronously
                executeOnPostgreSQL(postgresQuery, params);
            }
        }

        return true;
    }

    /**
     * Execute query on SQLite (primary database)
     */
    private boolean executeOnSQLite(String query, Object... params) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                logger.severe("Failed to get SQLite connection");
                return false;
            }

            stmt = conn.prepareStatement(query);
            setParameters(stmt, params);

            boolean isSelect = query.trim().toUpperCase().startsWith("SELECT");
            if (isSelect) {
                ResultSet rs = stmt.executeQuery();
                // For SELECT queries, we just check if execution succeeded
                rs.close();
            } else {
                stmt.executeUpdate();
            }

            logger.fine("SQLite operation successful: " + query.substring(0, Math.min(50, query.length())));
            return true;

        } catch (SQLException e) {
            logger.severe("SQLite operation failed: " + e.getMessage());
            logger.severe("Query: " + query);
            return false;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    /**
     * Execute query on PostgreSQL (secondary database)
     */
    private void executeOnPostgreSQL(String query, Object... params) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = PostgreSQLConfig.getConnection();
            if (conn == null) {
                logger.warning("Failed to get PostgreSQL connection for dual-write");
                return;
            }

            stmt = conn.prepareStatement(query);
            setParameters(stmt, params);

            boolean isSelect = query.trim().toUpperCase().startsWith("SELECT");
            if (isSelect) {
                ResultSet rs = stmt.executeQuery();
                rs.close();
            } else {
                stmt.executeUpdate();
            }

            logger.fine("PostgreSQL operation successful: " + query.substring(0, Math.min(50, query.length())));

        } catch (SQLException e) {
            logger.warning("PostgreSQL operation failed (continuing with SQLite only): " + e.getMessage());
            logger.warning("Query: " + query);
            // Don't throw exception - PostgreSQL failure shouldn't break the flow
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    /**
     * Set parameters for prepared statement
     */
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    /**
     * Close database resources
     */
    private void closeResources(Connection conn, PreparedStatement stmt, ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException e) { logger.warning("Failed to close ResultSet: " + e.getMessage()); }
        }
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException e) { logger.warning("Failed to close PreparedStatement: " + e.getMessage()); }
        }
        if (conn != null) {
            try { conn.close(); } catch (SQLException e) { logger.warning("Failed to close Connection: " + e.getMessage()); }
        }
    }

    /**
     * Check if PostgreSQL dual-write is enabled
     */
    public boolean isPostgresEnabled() {
        return PostgreSQLConfig.isEnabled();
    }

    /**
     * Enable/disable PostgreSQL dual-write
     */
    public void setPostgresEnabled(boolean enabled) {
        PostgreSQLConfig.setEnabled(enabled);
        logger.info("PostgreSQL dual-write " + (enabled ? "enabled" : "disabled"));
    }
}

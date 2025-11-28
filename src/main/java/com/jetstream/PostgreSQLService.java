package com.jetstream.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * PostgreSQL Service for database operations.
 * Provides methods for executing queries and updates with retry logic and async support.
 */
public class PostgreSQLService {

    private static final Logger logger = Logger.getLogger(PostgreSQLService.class.getName());
    private static final ExecutorService asyncExecutor = Executors.newFixedThreadPool(3);

    /**
     * Execute a query on PostgreSQL (synchronous)
     */
    public static ResultSet executeQuery(String sql) {
        if (!PostgreSQLConfig.isEnabled()) {
            return null;
        }

        Connection conn = null;
        try {
            conn = PostgreSQLConfig.getConnection();
            if (conn == null) return null;
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            logger.warning("PostgreSQL query failed: " + e.getMessage());
            return null;
        } finally {
            PostgreSQLConfig.releaseConnection(conn);
        }
    }

    /**
     * Execute an update on PostgreSQL (synchronous) with retry logic
     */
    public static int executeUpdate(String sql) {
        if (!PostgreSQLConfig.isEnabled()) {
            return -1;
        }

        int retries = PostgreSQLConfig.getMaxRetries();
        long delay = PostgreSQLConfig.getRetryDelayMs();

        for (int attempt = 1; attempt <= retries; attempt++) {
            Connection conn = null;
            try {
                conn = PostgreSQLConfig.getConnection();
                if (conn == null) {
                    logger.warning("PostgreSQL connection unavailable");
                    return -1;
                }
                Statement stmt = conn.createStatement();
                int affected = stmt.executeUpdate(sql);
                stmt.close();
                logger.fine("PostgreSQL update successful: " + affected + " rows");
                return affected;
            } catch (SQLException e) {
                logger.warning("PostgreSQL update attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < retries) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return -1;
                    }
                }
            } finally {
                PostgreSQLConfig.releaseConnection(conn);
            }
        }
        logger.severe("PostgreSQL update failed after " + retries + " attempts");
        return -1;
    }

    /**
     * Execute an update asynchronously (non-blocking for UI)
     */
    public static CompletableFuture<Integer> executeUpdateAsync(String sql) {
        if (!PostgreSQLConfig.isEnabled()) {
            return CompletableFuture.completedFuture(-1);
        }

        return CompletableFuture.supplyAsync(() -> executeUpdate(sql), asyncExecutor)
                .exceptionally(ex -> {
                    logger.severe("Async PostgreSQL update failed: " + ex.getMessage());
                    return -1;
                });
    }

    /**
     * Execute a prepared statement update on PostgreSQL with retry
     */
    public static int executeUpdatePrepared(String sql, Object... params) {
        if (!PostgreSQLConfig.isEnabled()) {
            return -1;
        }

        int retries = PostgreSQLConfig.getMaxRetries();
        long delay = PostgreSQLConfig.getRetryDelayMs();

        for (int attempt = 1; attempt <= retries; attempt++) {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = PostgreSQLConfig.getConnection();
                if (conn == null) {
                    logger.warning("PostgreSQL connection unavailable");
                    return -1;
                }
                stmt = conn.prepareStatement(sql);
                setParameters(stmt, params);
                int affected = stmt.executeUpdate();
                logger.fine("PostgreSQL prepared update successful: " + affected + " rows");
                return affected;
            } catch (SQLException e) {
                logger.warning("PostgreSQL prepared update attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < retries) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return -1;
                    }
                }
            } finally {
                if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
                PostgreSQLConfig.releaseConnection(conn);
            }
        }
        logger.severe("PostgreSQL prepared update failed after " + retries + " attempts");
        return -1;
    }

    /**
     * Execute a prepared statement update asynchronously
     */
    public static CompletableFuture<Integer> executeUpdatePreparedAsync(String sql, Object... params) {
        if (!PostgreSQLConfig.isEnabled()) {
            return CompletableFuture.completedFuture(-1);
        }

        return CompletableFuture.supplyAsync(() -> executeUpdatePrepared(sql, params), asyncExecutor)
                .exceptionally(ex -> {
                    logger.severe("Async PostgreSQL prepared update failed: " + ex.getMessage());
                    return -1;
                });
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
     * Shutdown the async executor
     */
    public static void shutdown() {
        asyncExecutor.shutdown();
    }
}


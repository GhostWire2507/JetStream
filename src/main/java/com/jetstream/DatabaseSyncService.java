package com.jetstream.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Database Synchronization Service - Syncs existing SQLite data to PostgreSQL.
 * Used for initial data migration and ongoing sync operations.
 */
public class DatabaseSyncService {

    private static final Logger logger = Logger.getLogger(DatabaseSyncService.class.getName());
    private static final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();
    private static final int BATCH_SIZE = 100;

    // Tables to sync in order (respecting foreign key dependencies)
    private static final String[] SYNC_TABLES = {
        "users", "customer_details", "fleet_information", "flights",
        "flight_information", "seats", "fare", "bookings", "tickets",
        "reserved_seats", "cancellations"
    };

    /**
     * Sync all data from SQLite to PostgreSQL (async)
     */
    public static CompletableFuture<SyncResult> syncAllDataAsync() {
        return CompletableFuture.supplyAsync(DatabaseSyncService::syncAllData, syncExecutor);
    }

    /**
     * Sync all data from SQLite to PostgreSQL (synchronous)
     */
    public static SyncResult syncAllData() {
        SyncResult result = new SyncResult();
        
        if (!PostgreSQLConfig.isEnabled()) {
            result.success = false;
            result.message = "PostgreSQL is not enabled";
            return result;
        }

        Connection sqliteConn = DatabaseConnection.getConnection();
        if (sqliteConn == null) {
            result.success = false;
            result.message = "SQLite connection unavailable";
            return result;
        }

        logger.info("Starting full database sync from SQLite to PostgreSQL...");

        for (String table : SYNC_TABLES) {
            try {
                int synced = syncTable(sqliteConn, table);
                result.tableResults.put(table, synced);
                result.totalRowsSynced += synced;
                logger.info("Synced " + synced + " rows from table: " + table);
            } catch (Exception e) {
                result.tableErrors.put(table, e.getMessage());
                logger.warning("Failed to sync table " + table + ": " + e.getMessage());
            }
        }

        result.success = result.tableErrors.isEmpty();
        result.message = result.success ? 
            "Sync completed: " + result.totalRowsSynced + " rows" :
            "Sync completed with errors in " + result.tableErrors.size() + " tables";
        
        logger.info(result.message);
        return result;
    }

    /**
     * Sync a single table from SQLite to PostgreSQL
     */
    public static int syncTable(Connection sqliteConn, String tableName) throws SQLException {
        // Get column names from SQLite
        List<String> columns = getTableColumns(sqliteConn, tableName);
        if (columns.isEmpty()) {
            return 0;
        }

        String selectSql = "SELECT * FROM " + tableName;
        String columnList = String.join(", ", columns);
        String placeholders = String.join(", ", columns.stream().map(c -> "?").toArray(String[]::new));
        String insertSql = "INSERT INTO " + tableName + " (" + columnList + ") VALUES (" + placeholders + ") ON CONFLICT DO NOTHING";

        int totalSynced = 0;

        try (PreparedStatement selectStmt = sqliteConn.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery();
             Connection pgConn = PostgreSQLConfig.getConnection()) {

            if (pgConn == null) {
                throw new SQLException("PostgreSQL connection unavailable");
            }

            pgConn.setAutoCommit(false);
            try (PreparedStatement insertStmt = pgConn.prepareStatement(insertSql)) {
                int batchCount = 0;

                while (rs.next()) {
                    for (int i = 0; i < columns.size(); i++) {
                        Object value = rs.getObject(columns.get(i));
                        insertStmt.setObject(i + 1, convertValue(value));
                    }
                    insertStmt.addBatch();
                    batchCount++;

                    if (batchCount >= BATCH_SIZE) {
                        int[] results = insertStmt.executeBatch();
                        totalSynced += countSuccessful(results);
                        batchCount = 0;
                    }
                }

                if (batchCount > 0) {
                    int[] results = insertStmt.executeBatch();
                    totalSynced += countSuccessful(results);
                }

                pgConn.commit();
            } catch (SQLException e) {
                pgConn.rollback();
                throw e;
            }
        }

        return totalSynced;
    }

    private static List<String> getTableColumns(Connection conn, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }

    private static Object convertValue(Object value) {
        if (value == null) return null;
        // Handle SQLite-specific type conversions if needed
        return value;
    }

    private static int countSuccessful(int[] results) {
        int count = 0;
        for (int r : results) {
            if (r >= 0 || r == Statement.SUCCESS_NO_INFO) count++;
        }
        return count;
    }

    public static void shutdown() {
        syncExecutor.shutdown();
    }

    /** Sync result container */
    public static class SyncResult {
        public boolean success = false;
        public String message = "";
        public int totalRowsSynced = 0;
        public java.util.Map<String, Integer> tableResults = new java.util.HashMap<>();
        public java.util.Map<String, String> tableErrors = new java.util.HashMap<>();
    }
}


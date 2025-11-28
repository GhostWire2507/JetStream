package com.jetstream.database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Utility to run the complete airline reservation system schema.
 * This sets up all tables for login, booking, cancellation, etc.
 */
public class CompleteSchemaRunner {

    private static final Logger logger = Logger.getLogger(CompleteSchemaRunner.class.getName());

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   JetStream Complete Database Schema Setup                ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Initialize database connections
        System.out.println("=== Initializing Database Connections ===");
        DatabaseConnection.init();
        System.out.println();

        // Run complete schema on available databases
        runCompleteSchema();

        // Close connections
        DatabaseConnection.close();

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          Complete Schema Setup Finished!                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }

    /**
     * Run the complete PostgreSQL schema script
     */
    public static void runCompleteSchema() {
        logger.info("Running complete PostgreSQL schema script...");
        
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            logger.severe("PostgreSQL connection not available");
            System.err.println("✗ Cannot run schema: PostgreSQL not connected");
            return;
        }

        try {
            // Read the schema file from resources
            InputStream is = CompleteSchemaRunner.class.getResourceAsStream("/database/schema_postgresql_complete.sql");
            if (is == null) {
                logger.severe("Schema file not found in resources");
                System.err.println("✗ Schema file not found: /database/schema_postgresql_complete.sql");
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sql = new StringBuilder();
            String line;
            int successCount = 0;
            int failCount = 0;

            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                sql.append(line).append(" ");

                // Execute when we hit a semicolon
                if (line.endsWith(";")) {
                    String statement = sql.toString().trim();
                    sql.setLength(0); // Clear the buffer

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(statement);
                        
                        // Log table creation
                        if (statement.toUpperCase().contains("CREATE TABLE")) {
                            String tableName = extractTableName(statement);
                            logger.info("✓ Created table: " + tableName);
                            System.out.println("✓ Created table: " + tableName);
                        } else if (statement.toUpperCase().contains("INSERT INTO")) {
                            String tableName = extractInsertTableName(statement);
                            logger.info("✓ Inserted data into: " + tableName);
                            System.out.println("✓ Inserted data into: " + tableName);
                        } else if (statement.toUpperCase().contains("DROP TABLE")) {
                            logger.info("✓ Dropped old tables");
                        } else if (statement.toUpperCase().contains("UPDATE")) {
                            logger.info("✓ Updated data");
                            System.out.println("✓ Updated data");
                        }
                        
                        successCount++;
                    } catch (Exception e) {
                        logger.warning("Failed to execute statement: " + e.getMessage());
                        System.err.println("✗ Failed: " + e.getMessage());
                        failCount++;
                    }
                }
            }

            reader.close();

            System.out.println();
            System.out.println("=== PostgreSQL Complete Schema Setup Complete ===");
            System.out.println("  Successful statements: " + successCount);
            System.out.println("  Failed statements: " + failCount);
            System.out.println("==================================================");
            System.out.println();

            logger.info("PostgreSQL complete schema setup: " + successCount + " successful, " + failCount + " failed");

        } catch (Exception e) {
            logger.severe("Error running schema: " + e.getMessage());
            System.err.println("✗ Error running schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract table name from CREATE TABLE statement
     */
    private static String extractTableName(String sql) {
        try {
            String upper = sql.toUpperCase();
            int start = upper.indexOf("CREATE TABLE") + 12;
            int end = sql.indexOf("(", start);
            if (end == -1) end = sql.indexOf(" ", start + 1);
            String tableName = sql.substring(start, end).trim();
            // Remove IF NOT EXISTS if present
            tableName = tableName.replace("IF NOT EXISTS", "").trim();
            return tableName;
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Extract table name from INSERT INTO statement
     */
    private static String extractInsertTableName(String sql) {
        try {
            String upper = sql.toUpperCase();
            int start = upper.indexOf("INSERT INTO") + 11;
            int end = sql.indexOf("(", start);
            if (end == -1) end = sql.indexOf(" ", start + 1);
            return sql.substring(start, end).trim();
        } catch (Exception e) {
            return "unknown";
        }
    }
}


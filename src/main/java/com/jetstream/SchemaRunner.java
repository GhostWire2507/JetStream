package com.jetstream.database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Utility class to run SQL schema scripts on the databases.
 * This is a one-time setup utility.
 */
public class SchemaRunner {

    private static final Logger logger = Logger.getLogger(SchemaRunner.class.getName());

    /**
     * Run schema on available databases
     */
    public static void runSchema() {
        System.out.println("=== Running Database Schema ===");

        // Run schema on SQLite (main database)
        try {
            Connection sqliteConn = SQLiteConnector.getConnection();
            if (sqliteConn != null) {
                System.out.println("✓ SQLite main database connection available for schema setup");
                runSchemaOnConnection(sqliteConn, "SQLite", schemaSQLite);
            } else {
                System.err.println("⚠ SQLite connection failed - schema setup skipped");
            }
        } catch (Exception e) {
            System.err.println("⚠ SQLite schema setup failed: " + e.getMessage());
        }

        // Also run schema on PostgreSQL if available (secondary)
        try {
            Connection pgConn = PostgreConnector.getConnection();
            if (pgConn != null) {
                System.out.println("✓ PostgreSQL secondary connection available for schema setup");
                runSchemaOnConnection(pgConn, "PostgreSQL", schemaPostgres);
            } else {
                System.err.println("⚠ PostgreSQL connection failed - skipping secondary schema setup");
            }
        } catch (Exception e) {
            System.err.println("⚠ PostgreSQL secondary schema setup failed: " + e.getMessage());
        }

        System.out.println("=== Schema Setup Complete ===");
    }

    /**
     * Run schema on a specific connection
     */
    private static void runSchemaOnConnection(Connection conn, String dbType, String schema) {
        try {
            Statement stmt = conn.createStatement();
            String[] statements = schema.split(";");
            int successCount = 0;
            int failCount = 0;

            for (String statement : statements) {
                statement = statement.trim();
                if (statement.isEmpty()) continue;

                try {
                    stmt.execute(statement);
                    successCount++;

                    // Log what was executed
                    if (statement.toUpperCase().startsWith("CREATE TABLE")) {
                        String tableName = extractTableName(statement);
                        logger.info("✓ Created table: " + tableName + " on " + dbType);
                        System.out.println("✓ Created table: " + tableName + " on " + dbType);
                    } else if (statement.toUpperCase().startsWith("INSERT INTO")) {
                        logger.info("✓ Inserted sample data on " + dbType);
                    }
                } catch (Exception e) {
                    failCount++;
                    logger.warning("Failed to execute statement on " + dbType + ": " + e.getMessage());
                }
            }

            stmt.close();

            System.out.println("\n=== " + dbType + " Schema Setup Complete ===");
            System.out.println("  Successful statements: " + successCount);
            System.out.println("  Failed statements: " + failCount);
            System.out.println("========================================\n");

            logger.info(dbType + " schema setup complete: " + successCount + " successful, " + failCount + " failed");

        } catch (Exception e) {
            logger.severe("Error running " + dbType + " schema: " + e.getMessage());
            System.err.println("✗ Error running " + dbType + " schema: " + e.getMessage());
        }
    }

    /**
     * Schema strings - loaded from files
     */
    private static String schemaSQLite = loadSchema("/database/schema_sqlite.sql");
    private static String schemaPostgres = loadSchema("/database/schema_postgresql.sql");

    /**
     * Load schema from resource file
     */
    private static String loadSchema(String resourcePath) {
        try {
            InputStream is = SchemaRunner.class.getResourceAsStream(resourcePath);
            if (is == null) {
                logger.severe("Schema file not found: " + resourcePath);
                return "";
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                sql.append(line).append(" ");
            }
            reader.close();
            return sql.toString();
        } catch (Exception e) {
            logger.severe("Error loading schema from " + resourcePath + ": " + e.getMessage());
            return "";
        }
    }

    /**
     * Run the MySQL schema script - REMOVED: Now using PostgreSQL only
     */
    @Deprecated
    public static void runMySQLSchema() {
        logger.info("MySQL schema setup is deprecated - using PostgreSQL only");
        System.out.println("⚠ MySQL schema setup skipped - PostgreSQL only mode");
    }

    /**
     * Extract table name from CREATE TABLE statement
     */
    private static String extractTableName(String statement) {
        try {
            String upper = statement.toUpperCase();
            int start = upper.indexOf("TABLE") + 5;
            int end = statement.indexOf("(", start);
            if (end == -1) end = statement.indexOf(" ", start + 1);
            String tableName = statement.substring(start, end).trim();
            // Remove IF NOT EXISTS if present
            tableName = tableName.replace("IF NOT EXISTS", "").trim();
            return tableName;
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Main method to run schema setup
     */
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          JetStream Database Schema Setup                  ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // Initialize database connections
        DatabaseConnection.init();

        if (!DatabaseConnection.isAvailable()) {
            System.err.println("✗ No database connections available. Exiting.");
            return;
        }

        // Run schema on available databases
        System.out.println("\n--- Setting up Databases ---");
        runSchema();

        // MySQL schema setup removed - PostgreSQL only

        // Close connections
        DatabaseConnection.close();

        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          Schema Setup Complete!                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
}


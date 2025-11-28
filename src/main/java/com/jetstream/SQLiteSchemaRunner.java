package com.jetstream.database;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Runner to initialize the local SQLite database file using
 * the SQL statements in `database/jetstream_sqlite_complete.sql`.
 */
public class SQLiteSchemaRunner {

    private static final Logger logger = Logger.getLogger(SQLiteSchemaRunner.class.getName());

    public static void main(String[] args) {
        System.out.println("=== SQLite Schema Runner ===");

        // Initialize DB connection (uses config.properties db.type=sqlite)
        DatabaseConnection.init();

        try {
            runSchema();
        } finally {
            DatabaseConnection.close();
        }

        System.out.println("=== Done ===");
    }

    public static void runSchema() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            logger.severe("No database connection available. Ensure db.type=sqlite in config.properties and that the connector can open the file.");
            System.err.println("✗ No DB connection. Aborting schema run.");
            return;
        }

        Path sqlPath = Path.of("database", "jetstream_sqlite_complete.sql");
        if (!Files.exists(sqlPath)) {
            logger.severe("Schema file not found: " + sqlPath.toAbsolutePath());
            System.err.println("✗ Schema file not found: " + sqlPath.toAbsolutePath());
            return;
        }

        int success = 0;
        int fail = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(sqlPath.toFile()))) {
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // skip comments
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("//")) continue;
                sb.append(line).append(' ');
                if (trimmed.endsWith(";")) {
                    String stmtSql = sb.toString().trim();
                    sb.setLength(0);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(stmtSql);
                        success++;
                        if (stmtSql.toUpperCase().contains("CREATE TABLE") || stmtSql.toUpperCase().contains("CREATE VIEW")) {
                            System.out.println("✓ Executed: " + firstLine(stmtSql));
                        }
                    } catch (Exception e) {
                        fail++;
                        logger.warning("Failed to execute statement: " + e.getMessage());
                        System.err.println("✗ Failed: " + e.getMessage());
                    }
                }
            }

            System.out.println("Schema run finished. Success: " + success + ", Fail: " + fail);

        } catch (Exception e) {
            logger.severe("Error reading/executing schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String firstLine(String s) {
        String[] parts = s.split("\\n");
        return parts.length > 0 ? parts[0].trim() : s;
    }
}

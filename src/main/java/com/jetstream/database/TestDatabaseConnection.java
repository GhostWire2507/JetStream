package com.jetstream.database;

import com.jetstream.config.AppConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Utility class to test database connections
 * Run this to verify PostgreSQL and MySQL connectivity
 */
public class TestDatabaseConnection {

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║        JetStream Database Connection Test Utility         ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // Load configuration
        try {
            AppConfig.load();
            System.out.println("✓ Configuration loaded successfully\n");
        } catch (Exception e) {
            System.err.println("✗ Failed to load configuration: " + e.getMessage());
            return;
        }

        // Test PostgreSQL
        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.println("Testing PostgreSQL Connection...");
        System.out.println("─────────────────────────────────────────────────────────────");
        testPostgreSQL();

        System.out.println("\n─────────────────────────────────────────────────────────────");
        System.out.println("Testing MySQL Connection...");
        System.out.println("─────────────────────────────────────────────────────────────");
        testMySQL();

        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    Test Complete                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }

    private static void testPostgreSQL() {
        try {
            Connection conn = PostgreConnector.getConnection();
            
            if (conn == null) {
                System.err.println("✗ PostgreSQL connection is NULL");
                System.err.println("\nPossible issues:");
                System.err.println("  1. Incorrect hostname in config.properties");
                System.err.println("  2. IP not whitelisted in Render dashboard");
                System.err.println("  3. Database not running or accessible");
                System.err.println("  4. SSL configuration issue");
                return;
            }

            System.out.println("✓ PostgreSQL connection established!");
            
            // Test query
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT version()");
                if (rs.next()) {
                    System.out.println("✓ PostgreSQL version: " + rs.getString(1).substring(0, 50) + "...");
                }
                
                // Test if tables exist
                rs = stmt.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'");
                if (rs.next()) {
                    System.out.println("✓ Number of tables in database: " + rs.getInt(1));
                }
                
                rs.close();
                stmt.close();
            } catch (Exception e) {
                System.err.println(" Query test failed: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("✗ PostgreSQL test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testMySQL() {
        try {
            MySQLConnector.connect();
            Connection conn = MySQLConnector.getConnection();
            
            if (conn == null) {
                System.err.println("✗ MySQL connection is NULL");
                System.err.println("\nPossible issues:");
                System.err.println("  1. MySQL server not running");
                System.err.println("  2. Incorrect username/password");
                System.err.println("  3. Database 'jetstream' doesn't exist");
                System.err.println("  4. MySQL not listening on port 3306");
                return;
            }

            System.out.println("✓ MySQL connection established!");
            
            // Test query
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT VERSION()");
                if (rs.next()) {
                    System.out.println("✓ MySQL version: " + rs.getString(1));
                }
                
                // Test if tables exist
                rs = stmt.executeQuery("SHOW TABLES");
                int tableCount = 0;
                while (rs.next()) {
                    tableCount++;
                }
                System.out.println("✓ Number of tables in database: " + tableCount);
                
                rs.close();
                stmt.close();
            } catch (Exception e) {
                System.err.println("Query test failed: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("✗ MySQL test failed: " + e.getMessage());
            System.err.println("  This is OK if you're only using PostgreSQL");
        }
    }
}


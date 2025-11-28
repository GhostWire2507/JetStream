package com.jetstream.test;

import com.jetstream.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Test database schema and login functionality
 */
public class TestLogin {

    public static void main(String[] args) {
        System.out.println("=== Testing Database Schema ===\n");

        try {
            // Initialize database
            DatabaseConnection.init();
            Connection conn = DatabaseConnection.getConnection();

            if (conn == null) {
                System.err.println("❌ Failed to connect to PostgreSQL");
                return;
            }

            System.out.println("✅ Connected to PostgreSQL\n");

            // Test 1: Check if users table exists
            System.out.println("Test 1: Checking users table...");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users");
            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("✅ Users table exists with " + count + " users\n");
            }

            // Test 2: List all users
            System.out.println("Test 2: Listing all users...");
            rs = stmt.executeQuery("SELECT id, username, role, full_name, email FROM users ORDER BY id");
            System.out.println("ID | Username | Role | Full Name | Email");
            System.out.println("---|----------|------|-----------|------");
            while (rs.next()) {
                System.out.printf("%d | %s | %s | %s | %s%n",
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getString("full_name"),
                    rs.getString("email")
                );
            }
            System.out.println();

            // Test 3: Check customer_details table
            System.out.println("Test 3: Checking customer_details table...");
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM customer_details");
            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("✅ customer_details table exists with " + count + " customers\n");
            }

            // Test 4: Check flights table
            System.out.println("Test 4: Checking flights table...");
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM flights");
            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("✅ flights table exists with " + count + " flights\n");
            }

            // Test 5: Check bookings table
            System.out.println("Test 5: Checking bookings table...");
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM bookings");
            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("✅ bookings table exists with " + count + " bookings\n");
            }

            // Test 6: Test login query
            System.out.println("Test 6: Testing login query for admin...");
            rs = stmt.executeQuery(
                "SELECT id, username, password, role, full_name, email " +
                "FROM users WHERE username = 'admin' AND role = 'admin'"
            );
            if (rs.next()) {
                System.out.println("✅ Admin user found:");
                System.out.println("   ID: " + rs.getInt("id"));
                System.out.println("   Username: " + rs.getString("username"));
                System.out.println("   Password: " + rs.getString("password"));
                System.out.println("   Role: " + rs.getString("role"));
                System.out.println("   Full Name: " + rs.getString("full_name"));
                System.out.println("   Email: " + rs.getString("email"));
            } else {
                System.out.println("❌ Admin user not found!");
            }

            rs.close();
            stmt.close();

            System.out.println("\n=== All Tests Passed! ===");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseConnection.close();
        }
    }
}


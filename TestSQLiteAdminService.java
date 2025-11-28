package com.jetstream.test;

import com.jetstream.services.AdminService;
import com.jetstream.models.User;
import com.jetstream.database.DatabaseConnection;

public class TestSQLiteAdminService {

    public static void main(String[] args) {
        System.out.println("=== Testing SQLite AdminService ===\n");

        try {
            // Initialize database connections
            DatabaseConnection.init();
            if (!DatabaseConnection.isAvailable()) {
                System.err.println("❌ Database connection is not available.");
                return;
            }
            System.out.println("✅ Database connection established.\n");

            AdminService adminService = new AdminService();

            // Test: Create a new user
            System.out.println("Test: Creating user 'testuser'...");
            boolean created = adminService.createUser("testuser", "testpass", "Test User", "testuser@example.com", "customer");
            System.out.println(created ? "✅ User created successfully.\n" : "❌ User creation failed.\n");

            // Test: Authenticate created user
            System.out.println("Test: Authenticating user 'testuser'...");
            User user = adminService.authenticate("testuser", "testpass");
            System.out.println(user != null ? "✅ User authenticated successfully." : "❌ Authentication failed.");
            if (user != null) {
                System.out.println("User details:");
                System.out.println(" ID: " + user.getId());
                System.out.println(" Username: " + user.getUsername());
                System.out.println(" Full Name: " + user.getFullName());
                System.out.println(" Email: " + user.getEmail());
                System.out.println(" Role: " + user.getRole());
            }
            System.out.println();

            // Test: Update last login
            System.out.println("Test: Updating last login for user ID: " + (user != null ? user.getId() : "N/A"));
            if (user != null) {
                // updateLastLogin is private, bypass by executing update via executeUpdate method or omit this test
                System.out.println("⚠ Skipping direct updateLastLogin call as it's private.\n");
            } else {
                System.out.println("❌ Cannot update last login - user not authenticated.\n");
            }

            // Test: Fetch user by username
            System.out.println("Test: Fetching user by username 'testuser'...");
            User fetchedUser = adminService.getUserByUsername("testuser");
            System.out.println(fetchedUser != null ? "✅ User fetched successfully." : "❌ Fetch user failed.");
            if (fetchedUser != null) {
                System.out.println("User details:");
                System.out.println(" ID: " + fetchedUser.getId());
                System.out.println(" Username: " + fetchedUser.getUsername());
                System.out.println(" Full Name: " + fetchedUser.getFullName());
                System.out.println(" Email: " + fetchedUser.getEmail());
                System.out.println(" Role: " + fetchedUser.getRole());
            }
            System.out.println();

            // Clean up: Ideally delete test user (if supported)

            System.out.println("=== SQLite AdminService tests completed ===");

        } catch (Exception e) {
            System.err.println("❌ Error during SQLite AdminService testing: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseConnection.close();
        }
    }
}

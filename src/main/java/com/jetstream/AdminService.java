package com.jetstream.services;

import java.util.logging.Logger;
import com.jetstream.models.User;

/**
 * AdminService for authentication and user management without database dependency.
 * Uses AuthenticationService reading user data from JSON file.
 */
public class AdminService {

    private static final Logger logger = Logger.getLogger(AdminService.class.getName());

    private AuthenticationService authenticationService = new AuthenticationService();

    /**
     * Authenticate user using JSON-based AuthenticationService.
     */
    public User authenticate(String username, String passwordPlain) {
        boolean authenticated = AuthenticationService.authenticate(username, passwordPlain);
        if (!authenticated) {
            logger.warning("Authentication failed for user: " + username);
            return null;
        }

        // Retrieve user info from AuthenticationService
        AuthenticationService.User user = AuthenticationService.getUser(username);
        if (user == null) {
            logger.warning("User not found after authentication: " + username);
            return null;
        }

        logger.info("User authenticated successfully: " + username + " (Role: " + user.role + ")");
        // Map AuthenticationService.User to com.jetstream.models.User
        // Attempt to resolve a database user id for this username so DB-backed features (last_login etc.) work
        int dbId = 0;
        String dbFullName = user.fullName;
        String dbEmail = user.email;
        java.sql.Connection conn = com.jetstream.database.DatabaseConnection.getConnection();
        if (conn != null) {
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id, full_name, email, role FROM users WHERE username = ?")) {
                ps.setString(1, user.username);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dbId = rs.getInt("id");
                        String full = rs.getString("full_name");
                        String mail = rs.getString("email");
                        String roleFromDb = rs.getString("role");
                        if (full != null && !full.isEmpty()) dbFullName = full;
                        if (mail != null && !mail.isEmpty()) dbEmail = mail;
                        if (roleFromDb != null && !roleFromDb.isEmpty()) user.role = roleFromDb;
                    }
                }
            } catch (Exception e) {
                logger.fine("No DB mapping for user or DB unavailable: " + e.getMessage());
            }
        }

        User appUser = new User(
            dbId,
            user.username,
            user.password,
            dbFullName,
            dbEmail,
            user.role,
            true // active by default
        );

        // Log the successful login event or integrate session management here as needed

        return appUser;
    }

    /**
     * Create a new user in the SQLite database.
     */
    public boolean createUser(String username, String password, String fullName, String email, String role) {
        // Optionally hash the password here if needed
        String passwordToStore = password; // For now, store as plain text (match your schema)
        String sql = "INSERT INTO users (username, password, role, full_name, email) VALUES (?, ?, ?, ?, ?)";
        java.sql.Connection conn2 = com.jetstream.database.DatabaseConnection.getConnection();
        if (conn2 == null) {
            logger.severe("Database not available when creating user");
            return false;
        }
        try (java.sql.PreparedStatement ps = conn2.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordToStore);
            ps.setString(3, role);
            ps.setString(4, fullName);
            ps.setString(5, email);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                logger.info(String.format("User created successfully in SQLite DB: %s", username));
                return true;
            } else {
                logger.warning(String.format("User creation failed, no rows affected: %s", username));
                return false;
            }
        } catch (Exception e) {
            logger.severe("Error creating user in SQLite DB: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get user by ID unsupported without DB; return null.
     */
    public User getUserById(int userId) {
        logger.warning("Get user by ID unsupported in JSON-based authentication service.");
        return null;
    }

    /**
     * Get user by username from AuthenticationService data.
     */
    public User getUserByUsername(String username) {
        AuthenticationService.User user = AuthenticationService.getUser(username);
        if (user == null) {
            return null;
        }
        return new User(
            0,
            user.username,
            user.password,
            user.fullName,
            user.email,
            user.role,
            true
        );
    }

    /**
     * Initialize default users - no-op as users are loaded from JSON file.
     */
    public void initializeDefaultUsers() {
        logger.info("Using JSON-based user data, no initialization needed.");
    }

}

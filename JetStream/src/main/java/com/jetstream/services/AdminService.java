package com.jetstream.services;

import com.jetstream.database.DatabaseConnection;

import java.sql.ResultSet;

/**
 * AdminService for authentication and user management
 */
public class AdminService {

    public boolean authenticate(String username, String passwordPlain) {
        String sql = "SELECT id, username, password_hash FROM admins WHERE username = '" + sanitize(username) + "'";
        try {
            ResultSet rs = DatabaseConnection.executeQuery(sql); // Reads from MySQL by default
            if (rs != null && rs.next()) {
                String hash = rs.getString("password_hash");
                return passwordPlain.equals(hash); // For demo; replace with hashing
            }
        } catch (Exception ignored) {}
        return false;
    }

    public boolean createAdmin(String username, String password) {
        String sql = "INSERT INTO admins(username, password_hash) VALUES ('"
                + sanitize(username) + "', '" + sanitize(password) + "')";
        return DatabaseConnection.executeUpdate(sql) > 0; // Writes to both DBs
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("'", "''");
    }
}

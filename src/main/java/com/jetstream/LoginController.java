 package com.jetstream.controllers;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.utils.SceneManager;
import com.jetstream.manager.SessionManager;
import com.jetstream.services.AdminService;
import com.jetstream.models.User;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * Controller for the login screen.
 * Handles user authentication and role-based navigation.
 */
public class LoginController {

    private static final Logger logger = Logger.getLogger(LoginController.class.getName());

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private RadioButton adminRadio;
    @FXML private RadioButton staffRadio;
    @FXML private RadioButton customerRadio;
    @FXML private ToggleGroup roleGroup;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;
    @FXML private ProgressIndicator progressIndicator;

    @FXML
    public void initialize() {
        logger.info("LoginController initialized");
        
        // Add fade transition to login button
        addButtonAnimation();
        
        // Clear error on typing
        usernameField.textProperty().addListener((obs, old, newVal) -> errorLabel.setText(""));
        passwordField.textProperty().addListener((obs, old, newVal) -> errorLabel.setText(""));
        
        // Handle Enter key
        passwordField.setOnAction(e -> handleLogin());
    }

    /**
     * Add fade animation to login button
     */
    private void addButtonAnimation() {
        FadeTransition fade = new FadeTransition(Duration.seconds(1.5), loginButton);
        fade.setFromValue(1.0);
        fade.setToValue(0.7);
        fade.setCycleCount(FadeTransition.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();
    }

    /**
     * Handle login button click
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        // Get selected role
        RadioButton selectedRole = (RadioButton) roleGroup.getSelectedToggle();
        String role = selectedRole.getText().toLowerCase();

        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }

        // Show progress
        progressIndicator.setVisible(true);
        loginButton.setDisable(true);

        // Authenticate user (in a real app, this would be async)
        authenticateUser(username, password, role);
    }

    /**
     * Authenticate user against database
     */
    private void authenticateUser(String username, String password, String role) {
        try {
            // Use AdminService for authentication
            AdminService adminService = new AdminService();
            User authenticatedUser = adminService.authenticate(username, password);

            if (authenticatedUser != null) {
                // Check if the user's role matches the selected role
                if (!authenticatedUser.getRole().equalsIgnoreCase(role)) {
                    showError("Role mismatch: You are not authorized as " + role);
                    resetLoginButton();
                    return;
                }

                logger.info("Login successful: " + username + " as " + role);

                // Store session with user details
                SessionManager.getInstance().login(
                    authenticatedUser.getId(),
                    authenticatedUser.getUsername(),
                    authenticatedUser.getFullName(),
                    authenticatedUser.getEmail(),
                    authenticatedUser.getRole()
                );

                // Update last_login in DB when possible
                try {
                    int uid = authenticatedUser.getId();
                    if (uid <= 0) {
                        // try to resolve DB id by username
                        Connection conn = DatabaseConnection.getConnection();
                        if (conn != null) {
                            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
                                ps.setString(1, authenticatedUser.getUsername());
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) uid = rs.getInt("id");
                                }
                            } catch (Exception ex) {
                                logger.fine("Could not resolve DB id for last_login: " + ex.getMessage());
                            }
                        } else {
                            logger.fine("Database unavailable when resolving DB id for last_login");
                        }
                    }
                    if (uid > 0) updateLastLogin(uid);
                } catch (Exception e) {
                    logger.fine("Failed to update last login: " + e.getMessage());
                }

                // Navigate to appropriate dashboard
                navigateToDashboard(role);
            } else {
                showError("Invalid credentials or role");
                resetLoginButton();
            }

        } catch (Exception e) {
            logger.severe("Login error: " + e.getMessage());
            showError("Login failed: " + e.getMessage());
            resetLoginButton();
            e.printStackTrace();
        }
    }

    /**
     * Update last login timestamp
     */
    private void updateLastLogin(int userId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            logger.warning("Failed to update last login: " + e.getMessage());
        }
    }

    /**
     * Navigate to role-specific dashboard
     */
    private void navigateToDashboard(String role) {
        try {
            switch (role.toLowerCase()) {
                case "admin":
                    SceneManager.getInstance().loadAdminDashboard();
                    break;
                case "staff":
                    // Route staff users to the staff dashboard (limited interface)
                    SceneManager.getInstance().loadStaffDashboard();
                    break;
                case "customer":
                    SceneManager.getInstance().loadCustomerDashboard(); // Customer uses dedicated dashboard
                    break;
                default:
                    showError("Unknown role: " + role);
                    resetLoginButton();
            }
        } catch (Exception e) {
            logger.severe("Navigation error: " + e.getMessage());
            showError("Failed to load dashboard");
            resetLoginButton();
        }
    }

    /**
     * Handle register link click
     */
    @FXML
    private void handleRegister() {
        try {
            SceneManager.getInstance().loadRegister();
        } catch (Exception e) {
            logger.severe("Failed to load registration: " + e.getMessage());
            showError("Failed to load registration page");
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText(message);

        // Fade in error
        FadeTransition fade = new FadeTransition(Duration.millis(300), errorLabel);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    /**
     * Reset login button state
     */
    private void resetLoginButton() {
        progressIndicator.setVisible(false);
        loginButton.setDisable(false);
    }
}


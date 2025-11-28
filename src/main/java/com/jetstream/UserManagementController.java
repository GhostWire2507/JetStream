package com.jetstream.controllers;

import com.jetstream.database.DatabaseConnection;
import com.jetstream.services.AdminService;
import com.jetstream.utils.SceneManager;
import com.jetstream.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Controller for User Management screen
 */
public class UserManagementController {

    private static final Logger logger = Logger.getLogger(UserManagementController.class.getName());

    @FXML private TextField txtUsername, txtFullName, txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private TextField txtEditUsername, txtEditFullName, txtEditEmail;
    @FXML private PasswordField txtEditPassword;
    @FXML private ComboBox<String> cmbEditRole;
    @FXML private Button btnAddUser, btnUpdateUser, btnDeleteUser;
    @FXML private Label totalUsersLabel, activeUsersLabel, roleDistributionLabel;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUsername, colFullName, colEmail, colRole, colStatus, colLastLogin;

    private final AdminService adminService = new AdminService();
    private final ObservableList<User> usersList = FXCollections.observableArrayList();
    private User selectedUser;

    @FXML
    public void initialize() {
        logger.info("UserManagementController initialized");

        // Setup combo boxes
        cmbRole.setItems(FXCollections.observableArrayList("admin", "staff", "customer"));
        cmbEditRole.setItems(FXCollections.observableArrayList("admin", "staff", "customer"));

        // Setup table columns
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(user.isActive() ? "Active" : "Inactive");
        });
        colLastLogin.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            LocalDateTime lastLogin = user.getLastLogin();
            String formatted = lastLogin != null ?
                lastLogin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "Never";
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        // Setup table selection listener
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateEditFields(newSelection);
                selectedUser = newSelection;
            }
        });

        // Load data
        loadUsers();
        loadStatistics();
        // Disable admin actions for staff users
        try {
            com.jetstream.manager.SessionManager session = com.jetstream.manager.SessionManager.getInstance();
            if (session.isStaff()) {
                btnAddUser.setDisable(true);
                btnUpdateUser.setDisable(true);
                btnDeleteUser.setDisable(true);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Load users from database
     */
    private void loadUsers() {
        try {
            usersList.clear();
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                String sql = "SELECT id, username, password, full_name, email, role, is_active, created_at, last_login FROM users ORDER BY created_at DESC";

                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    User user = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getInt("is_active") == 1
                    );

                    // Set timestamps if available
                    java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
                    if (createdTs != null) {
                        user.setCreatedAt(createdTs.toLocalDateTime());
                    }

                    java.sql.Timestamp lastLoginTs = rs.getTimestamp("last_login");
                    if (lastLoginTs != null) {
                        user.setLastLogin(lastLoginTs.toLocalDateTime());
                    }

                    usersList.add(user);
                }

                rs.close();
                stmt.close();
            }

            usersTable.setItems(usersList);
            logger.info("Loaded " + usersList.size() + " users");

        } catch (Exception e) {
            logger.severe("Error loading users: " + e.getMessage());
            showAlert("Error", "Failed to load users: " + e.getMessage());
        }
    }

    /**
     * Load statistics
     */
    private void loadStatistics() {
        try {
            int totalUsers = usersList.size();
            int activeUsers = (int) usersList.stream().filter(User::isActive).count();

            long adminCount = usersList.stream().filter(u -> "admin".equals(u.getRole())).count();
            long staffCount = usersList.stream().filter(u -> "staff".equals(u.getRole())).count();
            long customerCount = usersList.stream().filter(u -> "customer".equals(u.getRole())).count();

            totalUsersLabel.setText(String.valueOf(totalUsers));
            activeUsersLabel.setText(String.valueOf(activeUsers));
            roleDistributionLabel.setText("A:" + adminCount + " S:" + staffCount + " C:" + customerCount);

        } catch (Exception e) {
            logger.warning("Error loading statistics: " + e.getMessage());
        }
    }

    /**
     * Handle add user
     */
    @FXML
    private void handleAddUser() {
        if (!validateUserInput(txtUsername, txtPassword, txtFullName, txtEmail, cmbRole)) {
            return;
        }

        try {
            String username = txtUsername.getText().trim();
            String password = txtPassword.getText().trim();
            String fullName = txtFullName.getText().trim();
            String email = txtEmail.getText().trim();
            String role = cmbRole.getValue();

            boolean success = adminService.createUser(username, password, fullName, email, role);
            if (success) {
                showAlert("Success", "User added successfully!");
                clearAddFields();
                loadUsers();
                loadStatistics();
            } else {
                showAlert("Error", "Failed to add user");
            }

        } catch (Exception e) {
            logger.severe("Error adding user: " + e.getMessage());
            showAlert("Error", "Failed to add user: " + e.getMessage());
        }
    }

    /**
     * Handle update user
     */
    @FXML
    private void handleUpdateUser() {
        if (selectedUser == null) {
            showAlert("Error", "Please select a user to update");
            return;
        }

        if (!validateUserInput(txtEditUsername, null, txtEditFullName, txtEditEmail, cmbEditRole)) {
            return;
        }

        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                String sql = "UPDATE users SET username = ?, full_name = ?, email = ?, role = ?";
                if (!txtEditPassword.getText().trim().isEmpty()) {
                    sql += ", password = ?";
                }
                sql += " WHERE id = ?";

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, txtEditUsername.getText().trim());
                stmt.setString(2, txtEditFullName.getText().trim());
                stmt.setString(3, txtEditEmail.getText().trim());
                stmt.setString(4, cmbEditRole.getValue());

                int paramIndex = 5;
                if (!txtEditPassword.getText().trim().isEmpty()) {
                    stmt.setString(paramIndex++, txtEditPassword.getText().trim());
                }
                stmt.setInt(paramIndex, selectedUser.getId());

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    showAlert("Success", "User updated successfully!");
                    loadUsers();
                    loadStatistics();
                } else {
                    showAlert("Error", "Failed to update user");
                }

                stmt.close();
            }

        } catch (Exception e) {
            logger.severe("Error updating user: " + e.getMessage());
            showAlert("Error", "Failed to update user: " + e.getMessage());
        }
    }

    /**
     * Handle delete user
     */
    @FXML
    private void handleDeleteUser() {
        if (selectedUser == null) {
            showAlert("Error", "Please select a user to delete");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText("Delete User");
        confirmation.setContentText("Are you sure you want to delete user " + selectedUser.getUsername() + "?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Connection conn = DatabaseConnection.getConnection();
                    if (conn != null) {
                        String sql = "DELETE FROM users WHERE id = ?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, selectedUser.getId());

                        int rowsAffected = stmt.executeUpdate();
                        if (rowsAffected > 0) {
                            showAlert("Success", "User deleted successfully!");
                            loadUsers();
                            loadStatistics();
                            clearEditFields();
                            selectedUser = null;
                        } else {
                            showAlert("Error", "Failed to delete user");
                        }

                        stmt.close();
                    }

                } catch (Exception e) {
                    logger.severe("Error deleting user: " + e.getMessage());
                    showAlert("Error", "Failed to delete user: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Handle back to dashboard
     */
    @FXML
    private void handleBackToDashboard() {
        com.jetstream.manager.SessionManager session = com.jetstream.manager.SessionManager.getInstance();
        if (session.isStaff()) {
            SceneManager.getInstance().loadStaffDashboard();
        } else if (session.isAdmin()) {
            SceneManager.getInstance().loadAdminDashboard();
        } else {
            SceneManager.getInstance().loadCustomerDashboard();
        }
    }

    /**
     * Populate edit fields with selected user data
     */
    private void populateEditFields(User user) {
        txtEditUsername.setText(user.getUsername());
        txtEditFullName.setText(user.getFullName());
        txtEditEmail.setText(user.getEmail());
        cmbEditRole.setValue(user.getRole());
        txtEditPassword.clear(); // Don't populate password for security
    }

    /**
     * Clear add user fields
     */
    private void clearAddFields() {
        txtUsername.clear();
        txtPassword.clear();
        txtFullName.clear();
        txtEmail.clear();
        cmbRole.setValue(null);
    }

    /**
     * Clear edit user fields
     */
    private void clearEditFields() {
        txtEditUsername.clear();
        txtEditPassword.clear();
        txtEditFullName.clear();
        txtEditEmail.clear();
        cmbEditRole.setValue(null);
    }

    /**
     * Validate user input fields
     */
    private boolean validateUserInput(TextField username, PasswordField password, TextField fullName, TextField email, ComboBox<String> role) {
        if (username.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Username is required");
            return false;
        }

        if (password != null && password.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Password is required");
            return false;
        }

        if (fullName.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Full name is required");
            return false;
        }

        if (email.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Email is required");
            return false;
        }

        if (role.getValue() == null) {
            showAlert("Validation Error", "Role is required");
            return false;
        }

        // Basic email validation
        if (!email.getText().trim().contains("@")) {
            showAlert("Validation Error", "Please enter a valid email address");
            return false;
        }

        return true;
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

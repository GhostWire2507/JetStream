package com.jetstream.test;

import com.jetstream.services.AuthenticationService;
import com.jetstream.services.AdminService;
import com.jetstream.models.User;

public class AuthTest {
    public static void main(String[] args) {
        System.out.println("JSON Authenticate admin: " + AuthenticationService.authenticate("admin","admin123"));
        System.out.println("JSON Authenticate staff: " + AuthenticationService.authenticate("staff","staff123"));
        System.out.println("JSON Authenticate customer: " + AuthenticationService.authenticate("customer","customer123"));
        AuthenticationService.User u = AuthenticationService.getUser("staff");
        if (u != null) System.out.println("JSON Staff user role: " + u.role + ", fullName: " + u.fullName);

        AdminService as = new AdminService();
        User appUser = as.authenticate("staff","staff123");
        if (appUser != null) {
            System.out.println("App User mapped: id=" + appUser.getId() + ", username=" + appUser.getUsername() + ", role=" + appUser.getRole() + ", fullName=" + appUser.getFullName());
        } else {
            System.out.println("AdminService.authenticate returned null for staff");
        }
    }
}

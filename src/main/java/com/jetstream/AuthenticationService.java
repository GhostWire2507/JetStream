package com.jetstream.services;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Simple authentication service that reads user credentials from a JSON-like file.
 * Avoids using external JSON libraries for simplicity and lightweight.
 */
public class AuthenticationService {

    public static class User {
        public String username;
        public String password;
        public String role;
        public String fullName;
        public String email;
        public String phone;

        public User(String username, String password, String role, String fullName, String email, String phone) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
        }
    }

    private static List<User> users = new ArrayList<>();

    static {
        loadUsers();
    }

    private static void loadUsers() {
        try (InputStream is = AuthenticationService.class.getClassLoader().getResourceAsStream("users.json");
             Scanner scanner = new Scanner(is)) {

            StringBuilder jsonBuilder = new StringBuilder();
            while (scanner.hasNextLine()) {
                jsonBuilder.append(scanner.nextLine());
            }
            String json = jsonBuilder.toString();

            // Very simple parsing assuming fixed format of users.json
            json = json.replace("[", "").replace("]", "").replace("\"", "");
            String[] userEntries = json.split("\\},\\s*\\{");

            for (String entry : userEntries) {
                entry = entry.replace("{", "").replace("}", "");
                String[] fields = entry.split(",");
                String username = "", password = "", role = "", fullName = "", email = "", phone = "";
                for (String field : fields) {
                    String[] kv = field.split(":", 2);
                    if (kv.length < 2) continue;
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    switch (key) {
                        case "username":
                            username = value;
                            break;
                        case "password":
                            password = value;
                            break;
                        case "role":
                            role = value;
                            break;
                        case "fullName":
                            fullName = value;
                            break;
                        case "email":
                            email = value;
                            break;
                        case "phone":
                            phone = value;
                            break;
                    }
                }
                if (!username.isEmpty()) {
                    users.add(new User(username, password, role, fullName, email, phone));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to hardcoded users if reading fails
            users.add(new User("admin", "admin123", "admin", "System Administrator", "admin@jetstream.com", "58000000"));
            users.add(new User("staff", "staff123", "staff", "Nthabiseng Khoarai", "nthabi@jetstream.com", "58550123"));
            users.add(new User("customer", "customer123", "customer", "Thabo Mokhoro", "thabo@example.com", "58941234"));
        }
    }

    public static boolean authenticate(String username, String password) {
        for (User user : users) {
            if (user.username.equals(username) && user.password.equals(password)) {
                return true;
            }
        }
        return false;
    }

    public static User getUser(String username) {
        for (User user : users) {
            if (user.username.equals(username)) {
                return user;
            }
        }
        return null;
    }
}

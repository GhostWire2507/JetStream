package com.jetstream.database;

import java.sql.Connection;

public class TestPostgresConnection {
    public static void main(String[] args) {
        try {
            System.out.println("Testing PostgreSQL connection...");
            Connection conn = PostgreConnector.getConnection();
            if (conn != null) {
                System.out.println("✓ Connection successful!");
                PostgreConnector.closeConnection();
            } else {
                System.err.println("✗ Connection failed!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package com.jetstream.database;

public class TestPostgresConnection {
    public static void main(String[] args) {
        try {
            PostgreConnector.connect();
            PostgreConnector.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package com.jetstream.database;

import com.jetstream.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;

public class MySQLConnector {

    private static Connection conn;

    public static void connect() throws Exception {
        String url = AppConfig.get("mysql.url");
        String user = AppConfig.get("mysql.user");
        String pass = AppConfig.get("mysql.password");
        if (url == null || user == null) throw new Exception("MySQL config missing");
        conn = DriverManager.getConnection(url, user, pass);
    }

    public static Connection getConnection() {
        return conn;
    }
}

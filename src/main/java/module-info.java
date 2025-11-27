module com.jetstream.jetstream {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.logging;

    // Export packages for FXML access
    exports com.jetstream.application;
    exports com.jetstream.controllers;
    exports com.jetstream.controller;

    exports com.jetstream.database;
    exports com.jetstream.models;
    exports com.jetstream.services;
    exports com.jetstream.utils;
    exports com.jetstream.manager;
    exports com.jetstream.config;

    // Open packages for FXML reflection
    opens com.jetstream.controllers to javafx.fxml;
    opens com.jetstream.application to javafx.fxml;
    opens com.jetstream.utils to javafx.fxml;
    opens com.jetstream.manager to javafx.fxml;
    opens com.jetstream.controller to javafx.fxml;
    // Open models to javafx.base for PropertyValueFactory and bindings
    opens com.jetstream.models to javafx.base;
}

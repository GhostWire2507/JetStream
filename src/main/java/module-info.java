module com.jetstream.jetstream {
    // JavaFX dependencies
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    // Database
    requires java.sql;

    // Open packages to JavaFX for reflection (FXML loading)
    opens com.jetstream.application to javafx.fxml;
    opens com.jetstream.controllers to javafx.fxml;
    opens com.jetstream.models to javafx.fxml, javafx.base;

    // Export packages
    exports com.jetstream.application;
    exports com.jetstream.controllers;
    exports com.jetstream.database;
    exports com.jetstream.services;
    exports com.jetstream.models;
    exports com.jetstream.utils;
    exports com.jetstream.config;
}

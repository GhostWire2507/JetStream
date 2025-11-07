module com.jetstream.jetstream {
    requires javafx.controls;
    requires javafx.fxml;
    //to acess java.sql classes like Connection, DriverManager
    requires java.sql;

    opens com.jetstream.jetstream to javafx.fxml;
    exports com.jetstream.application;
    opens com.jetstream.application to javafx.fxml;
    exports com.jetstream.controllers;
    opens com.jetstream.controllers to javafx.fxml;

    // for database or service packages
    exports com.jetstream.database;
    opens com.jetstream.database to javafx.fxml;
    exports com.jetstream.services;
    opens com.jetstream.services to javafx.fxml;
}

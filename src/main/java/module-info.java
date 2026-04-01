module com.example.mail_client {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens com.example.shared.data to com.fasterxml.jackson.databind;

    opens com.example.mail_client to javafx.fxml;
    exports com.example.mail_client;

    exports com.example.mail_client.app;
    opens com.example.mail_client.app to javafx.fxml;

    exports com.example.mail_client.model;
    opens com.example.mail_client.model to javafx.fxml;

    exports com.example.mail_client.controller;
    opens com.example.mail_client.controller to javafx.fxml;

    exports com.example.shared.data;
}
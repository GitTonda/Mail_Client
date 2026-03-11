module com.example.mail_client {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.mail_client to javafx.fxml;
    exports com.example.mail_client;
    exports com.example.mail_client.app;
    opens com.example.mail_client.app to javafx.fxml;
    exports com.example.mail_client.model;
    opens com.example.mail_client.model to javafx.fxml;
    exports com.example.mail_client.controller;
    opens com.example.mail_client.controller to javafx.fxml;
}
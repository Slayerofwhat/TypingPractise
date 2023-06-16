module com.example.monkeytype {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.monkeytype to javafx.fxml;
    exports com.example.monkeytype;
}
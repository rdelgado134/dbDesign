module com.db440 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    exports com.db440;
    opens com.db440 to javafx.fxml;
}

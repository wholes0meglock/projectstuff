module com.CurrencyConverter {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;

    opens com.CurrecyConverter to javafx.graphics;
    exports com.CurrecyConverter;
}
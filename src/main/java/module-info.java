module calc.calculadora {
    requires javafx.controls;
    requires javafx.fxml;


    opens calc.calculadora to javafx.fxml;
    exports calc.calculadora;
}
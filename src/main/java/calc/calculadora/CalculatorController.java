package calc.calculadora;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ResourceBundle;

public class CalculatorController implements Initializable {

    private double number1;
    private String operator = "";
    private boolean start = true;
    private boolean decimalSeparator = false;
    private static ServerSocket serverSocket;
    private static int portUsed = 6000;
    private static String result = "";

    @FXML
    private Label output;

    @FXML
    private TextArea calcLog;

    @FXML
    private void clearOutput() {
        decimalSeparator = false;
        output.setText("0.0");
        start = true;
        operator = "";
    }

    @FXML
    private void clearLog() {
        calcLog.setText("");
    }

    @FXML
    private void processNumPad(ActionEvent event) {
        String value = ((Button)event.getSource()).getText();
        if (!decimalSeparator || !value.equals(".")) {
            if (start) {
                output.setText("");
                start = false;
            }
            if (value.equals(".")) {
                decimalSeparator = true;
            }
            output.setText(output.getText() + value);
        }
    }

    @FXML
    private void processOperator(ActionEvent event) throws IOException {
        decimalSeparator = false;
        if (output.getText().equals("Error"))
            return;
        String value = ((Button)event.getSource()).getText();
        if (!value.equals("=")) {
            if (!operator.isEmpty()) {
                return;
            }
            operator = value;
            number1 = Double.parseDouble(output.getText());
            output.setText("");
        } else {
            if (operator.isEmpty()) { // Sin operador seleccionado
                return;
            }
            if (output.getText().isEmpty() || output.getText().equals(".") || "Error".equals(String.valueOf(number1))) { // Num1 seleccionado, operador seleccionado, num2 faltante o sólo "."
                output.setText("Error");
                operator = "";
                start = true;
                return;
            }
            calculate(number1, Double.parseDouble(output.getText()), operator); // Realizar operación
            operator = "";
            start = true;
        }
    }

    void initializeClients(int port) {
        // Tratar de inicializar el cliente en el puerto definido, si ya está usado, pasar al siguiente puerto
        try {
            serverSocket = new ServerSocket(port);
            calculate(0.0, 0.0, "+"); // Enviar un paquete al middleware para añadir su puerto a la lista de células
        } catch (BindException ex) {
            initializeClients(++portUsed);
        } catch (ConnectException ignored) {
                calcLog.appendText("Debe correr primero el middleware");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeClients(portUsed);
        receivePackage();
    }

    void receivePackage() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Package serverPackage = (Package) inputStream.readObject();
                    if (serverPackage.getPackageType() == 'S') {
                        result = String.valueOf(serverPackage.getResult());
                        Platform.runLater(() -> {
                            output.setText(result);
                            calcLog.appendText("Operación procesada por el servidor " + serverPackage.getEmisor() + "\n");
                            calcLog.appendText("Código de operación: " + serverPackage.getOperationCode() + "\n");
                            calcLog.appendText("Resultado: " + serverPackage.getResult() + "\n\n");
                        });
                    }
                    inputStream.close();
                    socket.close();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private static void calculate(double number1, double number2, String op) throws IOException {
        Package packageToServer = new Package('C', portUsed);

        packageToServer.setNum1(number1);
        packageToServer.setNum2(number2);

        switch (op) {
            case "+" -> packageToServer.setOperationCode(1);
            case "-" -> packageToServer.setOperationCode(2);
            case "⨉" -> packageToServer.setOperationCode(3);
            case "÷" -> packageToServer.setOperationCode(4);
        }

        Socket socket;
        socket = new Socket("localhost", 5000);
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.writeObject(packageToServer);
        outputStream.close();
        socket.close();
    }
}

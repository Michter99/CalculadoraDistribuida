package calc.calculadora;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ResourceBundle;

public class CalculatorController implements Initializable {

    private double number1;
    private String operator = "";
    private boolean start = true;
    private static ServerSocket serverSocket;
    private static int portUsed = 6000;
    private static String result = "";

    @FXML
    private Label output;

    @FXML
    private void clearOutput() {
        output.setText("0");
        start = true;
        operator = "";
    }

    @FXML
    private void processNumPad(ActionEvent event) {
        if (start) {
            output.setText("");
            start = false;
        }
        String value = ((Button)event.getSource()).getText();
        output.setText(output.getText() + value);
    }

    @FXML
    private void processOperator(ActionEvent event) {
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

    static void initializeClients(int port) {
        // Tratar de inicializar el cliente en el puerto definido, si ya está usado, pasar al siguiente puerto
        try {
            serverSocket = new ServerSocket(port);
            calculate(0.0, 0.0, "+"); // Enviar un paquete al middleware para añadir su puerto a la lista de células
        } catch (BindException ex) {
            initializeClients(++portUsed);
        } catch (ConnectException ignored) {
                System.out.println("Debe correr primero el middleware");
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
                    PackageToClient serverPackage = (PackageToClient) inputStream.readObject();
                    result = String.valueOf(serverPackage.getResult());
                    Platform.runLater(() -> output.setText(result));
                    System.out.println("Operación procesada por el servidor " + serverPackage.getEmisor());
                    System.out.println("Resultado: " + serverPackage.getResult() + "\n");
                    inputStream.close();
                    socket.close();
                } catch (ClassCastException ignored) {
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private static void calculate(double number1, double number2, String op) {
        PackageToServer packageToSend = new PackageToServer(number1, number2, portUsed);

        switch (op) {
            case "+" -> packageToSend.setOperationCode(1);
            case "-" -> packageToSend.setOperationCode(2);
            case "⨉" -> packageToSend.setOperationCode(3);
            case "÷" -> packageToSend.setOperationCode(4);
        }

        Socket socket;
        try {
            socket = new Socket("localhost", 5000);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(packageToSend);
            outputStream.close();
            socket.close();
        } catch (ConnectException ignored) {
            System.out.println("Debe correr primero el middleware");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

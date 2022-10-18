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
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class CalculatorController implements Initializable {

    private double number1;
    private String operator = "";
    private boolean start = true;
    private boolean decimalSeparator = false;
    private static ServerSocket serverSocket;
    private static int portUsed = 6000;
    private static String result = "";
    private static String huellaCelula = "";
    private static final Acuses acusesSum = new Acuses();
    private static final Acuses acusesRes = new Acuses();
    private static final Acuses acusesMul = new Acuses();
    private static final Acuses acusesDiv = new Acuses();

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
    private void processOperator(ActionEvent event) {
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

    void initializeClient() {
        // Tratar de inicializar el cliente en el puerto definido, si ya está usado, pasar al siguiente puerto
        while (true) {
            try {
                serverSocket = new ServerSocket(portUsed);
                huellaCelula = generateSHA(String.valueOf(portUsed));
                calculate(0.0, 0.0, "?"); // Enviar un paquete al middleware para añadir su puerto a la lista de células
                break;
            } catch (BindException ex) {
                portUsed++;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static String generateSHA(String input) {
        String sha1="";
        String value= String.valueOf(input);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(value.getBytes(StandardCharsets.UTF_8));
            sha1 = String.format("%040x", new BigInteger(1, digest.digest()));
        } catch (Exception e){
            e.printStackTrace();
        }
        return sha1;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeClient();
        receivePackage();
    }

    void receivePackage() {
        new Thread(() -> {
            String eventoAnterior = "";
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Package serverPackage = (Package) inputStream.readObject();
                    if (!eventoAnterior.equals(serverPackage.evento)) {
                        if (serverPackage.getPackageType() == 'S' && serverPackage.getOperationCode() != 0) {
                            if (serverPackage.recognizedOp) {
                                result = String.valueOf(serverPackage.getResult());
                                Platform.runLater(() -> {
                                    output.setText(result);
                                    calcLog.appendText("Operación procesada por el servidor " + serverPackage.getEmisor() + "\n");
                                    calcLog.appendText("Código de operación: " + serverPackage.getOperationCode() + "\n");
                                    calcLog.appendText("Resultado: " + serverPackage.getResult() + "\n\n");
                                });
                                eventoAnterior = serverPackage.evento;
                            } else { // Si no se ha cumplido el mínimo de acuses, reenviar paquete
                                // TimeUnit.SECONDS.sleep(1);
                                switch (serverPackage.getOperationCode()) {
                                    case 1 -> acusesSum.verificaAcuse(serverPackage);
                                    case 2 -> acusesRes.verificaAcuse(serverPackage);
                                    case 3 -> acusesMul.verificaAcuse(serverPackage);
                                    case 4 -> acusesDiv.verificaAcuse(serverPackage);
                                }
                                sendPackage(serverPackage);
                            }
                        }
                    }
                    inputStream.close();
                    socket.close();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private static void calculate(double number1, double number2, String op) {
        Package packageToServer = new Package('C', portUsed);

        packageToServer.setNum1(number1);
        packageToServer.setNum2(number2);
        packageToServer.setLastTypeOfEmisor('C');
        packageToServer.huella = huellaCelula;
        packageToServer.evento = generateSHA(System.currentTimeMillis() + huellaCelula);
        packageToServer.recognizedOp = false;

        switch (op) {
            case "+" -> packageToServer.setOperationCode(1);
            case "-" -> packageToServer.setOperationCode(2);
            case "⨉" -> packageToServer.setOperationCode(3);
            case "÷" -> packageToServer.setOperationCode(4);
            default -> packageToServer.setOperationCode(0);
        }

        int nodePort = 5000;

        while (true) {
            try {
                Socket socket = new Socket("localhost", nodePort);
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject(packageToServer);
                outputStream.close();
                socket.close();
                nodePort++;
            } catch (ConnectException e) {
                if (nodePort == 5020) // Limite de 20 nodos
                    break;
                nodePort++;
            } catch (IOException ignored) {}
        }
    }

    public static void sendPackage(Package packageToSend) {
        int nodePort = 5000;
        while (true) {
            try {
                Socket socket = new Socket("localhost", nodePort);
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                packageToSend.setLastTypeOfEmisor('C');
                packageToSend.setEmisor(portUsed);
                packageToSend.setPackageType('C');
                packageToSend.huella = huellaCelula;
                outputStream.writeObject(packageToSend);
                outputStream.close();
                socket.close();
                nodePort++;
            } catch (ConnectException e) {
                if (nodePort == 5020) // Limite de 20 nodos
                    break;
                nodePort++;
            } catch (IOException ignored) {}
        }
    }
}


class Acuses {

    public record registroAcuse(int operationCode, double num1, double num2, Set<String> huellas){}
    public HashMap<String, registroAcuse> tuplas = new HashMap<>();

    public boolean verificaAcuse(Package serverPackage) {
        if (tuplas.containsKey(serverPackage.evento)) {
            tuplas.get(serverPackage.evento).huellas.add(serverPackage.huella);
            if (tuplas.get(serverPackage.evento).operationCode == 1 && tuplas.get(serverPackage.evento).huellas.size() >= 3 ||  // 3 servidores suma
                tuplas.get(serverPackage.evento).operationCode == 2 && tuplas.get(serverPackage.evento).huellas.size() >= 2 ||  // 2 servidores resta
                tuplas.get(serverPackage.evento).operationCode == 3 && tuplas.get(serverPackage.evento).huellas.size() >= 1 ||  // 1 servidores mult
                tuplas.get(serverPackage.evento).operationCode == 4 && tuplas.get(serverPackage.evento).huellas.size() >= 2)    // 3 servidores div
            {
                serverPackage.recognizedOp = true;
                tuplas.remove(serverPackage.evento);
                return true;
            }
            serverPackage.recognizedOp = false;
            return false;
        } else {
            tuplas.put(serverPackage.evento, new registroAcuse(serverPackage.getOperationCode(), serverPackage.getNum1(), serverPackage.getNum2(), new HashSet<>()));
            return verificaAcuse(serverPackage);
        }
    }

}

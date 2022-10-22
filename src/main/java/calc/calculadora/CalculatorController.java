package calc.calculadora;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
    private static int connectedNode = 5000;
    private static String footprint = "";
    private static final ArrayList<Package> sumas =  new ArrayList<>();
    private static final ArrayList<Package> restas = new ArrayList<>();
    private static final ArrayList<Package> mults = new ArrayList<>();
    private static final ArrayList<Package> divs = new ArrayList<>();
    private static final HashMap<String, Set<String>> acuses = new HashMap<>();

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

    private void initializeClients() {
        while (true) {
            try {
                serverSocket = new ServerSocket(portUsed);
                footprint = String.valueOf(portUsed);
                sendPackage(new Package('C', portUsed));
                break;
            } catch (Exception ex) {
                portUsed++;
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeClients();
        receivePackage();
    }

    void receivePackage() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Package serverPackage = (Package) inputStream.readObject();
                    if (serverPackage.getPackageType() == 'S' && serverPackage.getOperationCode() != 0) {
                        agregaAcuse(serverPackage);
                        if (verificaAcuse(serverPackage)) {
                            sendRecognizedOperations(serverPackage.getOperationCode());
                        } else if (!serverPackage.isRecognizedOp()) {
                            sendPackage(serverPackage);
                        }
                        if (serverPackage.isProccesedByServer()) {
                            result = String.valueOf(serverPackage.getResult());
                            Platform.runLater(() -> {
                                output.setText(result);
                                calcLog.appendText("Operación procesada por el servidor " + serverPackage.getEmisor() + "\n");
                                calcLog.appendText("Código de operación: " + serverPackage.getOperationCode() + "\n");
                                calcLog.appendText("Resultado: " + serverPackage.getResult() + "\n\n");
                            });
                        }
                    }
                    inputStream.close();
                    socket.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private static void sendRecognizedOperations(int operationCode) {
        new Thread(() -> {
            switch (operationCode) {
                case 1 -> {
                    for (Package packageToServer : sumas) {
                        packageToServer.setRecognizedOp(true);
                        sendPackage(packageToServer);
                    }
                    sumas.clear();
                }
                case 2 -> {
                    for (Package packageToServer : restas) {
                        packageToServer.setRecognizedOp(true);
                        sendPackage(packageToServer);
                    }
                    restas.clear();
                }
                case 3 -> {
                    for (Package packageToServer : mults) {
                        packageToServer.setRecognizedOp(true);
                        sendPackage(packageToServer);
                    }
                    mults.clear();
                }
                case 4 -> {
                    for (Package packageToServer : divs) {
                        packageToServer.setRecognizedOp(true);
                        sendPackage(packageToServer);
                    }
                    divs.clear();
                }
            }
        }).start();
    }

    private static void calculate(double number1, double number2, String op) {
        Package packageToServer = new Package('C', portUsed);

        packageToServer.setNum1(number1);
        packageToServer.setNum2(number2);
        packageToServer.setEvent(generateSHA(System.currentTimeMillis() + footprint));
        packageToServer.setRecognizedOp(false);

        switch (op) {
            case "+" -> packageToServer.setOperationCode(1);
            case "-" -> packageToServer.setOperationCode(2);
            case "⨉" -> packageToServer.setOperationCode(3);
            case "÷" -> packageToServer.setOperationCode(4);
        }

        switch (packageToServer.getOperationCode()) {
            case 1 -> sumas.add(packageToServer);
            case 2 -> restas.add(packageToServer);
            case 3 -> mults.add(packageToServer);
            case 4 -> divs.add(packageToServer);
        }

        sendPackage(packageToServer);
    }

    static void sendPackage(Package packageToServer) {
        new Thread(() -> {
            while (true) {
                try {
                    packageToServer.setLastTypeOfEmisor('C');
                    packageToServer.setFootprint(footprint);
                    packageToServer.setPackageType('C');
                    packageToServer.setEmisor(portUsed);
                    Socket socket = new Socket("localhost", connectedNode);
                    ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                    outputStream.writeObject(packageToServer);
                    outputStream.close();
                    socket.close();
                    break;
                } catch (Exception e) {
                    connectedNode++;
                    if (connectedNode == 5020)
                        connectedNode = 5000;
                }
            }
        }).start();
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

    private static void agregaAcuse(Package serverPackage) {
        if (acuses.containsKey(serverPackage.getEvent()))
            acuses.get(serverPackage.getEvent()).add(serverPackage.getFootprint());
        else
            acuses.put(serverPackage.getEvent(), new HashSet<>(Collections.singletonList(serverPackage.getFootprint())));
    }

    private static boolean verificaAcuse(Package serverPackage) {
        String event = serverPackage.getEvent();
        int minSum = 3;
        int minRes = 2;
        int minMult = 1;
        int minDiv = 2;

        switch (serverPackage.getOperationCode()) {
            case 1:
                if (acuses.get(event).size() >= minSum) {
                    serverPackage.setRecognizedOp(true);
                    return true;
                }
                break;
            case 2:
                if (acuses.get(event).size() >= minRes) {
                    serverPackage.setRecognizedOp(true);
                    return true;
                }
                break;
            case 3:
                if (acuses.get(event).size() >= minMult) {
                    serverPackage.setRecognizedOp(true);
                    return true;
                }
                break;
            case 4:
                if (acuses.get(event).size() >= minDiv) {
                    serverPackage.setRecognizedOp(true);
                    return true;
                }
                break;
        }
        return false;
    }
}

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
import java.util.concurrent.TimeUnit;

public class ClientController implements Initializable {

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
    private static final Set<String> acusesSuma = new HashSet<>();
    private static final Set<String> acusesResta = new HashSet<>();
    private static final Set<String> acusesMult = new HashSet<>();
    private static final Set<String> acusesDiv = new HashSet<>();
    private static final Set<String> eventosEnCiclo = new HashSet<>();
    private static final Set<String> eventosProcesados = new HashSet<>();
    private static int ultimoNumeroAcusesSum = 0;
    private static int ultimoNumeroAcusesRes = 0;
    private static int ultimoNumeroAcusesMul = 0;
    private static int ultimoNumeroAcusesDiv = 0;
    private static int minSum = 3;
    private static int minRes = 1;
    private static int minMult = 1;
    private static int minDiv = 1;

    @FXML
    private Label output;

    @FXML
    private TextArea calcLog;

    @FXML
    private void clearOutput() {
        Platform.runLater(() -> {
            decimalSeparator = false;
            output.setText("0.0");
            start = true;
            operator = "";
        });
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
        System.out.println("Client " + portUsed);
        receivePackage();
    }

    void receivePackage() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Package serverPackage = (Package) inputStream.readObject();
                    if (serverPackage.getPackageType() == 'S' && serverPackage.getOperationCode() != 0 && serverPackage.getOriginalEmisor() == portUsed) {
                        if (serverPackage.isProccesedByServer() && !eventosProcesados.contains(serverPackage.getEvent())) {
                            eventosProcesados.add(serverPackage.getEvent());
                            result = String.valueOf(serverPackage.getResult());
                            String symbol = switch (serverPackage.getOperationCode()) {
                                case 1 -> "+";
                                case 2 -> "-";
                                case 3 -> "*";
                                case 4 -> "/";
                                default -> "";
                            };
                            Platform.runLater(() -> {
                                output.setText(result);
                                calcLog.appendText("Código de operación: " + serverPackage.getOperationCode() + "\n");
                                calcLog.appendText(serverPackage.getNum1() + " " + symbol + " " + serverPackage.getNum2() + " = " + serverPackage.getResult() + "\n\n");
                            });
                        } else {
                            addFootprint(serverPackage);
                            verificaAcuse(serverPackage);
                        }
                    } else if (serverPackage.getPackageType() == 'A') {
                        minSum = serverPackage.getAcusesSuma();
                        minRes = serverPackage.getAcusesResta();
                        minMult = serverPackage.getAcusesMult();
                        minDiv = serverPackage.getAcusesDiv();
                    }
                    inputStream.close();
                    socket.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private static void sendRecognizedOperations(int operationCode) throws InterruptedException {
        TimeUnit.SECONDS.sleep(4);
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
    }

    private static void calculate(double number1, double number2, String op) {
        acusesSuma.clear();
        acusesResta.clear();
        acusesMult.clear();
        acusesDiv.clear();

        Package packageToServer = new Package('C', portUsed);

        packageToServer.setNum1(number1);
        packageToServer.setNum2(number2);
        packageToServer.setEvent(generateSHA(System.currentTimeMillis() + footprint));
        packageToServer.setRecognizedOp(false);
        packageToServer.setOriginalEmisor(portUsed);
        packageToServer.setClonePort(0);

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

    private static void addFootprint(Package serverPackage) {
        switch (serverPackage.getOperationCode()) {
            case 1 -> acusesSuma.add(serverPackage.getFootprint());
            case 2 -> acusesResta.add(serverPackage.getFootprint());
            case 3 -> acusesMult.add(serverPackage.getFootprint());
            case 4 -> acusesDiv.add(serverPackage.getFootprint());
        }
    }

    private static int selectCloningServer(Set<String> acuses) {
        int valorMin = 65535;
        for (String acuse : acuses) {
            if (Integer.parseInt(acuse) < valorMin)
                valorMin = Integer.parseInt(acuse);
        }
        return valorMin;
    }

    private static void verificaAcuse(Package serverPackage) {
        int sleepTime = 2;

        if (eventosEnCiclo.contains(serverPackage.getEvent()))
            return;
        eventosEnCiclo.add(serverPackage.getEvent());
        new Thread(() -> {
            try {
                switch (serverPackage.getOperationCode()) {
                    case 1 -> {
                        while (acusesSuma.size() < minSum) {
                            if (ultimoNumeroAcusesSum == acusesSuma.size()) {
                                Package clonePackage = new Package('C', portUsed);
                                clonePackage.setClonePort(selectCloningServer(acusesSuma));
                                clonePackage.setOperationCode(1);
                                sendPackage(clonePackage);
                            }
                            ultimoNumeroAcusesSum = acusesSuma.size();
                            TimeUnit.SECONDS.sleep(sleepTime);
                            sendPackage(serverPackage);
                        }
                        ultimoNumeroAcusesSum = 0;
                        sendRecognizedOperations(1);
                    }
                    case 2 -> {
                        while (acusesResta.size() < minRes) {
                            if (ultimoNumeroAcusesRes == acusesResta.size()) {
                                Package clonePackage = new Package('C', portUsed);
                                clonePackage.setClonePort(selectCloningServer(acusesResta));
                                clonePackage.setOperationCode(2);
                                sendPackage(clonePackage);
                            }
                            ultimoNumeroAcusesRes = acusesResta.size();
                            TimeUnit.SECONDS.sleep(sleepTime);
                            sendPackage(serverPackage);
                        }
                        ultimoNumeroAcusesRes = 0;
                        sendRecognizedOperations(2);
                    }
                    case 3 -> {
                        while (acusesMult.size() < minMult) {
                            if (ultimoNumeroAcusesMul == acusesMult.size()) {
                                Package clonePackage = new Package('C', portUsed);
                                clonePackage.setClonePort(selectCloningServer(acusesMult));
                                clonePackage.setOperationCode(3);
                                sendPackage(clonePackage);
                            }
                            ultimoNumeroAcusesMul = acusesMult.size();
                            TimeUnit.SECONDS.sleep(sleepTime);
                            sendPackage(serverPackage);
                        }
                        ultimoNumeroAcusesMul = 0;
                        sendRecognizedOperations(3);
                    }
                    case 4 -> {
                        while (acusesDiv.size() < minDiv) {
                            if (ultimoNumeroAcusesDiv == acusesDiv.size()) {
                                Package clonePackage = new Package('C', portUsed);
                                clonePackage.setClonePort(selectCloningServer(acusesDiv));
                                clonePackage.setOperationCode(4);
                                sendPackage(clonePackage);
                            }
                            ultimoNumeroAcusesDiv = acusesDiv.size();
                            TimeUnit.SECONDS.sleep(sleepTime);
                            sendPackage(serverPackage);
                        }
                        ultimoNumeroAcusesDiv = 0;
                        sendRecognizedOperations(4);
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }
}

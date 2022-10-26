package calc.calculadora;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.*;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

public class ServerController implements Initializable {

    @FXML
    private TextArea calcLog;

    @FXML
    void clearLog() {
        calcLog.setText("");
    }

    private static ServerSocket serverSocket;
    private static int portUsed = 7000;
    private static int connectedNode = 5000;
    private static String footprint = "";
    private static final Set<String> processedEvents = new HashSet<>();
    

    void receivePackage() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Package clientPackage = (Package) inputStream.readObject();
                    if (clientPackage.getPackageType() == 'C') {
                        if (clientPackage.isRecognizedOp()) {
                            if (!processedEvents.contains(clientPackage.getEvent()))
                                processOperation(clientPackage);
                            else
                                continue;
                        }
                        else
                            sendProcessedPackage(clientPackage);
                    }
                    inputStream.close();
                    socket.close();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    void processOperation(Package receivedPackage) throws IOException {
        double number1 = receivedPackage.getNum1();
        double number2 = receivedPackage.getNum2();
        int op = receivedPackage.getOperationCode();
        double result;

        try {
            result = switch (op) {
                case 1 -> sumMicroService(number1, number2);
                case 2 -> subMicroService(number1, number2);
                case 3 -> multMicroService(number1, number2);
                case 4 -> divMicroService(number1, number2);
                default -> 0;
            };
        } catch (Exception e) {
            receivedPackage.setProccesedByServer(false);
            return;
        }

        String operator = switch (op) {
            case 1 -> "+";
            case 2 -> "-";
            case 3 -> "*";
            case 4 -> "/";
            default -> "";
        };

        double finalResult = result;
        Platform.runLater(() -> {
            calcLog.appendText("Solicitud procesada por el servidor " + portUsed + "\n");
            calcLog.appendText("Código de operación: " + op + "\n");
            calcLog.appendText(number1 + " " + operator + " " + number2 + " = " + finalResult + "\n\n");
        });

        receivedPackage.setResult(result);
        receivedPackage.setProccesedByServer(true);
        processedEvents.add(receivedPackage.getEvent());
        sendProcessedPackage(receivedPackage);
    }

    private double sumMicroService(double num1, double num2) throws Exception {
        double result;
        File dir = new File("D:\\Suma.jar");
        Class<?> cls = new URLClassLoader(new URL[] { dir.toURI().toURL() }).loadClass("Suma");
        Method sumMethod = cls.getMethod("sumar", double.class, double.class);
        Object objInstance = cls.getDeclaredConstructor().newInstance();
        result = (double)sumMethod.invoke(objInstance, num1, num2);
        return result;
    }

    private double subMicroService(double num1, double num2) throws Exception {
        double result;
        File dir = new File("D:\\Resta.jar");
        Class<?> cls = new URLClassLoader(new URL[] { dir.toURI().toURL() }).loadClass("Resta");
        Method subMethod = cls.getMethod("restar", double.class, double.class);
        Object objInstance = cls.getDeclaredConstructor().newInstance();
        result = (double)subMethod.invoke(objInstance, num1, num2);
        return result;
    }

    private double multMicroService(double num1, double num2) throws Exception {
        double result;
        File dir = new File("D:\\Multiplicacion.jar");
        Class<?> cls = new URLClassLoader(new URL[] { dir.toURI().toURL() }).loadClass("Multiplicacion");
        Method multMethod = cls.getMethod("multiplicar", double.class, double.class);
        Object objInstance = cls.getDeclaredConstructor().newInstance();
        result = (double)multMethod.invoke(objInstance, num1, num2);
        return result;
    }

    private double divMicroService(double num1, double num2) throws Exception {
        double result;
        File dir = new File("D:\\Division.jar");
        Class<?> cls = new URLClassLoader(new URL[] { dir.toURI().toURL() }).loadClass("Division");
        Method divMethod = cls.getMethod("dividir", double.class, double.class);
        Object objInstance = cls.getDeclaredConstructor().newInstance();
        result = (double)divMethod.invoke(objInstance, num1, num2);
        return result;
    }

    static void sendProcessedPackage(Package packageToClient) {
        new Thread(() -> {
            while (true) {
                try {
                    packageToClient.setPackageType('S');
                    packageToClient.setLastTypeOfEmisor('S');
                    packageToClient.setEmisor(portUsed);
                    packageToClient.setFootprint(footprint);
                    Socket socketSender = new Socket("localhost", connectedNode);
                    ObjectOutputStream outputStream = new ObjectOutputStream(socketSender.getOutputStream());
                    outputStream.writeObject(packageToClient);
                    socketSender.close();
                    break;
                } catch (Exception ignored) {
                    connectedNode++;
                    if (connectedNode == 5020)
                        connectedNode = 5000;
                }
            }
        }).start();
    }

    void initializeServers() {
        while (true) {
            try {
                serverSocket = new ServerSocket(portUsed);
                footprint = String.valueOf(portUsed);
                Package temp = new Package('S', portUsed);
                temp.setOperationCode(0);
                sendProcessedPackage(temp);
                break;
            } catch (Exception ex) {
                portUsed++;
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeServers();
        receivePackage();
    }


}
package calc.calculadora;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ResourceBundle;

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
    

    void receivePackage() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Package clientPackage = (Package) inputStream.readObject();
                    if (clientPackage.getPackageType() == 'C') {
                        if (clientPackage.isRecognizedOp())
                            processOperation(clientPackage);
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

        double result = switch (op) {
            case 1 -> number1 + number2;
            case 2 -> number1 - number2;
            case 3 -> number1 * number2;
            case 4 -> number1 / number2;
            default -> 0.0;
        };

        String operator = switch (op) {
            case 1 -> "+";
            case 2 -> "-";
            case 3 -> "*";
            case 4 -> "/";
            default -> "";
        };

        Platform.runLater(() -> {
            calcLog.appendText("Solicitud procesada por el servidor " + portUsed + "\n");
            calcLog.appendText("Código de operación: " + op + "\n");
            calcLog.appendText(number1 + " " + operator + " " + number2 + " = " + result + "\n\n");
        });

        receivedPackage.setResult(result);
        sendProcessedPackage(receivedPackage);
    }

    static void sendProcessedPackage(Package packageToClient) throws IOException {
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
            } catch (ConnectException ignored) {
                connectedNode++;
                if (connectedNode == 5020)
                    connectedNode = 5000;
            }
        }
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
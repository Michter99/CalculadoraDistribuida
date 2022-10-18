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
    static int portUsed = 7000;
    String eventoAnterior = "";

    void receivePackage() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Package serverPackage = (Package) inputStream.readObject();
                    if (serverPackage.getPackageType() == 'C' && serverPackage.recognizedOp && !eventoAnterior.equals(serverPackage.evento)) {
                        processOperation(serverPackage);
                    } else if (serverPackage.getPackageType() == 'C') {
                        serverPackage.huella = String.valueOf(portUsed);
                        serverPackage.setPackageType('S');
                        serverPackage.setEmisor(portUsed);
                        serverPackage.setLastTypeOfEmisor('S');
                        sendProcessedPackage(serverPackage);
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

        if (!eventoAnterior.equals(receivedPackage.evento)) {
            Platform.runLater(() -> {
                calcLog.appendText("Solicitud procesada por el servidor " + portUsed + "\n");
                calcLog.appendText("Código de operación: " + op + "\n");
                calcLog.appendText(number1 + " " + operator + " " + number2 + " = " + result + "\n\n");
            });
        }

        eventoAnterior = receivedPackage.evento;

        receivedPackage.setResult(result);
        receivedPackage.setPackageType('S');
        receivedPackage.setEmisor(portUsed);
        receivedPackage.setLastTypeOfEmisor('S');
        receivedPackage.huella = String.valueOf(portUsed);

        sendProcessedPackage(receivedPackage);
    }

    void sendProcessedPackage(Package packageToClient) {
        int nodePort = 5000;
        while (true) {
            try {
                Socket socketSender = new Socket("localhost", nodePort);
                ObjectOutputStream outputStream = new ObjectOutputStream(socketSender.getOutputStream());
                outputStream.writeObject(packageToClient);
                socketSender.close();
                nodePort++;
            } catch (ConnectException e) {
                if (nodePort == 5020) // Limite de 20 nodos
                    break;
                nodePort++;
            } catch (IOException ignored) {}
        }
    }

    void initializeServer() {
        // Tratar de inicializar el servidor en el puerto definido, si ya está usado, pasar al siguiente puerto
        while (true) {
            try {
                serverSocket = new ServerSocket(portUsed);
                Package temp = new Package('S', portUsed);
                temp.setLastTypeOfEmisor('S');
                temp.setOperationCode(0);
                sendProcessedPackage(temp); // Enviar un paquete al middleware para añadir su puerto a la lista de células
                break;
            } catch (BindException ex) {
                portUsed++;
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeServer();
        receivePackage();
    }
}

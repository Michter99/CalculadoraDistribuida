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

    void receivePackage() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Package serverPackage = (Package) inputStream.readObject();
                    if (serverPackage.getPackageType() == 'C') {
                        processOperation(serverPackage);
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
        receivedPackage.setPackageType('S');
        receivedPackage.setEmisor(portUsed);

        sendProcessedPackage(receivedPackage);
    }

    static void sendProcessedPackage(Package packageToClient) throws IOException {
        Socket socketSender = new Socket("localhost", 5000);
        ObjectOutputStream outputStream = new ObjectOutputStream(socketSender.getOutputStream());
        outputStream.writeObject(packageToClient);
        socketSender.close();
    }

    void initializeServers(int port) {
        // Tratar de inicializar el servidor en el puerto definido, si ya está usado, pasar al siguiente puerto
        try {
            serverSocket = new ServerSocket(port);
            sendProcessedPackage(new Package('S', portUsed)); // Enviar un paquete al middleware para añadir su puerto a la lista de células
        } catch (BindException ex) {
            initializeServers(++portUsed);
        } catch (ConnectException ignored) {
            calcLog.appendText("Debe correr primero el middleware");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeServers(portUsed);
        receivePackage();
    }
}

package calc.calculadora;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/*
    Middleware  (5000 - 5999)
    Clientes    (6000 - 6999)
    Servidores  (7000 - 7999)
 */

public class MiddlewareController implements Initializable {

    private static ServerSocket middlewareSocket;
    private static int portUsed = 5000;
    private static final HashMap<Integer, Character> connections = new HashMap<>();
    public Label nodeName;

    @FXML
    private TextArea calcLog;

    @FXML
    private void clearLog() {
        calcLog.setText("");
        for (Map.Entry<Integer, Character> connection: connections.entrySet()) {
            System.out.println(connection.getKey());
        }
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        while (true) {
            try {
                middlewareSocket = new ServerSocket(portUsed);
                nodeName.setText("Node " + portUsed);
                for (Map.Entry<Integer, Character> connection: connections.entrySet()) {
                    Socket initialSocket = new Socket("localhost", connection.getKey());
                    ObjectOutputStream outputStream = new ObjectOutputStream(initialSocket.getOutputStream());
                    outputStream.writeObject(new Package('M', portUsed));
                    initialSocket.close();
                }
                break;
            } catch (Exception ex) {
                connections.put(portUsed, 'M');
                portUsed++;
            }
        }
        receiveAndResendPackage();
    }

    void receiveAndResendPackage() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = middlewareSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Package packageData = (Package) inputStream.readObject();
                    socket.close();

                    connections.put(packageData.getEmisor(), packageData.getPackageType());

                    if (packageData.recognizedOp)
                        Platform.runLater(() -> {
                            calcLog.appendText("Paquete recibido de " + packageData.getEmisor() + "\n");
                            calcLog.appendText("Código de operación: " + packageData.getOperationCode() + "\n\n");
                        });


                    // Realizar el broadcast menos al emisor, itirando por el diccionario de puertos conectados
                    for (Map.Entry<Integer, Character> connection: connections.entrySet()) {
                        if (connection.getKey() != packageData.getEmisor()) {
                            try {
                                if (packageData.getLastTypeOfEmisor() != 'M' && connection.getValue() == 'M') {
                                    Socket socketReceiver = new Socket("localhost", connection.getKey());
                                    ObjectOutputStream outputStream = new ObjectOutputStream(socketReceiver.getOutputStream());
                                    char tempLastEmisor = packageData.getLastTypeOfEmisor();
                                    packageData.setLastTypeOfEmisor('M');
                                    outputStream.writeObject(packageData);
                                    packageData.setLastTypeOfEmisor(tempLastEmisor);
                                    socketReceiver.close();
                                } else if (connection.getValue() != 'M') {
                                    Socket socketReceiver = new Socket("localhost", connection.getKey());
                                    ObjectOutputStream outputStream = new ObjectOutputStream(socketReceiver.getOutputStream());
                                    char tempLastEmisor = packageData.getLastTypeOfEmisor();
                                    packageData.setLastTypeOfEmisor('M');
                                    outputStream.writeObject(packageData);
                                    packageData.setLastTypeOfEmisor(tempLastEmisor);
                                    socketReceiver.close();
                                }
                            } catch (ConnectException ignored) {}
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}

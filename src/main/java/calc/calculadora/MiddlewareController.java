package calc.calculadora;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

/*
    Middleware  5000 - 5001
    Servidores  (7000 - 7999)
    Clientes    (6000 - 6999)
 */

public class MiddlewareController implements Initializable {

    @FXML
    private TextArea calcLog;

    @FXML
    private void clearLog() {
        calcLog.setText("");
    }

    private static ServerSocket serverSocketToServer;
    private static ServerSocket serverSocketToClient;
    private static final ArrayList<Integer> cells = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            serverSocketToServer = new ServerSocket(5000);
            serverSocketToClient = new ServerSocket(5001);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        receiveAndResendPackageToServer();
        receiveAndResendPackageToClient();
    }

    void receiveAndResendPackageToServer() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocketToServer.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    PackageToServer serverPackage = (PackageToServer) inputStream.readObject();

                    socket.close();

                    // Analizar la solicitud para ver si el puerto ya se encuentra en la lista de células
                    if (!cells.contains(serverPackage.getEmisor())) {
                        cells.add(serverPackage.getEmisor());
                    }

                    Platform.runLater(() -> {
                        calcLog.appendText("Paquete recibido del cliente " + serverPackage.getEmisor() + "\n");
                        calcLog.appendText("Código de operación: " + serverPackage.getOperationCode() + "\n\n");
                    });


                    // Realizar el broadcast menos al emisor
                    for (int i : cells) {
                        if (i != serverPackage.getEmisor()) {
                            try {
                                Socket socketReceiver = new Socket("localhost", i);
                                ObjectOutputStream outputStream = new ObjectOutputStream(socketReceiver.getOutputStream());
                                outputStream.writeObject(serverPackage);
                                System.out.println("Paquete reenviado a " + i);
                                socketReceiver.close();
                            } catch (ConnectException ignored) {}
                        }
                    }
                } catch (IOException | ClassNotFoundException e ) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    void receiveAndResendPackageToClient() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocketToClient.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    PackageToClient clientPackage = (PackageToClient) inputStream.readObject();

                    socket.close();

                    // Analizar la solicitud para ver si el puerto ya se encuentra en la lista de células
                    if (!cells.contains(clientPackage.getEmisor())) {
                        cells.add(clientPackage.getEmisor());
                    }

                    Platform.runLater(() -> {
                        calcLog.appendText("Paquete recibido del servidor " + clientPackage.getEmisor() + "\n");
                        calcLog.appendText("Código de operación: " + clientPackage.getOperationCode() + "\n\n");
                    });


                    // Realizar el broadcast menos al emisor
                    for (int i : cells) {
                        if (i != clientPackage.getEmisor()) {
                            try {
                                Socket socketReceiver = new Socket("localhost", i);
                                ObjectOutputStream outputStream = new ObjectOutputStream(socketReceiver.getOutputStream());
                                outputStream.writeObject(clientPackage);
                                System.out.println("Paquete reenviado a " + i);
                                socketReceiver.close();
                            } catch (ConnectException ignored) {}
                        }
                    }
                } catch (IOException | ClassNotFoundException e ) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}

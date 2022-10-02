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

    private static ServerSocket middlewareSocketToServer;
    private static final ArrayList<Integer> cells = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            middlewareSocketToServer = new ServerSocket(5000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        receiveAndResendPackage();
    }

    void receiveAndResendPackage() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = middlewareSocketToServer.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Package packageData = (Package) inputStream.readObject();

                    socket.close();

                    // Analizar la solicitud para ver si el puerto ya se encuentra en la lista de células
                    if (!cells.contains(packageData.getEmisor())) {
                        cells.add(packageData.getEmisor());
                    }

                    Platform.runLater(() -> {
                        calcLog.appendText("Paquete recibido de " + packageData.getEmisor() + "\n");
                        calcLog.appendText("Código de operación: " + packageData.getOperationCode() + "\n\n");
                    });


                    // Realizar el broadcast menos al emisor
                    for (int i : cells) {
                        if (i != packageData.getEmisor()) {
                            try {
                                Socket socketReceiver = new Socket("localhost", i);
                                ObjectOutputStream outputStream = new ObjectOutputStream(socketReceiver.getOutputStream());
                                outputStream.writeObject(packageData);
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

package calc.calculadora;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

/*
    Middleware  5000 - 5001
    Servidores  (7000 - 7999)
    Clientes    (6000 - 6999)
 */

public class MiddlewareController implements Initializable {

    @FXML
    public Label nodeName;
    @FXML
    private TextArea calcLog;

    @FXML
    private void clearLog() {
        calcLog.setText("");
    }

    private static ServerSocket middlewareSocket;
    private static int portUsed = 5000;
    private static final Set<Integer> cells = new HashSet<>();
    private static final Set<Integer> nodes = new HashSet<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeMiddlewares();
        receiveAndResendPackage();
    }

    private void initializeMiddlewares() {
        while (true) {
            try {
                middlewareSocket = new ServerSocket(portUsed);
                nodeName.setText("Node " +  portUsed);
                for (int port : nodes) {
                    Socket socket = new Socket("localhost", port);
                    ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                    outputStream.writeObject(new Package('M', portUsed));
                    socket.close();
                }
                break;
            } catch (Exception e) {
                nodes.add(portUsed);
                portUsed++;
            }
        }
    }

    void receiveAndResendPackage() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = middlewareSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Package packageData = (Package) inputStream.readObject();
                    socket.close();

                    if (packageData.getPackageType() == 'M')
                        nodes.add(packageData.getEmisor());
                    else
                        cells.add(packageData.getEmisor());

                    if (packageData.isRecognizedOp())
                        Platform.runLater(() -> {
                            calcLog.appendText("Paquete recibido de " + packageData.getEmisor() + "\n");
                            calcLog.appendText("Código de operación: " + packageData.getOperationCode() + "\n\n");
                        });


                    if (packageData.getLastTypeOfEmisor() == 'M') { // Si viene de nodo, envías a las celulas conectadas
                        for (int cell : cells) {
                            if (cell != packageData.getEmisor()) {
                                try {
                                    Socket socketReceiver = new Socket("localhost", cell);
                                    ObjectOutputStream outputStream = new ObjectOutputStream(socketReceiver.getOutputStream());
                                    packageData.setLastTypeOfEmisor('M');
                                    outputStream.writeObject(packageData);
                                    socketReceiver.close();
                                } catch (ConnectException ignored) {}
                            }
                        }
                    } else if (packageData.getPackageType() != 'M') { // Si viene de celulas, envías a celulas y nodos
                        for (int node : nodes) {
                            if (node != packageData.getEmisor()) {
                                try {
                                    Socket socketReceiver = new Socket("localhost", node);
                                    ObjectOutputStream outputStream = new ObjectOutputStream(socketReceiver.getOutputStream());
                                    packageData.setLastTypeOfEmisor('M');
                                    outputStream.writeObject(packageData);
                                    socketReceiver.close();
                                } catch (ConnectException ignored) {}
                            }
                        }
                        for (int cell : cells) {
                            if (cell != packageData.getEmisor()) {
                                try {
                                    Socket socketReceiver = new Socket("localhost", cell);
                                    ObjectOutputStream outputStream = new ObjectOutputStream(socketReceiver.getOutputStream());
                                    packageData.setLastTypeOfEmisor('M');
                                    outputStream.writeObject(packageData);
                                    socketReceiver.close();
                                } catch (ConnectException ignored) {}
                            }
                        }
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}
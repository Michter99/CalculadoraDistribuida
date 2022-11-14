package calc.calculadora;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

public class AdminController implements Initializable {

    @FXML
    private TextField sumAcuse;
    @FXML
    private TextField resAcuse;
    @FXML
    private TextField multAcuse;
    @FXML
    private TextField divAcuse;
    @FXML
    private ChoiceBox<String> servidorField;
    @FXML
    private ChoiceBox<String> microField;

    private static ServerSocket adminSocket;
    static int portUsed = 4000;
    private static int connectedNode = 5000;
    private static final Set<String> servidores = new HashSet<>();

    @FXML
    void configurarAcuses() {
        int acusesSuma = Integer.parseInt(sumAcuse.getText());
        int acusesResta = Integer.parseInt(resAcuse.getText());
        int acusesMultiplicacion = Integer.parseInt(multAcuse.getText());
        int acusesDivision = Integer.parseInt(divAcuse.getText());
        System.out.println("Suma " + sumAcuse.getText());
        Package packet = new Package('A', portUsed);
        packet.setAcuses(acusesSuma, acusesResta, acusesMultiplicacion, acusesDivision);
        sendPackage(packet);
    }

    @FXML
    void inyectarServicio() throws IOException {
        String servidor = servidorField.getValue();
        String microservicio = microField.getValue();
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "copy C:\\CalculadoraServicios\\Microservicios\\" + microservicio + ".jar C:\\CalculadoraServicios\\Server" + servidor);
        builder.start();
        servidorField.setValue("");
        microField.setValue("");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            adminSocket = new ServerSocket(portUsed);
            Package temp = new Package('A', portUsed);
            temp.setOperationCode(0);
            sendPackage(temp);
            receivePackage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        microField.getItems().addAll("Suma", "Resta", "Multiplicacion", "Division");
    }

    static void sendPackage(Package packageToClient) {
        try {
            packageToClient.setPackageType('A');
            packageToClient.setLastTypeOfEmisor('A');
            packageToClient.setEmisor(portUsed);
            Socket socketSender = new Socket("localhost", connectedNode);
            ObjectOutputStream outputStream = new ObjectOutputStream(socketSender.getOutputStream());
            outputStream.writeObject(packageToClient);
            socketSender.close();
        } catch (Exception ignored) {
            connectedNode++;
            if (connectedNode == 5020)
                connectedNode = 5000;
        }
    }

    void receivePackage() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = adminSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Package serverPackage = (Package) inputStream.readObject();
                    if (serverPackage.getPackageType() == 'S') {
                        servidores.add(String.valueOf(serverPackage.getEmisor()));
                        Platform.runLater(() -> {
                            servidorField.getItems().clear();
                            servidorField.getItems().addAll(servidores);
                        });
                    }
                    inputStream.close();
                    socket.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}

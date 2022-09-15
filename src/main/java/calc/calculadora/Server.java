package calc.calculadora;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {

    private static ServerSocket serverSocket;
    static int portUsed = 7000;

    static void receivePackage() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    PackageToServer serverPackage = (PackageToServer) inputStream.readObject();
                    processOperation(serverPackage.getNum1(), serverPackage.getNum2(), serverPackage.getOperationCode());
                    inputStream.close();
                    socket.close();
                } catch (ClassCastException ignored) {
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    static void processOperation(double number1, double number2, int op) throws IOException {
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
        System.out.println("Solicitud procesada por el servidor " + portUsed);
        System.out.println(number1 + " " + operator + " " + number2 + " = " + result + "\n");
        sendProcessedPackage(result);
    }

    static void sendProcessedPackage(double result) throws IOException {
        Socket socketSender = new Socket("localhost", 5001);
        ObjectOutputStream outputStream = new ObjectOutputStream(socketSender.getOutputStream());
        PackageToClient packageToClient = new PackageToClient(portUsed, result);
        outputStream.writeObject(packageToClient);
        socketSender.close();
    }

    static void initializeServers(int port) {
        // Tratar de inicializar el servidor en el puerto definido, si ya está usado, pasar al siguiente puerto
        try {
            serverSocket = new ServerSocket(port);
            sendProcessedPackage(0); // Enviar un paquete al middleware para añadir su puerto a la lista de células
        } catch (BindException ex) {
            initializeServers(++portUsed);
        } catch (ConnectException ignored) {
            System.out.println("Debe correr primero el middleware");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        initializeServers(portUsed);
        receivePackage();
    }
}

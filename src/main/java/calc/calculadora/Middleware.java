package calc.calculadora;

import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/*
    Middleware  5000 - 5001
    Servidores  (7000 - 7999)
    Clientes    (6000 - 6999)
 */

public class Middleware {

    private static ServerSocket serverSocketToServer;
    private static ServerSocket serverSocketToClient;
    private static final ArrayList<Integer> cells = new ArrayList<>();

    static void receiveAndResendPackageToServer() {
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

                    System.out.println("\nPaquete recibido de " + serverPackage.getEmisor());
                    System.out.println("Código de operación: " + serverPackage.getOperationCode());

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

    static void receiveAndResendPackageToClient() {
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

                    System.out.println("\nPaquete recibido de " + clientPackage.getEmisor());

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
                    Thread.currentThread().interrupt();
                } catch (IOException | ClassNotFoundException e ) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public static void main(String[] args) throws IOException {
        serverSocketToServer = new ServerSocket(5000);
        serverSocketToClient = new ServerSocket(5001);
        receiveAndResendPackageToServer();
        receiveAndResendPackageToClient();
    }
}

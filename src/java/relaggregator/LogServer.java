package relaggregator.LogServer; 

import java.io.Closeable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;

public class LogServer implements Closeable {
    private final ServerSocket server;
    private final Socket socket;
    private final BufferedReader in;

    public LogServer(final int port) throws IOException {
        System.out.println("Starting on port: " + port);
        this.server = new ServerSocket(port);
        System.out.println("Waiting for client...");
        this.socket = server.accept();
        System.out.println("Client accepted!");
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public BufferedReader getReader() {
        return in;
    }

    public void close() throws IOException {
        server.close();
        socket.close();
        in.close();
        System.out.println("Server closed!");
    }

}

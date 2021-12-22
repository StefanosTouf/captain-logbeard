

import java.io.Closeable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;

class LogServer implements Closeable {
    private final Socket socket;
    private final BufferedReader in;

    public LogServer(final int port) throws IOException {
        final ServerSocket server = new ServerSocket(port);
        this.socket = server.accept();
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public BufferedReader getReader() {
        return in;
    }

    public void close() throws IOException {
        socket.close();
        in.close();
    }

}

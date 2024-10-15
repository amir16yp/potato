package potato.server;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

class ClientHandler implements Runnable {
    private final int clientId;
    private final Socket clientSocket;
    private final GameServer server;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    public ClientHandler(int clientId, Socket socket, GameServer server) throws IOException {
        this.clientId = clientId;
        this.clientSocket = socket;
        this.server = server;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            while (true) {
                Packet packet = (Packet) in.readObject();
                handlePacket(packet);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client " + clientId + ": " + e.getMessage());
        } finally {
            server.removeClient(clientId);
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void handlePacket(Packet packet) {
        if (packet instanceof PlayerPositionPacket) {
            PlayerPositionPacket posPacket = (PlayerPositionPacket) packet;
            // Validate and update player position
            // Then broadcast to other clients
            server.broadcast(posPacket, clientId);
        }
        // Handle other packet types as needed
    }

    public void sendPacket(Packet packet) {
        try {
            out.writeObject(packet);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error sending packet to client " + clientId + ": " + e.getMessage());
        }
    }

    public int getClientId() {
        return clientId;
    }
}
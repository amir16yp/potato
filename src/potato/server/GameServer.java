package potato.server;

import potato.Map;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {
    private static final int PORT = 8080;
    private final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final AtomicInteger clientIdCounter = new AtomicInteger(0);
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Map gameMap;

    public GameServer() {
        this.gameMap = new Map(32, 32, System.currentTimeMillis()); // Use current time as seed
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Game Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientId = clientIdCounter.incrementAndGet();
                ClientHandler clientHandler = new ClientHandler(clientId, clientSocket, this);
                clients.put(clientId, clientHandler);

                // Send ClientIdPacket to the new client
                ClientIDPacket idPacket = new ClientIDPacket(clientId);
                clientHandler.sendPacket(idPacket);

                // Send MapDataPacket to the new client
                MapDataPacket mapPacket = new MapDataPacket(gameMap);
                clientHandler.sendPacket(mapPacket);

                pool.execute(clientHandler);
                System.out.println("New client connected: " + clientId);

                // Broadcast new player connection to other clients
                broadcastNewPlayer(clientId);
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    private void broadcastNewPlayer(int newClientId) {
        // You might want to create a new packet type for this, e.g., NewPlayerPacket
        // For now, we'll use PlayerPositionPacket with default position
        PlayerPositionPacket newPlayerPacket = new PlayerPositionPacket(newClientId, 1.5, 1.5, 0);
        broadcast(newPlayerPacket, newClientId);
    }

    public void broadcast(Packet packet, int excludeClientId) {
        for (ClientHandler client : clients.values()) {
            if (client.getClientId() != excludeClientId) {
                client.sendPacket(packet);
            }
        }
    }

    public void removeClient(int clientId) {
        clients.remove(clientId);
        System.out.println("Client disconnected: " + clientId);
        // You might want to broadcast a player disconnection message here
    }

    public Map getGameMap() {
        return gameMap;
    }

    public static void main(String[] args) {
        new GameServer().start();
    }
}
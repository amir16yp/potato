package potato.server;

import java.io.Serializable;

public class ClientIDPacket implements Packet, Serializable {
    private final int clientId;

    public ClientIDPacket(int clientId) {
        this.clientId = clientId;
    }

    @Override
    public PacketType getType() {
        return PacketType.CLIENT_ID;
    }

    public int getClientId() {
        return clientId;
    }
}
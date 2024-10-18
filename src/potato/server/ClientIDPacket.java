package potato.server;

import java.io.Serializable;

public class ClientIDPacket implements Packet, Serializable {
    private final int clientId;

    public ClientIDPacket(int uuid) {
        this.clientId = uuid;
    }

    @Override
    public PacketType getType() {
        return PacketType.CLIENT_ID;
    }

    public int getClientId() {
        return clientId;
    }
}
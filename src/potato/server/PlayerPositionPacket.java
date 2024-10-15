package potato.server;

import java.io.Serializable;

public class PlayerPositionPacket implements Packet, Serializable {
    private final int clientId;
    private final double x;
    private final double y;
    private final double angle;

    public PlayerPositionPacket(int clientId, double x, double y, double angle) {
        this.clientId = clientId;
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    @Override
    public PacketType getType() {
        return PacketType.PLAYER_POSITION;
    }

    public int getClientId() {
        return clientId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    // Getters for clientId, x, y, and angle
}
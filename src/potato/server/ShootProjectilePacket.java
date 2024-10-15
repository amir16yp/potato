package potato.server;

import java.io.Serializable;

public class ShootProjectilePacket implements Packet, Serializable {
    private static final long serialVersionUID = 1L;

    private final int clientId;
    private final double x;
    private final double y;
    private final double angle;
    private final double speed;
    private final int damage;
    private final int textureID;

    public ShootProjectilePacket(int clientId, double x, double y, double angle, double speed, int damage, int textureID) {
        this.clientId = clientId;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speed = speed;
        this.damage = damage;
        this.textureID = textureID;
    }

    @Override
    public PacketType getType() {
        return PacketType.SHOOT_PROJECTILE;
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

    public double getSpeed() {
        return speed;
    }

    public int getDamage() {
        return damage;
    }

    public int getTextureID() {
        return textureID;
    }
}

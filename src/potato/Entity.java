package potato;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Entity {
    protected double x;
    protected double y;
    protected double angle;
    protected BufferedImage sprite;
    protected boolean active;
    protected double speed;

    public Entity(double x, double y, BufferedImage sprite, double speed) {
        this.x = x;
        this.y = y;
        this.angle = 0; // Initial angle, will be updated to face player
        this.sprite = sprite;
        this.speed = speed;
        this.active = true;
    }

    public void update() {
        if (!active) return;
        // Update angle to face the player
        updateAngleToFacePlayer(Game.player);
        // Override this method in subclasses to implement entity-specific behavior
    }

    private void updateAngleToFacePlayer(Player player) {
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        this.angle = Math.atan2(dy, dx);
    }

    private double normalizeAngle(double angle) {
        angle = angle % (2 * Math.PI);
        if (angle > Math.PI) {
            angle -= 2 * Math.PI;
        } else if (angle < -Math.PI) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    public boolean isVisibleToPlayer(Player player, Map map) {
        double dx = this.x - player.getX();
        double dy = this.y - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Check if the entity is within rendering distance
        if (distance > Renderer.MAX_DISTANCE) {
            return false;
        }

        // Check if the entity is within the player's field of view
        double angleDiff = Math.abs(normalizeAngle(Math.atan2(dy, dx) - player.getAngle()));
        if (angleDiff > Renderer.HALF_FOV && distance > 0.5) {
            return false;
        }

        // Perform a simple ray cast to check for obstacles
        double step = 0.1; // Adjust this value for precision vs performance
        double rayX = player.getX();
        double rayY = player.getY();
        double rayAngle = Math.atan2(dy, dx);

        for (double d = 0; d < distance; d += step) {
            rayX += Math.cos(rayAngle) * step;
            rayY += Math.sin(rayAngle) * step;

            if (map.isWall((int)rayX, (int)rayY)) {
                return false; // Hit a wall before reaching the entity
            }
        }

        return true; // No obstacles found
    }

    // Getter and setter methods...

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setSprite(BufferedImage sprite) {
        this.sprite = sprite;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public BufferedImage getSprite() {
        return sprite;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
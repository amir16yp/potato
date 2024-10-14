package potato;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SpriteEntity {
    protected double x;
    protected double y;
    protected double angle;
    protected BufferedImage sprite;
    protected boolean active;
    protected double speed;
    private double size;
    private Runnable onInteractPlayer;

    public void deactivate() {
        this.active = false;
    }

    public void setOnInteractPlayer(Runnable action) {
        this.onInteractPlayer = action;
    }

    public SpriteEntity(double x, double y, BufferedImage sprite, double speed) {
        this.x = x;
        this.y = y;
        this.angle = 0; // Initial angle, will be updated to face player
        this.sprite = sprite;
        this.speed = speed;
        this.active = true;
        this.size = 1;
    }

    public void render(Renderer renderer, Player player) {
        if (!active) return;

        double dx = x - player.getX();
        double dy = y - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Don't render if too close or too far
        if (distance < 0.1 || distance > Renderer.MAX_DISTANCE) return;

        // Calculate the angle between player's view and the entity
        double entityAngle = Math.atan2(dy, dx) - player.getAngle();

        // Normalize the angle to stay within [-PI, PI]
        while (entityAngle < -Math.PI) entityAngle += 2 * Math.PI;
        while (entityAngle > Math.PI) entityAngle -= 2 * Math.PI;

        // Check if the entity is within the player's field of view
        if (Math.abs(entityAngle) > Renderer.HALF_FOV) return;

        // Calculate the screen position based on the angle
        int screenX = (int) ((entityAngle / Renderer.HALF_FOV + 1) * renderer.width / 2);

        // Calculate the entity's size based on its distance from the player
        double projectedSize = (renderer.gameHeight / distance) * size;

        // Adjust vertical position based on distance
        int screenY = (int) (renderer.gameHeight / 2 * (1 + 1 / distance));

        // Draw the entity
        renderer.drawSprite(sprite, screenX, screenY, (int) projectedSize, distance, RenderTarget.GAME);
    }

    public void update() {
        if (!active) {
            Game.renderer.entities.remove(this);
            return;
        }
        // Update angle to face the player
        updateToPlayer(Game.player);
    }

    private void updateToPlayer(Player player) {
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        this.angle = Math.atan2(dy, dx);
        if (this.onInteractPlayer == null) {
            return;
        }
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 2) {
            onInteractPlayer.run();
        }
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

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }
}
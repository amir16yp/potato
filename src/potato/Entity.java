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

    public void update(float deltaTime, Map map, Player player) {
        if (!active) return;
        // Update angle to face the player
        updateAngleToFacePlayer(player);
        // Override this method in subclasses to implement entity-specific behavior
    }

    private void updateAngleToFacePlayer(Player player) {
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        this.angle = Math.atan2(dy, dx);
    }


    public void render(Graphics2D g, Player player, int screenWidth, int screenHeight) {
        if (!active || sprite == null) return;

        double relativeX = x - player.getX();
        double relativeY = y - player.getY();

        double distance = Math.sqrt(relativeX * relativeX + relativeY * relativeY);
        double angleToEntity = Math.atan2(relativeY, relativeX) - player.getAngle();

        // Normalize angle
        angleToEntity = normalizeAngle(angleToEntity);

        // Check if entity is in field of view
        if (Math.abs(angleToEntity) > Renderer.HALF_FOV) return;

        // Calculate screen position
        int screenX = (int) ((angleToEntity / Renderer.FOV + 0.5) * screenWidth);
        int screenY = screenHeight / 2;

        // Calculate sprite size based on distance
        double spriteSize = (screenHeight / distance) * (sprite.getWidth() / (double)sprite.getHeight());
        int spriteWidth = (int) spriteSize;
        int spriteHeight = (int) spriteSize;

        // Calculate drawing coordinates
        int drawX = screenX - spriteWidth / 2;
        int drawY = screenY - spriteHeight / 2;

        // Ensure we're drawing the entire sprite
        g.drawImage(sprite,
                drawX, drawY,
                drawX + spriteWidth, drawY + spriteHeight,
                0, 0,
                sprite.getWidth(), sprite.getHeight(),
                null);

        // Optional: Add outline for debugging
        g.setColor(Color.RED);
        g.drawRect(drawX, drawY, spriteWidth, spriteHeight);
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
}
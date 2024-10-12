package potato;

import java.awt.image.BufferedImage;

public class Projectile {
    private double x;
    private double y;
    private double angle;
    private double speed;
    private int damage;
    private BufferedImage sprite;
    private boolean active;

    public Projectile(double x, double y, double angle, double speed, int damage, BufferedImage sprite) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speed = speed;
        this.damage = damage;
        this.sprite = sprite;
        this.active = true;
    }

    public void update(float deltaTime, Map map) {
        if (!active) return;

        double dx = Math.cos(angle) * speed * deltaTime;
        double dy = Math.sin(angle) * speed * deltaTime;

        x += dx;
        y += dy;

        // Check for collision with map
        if (map.isWall((int)x, (int)y)) {
            deactivate();
        }
    }

    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public BufferedImage getSprite() {
        return sprite;
    }

    public int getDamage() {
        return damage;
    }
}
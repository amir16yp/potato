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
    protected Logger logger = new Logger(this.getClass().getName());
    private double size;

    private static final double COLLISION_THRESHOLD = 0.1;

    public Projectile(double x, double y, double angle, double speed, int damage, BufferedImage sprite) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speed = speed;
        this.damage = damage;
        this.sprite = sprite;
        this.active = true;
        this.size = 0.2;
        logger.Log("Created projectile at " + x + "," + y + " with angle " + angle);
    }

    public void update() {
        if (!active || Game.renderer.getMap() == null) return;
        float deltaTime = Game.gameLoop.getDeltaTimeMillis() / 1000f; // Convert to seconds
        double dx = Math.cos(angle) * speed * deltaTime;
        double dy = Math.sin(angle) * speed * deltaTime;

        double newX = x + dx;
        double newY = y + dy;

        // Check for collision with map
        if (!isWall(newX, newY)) {
            x = newX;
            y = newY;
        } else {
            deactivate("wall");
        }

        for (Entity entity : Game.renderer.entities)
        {
            // TODO: add entity collision
        }

        // Deactivate if out of bounds
        if (x < 0 || x >= Game.renderer.getMap().getWidth() || y < 0 || y >= Game.renderer.getMap().getHeight()) {
            deactivate("out of bounds");
        }
    }


    private boolean isWall(double x, double y) {
        int mapX = (int) x;
        int mapY = (int) y;
        return Game.renderer.getMap().isWall(mapX, mapY);
    }

    private double distanceToWall(double x, double y) {
        int intX = (int) x;
        int intY = (int) y;
        double fractX = x - intX;
        double fractY = y - intY;

        return Math.min(
                Math.min(fractX, 1 - fractX),
                Math.min(fractY, 1 - fractY)
        );
    }

    public void deactivate() {
        logger.Log("Deactivated projectile at " + x + "," + y);
        active = false;
    }

    public void deactivate(String reason)
    {
        logger.Log("Deactivated projectile at " + x + "," + y +" : " + reason);
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

    public double getSize() {
        return size;
    }
}
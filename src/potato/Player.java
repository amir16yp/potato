package potato;

import java.util.concurrent.CopyOnWriteArrayList;

public class Player {
    private double x;
    private double y;
    private double angle;
    private double moveSpeed;
    private double rotateSpeed;
    private Weapon weapon;
    private CopyOnWriteArrayList<Projectile> projectiles;

    // Constants for collision detection
    private static final double COLLISION_RADIUS = 0.2;

    public Player(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.moveSpeed = 2.0; // Units per second
        this.rotateSpeed = Math.PI; // Radians per second
        this.projectiles = new CopyOnWriteArrayList<>();
        // Initialize with a default weapon
        this.weapon = Weapons.WEAPON_1;
    }

    public void update(InputHandler inputHandler, Map map) {
        float deltaTime = Game.gameLoop.getDeltaTime();
        float deltaTimeMillis = Game.gameLoop.getDeltaTimeMillis();

        handleMovement(inputHandler, deltaTime, map);
        handleRotation(inputHandler, deltaTime);
        handleWeapon(inputHandler, deltaTimeMillis);
        updateProjectiles(deltaTime, map);
    }

    private void handleMovement(InputHandler inputHandler, float deltaTime, Map map) {
        if (map == null)
        {
            return;
        }
        double moveDistance = moveSpeed * deltaTime;
        double dx = 0, dy = 0;

        if (inputHandler.isMovingForward()) {
            dx += Math.cos(angle) * moveDistance;
            dy += Math.sin(angle) * moveDistance;
        }
        if (inputHandler.isMovingBackward()) {
            dx -= Math.cos(angle) * moveDistance;
            dy -= Math.sin(angle) * moveDistance;
        }
        if (inputHandler.isStrafingLeft()) {
            dx += Math.cos(angle - Math.PI / 2) * moveDistance;
            dy += Math.sin(angle - Math.PI / 2) * moveDistance;
        }
        if (inputHandler.isStrafingRight()) {
            dx += Math.cos(angle + Math.PI / 2) * moveDistance;
            dy += Math.sin(angle + Math.PI / 2) * moveDistance;
        }

        if (!map.isWall((int)(x + dx + Math.signum(dx) * COLLISION_RADIUS), (int)y)) {
            x += dx;
        }
        if (!map.isWall((int)x, (int)(y + dy + Math.signum(dy) * COLLISION_RADIUS))) {
            y += dy;
        }
    }

    private void handleRotation(InputHandler inputHandler, float deltaTime) {
        if (inputHandler.isRotatingLeft()) {
            angle -= rotateSpeed * deltaTime;
        }
        if (inputHandler.isRotatingRight()) {
            angle += rotateSpeed * deltaTime;
        }
        // Normalize angle
        angle = (angle + 2 * Math.PI) % (2 * Math.PI);
    }

    private void handleWeapon(InputHandler inputHandler, float deltaTimeMillis) {
        if (weapon != null) {
            weapon.update(deltaTimeMillis);
            if (inputHandler.isFiring() && weapon.canFire()) {
                Projectile projectile = weapon.fire(x, y, angle);
                if (projectile != null) {
                    projectiles.add(projectile);
                }
            }
        }
    }

    private void updateProjectiles(float deltaTime, Map map) {
        for (Projectile projectile : projectiles) {
            projectile.update(deltaTime, map);
            if (map.isWall((int) projectile.getX(), (int) projectile.getY())) {
                projectiles.remove(projectile);
            }
        }
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

    public Weapon getWeapon() {
        return weapon;
    }

    public void setWeapon(Weapon weapon) {
        this.weapon = weapon;
    }

    public CopyOnWriteArrayList<Projectile> getProjectiles() {
        return projectiles;
    }

    // Method to check if the player is moving (for weapon bobbing effect)
    public boolean isMoving() {
        InputHandler inputHandler = Game.inputHandler;
        return inputHandler.isMovingForward() || inputHandler.isMovingBackward() ||
                inputHandler.isStrafingLeft() || inputHandler.isStrafingRight();
    }
}
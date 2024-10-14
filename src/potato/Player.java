package potato;

import potato.input.InputHandler;

import java.util.concurrent.CopyOnWriteArrayList;

public class Player {
    private double x;
    private double y;
    private double planeX;
    private double planeY;
    private double angle;
    private double moveSpeed;
    private double rotateSpeed;
    private Weapon weapon;
    private CopyOnWriteArrayList<Projectile> projectiles;
    private double health = 100.0;
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
        this.planeX = Math.cos(angle + Math.PI/2) * 0.66;
        this.planeY = Math.sin(angle + Math.PI/2) * 0.66;
    }

    public double getSpeed() {
        InputHandler inputHandler = Game.inputHandler;
        int movementCount = 0;

        if (inputHandler.isMovingForward()) movementCount++;
        if (inputHandler.isMovingBackward()) movementCount++;
        if (inputHandler.isStrafingLeft()) movementCount++;
        if (inputHandler.isStrafingRight()) movementCount++;

        // Normalize speed for diagonal movement
        return movementCount > 0 ? (movementCount == 1 ? 1.0 : 0.707) : 0;
    }


    public void update() {
        handleMovement();
        handleRotation();
        handleWeapon();
        updateProjectiles();
    }

    private void handleMovement() {
        Map map = Game.renderer.getMap();
        if (map == null)
        {
            return;
        }
        InputHandler inputHandler = Game.inputHandler;
        double moveDistance = moveSpeed * Game.gameLoop.getDeltaTime();
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

    private void handleRotation() {
        InputHandler inputHandler = Game.inputHandler;
        double rotationAmount = 0;
        if (inputHandler.isRotatingLeft()) {
            rotationAmount = -rotateSpeed * Game.gameLoop.getDeltaTime();
        }
        if (inputHandler.isRotatingRight()) {
            rotationAmount = rotateSpeed * Game.gameLoop.getDeltaTime();
        }

        if (rotationAmount != 0) {
            // Rotate the player
            angle += rotationAmount;
            angle = (angle + 2 * Math.PI) % (2 * Math.PI);

            // Rotate the camera plane
            double oldPlaneX = planeX;
            planeX = planeX * Math.cos(rotationAmount) - planeY * Math.sin(rotationAmount);
            planeY = oldPlaneX * Math.sin(rotationAmount) + planeY * Math.cos(rotationAmount);
        }
    }

    private void handleWeapon() {
        if (weapon != null) {
            InputHandler inputHandler = Game.inputHandler;
            weapon.update(Game.gameLoop.getDeltaTimeMillis());
            if (inputHandler.isFiring() && weapon.canFire()) {
                // Adjust the initial position to be slightly in front of the player
                double projectileX = x + Math.cos(angle) * 0.5;
                double projectileY = y + Math.sin(angle) * 0.5;
                Projectile projectile = weapon.fire(projectileX, projectileY, angle);
                projectiles.add(projectile);
            }
        }
    }

    private void updateProjectiles() {
        for (Projectile projectile : projectiles) {
            projectile.update();
            if (!projectile.isActive()) {
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

    public double getPlaneY() {
        return planeY;
    }

    public double getPlaneX() {
        return planeX;
    }
}
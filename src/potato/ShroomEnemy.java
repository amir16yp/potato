package potato;

import java.util.Random;

public class ShroomEnemy extends EnemyEntity {
    private static final double SHROOM_SIZE = 1.2;
    private static final int SHROOM_MAX_HEALTH = 15;
    private static final int SHROOM_ATTACK_DAMAGE = 8;
    private static final double SHROOM_SPEED = 2.5;
    private static final double WANDER_RADIUS = 3.0;
    private static final double CHASE_RADIUS = 5.0;

    private Random random;
    private double wanderTimer;
    private double wanderX, wanderY;

    public ShroomEnemy(double x, double y) {
        super(x, y, Enemies.SHROOM_TEXTURES, SHROOM_SPEED, SHROOM_MAX_HEALTH, SHROOM_ATTACK_DAMAGE);
        setSize(SHROOM_SIZE);
        random = new Random();
        wanderTimer = 0;
        setNewWanderTarget();
    }

    @Override
    public void update() {
        super.update();
        if (getCurrentState() == State.DEAD) {
            return;
        }

        Player player = Game.player;
        double distanceToPlayer = getDistanceTo(player.getX(), player.getY());

        if (distanceToPlayer <= CHASE_RADIUS) {
            // Chase the player
            double dx = player.getX() - getX();
            double dy = player.getY() - getY();
            double length = Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
            setVelocity(dx * SHROOM_SPEED, dy * SHROOM_SPEED);
        } else {
            // Wander around
            wander();
        }
    }

    private void wander() {
        wanderTimer += Game.gameLoop.getDeltaTimeMillis() / 1000.0;
        if (wanderTimer >= 3.0) {
            setNewWanderTarget();
            wanderTimer = 0;
        }

        double dx = wanderX - getX();
        double dy = wanderY - getY();
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length > 0.1) {
            dx /= length;
            dy /= length;
            setVelocity(dx * SHROOM_SPEED * 0.5, dy * SHROOM_SPEED * 0.5);
        } else {
            setVelocity(0, 0);
        }
    }

    private void setNewWanderTarget() {
        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = random.nextDouble() * WANDER_RADIUS;
        wanderX = getX() + Math.cos(angle) * radius;
        wanderY = getY() + Math.sin(angle) * radius;
    }

    private double getDistanceTo(double targetX, double targetY) {
        double dx = targetX - getX();
        double dy = targetY - getY();
        return Math.sqrt(dx * dx + dy * dy);
    }


    @Override
    public void attackPlayer() {
        Player player = Game.player;
        if (getDistanceTo(player.getX(), player.getY()) < 2) {
            player.takeDamage(SHROOM_ATTACK_DAMAGE);
        }
    }
}
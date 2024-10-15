package potato;

import java.util.HashMap;
import java.util.Map;

public abstract class EnemyEntity extends SpriteEntity {
    private static final Map<State, int[]> STATE_FRAMES = new HashMap<>();

    static {
        STATE_FRAMES.put(State.IDLE, new int[]{1, 2});
        STATE_FRAMES.put(State.ATTACKING, new int[]{4, 5});
        STATE_FRAMES.put(State.HURT, new int[]{8});
        STATE_FRAMES.put(State.DYING, new int[]{6, 7});
        STATE_FRAMES.put(State.DEAD, new int[]{3});
    }

    private final float frameDuration = 200;
    private float frameDelta = 0;
    private int currentFrameIndex = 0;
    private final Textures spritesheet;
    private State currentState = State.IDLE;
    private int health;
    private final int maxHealth;
    private final int attackDamage;
    private final float attackCooldown = 1000; // 1 second
    private float attackTimer = 0;
    public EnemyEntity(double x, double y, Textures spriteSheet, double speed, int maxHealth, int attackDamage) {
        super(x, y, spriteSheet.getTile(1), speed);
        this.spritesheet = spriteSheet;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.attackDamage = attackDamage;
        this.setSize(this.getSize() * 1.5);
    }

    private void updateAnimation(float deltaTime) {
        frameDelta += deltaTime;
        if (frameDelta >= frameDuration) {
            frameDelta = 0;
            currentFrameIndex = (currentFrameIndex + 1) % STATE_FRAMES.get(currentState).length;
            int tileId = STATE_FRAMES.get(currentState)[currentFrameIndex];
            setSprite(spritesheet.getTile(tileId));
        }
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            health = 0;
            setState(State.DYING);
        } else {
            setState(State.HURT);
        }
    }

    public void setState(State newState) {
        if (currentState != newState) {
            currentState = newState;
            currentFrameIndex = 0;
            frameDelta = 0;
        }
    }

    private void updateState() {
        if (currentState == State.DYING && currentFrameIndex == STATE_FRAMES.get(State.DYING).length - 1) {
            setState(State.DEAD);
        } else if (currentState == State.HURT && frameDelta >= frameDuration) {
            setState(State.IDLE);
        }
    }

    private void updateAttack(float deltaTime) {
        if (currentState != State.ATTACKING) {
            attackTimer += deltaTime;
            if (attackTimer >= attackCooldown) {
                attackTimer = 0;
                if (isPlayerInRange()) {
                    setState(State.ATTACKING);
                }
            }
        } else if (currentFrameIndex == STATE_FRAMES.get(State.ATTACKING).length - 1) {
            if (isPlayerInRange()) {
                attackPlayer();
            }
            setState(State.IDLE);
        }
    }

    public State getCurrentState() {
        return currentState;
    }

    private boolean isPlayerInRange() {
        Player player = Game.player;
        double distance = Math.sqrt(Math.pow(player.getX() - x, 2) + Math.pow(player.getY() - y, 2));
        return distance < 5; // Attack range of 2 units
    }
    @Override
    public void update() {
        super.update();
        if (currentState == State.DEAD)
        {
            return;
        }
        float deltaTime = Game.gameLoop.getDeltaTimeMillis();
        updateState();
        updateAttack(deltaTime);
        updateAnimation(deltaTime);
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public abstract void attackPlayer();

    // Animation states
    public enum State {
        IDLE, ATTACKING, HURT, DYING, DEAD
    }
}
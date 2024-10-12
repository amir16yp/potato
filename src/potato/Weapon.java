package potato;

public class Weapon {
    private String name;
    private int damage;
    private float cooldownTime; // Time between shots in seconds
    private float projectileSpeed;
    private float currentCooldown; // Current time until next shot
    private GunSprite gunSprite;

    public Weapon(String name, int damage, float cooldownTime, float projectileSpeed, GunSprite gunSprite) {
        this.name = name;
        this.damage = damage;
        this.cooldownTime = cooldownTime;
        this.projectileSpeed = projectileSpeed;
        this.currentCooldown = 0;
        this.gunSprite = gunSprite;
    }

    public void update(float deltaTime) {
        currentCooldown = Math.max(0, currentCooldown - deltaTime);
        gunSprite.update(deltaTime);
    }

    public boolean canFire() {
        return currentCooldown <= 0;
    }

    public Projectile fire(double x, double y, double angle) {
        if (canFire()) {
            currentCooldown = cooldownTime;
            this.gunSprite.triggerFire(cooldownTime * 1000); // Convert to milliseconds
            return new Projectile(x, y, angle, projectileSpeed, damage, Game.textures.getTile(6));
        }
        return null;
    }

    public float getCooldownTime() {
        return cooldownTime;
    }

    public void setCooldownTime(float cooldownTime) {
        this.cooldownTime = cooldownTime;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public float getProjectileSpeed() {
        return projectileSpeed;
    }

    public GunSprite getGunSprite() {
        return gunSprite;
    }

    public void setProjectileSpeed(float projectileSpeed) {
        this.projectileSpeed = projectileSpeed;
    }
}
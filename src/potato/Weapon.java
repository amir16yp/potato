package potato;

public class Weapon {
    private String name;
    private int damage;
    private float cooldownTime; // Time between shots in seconds
    private float projectileSpeed;
    private int projectileTextureID;
    private float currentCooldown; // Current time until next shot
    private GunSprite gunSprite;
    private int ammo = 100;

    protected Logger logger = new Logger(this.getClass().getName());

    public Weapon(String name, int damage, float cooldownTime, float projectileSpeed, GunSprite gunSprite, int projectileTextureID) {
        this.name = name;
        this.damage = damage;
        this.cooldownTime = cooldownTime;
        this.projectileSpeed = projectileSpeed;
        this.projectileTextureID = projectileTextureID;
        this.currentCooldown = 0;
        this.gunSprite = gunSprite;
        this.projectileTextureID = projectileTextureID;
    }

    public int getAmmo() {
        return ammo;
    }

    public void setAmmo(int ammo) {
        this.ammo = ammo;
    }

    public void addAmmo(int ammo)
    {
        this.ammo += ammo;
    }

    public String getName() {
        return name;
    }

    public void update(float deltaTime) {
        currentCooldown = Math.max(0, currentCooldown - deltaTime);
        gunSprite.update(deltaTime);
    }

    public boolean canFire() {
        return currentCooldown <= 0 && 0 < ammo;
    }

    public Projectile fire(double x, double y, double angle, int textureID) {
        if (canFire()) {
            currentCooldown = cooldownTime;
            this.gunSprite.triggerFire(cooldownTime * 1000); // Convert to milliseconds
            ammo--;
            if (ammo <= 0)
            {
                ammo = 0;
            }
            return new Projectile(x, y, angle, projectileSpeed, damage, textureID);
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

    public int getProjectileTextureID() {
        return projectileTextureID;
    }
}
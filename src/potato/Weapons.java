package potato;

public class Weapons {
    public static final Weapon WEAPON_1;

    static {
        // Parameters: name, damage, fireRate (shots per second), projectileSpeed, GunSprite
        WEAPON_1 = new Weapon("pistol", 1, 500, 5f,
                new GunSprite("/potato/sprites/pistol.png", 48, 48, 20, 5));
    }

    // Private constructor to prevent instantiation
    private Weapons() {}
}
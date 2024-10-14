package potato;

public class Weapons {
    public static final Weapon WEAPON_1;

    static {
        // Parameters: name, damage, fireRate (shots per second), projectileSpeed, GunSprite
        WEAPON_1 = new Weapon("pistol", 1, 500, 7f,
                new GunSprite("/potato/sprites/gun/pistol.png","/potato/sprites/gun/pistol-icon.png" ,48, 48, 20, 5), 1);
    }

    // Private constructor to prevent instantiation
    private Weapons() {}
}
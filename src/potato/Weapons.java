package potato;

public class Weapons {
    public static final Weapon WEAPON_1;

    static {
        // Parameters: name, damage, fireRate (shots per second), projectileSpeed, GunSprite
        WEAPON_1 = new Weapon("AR", 1, 0, 5f,
                new GunSprite("ar.png", 24, 24, 20));
    }

    // Private constructor to prevent instantiation
    private Weapons() {}
}
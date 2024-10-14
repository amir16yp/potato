package potato;

public class Weapons {
    public static final Weapon PISTOL;
    public static final Weapon SHOTGUN;

    static {
        // Parameters: name, damage, fireRate (shots per second), projectileSpeed, GunSprite
        PISTOL = new Weapon("pistol", 1, 300, 7f,
                new GunSprite(
                        "/potato/sprites/gun/pistol.png",
                        "/potato/sprites/gun/pistol-icon.png",
                        48, 48, 20, 5), 1);

        SHOTGUN = new Weapon("shotgun", 20, 600, 8f,
                new GunSprite(
                        "/potato/sprites/gun/shotgun.png",
                        "/potato/sprites/gun/shotgun-icon.png",
                        48, 48, 60, 5), 2);
    }

    // Private constructor to prevent instantiation
    private Weapons() {
    }
}

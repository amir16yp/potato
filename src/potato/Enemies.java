package potato;

public class Enemies {
    public static final Textures GUNSHELL_TEXTURES = new Textures("/potato/sprites/entity/gunshell.png", 18, 18);
    public static final Textures SHROOM_TEXTURES = new Textures("/potato/sprites/entity/shroom.png", 18, 18);

    public static void spawnShroomEnemy(int x, int y) {
        ShroomEnemy shroomEnemy = new ShroomEnemy(x, y);
        Game.renderer.entities.add(shroomEnemy);
    }

}

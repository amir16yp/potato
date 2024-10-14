package potato;

public class Enemies
{
    public static final Textures GUNSHELL_TEXTURES = new Textures("/potato/sprites/entity/gunshell.png", 18, 18);
    public static final Textures SHROOM_TEXTURES = new Textures("/potato/sprites/entity/shroom.png", 18, 18);

    public static void spawnEntity(int x, int y, Textures textures, double size)
    {
        EnemyEntity enemyEntity = new EnemyEntity(x, y, textures, 3, 10, 10);
        enemyEntity.setSize(size);
        Game.renderer.entities.add(enemyEntity);
    }

}

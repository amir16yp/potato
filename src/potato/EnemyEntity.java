package potato;

import java.awt.image.BufferedImage;

public class EnemyEntity extends SpriteEntity{

    protected float frameDuration = 200;
    protected float frameDelta = 0;
    protected int currentTile;
    protected Textures spritesheet;
    protected boolean idle = true;
    protected boolean dead = false;
    protected boolean dying = false;
    protected boolean attacking = false;
    protected boolean hurt = false;
    public EnemyEntity(double x, double y, Textures spriteSheet, double speed) {
        super(x, y, spriteSheet.getTile(1), speed);
        this.spritesheet = spriteSheet;
        currentTile = 1;
        this.setSize(this.getSize() *1.5);
    }

    protected void getNextTile()
    {
        if (idle) {
            if (currentTile == 1)
            {
                currentTile = 2;
            } else {
                currentTile = 1;
            }
            return;
        }
        if (dying){
            if (currentTile == 6)
            {
                currentTile = 7;
            } else {
                currentTile = 6;
            }
            return;
        }

        if (dead)
        {
            currentTile = 3;
            return;
        }

        if (attacking)
        {
            if (currentTile == 4)
            {
                currentTile = 5;
            } else {
                currentTile = 4;
            }
            return;
        }

        if (hurt)
        {
            currentTile =8;
        }
    }

    @Override
    public void update() {
        super.update();
        getNextTile();
        if (frameDelta >= frameDuration)
        {
            frameDelta = 0;
            setSprite(spritesheet.getTile(currentTile));
        }
        frameDelta += Game.gameLoop.getDeltaTimeMillis();
    }
}

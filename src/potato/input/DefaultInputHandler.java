package potato.input;

import potato.Enemies;
import potato.Game;
import potato.Renderer;
import potato.Weapons;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import static potato.Game.*;

public class DefaultInputHandler implements IInputHandler {
    protected final Set<Integer> heldKeys = new HashSet<>();

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        heldKeys.add(key);
        handleOtherInput(key);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        heldKeys.remove(e.getKeyCode());
    }

    protected boolean isKeyHeld(int keyCode) {
        return heldKeys.contains(keyCode);
    }

    @Override
    public boolean isMovingForward() {
        return isKeyHeld(KeyEvent.VK_W) || isKeyHeld(KeyEvent.VK_UP);
    }

    @Override
    public boolean isMovingBackward() {
        return isKeyHeld(KeyEvent.VK_S) || isKeyHeld(KeyEvent.VK_DOWN);
    }

    @Override
    public boolean isStrafingLeft() {
        return isKeyHeld(KeyEvent.VK_A) || isKeyHeld(KeyEvent.VK_LEFT);
    }

    @Override
    public boolean isStrafingRight() {
        return isKeyHeld(KeyEvent.VK_D) || isKeyHeld(KeyEvent.VK_RIGHT);
    }

    @Override
    public boolean isRotatingLeft() {
        return isKeyHeld(KeyEvent.VK_Q);
    }

    @Override
    public boolean isRotatingRight() {
        return isKeyHeld(KeyEvent.VK_E);
    }

    @Override
    public boolean isFiring() {
        return isKeyHeld(KeyEvent.VK_SPACE);
    }

    protected void handleOtherInput(int key) {
        if (key == KeyEvent.VK_X) {
            int[] coords = renderer.getMap().getRandomFreeCoordinate();
            /*
            try {
                SpriteEntity ammoEntity = new SpriteEntity(coords[0], coords[1], ImageIO.read(this.getClass().getResourceAsStream("/potato/sprites/gun/pistol-ammo.png")), 0);
                ammoEntity.setOnInteractPlayer(() -> {
                    player.getWeapon().addAmmo(20);
                    ammoEntity.deactivate();
                });
                renderer.entities.add(ammoEntity);
                new Logger(this.getClass().getName()).log("Ammo Entity at " + coords[0] + "," + coords[1]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
             */
            Enemies.spawnShroomEnemy(coords[0], coords[1]);
        }
        if (key == KeyEvent.VK_1) {
            player.setWeapon(Weapons.PISTOL);
        }
        if (key == KeyEvent.VK_2) {
            player.setWeapon(Weapons.SHOTGUN);
        }

        if (key == KeyEvent.VK_ESCAPE)
        {
            Game.setPaused(!Game.isPaused());
        }

        if (key == KeyEvent.VK_DOWN)
        {
            renderer.pauseMenu.moveSelectionDown();
        }

        if (key == KeyEvent.VK_UP)
        {
            renderer.pauseMenu.moveSelectionUp();
        }
    }
}

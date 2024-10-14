package potato.input;

import potato.*;
import sun.rmi.runtime.Log;

import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;
import java.io.IOException;
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
            Enemies.spawnEntity(coords[0], coords[1], Enemies.SHROOM_TEXTURES, 1.2);
        }
    }
}

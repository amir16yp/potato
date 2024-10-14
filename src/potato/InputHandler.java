package potato;

import javax.imageio.ImageIO;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class InputHandler extends KeyAdapter {
    private final Renderer renderer;
    private final Set<Integer> heldKeys = new HashSet<>();
    private boolean mouseLeftButtonHeld = false;

    public InputHandler(Renderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        heldKeys.add(key);

        handleGameInput(key);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        heldKeys.remove(e.getKeyCode());
    }

    public boolean isKeyHeld(int keyCode) {
        return heldKeys.contains(keyCode);
    }

    public boolean isMovingForward() {
        return isKeyHeld(KeyEvent.VK_W) || isKeyHeld(KeyEvent.VK_UP);
    }

    public boolean isMovingBackward() {
        return isKeyHeld(KeyEvent.VK_S) || isKeyHeld(KeyEvent.VK_DOWN);
    }

    public boolean isStrafingLeft() {
        return isKeyHeld(KeyEvent.VK_A) || isKeyHeld(KeyEvent.VK_LEFT);
    }

    public boolean isStrafingRight() {
        return isKeyHeld(KeyEvent.VK_D) || isKeyHeld(KeyEvent.VK_RIGHT);
    }

    public boolean isRotatingLeft() {
        return isKeyHeld(KeyEvent.VK_Q);
    }

    public boolean isRotatingRight() {
        return isKeyHeld(KeyEvent.VK_E);
    }

    public boolean isFiring() {
        return isKeyHeld(KeyEvent.VK_SPACE);
    }

    private void handleGameInput(int key) {
        if (key == KeyEvent.VK_X)
        {
            try {
                renderer.entities.add(new Entity(Game.player.getX(), Game.player.getY(), ImageIO.read(this.getClass().getResourceAsStream("Mushie.png")), 0));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
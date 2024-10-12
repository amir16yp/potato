package potato;

import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

public class InputHandler extends KeyAdapter implements MouseListener {
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

        if (!renderer.isGameStarted()) {
            handleMenuInput(key);
        } else if (renderer.isPaused()) {
            handlePausedInput(key);
        } else {
            handleGameInput(key);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        heldKeys.remove(e.getKeyCode());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseLeftButtonHeld = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseLeftButtonHeld = false;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
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

    private void handleMenuInput(int key) {
        if (!renderer.isSeedEntered()) {
            if (Character.isDigit(key)) {
                renderer.addToSeed((char) key);
            } else if (key == KeyEvent.VK_BACK_SPACE) {
                renderer.backspaceSeed();
            } else if (key == KeyEvent.VK_ENTER) {
                renderer.confirmSeed();
            }
        } else if (key == KeyEvent.VK_ENTER) {
            renderer.startGame();
        }
    }

    private void handlePausedInput(int key) {
        if (key == KeyEvent.VK_P) {
            renderer.resumeGame();
        }
    }

    private void handleGameInput(int key) {
        if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_P) {
            renderer.pauseGame();
        }
    }
}
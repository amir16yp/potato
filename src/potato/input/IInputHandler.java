package potato.input;

import java.awt.event.KeyEvent;

// Base interface for all input handlers
public interface IInputHandler {
    void keyPressed(KeyEvent e);
    void keyReleased(KeyEvent e);
    boolean isMovingForward();
    boolean isMovingBackward();
    boolean isStrafingLeft();
    boolean isStrafingRight();
    boolean isRotatingLeft();
    boolean isRotatingRight();
    boolean isFiring();
}
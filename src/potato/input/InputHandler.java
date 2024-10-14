package potato.input;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class InputHandler extends KeyAdapter {
    private final List<InputHandlerExtension> extensions = new ArrayList<>();
    private IInputHandler activeHandler;

    public InputHandler() {
        this.activeHandler = new DefaultInputHandler();
    }

    public void setActiveHandler(IInputHandler handler) {
        this.activeHandler = handler;
    }

    public void registerExtension(InputHandlerExtension extension) {
        extensions.add(extension);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        activeHandler.keyPressed(e);
        for (InputHandlerExtension extension : extensions) {
            extension.onKeyPressed(e.getKeyCode());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        activeHandler.keyReleased(e);
        for (InputHandlerExtension extension : extensions) {
            extension.onKeyReleased(e.getKeyCode());
        }
    }

    public boolean isMovingForward() {
        return activeHandler.isMovingForward();
    }

    public boolean isMovingBackward() {
        return activeHandler.isMovingBackward();
    }

    public boolean isStrafingLeft() {
        return activeHandler.isStrafingLeft();
    }

    public boolean isStrafingRight() {
        return activeHandler.isStrafingRight();
    }

    public boolean isRotatingLeft() {
        return activeHandler.isRotatingLeft();
    }

    public boolean isRotatingRight() {
        return activeHandler.isRotatingRight();
    }

    public boolean isFiring() {
        return activeHandler.isFiring();
    }

    // Interface for InputHandler extensions
    public interface InputHandlerExtension {
        void onKeyPressed(int keyCode);

        void onKeyReleased(int keyCode);
    }
}

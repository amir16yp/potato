package potato.input;

import potato.Game;
import potato.Weapons;

import java.awt.event.KeyEvent;
import java.io.IOException;

public class LinuxGamepadInputHandler implements IInputHandler, AutoCloseable {
    private final LinuxGamepad gamepad;
    private boolean movingForward = false;
    private boolean movingBackward = false;
    private boolean strafingLeft = false;
    private boolean strafingRight = false;
    private boolean rotatingLeft = false;
    private boolean rotatingRight = false;
    private boolean firing = false;
    private Thread eventThread;
    private boolean threadRunning = true; // To control the thread
    private static final float AXIS_THRESHOLD = 0.5f;

    public LinuxGamepadInputHandler(int deviceNumber) throws IOException {
        this.gamepad = new LinuxGamepad(deviceNumber);
        this.gamepad.setEventHandler(new GamepadEventHandler());
        startEventThread();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Not used for gamepad input
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Not used for gamepad input
    }

    private void startEventThread() {
        eventThread = new Thread(() -> {
            while (threadRunning) {
                try {
                    gamepad.processEvents(); // Continuously process gamepad events
                    Thread.sleep(10); // Small delay to prevent excessive CPU usage
                } catch (IOException e) {
                    System.err.println("Error processing gamepad events: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                    break;
                }
            }
        });
        eventThread.start(); // Start the thread
    }

    @Override
    public boolean isMovingForward() {
        return movingForward;
    }

    @Override
    public boolean isMovingBackward() {
        return movingBackward;
    }

    @Override
    public boolean isStrafingLeft() {
        return strafingLeft;
    }

    @Override
    public boolean isStrafingRight() {
        return strafingRight;
    }

    @Override
    public boolean isRotatingLeft() {
        return rotatingLeft;
    }

    @Override
    public boolean isRotatingRight() {
        return rotatingRight;
    }

    @Override
    public boolean isFiring() {
        return firing;
    }

    @Override
    public void close() throws IOException {
        gamepad.close();
    }

    // Inner class to handle gamepad events
    private class GamepadEventHandler implements LinuxGamepad.GamepadEventHandler {
        @Override
        public void onButtonEvent(String buttonName, boolean pressed) {
            switch (buttonName) {
                case "A":
                    firing = pressed;
                    break;
                case "LB":
                    rotatingLeft = pressed;
                    break;
                case "RB":
                    rotatingRight = pressed;
                    break;
                case "X":
                    if (pressed){
                        if (Game.player.getWeapon() == Weapons.PISTOL)
                        {
                            Game.player.setWeapon(Weapons.SHOTGUN);
                        } else {
                            Game.player.setWeapon(Weapons.PISTOL);
                        }
                    }

                    break;
                case "Start":
                    Game.setPaused(!Game.isPaused());
                    break;

                // Add more button mappings as needed
            }
        }

        @Override
        public void onAxisEvent(String axisName, float value) {
            switch (axisName) {
                case "DPAD X":
                    strafingLeft = value < -AXIS_THRESHOLD;
                    strafingRight = value > AXIS_THRESHOLD;
                    break;
                case "DPAD Y":
                    movingForward = value < -AXIS_THRESHOLD;
                    movingBackward = value > AXIS_THRESHOLD;
                    break;
                // Add more axis mappings as needed
            }
        }
    }
}
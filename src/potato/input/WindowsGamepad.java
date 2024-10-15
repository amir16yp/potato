package potato.input;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class WindowsGamepad {
    private static final int MAX_CONTROLLERS = 4;
    private static final int XINPUT_GAMEPAD_LEFT_THUMB_DEADZONE = 7849;
    private static final int XINPUT_GAMEPAD_RIGHT_THUMB_DEADZONE = 8689;
    private static final int XINPUT_GAMEPAD_TRIGGER_THRESHOLD = 30;

    private final int controllerIndex;
    private Object state;
    private Method xInputGetState;
    private Field wButtons, bLeftTrigger, bRightTrigger, sThumbLX, sThumbLY, sThumbRX, sThumbRY;

    private static final Map<Integer, String> BUTTON_MAP = new HashMap<>();
    static {
        BUTTON_MAP.put(0x1000, "A");
        BUTTON_MAP.put(0x2000, "B");
        BUTTON_MAP.put(0x4000, "X");
        BUTTON_MAP.put(0x8000, "Y");
        BUTTON_MAP.put(0x0100, "LB");
        BUTTON_MAP.put(0x0200, "RB");
        BUTTON_MAP.put(0x0020, "Back");
        BUTTON_MAP.put(0x0010, "Start");
        BUTTON_MAP.put(0x0001, "D-Pad Up");
        BUTTON_MAP.put(0x0002, "D-Pad Down");
        BUTTON_MAP.put(0x0004, "D-Pad Left");
        BUTTON_MAP.put(0x0008, "D-Pad Right");
    }

    public WindowsGamepad(int controllerIndex) {
        if (controllerIndex < 0 || controllerIndex >= MAX_CONTROLLERS) {
            throw new IllegalArgumentException("Controller index must be between 0 and 3");
        }
        this.controllerIndex = controllerIndex;
        initializeXInput();
    }

    private void initializeXInput() {
        try {
            Class<?> xInputClass = Class.forName("com.sun.jna.Native");
            Method loadMethod = xInputClass.getMethod("load", String.class, Class.class);
            Object xinputInstance = loadMethod.invoke(null, "XInput1_4", Class.forName("potato.input.WindowsGamepad$XInput"));

            xInputGetState = xinputInstance.getClass().getMethod("XInputGetState", int.class, Class.forName("potato.input.WindowsGamepad$State$ByReference"));

            Class<?> stateClass = Class.forName("potato.input.WindowsGamepad$State");
            Class<?> byReferenceClass = Class.forName("potato.input.WindowsGamepad$State$ByReference");
            state = byReferenceClass.newInstance();

            Class<?> gamepadClass = Class.forName("potato.input.WindowsGamepad$Gamepad");
            wButtons = gamepadClass.getField("wButtons");
            bLeftTrigger = gamepadClass.getField("bLeftTrigger");
            bRightTrigger = gamepadClass.getField("bRightTrigger");
            sThumbLX = gamepadClass.getField("sThumbLX");
            sThumbLY = gamepadClass.getField("sThumbLY");
            sThumbRX = gamepadClass.getField("sThumbRX");
            sThumbRY = gamepadClass.getField("sThumbRY");
        } catch (Exception e) {
            System.out.println("XInput initialization failed. This is expected on non-Windows systems.");
        }
    }

    public boolean isConnected() {
        if (xInputGetState == null) return false;
        try {
            int result = (int) xInputGetState.invoke(null, controllerIndex, state);
            return result == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void processEvents() {
        if (!isConnected()) return;

        try {
            xInputGetState.invoke(null, controllerIndex, state);
            Object gamepad = state.getClass().getField("Gamepad").get(state);

            short buttons = wButtons.getShort(gamepad);
            for (Map.Entry<Integer, String> entry : BUTTON_MAP.entrySet()) {
                processButton(buttons, entry.getKey(), entry.getValue());
            }

            processAnalogStick("Left Stick X", sThumbLX.getShort(gamepad), XINPUT_GAMEPAD_LEFT_THUMB_DEADZONE);
            processAnalogStick("Left Stick Y", sThumbLY.getShort(gamepad), XINPUT_GAMEPAD_LEFT_THUMB_DEADZONE);
            processAnalogStick("Right Stick X", sThumbRX.getShort(gamepad), XINPUT_GAMEPAD_RIGHT_THUMB_DEADZONE);
            processAnalogStick("Right Stick Y", sThumbRY.getShort(gamepad), XINPUT_GAMEPAD_RIGHT_THUMB_DEADZONE);

            processTrigger("Left Trigger", bLeftTrigger.getByte(gamepad));
            processTrigger("Right Trigger", bRightTrigger.getByte(gamepad));

        } catch (Exception e) {
            System.out.println("Error processing gamepad events: " + e.getMessage());
        }
    }

    private void processButton(short buttons, int mask, String buttonName) {
        boolean isPressed = (buttons & mask) != 0;
        System.out.printf("Button %s %s%n", buttonName, isPressed ? "pressed" : "released");
    }

    private void processAnalogStick(String stickName, short value, int deadzone) {
        float normalizedValue = Math.abs(value) < deadzone ? 0 : value / 32767f;
        System.out.printf("Axis %s: %.2f%n", stickName, normalizedValue);
    }

    private void processTrigger(String triggerName, byte value) {
        float normalizedValue = value < XINPUT_GAMEPAD_TRIGGER_THRESHOLD ? 0 : value / 255f;
        System.out.printf("Axis %s: %.2f%n", triggerName, normalizedValue);
    }

    public static void main(String[] args) {
        WindowsGamepad gamepad = new WindowsGamepad(0);
        System.out.println("Gamepad connected. Press Ctrl+C to exit.");
        while (true) {
            gamepad.processEvents();
            try {
                Thread.sleep(16); // Poll at roughly 60Hz
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    // XInput interface and structure definitions (not used directly, but needed for reflection)
    public interface XInput {
        int XInputGetState(int dwUserIndex, State.ByReference state);
    }

    public static class Gamepad {
        public short wButtons;
        public byte bLeftTrigger;
        public byte bRightTrigger;
        public short sThumbLX;
        public short sThumbLY;
        public short sThumbRX;
        public short sThumbRY;
    }

    public static class State {
        public int dwPacketNumber;
        public Gamepad Gamepad;

        public static class ByReference extends State {}
    }
}
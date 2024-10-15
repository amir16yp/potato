package potato.input;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;


public class LinuxGamepad implements AutoCloseable {
    private static final String DEVICE_PATH = "/dev/input/js";
    private final FileChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

    private static final Map<Byte, String> BUTTON_MAP = new HashMap<>();
    private static final Map<Byte, String> AXIS_MAP = new HashMap<>();

    static {
        BUTTON_MAP.put((byte) 0, "A");
        BUTTON_MAP.put((byte) 1, "B");
        BUTTON_MAP.put((byte) 2, "X");
        BUTTON_MAP.put((byte) 3, "Y");
        BUTTON_MAP.put((byte) 4, "LB");
        BUTTON_MAP.put((byte) 5, "RB");
        BUTTON_MAP.put((byte) 6, "Back");
        BUTTON_MAP.put((byte) 7, "Start");

        AXIS_MAP.put((byte) 0, "Left Stick X");
        AXIS_MAP.put((byte) 1, "Left Stick Y");
        AXIS_MAP.put((byte) 2, "Left Trigger");
        AXIS_MAP.put((byte) 3, "Right Stick Y");
        AXIS_MAP.put((byte) 4, "Right Stick X");
        AXIS_MAP.put((byte) 5, "Right Trigger");
        AXIS_MAP.put((byte) 6, "DPAD X");
        AXIS_MAP.put((byte) 7, "DPAD Y");
    }

    public LinuxGamepad(int deviceNumber) throws IOException {
        Path path = Paths.get(DEVICE_PATH + deviceNumber);
        if (!path.toFile().exists()) {
            throw new IOException("Gamepad device not found: " + path);
        }
        this.channel = FileChannel.open(path, StandardOpenOption.READ);
    }

    private GamepadEvent readEvent() throws IOException {
        buffer.clear();
        int bytesRead = channel.read(buffer);
        if (bytesRead == 8) {
            buffer.flip();
            int time = buffer.getInt();
            short value = buffer.getShort();
            byte type = buffer.get();
            byte number = buffer.get();
            return new GamepadEvent(time, value, type, number);
        }
        return null;
    }

    public void processEvents() throws IOException {
        GamepadEvent event = readEvent();
        if (event != null && eventHandler != null) {
            if (event.type == 1) {  // Button event
                String buttonName = BUTTON_MAP.getOrDefault(event.number, "Unknown");
                eventHandler.onButtonEvent(buttonName, event.value == 1);
            } else if (event.type == 2) {  // Axis event
                String axisName = AXIS_MAP.getOrDefault(event.number, "Unknown");
                float normalizedValue = event.value / 32767f;
                eventHandler.onAxisEvent(axisName, normalizedValue);
            }
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    public static void main(String[] args) {
        try (LinuxGamepad gamepad = new LinuxGamepad(0)) {
            System.out.println("Gamepad connected. Press Ctrl+C to exit.");
            while (true) {
                gamepad.processEvents();
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private GamepadEventHandler eventHandler;

    public void setEventHandler(GamepadEventHandler handler) {
        this.eventHandler = handler;
    }



    public interface GamepadEventHandler {
        void onButtonEvent(String buttonName, boolean pressed);
        void onAxisEvent(String axisName, float value);
    }

    private static class GamepadEvent {
        final int time;
        final short value;
        final byte type;
        final byte number;

        GamepadEvent(int time, short value, byte type, byte number) {
            this.time = time;
            this.value = value;
            this.type = type;
            this.number = number;
        }

        @Override
        public String toString() {
            return String.format("GamepadEvent{time=%d, value=%d, type=%d, number=%d}", time, value, type, number);
        }
    }
}
package potato;

import java.util.Random;

public class Map {
    private int[][] map;
    private final int width;
    private final int height;
    private final int minRoomSize = 5;
    private final int maxRoomSize = 15;
    private final Random random;

    public Map(int width, int height, long seed) {
        this.width = width;
        this.height = height;
        this.random = new Random(seed);
        generateMap();
    }

    private void generateMap() {
        map = new int[height][width];

        // Fill the map with walls
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    map[y][x] = 2; // Outer walls
                } else {
                    map[y][x] = 0; // Empty space
                }
            }
        }

        // Generate rooms
        int numRooms = (width * height) / (maxRoomSize * maxRoomSize);
        for (int i = 0; i < numRooms; i++) {
            int roomWidth = random.nextInt(maxRoomSize - minRoomSize + 1) + minRoomSize;
            int roomHeight = random.nextInt(maxRoomSize - minRoomSize + 1) + minRoomSize;
            int roomX = random.nextInt(width - roomWidth - 1) + 1;
            int roomY = random.nextInt(height - roomHeight - 1) + 1;

            createRoom(roomX, roomY, roomWidth, roomHeight);
        }

        // Connect rooms with doors
        connectRooms();
    }

    private void createRoom(int x, int y, int width, int height) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                if (dx == 0 || dx == width - 1 || dy == 0 || dy == height - 1) {
                    map[y + dy][x + dx] = 2; // Room walls
                }
            }
        }
    }

    private void connectRooms() {
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (map[y][x] == 1 && random.nextDouble() < 0.1) {
                    // Check if it's a valid position for a door
                    if ((map[y][x-1] == 0 && map[y][x+1] == 0) ||
                            (map[y-1][x] == 0 && map[y+1][x] == 0)) {
                        map[y][x] = 2; // Place a door
                    }
                }
            }
        }
    }

    public boolean isWall(int x, int y) {
        return map[y][x] >= 1;
    }

    public int getTextureId(int x, int y) {
        return map[y][x];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // Method to print the map (for debugging)
    public void printMap() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                System.out.print(map[y][x] + " ");
            }
            System.out.println();
        }
    }
}
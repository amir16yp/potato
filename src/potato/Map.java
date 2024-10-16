package potato;

import java.awt.image.BufferedImage;
import java.util.Random;

public class Map {
    public static final int[] wallTextureIDs = {31};
    protected int width;
    protected int height;
    protected Random random;
    protected Logger logger;
    private int minRoomSize;
    private int maxRoomSize;
    protected int[][] map;
    protected BufferedImage floorImage;
    protected BufferedImage ceilingImage;

    public Map(int width, int height, long seed) {
        this(width, height, seed, 5, 15);
    }

    public Map(int[][] data) {
        this.map = data;
        this.height = data.length;
        this.width = data[0].length;
        this.logger = new Logger(this.getClass().getName());
        // Initialize other necessary fields
        this.random = new Random();
        this.minRoomSize = 5;
        this.maxRoomSize = 15;
        //this.ceilingImage = Game.textures.getTile(13);
        //this.floorImage = Game.textures.getTile(17);
    }

    public Map(int width, int height, long seed, int minRoomSize, int maxRoomSize) {
        this.width = width;
        this.height = height;
        this.random = new Random(seed);
        this.minRoomSize = minRoomSize;
        this.maxRoomSize = maxRoomSize;
        this.logger = new Logger(this.getClass().getName());
        //this.ceilingImage = Game.textures.getTile(13);
        //this.floorImage = Game.textures.getTile(17);
        generateMap();
    }

    public BufferedImage getCeilingImage() {
        return ceilingImage;
    }

    public BufferedImage getFloorImage() {
        return floorImage;
    }

    private int getWallTextureID() {
        return wallTextureIDs[random.nextInt(wallTextureIDs.length)];
    }

    protected void generateMap() {
        initializeMap();
        generateRooms();
        connectRooms();
        addRandomFeatures();
    }

    private void initializeMap() {
        map = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = (x == 0 || x == width - 1 || y == 0 || y == height - 1) ? getWallTextureID() : 0;
            }
        }
    }

    public int[] getRandomFreeCoordinate() {
        int x, y;
        int attempts = 0;
        int maxAttempts = 100; // Limit to avoid infinite loops

        do {
            x = random.nextInt(width);
            y = random.nextInt(height);
            attempts++;
            if (attempts > maxAttempts) {
                logger.log("Max attempts reached while trying to find a free coordinate.");
                return null; // Return null if no free space is found
            }
        } while (map[y][x] != 0); // Keep trying until a free space is found

        return new int[]{x, y}; // Return the found coordinates
    }


    private void generateRooms() {
        int numRooms = (width * height) / (maxRoomSize * maxRoomSize);
        for (int i = 0; i < numRooms; i++) {
            int roomWidth = random.nextInt(maxRoomSize - minRoomSize + 1) + minRoomSize;
            int roomHeight = random.nextInt(maxRoomSize - minRoomSize + 1) + minRoomSize;
            int roomX = random.nextInt(width - roomWidth - 1) + 1;
            int roomY = random.nextInt(height - roomHeight - 1) + 1;
            createRoom(roomX, roomY, roomWidth, roomHeight);
        }
    }

    private void createRoom(int x, int y, int width, int height) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                if (dx == 0 || dx == width - 1 || dy == 0 || dy == height - 1) {
                    map[y + dy][x + dx] = getWallTextureID();
                }
            }
        }
    }

    private void connectRooms() {
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (map[y][x] != 0 && random.nextDouble() < 0.1) {
                    if ((map[y][x - 1] == 0 && map[y][x + 1] == 0) ||
                            (map[y - 1][x] == 0 && map[y + 1][x] == 0)) {
                        map[y][x] = 0; // Place a door
                    }
                }
            }
        }
    }

    private void addRandomFeatures() {
        // Add some random features to the map (e.g., pillars, decorations)
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (map[y][x] == 0 && random.nextDouble() < 0.01) {
                    map[y][x] = getWallTextureID(); // Add a random feature
                }
            }
        }
    }

    public boolean isWall(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            logger.log("Out of bounds wall check: " + x + "," + y);
            return true; // Treat out of bounds as walls
        }
        return map[y][x] != 0;
    }

    public BufferedImage getTexture(int x, int y) {
        try {
            return Game.textures.getTile(map[y][x]);
        } catch (Exception e) {
            logger.log("Error getting texture for tile at " + x + "," + y + ": " + e.getMessage());
            return OutdoorMap.createSkyGradient(32, 32);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void printMap() {
        for (int y = 0; y < height; y++) {
            StringBuilder rowBuilder = new StringBuilder("["); // Start the row with an opening bracket
            for (int x = 0; x < width; x++) {
                rowBuilder.append(map[y][x]); // Append the tile ID
                if (x < width - 1) {
                    rowBuilder.append(", "); // Add a comma and space if not the last element
                }
            }
            rowBuilder.append("]"); // Close the row with a closing bracket
            logger.log(rowBuilder.toString()); // Log the formatted row
        }
    }

    public int getTileID(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return -1; // Invalid tile
        }
        return map[y][x];
    }

    public void setTile(int x, int y, int tileID) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            map[y][x] = tileID;
        } else {
            logger.log("Attempt to set tile out of bounds: " + x + "," + y);
        }
    }

    public int[][] getMapData() {
        return map;
    }

    public void setMapData(int[][] mapData) {
        this.map = mapData;
    }
}
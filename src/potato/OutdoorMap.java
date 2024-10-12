package potato;

import java.awt.*;
import java.awt.image.BufferedImage;

public class OutdoorMap extends Map {
    public OutdoorMap(int width, int height, long seed) {
        super(width, height, seed);
        this.ceilingImage = createSkyGradient(32, 32);
    }

    public static BufferedImage createSkyGradient(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        Color skyBlue = new Color(135, 206, 235);
        Color darkBlue = new Color(0, 0, 139);
        GradientPaint gp = new GradientPaint(0, 0, skyBlue, 0, height, darkBlue);

        g2d.setPaint(gp);
        g2d.fillRect(0, 0, width, height);

        g2d.dispose();
        return image;
    }

    @Override
    protected void generateMap() {
        map = new int[height][width];

        // Fill the map with empty space (0)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = 0;
            }
        }

        // Add some random obstacles or terrain features
        for (int i = 0; i < width * height / 50; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            map[y][x] = 2; // Use 2 for obstacles/terrain
        }

        // Ensure the player has a clear starting area
        int clearRadius = 5;
        for (int y = height / 2 - clearRadius; y < height / 2 + clearRadius; y++) {
            for (int x = width / 2 - clearRadius; x < width / 2 + clearRadius; x++) {
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    map[y][x] = 0;
                }
            }
        }
    }
}
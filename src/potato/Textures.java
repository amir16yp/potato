package potato;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;

public class Textures {
    private BufferedImage tilesetImage;
    private HashMap<Integer, BufferedImage> tiles;
    private int tileWidth;
    private int tileHeight;

    public Textures(String tilesetPath, int tileWidth, int tileHeight) {
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        tiles = new HashMap<>();
        loadTileset(tilesetPath);
    }

    private void loadTileset(String tilesetPath) {
        try {
            tilesetImage = ImageIO.read(this.getClass().getResourceAsStream(tilesetPath));
            int cols = tilesetImage.getWidth() / tileWidth;
            int rows = tilesetImage.getHeight() / tileHeight;

            int id = 1; // Start IDs from 1
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    BufferedImage tile = tilesetImage.getSubimage(
                            x * tileWidth, y * tileHeight, tileWidth, tileHeight);
                    tiles.put(id, tile);
                    id++;
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading tileset: " + e.getMessage());
        }
    }

    public BufferedImage getTile(int id) {
        return tiles.get(id);
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }
}
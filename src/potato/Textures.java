package potato;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Textures {
    private final Map<Integer, BufferedImage> tiles;
    private final int tileWidth;
    private final int tileHeight;
    private final Logger logger;

    public Textures(String tilesetPath, int tileWidth, int tileHeight) {
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tiles = new HashMap<>();
        this.logger = new Logger(this.getClass().getName());
        loadTileset(tilesetPath);
    }

    private void loadTileset(String tilesetPath) {
        try {
            BufferedImage tilesetImage = ImageIO.read(getClass().getResourceAsStream(tilesetPath));
            if (tilesetImage == null) {
                throw new IOException("Failed to load tileset image");
            }

            int cols = tilesetImage.getWidth() / tileWidth;
            int rows = tilesetImage.getHeight() / tileHeight;

            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    int id = y * cols + x + 1; // Start IDs from 1
                    BufferedImage tile = tilesetImage.getSubimage(
                            x * tileWidth, y * tileHeight, tileWidth, tileHeight);
                    tiles.put(id, tile);
                }
            }
            logger.Log("Loaded " + tiles.size() + " tiles");
        } catch (IOException e) {
            logger.Log("Error loading tileset: " + e.getMessage());
            throw new RuntimeException("Failed to load tileset", e);
        }
    }

    public BufferedImage getTile(int id) {
        BufferedImage tile = tiles.get(id);
        if (tile == null) {
            logger.Log("Warning: Tile with ID " + id + " not found");
            return createPlaceholderTile();
        }
        return tile;
    }

    private BufferedImage createPlaceholderTile() {
        BufferedImage placeholder = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB);
        // Fill with a noticeable color or pattern
        // For example, a magenta and black checkered pattern
        for (int y = 0; y < tileHeight; y++) {
            for (int x = 0; x < tileWidth; x++) {
                placeholder.setRGB(x, y, ((x + y) % 2 == 0) ? 0xFFFF00FF : 0xFF000000);
            }
        }
        return placeholder;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getTileCount() {
        return tiles.size();
    }
}
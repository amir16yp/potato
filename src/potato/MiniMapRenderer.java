package potato;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MiniMapRenderer {
    private final Map map;
    private final Textures textures;
    private final int tileWidth;
    private final int tileHeight;
    private final Logger logger;

    public MiniMapRenderer(Map map, Textures textures) {
        this.map = map;
        this.textures = textures;
        this.tileWidth = textures.getTileWidth();
        this.tileHeight = textures.getTileHeight();
        this.logger = new Logger(this.getClass().getName());
    }

    public BufferedImage renderMap() {
        int mapWidth = map.getWidth();
        int mapHeight = map.getHeight();
        BufferedImage renderedMap = new BufferedImage(
                mapWidth * tileWidth,
                mapHeight * tileHeight,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = renderedMap.createGraphics();

        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                int tileID = map.getTileID(x, y);
                BufferedImage tileImage;

                if (tileID == 0) {
                    tileImage = map.getFloorImage();
                } else {
                    tileImage = textures.getTile(tileID);
                }

                if (tileImage != null) {
                    g2d.drawImage(tileImage, x * tileWidth, y * tileHeight, null);
                } else {
                    logger.log("Warning: No image found for tile ID " + tileID + " at position (" + x + ", " + y + ")");
                }
            }
        }

        g2d.dispose();
        return renderedMap;
    }

    public BufferedImage renderMiniMap(int scale, Player player) {
        int mapWidth = map.getWidth();
        int mapHeight = map.getHeight();
        BufferedImage miniMap = new BufferedImage(
                mapWidth * scale,
                mapHeight * scale,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = miniMap.createGraphics();

        // Draw map tiles
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                int tileID = map.getTileID(x, y);

                if (tileID == 0) {
                    g2d.setColor(Color.WHITE);
                } else {
                    g2d.setColor(Color.BLACK);
                }

                g2d.fillRect(x * scale, y * scale, scale, scale);
            }
        }

        // Draw player
        int playerX = (int) (player.getX() * scale);
        int playerY = (int) (player.getY() * scale);
        g2d.setColor(Color.RED);
        g2d.fillOval(playerX - scale/2, playerY - scale/2, scale, scale);

        // Draw player direction
        int dirX = playerX + (int) (Math.cos(player.getAngle()) * scale * 2);
        int dirY = playerY + (int) (Math.sin(player.getAngle()) * scale * 2);
        g2d.drawLine(playerX, playerY, dirX, dirY);

        g2d.dispose();
        return miniMap;
    }

    public void saveMapAsImage(String filePath, String format) {
        BufferedImage mapImage = renderMap();
        File outputFile = new File(filePath);
        try {
            ImageIO.write(mapImage, format, outputFile);
            logger.log("Map saved successfully to: " + filePath);
        } catch (IOException e) {
            logger.log("Error saving map image: " + e.getMessage());
        }
    }

    public void saveMiniMapAsImage(String filePath, String format, int scale) {
        BufferedImage miniMapImage = renderMiniMap(scale, Game.player);
        File outputFile = new File(filePath);
        try {
            ImageIO.write(miniMapImage, format, outputFile);
            logger.log("Mini map saved successfully to: " + filePath);
        } catch (IOException e) {
            logger.log("Error saving mini map image: " + e.getMessage());
        }
    }
}
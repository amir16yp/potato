package potato;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Renderer {
    private static int HALF_HEIGHT;
    public static final double FOV = Math.PI / 3;
    public static final double HALF_FOV = FOV / 2;
    private static final double MAX_DISTANCE = 20.0;
    private double weaponBobOffset = 0;
    private static final double WEAPON_BOB_AMOUNT = 5.0;
    private static final double WEAPON_BOB_SPEED = 0.05;

    private final int width;
    private final int height;
    private final BufferStrategy bufferStrategy;
    private Map map = null;
    private final Player player;
    public final CopyOnWriteArrayList<Entity> entities = new CopyOnWriteArrayList<>();
    private final Textures textures;

    private double[] zBuffer;
    private BufferedImage offScreenBuffer;
    private Graphics2D offScreenGraphics;

    public Renderer(int width, int height, BufferStrategy bufferStrategy, Player player, Textures textures) {
        this.width = width;
        this.height = height;
        HALF_HEIGHT = height / 2;
        this.bufferStrategy = bufferStrategy;
        this.player = player;
        this.textures = textures;

        this.zBuffer = new double[width];
        this.offScreenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.offScreenGraphics = offScreenBuffer.createGraphics();
    }

    public void render(long fps) {
        offScreenGraphics.setColor(Color.BLACK);
        offScreenGraphics.fillRect(0, 0, width, height);

        renderGame(offScreenGraphics, fps);

        Graphics2D g2d = (Graphics2D) bufferStrategy.getDrawGraphics();
        g2d.drawImage(offScreenBuffer, 0, 0, null);
        g2d.dispose();
        bufferStrategy.show();
    }

    private void renderGame(Graphics2D g2d, long fps) {
        if (map == null) {
            initializeMap();
        }

        for (int i = 0; i < width; i++) {
            zBuffer[i] = Double.MAX_VALUE;
        }

        drawCeilingAndFloor(g2d);
        castRays(g2d);
        renderEntities(g2d);
        renderProjectiles(g2d);
        renderWeapon(g2d);
        drawFPS(g2d, fps);
    }

    private void initializeMap() {
        // Initialize the map with a default size or load from a file
        map = new Map(100, 100, new Random().nextLong()); // Adjust size as needed
    }

    private void drawCeilingAndFloor(Graphics2D g2d) {
        for (int y = 0; y < height; y++) {
            if (y < HALF_HEIGHT) {
                g2d.setColor(new Color(100, 100, 200)); // Sky color
            } else {
                g2d.setColor(new Color(50, 50, 50)); // Floor color
            }
            g2d.drawLine(0, y, width, y);
        }
    }

    private void castRays(Graphics2D g) {
        double startAngle = player.getAngle() - HALF_FOV;
        for (int ray = 0; ray < width; ray++) {
            double angle = startAngle + (ray / (double) width) * FOV;
            double sinA = Math.sin(angle);
            double cosA = Math.cos(angle);
            double x = player.getX();
            double y = player.getY();

            if (Math.abs(angle - player.getAngle()) > HALF_FOV) continue;

            for (double distance = 0; distance < MAX_DISTANCE; distance += 0.1) {
                x += 0.1 * cosA;
                y += 0.1 * sinA;
                int mapX = (int) x;
                int mapY = (int) y;

                if (mapX < 0 || mapX >= map.getWidth() || mapY < 0 || mapY >= map.getHeight()) {
                    break;
                }

                if (map.isWall(mapX, mapY)) {
                    distance *= Math.cos(player.getAngle() - angle);
                    int wallHeight = Math.min((int) (height / distance), height);

                    BufferedImage tileTexture = map.getTexture(mapX, mapY);
                    if (tileTexture == null) {
                        break;
                    }

                    double wallX = (Math.abs(y - Math.floor(y)) < Math.abs(x - Math.floor(x))) ? y % 1 : x % 1;
                    int textureX = (int)(wallX * tileTexture.getWidth());
                    textureX = Math.max(0, Math.min(textureX, tileTexture.getWidth() - 1));

                    if (distance < zBuffer[ray]) {
                        zBuffer[ray] = distance;
                        drawWallSlice(g, ray, wallHeight, tileTexture, textureX, distance);
                    }
                    break;
                }
            }
        }
    }

    private void drawWallSlice(Graphics2D g, int ray, int wallHeight, BufferedImage texture, int textureX, double distance) {
        int startY = HALF_HEIGHT - wallHeight / 2;
        int endY = startY + wallHeight;

        for (int y = startY; y < endY; y++) {
            int textureY = (int) ((y - startY) * texture.getHeight() / wallHeight);
            textureY = Math.max(0, Math.min(textureY, texture.getHeight() - 1));

            int color;
            try {
                color = texture.getRGB(textureX, textureY);
            } catch (ArrayIndexOutOfBoundsException e) {
                color = Color.MAGENTA.getRGB();
            }

            color = applyShading(color, distance);
            offScreenBuffer.setRGB(ray, y, color);
        }
    }

    private int applyShading(int color, double distance) {
        double shade = 1.0 - Math.min(distance / MAX_DISTANCE, 1.0);
        int r = (int) ((color >> 16 & 0xFF) * shade);
        int g = (int) ((color >> 8 & 0xFF) * shade);
        int b = (int) ((color & 0xFF) * shade);
        return (r << 16) | (g << 8) | b;
    }

    private void renderEntities(Graphics2D g) {
        for (Entity entity : entities) {
            double relativeX = entity.getX() - player.getX();
            double relativeY = entity.getY() - player.getY();
            double angle = Math.atan2(relativeY, relativeX) - player.getAngle();
            double distance = Math.sqrt(relativeX * relativeX + relativeY * relativeY);

            if (Math.abs(angle) < HALF_FOV) {
                int screenX = (int) ((angle + HALF_FOV) / FOV * width);
                int screenY = height / 2;
                int spriteSize = (int) (height / distance);

                BufferedImage sprite = entity.getSprite();
                drawSprite(g, sprite, screenX, screenY, spriteSize, distance);
            }
        }
    }

    private void drawSprite(Graphics2D g, BufferedImage sprite, int screenX, int screenY, int spriteSize, double distance) {
        int halfSize = spriteSize / 2;
        int startX = screenX - halfSize;
        int endX = screenX + halfSize;
        int startY = screenY - halfSize;
        int endY = screenY + halfSize;

        for (int x = startX; x < endX; x++) {
            if (x < 0 || x >= width) continue;
            if (distance >= zBuffer[x]) continue;

            int texX = (x - startX) * sprite.getWidth() / spriteSize;
            for (int y = startY; y < endY; y++) {
                if (y < 0 || y >= height) continue;

                int texY = (y - startY) * sprite.getHeight() / spriteSize;
                int color = sprite.getRGB(texX, texY);

                if ((color & 0xFF000000) != 0) {
                    color = applyShading(color, distance);
                    offScreenBuffer.setRGB(x, y, color);
                }
            }
        }
    }

    private void renderProjectiles(Graphics2D g) {
        for (Projectile projectile : player.getProjectiles()) {
            double relativeX = projectile.getX() - player.getX();
            double relativeY = projectile.getY() - player.getY();
            double angle = Math.atan2(relativeY, relativeX) - player.getAngle();
            double distance = Math.sqrt(relativeX * relativeX + relativeY * relativeY);

            if (Math.abs(angle) < HALF_FOV) {
                int screenX = (int) ((angle + HALF_FOV) / FOV * width);
                int screenY = height / 2;
                int spriteSize = (int) (height / distance);

                BufferedImage sprite = projectile.getSprite();
                drawSprite(g, sprite, screenX, screenY, spriteSize, distance);
            }
        }
    }

    private void renderWeapon(Graphics2D g) {
        if (player.getWeapon() == null) {
            return;
        }
        BufferedImage weaponTexture = player.getWeapon().getGunSprite().getCurrentFrame();
        int weaponWidth = weaponTexture.getWidth() * 3;
        int weaponHeight = weaponTexture.getHeight() * 3;

        int x = (width - weaponWidth) / 2;
        int y = height - weaponHeight;

        if (player.isMoving()) {
            weaponBobOffset += WEAPON_BOB_SPEED;
            if (weaponBobOffset > Math.PI * 2) {
                weaponBobOffset -= Math.PI * 2;
            }
        } else {
            weaponBobOffset *= 0.8;
        }

        double bobY = Math.sin(weaponBobOffset) * WEAPON_BOB_AMOUNT;
        double bobX = Math.cos(weaponBobOffset * 0.5) * WEAPON_BOB_AMOUNT * 0.5;

        g.drawImage(weaponTexture,
                (int)(x + bobX),
                (int)(y + bobY),
                weaponWidth,
                weaponHeight,
                null);
    }

    private void drawFPS(Graphics g, long fps) {
        g.setColor(Color.WHITE);
        g.drawString("FPS: " + fps, 10, 20);
    }

    public Map getMap() {
        return map;
    }
}
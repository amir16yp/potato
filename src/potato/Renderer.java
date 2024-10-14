package potato;

import potato.modsupport.Mod;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class Renderer {
    private static int HALF_HEIGHT;
    public static final double FOV = Math.toRadians(120);
    public static final double HALF_FOV = FOV / 2;
    public static final double MAX_DISTANCE = 20.0;
    private static final double WALL_HEIGHT = 1.0;
    private static final double EPSILON = 1e-4;

    private int width;
    private int height;
    private final BufferStrategy bufferStrategy;
    private Map map;
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

    public void render() {
        clearScreen();
        if (map == null) {
            map = new Map(32, 32, 123);
            map.printMap();
        }
        drawCeilingAndFloor();
        castRays();
        renderEntities();
        renderWeapon();
        renderProjectile();
        drawFPS(Game.gameLoop.getFPS());
        for (Mod mod : Game.MOD_LOADER.getLoadedMods())
        {
            mod.draw(offScreenGraphics);
        }
        presentBuffer();
    }

    private void clearScreen() {
        offScreenGraphics.setColor(Color.BLACK);
        offScreenGraphics.fillRect(0, 0, width, height);
    }

    private void renderProjectile() {
        // Use an iterator to safely remove projectiles while iterating
        Iterator<Projectile> iterator = this.player.getProjectiles().iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            if (projectile.isActive()) {
                drawProjectile(projectile);
            } else {
                iterator.remove(); // Safely remove inactive projectiles
            }
        }
    }

    private void drawProjectile(Projectile projectile) {
        double dx = projectile.getX() - player.getX();
        double dy = projectile.getY() - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Calculate the angle between player's view and the projectile
        double angle = Math.atan2(dy, dx) - player.getAngle();

        // Normalize the angle to stay within [-PI, PI]
        while (angle < -Math.PI) angle += 2 * Math.PI;
        while (angle > Math.PI) angle -= 2 * Math.PI;

        // Ensure the projectile is within the player's field of view
        if (Math.abs(angle) > HALF_FOV || distance > MAX_DISTANCE) return;

        // Calculate the screen position based on the angle
        int screenX = (int) ((angle / HALF_FOV + 1) * width / 2);

        // Calculate the projectile's size based on its distance
        double size = (height / distance) * projectile.getSize();
        int halfSize = (int) (size / 2);

        // Calculate the vertical center of the projectile on the screen
        int screenY = HALF_HEIGHT;

        // Get the projectile's sprite
        BufferedImage sprite = projectile.getSprite();

        // Draw the projectile
        for (int x = -halfSize; x < halfSize; x++) {
            if (screenX + x < 0 || screenX + x >= width) continue;
            if (distance > zBuffer[screenX + x]) continue; // Ensure it's in front of other objects

            for (int y = -halfSize; y < halfSize; y++) {
                int drawY = screenY + y;
                if (drawY < 0 || drawY >= height) continue;

                int texX = (x + halfSize) * sprite.getWidth() / (int) size;
                int texY = (y + halfSize) * sprite.getHeight() / (int) size;

                int color = sprite.getRGB(texX, texY);

                // Only draw non-transparent pixels
                if ((color & 0xFF000000) != 0) {
                    // Apply shading based on distance
                    color = applyShading(color, distance);
                    offScreenBuffer.setRGB(screenX + x, drawY, color);
                }
            }
        }
    }

    private void drawCeilingAndFloor() {
        for (int y = 0; y < height; y++) {
            if (y < HALF_HEIGHT) {
                drawHorizontalLine(y, Color.CYAN);
            } else {
                drawHorizontalLine(y, Color.DARK_GRAY);
            }
        }
    }

    private void drawHorizontalLine(int y, Color color) {
        try {
            if (y < 0 || y >= height) return; // Avoid out-of-bounds errors
            for (int x = 0; x < width; x++) {
                // Add a check to ensure x is within bounds.
                if (x < 0 || x >= width) continue;
                offScreenBuffer.setRGB(x, y, color.getRGB());
            }
        } catch (Exception e)
        {
            return;
        }

    }

    private void castRays() {
        double playerX = player.getX();
        double playerY = player.getY();
        double playerAngle = player.getAngle();

        for (int x = 0; x < width; x++) {
            double cameraX = 2 * x / (double) width - 1;
            double rayDirX = Math.cos(playerAngle) + player.getPlaneX() * cameraX;
            double rayDirY = Math.sin(playerAngle) + player.getPlaneY() * cameraX;

            RaycastHit hit = castRay(playerX, playerY, rayDirX, rayDirY);

            if (hit != null) {
                drawWallSlice(x, hit, rayDirX, rayDirY);
                zBuffer[x] = hit.distance;
            }
        }
    }

    private RaycastHit castRay(double startX, double startY, double dirX, double dirY) {
        double deltaDistX = Math.abs(1 / dirX);
        double deltaDistY = Math.abs(1 / dirY);

        int mapX = (int) startX;
        int mapY = (int) startY;

        double sideDistX, sideDistY;
        int stepX, stepY;

        if (dirX < 0) {
            stepX = -1;
            sideDistX = (startX - mapX) * deltaDistX;
        } else {
            stepX = 1;
            sideDistX = (mapX + 1.0 - startX) * deltaDistX;
        }
        if (dirY < 0) {
            stepY = -1;
            sideDistY = (startY - mapY) * deltaDistY;
        } else {
            stepY = 1;
            sideDistY = (mapY + 1.0 - startY) * deltaDistY;
        }

        boolean hit = false;
        boolean side = false;
        double perpWallDist = 0;

        while (!hit && perpWallDist < MAX_DISTANCE) {
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX;
                mapX += stepX;
                side = false;
            } else {
                sideDistY += deltaDistY;
                mapY += stepY;
                side = true;
            }

            if (map.isWall(mapX, mapY)) {
                hit = true;
                if (side) {
                    perpWallDist = (mapY - startY + (1 - stepY) / 2) / dirY;
                } else {
                    perpWallDist = (mapX - startX + (1 - stepX) / 2) / dirX;
                }
            }
        }


        if (hit) {
            double wallX;
            if (side) {
                wallX = startX + perpWallDist * dirX;
            } else {
                wallX = startY + perpWallDist * dirY;
            }
            wallX -= Math.floor(wallX);

            // Adjust wallX for edge cases
            if (wallX < EPSILON || wallX > 1 - EPSILON) {
                wallX = EPSILON;
            }

            return new RaycastHit(perpWallDist, wallX, map.getTileID(mapX, mapY), side);
        }

        return null;
    }

    private void drawWallSlice(int x, RaycastHit hit, double rayDirX, double rayDirY) {
        int lineHeight = (int) (height / hit.distance);

        int drawStart = -lineHeight / 2 + height / 2;
        if (drawStart < 0) drawStart = 0;
        int drawEnd = lineHeight / 2 + height / 2;
        if (drawEnd >= height) drawEnd = height - 1;

        BufferedImage texture = textures.getTile(hit.tileID);
        int texX = (int) (hit.wallX * texture.getWidth());
        if ((!hit.side && rayDirX > 0) || (hit.side && rayDirY < 0)) {
            texX = texture.getWidth() - texX - 1;
        }

        double step = 1.0 * texture.getHeight() / lineHeight;
        double texPos = (drawStart - height / 2 + lineHeight / 2) * step;

        for (int y = drawStart; y < drawEnd; y++) {
            int texY = (int) texPos & (texture.getHeight() - 1);
            texPos += step;
            int color = texture.getRGB(texX, texY);
            color = applyShading(color, hit.distance);
            offScreenBuffer.setRGB(x, y, color);
        }
    }

    private int applyShading(int color, double distance) {
        double shade = 1.0 - Math.min(distance / MAX_DISTANCE, 1.0);
        int r = (int) ((color >> 16 & 0xFF) * shade);
        int g = (int) ((color >> 8 & 0xFF) * shade);
        int b = (int) ((color & 0xFF) * shade);
        return (r << 16) | (g << 8) | b;
    }

    private void renderEntities() {
        for (Entity entity : entities) {
            {
                renderEntity(entity);
            }
        }
    }


    private void renderEntity(Entity entity) {
        double dx = entity.getX() - player.getX();
        double dy = entity.getY() - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Prevent rendering if the entity is at the player's position
        if (distance < EPSILON) return;

        double angle = Math.atan2(dy, dx) - player.getAngle();
        while (angle < -Math.PI) angle += 2 * Math.PI;
        while (angle > Math.PI) angle -= 2 * Math.PI;

        if (Math.abs(angle) > HALF_FOV) return;

        int screenX = (int) ((angle / HALF_FOV + 1) * width / 2);
        double size = (height / distance) * entity.getSize();
        int halfSize = (int) (size / 2);
        int screenY = height / 2;

        BufferedImage sprite = entity.getSprite();
        drawSprite(sprite, screenX, screenY, (int) size, distance);
    }
    private void drawSprite(BufferedImage sprite, int screenX, int screenY, int size, double distance) {
        int halfSize = size / 2;

        // Iterate over each pixel of the sprite
        for (int sx = -halfSize; sx < halfSize; sx++) {
            int x = screenX + sx;

            // Skip if outside screen bounds or if the entity is further than the wall
            if (x < 0 || x >= width || distance >= zBuffer[x]) continue;

            for (int sy = -halfSize; sy < halfSize; sy++) {
                int y = screenY + sy;

                if (y < 0 || y >= height) continue;

                int textureX = (sx + halfSize) * sprite.getWidth() / size;
                int textureY = (sy + halfSize) * sprite.getHeight() / size;
                int color = sprite.getRGB(textureX, textureY);

                // Check if the pixel is not transparent
                if ((color & 0xFF000000) != 0) {
                    // Apply distance shading and draw pixel
                    color = applyShading(color, distance);
                    offScreenBuffer.setRGB(x, y, color);
                }
            }
        }
    }

    private void renderWeapon() {
        // Implement weapon rendering here
    }

    private void drawFPS(long fps) {
        offScreenGraphics.setColor(Color.WHITE);
        offScreenGraphics.drawString("FPS: " + fps, 10, 20);
    }

    private void presentBuffer() {
        Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
        g.drawImage(offScreenBuffer, 0, 0, null);
        g.dispose();
        bufferStrategy.show();
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        HALF_HEIGHT = height / 2;

        // Reinitialize buffers to match new dimensions
        this.zBuffer = new double[width];; // Depth buffer should account for both width and height
        this.offScreenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.offScreenGraphics = offScreenBuffer.createGraphics();
        clearScreen();
    }

    private static class RaycastHit {
        final double distance;
        final double wallX;
        final int tileID;
        final boolean side;

        RaycastHit(double distance, double wallX, int tileID, boolean side) {
            this.distance = distance;
            this.wallX = wallX;
            this.tileID = tileID;
            this.side = side;
        }
    }
}
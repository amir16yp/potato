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
    private static final double WEAPON_BOB_SPEED = 4.0;
    private static final double WEAPON_BOB_AMOUNT = 10.0;
    private int width;
    private int height;
    private int gameHeight;
    private int hudHeight;
    private final BufferStrategy bufferStrategy;
    private Map map;
    private final Player player;
    public final CopyOnWriteArrayList<Entity> entities = new CopyOnWriteArrayList<>();
    private final Textures textures;
    public static GlyphRenderer TextRenderer = new GlyphRenderer("/potato/sprites/ascii.png");
    private double[] zBuffer;
    private BufferedImage gameBuffer;
    private BufferedImage hudBuffer;
    private Graphics2D gameGraphics;
    private Graphics2D hudGraphics;

    public double getGameHeight() {
        return gameHeight;
    }

    public void setGameHeight(double gameHeight) {
        this.gameHeight = (int) gameHeight;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = (int) width;
    }

    public enum RenderTarget {
        GAME,
        HUD
    }


    public Renderer(int width, int height, BufferStrategy bufferStrategy, Player player, Textures textures) {
        this.width = width;
        this.height = height;
        this.gameHeight = (int)(height * 0.8); // 80% of height for game view
        this.hudHeight = height - gameHeight;  // Remaining 20% for HUD
        HALF_HEIGHT = gameHeight / 2;
        this.bufferStrategy = bufferStrategy;
        this.player = player;
        this.textures = textures;

        this.zBuffer = new double[width];
        this.gameBuffer = new BufferedImage(width, gameHeight, BufferedImage.TYPE_INT_RGB);
        this.hudBuffer = new BufferedImage(width, hudHeight, BufferedImage.TYPE_INT_RGB);
        this.gameGraphics = gameBuffer.createGraphics();
        this.hudGraphics = hudBuffer.createGraphics();
    }

    public void render() {
        clearBuffers();
        if (map == null) {
            map = new Map(32, 32, 123);
            map.printMap();
        }
        drawCeilingAndFloor();
        castRays();
        renderEntities();
        renderWeapon();
        renderProjectile();
        renderHUD();
        drawFPS(Game.gameLoop.getFPS());
        for (Mod mod : Game.MOD_LOADER.getLoadedMods()) {
            mod.drawGame(gameGraphics);
            mod.drawHUD(hudGraphics);
        }
        presentBuffers();
    }

    private void renderHUD()
    {
        int count = player.getProjectiles().size();
        new GlyphText("PROJECTILE COUNT:" + String.valueOf(count), 2).setTextColor(Color.MAGENTA).draw(hudGraphics, 0, 0);
        // Add more HUD elements here as needed
    }

    private void clearBuffers() {
        gameGraphics.setColor(Color.BLACK);
        gameGraphics.fillRect(0, 0, width, gameHeight);
        hudGraphics.setColor(Color.BLACK);
        hudGraphics.fillRect(0, 0, width, hudHeight);
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
        double size = (gameHeight / distance) * projectile.getSize();

        // Calculate the vertical center of the projectile on the screen
        int screenY = HALF_HEIGHT;

        // Get the projectile's sprite
        BufferedImage sprite = projectile.getSprite();

        // Draw the projectile using the drawSprite method
        drawSprite(sprite, screenX, screenY, (int) size, distance, RenderTarget.GAME);
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
        if (y < 0 || y >= gameHeight) return; // Avoid out-of-bounds errors

        for (int x = 0; x < width; x++) {
            gameBuffer.setRGB(x, y, color.getRGB());
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
        int lineHeight = (int) (gameHeight / hit.distance);

        int drawStart = -lineHeight / 2 + gameHeight / 2;
        if (drawStart < 0) drawStart = 0;
        int drawEnd = lineHeight / 2 + gameHeight / 2;
        if (drawEnd >= gameHeight) drawEnd = gameHeight - 1;

        BufferedImage texture = textures.getTile(hit.tileID);
        int texX = (int) (hit.wallX * texture.getWidth());
        if ((!hit.side && rayDirX > 0) || (hit.side && rayDirY < 0)) {
            texX = texture.getWidth() - texX - 1;
        }

        double step = 1.0 * texture.getHeight() / lineHeight;
        double texPos = (drawStart - gameHeight / 2 + lineHeight / 2) * step;

        for (int y = drawStart; y < drawEnd; y++) {
            int texY = (int) texPos & (texture.getHeight() - 1);
            texPos += step;
            int color = texture.getRGB(texX, texY);
            color = applyShading(color, hit.distance);
            gameBuffer.setRGB(x, y, color);
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
                entity.render(this, player);
            }
        }
    }

    public void drawSprite(BufferedImage sprite, int screenX, int screenY, int size, double distance, RenderTarget target) {
        if (sprite == null || size <= 0) return;

        int halfSize = size / 2;
        BufferedImage targetBuffer = (target == RenderTarget.GAME) ? gameBuffer : hudBuffer;
        int targetHeight = (target == RenderTarget.GAME) ? gameHeight : hudHeight;

        // Pre-calculate texture step sizes
        double texStepX = (double) sprite.getWidth() / size;
        double texStepY = (double) sprite.getHeight() / size;

        int startX = Math.max(0, screenX - halfSize);
        int endX = Math.min(width - 1, screenX + halfSize);
        int startY = Math.max(0, screenY - halfSize);
        int endY = Math.min(targetHeight - 1, screenY + halfSize);

        for (int x = startX; x <= endX; x++) {
            if (target == RenderTarget.GAME && distance >= zBuffer[x]) continue;

            double texX = (x - (screenX - halfSize)) * texStepX;

            for (int y = startY; y <= endY; y++) {
                double texY = (y - (screenY - halfSize)) * texStepY;

                int textureX = (int) texX;
                int textureY = (int) texY;

                if (textureX >= 0 && textureX < sprite.getWidth() && textureY >= 0 && textureY < sprite.getHeight()) {
                    int color = sprite.getRGB(textureX, textureY);

                    if ((color & 0xFF000000) != 0) {
                        if (target == RenderTarget.GAME) {
                            color = applyShading(color, distance);
                        }
                        targetBuffer.setRGB(x, y, color);
                    }
                }
            }
        }
    }


    private void renderWeapon() {
        Weapon weapon = this.player.getWeapon();
        if (weapon == null) {
            return;
        }
        BufferedImage weaponFrame = weapon.getGunSprite().getCurrentFrame();

        double playerSpeed = player.getSpeed();
        double weaponBobOffset = Math.sin(System.currentTimeMillis() / 1000.0 * WEAPON_BOB_SPEED) * WEAPON_BOB_AMOUNT * playerSpeed;

        int weaponX = (width - weaponFrame.getWidth()) / 2;
        int weaponY = gameHeight - weaponFrame.getHeight() + (int)weaponBobOffset;

        gameGraphics.drawImage(weaponFrame, weaponX, weaponY, null);
    }

    private void drawFPS(long fps) {
        gameGraphics.setColor(Color.WHITE);
        gameGraphics.drawString("FPS: " + fps, 10, 20);
    }

    private void presentBuffers() {
        Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
        g.drawImage(gameBuffer, 0, 0, null);
        g.drawImage(hudBuffer, 0, gameHeight, null);
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
        this.gameHeight = (int)(height * 0.8);
        this.hudHeight = height - gameHeight;
        HALF_HEIGHT = gameHeight / 2;

        this.zBuffer = new double[width];
        this.gameBuffer = new BufferedImage(width, gameHeight, BufferedImage.TYPE_INT_RGB);
        this.hudBuffer = new BufferedImage(width, hudHeight, BufferedImage.TYPE_INT_RGB);
        this.gameGraphics = gameBuffer.createGraphics();
        this.hudGraphics = hudBuffer.createGraphics();
        clearBuffers();
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
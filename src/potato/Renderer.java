package potato;

import potato.modsupport.Mod;
import sun.awt.windows.WComponentPeer;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.windows.GDIWindowSurfaceData;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.peer.ComponentPeer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import static potato.Game.textures;

public class Renderer {
    private static int HALF_HEIGHT;
    public static final double FOV = Math.toRadians(60);
    public static final double HALF_FOV = FOV / 2;
    public static final double MAX_DISTANCE = 20.0;
    private static final double WALL_HEIGHT = 1.0;
    private static final double EPSILON = 1e-4;
    private static final double WEAPON_BOB_SPEED = 4.0;
    private static final double WEAPON_BOB_AMOUNT = 10.0;
    public int width;
    private int height;
    public int gameHeight;
    private int hudHeight;
    private Map map;
    private final Player player;
    public final CopyOnWriteArrayList<SpriteEntity> entities = new CopyOnWriteArrayList<>();
    public static GlyphRenderer TextRenderer = new GlyphRenderer("/potato/sprites/ascii.png");
    private double[] zBuffer;
    public static final GlyphText GUN_NAME_TEXT = new GlyphText("", 2);
    public static final GlyphText GUN_AMMO_TEXT = new GlyphText("", 2);
    private SunGraphics2D fastGraphics;
    private SurfaceData surfaceData;
    private BufferedImage buffer;
    private int[] pixels;
    private Component canvas;
    public Renderer(int width, int height, Component canvas, Player player) {
        this.width = width;
        this.height = height;
        this.gameHeight = (int) (height * 0.8);
        this.hudHeight = height - gameHeight;
        HALF_HEIGHT = gameHeight / 2;
        this.player = player;
        this.canvas = canvas;
        this.buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.pixels = ((DataBufferInt) buffer.getRaster().getDataBuffer()).getData();

        this.zBuffer = new double[width];
        new BufferedImage(width, hudHeight, BufferedImage.TYPE_INT_ARGB);
        initializeFastGraphics(canvas);
    }


    private void initializeFastGraphics(Component canvas) {
        try {
            ComponentPeer peer = canvas.getPeer();
            if (peer instanceof WComponentPeer) {
                surfaceData = GDIWindowSurfaceData.createData((WComponentPeer) peer);
                fastGraphics = new SunGraphics2D(surfaceData, Color.BLACK, Color.BLACK, null);
            } else {
                throw new RuntimeException("Unsupported peer type for fast rendering");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize fast graphics: " + e.getMessage());
        }
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
        renderHUD();
        for (Mod mod : Game.MOD_LOADER.getLoadedMods()) {
            mod.drawGame(fastGraphics);
            mod.drawHUD(fastGraphics);
        }
        presentBuffer();
    }

    private void drawProjectile(Projectile projectile) {
        double dx = projectile.getX() - player.getX();
        double dy = projectile.getY() - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        double angle = Math.atan2(dy, dx) - player.getAngle();

        while (angle < -Math.PI) angle += 2 * Math.PI;
        while (angle > Math.PI) angle -= 2 * Math.PI;

        if (Math.abs(angle) > HALF_FOV || distance > MAX_DISTANCE) return;

        int screenX = (int) ((angle / HALF_FOV + 1) * width / 2);
        double size = (gameHeight / distance) * projectile.getSize();
        int screenY = HALF_HEIGHT;

        BufferedImage sprite = projectile.getSprite();
        drawSprite(sprite, screenX, screenY, (int) size, distance, RenderTarget.GAME);
    }

    private void renderProjectile() {
        Iterator<Projectile> iterator = this.player.getProjectiles().iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            if (projectile.isActive()) {
                drawProjectile(projectile);
            } else {
                iterator.remove();
            }
        }
    }

    private void renderHUD() {
        Graphics2D g = buffer.createGraphics();

        // Draw health
        drawHealth(g);

        // Draw weapon icon and info
        drawWeaponIcon(g);

        // Draw FPS
        drawFPS(g, Game.gameLoop.getFPS());

        g.dispose();
    }

    private void drawHealth(Graphics2D g) {
        BufferedImage fullHeart = Game.hudTextures.getTile(1);
        BufferedImage halfHeart = Game.hudTextures.getTile(2);
        BufferedImage emptyHeart = Game.hudTextures.getTile(3);

        double healthPercentage = player.getHealth();
        int totalHearts = 10;
        int fullHearts = (int) (healthPercentage / 10.0);
        int halfHearts = (healthPercentage % 10.0 >= 5.0) ? 1 : 0;
        int emptyHearts = totalHearts - fullHearts - halfHearts;

        int heartWidth = fullHeart.getWidth();
        int startX = 10;
        int startY = gameHeight + 10;

        for (int i = 0; i < fullHearts; i++) {
            g.drawImage(fullHeart, startX + i * heartWidth, startY, null);
        }
        if (halfHearts == 1) {
            g.drawImage(halfHeart, startX + fullHearts * heartWidth, startY, null);
        }
        for (int i = 0; i < emptyHearts; i++) {
            g.drawImage(emptyHeart, startX + (fullHearts + halfHearts + i) * heartWidth, startY, null);
        }
    }

    private void drawWeaponIcon(Graphics2D g) {
        Weapon weapon = player.getWeapon();
        BufferedImage weaponIcon = weapon.getGunSprite().getIconSprite();
        int weaponIconX = 10;
        int weaponIconY = gameHeight + 40;
        g.drawImage(weaponIcon, weaponIconX, weaponIconY, null);

        GUN_NAME_TEXT.setText(weapon.getName());
        GUN_NAME_TEXT.draw(g, weaponIconX + 40, weaponIconY);

        GUN_AMMO_TEXT.setText(String.valueOf(weapon.getAmmo()));
        GUN_AMMO_TEXT.draw(g, weaponIconX + 40, weaponIconY + 15);
    }

    private void drawFPS(Graphics2D g, long fps) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("FPS: " + fps, 10, height - 10);
    }
    private void drawCeilingAndFloor() {
        Map map = getMap();
        for (int y = 0; y < gameHeight; y++) {
            if (y < HALF_HEIGHT) {
                if (map.getCeilingImage() != null) {
                    drawTextureRow(map.getCeilingImage(), y, 0);
                } else {
                    Arrays.fill(pixels, y * width, (y + 1) * width, Color.BLACK.getRGB());
                }
            } else {
                if (map.getFloorImage() != null) {
                    drawTextureRow(map.getFloorImage(), y, HALF_HEIGHT);
                } else {
                    Arrays.fill(pixels, y * width, (y + 1) * width, Color.DARK_GRAY.getRGB());
                }
            }
        }
    }

    private void drawTextureRow(BufferedImage texture, int y, int offset) {
        if (y < 0 || y >= gameHeight) return;

        double planeZ = 0.5 * gameHeight;
        double rowDistance = planeZ / (y - HALF_HEIGHT + 0.1);

        double floorStepX = rowDistance * (player.getPlaneX() * 2) / width;
        double floorStepY = rowDistance * (player.getPlaneY() * 2) / width;

        double floorX = player.getX() + rowDistance * (player.getDirX() - player.getPlaneX());
        double floorY = player.getY() + rowDistance * (player.getDirY() - player.getPlaneY());

        int pixelOffset = y * width;
        for (int x = 0; x < width; x++) {
            int tileX = Math.abs((int) (floorX * texture.getWidth()) % texture.getWidth());
            int tileY = Math.abs((int) (floorY * texture.getHeight()) % texture.getHeight());

            floorX += floorStepX;
            floorY += floorStepY;

            int color = texture.getRGB(tileX, tileY);
            color = applyShading(color, rowDistance);
            pixels[pixelOffset + x] = color;
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

            if (wallX < EPSILON || wallX > 1 - EPSILON) {
                wallX = EPSILON;
            }

            return new RaycastHit(perpWallDist, wallX, map.getTileID(mapX, mapY), side);
        }

        return null;
    }

    private void drawWallSlice(int x, RaycastHit hit, double rayDirX, double rayDirY) {
        int lineHeight = (int) (gameHeight / hit.distance);

        int drawStart = Math.max(0, -lineHeight / 2 + gameHeight / 2);
        int drawEnd = Math.min(gameHeight - 1, lineHeight / 2 + gameHeight / 2);

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
            pixels[y * width + x] = color;
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
        for (SpriteEntity spriteEntity : entities) {
            spriteEntity.render(this, player);
        }
    }

    public void drawSprite(BufferedImage sprite, int screenX, int screenY, int size, double distance, RenderTarget target) {
        if (sprite == null || size <= 0) return;

        int halfSize = size / 2;
        int targetHeight = (target == RenderTarget.GAME) ? gameHeight : hudHeight;

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
                        pixels[y * width + x] = color;
                    }
                }
            }
        }
    }

    private void presentBuffer() {
        fastGraphics.drawImage(buffer, 0, 0, null);
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

        Graphics2D g = buffer.createGraphics();
        g.drawImage(weaponFrame, weaponX, weaponY, null);
        g.dispose();
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

        // Recreate zBuffer with new width
        this.zBuffer = new double[width];

        // Recreate buffer and pixels array with new dimensions
        this.buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.pixels = ((DataBufferInt) buffer.getRaster().getDataBuffer()).getData();

        // Update or recreate fastGraphics and surfaceData
        try {
            if (surfaceData != null) {
                surfaceData.invalidate();
            }
            ComponentPeer peer = canvas.getPeer();
            if (peer instanceof WComponentPeer) {
                surfaceData = GDIWindowSurfaceData.createData((WComponentPeer) peer);
                fastGraphics = new SunGraphics2D(surfaceData, Color.BLACK, Color.BLACK, null);
            } else {
                throw new RuntimeException("Unsupported peer type for fast rendering");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update fast graphics: " + e.getMessage());
        }

        // Update player's plane values if they depend on screen dimensions
        double planeLength = Math.tan(HALF_FOV);
        player.setPlaneX(-planeLength * Math.sin(player.getAngle()));
        player.setPlaneY(planeLength * Math.cos(player.getAngle()));

        // Notify any listeners about the dimension change (if implemented)
        // notifyDimensionChangeListeners(width, height);
    }

    private void clearScreen() {
        Arrays.fill(pixels, 0);
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
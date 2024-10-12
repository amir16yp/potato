package potato;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.concurrent.CopyOnWriteArrayList;

public class Renderer {
    private static int HALF_HEIGHT;
    private static final double FOV = Math.PI / 3;
    private static final double HALF_FOV = FOV / 2;
    private static final double MAX_DISTANCE = 20.0;
    private double weaponBobOffset = 0;
    private static final double WEAPON_BOB_AMOUNT = 5.0; // Reduced from 10.0
    private static final double WEAPON_BOB_SPEED = 0.05; // Reduced from 0.1

    private final int width;
    private final int height;
    private final BufferStrategy bufferStrategy;
    private Map map = null;
    private final Player player;
    private final Textures textures;

    public Map getMap() {
        return map;
    }

    private boolean isGameStarted = false;
    private boolean isPaused = false;
    private StringBuilder seedInput = new StringBuilder();
    private boolean isSeedEntered = false;

    public Renderer(int width, int height, BufferStrategy bufferStrategy, Player player, Textures textures) {
        this.width = width;
        this.height = height;
        HALF_HEIGHT = height / 2;
        this.bufferStrategy = bufferStrategy;
        this.player = player;
        this.textures = textures;
    }

    public void render(long fps) {
        Graphics2D g2d = (Graphics2D) bufferStrategy.getDrawGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        if (!isGameStarted || isPaused) {
            renderMenu(g2d);
        } else {
            renderGame(g2d, fps);
        }

        g2d.dispose();
        bufferStrategy.show();
    }

    private void renderMenu(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 32));

        String mainText = isGameStarted ? "PAUSED" : "RAYCASTER GAME";
        int mainTextWidth = g2d.getFontMetrics().stringWidth(mainText);
        g2d.drawString(mainText, (width - mainTextWidth) / 2, height / 3);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));

        if (!isGameStarted && !isSeedEntered) {
            String seedPrompt = "Enter seed (numbers only):";
            int seedPromptWidth = g2d.getFontMetrics().stringWidth(seedPrompt);
            g2d.drawString(seedPrompt, (width - seedPromptWidth) / 2, height / 2 - 30);

            String seedText = seedInput.toString();
            int seedTextWidth = g2d.getFontMetrics().stringWidth(seedText);
            g2d.drawString(seedText, (width - seedTextWidth) / 2, height / 2 + 10);

            String seedInstruction = "Press ENTER to confirm seed";
            int seedInstructionWidth = g2d.getFontMetrics().stringWidth(seedInstruction);
            g2d.drawString(seedInstruction, (width - seedInstructionWidth) / 2, height / 2 + 50);
        } else {
            String instructionText = isGameStarted ? "Press P to resume" : "Press ENTER to start";
            int instructionTextWidth = g2d.getFontMetrics().stringWidth(instructionText);
            g2d.drawString(instructionText, (width - instructionTextWidth) / 2, height / 2);
        }
    }

    private void renderGame(Graphics2D g2d, long fps) {
        if (map == null)
        {
            try {
                map = new Map(100, 100, Long.parseLong(this.getSeed()));
            } catch (Exception e)
            {
                map = new Map(100, 100, this.getSeed().hashCode());
            }
        };
        drawFloor(g2d);
        castRays(g2d);
        renderWeapon(g2d);
        renderProjectiles(g2d);
        drawFPS(g2d, fps);
    }

    private void renderProjectiles(Graphics2D g2d) {
        CopyOnWriteArrayList<Projectile> projectiles = player.getProjectiles();
        for (Projectile projectile : projectiles) {
            // Calculate projectile position relative to player
            double relativeX = projectile.getX() - player.getX();
            double relativeY = projectile.getY() - player.getY();

            // Calculate angle and distance to projectile
            double angle = Math.atan2(relativeY, relativeX) - player.getAngle();
            double distance = Math.sqrt(relativeX * relativeX + relativeY * relativeY);

            // Only render if in field of view             // Adjust for fish-eye effect
            distance *= Math.cos(angle);
            int screenX = (int) ((angle + HALF_FOV) / FOV * width);
            int screenY = height / 2;

            // Get the projectile sprite
            BufferedImage projectileSprite = projectile.getSprite();

            // Calculate size based on distance (you may need to adjust this scaling factor)
            double scaleFactor = 1.0 / distance;
            int spriteWidth = (int) (projectileSprite.getWidth() * scaleFactor);
            int spriteHeight = (int) (projectileSprite.getHeight() * scaleFactor);

            // Center the sprite on its position
            int drawX = screenX - spriteWidth / 2;
            int drawY = screenY - spriteHeight / 2;

            // Draw the sprite
            g2d.drawImage(projectileSprite, drawX, drawY, spriteWidth, spriteHeight, null);
        }
    }

    private void renderWeapon(Graphics2D g2d) {
        if (player.getWeapon() == null) {
            return;
        }
        BufferedImage weaponTexture = player.getWeapon().getGunSprite().getCurrentFrame();
        int weaponWidth = weaponTexture.getWidth() * 3;
        int weaponHeight = weaponTexture.getHeight() * 3;

        // Position the weapon at the bottom center of the screen
        int x = (width - weaponWidth) / 2;
        int y = height - weaponHeight;

        // Calculate bobbing effect based on player movement
        if (player.isMoving()) {
            weaponBobOffset += WEAPON_BOB_SPEED;
            if (weaponBobOffset > Math.PI * 2) {
                weaponBobOffset -= Math.PI * 2;
            }
        } else {
            // Gradually return to center when not moving
            weaponBobOffset *= 0.8;
        }

        double bobY = Math.sin(weaponBobOffset) * WEAPON_BOB_AMOUNT;
        double bobX = Math.cos(weaponBobOffset * 0.5) * WEAPON_BOB_AMOUNT * 0.5;

        g2d.drawImage(weaponTexture,
                (int)(x + bobX),
                (int)(y + bobY),
                weaponWidth,
                weaponHeight,
                null);
    }




    private void drawFloor(Graphics2D g2d) {
        BufferedImage floorTexture = textures.getTile(5);
        int textureWidth = floorTexture.getWidth();
        int textureHeight = floorTexture.getHeight();

        for (int y = HALF_HEIGHT; y < height; y++) {
            double rayDirX0 = player.getX() - Math.tan(HALF_FOV);
            double rayDirY0 = player.getY() + 1;
            double rayDirX1 = player.getX() + Math.tan(HALF_FOV);
            double rayDirY1 = player.getY() + 1;

            int p = y - HALF_HEIGHT;
            double posZ = 0.5 * height;
            double rowDistance = posZ / p;

            if (rowDistance > MAX_DISTANCE) continue;

            double floorStepX = rowDistance * (rayDirX1 - rayDirX0) / width;
            double floorStepY = rowDistance * (rayDirY1 - rayDirY0) / width;

            double floorX = player.getX() + rowDistance * rayDirX0;
            double floorY = player.getY() + rowDistance * rayDirY0;

            for (int x = 0; x < width; ++x) {
                int tx = (int)(textureWidth * (floorX - Math.floor(floorX))) & (textureWidth - 1);
                int ty = (int)(textureHeight * (floorY - Math.floor(floorY))) & (textureHeight - 1);

                floorX += floorStepX;
                floorY += floorStepY;

                int color = floorTexture.getRGB(tx, ty);
                g2d.setColor(new Color(color));
                g2d.drawLine(x, y, x, y);
            }
        }
    }

    private void castRays(Graphics g) {
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

                if (map.isWall(mapX, mapY)) {
                    distance *= Math.cos(player.getAngle() - angle);
                    int wallHeight = Math.min((int) (height / distance), height);

                    int textureId = map.getTextureId(mapX, mapY);
                    BufferedImage tileTexture = textures.getTile(textureId);

                    double wallX;
                    if (Math.abs(y - Math.floor(y)) < Math.abs(x - Math.floor(x))) {
                        wallX = y % 1;
                    } else {
                        wallX = x % 1;
                    }
                    int textureX = (int)(wallX * textures.getTileWidth());

                    for (int texY = 0; texY < wallHeight; texY++) {
                        int textureY = texY * textures.getTileHeight() / wallHeight;
                        int color = tileTexture.getRGB(textureX, textureY);
                        g.setColor(new Color(color));
                        g.drawLine(ray, HALF_HEIGHT - wallHeight / 2 + texY, ray, HALF_HEIGHT - wallHeight / 2 + texY);
                    }
                    break;
                }
            }
        }
    }

    private void drawFPS(Graphics g, long fps) {
        g.setColor(Color.WHITE);
        g.drawString("FPS: " + fps, 10, 20);
    }

    public void startGame() {
        if (isSeedEntered) {
            isGameStarted = true;
            isPaused = false;
            long seed = Long.parseLong(seedInput.toString());
            // Example: map.generateMap(seed);
        }
    }

    public void pauseGame() {
        isPaused = true;
    }

    public void resumeGame() {
        isPaused = false;
    }

    public boolean isGameStarted() {
        return isGameStarted;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void addToSeed(char c) {
        if (!isSeedEntered && Character.isDigit(c) && seedInput.length() < 10) {
            seedInput.append(c);
        }
    }

    public void backspaceSeed() {
        if (!isSeedEntered && seedInput.length() > 0) {
            seedInput.setLength(seedInput.length() - 1);
        }
    }

    public void confirmSeed() {
        if (!isSeedEntered && seedInput.length() > 0) {
            isSeedEntered = true;
        }
    }

    public boolean isSeedEntered() {
        return isSeedEntered;
    }

    public String getSeed() {
        return seedInput.toString();
    }
}
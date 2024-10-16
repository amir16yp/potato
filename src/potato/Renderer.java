package potato;

import potato.modsupport.Mod;
import potato.server.*;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.peer.ComponentPeer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static potato.Game.*;

public class Renderer {
    public static final double FOV = Math.toRadians(60);
    public static final double HALF_FOV = FOV / 2;
    public static final double MAX_DISTANCE = 20.0;
    public static final GlyphRenderer TextRenderer = new GlyphRenderer("/potato/sprites/ascii.png");
    public static final GlyphText GUN_NAME_TEXT = new GlyphText("", 2);
    public static final GlyphText GUN_AMMO_TEXT = new GlyphText("", 2);
    public static final GlyphText FPS_TEXT = new GlyphText("", 2).setTextColor(Color.YELLOW).setBackgroundColor(Color.DARK_GRAY);
    private static final double WALL_HEIGHT = 1.0;
    private static final double EPSILON = 1e-4;
    private static final double WEAPON_BOB_SPEED = 4.0;
    private static final double WEAPON_BOB_AMOUNT = 10.0;
    private static int HALF_HEIGHT;
    public final CopyOnWriteArrayList<Projectile> projectiles = new CopyOnWriteArrayList<>();
    public final CopyOnWriteArrayList<SpriteEntity> entities = new CopyOnWriteArrayList<>();
    private final Player player;
    public int width;
    public int gameHeight;
    private int height;
    private int hudHeight;
    private BufferedImage lastRenderedFrame;

    private Map map;

    public PauseMenu pauseMenu = new PauseMenu(WIDTH, HEIGHT);

    private MiniMapRenderer miniMapRenderer;
    private static final int MINIMAP_SIZE = 80; // Size of the minimap
    private static final int MINIMAP_SCALE = 5; // Scale factor for the minimap

    private double[] zBuffer;
    private SunGraphics2D fastGraphics;
    private SurfaceData surfaceData;
    private BufferedImage buffer;
    private int[] pixels;
    private final Component canvas;

    public int clientId = -1; // Initialize with an invalid ID
    private boolean mapReceived = false;

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
        this.isMultiplayer = false;
        initializeFastGraphics(canvas);

    }

    public void initializeMultiplayer(String serverIP, int serverPort) {
        try {
            socket = new Socket(serverIP, serverPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            isMultiplayer = true;

            // Start a thread to receive packets from the server
            new Thread(this::receivePackets).start();

            System.out.println("Connected to server: " + serverIP + ":" + serverPort);

            // Wait for the server to send the client ID
            while (clientId == -1) {
                Thread.sleep(100); // Wait a bit before checking again
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            isMultiplayer = false;
        }
    }

    private void receivePackets() {
        try {
            while (isMultiplayer) {
                Packet packet = (Packet) in.readObject();
                if (packet instanceof ClientIDPacket) {
                    clientId = ((ClientIDPacket) packet).getClientId();
                    System.out.println("Received client ID: " + clientId);
                } else {
                    //System.out.println(packet.getType());
                    incomingPackets.offer(packet);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error receiving packet: " + e.getMessage());
            isMultiplayer = false;
        }
    }

    public void sendPacket(Packet packet) {
        if (!isMultiplayer) return;
        try {
            out.writeObject(packet);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error sending packet: " + e.getMessage());
            isMultiplayer = false;
        }
    }

    private void sendPlayerPosition() {
        if (clientId != -1) {
            PlayerPositionPacket posPacket = new PlayerPositionPacket(clientId, player.getX(), player.getY(), player.getAngle());
            sendPacket(posPacket);
        }
    }

    private void processServerUpdates() {
        Packet packet;
        while ((packet = incomingPackets.poll()) != null) {
            switch (packet.getType()) {
                case MAP_DATA:
                    updateMap((MapDataPacket) packet);
                    break;
                case PLAYER_POSITION:
                    PlayerPositionPacket posPacket = (PlayerPositionPacket) packet;
                    if (posPacket.getClientId() != clientId) {
                        updateOtherPlayerPosition(posPacket);
                    }
                    break;
                case SHOOT_PROJECTILE:
                    handleShootProjectilePacket((ShootProjectilePacket) packet);
                    break;
                default:
                    System.err.println("Unknown packet type: " + packet.getType());
            }
        }
    }

    private void handleShootProjectilePacket(ShootProjectilePacket packet) {
        if (packet.getClientId() != clientId) {
            Projectile projectile = new Projectile(
                    packet.getX(),
                    packet.getY(),
                    packet.getAngle(),
                    packet.getSpeed(),
                    packet.getDamage(),
                    packet.getTextureID()
            );
            projectiles.add(projectile);
        }
    }


    private void updateOtherPlayerPosition(PlayerPositionPacket posPacket) {
        int clientId = posPacket.getClientId();
        SpriteEntity playerEntity = otherPlayers.computeIfAbsent(clientId, id -> new SpriteEntity(posPacket.getX(), posPacket.getY(), textures.getTile(1), 0)); // Use appropriate sprite
        playerEntity.setX(posPacket.getX());
        playerEntity.setY(posPacket.getY());
    }

    private void renderOtherPlayers() {
        for (SpriteEntity playerEntity : otherPlayers.values()) {
            playerEntity.render(this, player);
        }
    }


    public void cleanup() {
        if (isMultiplayer) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }


    private void updateMap(MapDataPacket mapPacket) {
        int[][] mapData = mapPacket.getMapData();
        this.map = new Map(mapData);
        this.map.printMap();
        this.miniMapRenderer = new MiniMapRenderer(map, textures);
        System.out.println("Map data received and initialized.");
        this.mapReceived = true;
    }

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    public boolean isMultiplayer;
    private ConcurrentLinkedQueue<Packet> incomingPackets = new ConcurrentLinkedQueue<>();
    private ConcurrentHashMap<Integer, SpriteEntity> otherPlayers = new ConcurrentHashMap<>();



    private void initializeFastGraphics(Component canvas) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("windows")) {
                initializeWindowsGraphics(canvas);
            } else if (os.contains("linux")) {
                initializeLinuxGraphics(canvas);
            } else {
                initializeFallbackGraphics();
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize fast graphics: " + e.getMessage());
            initializeFallbackGraphics();
        }
    }

    private void initializeWindowsGraphics(Component canvas) throws Exception {
        System.out.println("Init windows graphics");
        Class<?> peerClass = Class.forName("sun.awt.windows.WComponentPeer");
        Class<?> surfaceDataClass = Class.forName("sun.java2d.windows.GDIWindowSurfaceData");
        Method createDataMethod = surfaceDataClass.getMethod("createData", peerClass);

        Object peer = canvas.getPeer();
        if (peerClass.isInstance(peer)) {
            surfaceData = (SurfaceData) createDataMethod.invoke(null, peer);
            fastGraphics = new SunGraphics2D(surfaceData, Color.BLACK, Color.BLACK, null);
        } else {
            throw new RuntimeException("Unsupported peer type for Windows fast rendering");
        }
    }

    private void initializeLinuxGraphics(Component canvas) throws Exception {
        System.out.println("Init linux graphics");
        Class<?> peerClass = Class.forName("sun.awt.X11ComponentPeer");
        Class<?> surfaceDataClass = Class.forName("sun.java2d.xr.XRSurfaceData");
        Method createDataMethod = surfaceDataClass.getMethod("createData", peerClass);

        Object peer = canvas.getPeer();
        if (peerClass.isInstance(peer)) {
            surfaceData = (SurfaceData) createDataMethod.invoke(null, peer);
            fastGraphics = new SunGraphics2D(surfaceData, Color.BLACK, Color.BLACK, null);
        } else {
            throw new RuntimeException("Unsupported peer type for Linux fast rendering");
        }
    }

    private void initializeFallbackGraphics() {
        System.out.println("Init fallback graphics, SLOW!");
        BufferedImage fallbackImage = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
        fastGraphics = (SunGraphics2D) fallbackImage.createGraphics();
        surfaceData = null;
    }

    public void render() {
        if (Game.isPaused())
        {
            Graphics2D g = buffer.createGraphics();
            pauseMenu.draw(g, lastRenderedFrame);
            g.dispose();
            presentBuffer(buffer);
            return;
        }
        if (!mapReceived && isMultiplayer) {
            renderLoadingScreen("Retrieving map data from server...");
            return;
        }
        if (!isMultiplayer)
        {
            map = new Map(32, 32, 123);
            this.miniMapRenderer = new MiniMapRenderer(map, Game.textures);
            this.mapReceived = true;
        }

        clearScreen();
        drawCeilingAndFloor();
        castRays();
        renderEntities();
        if (this.isMultiplayer)
        {
            renderOtherPlayers();
        }
        renderWeapon();
        renderProjectile();
        renderHUD();
        for (Mod mod : Game.MOD_LOADER.getLoadedMods()) {
            mod.drawGame(fastGraphics);
            mod.drawHUD(fastGraphics);
        }
        lastRenderedFrame = buffer;
        presentBuffer(buffer);
    }

    private void renderLoadingScreen(String text)
    {
        Graphics2D g = buffer.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // Draw the loading text
        GlyphText loadingText = new GlyphText(text, 2);
        int textWidth = loadingText.getWidth();
        int textHeight = loadingText.getHeight();
        int textX = (width - textWidth) / 2;
        int textY = (height - textHeight) / 2;
        loadingText.draw(g, textX, textY);

        // Draw a loading bar
        int barWidth = width / 2;
        int barHeight = 20;
        int barX = (width - barWidth) / 2;
        int barY = textY + textHeight + 20;

        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);

        // Animate the loading bar
        int progress = (int)(System.currentTimeMillis() / 20 % barWidth);
        g.setColor(Color.GREEN);
        g.fillRect(barX, barY, progress, barHeight);

        g.dispose();
        presentBuffer(buffer);
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
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            if (projectile.isActive()) {
                drawProjectile(projectile);
            } else {
                projectiles.remove(projectile);
            }
        }
    }

    private void renderHUD()
    {
        if (!mapReceived) {return;}
        Graphics2D g = buffer.createGraphics();

        // Draw health
        drawHealth(g);

        // Draw weapon icon and info
        drawWeaponIcon(g);
        drawMinimap(g);
        // Draw FPS
        drawFPS(g);
        g.dispose();
    }

    private void drawMinimap(Graphics2D g) {
        BufferedImage miniMap = miniMapRenderer.renderMiniMap(MINIMAP_SCALE, player);

        int mapWidth = map.getWidth() * MINIMAP_SCALE;
        int mapHeight = map.getHeight() * MINIMAP_SCALE;

        int miniMapX = width - MINIMAP_SIZE - 10;
        int miniMapY = gameHeight + 10;

        // Calculate the visible portion of the minimap
        int playerMiniMapX = (int) (player.getX() * MINIMAP_SCALE);
        int playerMiniMapY = (int) (player.getY() * MINIMAP_SCALE);

        int startX = Math.max(0, playerMiniMapX - MINIMAP_SIZE / 2);
        int startY = Math.max(0, playerMiniMapY - MINIMAP_SIZE / 2);
        int endX = Math.min(mapWidth, startX + MINIMAP_SIZE);
        int endY = Math.min(mapHeight, startY + MINIMAP_SIZE);

        int visibleWidth = endX - startX;
        int visibleHeight = endY - startY;

        // Draw the visible portion of the minimap
        g.drawImage(miniMap,
                miniMapX, miniMapY, miniMapX + visibleWidth, miniMapY + visibleHeight,
                startX, startY, endX, endY,
                null);

        // Draw border around the minimap
        g.setColor(Color.WHITE);
        g.drawRect(miniMapX, miniMapY, MINIMAP_SIZE, MINIMAP_SIZE);
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

    private void drawFPS(Graphics2D g) {
        FPS_TEXT.draw(g, 0, 0);
    }

    private void drawCeilingAndFloor()
    {
        if (!mapReceived) { return; }
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

    private void presentBuffer(BufferedImage bufferedImage) {
        fastGraphics.drawImage(bufferedImage, 0, 0, null);
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
        int weaponY = gameHeight - weaponFrame.getHeight() + (int) weaponBobOffset;

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
        this.gameHeight = (int) (height * 0.8);
        this.hudHeight = height - gameHeight;
        HALF_HEIGHT = gameHeight / 2;

        // Recreate zBuffer with new width
        this.zBuffer = new double[width];

        // Recreate buffer and pixels array with new dimensions
        this.buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.pixels = ((DataBufferInt) buffer.getRaster().getDataBuffer()).getData();

        // Update or recreate fastGraphics and surfaceData
        updateFastGraphics();

        // Update player's plane values if they depend on screen dimensions
        double planeLength = Math.tan(HALF_FOV);
        player.setPlaneX(-planeLength * Math.sin(player.getAngle()));
        player.setPlaneY(planeLength * Math.cos(player.getAngle()));
        pauseMenu = new PauseMenu(width, height);
        // Notify any listeners about the dimension change (if implemented)
        // notifyDimensionChangeListeners(width, height);
    }

    private void updateFastGraphics() {
        try {
            if (surfaceData != null) {
                surfaceData.invalidate();
            }

            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                updateWindowsGraphics();
            } else if (os.contains("linux")) {
                updateLinuxGraphics();
            } else {
                updateFallbackGraphics();
            }
        } catch (Exception e) {
            System.err.println("Failed to update fast graphics: " + e.getMessage());
            updateFallbackGraphics();
        }
    }

    private void updateWindowsGraphics() throws Exception {
        Class<?> peerClass = Class.forName("sun.awt.windows.WComponentPeer");
        Class<?> surfaceDataClass = Class.forName("sun.java2d.windows.GDIWindowSurfaceData");
        Method createDataMethod = surfaceDataClass.getMethod("createData", peerClass);

        Object peer = canvas.getPeer();
        if (peerClass.isInstance(peer)) {
            surfaceData = (SurfaceData) createDataMethod.invoke(null, peer);
            fastGraphics = new SunGraphics2D(surfaceData, Color.BLACK, Color.BLACK, null);
        } else {
            throw new RuntimeException("Unsupported peer type for Windows fast rendering");
        }
    }

    private void updateLinuxGraphics() throws Exception {
        Class<?> peerClass = Class.forName("sun.awt.X11ComponentPeer");
        Class<?> surfaceDataClass = Class.forName("sun.java2d.xr.XRSurfaceData");
        Method createDataMethod = surfaceDataClass.getMethod("createData", peerClass);

        Object peer = canvas.getPeer();
        if (peerClass.isInstance(peer)) {
            surfaceData = (SurfaceData) createDataMethod.invoke(null, peer);
            fastGraphics = new SunGraphics2D(surfaceData, Color.BLACK, Color.BLACK, null);
        } else {
            throw new RuntimeException("Unsupported peer type for Linux fast rendering");
        }
    }

    private void updateFallbackGraphics() {
        surfaceData = null;
        fastGraphics = (SunGraphics2D) buffer.createGraphics();
    }

    private void clearScreen() {
        Arrays.fill(pixels, 0);
    }


    public void updateMP() {
        if (isMultiplayer) {
            sendPlayerPosition();
            processServerUpdates();
        }
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
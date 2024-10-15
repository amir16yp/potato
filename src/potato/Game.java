package potato;

import potato.input.InputHandler;
import potato.input.LinuxGamepadInputHandler;
import potato.modsupport.Mod;
import potato.modsupport.ModLoader;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Game extends JFrame {
    public static final ModLoader MOD_LOADER = new ModLoader();
    public static int WIDTH = 640;
    public static int HEIGHT = 480;
    public static Renderer renderer;
    public static Player player;
    public static Textures textures;
    public static Textures hudTextures;
    public static Textures projectileTextures;
    public static InputHandler inputHandler = new InputHandler();
    public static GameLoop gameLoop;
    public Canvas canvas;
    private boolean isMultiplayer;

    public Game(String serverIP, int serverPort) {
        this();
        isMultiplayer = true;
        renderer.initializeMultiplayer(serverIP, serverPort);
    }

    public Game() {
        setTitle("Potato");
        try {
            setIconImage(ImageIO.read(this.getClass().getResourceAsStream("/potato/sprites/icon.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        player = new Player(1.5, 1.5, 0);
        textures = new Textures("/potato/sprites/textures.png", 16, 16);
        hudTextures = new Textures("/potato/sprites/hud.png", 32, 32);
        projectileTextures = new Textures("/potato/sprites/gun/boolet.png", 32, 32);
        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(canvas);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        canvas.createBufferStrategy(2);
        try {
            LinuxGamepadInputHandler linuxGamepadInputHandler = new LinuxGamepadInputHandler(0);
            inputHandler.setActiveHandler(linuxGamepadInputHandler);
            System.out.println("Gamepad input initialized successfully.");
        } catch (IOException e) {
            System.err.println("Failed to initialize gamepad input: " + e.getMessage());
            System.out.println("Falling back to default keyboard input.");
        }

        renderer = new Renderer(WIDTH, HEIGHT, canvas, player);
        addKeyListener(inputHandler);
        canvas.addKeyListener(inputHandler);
        gameLoop = new GameLoop(this);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Game game;
            if (args.length == 2) {
                String serverIP = args[0];
                int serverPort = Integer.parseInt(args[1]);
                game = new Game(serverIP, serverPort);
            } else {
                game = new Game();
            }

            MOD_LOADER.loadMods();
            for (Mod mod : MOD_LOADER.getLoadedMods()) {
                mod.preinit();
            }
            for (Mod mod : MOD_LOADER.getLoadedMods()) {
                mod.init();
            }
            game.start();
            for (Mod mod : MOD_LOADER.getLoadedMods()) {
                mod.postinit();
            }
        });
    }

    public void start() {
        gameLoop.start();
    }

    public void update() {
        player.update();
        renderer.update(); // This now includes multiplayer updates if applicable
        for (Projectile projectile : Game.renderer.projectiles) {
            projectile.update();
        }
        for (SpriteEntity spriteEntity : Game.renderer.entities) {
            spriteEntity.update();
        }
        updateHUD();
        MOD_LOADER.updateMods();
    }

    private void updateHUD() {
        if (player.getWeapon() == null) {
            Renderer.GUN_NAME_TEXT.setText("Unarmed");
            Renderer.GUN_AMMO_TEXT.setText("");
        } else {
            Renderer.GUN_NAME_TEXT.setText(player.getWeapon().getName());
            Renderer.GUN_AMMO_TEXT.setText(String.valueOf(player.getWeapon().getAmmo()));
        }
        Renderer.FPS_TEXT.setText("FPS:" + gameLoop.getFPS());
    }

    public void render() {
        renderer.render();
    }

    public void setResolution(int width, int height) {
        WIDTH = width;
        HEIGHT = height;
        setSize(WIDTH, HEIGHT);
        canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        canvas.setSize(WIDTH, HEIGHT);
        renderer.setDimensions(WIDTH, HEIGHT);
        pack();
        setLocationRelativeTo(null);
    }

    public void cleanup() {
        if (isMultiplayer) {
            renderer.cleanup();
        }
    }
}
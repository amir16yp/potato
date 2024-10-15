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
    public static int WIDTH = 640; // Now modifiable
    public static int HEIGHT = 480; // Now modifiable
    public static Renderer renderer;
    public static Player player;
    public static Textures textures;
    public static Textures hudTextures;
    public static Textures projectileTextures;
    public static InputHandler inputHandler = new InputHandler();
    public static GameLoop gameLoop;
    public Canvas canvas;

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

        player = new Player(1.5, 1.5, 0); // Starting position and angle
        textures = new Textures("/potato/sprites/textures.png", 16, 16); // Load textures
        hudTextures = new Textures("/potato/sprites/hud.png", 32, 32);
        projectileTextures = new Textures("/potato/sprites/gun/boolet.png", 32, 32);
        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(canvas);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        canvas.createBufferStrategy(2);
        //BufferStrategy bufferStrategy = canvas.getBufferStrategy();
        try {
            LinuxGamepadInputHandler linuxGamepadInputHandler = new LinuxGamepadInputHandler(0); // Assuming gamepad 0
            inputHandler.setActiveHandler(linuxGamepadInputHandler);
            System.out.println("Gamepad input initialized successfully.");
        } catch (IOException e) {
            System.err.println("Failed to initialize gamepad input: " + e.getMessage());
            System.out.println("Falling back to default keyboard input.");
            // The InputHandler will use the default keyboard input
        }

        renderer = new Renderer(WIDTH, HEIGHT, canvas, player);
        addKeyListener(inputHandler);
        canvas.addKeyListener(inputHandler);
        gameLoop = new GameLoop(this);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
        {
            MOD_LOADER.loadMods();
            for (Mod mod : MOD_LOADER.getLoadedMods()) {
                mod.preinit();
            }
            Game game = new Game();
            for (Mod mod : MOD_LOADER.getLoadedMods()) {
                mod.init();
            }
            //game.setResolution(800, 600);
            game.start();
            for (Mod mod : MOD_LOADER.getLoadedMods()) {
                mod.postinit();
            }
            //game.setResolution(800, 600);
        });
    }

    public void start() {
        gameLoop.start();
    }

    public void update() {
        player.update();
        for (Projectile projectile : Game.renderer.projectiles)
        {
            projectile.update();
        }
        for (SpriteEntity spriteEntity : Game.renderer.entities) {
            spriteEntity.update();
        }
        if (player.getWeapon() == null) {
            Renderer.GUN_NAME_TEXT.setText("Unarmed");
            Renderer.GUN_AMMO_TEXT.setText("");
        } else {
            Renderer.GUN_NAME_TEXT.setText(player.getWeapon().getName());
            Renderer.GUN_AMMO_TEXT.setText(String.valueOf(player.getWeapon().getAmmo()));
        }
        Renderer.FPS_TEXT.setText("FPS:" + gameLoop.getFPS());
        MOD_LOADER.updateMods();
    }

    public void render() {
        renderer.render();
    }

    // Method to set resolution
    public void setResolution(int width, int height) {
        WIDTH = width;
        HEIGHT = height;

        // Update JFrame and Canvas size
        setSize(WIDTH, HEIGHT);
        canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        canvas.setSize(WIDTH, HEIGHT);

        // Update Renderer dimensions
        renderer.setDimensions(WIDTH, HEIGHT);

        // Adjust the actual window size
        pack();
        setLocationRelativeTo(null); // Center the window on screen
    }
}

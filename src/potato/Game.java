package potato;

import potato.input.InputHandler;
import potato.modsupport.Mod;
import potato.modsupport.ModLoader;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.io.IOException;

public class Game extends JFrame {
    public static int WIDTH = 640; // Now modifiable
    public static int HEIGHT = 480; // Now modifiable

    public static Renderer renderer;
    public static Player player;
    public static Textures textures;
    public static InputHandler inputHandler = new InputHandler();;
    public static GameLoop gameLoop;
    public static final ModLoader MOD_LOADER = new ModLoader();
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

        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(canvas);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        canvas.createBufferStrategy(2);
        BufferStrategy bufferStrategy = canvas.getBufferStrategy();

        renderer = new Renderer(WIDTH, HEIGHT, bufferStrategy, player, textures);
        addKeyListener(inputHandler);
        canvas.addKeyListener(inputHandler);
        gameLoop = new GameLoop(this);
    }

    public void start() {
        gameLoop.start();
    }

    public void update() {
        player.update();
        for (Entity entity : Game.renderer.entities) {
            entity.update();
        }
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
        {
            MOD_LOADER.loadMods();
            for (Mod mod : MOD_LOADER.getLoadedMods()) {
                mod.preinit();
            }
            Game game = new Game();
            for (Mod mod : MOD_LOADER.getLoadedMods())
            {
                mod.init();
            }
            //game.setResolution(800, 600);
            game.start();
            for (Mod mod : MOD_LOADER.getLoadedMods())
            {
                mod.postinit();
            }
            //game.setResolution(800, 600);
        });
    }
}

package potato;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;

public class Game extends JFrame {
    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;

    public static Renderer renderer;
    public static Player player;
    public static Textures textures;
    public static InputHandler inputHandler;
    public static GameLoop gameLoop;

    public Game() {
        setTitle("Potato");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        player = new Player(1.5, 1.5, 0); // Starting position and angle
        textures = new Textures("textures.png", 16, 16); // Load textures

        Canvas canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));;
        add(canvas);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        canvas.createBufferStrategy(2);
        BufferStrategy bufferStrategy = canvas.getBufferStrategy();

        renderer = new Renderer(WIDTH, HEIGHT, bufferStrategy, player, textures);

        inputHandler = new InputHandler(renderer);
        addKeyListener(inputHandler);
        canvas.addKeyListener(inputHandler);
        gameLoop = new GameLoop(this);
    }

    public void start() {
        gameLoop.start();
    }

    public void update() {
        player.update();
        for (Entity entity : Game.renderer.entities)
        {
            entity.update();
        }
    }

    public void render() {
        renderer.render(gameLoop.getFPS());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Game game = new Game();
            game.start();
        });
    }
}
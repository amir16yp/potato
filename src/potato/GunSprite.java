package potato;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GunSprite {
    private BufferedImage spritesheet;
    private List<BufferedImage> frames;
    private int currentFrame;
    private int frameWidth;
    private int frameHeight;
    private int framesCount;
    private boolean isFiring;
    private float frameTimer;
    private float frameDuration;
    private float firingTimer;
    private float firingDuration;
    private int cycleCount;
    private int maxCycles;

    public GunSprite(String spritesheetPath, int frameWidth, int frameHeight, float frameDuration) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameDuration = frameDuration;
        this.currentFrame = 0;
        this.isFiring = false;
        this.frameTimer = 0;
        this.firingTimer = 0;
        this.firingDuration = 0;
        this.cycleCount = 0;
        this.maxCycles = 1; // Default to 1 cycle

        try {
            this.spritesheet = ImageIO.read(getClass().getResourceAsStream(spritesheetPath));
            this.framesCount = calculateFrameCount();
            this.frames = new ArrayList<>(framesCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int calculateFrameCount() {
        int columns = spritesheet.getWidth() / frameWidth;
        int rows = spritesheet.getHeight() / frameHeight;
        return columns * rows;
    }

    private void loadFrame(int index) {
        if (index < frames.size()) {
            return; // Frame already loaded
        }

        int row = index / (spritesheet.getWidth() / frameWidth);
        int col = index % (spritesheet.getWidth() / frameWidth);

        BufferedImage frame = spritesheet.getSubimage(
                col * frameWidth, row * frameHeight, frameWidth, frameHeight
        );
        frames.add(frame);
    }

    public void update(float deltaTime) {
        if (isFiring) {
            firingTimer += deltaTime;
            if (firingTimer >= firingDuration) {
                isFiring = false;
                currentFrame = 0;
                firingTimer = 0;
                cycleCount = 0;
            }
        }

        frameTimer += deltaTime;
        if (frameTimer >= frameDuration) {
            frameTimer -= frameDuration;
            if (isFiring) {
                currentFrame = (currentFrame + 1) % framesCount;
                if (currentFrame == 0) {
                    cycleCount++;
                    if (cycleCount >= maxCycles) {
                        isFiring = false;
                        cycleCount = 0;
                    }
                }
            } else {
                currentFrame = 0;
            }
        }

        // Ensure the current frame is loaded
        loadFrame(currentFrame);
    }

    public void triggerFire(float duration) {
        if (!isFiring) {
            isFiring = true;
            currentFrame = 0;
            firingTimer = 0;
            firingDuration = duration;
            cycleCount = 0;
        }
    }

    public BufferedImage getCurrentFrame() {
        loadFrame(currentFrame);
        return frames.get(currentFrame);
    }

    public int getFrameCount() {
        return framesCount;
    }

    public boolean isFiring() {
        return isFiring;
    }

    public void setMaxCycles(int maxCycles) {
        this.maxCycles = maxCycles;
    }

    public int getMaxCycles() {
        return maxCycles;
    }
}
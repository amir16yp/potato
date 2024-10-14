package potato;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GunSprite {
    protected Logger logger;
    private BufferedImage spritesheet;
    private List<BufferedImage> frames;
    private int currentFrame;
    private final int frameWidth;
    private final int frameHeight;
    private int framesCount;
    private boolean isFiring;
    private float frameTimer;
    private final float frameDuration;
    private float firingTimer;
    private float firingDuration;
    private int cycleCount;
    private int maxCycles;
    private int renderScale;
    private List<BufferedImage> scaledFrames;
    private BufferedImage iconSprite;

    public GunSprite(String spritesheetPath, String iconSpritePath, int frameWidth, int frameHeight, float frameDuration, int renderScale) {
        this.logger = new Logger(this.getClass().getName());
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
        this.renderScale = renderScale;
        if (renderScale >= 2) {
            this.scaledFrames = new ArrayList<>(framesCount);
        }
        try {
            this.spritesheet = ImageIO.read(getClass().getResourceAsStream(spritesheetPath));
            this.framesCount = calculateFrameCount();
            this.frames = new ArrayList<>(framesCount);
        } catch (IOException e) {
            logger.error(e);
        }
        try {
            this.iconSprite = ImageIO.read(this.getClass().getResourceAsStream(iconSpritePath));
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public int getRenderScale() {
        return renderScale;
    }

    public void setRenderScale(int renderScale) {
        this.renderScale = renderScale;
        if (renderScale >= 2) {
            scaledFrames.clear();
            for (BufferedImage frame : frames) {
                scaledFrames.add(createScaledImage(frame));
            }
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

        if (this.getRenderScale() >= 2) {
            BufferedImage scaledFrame = createScaledImage(frame);
            scaledFrames.add(scaledFrame);
        }

    }

    private BufferedImage createScaledImage(BufferedImage original) {
        int scaledWidth = original.getWidth() * renderScale;
        int scaledHeight = original.getHeight() * renderScale;

        // Create a new image with the scaled dimensions
        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);

        // Get the graphics context of the new image
        Graphics2D g2d = scaledImage.createGraphics();

        // Set the interpolation method to nearest neighbor for pixel art
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Disable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Disable alpha interpolation
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

        // Draw the original image onto the new image, scaling it up
        g2d.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);

        g2d.dispose();

        return scaledImage;
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
        if (this.getRenderScale() >= 2) {
            return scaledFrames.get(currentFrame);
        } else {
            return frames.get(currentFrame);

        }
    }

    public int getFrameCount() {
        return framesCount;
    }

    public boolean isFiring() {
        return isFiring;
    }

    public int getMaxCycles() {
        return maxCycles;
    }

    public void setMaxCycles(int maxCycles) {
        this.maxCycles = maxCycles;
    }

    public BufferedImage getIconSprite() {
        return iconSprite;
    }
}
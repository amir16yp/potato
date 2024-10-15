package potato;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
public class GlyphRenderer {
    private static final int GLYPH_WIDTH = 8;
    private static final int GLYPH_HEIGHT = 8;
    private static final int GLYPHS_PER_ROW = 16;
    private static final int TOTAL_GLYPHS = 256;

    private Map<Character, BufferedImage> glyphCache;

    public GlyphRenderer(String spritesheetPath) {
        try {
            BufferedImage spritesheet = ImageIO.read(getClass().getResourceAsStream(spritesheetPath));
            initializeGlyphCache(spritesheet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeGlyphCache(BufferedImage spritesheet) {
        glyphCache = new HashMap<>();
        for (int i = 0; i < TOTAL_GLYPHS; i++) {
            int row = i / GLYPHS_PER_ROW;
            int col = i % GLYPHS_PER_ROW;
            int sx = col * GLYPH_WIDTH;
            int sy = row * GLYPH_HEIGHT;
            BufferedImage glyphImage = spritesheet.getSubimage(sx, sy, GLYPH_WIDTH, GLYPH_HEIGHT);
            glyphCache.put((char) i, glyphImage);
        }
    }

    public BufferedImage getGlyphImage(char glyph) {
        return glyphCache.get(glyph);
    }

    public int getGlyphWidth() {
        return GLYPH_WIDTH;
    }

    public int getGlyphHeight() {
        return GLYPH_HEIGHT;
    }
}
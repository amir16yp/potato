package potato;

import java.awt.*;
import java.awt.image.BufferedImage;

public class GlyphText {
    private GlyphRenderer renderer;
    private String text;
    private int scale;
    private Color textColor;
    private Color backgroundColor;

    public GlyphText(String text, int scale) {
        this.text = text;
        this.scale = scale;
        this.textColor = Color.WHITE;
        this.backgroundColor = null;
        this.renderer = Renderer.TextRenderer;
    }

    public GlyphText(GlyphRenderer renderer, String text, int scale)
    {
        this.renderer = renderer;
        this.text = text;
        this.textColor = Color.WHITE;
        this.backgroundColor = null;
    }

    public void draw(Graphics2D g, int x, int y) {
        if (backgroundColor != null) {
            g.setColor(backgroundColor);
            g.fillRect(x, y, getWidth(), getHeight());
        }

        int currentX = x;
        for (char c : text.toCharArray()) {
            drawGlyph(g, c, currentX, y);
            currentX += renderer.getGlyphWidth() * scale;
        }
    }

    private void drawGlyph(Graphics2D g, char glyph, int x, int y) {
        BufferedImage glyphImage = renderer.getGlyphImage(glyph);
        if (glyphImage != null) {
            int scaledWidth = renderer.getGlyphWidth() * scale;
            int scaledHeight = renderer.getGlyphHeight() * scale;

            if (backgroundColor == null) {
                g.drawImage(glyphImage, x, y, x + scaledWidth, y + scaledHeight, 0, 0, renderer.getGlyphWidth(), renderer.getGlyphHeight(), null);
            } else {
                for (int px = 0; px < scaledWidth; px++) {
                    for (int py = 0; py < scaledHeight; py++) {
                        int srcX = px / scale;
                        int srcY = py / scale;
                        if ((glyphImage.getRGB(srcX, srcY) & 0xFF) > 0) {
                            g.setColor(textColor);
                            g.drawLine(x + px, y + py, x + px, y + py);
                        }
                    }
                }
            }
        }
    }

    public int getWidth() {
        return text.length() * renderer.getGlyphWidth() * scale;
    }

    public int getHeight() {
        return renderer.getGlyphHeight() * scale;
    }

    public GlyphText setTextColor(Color color) {
        this.textColor = color;
        return this;
    }

    public GlyphText setBackgroundColor(Color color) {
        this.backgroundColor = color;
        return this;
    }

    public GlyphText setText(String text) {
        this.text = text;
        return this;
    }

    public String getText() {
        return text;
    }

    public GlyphText setScale(int scale) {
        this.scale = scale;
        return this;
    }

    public int getScale() {
        return scale;
    }
}
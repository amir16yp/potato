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

            // Create a temporary image for the scaled glyph
            BufferedImage scaledGlyph = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gScaled = scaledGlyph.createGraphics();

            // Draw the scaled glyph
            gScaled.drawImage(glyphImage, 0, 0, scaledWidth, scaledHeight, null);
            gScaled.dispose();

            // Draw background if set
            if (backgroundColor != null) {
                g.setColor(backgroundColor);
                g.fillRect(x, y, scaledWidth, scaledHeight);
            }

            // Draw the glyph
            for (int px = 0; px < scaledWidth; px++) {
                for (int py = 0; py < scaledHeight; py++) {
                    int pixelColor = scaledGlyph.getRGB(px, py);
                    if ((pixelColor & 0xFF) > 0) {  // Check if the pixel is not fully transparent
                        g.setColor(textColor);
                        g.drawLine(x + px, y + py, x + px, y + py);
                    }
                }
            }

            // Debug: Draw a border around the glyph area
            //g.setColor(Color.RED);
            //g.drawRect(x, y, scaledWidth - 1, scaledHeight - 1);
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
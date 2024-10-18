package potato;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class PauseMenu {
    private final int width;
    private final int height;
    private final List<String> menuItems;
    private int selectedIndex;
    //private final Color overlayColor = new Color(255, 255, 255, 128);
    private final Color menuBackgroundColor = new Color(0, 0, 139, 220);
    private final Color textColor = Color.WHITE;
    private final Color selectedTextColor = Color.YELLOW;
    private final int menuPadding = 20;
    private final int menuItemSpacing = 10; // Add spacing between menu items

    public PauseMenu(int width, int height) {
        this.width = width;
        this.height = height;
        this.menuItems = new ArrayList<>();
        this.selectedIndex = 0;

        // Add default menu items
        menuItems.add("Resume");
        menuItems.add("Options");
        menuItems.add("Quit Game");
    }

    public void draw(Graphics2D g, BufferedImage lastRenderedFrame) {
        // Draw the last rendered frame
        g.drawImage(lastRenderedFrame, 0, 0, null);

        // Calculate menu dimensions
        int menuWidth = 250; // Increased width to accommodate longer text
        int menuItemHeight = Renderer.TextRenderer.getGlyphHeight();
        int totalMenuHeight = menuItems.size() * (menuItemHeight + menuItemSpacing) + 2 * menuPadding;
        int menuX = (width - menuWidth) / 2;
        int menuY = (height - totalMenuHeight) / 2;

        // Draw menu background
        g.setColor(menuBackgroundColor);
        g.fillRect(menuX, menuY, menuWidth, totalMenuHeight);
        g.setColor(Color.WHITE);
        g.drawRect(menuX, menuY, menuWidth, totalMenuHeight);

        // Draw menu items
        int currentY = menuY + menuPadding;
        for (int i = 0; i < menuItems.size(); i++) {
            String item = menuItems.get(i);
            GlyphText itemText = new GlyphText(item, 2);
            int textWidth = itemText.getWidth();
            int textX = menuX + (menuWidth - textWidth) / 2;

            if (i == selectedIndex) {
                g.setColor(selectedTextColor);
                g.fillRect(menuX + 5, currentY - 5, menuWidth - 10, menuItemHeight + 10);
            }

            itemText.setTextColor(textColor);
            itemText.draw(g, textX, currentY);
            currentY += menuItemHeight + menuItemSpacing;
        }

        // Draw "PAUSE" text at the top
        GlyphText pauseText = new GlyphText("PAUSE", 3);
        pauseText.setTextColor(Color.WHITE);
        pauseText.draw(g, (width - pauseText.getWidth()) / 2, menuY - 30);
    }

    public void moveSelectionUp() {
        selectedIndex = (selectedIndex - 1 + menuItems.size()) % menuItems.size();
    }

    public void moveSelectionDown() {
        selectedIndex = (selectedIndex + 1) % menuItems.size();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getSelectedItem() {
        return menuItems.get(selectedIndex);
    }
}
package potato.modsupport;

import java.awt.*;

public interface Mod {
    void preinit();
    void init();
    void postinit();
    void update();
    void drawHUD(Graphics2D g);
    void drawGame(Graphics2D g);
}

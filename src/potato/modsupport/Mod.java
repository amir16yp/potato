package potato.modsupport;

import java.awt.*;

public interface Mod {
    void preinit();
    void init();
    void postinit();
    void update();
    void draw(Graphics2D g);
}

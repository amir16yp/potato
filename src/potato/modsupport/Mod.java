package potato.modsupport;

import java.awt.*;

public interface Mod {
    void init();
    void update();
    void draw(Graphics2D g);
}

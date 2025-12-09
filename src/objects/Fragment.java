package objects;

import static utilz.Constants.ObjectConstants.*;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import main.Game;
import utilz.LoadSave;

public class Fragment extends GameObject {

    private BufferedImage img;

    public Fragment(int x, int y) {
        super(x, y, FRAGMENT);
        initHitbox(12, 12);
        img = LoadSave.GetSpriteAtlas(LoadSave.FRAGMENT);
        doAnimation = false;
        xDrawOffset = (int) (2 * Game.SCALE);
        yDrawOffset = (int) (2 * Game.SCALE);
    }

    public void update() {
        updateAnimationTick();
        // simple bobbing could be added later
    }

    public void draw(Graphics g, int xLvlOffset) {
        if (!active) return;
        int drawX = (int) (x - xDrawOffset - xLvlOffset);
        int drawY = (int) (y - yDrawOffset);
        if (img != null)
            g.drawImage(img, drawX, drawY, (int) (12 * Game.SCALE), (int) (12 * Game.SCALE), null);
        else {
            g.setColor(java.awt.Color.MAGENTA);
            g.fillRect(drawX, drawY, (int) (12 * Game.SCALE), (int) (12 * Game.SCALE));
        }
    }
}

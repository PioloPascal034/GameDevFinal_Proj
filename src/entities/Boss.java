package entities;

import gamestates.Playing;
import utilz.LoadSave;
import main.Game;

import static utilz.Constants.EnemyConstants.*;
// no extra static imports required

import java.awt.image.BufferedImage;

public class Boss extends Enemy {

    private BufferedImage[] frames;
    private int frameCount = 1;

    public Boss(float x, float y, int bossType, String folderPath) {
        super(x, y, 128, 128, bossType); // default boss size, can be adjusted
        initHitbox(60, 80);
        frames = LoadSave.GetSpritesFromFolder(folderPath);
        if (frames == null) frames = new BufferedImage[0];
        frameCount = frames.length > 0 ? frames.length : 1;
        // bosses are tougher
        maxHealth = GetMaxHealth(bossType);
        currentHealth = maxHealth;
        walkSpeed = Game.SCALE * 0.2f;
    }

    public void update(int[][] lvlData, Playing playing) {
        // simple boss behavior: if in air handle physics else idle
        if (firstUpdate)
            firstUpdateCheck(lvlData);

        if (inAir) {
            inAirChecks(lvlData, playing);
        } else {
            // stationary by default; can be extended later
        }

        updateAnimationTick();
    }

    @Override
    protected void updateAnimationTick() {
        aniTick++;
        if (aniTick >= 25) {
            aniTick = 0;
            aniIndex++;
            if (aniIndex >= frameCount)
                aniIndex = 0;
        }
    }

    public BufferedImage getCurrentFrame() {
        if (frames == null || frames.length == 0) return null;
        return frames[aniIndex];
    }

}

package objects;

import entities.Enemy;
import entities.Player;
import gamestates.Playing;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import levels.Level;
import main.Game;
import static utilz.Constants.EnemyConstants.*;
import static utilz.Constants.ObjectConstants.*;
import static utilz.Constants.Projectiles.*;
import utilz.HelpMethods;
import utilz.LoadSave;

public class ObjectManager {

    private Playing playing;
    private Level currentLevel;

    private BufferedImage[][] potionImgs, containerImgs;
    private BufferedImage[] cannonImgs, grassImgs;
    private BufferedImage[][] treeImgs;
    private BufferedImage spikeImg, cannonBallImg, bulletImg;
    private BufferedImage ammoImg;

    private ArrayList<Potion> potions;
    private ArrayList<Ammo> ammos;
    private ArrayList<GameContainer> containers;
    private ArrayList<Fragment> fragments;
    private ArrayList<Projectile> projectiles = new ArrayList<>();

    public ObjectManager(Playing playing) {
        this.playing = playing;
        this.currentLevel = playing.getLevelManager().getCurrentLevel();
        // initialize collections to avoid NPE if update/draw run
        potions = new ArrayList<>();
        ammos = new ArrayList<>();
        containers = new ArrayList<>();
        fragments = new ArrayList<>();
        projectiles = new ArrayList<>();

        loadImgs();
    }

    /* -------------------------------------------------------------------------
       Collision / Interaction 
       ------------------------------------------------------------------------- */

    public void checkSpikesTouched(Player p) {
        for (Spike s : currentLevel.getSpikes()) {
            if (s.getHitbox().intersects(p.getHitbox())) {
                if (!p.isInvulnerable())
                    p.kill();
            }
        }
    }

    public void checkSpikesTouched(Enemy e) {
        for (Spike s : currentLevel.getSpikes()) {
            if (s.getHitbox().intersects(e.getHitbox())) {
                e.hurt(200);
            }
        }
    }

    public void checkObjectTouched(Rectangle2D.Float hitbox) {
        for (Potion p : potions) {
            if (p.isActive() && hitbox.intersects(p.getHitbox())) {
                p.setActive(false);
                applyEffectToPlayer(p);
            }
        }
        for (Ammo a : ammos) {
            if (a.isActive() && hitbox.intersects(a.getHitbox())) {
                a.setActive(false);
                applyEffectToPlayer(a);
            }
        }
    }

    private void applyEffectToPlayer(Potion p) {
        Player player = playing.getPlayer();
        if (p.getObjType() == RED_POTION)
            player.changeHealth(RED_POTION_VALUE);
        else
            player.changeHealth(BLUE_POTION_VALUE);
        try { playing.showStatusMessage("Picked up potion", 900); } catch (Exception ignored) {}
    }

    private void applyEffectToPlayer(Ammo a) {
        int add = (a.getObjType() == RED_AMMO) ? RED_AMMO_VALUE : BLUE_AMMO_VALUE;
        playing.getPlayer().changeAmmo(add);
        try { playing.showStatusMessage("+" + add + " ammo", 900); } catch (Exception ignored) {}
    }

    public void checkObjectHit(Rectangle2D.Float attackbox) {
        for (GameContainer gc : containers) {
            if (gc.isActive() && !gc.doAnimation && gc.getHitbox().intersects(attackbox)) {

                gc.setAnimation(true);
                int type = gc.getObjType() == BARREL ? 1 : 0;

                // 50% potion, 50% ammo
                if (Math.random() < 0.5)
                    potions.add(new Potion(centerX(gc), centerY(gc), type));
                else
                    ammos.add(new Ammo(centerX(gc), centerY(gc), type));

                return;
            }
        }
    }

    /* -------------------------------------------------------------------------
       Object Loading
       ------------------------------------------------------------------------- */

    public void loadObjects(Level newLevel) {
        this.currentLevel = newLevel;

        potions = new ArrayList<>(newLevel.getPotions());
        ammos = new ArrayList<>(newLevel.getAmmos());
        containers = new ArrayList<>(newLevel.getContainers());
        fragments = new ArrayList<>();

        projectiles.clear();
    }

    /* -------------------------------------------------------------------------
       Image Loading
       ------------------------------------------------------------------------- */

    private void loadImgs() {
        loadPotionImgs();
        loadContainerImgs();
        loadTreeImgs();
        loadGrassImgs();

        spikeImg = LoadSave.GetSpriteAtlas(LoadSave.TRAP_ATLAS);
        cannonBallImg = LoadSave.GetSpriteAtlas(LoadSave.CANNON_BALL);
        bulletImg = LoadSave.GetSpriteAtlas(LoadSave.BULLET);
        // optional debug ammo sprite (fire orb) - drop your image as /res/fire_orb.png
        ammoImg = LoadSave.GetSpriteAtlas("fire_orb.png");

        loadCannonImgs();
    }

    private void loadPotionImgs() {
        BufferedImage atlas = LoadSave.GetSpriteAtlas(LoadSave.POTION_ATLAS);
        potionImgs = new BufferedImage[2][7];

        for (int row = 0; row < potionImgs.length; row++)
            for (int col = 0; col < potionImgs[row].length; col++)
                potionImgs[row][col] = atlas.getSubimage(col * 12, row * 16, 12, 16);
    }

    private void loadContainerImgs() {
        BufferedImage atlas = LoadSave.GetSpriteAtlas(LoadSave.CONTAINER_ATLAS);
        containerImgs = new BufferedImage[2][8];

        for (int row = 0; row < containerImgs.length; row++)
            for (int col = 0; col < containerImgs[row].length; col++)
                containerImgs[row][col] = atlas.getSubimage(col * 40, row * 30, 40, 30);
    }

    private void loadCannonImgs() {
        BufferedImage atlas = LoadSave.GetSpriteAtlas(LoadSave.CANNON_ATLAS);
        cannonImgs = new BufferedImage[7];

        for (int i = 0; i < cannonImgs.length; i++)
            cannonImgs[i] = atlas.getSubimage(i * 40, 0, 40, 26);
    }

    private void loadTreeImgs() {
        treeImgs = new BufferedImage[2][4];

        BufferedImage tree1 = LoadSave.GetSpriteAtlas(LoadSave.TREE_ONE_ATLAS);
        BufferedImage tree2 = LoadSave.GetSpriteAtlas(LoadSave.TREE_TWO_ATLAS);

        for (int i = 0; i < 4; i++) {
            treeImgs[0][i] = tree1.getSubimage(i * 39, 0, 39, 92);
            treeImgs[1][i] = tree2.getSubimage(i * 62, 0, 62, 54);
        }
    }

    private void loadGrassImgs() {
        BufferedImage atlas = LoadSave.GetSpriteAtlas(LoadSave.GRASS_ATLAS);
        grassImgs = new BufferedImage[2];
        for (int i = 0; i < grassImgs.length; i++)
            grassImgs[i] = atlas.getSubimage(i * 32, 0, 32, 32);
    }

    /* -------------------------------------------------------------------------
       Update Loop
       ------------------------------------------------------------------------- */

    public void update(int[][] lvlData, Player player) {
        updateBackgroundTrees();

        updateList(potions);
        updateList(ammos);
        updateList(fragments);
        updateList(containers);

        updateCannons(lvlData, player);
        updateProjectiles(lvlData, player);
        checkFragmentCollection(player);
    }

    private <T extends GameObject> void updateList(ArrayList<T> list) {
        for (T obj : list)
            if (obj.isActive())
                obj.update();
    }

    private void checkFragmentCollection(Player player) {
        for (Fragment f : fragments) {
            if (f.isActive() && f.getHitbox().intersects(player.getHitbox())) {

                f.setActive(false);
                player.addFragment();

                playing.addDialogue(
                        (int) player.getHitbox().x,
                        (int) player.getHitbox().y - 20,
                        utilz.Constants.Dialogue.EXCLAMATION);

                handleFragmentCutscene(player.getFragmentCount());
            }
        }
    }

    private void handleFragmentCutscene(int frag) {
        try {
            var cut = playing.getGame().getCutsceneManager();
            if (cut == null) return;

            String message = loadFragmentMessage(frag);
            grantFragmentAbility(frag, message);
        } catch (Exception ignored) { }
    }

    private String loadFragmentMessage(int frag) {
        String base = "/res/va's/";
        String[] names = {
                "frag" + frag + ".txt", "frag" + frag + ".TXT",
                "fragment_" + frag + ".txt", "fragment" + frag + ".txt",
                "Fragment_" + frag + ".txt", "Fragment" + frag + ".TXT",
                "fragment.txt", "fragment.TXT"
        };

        for (String file : names) {
            try {
                var stream = LoadSave.class.getResourceAsStream(base + file);
                if (stream != null) {
                    return new java.io.BufferedReader(
                            new java.io.InputStreamReader(stream))
                            .lines()
                            .reduce("", (a, b) -> a + " " + b).trim();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void grantFragmentAbility(int frag, String msg) {

        Player p = playing.getPlayer();
        String fallback = msg;

        switch (frag) {
            case 1 -> fallback = fallback != null ? fallback : "Dash unlocked (right-click)";
            case 2 -> fallback = fallback != null ? fallback : "Double-jump unlocked";
            case 3 -> fallback = fallback != null ? fallback : "Untargetable unlocked (press R)";
            case 4 -> fallback = fallback != null ? fallback : "Gun unlocked (press F)";
            case 5 -> fallback = fallback != null ? fallback : "Block unlocked (press Q)";
            case 6 -> fallback = fallback != null ? fallback : "Immortality unlocked (press G)";
        }

        switch (frag) {
            case 1 -> p.grantDash();
            case 2 -> p.grantDoubleJump();
            case 3 -> p.grantUntargetable();
            case 4 -> p.grantGun();
            case 5 -> p.grantBlock();
            case 6 -> p.grantImmortality();
        }

        if (fallback != null)
            playing.getGame().getCutsceneManager().startFragmentCutscene(frag, fallback);
        else
            playing.getGame().getCutsceneManager().startFragmentCutscene(frag);
    }

    /* -------------------------------------------------------------------------
       Cannons & Projectiles
       ------------------------------------------------------------------------- */

    private void updateCannons(int[][] lvlData, Player player) {
        for (Cannon c : currentLevel.getCannons()) {

            if (!c.doAnimation &&
                c.getTileY() == player.getTileY() &&
                isPlayerInRange(c, player) &&
                isPlayerInfrontOfCannon(c, player) &&
                HelpMethods.CanCannonSeePlayer(lvlData, player.getHitbox(), c.getHitbox(), c.getTileY())) {
                c.setAnimation(true);
            }

            c.update();

            if (c.getAniIndex() == 4 && c.getAniTick() == 0)
                shootCannon(c);
        }
    }

    private boolean isPlayerInRange(Cannon c, Player player) {
        return Math.abs(player.getHitbox().x - c.getHitbox().x)
                <= Game.TILES_SIZE * 5;
    }

    private boolean isPlayerInfrontOfCannon(Cannon c, Player player) {
        return c.getObjType() == CANNON_LEFT
                ? c.getHitbox().x > player.getHitbox().x
                : c.getHitbox().x < player.getHitbox().x;
    }

    private void shootCannon(Cannon c) {
        int dir = (c.getObjType() == CANNON_LEFT) ? -1 : 1;
        projectiles.add(new Projectile(
                (int) c.getHitbox().x,
                (int) c.getHitbox().y,
                dir));
    }

    private void updateProjectiles(int[][] lvlData, Player player) {
        for (Projectile p : projectiles) {
            if (!p.isActive()) continue;

            p.updatePos();

            if (handleProjectileHits(p, lvlData, player))
                p.setActive(false);
        }
    }

    private boolean handleProjectileHits(Projectile p, int[][] lvlData, Player player) {

        // enemy → player
        if (p.getType() == 0 &&
            p.getHitbox().intersects(player.getHitbox())) {
            player.changeHealth(-25);
            return true;
        }

        // player bullets
        if (p.getType() == 1) {
            if (hitEnemy(currentLevel.getCrabs(), p)) return true;
            if (hitEnemy(currentLevel.getPinkstars(), p)) return true;
            if (hitEnemy(currentLevel.getSharks(), p)) return true;
        }

        return HelpMethods.IsProjectileHittingLevel(p, lvlData);
    }

    private <T extends Enemy> boolean hitEnemy(ArrayList<T> list, Projectile p) {
        for (T e : list) {
            if (e.isActive() && e.getState() != DEAD && e.getState() != HIT &&
                p.getHitbox().intersects(e.getHitbox())) {
                System.out.println("DBG: Projectile hit enemy at x=" + p.getHitbox().x + " type=" + p.getType());
                // pass attacker x to compute correct pushback direction
                e.hurt(BULLET_DAMAGE, (float) p.getHitbox().x);
                return true;
            }
        }
        return false;
    }

    /* -------------------------------------------------------------------------
       Rendering
       ------------------------------------------------------------------------- */

    public void draw(Graphics g, int xLvlOffset) {
        drawPotions(g, xLvlOffset);
        drawContainers(g, xLvlOffset);
        drawTraps(g, xLvlOffset);
        drawCannons(g, xLvlOffset);
        drawProjectiles(g, xLvlOffset);
        drawAmmos(g, xLvlOffset);
        drawFragments(g, xLvlOffset);
        drawGrass(g, xLvlOffset);
    }

    public void drawBackgroundTrees(Graphics g, int xLvlOffset) {
        for (BackgroundTree bt : currentLevel.getTrees()) {

            int type = bt.getType();
            if (type == 9) type = 8;

            g.drawImage(
                    treeImgs[type - 7][bt.getAniIndex()],
                    bt.getX() - xLvlOffset + GetTreeOffsetX(bt.getType()),
                    (int) (bt.getY() + GetTreeOffsetY(bt.getType())),
                    GetTreeWidth(bt.getType()),
                    GetTreeHeight(bt.getType()),
                    null);
        }
    }

    private void updateBackgroundTrees() {
        for (BackgroundTree bt : currentLevel.getTrees())
            bt.update();
    }

    private void drawProjectiles(Graphics g, int xLvlOffset) {
        for (Projectile p : projectiles) {
            if (!p.isActive()) continue;

            int dx = (int) (p.getHitbox().x - xLvlOffset);
            int dy = (int) p.getHitbox().y;

            BufferedImage img = (p.getType() == 1) ? bulletImg : cannonBallImg;

            int w = CANNON_BALL_WIDTH;
            int h = CANNON_BALL_HEIGHT;
            // flip horizontally when traveling left
            if (p.getDir() >= 0)
                g.drawImage(img, dx, dy, w, h, null);
            else
                g.drawImage(img, dx + w, dy, -w, h, null);
        }
    }

    private void drawFragments(Graphics g, int xLvlOffset) {
        if (fragments == null) return;

        for (Fragment f : fragments)
            if (f.isActive())
                f.draw(g, xLvlOffset);
    }

    private void drawCannons(Graphics g, int xLvlOffset) {
        for (Cannon c : currentLevel.getCannons()) {
            int x = (int) (c.getHitbox().x - xLvlOffset);
            int w = CANNON_WIDTH;

            if (c.getObjType() == CANNON_RIGHT) {
                x += w;
                w *= -1;
            }
            g.drawImage(
                    cannonImgs[c.getAniIndex()],
                    x, (int) c.getHitbox().y,
                    w, CANNON_HEIGHT, null);
        }
    }

    private void drawTraps(Graphics g, int xLvlOffset) {
        for (Spike s : currentLevel.getSpikes())
            g.drawImage(
                    spikeImg,
                    (int) (s.getHitbox().x - xLvlOffset),
                    (int) (s.getHitbox().y - s.getyDrawOffset()),
                    SPIKE_WIDTH, SPIKE_HEIGHT,
                    null);
    }

    private void drawContainers(Graphics g, int xLvlOffset) {
        for (GameContainer gc : containers) {
            if (!gc.isActive()) continue;

            int type = (gc.getObjType() == BARREL) ? 1 : 0;

            g.drawImage(
                    containerImgs[type][gc.getAniIndex()],
                    (int) (gc.getHitbox().x - gc.getxDrawOffset() - xLvlOffset),
                    (int) (gc.getHitbox().y - gc.getyDrawOffset()),
                    CONTAINER_WIDTH, CONTAINER_HEIGHT, null);
        }
    }

    private void drawPotions(Graphics g, int xLvlOffset) {
        for (Potion p : potions) {
            if (!p.isActive()) continue;

            int type = (p.getObjType() == RED_POTION) ? 1 : 0;

            g.drawImage(
                    potionImgs[type][p.getAniIndex()],
                    (int) (p.getHitbox().x - p.getxDrawOffset() - xLvlOffset),
                    (int) (p.getHitbox().y - p.getyDrawOffset()),
                    POTION_WIDTH, POTION_HEIGHT, null);
        }
    }

    private void drawAmmos(Graphics g, int xLvlOffset) {
        for (Ammo a : ammos) {
            if (!a.isActive()) continue;

            int drawX = (int) (a.getHitbox().x - a.getxDrawOffset() - xLvlOffset);
            int drawY = (int) (a.getHitbox().y - a.getyDrawOffset());
            if (ammoImg != null) {
                g.drawImage(ammoImg, drawX, drawY, AMMO_WIDTH, AMMO_HEIGHT, null);
            } else {
                int type = (a.getObjType() == RED_AMMO) ? 1 : 0;
                g.drawImage(potionImgs[type][a.getAniIndex()], drawX, drawY, AMMO_WIDTH, AMMO_HEIGHT, null);
            }
        }
    }

    private void drawGrass(Graphics g, int xLvlOffset) {
        for (Grass grass : currentLevel.getGrass())
            g.drawImage(
                    grassImgs[grass.getType()],
                    grass.getX() - xLvlOffset,
                    grass.getY(),
                    (int) (32 * Game.SCALE),
                    (int) (32 * Game.SCALE),
                    null);
    }

    /* -------------------------------------------------------------------------
       Utilities
       ------------------------------------------------------------------------- */

    private int centerX(GameContainer gc) {
        return (int) (gc.getHitbox().x + gc.getHitbox().width / 2);
    }

    private int centerY(GameContainer gc) {
        return (int) (gc.getHitbox().y - gc.getHitbox().height / 2);
    }

    /* -------------------------------------------------------------------------
       External API
       ------------------------------------------------------------------------- */

    public void resetAllObjects() {
        loadObjects(playing.getLevelManager().getCurrentLevel());

        for (Potion p : potions) p.reset();
        for (Ammo a : ammos) a.reset();
        for (GameContainer gc : containers) gc.reset();
        for (Cannon c : currentLevel.getCannons()) c.reset();
    }

    public void addPlayerProjectile(float x, float y, int dir) {
        projectiles.add(new Projectile((int) x, (int) y, dir, 1));
    }

    public void addEnemyProjectile(float x, float y, int dir) {
        projectiles.add(new Projectile((int) x, (int) y, dir));
    }

    public void spawnFragment(int x, int y) {
        if (fragments == null) fragments = new ArrayList<>();
        // Ensure fragment doesn't spawn inside water: if tile is water, move up until solid or max attempts
        int spawnX = x;
        int spawnY = y;
        int attempts = 0;
        int maxAttempts = 6; // try moving up up to 6 tiles
        int fragmentSize = (int) (12 * Game.SCALE);
        java.awt.geom.Rectangle2D.Float testBox = new java.awt.geom.Rectangle2D.Float(spawnX, spawnY, fragmentSize, fragmentSize);
        while (attempts < maxAttempts && utilz.HelpMethods.IsEntityInWater(testBox, currentLevel.getLevelData())) {
            spawnY -= Game.TILES_SIZE; // move up one tile
            testBox.y = spawnY;
            attempts++;
        }
        fragments.add(new Fragment(spawnX, spawnY));
    }

    public boolean hasActiveFragments() {
        if (fragments == null) return false;

        for (Fragment f : fragments)
            if (f.isActive())
                return true;

        return false;
    }
}

package gamestates;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.ArrayList;

import entities.EnemyManager;
import entities.Player;
import entities.PlayerCharacter;
import levels.LevelManager;
import main.Game;
import objects.ObjectManager;
import ui.GameCompletedOverlay;
import ui.GameOverOverlay;
import ui.LevelCompletedOverlay;
import ui.PauseOverlay;
import utilz.LoadSave;
import effects.DialogueEffect;
import effects.Rain;

import static utilz.Constants.Environment.*;
import static utilz.Constants.Dialogue.*;

public class Playing extends State implements Statemethods {

    private Player player;
    private LevelManager levelManager;
    private EnemyManager enemyManager;
    private ObjectManager objectManager;
    private PauseOverlay pauseOverlay;
    private GameOverOverlay gameOverOverlay;
    private GameCompletedOverlay gameCompletedOverlay;
    private LevelCompletedOverlay levelCompletedOverlay;
    private Rain rain;

    private boolean paused = false;

    private int xLvlOffset;
    private int leftBorder = (int) (0.25 * Game.GAME_WIDTH);
    private int rightBorder = (int) (0.75 * Game.GAME_WIDTH);
    private int maxLvlOffsetX;

    private BufferedImage backgroundImg, bigCloud, smallCloud;
    private BufferedImage[] questionImgs, exclamationImgs;
    private ArrayList<DialogueEffect> dialogEffects = new ArrayList<>();

    private int[] smallCloudsPos;
    private Random rnd = new Random();

    private boolean gameOver;
    private boolean lvlCompleted;
    private boolean gameCompleted;
    private boolean playerDying;
    private boolean drawRain;

    // New flags for fragment/dialogue flow
    private boolean fragmentCollected = false;         // set when fragment picked up
    private boolean fragmentDialogueDone = false;      // set when dialogue/VO finishes or skipped
    private boolean fragmentVOPlaying = false;         // true while voice audio is playing
    private boolean startFadeToLevelComplete = false;  // starts fade when dialogue finishes
    private float fadeAlpha = 0f;                      // 0..1
    private float fadeSpeed = 0.02f;                   // tuning: fade per tick

    private boolean showingFragmentDialogue = false;

    private boolean drawShip = false;
    private boolean debugOffsetMode = false;
    // guard to ensure one-shot fire per press
    private boolean fPressed = false;
    // temporary on-screen status message (e.g., "Immortality ON") in ms
    private String statusMessage = null;
    private int statusMsgTimer = 0;

    public Playing(Game game) {
        super(game);
        initClasses();

        backgroundImg = LoadSave.GetSpriteAtlas(LoadSave.PLAYING_BG_IMG);
        bigCloud = LoadSave.GetSpriteAtlas(LoadSave.BIG_CLOUDS);
        smallCloud = LoadSave.GetSpriteAtlas(LoadSave.SMALL_CLOUDS);
        smallCloudsPos = new int[8];
        for (int i = 0; i < smallCloudsPos.length; i++)
            smallCloudsPos[i] = (int) (90 * Game.SCALE) + rnd.nextInt((int) (100 * Game.SCALE));

        loadDialogue();
        calcLvlOffset();
        loadStartLevel();
        setDrawRainBoolean();
    }

    private void loadDialogue() {
        loadDialogueImgs();

        for (int i = 0; i < 10; i++)
            dialogEffects.add(new DialogueEffect(0, 0, EXCLAMATION));
        for (int i = 0; i < 10; i++)
            dialogEffects.add(new DialogueEffect(0, 0, QUESTION));

        for (DialogueEffect de : dialogEffects)
            de.deactive();
    }

    private void loadDialogueImgs() {
        BufferedImage qtemp = LoadSave.GetSpriteAtlas(LoadSave.QUESTION_ATLAS);
        questionImgs = new BufferedImage[5];
        for (int i = 0; i < questionImgs.length; i++)
            questionImgs[i] = qtemp.getSubimage(i * 14, 0, 14, 12);

        BufferedImage etemp = LoadSave.GetSpriteAtlas(LoadSave.EXCLAMATION_ATLAS);
        exclamationImgs = new BufferedImage[5];
        for (int i = 0; i < exclamationImgs.length; i++)
            exclamationImgs[i] = etemp.getSubimage(i * 14, 0, 14, 12);
    }

    public void loadNextLevel() {
        levelManager.setLevelIndex(levelManager.getLevelIndex() + 1);
        levelManager.loadNextLevel();
        player.setSpawn(levelManager.getCurrentLevel().getPlayerSpawn());
        // Add +5 ammo when advancing to next level (like potion rewards)
        try {
            player.changeAmmo(5);
        } catch (Exception ignored) {}
        resetAll();
        drawShip = false;
    }

    private void loadStartLevel() {
        enemyManager.loadEnemies(levelManager.getCurrentLevel());
        objectManager.loadObjects(levelManager.getCurrentLevel());
    }

    private void calcLvlOffset() {
        maxLvlOffsetX = levelManager.getCurrentLevel().getLvlOffset();
    }

    private void initClasses() {
        levelManager = new LevelManager(game);
        // restore saved level index if present
        try {
            if (game.getSaveManager() != null && game.getSaveManager().getSaveData() != null) {
                int savedIdx = game.getSaveManager().getSaveData().levelIndex;
                if (savedIdx >= 0 && savedIdx < levelManager.getAmountOfLevels())
                    levelManager.setLevelIndex(savedIdx);
            }
        } catch (Exception ignored) {}
        enemyManager = new EnemyManager(this);
        objectManager = new ObjectManager(this);

        pauseOverlay = new PauseOverlay(this);
        gameOverOverlay = new GameOverOverlay(this);
        levelCompletedOverlay = new LevelCompletedOverlay(this);
        gameCompletedOverlay = new GameCompletedOverlay(this);

        rain = new Rain();

        // Always use PIRATE character
        player = new Player(PlayerCharacter.PIRATE, this);
        // restore player saved flags
        try {
            if (game.getSaveManager() != null && game.getSaveManager().getSaveData() != null) {
                utilz.SaveManager.SaveData sd = game.getSaveManager().getSaveData();
                player.setFragmentCount(sd.fragmentCount);
                player.setGunUnlocked(sd.gunUnlocked);
                player.setImmortalityUnlocked(sd.immortalityUnlocked);
                try { player.changeAmmo(sd.ammo); } catch (Exception ignored) {}
                // restore new abilities
                player.setDashUnlocked(sd.dashUnlocked);
                player.setDoubleJumpUnlocked(sd.doubleJumpUnlocked);
                player.setUntargetableUnlocked(sd.untargetableUnlocked);
                player.setBlockUnlocked(sd.blockUnlocked);
            }
        } catch (Exception ignored) {}
        player.loadLvlData(levelManager.getCurrentLevel().getLevelData());
        player.setSpawn(levelManager.getCurrentLevel().getPlayerSpawn());
    }

    @Override
    public void update() {
        System.out.println("DBG: Playing.update -> gameOver=" + gameOver + " playerDying=" + playerDying + " lvlCompleted=" + lvlCompleted + " fragmentCollected=" + fragmentCollected + " fragmentDialogueDone=" + fragmentDialogueDone + " startFade=" + startFadeToLevelComplete);
        // if a cutscene is active, only update it and skip normal game updates
        if (game.getCutsceneManager() != null && game.getCutsceneManager().isActive()) {
            game.getCutsceneManager().update();
            return;
        }
        // update status message timer
        updateStatusMessage();
        if (paused)
            pauseOverlay.update();
        else if (lvlCompleted)
            levelCompletedOverlay.update();
        else if (gameCompleted)
            gameCompletedOverlay.update();
        else if (gameOver)
            gameOverOverlay.update(); // <-- Ensure GameOver overlay updates BEFORE playerDying
        else if (playerDying)
            player.update();
        else {
            // Update active dialogue effects
            updateDialogue();

            // If fragment was collected and its dialogue is done, start fade to level complete (one-shot)
            if (fragmentCollected && fragmentDialogueDone && !startFadeToLevelComplete && !lvlCompleted) {
                startFadeToLevelComplete = true;
                fadeAlpha = 0f;
            }

            // If we're starting fade -> increment fadeAlpha until full then set level complete
            if (startFadeToLevelComplete) {
                fadeAlpha += fadeSpeed;
                if (fadeAlpha >= 1f) {
                    fadeAlpha = 1f;
                    startFadeToLevelComplete = false;
                    // Finally mark level completed. This will trigger overlay and prevent normal updates.
                    setLevelCompleted(true);
                    return;
                } else {
                    // While fading, continue updating dialogue visuals only (VO may finish concurrently).
                    // Do not advance game simulation.
                    return;
                }
            }

            // If dialogue for fragment is showing and not done, freeze game world updates and block input effects
            if (showingFragmentDialogue && !fragmentDialogueDone) {
                // update any relevant dialogue visuals (already done above)
                // prevent normal gameplay updates while the dialogue is active
                return;
            }

            // normal gameplay updates
            if (drawRain)
                rain.update(xLvlOffset);
            levelManager.update();
            objectManager.update(levelManager.getCurrentLevel().getLevelData(), player);
            player.update();
            enemyManager.update(levelManager.getCurrentLevel().getLevelData());
            checkCloseToBorder();
        }
    }

    private void updateDialogue() {
        for (DialogueEffect de : dialogEffects)
            if (de.isActive())
                de.update();
    }

    private void drawDialogue(Graphics g, int xLvlOffset) {
        for (DialogueEffect de : dialogEffects)
            if (de.isActive()) {
                if (de.getType() == QUESTION)
                    g.drawImage(questionImgs[de.getAniIndex()], de.getX() - xLvlOffset, de.getY(), DIALOGUE_WIDTH, DIALOGUE_HEIGHT, null);
                else
                    g.drawImage(exclamationImgs[de.getAniIndex()], de.getX() - xLvlOffset, de.getY(), DIALOGUE_WIDTH, DIALOGUE_HEIGHT, null);
            }
    }

    public void addDialogue(int x, int y, int type) {
        dialogEffects.add(new DialogueEffect(x, y - (int) (Game.SCALE * 15), type));
        for (DialogueEffect de : dialogEffects)
            if (!de.isActive())
                if (de.getType() == type) {
                    de.reset(x, -(int) (Game.SCALE * 15));
                    return;
                }
    }

    private void checkCloseToBorder() {
        int playerX = (int) player.getHitbox().x;
        int diff = playerX - xLvlOffset;

        if (diff > rightBorder)
            xLvlOffset += diff - rightBorder;
        else if (diff < leftBorder)
            xLvlOffset += diff - leftBorder;

        xLvlOffset = Math.max(Math.min(xLvlOffset, maxLvlOffsetX), 0);
    }

    @Override
    public void draw(Graphics g) {
        g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);

        drawClouds(g);
        if (drawRain)
            rain.draw(g, xLvlOffset);

        levelManager.draw(g, xLvlOffset);
        objectManager.draw(g, xLvlOffset);
        enemyManager.draw(g, xLvlOffset);
        player.render(g, xLvlOffset);

        // Draw level and fragment count indicator
        try {
            String lvlText = "Level: " + (levelManager.getLevelIndex() + 1);
            String fragText = "Fragments: " + player.getFragmentCount() + "/6";
            g.setColor(Color.WHITE);
            g.drawString(lvlText + "  " + fragText, 12, 16);
        } catch (Exception ignored) {}
        if (debugOffsetMode) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(8, 8, 260, 60);
            g.setColor(Color.WHITE);
            g.drawString("Debug Offset Mode (F3 to toggle)", 12, 24);
            g.drawString("xDrawOffset: " + player.getPlayerCharacter().xDrawOffset, 12, 40);
            g.drawString("yDrawOffset: " + player.getPlayerCharacter().yDrawOffset, 140, 40);
            g.drawString("Use Arrow Keys to nudge (1px).", 12, 56);
        }

        // transient status message (e.g., Immortality ON/OFF)
        if (statusMessage != null) {
            java.awt.FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(statusMessage) + 20;
            int h = fm.getHeight() + 12;
            int x = Game.GAME_WIDTH / 2 - w / 2;
            int y = 40;
            java.awt.Color bg = new java.awt.Color(0, 0, 0, 180);
            g.setColor(bg);
            g.fillRect(x, y, w, h);
            g.setColor(java.awt.Color.WHITE);
            g.drawRect(x, y, w, h);
            g.drawString(statusMessage, x + 10, y + fm.getAscent() + 6);
        }
        objectManager.drawBackgroundTrees(g, xLvlOffset);
        drawDialogue(g, xLvlOffset);

        // draw fade if active (fadeAlpha 0..1)
        if (fadeAlpha > 0f) {
            int alpha = (int) (Math.min(1f, fadeAlpha) * 255);
            g.setColor(new Color(0, 0, 0, alpha));
            g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
        }

        if (game.getCutsceneManager() != null)
            game.getCutsceneManager().draw(g);

        if (paused) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
            pauseOverlay.draw(g);
        } else if (gameOver)
            gameOverOverlay.draw(g);
        else if (lvlCompleted)
            levelCompletedOverlay.draw(g);
        else if (gameCompleted)
            gameCompletedOverlay.draw(g);

    }

    private void drawClouds(Graphics g) {
        for (int i = 0; i < 4; i++)
            g.drawImage(bigCloud, i * BIG_CLOUD_WIDTH - (int) (xLvlOffset * 0.3), (int) (204 * Game.SCALE), BIG_CLOUD_WIDTH, BIG_CLOUD_HEIGHT, null);

        for (int i = 0; i < smallCloudsPos.length; i++)
            g.drawImage(smallCloud, SMALL_CLOUD_WIDTH * 4 * i - (int) (xLvlOffset * 0.7), smallCloudsPos[i], SMALL_CLOUD_WIDTH, SMALL_CLOUD_HEIGHT, null);
    }

    public void setGameCompleted() {
        gameCompleted = true;
    }

    public void resetGameCompleted() {
        gameCompleted = false;
    }

    public void resetAll() {
        gameOver = false;
        paused = false;
        lvlCompleted = false;
        playerDying = false;
        drawRain = false;

        // reset fragment/dialogue flags
        fragmentCollected = false;
        fragmentDialogueDone = false;
        fragmentVOPlaying = false;
        startFadeToLevelComplete = false;
        fadeAlpha = 0f;
        showingFragmentDialogue = false;

        setDrawRainBoolean();

        player.resetAll();
        enemyManager.resetAllEnemies();
        objectManager.resetAllObjects();
        dialogEffects.clear();
    }

    private void setDrawRainBoolean() {
        if (rnd.nextFloat() >= 0.8f)
            drawRain = true;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
        System.out.println("DEBUG: Playing.setGameOver called. gameOver=" + gameOver);
    }

    public void checkObjectHit(Rectangle2D.Float attackBox) {
        objectManager.checkObjectHit(attackBox);
    }

    public void checkEnemyHit(Rectangle2D.Float attackBox) {
        enemyManager.checkEnemyHit(attackBox);
    }

    public void checkPotionTouched(Rectangle2D.Float hitbox) {
        objectManager.checkObjectTouched(hitbox);
    }

    public void checkSpikesTouched(Player p) {
        objectManager.checkSpikesTouched(p);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Block attacks while fragment dialogue is active
        if (showingFragmentDialogue && !fragmentDialogueDone) {
            return;
        }

        if (!gameOver) {
            if (e.getButton() == MouseEvent.BUTTON1)
                player.setAttacking(true);
            else if (e.getButton() == MouseEvent.BUTTON3) {
                // If dash unlocked and available, use dash on right-click; otherwise fallback to powerAttack
                try {
                    if (player.isDashAvailable()) {
                        player.dash();
                        statusMessage = "Dash";
                        statusMsgTimer = 600;
                    } else if (player.isDashUnlocked() && !player.isDashAvailable()) {
                        statusMessage = "Dash on cooldown";
                        statusMsgTimer = 800;
                    } else {
                        player.powerAttack();
                    }
                } catch (Exception ignored) { player.powerAttack(); }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Block most inputs if game over/completed/level completed
        if (!gameOver && !gameCompleted && !lvlCompleted) {
            if (e.getKeyCode() == KeyEvent.VK_F3) {
                debugOffsetMode = !debugOffsetMode;
                return;
            }

            if (debugOffsetMode) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> player.getPlayerCharacter().xDrawOffset -= 1;
                    case KeyEvent.VK_RIGHT -> player.getPlayerCharacter().xDrawOffset += 1;
                    case KeyEvent.VK_UP -> player.getPlayerCharacter().yDrawOffset -= 1;
                    case KeyEvent.VK_DOWN -> player.getPlayerCharacter().yDrawOffset += 1;
                }
                return;
            }

            // If fragment dialogue is active, block gameplay inputs except ESC skip
            if (showingFragmentDialogue && !fragmentDialogueDone) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    // Skip dialogue and stop VO if playing
                    skipFragmentDialogueAndVO();
                }
                // ignore other inputs while dialogue is active
                return;
            }

            switch (e.getKeyCode()) {
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    player.setLeft(true);
                    System.out.println("[Input] KeyPressed LEFT/A -> setLeft(true)");
                    break;
                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    player.setRight(true);
                    System.out.println("[Input] KeyPressed RIGHT/D -> setRight(true)");
                    break;
                case KeyEvent.VK_F:
                    if (!fPressed) {
                        player.shoot();
                        fPressed = true;
                        System.out.println("[Input] KeyPressed F -> shoot() (fPressed now true)");
                    }
                    break;
                case KeyEvent.VK_R:
                    // Untargetable activation (1.5s) with cooldown
                    try {
                        if (player.canUseR()) {
                            player.startUntargetable(1500);
                            player.setROnCooldown();
                            statusMessage = "Untargetable: ON";
                            statusMsgTimer = 1500;
                        } else {
                            statusMessage = "Untargetable on cooldown";
                            statusMsgTimer = 1000;
                        }
                    } catch (Exception ignored) {}
                    break;
                case KeyEvent.VK_Q:
                    // Hold-to-use: start block (or immortality if final fragment unlocked)
                    try {
                        if (player.canUseQ()) {
                            player.startQHold();
                            statusMessage = player.isImmortalityActive() ? "Immortality: ON" : "Block: ON";
                            statusMsgTimer = 800;
                        } else {
                            statusMessage = "Ability on cooldown";
                            statusMsgTimer = 800;
                        }
                    } catch (Exception ignored) {}
                    break;
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_UP:
                    player.setJump(true);
                    System.out.println("[Input] KeyPressed SPACE/UP -> setJump(true)");
                    break;
                case KeyEvent.VK_G:
                    // Toggle immortality if unlocked
                    try {
                        if (player.isImmortalityUnlocked()) {
                            player.toggleImmortality();
                            if (player.isImmortalityActive()) {
                                statusMessage = "Immortality: ON";
                            } else {
                                statusMessage = "Immortality: OFF";
                            }
                            statusMsgTimer = 1800; // show for ~1.8 seconds
                        }
                    } catch (Exception ignored) {}
                    break;
                case KeyEvent.VK_SHIFT:
                    // Only treat left shift as run if possible
                    if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_LEFT)
                        player.setRunning(true);
                    break;
                case KeyEvent.VK_ESCAPE:
                    // If a cutscene is active, skip it. If any on-screen dialogue icons are active, deactivate them.
                    try {
                        if (game.getCutsceneManager() != null && game.getCutsceneManager().isActive()) {
                            game.getCutsceneManager().skip();
                            return;
                        }
                    } catch (Exception ignored) {}

                    // If any dialogue effects active, deactivate all (ESC acts as skip)
                    boolean anyActive = false;
                    for (DialogueEffect de : dialogEffects) {
                        if (de.isActive()) { anyActive = true; break; }
                    }
                    if (anyActive) {
                        for (DialogueEffect de : dialogEffects) de.deactive();
                        // also clear transient status messages
                        statusMessage = null;
                        statusMsgTimer = 0;
                        return;
                    }

                    // Otherwise, toggle pause as before
                    paused = !paused;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!gameOver && !gameCompleted && !lvlCompleted) {
            if (debugOffsetMode) return;

            switch (e.getKeyCode()) {
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    player.setLeft(false);
                    System.out.println("[Input] KeyReleased LEFT/A -> setLeft(false)");
                    break;
                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    player.setRight(false);
                    System.out.println("[Input] KeyReleased RIGHT/D -> setRight(false)");
                    break;
                case KeyEvent.VK_F:
                    fPressed = false;
                    System.out.println("[Input] KeyReleased F -> fPressed=false");
                    break;
                case KeyEvent.VK_Q:
                    try {
                        // stop hold-based Q ability (block or immortality)
                        boolean wasImm = player.isImmortalityActive();
                        boolean wasBlock = player.isBlockActive();
                        player.stopQHold();
                        if (wasImm) {
                            statusMessage = "Immortality: OFF";
                        } else if (wasBlock) {
                            statusMessage = "Block: OFF";
                        }
                        statusMsgTimer = 800;
                    } catch (Exception ignored) {}
                    break;
                case KeyEvent.VK_SHIFT:
                    if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_LEFT)
                        player.setRunning(false);
                    break;
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_UP:
                    player.setJump(false);
                    System.out.println("[Input] KeyReleased SPACE/UP -> setJump(false)");
                    break;
            }
        }
    }

    private void updateStatusMessage() {
        if (statusMsgTimer > 0) {
            statusMsgTimer -= 16; // approximate tick
            if (statusMsgTimer <= 0) {
                statusMsgTimer = 0;
                statusMessage = null;
            }
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (!gameOver && !gameCompleted && !lvlCompleted)
            if (paused)
                pauseOverlay.mouseDragged(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (gameOver)
            gameOverOverlay.mousePressed(e);
        else if (paused)
            pauseOverlay.mousePressed(e);
        else if (lvlCompleted)
            levelCompletedOverlay.mousePressed(e);
        else if (gameCompleted)
            gameCompletedOverlay.mousePressed(e);

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (gameOver)
            gameOverOverlay.mouseReleased(e);
        else if (paused)
            pauseOverlay.mouseReleased(e);
        else if (lvlCompleted)
            levelCompletedOverlay.mouseReleased(e);
        else if (gameCompleted)
            gameCompletedOverlay.mouseReleased(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (gameOver)
            gameOverOverlay.mouseMoved(e);
        else if (paused)
            pauseOverlay.mouseMoved(e);
        else if (lvlCompleted)
            levelCompletedOverlay.mouseMoved(e);
        else if (gameCompleted)
            gameCompletedOverlay.mouseMoved(e);
    }

    /**
     * Call this when fragment is collected (from ObjectManager or fragment object).
     * Supply the fragment index (1..6).
     * Do NOT call setLevelCompleted(true) directly from the fragment pickup.
     * This starts the fragment-dialogue sequence (VO + visuals).
     */
    public void onFragmentCollected(int fragmentIndex) {
        if (fragmentCollected) return; // guard: only once
        fragmentCollected = true;
        fragmentDialogueDone = false;
        showingFragmentDialogue = true;
        fragmentVOPlaying = false;

        // show visual dialogue effect (exclamation above player)
        addDialogue((int) player.getHitbox().x, (int) player.getHitbox().y - 20, EXCLAMATION);

        // Try to start VO clip for this fragment. AudioPlayer will call the callback when VO ends.
        try {
            if (game.getAudioPlayer() != null) {
                // play VO and provide onComplete callback
                final int idx = fragmentIndex;
                game.getAudioPlayer().playFragmentVO(idx, new Runnable() {
                    @Override
                    public void run() {
                        // This will run on the audio event thread — it's safe to call this method.
                        onFragmentDialogueFinished();
                    }
                });
                fragmentVOPlaying = true;
            } else {
                // no audio player — finish immediately
                onFragmentDialogueFinished();
            }
        } catch (Exception e) {
            e.printStackTrace();
            onFragmentDialogueFinished();
        }
    }

    /**
     * Call this when fragment dialogue / voice audio finishes or when ESC skip occurs.
     */
    public void onFragmentDialogueFinished() {
        // Deactivate dialogue visuals
        for (DialogueEffect de : dialogEffects)
            de.deactive();

        fragmentDialogueDone = true;
        showingFragmentDialogue = false;
        // Ensure VO stopped
        try { if (game.getAudioPlayer() != null) game.getAudioPlayer().stopAllVO(); } catch (Exception ignored) {}
        fragmentVOPlaying = false;

        // Begin fade to level complete (handled in update)
        startFadeToLevelComplete = true;
        fadeAlpha = 0f;
    }

    /**
     * Skip fragment dialogue and any VO (used by ESC).
     */
    private void skipFragmentDialogueAndVO() {
        try { if (game.getAudioPlayer() != null) game.getAudioPlayer().stopAllVO(); } catch (Exception ignored) {}
        fragmentVOPlaying = false;
        onFragmentDialogueFinished();
    }

    public void setLevelCompleted(boolean levelCompleted) {
        // play lvl-complete sound/effect
        try { if (game.getAudioPlayer() != null) game.getAudioPlayer().lvlCompleted(); } catch (Exception ignored) {}
        if (levelManager.getLevelIndex() + 1 >= levelManager.getAmountOfLevels()) {
            gameCompleted = true;
            levelManager.setLevelIndex(0);
            levelManager.loadNextLevel();
            resetAll();
            return;
        }
        this.lvlCompleted = levelCompleted;
    }

    public void setMaxLvlOffset(int lvlOffset) {
        this.maxLvlOffsetX = lvlOffset;
    }

    public void unpauseGame() {
        paused = false;
    }

    public void windowFocusLost() {
        player.resetDirBooleans();
    }

    public Player getPlayer() {
        return player;
    }

    public EnemyManager getEnemyManager() {
        return enemyManager;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }
    public boolean isGameOver() {
        return gameOver;
    }
    public void setPlayerDying(boolean playerDying) {
        this.playerDying = playerDying;
    }

}

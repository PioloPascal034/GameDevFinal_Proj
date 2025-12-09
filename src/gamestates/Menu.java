package gamestates;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import main.Game;
import ui.MenuButton;
import utilz.LoadSave;

public class Menu extends State implements Statemethods {

    private MenuButton[] buttons = new MenuButton[4];
    private BufferedImage backgroundImg, backgroundImgPink;
    private int menuX, menuY, menuWidth, menuHeight;
    private boolean introStarted = false;

    public Menu(Game game) {
        super(game);
        loadButtons();
        loadBackground();
        backgroundImgPink = LoadSave.GetSpriteAtlas(LoadSave.MENU_BACKGROUND_IMG);

    }

    private void loadBackground() {
        backgroundImg = LoadSave.GetSpriteAtlas(LoadSave.MENU_BACKGROUND);
        menuWidth = (int) (backgroundImg.getWidth() * Game.SCALE);
        menuHeight = (int) (backgroundImg.getHeight() * Game.SCALE);
        menuX = Game.GAME_WIDTH / 2 - menuWidth / 2;
        menuY = (int) (25 * Game.SCALE);
    }

    private void loadButtons() {
        buttons[0] = new MenuButton(Game.GAME_WIDTH / 2, (int) (130 * Game.SCALE), 0, Gamestate.PLAYING);
        buttons[1] = new MenuButton(Game.GAME_WIDTH / 2, (int) (200 * Game.SCALE), 1, Gamestate.OPTIONS);
        buttons[2] = new MenuButton(Game.GAME_WIDTH / 2, (int) (270 * Game.SCALE), 3, Gamestate.CREDITS);
        buttons[3] = new MenuButton(Game.GAME_WIDTH / 2, (int) (340 * Game.SCALE), 2, Gamestate.QUIT);
    }

    @Override
    public void update() {
        // If a cutscene is active, let it run and skip menu updates
        if (game.getCutsceneManager() != null && game.getCutsceneManager().isActive()) {
            game.getCutsceneManager().update();
            return;
        }

        // start the intro cutscene once when menu first appears, unless save marks it played
        if (!introStarted) {
            introStarted = true;
            boolean shouldPlay = true;
            try {
                if (game.getSaveManager() != null && game.getSaveManager().getSaveData() != null) {
                    shouldPlay = !game.getSaveManager().getSaveData().introPlayed;
                }
            } catch (Exception ignored) {}
            if (shouldPlay) {
                if (game.getCutsceneManager() != null) game.getCutsceneManager().startIntro();
            }
            return;
        }

        for (MenuButton mb : buttons)
            mb.update();
    }

    @Override
    public void draw(Graphics g) {
        g.drawImage(backgroundImgPink, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        g.drawImage(backgroundImg, menuX, menuY, menuWidth, menuHeight, null);

        for (MenuButton mb : buttons)
            mb.draw(g);

        if (game.getCutsceneManager() != null)
            game.getCutsceneManager().draw(g);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        for (MenuButton mb : buttons) {
            if (isIn(e, mb)) {
                mb.setMousePressed(true);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        for (MenuButton mb : buttons) {
            if (isIn(e, mb)) {
                if (mb.isMousePressed()) {
                    mb.applyGamestate();
                    if (mb.getState() == Gamestate.PLAYING) {
                        // Match GameOverOverlay -> Try Again behavior:
                        // 1) (re)load the current level
                        // 2) set player spawn to level spawn
                        // 3) clear any game over / dying flags
                        // 4) reset all Playing state
                        // 5) ensure level song is started and restore volume
                        try {
                            game.getPlaying().getLevelManager().loadNextLevel();
                        } catch (Exception ignored) {}

                        try {
                            if (game.getPlaying().getLevelManager() != null && game.getPlaying().getLevelManager().getCurrentLevel() != null) {
                                game.getPlaying().getPlayer().setSpawn(game.getPlaying().getLevelManager().getCurrentLevel().getPlayerSpawn());
                            }
                        } catch (Exception ignored) {}

                        try {
                            game.getPlaying().setGameOver(false);
                            game.getPlaying().setPlayerDying(false);
                            game.getPlaying().resetAll();
                        } catch (Exception ignored) {}

                        try {
                            game.getAudioPlayer().setLevelSong(game.getPlaying().getLevelManager().getLevelIndex());
                            // Restore audio volume after returning from Menu
                            game.getAudioPlayer().restoreSavedVolume(800);
                        } catch (Exception ignored) {}
                    }
                }
                break;
            }
        }
        resetButtons();
    }

    private void resetButtons() {
        for (MenuButton mb : buttons)
            mb.resetBools();

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        for (MenuButton mb : buttons)
            mb.setMouseOver(false);

        for (MenuButton mb : buttons)
            if (isIn(e, mb)) {
                mb.setMouseOver(true);
                break;
            }

    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Allow ESC to skip an active cutscene started from the menu (e.g., intro)
        try {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                if (game.getCutsceneManager() != null && game.getCutsceneManager().isActive()) {
                    game.getCutsceneManager().skip();
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void mouseClicked(MouseEvent e) {
                  

    }

    @Override
    public void keyReleased(KeyEvent e) {
                  

    }

}
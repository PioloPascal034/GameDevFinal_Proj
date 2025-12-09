package utilz;

import entities.PlayerCharacter;
import utilz.Constants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.File;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

public class LoadSave {
    // New player sprite sheet
    public static final String PLAYER_SHEET_NEW = "player_sprites/new_player_sheet.png";

    // Loads all player states as per PlayerConstants
    public static BufferedImage[][] loadPlayerAnimations() {
        BufferedImage img = GetSpriteAtlas("player_sprites/new_player_sheet.png");
        if (img == null) {
            System.err.println("Player sprite sheet not found: player_sprites/new_player_sheet.png");
            return new BufferedImage[0][0];
        }
        int frameW = 250;
        int frameH = 192;
        int imgW = img.getWidth();
        int imgH = img.getHeight();
        BufferedImage[][] animations = new BufferedImage[7][];
        // IDLE: row 0, cols 3-4
        animations[Constants.PlayerConstants.IDLE] = new BufferedImage[2];
        for (int i = 0; i < 2; i++) {
            int x = (3+i)*frameW, y = 0;
            if (x+frameW <= imgW && y+frameH <= imgH)
                animations[Constants.PlayerConstants.IDLE][i] = img.getSubimage(x, y, frameW, frameH);
            else
                animations[Constants.PlayerConstants.IDLE][i] = null;
        }
        // RUNNING: row 0, col 5 to row 1, col 4
        animations[Constants.PlayerConstants.RUNNING] = new BufferedImage[10];
        for (int i = 0; i < 5; i++) {
            int x = (5+i)*frameW, y = 0;
            if (x+frameW <= imgW && y+frameH <= imgH)
                animations[Constants.PlayerConstants.RUNNING][i] = img.getSubimage(x, y, frameW, frameH);
            else
                animations[Constants.PlayerConstants.RUNNING][i] = null;
        }
        for (int i = 0; i < 5; i++) {
            int x = i*frameW, y = frameH;
            if (x+frameW <= imgW && y+frameH <= imgH)
                animations[Constants.PlayerConstants.RUNNING][5+i] = img.getSubimage(x, y, frameW, frameH);
            else
                animations[Constants.PlayerConstants.RUNNING][5+i] = null;
        }
        // JUMP: row 2, cols 2-5
        animations[Constants.PlayerConstants.JUMP] = new BufferedImage[4];
        for (int i = 0; i < 4; i++) {
            int x = (2+i)*frameW, y = 2*frameH;
            if (x+frameW <= imgW && y+frameH <= imgH)
                animations[Constants.PlayerConstants.JUMP][i] = img.getSubimage(x, y, frameW, frameH);
            else
                animations[Constants.PlayerConstants.JUMP][i] = null;
        }
        // FALLING: placeholder (can be filled later)
        animations[Constants.PlayerConstants.FALLING] = new BufferedImage[1];
        animations[Constants.PlayerConstants.FALLING][0] = null;
        // ATTACK: row 5, col 5 to row 6, col 1
        animations[Constants.PlayerConstants.ATTACK] = new BufferedImage[4];
        for (int i = 0; i < 2; i++) {
            int x = (5+i)*frameW, y = 5*frameH;
            if (x+frameW <= imgW && y+frameH <= imgH)
                animations[Constants.PlayerConstants.ATTACK][i] = img.getSubimage(x, y, frameW, frameH);
            else
                animations[Constants.PlayerConstants.ATTACK][i] = null;
        }
        for (int i = 0; i < 2; i++) {
            int x = i*frameW, y = 6*frameH;
            if (x+frameW <= imgW && y+frameH <= imgH)
                animations[Constants.PlayerConstants.ATTACK][2+i] = img.getSubimage(x, y, frameW, frameH);
            else
                animations[Constants.PlayerConstants.ATTACK][2+i] = null;
        }
        // HIT: row 9, col 4
        animations[Constants.PlayerConstants.HIT] = new BufferedImage[1];
        int xHurt = 4*frameW, yHurt = 9*frameH;
        if (xHurt+frameW <= imgW && yHurt+frameH <= imgH)
            animations[Constants.PlayerConstants.HIT][0] = img.getSubimage(xHurt, yHurt, frameW, frameH);
        else
            animations[Constants.PlayerConstants.HIT][0] = null;
        // DEAD: row 10, col 1 to col 6
        animations[Constants.PlayerConstants.DEAD] = new BufferedImage[6];
        for (int i = 0; i < 6; i++) {
            int x = (1+i)*frameW, y = 10*frameH;
            if (x+frameW <= imgW && y+frameH <= imgH)
                animations[Constants.PlayerConstants.DEAD][i] = img.getSubimage(x, y, frameW, frameH);
            else
                animations[Constants.PlayerConstants.DEAD][i] = null;
        }
        return animations;
    }

    // Sprites
    public static final String PLAYER_PIRATE = "player_sprites.png";
        // New idle spritesheet for player
        public static final String PLAYER_IDLE_NEW = "player_sprites/idle/new_idle.png";
    public static final String PLAYER_ORC = "player_orc.png";
    public static final String PLAYER_SOLDIER = "player_soldier.png";
    public static final String LEVEL_ATLAS = "outside_sprites.png";
    public static final String MENU_BUTTONS = "button_atlas.png";
    public static final String MENU_BACKGROUND = "menu_background.png";
    public static final String PAUSE_BACKGROUND = "pause_menu.png";
    public static final String SOUND_BUTTONS = "sound_button.png";
    public static final String URM_BUTTONS = "urm_buttons.png";
    public static final String VOLUME_BUTTONS = "volume_buttons.png";
    public static final String MENU_BACKGROUND_IMG = "background_menu.png";
    public static final String PLAYING_BG_IMG = "playing_bg_img.png";
    public static final String BIG_CLOUDS = "big_clouds.png";
    public static final String SMALL_CLOUDS = "small_clouds.png";
    public static final String CRABBY_SPRITE = "crabby_sprite.png";
    public static final String STATUS_BAR = "health_power_bar.png";
    public static final String COMPLETED_IMG = "completed_sprite.png";
    public static final String POTION_ATLAS = "potions_sprites.png";
    public static final String CONTAINER_ATLAS = "objects_sprites.png";
    public static final String BALL = "ball.png";
    public static final String BULLET = "gun/bullet.png";
    public static final String GUN = "gun/gun.png";
    public static final String FRAGMENT = "fragment.png";
    public static final String TRAP_ATLAS = "trap_atlas.png";
    public static final String CANNON_ATLAS = "cannon_atlas.png";
    public static final String CANNON_BALL = "ball.png";
    public static final String DEATH_SCREEN = "death_screen.png";
    public static final String OPTIONS_MENU = "options_background.png";
    public static final String PINKSTAR_ATLAS = "pinkstar_atlas.png";
    public static final String QUESTION_ATLAS = "question_atlas.png";
    public static final String EXCLAMATION_ATLAS = "exclamation_atlas.png";
    public static final String SHARK_ATLAS = "shark_atlas.png";
    public static final String CREDITS = "credits_list.png";
    public static final String GRASS_ATLAS = "grass_atlas.png";
    public static final String TREE_ONE_ATLAS = "tree_one_atlas.png";
    public static final String TREE_TWO_ATLAS = "tree_two_atlas.png";
    public static final String GAME_COMPLETED = "game_completed.png";
    public static final String RAIN_PARTICLE = "rain_particle.png";
    public static final String WATER_TOP = "water_atlas_animation.png";
    public static final String WATER_BOTTOM = "water.png";
    public static final String SHIP = "ship.png";

    // Add other resources as needed

    // -----------------------------
    // Load player animations
    // Old logic (commented out for revert):
    /*
    public static BufferedImage[][] loadAnimations(PlayerCharacter pc) {
        BufferedImage img = GetSpriteAtlas(pc.playerAtlas);
        if (img == null) {
            System.err.println("Player sprite not found: " + pc.playerAtlas);
            return new BufferedImage[0][0];
        }

        BufferedImage[][] animations = new BufferedImage[pc.rowA][pc.colA];
        for (int j = 0; j < animations.length; j++)
            for (int i = 0; i < animations[j].length; i++)
                animations[j][i] = img.getSubimage(i * pc.spriteW, j * pc.spriteH, pc.spriteW, pc.spriteH);

        return animations;
    }
    */

    // New logic: load idle animation from new spritesheet
    public static BufferedImage[] loadNewIdleAnimation() {
        BufferedImage img = GetSpriteAtlas(PLAYER_IDLE_NEW);
        if (img == null) {
            System.err.println("New idle sprite not found: " + PLAYER_IDLE_NEW);
            return new BufferedImage[0];
        }
        // Assuming 2 frames, each 64x64 (adjust if needed)
        int frameWidth = 64;
        int frameHeight = 64;
        int frames = 2;
        BufferedImage[] idleFrames = new BufferedImage[frames];
        for (int i = 0; i < frames; i++) {
            idleFrames[i] = img.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
        }
        return idleFrames;
    }

    // Backwards-compatible loader used by existing Player code
    public static BufferedImage[][] loadTestAnimations() {
        return loadPlayerAnimations();
    }

    // New: load animations per-state from separated folders under /res/player_sprites/<state>/
    public static BufferedImage[][] loadAnimations(entities.PlayerCharacter pc) {
        BufferedImage[][] animations = new BufferedImage[7][];

        // map player constants to folder names
        String[] folders = new String[7];
        folders[utilz.Constants.PlayerConstants.IDLE] = "idle";
        folders[utilz.Constants.PlayerConstants.RUNNING] = "run";
        folders[utilz.Constants.PlayerConstants.JUMP] = "jump";
        folders[utilz.Constants.PlayerConstants.FALLING] = "jump"; // reuse jump for falling
        folders[utilz.Constants.PlayerConstants.ATTACK] = "dash";
        folders[utilz.Constants.PlayerConstants.HIT] = "hurt";
        folders[utilz.Constants.PlayerConstants.DEAD] = "death";

        for (int state = 0; state < folders.length; state++) {
            String folder = folders[state];
            if (folder == null) {
                animations[state] = new BufferedImage[1];
                animations[state][0] = null;
                continue;
            }

            BufferedImage[] imgs = getSpritesFromFolder("player_sprites/" + folder);
            if (imgs.length == 0) {
                animations[state] = new BufferedImage[1];
                animations[state][0] = null;
                continue;
            }

            // scale frames to character sprite size
            BufferedImage[] scaled = new BufferedImage[imgs.length];
            for (int i = 0; i < imgs.length; i++) {
                if (imgs[i] == null) continue;
                scaled[i] = scaleImage(imgs[i], pc.spriteW, pc.spriteH);
            }
            animations[state] = scaled;
        }

        return animations;
    }

    private static BufferedImage scaleImage(BufferedImage src, int w, int h) {
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return scaled;
    }

    private static BufferedImage[] getSpritesFromFolder(String folderPath) {
        URL url = LoadSave.class.getResource("/res/" + folderPath);
        if (url == null) return new BufferedImage[0];

        File folder;
        try {
            folder = new File(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return new BufferedImage[0];
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
        if (files == null) return new BufferedImage[0];

        java.util.Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        List<BufferedImage> imgs = new ArrayList<>();
        for (File f : files) {
            try {
                imgs.add(ImageIO.read(f));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return imgs.toArray(new BufferedImage[0]);
    }

    // Public wrapper for loading all sprite images from a folder under /res
    public static BufferedImage[] GetSpritesFromFolder(String folderPath) {
        return getSpritesFromFolder(folderPath);
    }

    private static java.util.Map<String, BufferedImage> getSpritesMapFromFolder(String folderPath) {
        URL url = LoadSave.class.getResource("/res/" + folderPath);
        if (url == null) return java.util.Collections.emptyMap();

        File folder;
        try {
            folder = new File(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return java.util.Collections.emptyMap();
        }

        java.util.Map<String, BufferedImage> map = new java.util.HashMap<>();
        // Walk folder recursively and read image files
        java.util.Queue<File> queue = new java.util.ArrayDeque<>();
        queue.add(folder);
        while (!queue.isEmpty()) {
            File dir = queue.poll();
            File[] list = dir.listFiles();
            if (list == null) continue;
            for (File f : list) {
                if (f.isDirectory()) {
                    queue.add(f);
                    continue;
                }
                String fname = f.getName().toLowerCase();
                if (fname.endsWith(".png") || fname.endsWith(".jpg") || fname.endsWith(".jpeg")) {
                    try {
                        BufferedImage img = ImageIO.read(f);
                        String name = f.getName();
                        int dot = name.lastIndexOf('.');
                        if (dot > 0) name = name.substring(0, dot);
                        map.put(name, img);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return map;
    }

    // Public wrapper to expose sprite name -> image map for a folder under /res
    public static java.util.Map<String, BufferedImage> GetSpritesMapFromFolder(String folderPath) {
        return getSpritesMapFromFolder(folderPath);
    }

    public static BufferedImage[][] loadPlayerSpritesFromFolder(String folderPath) {
        URL url = LoadSave.class.getResource("/res/" + folderPath);
        if (url == null) return null;

        File folder;
        try {
            folder = new File(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }

        BufferedImage[][] animations = new BufferedImage[7][];

        File[] children = folder.listFiles();
        if (children == null) return null;

        for (File child : children) {
            if (!child.isDirectory()) continue;
            String name = child.getName().toLowerCase();
            BufferedImage[] imgs = getSpritesFromFolder(folderPath + "/" + child.getName());
            if (imgs.length == 0) continue;

            if (name.contains("idle")) {
                animations[utilz.Constants.PlayerConstants.IDLE] = imgs;
            } else if (name.contains("run") || name.contains("walk")) {
                animations[utilz.Constants.PlayerConstants.RUNNING] = imgs;
            } else if (name.contains("jump")) {
                animations[utilz.Constants.PlayerConstants.JUMP] = imgs;
                animations[utilz.Constants.PlayerConstants.FALLING] = imgs;
            } else if (name.contains("attack") || name.contains("attak")) {
                animations[utilz.Constants.PlayerConstants.ATTACK] = imgs;
            } else if (name.contains("hurt") || name.contains("hit")) {
                animations[utilz.Constants.PlayerConstants.HIT] = imgs;
            } else if (name.contains("dead")) {
                animations[utilz.Constants.PlayerConstants.DEAD] = imgs;
            }
        }

        return animations;
    }

    // -----------------------------
    // Load sprite/image from /res folder
    public static BufferedImage GetSpriteAtlas(String fileName) {
        BufferedImage img = null;
        String path = "/res/" + fileName;
        InputStream is = LoadSave.class.getResourceAsStream(path);
        if (is == null) {
            System.err.println("Resource not found: " + path);
            return null;
        }

        try {
            img = ImageIO.read(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return img;
    }

    // -----------------------------
    // Load all level images from /res/lvls
    public static BufferedImage[] GetAllLevels() {
        URL url = LoadSave.class.getResource("/res/lvls");
        if (url == null) {
            System.err.println("Levels folder not found!");
            return new BufferedImage[0];
        }

        File folder;
        try {
            folder = new File(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return new BufferedImage[0];
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".png"));
        if (files == null) return new BufferedImage[0];

        // Sort files by name
        java.util.Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        BufferedImage[] imgs = new BufferedImage[files.length];
        for (int i = 0; i < files.length; i++) {
            try {
                imgs[i] = ImageIO.read(files[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return imgs;
    }

    // -----------------------------
    // Load audio files
    public static InputStream GetAudioStream(String fileName) {
        String path = "/res/audio/" + fileName;
        InputStream is = LoadSave.class.getResourceAsStream(path);
        if (is == null) {
            System.err.println("Audio file not found: " + fileName);
        }
        return is;
    }
}

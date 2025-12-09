package utilz;

import main.Game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class SaveManager {

    public static class SaveData {
        public int levelIndex = 0;
        public int fragmentCount = 0;
        public int ammo = 0;
        public boolean gunUnlocked = false;
        public boolean immortalityUnlocked = false;
        public boolean dashUnlocked = false;
        public boolean doubleJumpUnlocked = false;
        public boolean untargetableUnlocked = false;
        public boolean blockUnlocked = false;
        public boolean introPlayed = false;
    }

    private final File saveFile;
    private SaveData data;

    public SaveManager() {
        String dir = System.getProperty("user.dir");
        saveFile = new File(dir, "savegame.properties");
        data = new SaveData();
        load();
    }

    public SaveData getSaveData() {
        return data;
    }

    public void load() {
        if (!saveFile.exists()) return;
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(saveFile)) {
            p.load(fis);
            data.levelIndex = Integer.parseInt(p.getProperty("levelIndex", "0"));
            data.fragmentCount = Integer.parseInt(p.getProperty("fragmentCount", "0"));
            data.ammo = Integer.parseInt(p.getProperty("ammo", "0"));
            data.gunUnlocked = Boolean.parseBoolean(p.getProperty("gunUnlocked", "false"));
                data.immortalityUnlocked = Boolean.parseBoolean(p.getProperty("immortalityUnlocked", "false"));
                data.dashUnlocked = Boolean.parseBoolean(p.getProperty("dashUnlocked", "false"));
                data.doubleJumpUnlocked = Boolean.parseBoolean(p.getProperty("doubleJumpUnlocked", "false"));
                data.untargetableUnlocked = Boolean.parseBoolean(p.getProperty("untargetableUnlocked", "false"));
                data.blockUnlocked = Boolean.parseBoolean(p.getProperty("blockUnlocked", "false"));
            data.introPlayed = Boolean.parseBoolean(p.getProperty("introPlayed", "false"));
        } catch (Exception e) {
            System.err.println("Failed to load save: " + e.getMessage());
        }
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("levelIndex", Integer.toString(data.levelIndex));
        p.setProperty("fragmentCount", Integer.toString(data.fragmentCount));
        p.setProperty("ammo", Integer.toString(data.ammo));
        p.setProperty("gunUnlocked", Boolean.toString(data.gunUnlocked));
        p.setProperty("immortalityUnlocked", Boolean.toString(data.immortalityUnlocked));
        p.setProperty("dashUnlocked", Boolean.toString(data.dashUnlocked));
        p.setProperty("doubleJumpUnlocked", Boolean.toString(data.doubleJumpUnlocked));
        p.setProperty("untargetableUnlocked", Boolean.toString(data.untargetableUnlocked));
        p.setProperty("blockUnlocked", Boolean.toString(data.blockUnlocked));
        p.setProperty("introPlayed", Boolean.toString(data.introPlayed));
        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            p.store(fos, "Savegame properties");
        } catch (Exception e) {
            System.err.println("Failed to save game: " + e.getMessage());
        }
    }

    // Collect current game state and persist
    public void saveFromGame(Game game) {
        if (game == null) return;
        try {
            if (game.getPlaying() != null && game.getPlaying().getLevelManager() != null) {
                data.levelIndex = game.getPlaying().getLevelManager().getLevelIndex();
            }
            if (game.getPlaying() != null && game.getPlaying().getPlayer() != null) {
                data.fragmentCount = game.getPlaying().getPlayer().getFragmentCount();
                try { data.ammo = game.getPlaying().getPlayer().getAmmo(); } catch (Exception ignored) {}
                data.gunUnlocked = game.getPlaying().getPlayer().hasGun();
                data.immortalityUnlocked = game.getPlaying().getPlayer().isImmortalityUnlocked();
                data.dashUnlocked = game.getPlaying().getPlayer().isDashUnlocked();
                data.doubleJumpUnlocked = game.getPlaying().getPlayer().isDoubleJumpUnlocked();
                data.untargetableUnlocked = game.getPlaying().getPlayer().isUntargetableUnlocked();
                data.blockUnlocked = game.getPlaying().getPlayer().isBlockUnlocked();
            }
        } catch (Exception ignored) {}
        save();
    }

}

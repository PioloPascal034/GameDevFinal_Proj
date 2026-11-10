package levels;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import main.Game;
import utilz.LoadSave;
import game.items.ItemDefinitions;
import objects.CollectibleItem;

public class LevelManager {

	private Game game;
	private BufferedImage[] levelSprite;
	private BufferedImage[] waterSprite;
	private ArrayList<Level> levels;
	private int lvlIndex = 0, aniTick, aniIndex;

	public LevelManager(Game game) {
		this.game = game;
		importOutsideSprites();
		createWater();
		levels = new ArrayList<>();
		buildAllLevels();
	}

	private void createWater() {
		waterSprite = new BufferedImage[5];
		BufferedImage img = LoadSave.GetSpriteAtlas(LoadSave.WATER_TOP);
		if (img != null) {
			for (int i = 0; i < 4; i++)
				waterSprite[i] = (i + 1) * 32 <= img.getWidth() && img.getHeight() >= 32
						? img.getSubimage(i * 32, 0, 32, 32) : null;
		}
		waterSprite[4] = LoadSave.GetSpriteAtlas(LoadSave.WATER_BOTTOM);
	}

	public void loadNextLevel() {
		Level newLevel = levels.get(lvlIndex);
		game.getPlaying().getEnemyManager().loadEnemies(newLevel);
		game.getPlaying().getPlayer().loadLvlData(newLevel.getLevelData());
		game.getPlaying().setMaxLvlOffset(newLevel.getLvlOffset());
		game.getPlaying().getObjectManager().loadObjects(newLevel);
	}

	private void buildAllLevels() {
		BufferedImage[] allLevels = LoadSave.GetAllLevels();
		for (BufferedImage img : allLevels)
			if (img != null && img.getWidth() > 0 && img.getHeight() > 0)
				levels.add(new Level(img));
		if (levels.isEmpty())
			levels.add(new Level(createFallbackLevel()));
		addAuroraDemonstrationCollectibles();
	}

	private void addAuroraDemonstrationCollectibles() {
		Level first = levels.get(0);
		int spawnX = first.getPlayerSpawn().x;
		int spawnY = first.getPlayerSpawn().y;
		first.addCollectible(new CollectibleItem(spawnX + Game.TILES_SIZE * 3, spawnY,
				ItemDefinitions.AURORA_SHARD, 1));
		first.addCollectible(new CollectibleItem(spawnX + Game.TILES_SIZE * 6, spawnY,
				ItemDefinitions.HERB, 3));
	}

	/** Minimal playable fallback so a missing/corrupt level asset is recoverable. */
	private BufferedImage createFallbackLevel() {
		int width = Game.TILES_IN_WIDTH;
		int height = Game.TILES_IN_HEIGHT;
		BufferedImage fallback = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++) {
				int red = y == height - 1 ? 1 : 11;
				int green = (x == 2 && y == height - 2) ? 100 : 0;
				fallback.setRGB(x, y, new java.awt.Color(red, green, 0).getRGB());
			}
		return fallback;
	}

	private void importOutsideSprites() {
		BufferedImage img = LoadSave.GetSpriteAtlas(LoadSave.LEVEL_ATLAS);
		levelSprite = new BufferedImage[48];
		for (int j = 0; j < 4; j++)
			for (int i = 0; i < 12; i++) {
				int index = j * 12 + i;
				levelSprite[index] = img.getSubimage(i * 32, j * 32, 32, 32);
			}
	}

	public void draw(Graphics g, int lvlOffset) {
		Level level = levels.get(lvlIndex);
		int firstCol = Math.max(0, lvlOffset / Game.TILES_SIZE - 1);
		int lastCol = Math.min(level.getWidthInTiles(), firstCol + Game.TILES_IN_WIDTH + 3);
		int firstRow = 0;
		int lastRow = Math.min(level.getHeightInTiles(), Game.TILES_IN_HEIGHT + 1);
		for (int j = firstRow; j < lastRow; j++)
			for (int i = firstCol; i < lastCol; i++) {
				int index = levels.get(lvlIndex).getSpriteIndex(i, j);
				int x = Game.TILES_SIZE * i - lvlOffset;
				int y = Game.TILES_SIZE * j;
				if (index == 48 && waterSprite[aniIndex] != null)
					g.drawImage(waterSprite[aniIndex], x, y, Game.TILES_SIZE, Game.TILES_SIZE, null);
				else if (index == 49 && waterSprite[4] != null)
					g.drawImage(waterSprite[4], x, y, Game.TILES_SIZE, Game.TILES_SIZE, null);
				else if (index >= 0 && index < levelSprite.length && levelSprite[index] != null)
					g.drawImage(levelSprite[index], x, y, Game.TILES_SIZE, Game.TILES_SIZE, null);
			}
	}

	public void update() {
		updateWaterAnimation();
	}

	private void updateWaterAnimation() {
		aniTick++;
		if (aniTick >= 40) {
			aniTick = 0;
			aniIndex++;

			if (aniIndex >= 4)
				aniIndex = 0;
		}
	}

	public Level getCurrentLevel() {
		return levels.get(lvlIndex);
	}

	public int getAmountOfLevels() {
		return levels.size();
	}

	public int getLevelIndex() {
		return lvlIndex;
	}

	public void setLevelIndex(int lvlIndex) {
		this.lvlIndex = lvlIndex;
	}
}

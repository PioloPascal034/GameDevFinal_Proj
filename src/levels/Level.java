package levels;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import entities.Crabby;
import entities.Pinkstar;
import entities.Shark;
import entities.Boss;
import entities.LesserEnemy;
import main.Game;
import objects.BackgroundTree;
import objects.Cannon;
import objects.GameContainer;
import objects.Grass;
import objects.Potion;
import objects.Spike;
import objects.Ammo;
import objects.CollectibleItem;
import game.items.ItemDefinitions;

import static utilz.Constants.EnemyConstants.*;
import static utilz.Constants.ObjectConstants.*;

public class Level {

	private BufferedImage img;
	private int[][] lvlData;

	private ArrayList<Crabby> crabs = new ArrayList<>();
	private ArrayList<Pinkstar> pinkstars = new ArrayList<>();
	private ArrayList<Shark> sharks = new ArrayList<>();
	private ArrayList<Boss> bosses = new ArrayList<>();
	private ArrayList<LesserEnemy> lessers = new ArrayList<>();
	private ArrayList<Potion> potions = new ArrayList<>();
	private ArrayList<Ammo> ammos = new ArrayList<>();
	private ArrayList<Spike> spikes = new ArrayList<>();
	private ArrayList<GameContainer> containers = new ArrayList<>();
	private ArrayList<Cannon> cannons = new ArrayList<>();
	private ArrayList<BackgroundTree> trees = new ArrayList<>();
	private ArrayList<Grass> grass = new ArrayList<>();
	private ArrayList<CollectibleItem> collectibles = new ArrayList<>();

	private int lvlTilesWide;
	private int maxTilesOffset;
	private int maxLvlOffsetX;
	private Point playerSpawn;

	public Level(BufferedImage img) {
		if (img == null || img.getWidth() == 0 || img.getHeight() == 0)
			throw new IllegalArgumentException("A level image must not be empty");
		this.img = img;
		lvlData = new int[img.getHeight()][img.getWidth()];
		loadLevel();
		calcLvlOffsets();
	}

	private void loadLevel() {

		// Looping through the image colors just once. Instead of one per
		// object/enemy/etc..
		// Removed many methods in HelpMethods class.

		for (int y = 0; y < img.getHeight(); y++)
			for (int x = 0; x < img.getWidth(); x++) {
				Color c = new Color(img.getRGB(x, y), true);
				int red = c.getRed();
				int green = c.getGreen();
				int blue = c.getBlue();

				loadLevelData(red, x, y);
				loadEntities(green, x, y);
				loadObjects(blue, x, y);
			}
	}

	private void loadLevelData(int redValue, int x, int y) {
		if (redValue >= 50)
			lvlData[y][x] = 0;
		else
			lvlData[y][x] = redValue;
		switch (redValue) {
		case 0, 1, 2, 3, 30, 31, 33, 34, 35, 36, 37, 38, 39 -> 
		grass.add(new Grass((int) (x * Game.TILES_SIZE), (int) (y * Game.TILES_SIZE) - Game.TILES_SIZE, getRndGrassType(x)));
		}
	}

	private int getRndGrassType(int xPos) {
		return xPos % 2;
	}

	private void loadEntities(int greenValue, int x, int y) {
		switch (greenValue) {
		case CRABBY -> crabs.add(new Crabby(x * Game.TILES_SIZE, y * Game.TILES_SIZE));
		case PINKSTAR -> pinkstars.add(new Pinkstar(x * Game.TILES_SIZE, y * Game.TILES_SIZE));
		case SHARK -> sharks.add(new Shark(x * Game.TILES_SIZE, y * Game.TILES_SIZE));
		case GOTOKU -> bosses.add(new Boss(x * Game.TILES_SIZE, y * Game.TILES_SIZE, GOTOKU, "bosses/Gotoku"));
		case MEDUSSA -> bosses.add(new Boss(x * Game.TILES_SIZE, y * Game.TILES_SIZE, MEDUSSA, "bosses/medussa"));
		case ONRE -> bosses.add(new Boss(x * Game.TILES_SIZE, y * Game.TILES_SIZE, ONRE, "bosses/Onre"));
		case YUREI -> bosses.add(new Boss(x * Game.TILES_SIZE, y * Game.TILES_SIZE, YUREI, "bosses/Yurei"));
		case SKELETON_ARCHER -> lessers.add(new LesserEnemy(x * Game.TILES_SIZE, y * Game.TILES_SIZE, SKELETON_ARCHER));
		case SKELETON_SPEARMAN -> lessers.add(new LesserEnemy(x * Game.TILES_SIZE, y * Game.TILES_SIZE, SKELETON_SPEARMAN));
		case SKELETON_WARRIOR -> lessers.add(new LesserEnemy(x * Game.TILES_SIZE, y * Game.TILES_SIZE, SKELETON_WARRIOR));
		case WILD_ZOMBIE -> lessers.add(new LesserEnemy(x * Game.TILES_SIZE, y * Game.TILES_SIZE, WILD_ZOMBIE));
		case ZOMBIE_MAN -> lessers.add(new LesserEnemy(x * Game.TILES_SIZE, y * Game.TILES_SIZE, ZOMBIE_MAN));
		case ZOMBIE_WOMAN -> lessers.add(new LesserEnemy(x * Game.TILES_SIZE, y * Game.TILES_SIZE, ZOMBIE_WOMAN));
		case 100 -> playerSpawn = new Point(x * Game.TILES_SIZE, y * Game.TILES_SIZE);
		}
	}

	private void loadObjects(int blueValue, int x, int y) {
		switch (blueValue) {
		case RED_POTION, BLUE_POTION -> potions.add(new Potion(x * Game.TILES_SIZE, y * Game.TILES_SIZE, blueValue));
		case BOX, BARREL -> containers.add(new GameContainer(x * Game.TILES_SIZE, y * Game.TILES_SIZE, blueValue));
		case SPIKE -> spikes.add(new Spike(x * Game.TILES_SIZE, y * Game.TILES_SIZE, SPIKE));
		case CANNON_LEFT, CANNON_RIGHT -> cannons.add(new Cannon(x * Game.TILES_SIZE, y * Game.TILES_SIZE, blueValue));
		case TREE_ONE, TREE_TWO, TREE_THREE -> trees.add(new BackgroundTree(x * Game.TILES_SIZE, y * Game.TILES_SIZE, blueValue));
		}
	}

	private void calcLvlOffsets() {
		lvlTilesWide = img.getWidth();
		maxTilesOffset = Math.max(0, lvlTilesWide - Game.TILES_IN_WIDTH);
		maxLvlOffsetX = Game.TILES_SIZE * maxTilesOffset;
		if (playerSpawn == null)
			playerSpawn = findSafeSpawn();
	}

	private Point findSafeSpawn() {
		for (int row = 0; row < lvlData.length; row++) {
			for (int col = 0; col < lvlData[row].length; col++) {
				if (!utilz.HelpMethods.isSolid(row, col, lvlData)
						&& (row + 1 >= lvlData.length || utilz.HelpMethods.isSolid(row + 1, col, lvlData)))
					return new Point(col * Game.TILES_SIZE, Math.max(0, row * Game.TILES_SIZE - Game.TILES_SIZE));
			}
		}
		return new Point(Game.TILES_SIZE, Game.TILES_SIZE);
	}

	public int getSpriteIndex(int x, int y) {
		if (y < 0 || y >= lvlData.length || x < 0 || x >= lvlData[y].length)
			return 0;
		return lvlData[y][x];
	}

	public int getWidthInTiles() { return lvlTilesWide; }

	public int getHeightInTiles() { return lvlData.length; }

	public int[][] getLevelData() {
		return lvlData;
	}

	public int getLvlOffset() {
		return maxLvlOffsetX;
	}

	public Point getPlayerSpawn() {
		return playerSpawn;
	}

	public ArrayList<Crabby> getCrabs() {
		return crabs;
	}

	public ArrayList<LesserEnemy> getLesserEnemies() {
		return lessers;
	}

	public ArrayList<Shark> getSharks() {
		return sharks;
	}

	public ArrayList<Potion> getPotions() {
		return potions;
	}

	public ArrayList<Ammo> getAmmos() {
		return ammos;
	}

	public ArrayList<GameContainer> getContainers() {
		return containers;
	}

	public ArrayList<Spike> getSpikes() {
		return spikes;
	}

	public ArrayList<Cannon> getCannons() {
		return cannons;
	}

	public ArrayList<Pinkstar> getPinkstars() {
		return pinkstars;
	}

	public ArrayList<Boss> getBosses() { return bosses; }

	public ArrayList<BackgroundTree> getTrees() {
		return trees;
	}

	public ArrayList<Grass> getGrass() {
		return grass;
	}

	public ArrayList<CollectibleItem> getCollectibles() { return collectibles; }

	public void addCollectible(CollectibleItem collectible) {
		if (collectible != null) collectibles.add(collectible);
	}

}

package entities;

import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import gamestates.Playing;
import levels.Level;
import utilz.LoadSave;
import static utilz.Constants.EnemyConstants.*;
import entities.Boss;

public class EnemyManager {

	private Playing playing;
	private BufferedImage[][] crabbyArr, pinkstarArr, sharkArr;
	private final Map<Integer, BufferedImage[]> bossFrames = new HashMap<>();
	// capability sets per enemy type (e.g., "shoot", "dash", "dodge")
	private final Map<Integer, java.util.Set<String>> enemyCapabilities = new HashMap<>();
	private Level currentLevel;

	public EnemyManager(Playing playing) {
		this.playing = playing;
		loadEnemyImgs();
	}

	public void loadEnemies(Level level) {
		this.currentLevel = level;
	}

	public void update(int[][] lvlData) {
		boolean isAnyActive = false;
		// track last enemy death position so we can spawn fragment there if it was the last enemy
		int lastDeadX = -1, lastDeadY = -1;
		boolean spawnPending = false;

		for (Crabby c : currentLevel.getCrabs()) {
			boolean wasActive = c.isActive();
			c.update(lvlData, playing);
			if (c.isActive()) isAnyActive = true;
			if (wasActive && !c.isActive()) {
				lastDeadX = (int) c.getHitbox().x;
				lastDeadY = (int) c.getHitbox().y;
				spawnPending = true;
			}
		}

		for (Pinkstar p : currentLevel.getPinkstars()) {
			boolean wasActive = p.isActive();
			p.update(lvlData, playing);
			if (p.isActive()) isAnyActive = true;
			if (wasActive && !p.isActive()) {
				lastDeadX = (int) p.getHitbox().x;
				lastDeadY = (int) p.getHitbox().y;
				spawnPending = true;
			}
		}

		for (Shark s : currentLevel.getSharks()) {
			boolean wasActive = s.isActive();
			s.update(lvlData, playing);
			if (s.isActive()) isAnyActive = true;
			if (wasActive && !s.isActive()) {
				lastDeadX = (int) s.getHitbox().x;
				lastDeadY = (int) s.getHitbox().y;
				spawnPending = true;
			}
		}

		// lesser enemies (skeletons, zombies)
		for (entities.LesserEnemy le : currentLevel.getLesserEnemies()) {
			boolean wasActive = le.isActive();
			le.update(lvlData, playing);
			if (le.isActive()) isAnyActive = true;
			if (wasActive && !le.isActive()) {
				lastDeadX = (int) le.getHitbox().x;
				lastDeadY = (int) le.getHitbox().y;
				spawnPending = true;
			}
		}

		for (Boss b : currentLevel.getBosses()) {
			boolean wasActive = b.isActive();
			b.update(lvlData, playing);
			if (b.isActive()) isAnyActive = true;
			// handle boss death event (one-shot)
			if (wasActive && !b.isActive()) {
				System.out.println("DBG: Boss died -> type=" + b.enemyType + " at level=" + (playing.getLevelManager().getLevelIndex() + 1));
				// If boss is Gotoku and this is a fragment-boss level (levels 3,6,9,12...), grant a fragment
				int lvlIndex = playing.getLevelManager().getLevelIndex() + 1; // 1-based for checks
				if (b.enemyType == GOTOKU && lvlIndex % 3 == 0) {
					try {
						// spawn a fragment entity where the boss died
						int fx = (int) b.getHitbox().x;
						int fy = (int) b.getHitbox().y;
						playing.getObjectManager().spawnFragment(fx, fy);
					} catch (Exception ignored) {}
				}
				// record last dead pos for generic last-enemy fragment
				lastDeadX = (int) b.getHitbox().x;
				lastDeadY = (int) b.getHitbox().y;
				spawnPending = true;
			}
		}

		// If no active enemies remain, either spawn a fragment (if the last enemy just died),
		// or if a fragment is already present wait until it's collected, otherwise finish level.
		if (!isAnyActive) {
			if (playing.getObjectManager().hasActiveFragments()) {
				// waiting for fragment pickup - don't complete level yet
				return;
			}
			if (spawnPending && lastDeadX >= 0 && lastDeadY >= 0) {
				// spawn the global fragment for completing the level
				playing.getObjectManager().spawnFragment(lastDeadX, lastDeadY);
				// clear pending so we don't spawn repeatedly
				spawnPending = false;
				return;
			}
			// no fragments present and nothing pending -> level complete
			playing.setLevelCompleted(true);
		}
	}

	public void draw(Graphics g, int xLvlOffset) {
		drawCrabs(g, xLvlOffset);
		drawPinkstars(g, xLvlOffset);
		drawSharks(g, xLvlOffset);
		drawLessers(g, xLvlOffset);
		drawBosses(g, xLvlOffset);
	}

	private void drawBosses(Graphics g, int xLvlOffset) {
		for (Boss b : currentLevel.getBosses())
			if (b.isActive()) {
				BufferedImage[] frames = bossFrames.get(b.enemyType);
				BufferedImage f = b.getCurrentFrame();
				if (f == null && frames != null && frames.length > 0) f = frames[0];
				if (f != null)
					g.drawImage(f, (int) b.getHitbox().x - xLvlOffset, (int) b.getHitbox().y, b.width, b.height, null);
				// boss health bar drawn at top center if active
				if (b.isActive()) drawBossHealthBar(g, b);
			}
	}

	private void drawBossHealthBar(Graphics g, Boss b) {
		int barW = (int) (300 * main.Game.SCALE);
		int barH = (int) (18 * main.Game.SCALE);
		int x = main.Game.GAME_WIDTH / 2 - barW / 2;
		int y = (int) (20 * main.Game.SCALE);
		// background
		g.setColor(java.awt.Color.DARK_GRAY);
		g.fillRect(x, y, barW, barH);
		// health
		float pct = Math.max(0f, Math.min(1f, (float) b.currentHealth / (float) b.maxHealth));
		g.setColor(java.awt.Color.RED);
		g.fillRect(x + 2, y + 2, (int) ((barW - 4) * pct), barH - 4);
		// border
		g.setColor(java.awt.Color.BLACK);
		g.drawRect(x, y, barW, barH);
	}

	private void drawSharks(Graphics g, int xLvlOffset) {
		for (Shark s : currentLevel.getSharks())
			if (s.isActive()) {
				g.drawImage(sharkArr[s.getState()][s.getAniIndex()], (int) s.getHitbox().x - xLvlOffset - SHARK_DRAWOFFSET_X + s.flipX(),
						(int) s.getHitbox().y - SHARK_DRAWOFFSET_Y + (int) s.getPushDrawOffset(), SHARK_WIDTH * s.flipW(), SHARK_HEIGHT, null);
//				s.drawHitbox(g, xLvlOffset);
//				s.drawAttackBox(g, xLvlOffset);
			}
	}

	private void drawPinkstars(Graphics g, int xLvlOffset) {
		for (Pinkstar p : currentLevel.getPinkstars())
			if (p.isActive()) {
				g.drawImage(pinkstarArr[p.getState()][p.getAniIndex()], (int) p.getHitbox().x - xLvlOffset - PINKSTAR_DRAWOFFSET_X + p.flipX(),
						(int) p.getHitbox().y - PINKSTAR_DRAWOFFSET_Y + (int) p.getPushDrawOffset(), PINKSTAR_WIDTH * p.flipW(), PINKSTAR_HEIGHT, null);
//				p.drawHitbox(g, xLvlOffset);
			}
	}

	private void drawCrabs(Graphics g, int xLvlOffset) {
		for (Crabby c : currentLevel.getCrabs())
			if (c.isActive()) {

				g.drawImage(crabbyArr[c.getState()][c.getAniIndex()], (int) c.getHitbox().x - xLvlOffset - CRABBY_DRAWOFFSET_X + c.flipX(),
						(int) c.getHitbox().y - CRABBY_DRAWOFFSET_Y + (int) c.getPushDrawOffset(), CRABBY_WIDTH * c.flipW(), CRABBY_HEIGHT, null);

//				c.drawHitbox(g, xLvlOffset);
//				c.drawAttackBox(g, xLvlOffset);
			}

	}

	public void checkEnemyHit(Rectangle2D.Float attackBox) {
		for (Crabby c : currentLevel.getCrabs())
			if (c.isActive())
				if (c.getState() != DEAD && c.getState() != HIT)
					if (attackBox.intersects(c.getHitbox())) {
						c.hurt(20);
						return;
					}

		for (Pinkstar p : currentLevel.getPinkstars())
			if (p.isActive()) {
				if (p.getState() == ATTACK && p.getAniIndex() >= 3)
					return;
				else {
					if (p.getState() != DEAD && p.getState() != HIT)
						if (attackBox.intersects(p.getHitbox())) {
							p.hurt(20);
							return;
						}
				}
			}

		for (Shark s : currentLevel.getSharks())
			if (s.isActive()) {
				if (s.getState() != DEAD && s.getState() != HIT)
					if (attackBox.intersects(s.getHitbox())) {
						s.hurt(20);
						return;
					}
			}

		for (Boss b : currentLevel.getBosses())
			if (b.isActive()) {
				if (attackBox.intersects(b.getHitbox())) {
					b.hurt(20);
					return;
				}
			}
	}

	private void loadEnemyImgs() {
		crabbyArr = getImgArr(LoadSave.GetSpriteAtlas(LoadSave.CRABBY_SPRITE), 9, 5, CRABBY_WIDTH_DEFAULT, CRABBY_HEIGHT_DEFAULT);
		pinkstarArr = getImgArr(LoadSave.GetSpriteAtlas(LoadSave.PINKSTAR_ATLAS), 8, 5, PINKSTAR_WIDTH_DEFAULT, PINKSTAR_HEIGHT_DEFAULT);
		sharkArr = getImgArr(LoadSave.GetSpriteAtlas(LoadSave.SHARK_ATLAS), 8, 5, SHARK_WIDTH_DEFAULT, SHARK_HEIGHT_DEFAULT);
		// load boss frames from folders if available
		BufferedImage[] gotoku = LoadSave.GetSpritesFromFolder("bosses/Gotoku");
		if (gotoku != null && gotoku.length > 0) bossFrames.put(GOTOKU, gotoku);
		BufferedImage[] med = LoadSave.GetSpritesFromFolder("bosses/medussa");
		if (med != null && med.length > 0) bossFrames.put(MEDUSSA, med);
		BufferedImage[] onre = LoadSave.GetSpritesFromFolder("bosses/Onre");
		if (onre != null && onre.length > 0) bossFrames.put(ONRE, onre);
		BufferedImage[] yurei = LoadSave.GetSpritesFromFolder("bosses/Yurei");
		if (yurei != null && yurei.length > 0) bossFrames.put(YUREI, yurei);
		// load lesser enemies from /res/lesser enemies
		BufferedImage[] sa = LoadSave.GetSpritesFromFolder("lesser enemies/Skeleton_Archer");
		if (sa != null && sa.length > 0) bossFrames.put(SKELETON_ARCHER, sa);
		// detect capabilities by sprite filenames
		try {
			java.util.Map<String, BufferedImage> map = LoadSave.GetSpritesMapFromFolder("lesser enemies/Skeleton_Archer");
			java.util.Set<String> caps = detectCapsFromMap(map);
			enemyCapabilities.put(SKELETON_ARCHER, caps);
		} catch (Exception ignored) {}
		BufferedImage[] ss = LoadSave.GetSpritesFromFolder("lesser enemies/Skeleton_Spearman");
		if (ss != null && ss.length > 0) bossFrames.put(SKELETON_SPEARMAN, ss);
		try {
			java.util.Map<String, BufferedImage> map = LoadSave.GetSpritesMapFromFolder("lesser enemies/Skeleton_Spearman");
			java.util.Set<String> caps = detectCapsFromMap(map);
			enemyCapabilities.put(SKELETON_SPEARMAN, caps);
		} catch (Exception ignored) {}
		BufferedImage[] sw = LoadSave.GetSpritesFromFolder("lesser enemies/Skeleton_Warrior");
		if (sw != null && sw.length > 0) bossFrames.put(SKELETON_WARRIOR, sw);
		try {
			java.util.Map<String, BufferedImage> map = LoadSave.GetSpritesMapFromFolder("lesser enemies/Skeleton_Warrior");
			java.util.Set<String> caps = detectCapsFromMap(map);
			enemyCapabilities.put(SKELETON_WARRIOR, caps);
		} catch (Exception ignored) {}
		BufferedImage[] wz = LoadSave.GetSpritesFromFolder("lesser enemies/Wild Zombie");
		if (wz != null && wz.length > 0) bossFrames.put(WILD_ZOMBIE, wz);
		try {
			java.util.Map<String, BufferedImage> map = LoadSave.GetSpritesMapFromFolder("lesser enemies/Wild Zombie");
			java.util.Set<String> caps = detectCapsFromMap(map);
			enemyCapabilities.put(WILD_ZOMBIE, caps);
		} catch (Exception ignored) {}
		BufferedImage[] zm = LoadSave.GetSpritesFromFolder("lesser enemies/Zombie Man");
		if (zm != null && zm.length > 0) bossFrames.put(ZOMBIE_MAN, zm);
		try {
			java.util.Map<String, BufferedImage> map = LoadSave.GetSpritesMapFromFolder("lesser enemies/Zombie Man");
			java.util.Set<String> caps = detectCapsFromMap(map);
			enemyCapabilities.put(ZOMBIE_MAN, caps);
		} catch (Exception ignored) {}
		BufferedImage[] zw = LoadSave.GetSpritesFromFolder("lesser enemies/Zombie Woman");
		if (zw != null && zw.length > 0) bossFrames.put(ZOMBIE_WOMAN, zw);
		try {
			java.util.Map<String, BufferedImage> map = LoadSave.GetSpritesMapFromFolder("lesser enemies/Zombie Woman");
			java.util.Set<String> caps = detectCapsFromMap(map);
			enemyCapabilities.put(ZOMBIE_WOMAN, caps);
		} catch (Exception ignored) {}
	}

	// utility: detect capabilities by looking at filenames in the sprite folder
	private java.util.Set<String> detectCapsFromMap(java.util.Map<String, BufferedImage> map) {
		java.util.Set<String> caps = new java.util.HashSet<>();
		if (map == null || map.isEmpty()) return caps;
		for (String name : map.keySet()) {
			String n = name.toLowerCase();
			if (n.contains("shoot") || n.contains("arrow") || n.contains("bow") || n.contains("bullet")) caps.add("shoot");
			if (n.contains("dash") || n.contains("dodge") || n.contains("roll")) caps.add("dash");
			if (n.contains("spear") || n.contains("stab") || n.contains("thrust")) caps.add("spear");
			if (n.contains("jump")) caps.add("jump");
			if (n.contains("heal") || n.contains("regen")) caps.add("heal");
		}
		return caps;
	}

	public java.util.Set<String> getCapabilitiesForType(int enemyType) {
		return enemyCapabilities.getOrDefault(enemyType, java.util.Collections.emptySet());
	}

	private BufferedImage[][] getImgArr(BufferedImage atlas, int xSize, int ySize, int spriteW, int spriteH) {
		BufferedImage[][] tempArr = new BufferedImage[ySize][xSize];
		for (int j = 0; j < tempArr.length; j++)
			for (int i = 0; i < tempArr[j].length; i++)
				tempArr[j][i] = atlas.getSubimage(i * spriteW, j * spriteH, spriteW, spriteH);
		return tempArr;
	}

	public void resetAllEnemies() {
		for (Crabby c : currentLevel.getCrabs())
			c.resetEnemy();
		for (Pinkstar p : currentLevel.getPinkstars())
			p.resetEnemy();
		for (Shark s : currentLevel.getSharks())
			s.resetEnemy();
		for (Boss b : currentLevel.getBosses())
			b.resetEnemy();
		for (entities.LesserEnemy le : currentLevel.getLesserEnemies())
			le.resetEnemy();
	}

	private void drawLessers(Graphics g, int xLvlOffset) {
		for (entities.LesserEnemy le : currentLevel.getLesserEnemies())
			if (le.isActive()) {
				BufferedImage[] frames = bossFrames.get(le.enemyType);
				if (frames != null && frames.length > 0) {
					int fi = le.getAniIndex() % frames.length;
					g.drawImage(frames[fi], (int) le.getHitbox().x - xLvlOffset, (int) le.getHitbox().y, le.width, le.height, null);
				}
			}
	}

	// Return number of frames available for an enemy type (used by LesserEnemy animation)
	public int getSpriteCount(int enemyType) {
		BufferedImage[] arr = bossFrames.get(enemyType);
		return arr == null ? 1 : arr.length;
	}

}

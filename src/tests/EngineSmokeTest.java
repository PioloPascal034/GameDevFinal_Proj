package tests;

import java.awt.geom.Rectangle2D;
import engine.Camera;
import engine.Inventory;
import engine.Item;
import entities.Crabby;
import entities.Enemy;
import main.Game;
import static utilz.Constants.EnemyConstants.CRABBY;
import utilz.HelpMethods;

/** Dependency-free regression checks runnable with `java tests.EngineSmokeTest`. */
public final class EngineSmokeTest {
    public static void main(String[] args) {
        collisionQueriesAreSafeAtEdges();
        cameraClampsAndAccountsForZoom();
        inventoryStacksAndRejectsOverflow();
        enemyCombatAndTerrainRegression();
        System.out.println("EngineSmokeTest: PASS");
    }

    private static void collisionQueriesAreSafeAtEdges() {
        int[][] map = {
                {1, 1, 1},
                {1, 11, 1},
                {1, 1, 1}
        };
        if (!HelpMethods.isSolid(-1, 1, map) || !HelpMethods.isSolid(1, 99, map))
            throw new AssertionError("out-of-bounds tiles must be solid");
        if (!HelpMethods.CanMoveHere(64.1f, 64.1f, 20f, 20f, map))
            throw new AssertionError("open tile should be traversable");
        if (HelpMethods.CanMoveHere(0f, 32f, 20f, 20f, map))
            throw new AssertionError("solid border should block movement");
    }

    private static void cameraClampsAndAccountsForZoom() {
        Camera camera = new Camera();
        Rectangle2D.Float target = new Rectangle2D.Float(1900, 900, 32, 48);
        camera.setZoom(1.5f);
        camera.follow(target, 800, 600, 2400, 1500);
        if (camera.getX() < 0 || camera.getY() < 0 || camera.getX() > 1866.67f || camera.getY() > 1100.01f)
            throw new AssertionError("camera did not clamp to zoomed world bounds");
        camera.follow(new Rectangle2D.Float(0, 0, 32, 48), 800, 600, 500, 400);
        if (camera.getX() != 0 || camera.getY() != 0)
            throw new AssertionError("small worlds should remain clamped at origin");
    }

    private static void inventoryStacksAndRejectsOverflow() {
        Item shard = new Item("test_shard", "Test Shard", "", Item.Category.QUEST, 10, null);
        Inventory inventory = new Inventory(2);
        if (inventory.add(shard, 7) != 7 || inventory.add(shard, 8) != 8)
            throw new AssertionError("identical items should stack across available slots");
        if (inventory.count("test_shard") != 15 || inventory.remove("test_shard", 99) != 15)
            throw new AssertionError("remove/count should be bounded");
        if (inventory.add(shard, 20) != 20)
            throw new AssertionError("empty slots should accept items safely");
        if (inventory.add(new Item("other", "Other", "", Item.Category.KEY, 1, null), 1) != 0)
            throw new AssertionError("full inventory must reject additional items");
        for (int i = 0; i < inventory.size(); i++)
            if (inventory.getSlot(i).getQuantity() < 0) throw new AssertionError("negative quantity");
    }

    private static void enemyCombatAndTerrainRegression() {
        Crabby crab = new Crabby(128, 160);
        crab.hurt(20);
        if (crab.getCurrentHealth() != crab.getMaxHealth() - 20)
            throw new AssertionError("enemy damage should reduce health");
        crab.hurt(999);
        if (crab.getCurrentHealth() != 0 || crab.getState() != utilz.Constants.EnemyConstants.DEAD)
            throw new AssertionError("enemy should enter the death state at zero health");

        ProbeEnemy probe = new ProbeEnemy(256, 160);
        int[][] map = new int[5][8];
        for (int row = 0; row < map.length; row++)
            for (int col = 0; col < map[row].length; col++)
                map[row][col] = row >= 3 || col == 1 ? 1 : 11;
        probe.step(map);
        float startX = probe.getHitbox().x;
        probe.step(map);
        if (probe.getHitbox().x >= startX || probe.getHitbox().y != 160)
            throw new AssertionError("enemy should patrol on solid terrain");
        for (int i = 0; i < 100; i++) probe.step(map);
        if (probe.getHitbox().x < Game.TILES_SIZE * 2 - 0.1f)
            throw new AssertionError("enemy should not pass through a wall");
    }

    private static final class ProbeEnemy extends Enemy {
        ProbeEnemy(float x, float y) {
            super(x, y, 32, 32, CRABBY);
            initHitbox(16, 16);
            initAttackBox(20, 20, 10);
        }

        void step(int[][] map) {
            if (firstUpdate) firstUpdateCheck(map);
            if (inAir) updateInAir(map);
            else move(map);
        }
    }
}

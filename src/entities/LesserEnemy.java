package entities;

import static utilz.Constants.EnemyConstants.*;
import static utilz.HelpMethods.IsFloor;
import static utilz.HelpMethods.CanMoveHere;
import static utilz.Constants.Dialogue.*;

import gamestates.Playing;
import main.Game;

public class LesserEnemy extends Enemy {

    // Capabilities derived from sprite folder names
    private boolean canShoot = false;
    private boolean canDash = false;
    private boolean canSpear = false;
    private int shootCooldown = 0; // ms

    public LesserEnemy(float x, float y, int enemyType) {
        super(x, y, CRABBY_WIDTH, CRABBY_HEIGHT, enemyType);
        initHitbox(22, 19);
        initAttackBox(82, 19, 30);
    }

    public void update(int[][] lvlData, Playing playing) {
        if (firstUpdate) {
            // detect capabilities once from EnemyManager
            try {
                java.util.Set<String> caps = playing.getEnemyManager().getCapabilitiesForType(enemyType);
                if (caps.contains("shoot")) canShoot = true;
                if (caps.contains("dash")) canDash = true;
                if (caps.contains("spear")) canSpear = true;
            } catch (Exception ignored) {}
            firstUpdateCheck(lvlData);
        }

        if (inAir) {
            inAirChecks(lvlData, playing);
        } else {
            switch (state) {
            case IDLE:
                if (IsFloor(hitbox, lvlData))
                    newState(RUNNING);
                else
                    inAir = true;
                break;
            case RUNNING:
                if (canSeePlayer(lvlData, playing.getPlayer())) {
                    turnTowardsPlayer(playing.getPlayer());
                    // If archer and player is mid/long range, attack by shooting
                    if (canShoot) {
                        int abs = (int) Math.abs(playing.getPlayer().getHitbox().x - hitbox.x);
                        if (abs > Game.TILES_SIZE && abs <= Game.TILES_SIZE * 8) {
                            newState(ATTACK);
                        } else if (isPlayerCloseForAttack(playing.getPlayer())) {
                            newState(ATTACK);
                        }
                    } else {
                        if (isPlayerCloseForAttack(playing.getPlayer()))
                            newState(ATTACK);
                    }
                }
                move(lvlData);
                if (inAir)
                    playing.addDialogue((int) hitbox.x, (int) hitbox.y, EXCLAMATION);
                break;
            case ATTACK:
                if (aniIndex == 0)
                    attackChecked = false;
                // For shooters: spawn projectile at a concrete frame
                if (canShoot) {
                    if (aniIndex == 3 && !attackChecked && shootCooldown <= 0) {
                        int dir = (playing.getPlayer().getHitbox().x > hitbox.x) ? 1 : -1;
                        playing.getObjectManager().addEnemyProjectile((int) hitbox.x, (int) hitbox.y + (int)(10*Game.SCALE), dir);
                        attackChecked = true;
                        shootCooldown = 1200; // 1.2s cooldown
                    }
                } else {
                    if (aniIndex == 3 && !attackChecked)
                        checkPlayerHit(attackBox, playing.getPlayer());
                }
                // If close and canDash, perform a simple evasive dash away from player
                if (canDash) {
                    int abs = (int) Math.abs(playing.getPlayer().getHitbox().x - hitbox.x);
                    if (abs < Game.TILES_SIZE && !dodgeActive && dodgeTimer <= 0) {
                        int dir = (playing.getPlayer().getHitbox().x > hitbox.x) ? -1 : 1;
                        int step = dir * (int)(60 * Game.SCALE);
                        if (CanMoveHere(hitbox.x + step, hitbox.y, hitbox.width, hitbox.height, lvlData)) {
                            hitbox.x += step;
                            dodgeActive = true;
                            dodgeTimer = 400; // 400 ms dodge duration
                        }
                    }
                }
                break;
            case HIT:
                if (aniIndex <= GetSpriteAmount(enemyType, state) - 2)
                    pushBack(pushBackDir, lvlData, 2f);
                updatePushBackDrawOffset();
                break;
            }

            // custom animation tick: use EnemyManager's sprite count if available
            aniTick++;
            int spriteCount = 1;
            try {
                spriteCount = playing.getEnemyManager().getSpriteCount(enemyType);
            } catch (Exception ignored) {}
            if (aniTick >= utilz.Constants.ANI_SPEED) {
                aniTick = 0;
                aniIndex++;
                if (spriteCount > 0 && aniIndex >= spriteCount) {
                    aniIndex = 0;
                    if (state == HIT)
                        state = IDLE;
                }
            }

            updateAttackBox();

            // tick down dodge/shoot cooldown (approximate ms per tick)
            if (dodgeActive) {
                dodgeTimer -= 16;
                if (dodgeTimer <= 0) dodgeActive = false;
            }
            if (shootCooldown > 0) shootCooldown = Math.max(0, shootCooldown - 16);
        }
    }
}

package entities;

import audio.AudioPlayer;
import gamestates.Playing;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import main.Game;
import static utilz.Constants.*;
import static utilz.Constants.Directions.*;
import static utilz.Constants.PlayerConstants.*;
import static utilz.HelpMethods.*;
import utilz.LoadSave;

public class Player extends Entity {
    // Draw offsets for sprite alignment
    private int xDrawOffset = 0;
    private int yDrawOffset = 0;

    private BufferedImage[][] animations;
    private boolean moving = false, attacking = false;
    private boolean left, right, jump;
    private int[][] lvlData;

    // Jumping / Gravity
    private float jumpSpeed = -2.25f * Game.SCALE;
    private float fallSpeedAfterCollision = 0.5f * Game.SCALE;

    // StatusBarUI
    private BufferedImage statusBarImg;
    // debug last shot position (world coords)
    private int lastShotX = -1;
    private int lastShotY = -1;

    private int statusBarWidth = (int) (192 * Game.SCALE);
    private int statusBarHeight = (int) (58 * Game.SCALE);
    private int statusBarX = (int) (10 * Game.SCALE);
    private int statusBarY = (int) (10 * Game.SCALE);

    private int healthBarWidth = (int) (150 * Game.SCALE);
    private int healthBarHeight = (int) (4 * Game.SCALE);
    private int healthBarXStart = (int) (34 * Game.SCALE);
    private int healthBarYStart = (int) (14 * Game.SCALE);
    private int healthWidth = healthBarWidth;

    private int powerBarWidth = (int) (104 * Game.SCALE);
    private int powerBarHeight = (int) (2 * Game.SCALE);
    private int powerBarXStart = (int) (44 * Game.SCALE);
    private int powerBarYStart = (int) (34 * Game.SCALE);
    private int powerWidth = powerBarWidth;
    private int powerMaxValue = 200;
    private int powerValue = powerMaxValue;

    // Ammo for ranged attacks
    private int ammo = 0;
    private int ammoMax = 10;
    // Whether player has been granted the gun ability
    private boolean gunUnlocked = false;
    // Collected fragments toward unlocking the gun
    private int fragmentCount = 0;
    // Whether the player unlocked the immortality ability (from fragment #6)
    private boolean immortalityUnlocked = false;
    // Whether the immortality ability is currently active (toggled by player using 'G')
    private boolean immortalityActive = false;
    // Additional ability unlocks
    private boolean dashUnlocked = false;
    private boolean doubleJumpUnlocked = false;
    private boolean untargetableUnlocked = false;
    private boolean blockUnlocked = false;
    // active states / timers
    private boolean blockActive = false;
    private boolean untargetableActive = false;
    private int untargetableTimer = 0; // ms
    // Cooldowns (ms)
    private final int qCooldownMs = 1000; // 1s (adjustable)
    private final int rCooldownMs = 5000; // 5s (adjustable)
    private int qCooldownTimer = 0; // time remaining (ms)
    private int rCooldownTimer = 0; // time remaining (ms)

    // Running and double-jump
    private boolean running = false;
    // start without extra jumps; double-jump is unlocked via fragment #2
    private int maxExtraJumps = 0; // no extra jumps by default
    private int extraJumps = maxExtraJumps;

    // flipX used for anchor calculation if needed; flipW used for drawing scale (-1 or 1)
    private int flipX = 0;
    private int flipW = 1;

    // stable facing direction: 1 = right, -1 = left
    private int facingDir = 1;

    private boolean attackChecked;
    private Playing playing;

    // Dash state
    private boolean isDashing = false;
    private int dashFramesRemaining = 0;
    // Dash cooldown (ms)
    private final int dashCooldownMs = 1000; // 1s cooldown
    private int dashCooldownTimer = 0; // ms remaining

    private int tileY = 0;

    private boolean powerAttackActive;
    private int powerAttackTick;
    private int powerGrowSpeed = 15;
    private int powerGrowTick;

    private final PlayerCharacter playerCharacter;
    // short invulnerability after reset to avoid immediate re-death
    private int invulnerableFrames = 0;

    public Player(PlayerCharacter playerCharacter, Playing playing) {
        super(0, 0, (int) (playerCharacter.spriteW * Game.SCALE), (int) (playerCharacter.spriteH * Game.SCALE));
        this.playerCharacter = playerCharacter;
        this.playing = playing;
        this.state = IDLE;
        this.maxHealth = 100;
        this.currentHealth = maxHealth;
        this.walkSpeed = Game.SCALE * 1.0f;
        // Try loading custom player sprites. Prefer singular folder `player_sprite` then legacy `player_sprites/P_update`.
        BufferedImage[][] custom = LoadSave.loadPlayerSpritesFromFolder("player_sprite");
        if (custom == null)
            custom = LoadSave.loadPlayerSpritesFromFolder("player_sprites/P_update");
        if (custom != null)
            animations = custom;
        else
            animations = LoadSave.loadTestAnimations();
        // no gun overlay: we only track whether player has the gun ability
        // initialize hitbox to character defaults
        initHitbox(playerCharacter.hitboxW, playerCharacter.hitboxH);
        statusBarImg = LoadSave.GetSpriteAtlas(LoadSave.STATUS_BAR);
        initAttackBox();
        // start with 5 ammo at spawn
        this.ammo = 5;
    }

    public void setSpawn(Point spawn) {
        this.x = spawn.x;
        this.y = spawn.y;
        hitbox.x = x;
        hitbox.y = y;
        syncXYWithHitbox();
    }

    public PlayerCharacter getPlayerCharacter() {
        return playerCharacter;
    }

    private void initAttackBox() {
        attackBox = new Rectangle2D.Float(x, y, (int) (35 * Game.SCALE), (int) (20 * Game.SCALE));
        resetAttackBox();
    }

    /**
     * Keep entity.x/y and hitbox.x/y in sync to avoid rendering vs collision drift.
     */
    private void syncXYWithHitbox() {
        this.x = (int) hitbox.x;
        this.y = (int) hitbox.y;
    }

    public void update() {
        // decrement invulnerability timer if active
        if (invulnerableFrames > 0) {
            invulnerableFrames--;
            if (invulnerableFrames == 0) {
                // dash ended if it was active
                isDashing = false;
            }
        }

        // decrement untargetable timer
        if (untargetableTimer > 0) {
            untargetableTimer -= 16;
            if (untargetableTimer <= 0) {
                untargetableTimer = 0;
                untargetableActive = false;
            }
        }

        // cooldown timers (simple tick-based ms subtraction)
        if (qCooldownTimer > 0) qCooldownTimer = Math.max(0, qCooldownTimer - 16);
        if (rCooldownTimer > 0) rCooldownTimer = Math.max(0, rCooldownTimer - 16);
        if (dashCooldownTimer > 0) dashCooldownTimer = Math.max(0, dashCooldownTimer - 16);

        updateHealthBar();
        updatePowerBar();

        if (currentHealth <= 0) {
            if (state != DEAD) {
                state = DEAD;
                aniTick = 0;
                aniIndex = 0;
                playing.setPlayerDying(true);
                playing.getGame().getAudioPlayer().playEffect(AudioPlayer.DIE);
                playing.setGameOver(true);
                // keep background music playing but lower it for the Game Over state (fade)
                playing.getGame().getAudioPlayer().fadeVolumeTo(0.15f, 400, true);
                playing.getGame().getAudioPlayer().playEffect(AudioPlayer.GAMEOVER);

                // Check if player died in air
                if (!IsEntityOnFloor(hitbox, lvlData)) {
                    inAir = true;
                    airSpeed = 0;
                }
            } else {
                updateAnimationTick();

                // Fall if in air
                if (inAir) {
                    if (CanMoveHere(hitbox.x, hitbox.y + airSpeed, hitbox.width, hitbox.height, lvlData)) {
                        hitbox.y += airSpeed;
                        airSpeed += GRAVITY;
                        syncXYWithHitbox();
                    } else {
                        inAir = false;
                    }
                }
            }

            return;
        }

        updateAttackBox();

        // Dash movement handling: move in small steps over multiple frames
        if (isDashing) {
            int step = facingDir * (int) (8 * Game.SCALE);
            if (CanMoveHere(hitbox.x + step, hitbox.y, hitbox.width, hitbox.height, lvlData)) {
                hitbox.x += step;
            } else {
                // stop dash early if blocked
                isDashing = false;
                dashFramesRemaining = 0;
            }
            dashFramesRemaining--;
            if (dashFramesRemaining <= 0)
                isDashing = false;

            // advance animation quickly during dash for a smoother visual
            updateAnimationTick();

            syncXYWithHitbox();
            // update grounded state after moving so gravity applies if over edge
            if (!IsEntityOnFloor(hitbox, lvlData))
                inAir = true;

            // treat dash as movement for other systems
            moving = true;
        }

        if (state == HIT) {
            if (aniIndex <= playerCharacter.getSpriteAmount(state) - 3)
                pushBack(pushBackDir, lvlData, 1.25f);
            updatePushBackDrawOffset();
        } else
            updatePos();

        if (moving) {
            checkPotionTouched();
            checkSpikesTouched();
            checkInsideWater();
            tileY = (int) (hitbox.y / Game.TILES_SIZE);
            if (powerAttackActive) {
                powerAttackTick++;
                if (powerAttackTick >= 35) {
                    powerAttackTick = 0;
                    powerAttackActive = false;
                }
            }
        }

        if (attacking || powerAttackActive)
            checkAttack();

        updateAnimationTick();
        setAnimation();
        // keep animating push/bounce draw offset until it returns to 0
        if (pushDrawOffset != 0)
            updatePushBackDrawOffset();

        // no gun visual overlay; shooting is gated by `gunUnlocked`
    }

    private void checkInsideWater() {
        if (IsEntityInWater(hitbox, playing.getLevelManager().getCurrentLevel().getLevelData())) {
            if (!isInvulnerable())
                kill();
        }
    }

    private void checkSpikesTouched() {
        playing.checkSpikesTouched(this);
    }

    private void checkPotionTouched() {
        playing.checkPotionTouched(hitbox);
    }

    private void checkAttack() {
        if (attackChecked || aniIndex != 1)
            return;
        attackChecked = true;

        if (powerAttackActive)
            attackChecked = false;

        playing.checkEnemyHit(attackBox);
        playing.checkObjectHit(attackBox);
        playing.getGame().getAudioPlayer().playAttackSound();
    }

    private void setAttackBoxOnRightSide() {
        attackBox.x = hitbox.x + hitbox.width - (int) (Game.SCALE * 5);
    }

    private void setAttackBoxOnLeftSide() {
        attackBox.x = hitbox.x - hitbox.width - (int) (Game.SCALE * 10);
    }

    private void updateAttackBox() {
        if (right && left) {
            if (flipW == 1) {
                setAttackBoxOnRightSide();
            } else {
                setAttackBoxOnLeftSide();
            }

        } else if (right || (powerAttackActive && flipW == 1))
            setAttackBoxOnRightSide();
        else if (left || (powerAttackActive && flipW == -1))
            setAttackBoxOnLeftSide();

        attackBox.y = hitbox.y + (Game.SCALE * 10);
    }

    private void updateHealthBar() {
        healthWidth = (int) ((currentHealth / (float) maxHealth) * healthBarWidth);
    }

    private void updatePowerBar() {
        powerWidth = (int) ((powerValue / (float) powerMaxValue) * powerBarWidth);
        powerGrowTick++;
        if (powerGrowTick >= powerGrowSpeed) {
            powerGrowTick = 0;
            changePower(1);
        }
    }

    public void render(Graphics g, int lvlOffset) {
        // ensure flip values reflect facingDir
        flipW = facingDir;
        flipX = (facingDir == 1 ? 0 : width);

        // Compute draw coordinates explicitly and mirror anchor when flipped
        int anchorX = playerCharacter.xDrawOffset;
        int drawX = (int) (hitbox.x - anchorX) - lvlOffset;
        if (facingDir == -1)
            drawX += width;
        int drawY = (int) (hitbox.y - playerCharacter.yDrawOffset + pushDrawOffset);

        BufferedImage frame = animations[playerCharacter.getRowIndex(state)][aniIndex];
        // If block or immortality is active (Q hold or immortality hold), draw inverted colors
        if (blockActive || immortalityActive) {
            try {
                BufferedImage copy = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = copy.createGraphics();
                g2d.drawImage(frame, 0, 0, null);
                g2d.dispose();
                float[] scales = { -1f, -1f, -1f, 1f };
                float[] offsets = { 255f, 255f, 255f, 0f };
                RescaleOp rop = new RescaleOp(scales, offsets, null);
                BufferedImage inverted = rop.filter(copy, null);
                g.drawImage(inverted, drawX, drawY, width * flipW, height, null);
            } catch (Exception e) {
                // fallback to normal draw if inversion fails
                g.drawImage(frame, drawX, drawY, width * flipW, height, null);
            }
        } else {
            g.drawImage(frame, drawX, drawY, width * flipW, height, null);
        }

        // Draw gun overlay (if provided) anchored roughly to the player's hand
        // no gun overlay rendering (we'll indicate gun state elsewhere in UI)

        drawHitbox(g, lvlOffset);
//        drawAttackBox(g, lvlOffset);
        drawUI(g);

        // Debug: draw last shot marker in world coords (visible where bullet spawns)
        if (lastShotX >= 0 && lastShotY >= 0) {
            int mx = lastShotX - lvlOffset;
            int my = lastShotY;
            g.setColor(java.awt.Color.YELLOW);
            g.fillOval(mx - 3, my - 3, 6, 6);
        }
    }

    private void drawUI(Graphics g) {
        // Background ui
        g.drawImage(statusBarImg, statusBarX, statusBarY, statusBarWidth, statusBarHeight, null);

        // Health bar
        g.setColor(Color.red);
        g.fillRect(healthBarXStart + statusBarX, healthBarYStart + statusBarY, healthWidth, healthBarHeight);

        // Power Bar
        g.setColor(Color.yellow);
        g.fillRect(powerBarXStart + statusBarX, powerBarYStart + statusBarY, powerWidth, powerBarHeight);
        // Ammo count
        g.setColor(Color.WHITE);
        g.drawString("Ammo: " + ammo, statusBarX + 8, statusBarY + statusBarHeight + 16);
    }

    private void updateAnimationTick() {
        aniTick++;
        if (aniTick >= ANI_SPEED) {
            aniTick = 0;
            aniIndex++;
            int currentRow = playerCharacter.getRowIndex(state);
            int frameCount = (animations[currentRow] != null) ? animations[currentRow].length : 1;
            if (aniIndex >= frameCount) {
                aniIndex = 0;
                attacking = false;
                attackChecked = false;
                if (state == HIT) {
                    newState(IDLE);
                    airSpeed = 0f;
                    if (!IsFloor(hitbox, 0, lvlData))
                        inAir = true;
                }
            }
        }
    }

    private void setAnimation() {
        int startAni = state;

        if (state == HIT)
            return;

        if (moving)
            state = RUNNING;
        else
            state = IDLE;

        if (inAir) {
            if (airSpeed < 0)
                state = JUMP;
            else
                state = FALLING;
        }

        if (powerAttackActive) {
            state = ATTACK;
            aniIndex = 1;
            aniTick = 0;
            return;
        }

        if (attacking) {
            state = ATTACK;
            if (startAni != ATTACK) {
                aniIndex = 1;
                aniTick = 0;
                return;
            }
        }
        if (startAni != state)
            resetAniTick();
    }

    private void resetAniTick() {
        aniTick = 0;
        aniIndex = 0;
    }

    private void updatePos() {
        moving = false;

        if (jump)
            jump();

        if (!inAir)
            if (!powerAttackActive)
                if ((!left && !right) || (right && left))
                    return;

        float xSpeed = 0;
        // Apply run multiplier when running (shift held)
        float baseSpeed = walkSpeed;
        if (running && !powerAttackActive)
            baseSpeed *= 1.8f; // running speed multiplier

        // Only change facingDir when there's clear horizontal input (left XOR right)
        if (left && !right) {
            xSpeed -= baseSpeed;
            facingDir = -1;
        } else if (right && !left) {
            xSpeed += baseSpeed;
            facingDir = 1;
        }

        // Power attack behavior: keep moving forward in facingDir if no input
        if (powerAttackActive) {
            if ((!left && !right) || (left && right)) {
                // move in facingDir when no horizontal input
                xSpeed = facingDir * walkSpeed;
            }
            xSpeed *= 3;
        }

        // update flip variables consistently (used by attack box logic elsewhere)
        flipW = facingDir;
        flipX = (facingDir == 1 ? 0 : width);

        if (!inAir)
            if (!IsEntityOnFloor(hitbox, lvlData))
                inAir = true;

        if (inAir && !powerAttackActive) {
            if (CanMoveHere(hitbox.x, hitbox.y + airSpeed, hitbox.width, hitbox.height, lvlData)) {
                hitbox.y += airSpeed;
                airSpeed += GRAVITY;
                // ensure x/y synced after vertical movement
                syncXYWithHitbox();
                updateXPos(xSpeed);
            } else {
                hitbox.y = GetEntityYPosUnderRoofOrAboveFloor(hitbox, airSpeed);
                if (airSpeed > 0)
                    resetInAir();
                else
                    airSpeed = fallSpeedAfterCollision;
                // ensure x/y synced after vertical correction
                syncXYWithHitbox();
                updateXPos(xSpeed);
            }

        } else
            updateXPos(xSpeed);
        moving = true;
    }

    private void jump() {
        // Primary jump when on ground, or extra jump if available while in air
        if (!inAir) {
            playing.getGame().getAudioPlayer().playEffect(AudioPlayer.JUMP);
            inAir = true;
            airSpeed = jumpSpeed;
            // when leaving ground, extra jumps remain available
            extraJumps = maxExtraJumps;
            // consume the jump input so holding doesn't trigger extra immediate jump
            jump = false;
            return;
        }
        // Double-jump
        if (inAir && extraJumps > 0) {
            playing.getGame().getAudioPlayer().playEffect(AudioPlayer.JUMP);
            airSpeed = jumpSpeed;
            extraJumps--;
            // consume input so holding doesn't repeat
            jump = false;
        }
    }

    private void resetInAir() {
        inAir = false;
        airSpeed = 0;
        // restore extra jumps on landing
        extraJumps = maxExtraJumps;
    }

    private void updateXPos(float xSpeed) {
        if (CanMoveHere(hitbox.x + xSpeed, hitbox.y, hitbox.width, hitbox.height, lvlData))
            hitbox.x += xSpeed;
        else {
            hitbox.x = GetEntityXPosNextToWall(hitbox, xSpeed);
            if (powerAttackActive) {
                powerAttackActive = false;
                powerAttackTick = 0;
            }
        }
        // keep canonical coordinates in sync
        syncXYWithHitbox();
    }

    public void changeHealth(int value) {
        if (value < 0) {
            // If immortality is active, ignore damage entirely
            if (immortalityActive) return;
            if (untargetableActive) return;
            if (blockActive) return;
            if (state == HIT || isInvulnerable())
                return;
            else
                newState(HIT);
        }

        currentHealth += value;
        currentHealth = Math.max(Math.min(currentHealth, maxHealth), 0);
    }

    public void changeHealth(int value, Enemy e) {
        if (state == HIT || isInvulnerable())
            return;
        changeHealth(value);
        pushBackOffsetDir = UP;
        pushDrawOffset = 0;

        if (e.getHitbox().x < hitbox.x)
            pushBackDir = RIGHT;
        else
            pushBackDir = LEFT;
    }

    public void kill() {
        currentHealth = 0;
        // set dying flag so Playing.update() runs player update() in dying mode
        playing.setPlayerDying(true);
    }

    public void changePower(int value) {
        powerValue += value;
        powerValue = Math.max(Math.min(powerValue, powerMaxValue), 0);
    }

    public void changeAmmo(int value) {
        ammo += value;
        ammo = Math.max(Math.min(ammo, ammoMax), 0);
    }

    public int getAmmo() {
        return ammo;
    }

    public boolean hasGun() {
        return gunUnlocked;
    }

    public int getFragmentCount() {
        return fragmentCount;
    }

    public void addFragment() {
        fragmentCount++;
        if (fragmentCount >= 6) {
            // cap at 6
            fragmentCount = 6;
            // keep previous behavior: unlock gun when reaching 6 fragments
            gunUnlocked = true;
            System.out.println("DBG: Player collected fragment (6/6) -> gun unlocked");
            // persist new state
            try {
                if (playing != null && playing.getGame() != null && playing.getGame().getSaveManager() != null) {
                    playing.getGame().getSaveManager().saveFromGame(playing.getGame());
                }
            } catch (Exception ignored) {}
        } else {
            System.out.println("DBG: Player collected fragment (" + fragmentCount + "/6)");
            try {
                if (playing != null && playing.getGame() != null && playing.getGame().getSaveManager() != null) {
                    playing.getGame().getSaveManager().saveFromGame(playing.getGame());
                }
            } catch (Exception ignored) {}
        }
    }

    public void grantImmortality() {
        immortalityUnlocked = true;
        System.out.println("DBG: Player gained immortality ability");
        // persist new state
        try {
            if (playing != null && playing.getGame() != null && playing.getGame().getSaveManager() != null) {
                playing.getGame().getSaveManager().saveFromGame(playing.getGame());
            }
        } catch (Exception ignored) {}
    }

    // Dash
    public void grantDash() {
        dashUnlocked = true;
        System.out.println("DBG: Player granted dash ability");
        try { if (playing != null && playing.getGame() != null && playing.getGame().getSaveManager() != null) playing.getGame().getSaveManager().saveFromGame(playing.getGame()); } catch (Exception ignored) {}
    }

    public boolean isDashUnlocked() { return dashUnlocked; }

    public void setDashUnlocked(boolean unlocked) { this.dashUnlocked = unlocked; }

    public void dash() {
        if (!dashUnlocked) return;
        if (dashCooldownTimer > 0) return; // on cooldown
        // reduce dash distance for better control during debugging
        final int dashDist = (int) (80 * Game.SCALE);
        // step per frame during dash
        int step = (int) (8 * Game.SCALE);
        int frames = Math.max(1, Math.abs(dashDist / step));
        // Grant brief invulnerability during dash
        invulnerableFrames = frames * 2;
        // start dash cooldown
        dashCooldownTimer = dashCooldownMs;
        isDashing = true;
        dashFramesRemaining = frames;
    }

    // Double jump
    public void grantDoubleJump() {
        doubleJumpUnlocked = true;
        // grant a single extra jump (double-jump)
        maxExtraJumps = 1;
        extraJumps = Math.min(extraJumps, maxExtraJumps);
        System.out.println("DBG: Player granted double-jump");
        try { if (playing != null && playing.getGame() != null && playing.getGame().getSaveManager() != null) playing.getGame().getSaveManager().saveFromGame(playing.getGame()); } catch (Exception ignored) {}
    }

    public boolean isDoubleJumpUnlocked() { return doubleJumpUnlocked; }

    public void setDoubleJumpUnlocked(boolean unlocked) { this.doubleJumpUnlocked = unlocked; if (unlocked) { maxExtraJumps = 1; extraJumps = Math.min(extraJumps, maxExtraJumps); } }

    // Untargetable (R-key) temporary
    public void grantUntargetable() {
        untargetableUnlocked = true;
        System.out.println("DBG: Player granted untargetable ability");
        try { if (playing != null && playing.getGame() != null && playing.getGame().getSaveManager() != null) playing.getGame().getSaveManager().saveFromGame(playing.getGame()); } catch (Exception ignored) {}
    }

    public boolean isUntargetableUnlocked() { return untargetableUnlocked; }

    public void setUntargetableUnlocked(boolean unlocked) { this.untargetableUnlocked = unlocked; }

    public void startUntargetable(int durationMs) {
        if (!untargetableUnlocked) return;
        untargetableActive = true;
        untargetableTimer = durationMs;
    }

    // Block (Q-key)
    public void grantBlock() {
        blockUnlocked = true;
        System.out.println("DBG: Player granted block ability");
        try { if (playing != null && playing.getGame() != null && playing.getGame().getSaveManager() != null) playing.getGame().getSaveManager().saveFromGame(playing.getGame()); } catch (Exception ignored) {}
    }

    public boolean isBlockUnlocked() { return blockUnlocked; }

    public void toggleBlock() {
        if (!blockUnlocked) return;
        blockActive = !blockActive;
        System.out.println("DBG: Block toggled -> " + (blockActive ? "ON" : "OFF"));
    }

    public void setBlockUnlocked(boolean unlocked) { this.blockUnlocked = unlocked; }

    public boolean isBlockActive() { return blockActive; }

    public void setUntargetableActive(boolean active, int timerMs) { this.untargetableActive = active; this.untargetableTimer = active ? timerMs : 0; }

    public void toggleImmortality() {
        if (!immortalityUnlocked) return;
        immortalityActive = !immortalityActive;
        System.out.println("DBG: Immortality toggled -> " + (immortalityActive ? "ON" : "OFF"));
    }

    // Hold-to-use Q (block before final fragment, immortality after final fragment)
    public boolean canUseQ() {
        return (blockUnlocked || immortalityUnlocked) && qCooldownTimer <= 0;
    }

    public boolean startQHold() {
        if (!canUseQ()) return false;
        if (immortalityUnlocked) {
            immortalityActive = true;
            System.out.println("DBG: Immortality hold ON");
        } else {
            blockActive = true;
            System.out.println("DBG: Block hold ON");
        }
        return true;
    }

    public void stopQHold() {
        if (immortalityActive) {
            immortalityActive = false;
            System.out.println("DBG: Immortality hold OFF");
        }
        if (blockActive) {
            blockActive = false;
            System.out.println("DBG: Block hold OFF");
        }
        qCooldownTimer = qCooldownMs;
    }

    // R cooldown helpers
    public boolean canUseR() { return untargetableUnlocked && rCooldownTimer <= 0; }

    public void setROnCooldown() { rCooldownTimer = rCooldownMs; }

    public int getQCooldownTimer() { return qCooldownTimer; }

    public int getRCooldownTimer() { return rCooldownTimer; }

    public boolean isImmortalityActive() {
        return immortalityActive;
    }

    public void setFragmentCount(int count) {
        this.fragmentCount = Math.max(0, Math.min(6, count));
    }

    public void setGunUnlocked(boolean unlocked) {
        this.gunUnlocked = unlocked;
    }

    public void setImmortalityUnlocked(boolean unlocked) {
        this.immortalityUnlocked = unlocked;
    }

    public boolean isImmortalityUnlocked() {
        return immortalityUnlocked;
    }

    public void grantGun() {
        gunUnlocked = true;
        System.out.println("DBG: Player gun granted");
    }

    public void shoot() {
        if (!gunUnlocked)
            return;
        if (ammo <= 0)
            return;
        // create projectile and consume ammo
        // spawn bullet a bit in front of player's hand
        int offsetX = (int) (10 * Game.SCALE);
        int spawnX = (int) (hitbox.x + hitbox.width / 2 + facingDir * offsetX);
        // raise spawn a bit higher (mid-torso) so bullet appears from gun
        int spawnY = (int) (hitbox.y + hitbox.height / 2 - (int) (10 * Game.SCALE));
        playing.getObjectManager().addPlayerProjectile(spawnX, spawnY, facingDir);
        // record last shot position for debug marker
        lastShotX = spawnX;
        lastShotY = spawnY;
        changeAmmo(-1);
        try {
            playing.getGame().getAudioPlayer().playAttackSound();
        } catch (Exception ignored) {
        }
    }

    public void loadLvlData(int[][] lvlData) {
        this.lvlData = lvlData;
        if (!IsEntityOnFloor(hitbox, lvlData))
            inAir = true;
        // give a short invulnerability window after resetting so spawn hazards
        // can't instantly kill the player (prevents death-on-resume).
        invulnerableFrames = 60; // ~1 second at 60 FPS, scaled to UPS
        System.out.println("DBG: Player.resetAll() end -> health=" + currentHealth + " x=" + x + " y=" + y + " invFrames=" + invulnerableFrames);
    }

    public void resetDirBooleans() {
        left = false;
        right = false;
    }

    public void setAttacking(boolean attacking) {
        this.attacking = attacking;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isLeft() {
        return left;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public boolean isRight() {
        return right;
    }

    public void setRight(boolean right) {
        this.right = right;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }

    public void resetAll() {
        System.out.println("DBG: Player.resetAll() start -> health=" + currentHealth + " x=" + x + " y=" + y);
        resetDirBooleans();
        inAir = false;
        attacking = false;
        moving = false;
        airSpeed = 0f;
        state = IDLE;
        currentHealth = maxHealth;
        powerAttackActive = false;
        powerAttackTick = 0;
        powerValue = powerMaxValue;
        ammo = ammoMax;
        running = false;
        extraJumps = maxExtraJumps;

        pushDrawOffset = 0; // Reset push draw offset to fix permanent sprite shift after damage

        hitbox.x = x;
        hitbox.y = y;
        syncXYWithHitbox();
        resetAttackBox();

        if (!IsEntityOnFloor(hitbox, lvlData))
            inAir = true;
        System.out.println("DBG: Player.resetAll() end -> health=" + currentHealth + " x=" + x + " y=" + y);
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    private void resetAttackBox() {
        if (flipW == 1)
            setAttackBoxOnRightSide();
        else
            setAttackBoxOnLeftSide();
    }

    public int getTileY() {
        return tileY;
    }

    public boolean isInvulnerable() {
        // Player is invulnerable during short invulnerability frames or when immortality is active
        return invulnerableFrames > 0 || immortalityActive;
    }

    public boolean isDashAvailable() {
        return dashUnlocked && dashCooldownTimer <= 0;
    }

    public void powerAttack() {
        if (powerAttackActive)
            return;
        if (powerValue >= 60) {
            powerAttackActive = true;
            changePower(-60);
        }

    }
}

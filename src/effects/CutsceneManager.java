package effects;

import main.Game;
import utilz.LoadSave;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class CutsceneManager {

    private final Game game;
    private boolean active = false;
    // whether this running cutscene was the intro sequence (used to persist introPlayed)
    private boolean wasIntroSequence = false;

    private BufferedImage image;
    private Clip vaClip;
    private Clip titleClip;

    private enum Stage {IDLE, FADE_IN, PLAY_VA, SHOW_IMAGE, FADE_OUT}

    private Stage stage = Stage.IDLE;
    private int timer = 0; // ms counter for stages
    private float alpha = 0f; // 0..1 for black overlay
    private String[] introLines = null;
    private java.util.List<String> wrappedIntroLines = null;
    private java.util.List<Integer> wrappedDurations = null;
    private int visibleLineCount = 0;
    private int lineTimer = 0; // ms within current visible line
    // Typewriter effect state (ms per char and visible char counts per wrapped line)
    private final int msPerChar = 100; // much slower: ~10 chars/sec reveal
    private int charTimer = 0;
    private int[] visibleChars = null;
    // Optional per-cutscene (fragment) wrapped post-message and visibility
    private java.util.List<String> postWrapped = null;
    private int[] postVisibleChars = null;
    // optional post-cutscene single-line message (e.g. ability unlock hints)
    private String postMessage = null;

    public CutsceneManager(Game game) {
        this.game = game;
    }

    public boolean isActive() { return active; }

    // Skip the currently playing cutscene immediately (stop audio and finish)
    public void skip() {
        if (!active) return;
        try { if (vaClip != null && vaClip.isRunning()) vaClip.stop(); } catch (Exception ignored) {}
        try { if (titleClip != null && titleClip.isRunning()) titleClip.stop(); } catch (Exception ignored) {}
        finish();
    }

    // Start the intro sequence: play intro VA then show image with title wav
    public void startIntro() {
        if (active) return;
        // fade out music smoothly
        game.getAudioPlayer().fadeVolumeTo(0f, 800, true);
        // prepare image and VA
        // Prepare intro dialogue lines (voice-over text)
        introLines = new String[] {
                "The world is aflame. A city once teeming with life now lies in ruin, silent, and peopled by monsters.",
                "A man wakes up in a strange capsule from beneath broken concrete and debris.",
                "He can't remember his name.",
                "",
                "He cannot recall what happened.",
                "Confused and afraid, he rises to his feet, calling for help.",
                "Instead of a human… a hollow, twisted figure approaches him.",
                "",
                "It strikes without warning.",
                "He survives - but with many questions.",
                "",
                "Who is he?", 
                "What has happened to the world?",
                "Why is he still alive?"
        };
        // Pre-wrap and compute durations for lines
        prepareWrappedIntroLines();
        // We'll show the hollowborn image after the VA and title sound
        image = null; // set later after VA
        vaClip = openClipFromRes("va's/intro.wav");
        titleClip = openClipFromRes("title.wav");
        if (titleClip == null) titleClip = openClipFromRes("audio/title.wav");
        stage = Stage.FADE_IN;
        timer = 0;
        alpha = 0f;
        active = true;
        wasIntroSequence = true;
    }

    // Start a            pickup cutscene; fragIndex used to choose audio file if available
    public void startFragmentCutscene(int fragIndex) {
        // backwards-compatible single-arg call -> delegate
        startFragmentCutscene(fragIndex, null);
    }

    // Start a fragment pickup cutscene; fragIndex used to choose audio file if available
    public void startFragmentCutscene(int fragIndex, String postMessage) {
        if (active) return;
        game.getAudioPlayer().fadeVolumeTo(0f, 500, true);
        image = LoadSave.GetSpriteAtlas(LoadSave.FRAGMENT);
        // try several naming conventions
        vaClip = openClipFromRes("va's/frag" + fragIndex + ".wav");
        if (vaClip == null) vaClip = openClipFromRes("va's/fragment.wav");
        this.postMessage = postMessage;
        // prepare post-message wrapping and visibility counters for typewriter
        if (this.postMessage != null) {
            postWrapped = wrapText(new String[]{ this.postMessage });
            postVisibleChars = new int[postWrapped.size()];
            for (int i = 0; i < postVisibleChars.length; i++) postVisibleChars[i] = 0;
            charTimer = 0;
        } else {
            postWrapped = null;
            postVisibleChars = null;
        }
        stage = Stage.FADE_IN;
        timer = 0;
        alpha = 0f;
        active = true;
    }

    // non-blocking update called from game loop; dt in ms (approx)
    public void update() {
        if (!active) return;
        int dt = 16; // approximately per tick at 60fps; fine-grained timing not required
        timer += dt;
        switch (stage) {
            case IDLE -> {
                // do nothing
            }
            case FADE_IN -> {
                alpha = Math.min(1f, alpha + 0.06f);
                if (alpha >= 1f) {
                    stage = Stage.PLAY_VA;
                    timer = 0;
                    if (vaClip != null) {
                        try {
                            if (vaClip.isRunning()) vaClip.stop();
                            vaClip.setMicrosecondPosition(0);
                            vaClip.start();
                        } catch (Exception ignored) {}
                    }
                }
            }
            case PLAY_VA -> {
                // reveal intro lines with typewriter while the VA is playing
                if (wrappedIntroLines != null && visibleLineCount < wrappedIntroLines.size()) {
                    if (visibleChars == null || visibleChars.length != wrappedIntroLines.size()) {
                        visibleChars = new int[wrappedIntroLines.size()];
                        for (int i = 0; i < visibleChars.length; i++) visibleChars[i] = 0;
                        charTimer = 0;
                    }
                    // reveal characters for current line
                    String curLine = wrappedIntroLines.get(visibleLineCount);
                    if (visibleChars[visibleLineCount] < curLine.length()) {
                        charTimer += dt;
                        while (charTimer >= msPerChar && visibleChars[visibleLineCount] < curLine.length()) {
                            visibleChars[visibleLineCount]++;
                            charTimer -= msPerChar;
                        }
                    } else {
                        // advance immediately to next line once fully revealed
                        visibleLineCount++;
                    }
                }

                // wait until VA finishes (or a short timeout)
                if (vaClip == null || !vaClip.isActive()) {
                    // after VA, play title clip (if present) and set image
                    if (titleClip != null) {
                        try {
                            if (titleClip.isRunning()) titleClip.stop();
                            titleClip.setMicrosecondPosition(0);
                            titleClip.start();
                        } catch (Exception ignored) {}
                    }
                    // prefer jpg if present, fallback to png
                    image = LoadSave.GetSpriteAtlas("hollowborn.jpg");
                    if (image == null) image = LoadSave.GetSpriteAtlas("hollowborn.png");
                    stage = Stage.SHOW_IMAGE;
                    timer = 0;
                    // reset per-line timers when entering SHOW_IMAGE (start reveal fresh in SHOW_IMAGE)
                    visibleLineCount = 0;
                    lineTimer = 0;
                }
            }
            case SHOW_IMAGE -> {
                // advance visible lines sequentially based on per-line durations, with typewriter reveal
                if (wrappedIntroLines != null && visibleLineCount < wrappedIntroLines.size()) {
                    // reveal characters for current visible line
                    String curLine = wrappedIntroLines.get(visibleLineCount);
                    if (visibleChars == null || visibleChars.length != wrappedIntroLines.size()) {
                        visibleChars = new int[wrappedIntroLines.size()];
                        for (int i = 0; i < visibleChars.length; i++) visibleChars[i] = 0;
                        charTimer = 0;
                    }
                    if (visibleChars[visibleLineCount] < curLine.length()) {
                        charTimer += dt;
                        while (charTimer >= msPerChar && visibleChars[visibleLineCount] < curLine.length()) {
                            visibleChars[visibleLineCount]++;
                            charTimer -= msPerChar;
                        }
                    } else {
                        // full line visible: start its display timer
                        lineTimer += dt;
                        int dur = wrappedDurations.get(Math.max(0, Math.min(visibleLineCount, wrappedDurations.size()-1)));
                        if (lineTimer >= dur) {
                            visibleLineCount++;
                            lineTimer = 0;
                        }
                    }
                }

                // when all intro lines shown and image shown for an extra 1200ms, fade out
                if (wrappedIntroLines == null || visibleLineCount >= (wrappedIntroLines == null ? 0 : wrappedIntroLines.size())) {
                    if (timer >= 1200) {
                        stage = Stage.FADE_OUT;
                        timer = 0;
                    }
                }
            }
            case FADE_OUT -> {
                alpha = Math.max(0f, alpha - 0.06f);
                if (alpha <= 0f) {
                    finish();
                }
            }
        }
    }

    public void draw(Graphics g) {
        if (!active) return;
        // Draw black overlay with alpha
        int aw = (int) (255 * Math.min(1f, alpha));
        Color c = new Color(0, 0, 0, aw);
        g.setColor(c);
        g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);

        // If showing the image and alpha>0, draw image centered
        if ((stage == Stage.SHOW_IMAGE || stage == Stage.PLAY_VA) && image != null) {
            int iw = (int) (image.getWidth() * Game.SCALE);
            int ih = (int) (image.getHeight() * Game.SCALE);
            int x = Game.GAME_WIDTH / 2 - iw / 2;
            int y = Game.GAME_HEIGHT / 2 - ih / 2;
            g.drawImage(image, x, y, iw, ih, null);
        }

        // Draw intro dialogue text box during intro VA/show-image stages using wrapped/typewriter drawing
        if (wrappedIntroLines != null && (stage == Stage.PLAY_VA || stage == Stage.SHOW_IMAGE)) {
            drawWrappedDialogueBox(g, wrappedIntroLines, visibleChars);
        }

        // If a fragment post-message is present (e.g. "You have gained immortality (press 'G')"),
        // display it while showing the image.
        if (postWrapped != null && stage == Stage.SHOW_IMAGE) {
            drawWrappedDialogueBox(g, postWrapped, postVisibleChars);
        }
    }

    private void drawDialogueBox(Graphics g, String[] lines) {
        int padding = (int) (12 * Game.SCALE);
        int maxWidth = Game.GAME_WIDTH - padding * 4;
        java.util.List<String> wrapped = new java.util.ArrayList<>();
        java.awt.Font font = new java.awt.Font("Serif", java.awt.Font.PLAIN, (int) (18 * Game.SCALE));
        g.setFont(font);
        java.awt.FontMetrics fm = g.getFontMetrics(font);

        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                wrapped.add("");
                continue;
            }
            String[] words = line.split(" ");
            StringBuilder cur = new StringBuilder();
            for (String w : words) {
                String test = cur.length() == 0 ? w : cur + " " + w;
                if (fm.stringWidth(test) > maxWidth) {
                    wrapped.add(cur.toString());
                    cur = new StringBuilder(w);
                } else {
                    cur = new StringBuilder(test);
                }
            }
            if (cur.length() > 0) wrapped.add(cur.toString());
        }

        int lineH = fm.getHeight();
        int boxH = lineH * wrapped.size() + padding * 2;
        int boxW = maxWidth + padding * 2;
        int x = (Game.GAME_WIDTH - boxW) / 2;
        int y = Game.GAME_HEIGHT - boxH - (int) (20 * Game.SCALE);

        // background
        java.awt.Color bg = new java.awt.Color(0, 0, 0, 200);
        g.setColor(bg);
        g.fillRect(x, y, boxW, boxH);
        // border
        g.setColor(java.awt.Color.WHITE);
        g.drawRect(x, y, boxW, boxH);

        // draw lines
        g.setColor(java.awt.Color.WHITE);
        int ty = y + padding + fm.getAscent();
        for (String s : wrapped) {
            g.drawString(s, x + padding, ty);
            ty += lineH;
        }
    }

    private java.util.List<String> wrapText(String[] lines) {
        java.util.List<String> wrapped = new java.util.ArrayList<>();
        java.awt.Font font = new java.awt.Font("Serif", java.awt.Font.PLAIN, (int) (18 * Game.SCALE));
        // create a temporary Graphics for metrics
        java.awt.image.BufferedImage tmp = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics tg = tmp.getGraphics();
        tg.setFont(font);
        java.awt.FontMetrics fm = tg.getFontMetrics();
        int padding = (int) (12 * Game.SCALE);
        int maxWidth = Game.GAME_WIDTH - padding * 4;

        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                wrapped.add("");
                continue;
            }
            String[] words = line.split(" ");
            StringBuilder cur = new StringBuilder();
            for (String w : words) {
                String test = cur.length() == 0 ? w : cur + " " + w;
                if (fm.stringWidth(test) > maxWidth) {
                    wrapped.add(cur.toString());
                    cur = new StringBuilder(w);
                } else {
                    cur = new StringBuilder(test);
                }
            }
            if (cur.length() > 0) wrapped.add(cur.toString());
        }
        tg.dispose();
        return wrapped;
    }

    private void drawWrappedDialogueBox(Graphics g, java.util.List<String> wrapped, int[] visChars) {
        int padding = (int) (12 * Game.SCALE);
        int maxWidth = Game.GAME_WIDTH - padding * 4;
        java.awt.Font font = new java.awt.Font("Serif", java.awt.Font.PLAIN, (int) (18 * Game.SCALE));
        g.setFont(font);
        java.awt.FontMetrics fm = g.getFontMetrics(font);

        int lineH = fm.getHeight();
        int boxH = lineH * wrapped.size() + padding * 2;
        int boxW = maxWidth + padding * 2;
        int x = (Game.GAME_WIDTH - boxW) / 2;
        int y = Game.GAME_HEIGHT - boxH - (int) (20 * Game.SCALE);

        // background
        java.awt.Color bg = new java.awt.Color(0, 0, 0, 200);
        g.setColor(bg);
        g.fillRect(x, y, boxW, boxH);
        // border
        g.setColor(java.awt.Color.WHITE);
        g.drawRect(x, y, boxW, boxH);

        // draw lines with typewriter visibility
        g.setColor(java.awt.Color.WHITE);
        int ty = y + padding + fm.getAscent();
        for (int i = 0; i < wrapped.size(); i++) {
            String s = wrapped.get(i);
            int end = s.length();
            if (visChars != null && i < visChars.length) end = Math.max(0, Math.min(s.length(), visChars[i]));
            String toDraw = s.substring(0, end);
            g.drawString(toDraw, x + padding, ty);
            ty += lineH;
        }
    }

    private void prepareWrappedIntroLines() {
        if (introLines == null) return;
        wrappedIntroLines = new java.util.ArrayList<>();
        wrappedDurations = new java.util.ArrayList<>();
        java.awt.Font font = new java.awt.Font("Serif", java.awt.Font.PLAIN, (int) (18 * Game.SCALE));
        // create a temporary Graphics for metrics
        java.awt.image.BufferedImage tmp = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics tg = tmp.getGraphics();
        tg.setFont(font);
        java.awt.FontMetrics fm = tg.getFontMetrics();
        int padding = (int) (12 * Game.SCALE);
        int maxWidth = Game.GAME_WIDTH - padding * 4;

        for (String line : introLines) {
            if (line == null || line.isEmpty()) {
                wrappedIntroLines.add("");
                // short pause for blank lines
                wrappedDurations.add(600);
                continue;
            }
            String[] words = line.split(" ");
            StringBuilder cur = new StringBuilder();
            for (String w : words) {
                String test = cur.length() == 0 ? w : cur + " " + w;
                if (fm.stringWidth(test) > maxWidth) {
                    wrappedIntroLines.add(cur.toString());
                    // duration proportional to length
                    wrappedDurations.add(Math.max(900, fm.stringWidth(cur.toString()) / 2));
                    cur = new StringBuilder(w);
                } else {
                    cur = new StringBuilder(test);
                }
            }
            if (cur.length() > 0) {
                wrappedIntroLines.add(cur.toString());
                wrappedDurations.add(Math.max(900, fm.stringWidth(cur.toString()) / 2));
            }
        }
        tg.dispose();
        visibleLineCount = 0;
        lineTimer = 0;
    }

    private void finish() {
        active = false;
        stage = Stage.IDLE;
        timer = 0;
        alpha = 0f;
        // stop clip
        if (vaClip != null) {
            try { vaClip.stop(); } catch (Exception ignored) {}
            vaClip = null;
        }
        // clear any post-message
        postMessage = null;
        // restore music
        game.getAudioPlayer().restoreSavedVolume(600);
        // if we just finished the intro, mark it as played and persist
        if (wasIntroSequence) {
            try {
                if (game.getSaveManager() != null) {
                    game.getSaveManager().getSaveData().introPlayed = true;
                    game.getSaveManager().saveFromGame(game);
                }
            } catch (Exception ignored) {}
            wasIntroSequence = false;
        }
    }

    private Clip openClipFromRes(String relPath) {
        try {
            InputStream is = LoadSave.class.getResourceAsStream("/res/" + relPath);
            if (is == null) {
                // try under audio
                is = LoadSave.class.getResourceAsStream("/res/audio/" + relPath);
            }
            if (is == null) return null;
            java.io.BufferedInputStream bis = new java.io.BufferedInputStream(is);
            AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
            Clip c = AudioSystem.getClip();
            c.open(ais);
            return c;
        } catch (Exception e) {
            // silent fail if not found
            return null;
        }
    }
}

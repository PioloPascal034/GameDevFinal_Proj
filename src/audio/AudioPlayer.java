package audio;

import utilz.LoadSave;

import javax.sound.sampled.*;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.File;
import java.util.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AudioPlayer: loads background songs, effects, and VO fragment clips.
 * - playFragmentVO(int index, Runnable onComplete) -> non-blocking, safe callback
 * - stopAllVO() -> stops any fragment VO safely
 *
 * Looks for files named: fragment_1.wav ... fragment_6.wav in /res/audio
 */
public class AudioPlayer {

    // Song IDs (legacy constants kept for compatibility)
    public static final int MENU_1 = 0;
    public static final int LEVEL_1 = 1;
    public static final int LEVEL_2 = 2;

    // Effect IDs
    public static final int DIE = 0;
    public static final int JUMP = 1;
    public static final int GAMEOVER = 2;
    public static final int LVL_COMPLETED = 3;
    public static final int ATTACK_ONE = 4;
    public static final int ATTACK_TWO = 5;
    public static final int ATTACK_THREE = 6;

    private Clip[] effects;
    // dynamic song handling
    private Clip menuClip;
    private final List<Clip> levelClips = new ArrayList<>();
    private Clip currentSongClip = null;

    // Default volume: set to max by user request
    private float volume = 1.0f;
    private float savedVolume = -1f; // stored when temporarily lowering volume
    private volatile Thread fadeThread = null;

    private boolean songMute, effectMute;
    private final Random rand = new Random();
    private String[] effectNames;

    // === Voice Over Clips ===
    private static final int NUM_FRAGMENTS = 6; // change if you have more/less
    private final Clip[] fragmentVOs = new Clip[NUM_FRAGMENTS];

    // For cleanup/stop we keep per-fragment listeners & fallback threads
    private final LineListener[] fragmentListeners = new LineListener[NUM_FRAGMENTS];
    private final Thread[] fragmentFallbackThreads = new Thread[NUM_FRAGMENTS];

    public AudioPlayer() {
        loadSongs();
        loadEffects();
        loadVO(); // load fragment VO clips
        // start menu music if available
        try {
            if (menuClip != null) playClip(menuClip);
        } catch (Exception ignored) {}
    }

    private void loadSongs() {
        // Try to auto-discover audio files under /res/audio
        URL url = LoadSave.class.getResource("/res/audio");
        if (url == null) {
            // fallback to legacy names
            menuClip = getClip("menu");
            Clip l1 = getClip("level1");
            Clip l2 = getClip("level2");
            if (l1 != null) levelClips.add(l1);
            if (l2 != null) levelClips.add(l2);
            return;
        }

        File folder;
        try {
            folder = new File(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            // fallback
            menuClip = getClip("menu");
            return;
        }

        File[] files = folder.listFiles((d, name) -> name.toLowerCase().endsWith(".wav"));
        if (files == null || files.length == 0) {
            System.err.println("No audio files found in /res/audio");
            menuClip = getClip("menu");
            return;
        }

        // Collect level files with numeric order, keep menu separately
        Map<Integer, Clip> levelMap = new TreeMap<>();
        for (File f : files) {
            String fname = f.getName();
            String base = fname.substring(0, fname.lastIndexOf('.')).toLowerCase();
            if (base.startsWith("menu")) {
                Clip c = getClip(base);
                if (c != null) menuClip = c;
            } else if (base.startsWith("level")) {
                // parse number after 'level'
                String numPart = base.substring(5);
                int idx = -1;
                try { idx = Integer.parseInt(numPart); } catch (NumberFormatException ignored) {}
                Clip c = getClip(base);
                if (c != null) {
                    if (idx >= 0) levelMap.put(idx, c);
                    else levelClips.add(c);
                }
            }
        }

        // Add ordered level clips
        for (Clip c : levelMap.values()) levelClips.add(c);
    }

    private void loadEffects() {
        String[] eNames = {"die", "jump", "gameover", "lvlcompleted", "attack1", "attack2", "attack3"};
        this.effectNames = eNames;
        effects = new Clip[eNames.length];

        for (int i = 0; i < eNames.length; i++) {
            Clip c = getClip(eNames[i]);
            effects[i] = c;
            if (c == null) System.err.println("Failed to load effect: " + eNames[i] + ".wav");
        }

        updateEffectsVolume();
    }

    /**
     * Load fragment VO files named fragment_1.wav ... fragment_6.wav
     * If a file is missing the slot will be null.
     */
    private void loadVO() {
        for (int i = 0; i < NUM_FRAGMENTS; i++) {
            String name = "fragment_" + (i + 1);
            Clip c = getClip(name);
            fragmentVOs[i] = c;
            fragmentListeners[i] = null;
            fragmentFallbackThreads[i] = null;
            if (c == null) {
                System.err.println("Fragment VO not found: " + name + ".wav (slot " + (i+1) + ")");
            }
        }
        // Make sure VO volume follows master volume
        updateEffectsVolume();
    }

    private Clip getClip(String name) {
        InputStream is = LoadSave.GetAudioStream(name + ".wav");
        if (is == null) return null;
        // Wrap in BufferedInputStream to ensure mark/reset support
        try (java.io.BufferedInputStream bis = new java.io.BufferedInputStream(is);
             AudioInputStream audio = AudioSystem.getAudioInputStream(bis)) {
            Clip c = AudioSystem.getClip();
            c.open(audio);
            return c;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized void playSong(int songId) {
        // Legacy: only menu expected via playSong(MENU_1)
        if (songId == MENU_1) {
            if (menuClip != null) playClip(menuClip);
            else System.err.println("Menu clip not available");
        } else {
            System.err.println("playSong: unknown song id " + songId);
        }
    }

    private synchronized void playClip(Clip c) {
        if (c == null) return;
        // stop previous
        if (currentSongClip != null && currentSongClip.isRunning()) {
            try { currentSongClip.stop(); } catch (Exception ignored) {}
            try { currentSongClip.setMicrosecondPosition(0); } catch (Exception ignored) {}
        }
        currentSongClip = c;
        try {
            if (c.isRunning()) c.stop();
            c.setMicrosecondPosition(0);
            setClipVolume(c, volume);
            c.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void stopSong() {
        if (currentSongClip != null) {
            try { currentSongClip.stop(); } catch (Exception ignored) {}
            try { currentSongClip.setMicrosecondPosition(0); } catch (Exception ignored) {}
        }
        currentSongClip = null;
    }

    public void toggleSongMute() {
        songMute = !songMute;
        // mute menu and level clips
        if (menuClip != null) {
            try {
                if (menuClip.isControlSupported(BooleanControl.Type.MUTE)) {
                    BooleanControl mute = (BooleanControl) menuClip.getControl(BooleanControl.Type.MUTE);
                    mute.setValue(songMute);
                }
            } catch (Exception ignored) {}
        }
        for (Clip c : levelClips) {
            if (c == null) continue;
            try {
                if (c.isControlSupported(BooleanControl.Type.MUTE)) {
                    BooleanControl mute = (BooleanControl) c.getControl(BooleanControl.Type.MUTE);
                    mute.setValue(songMute);
                }
            } catch (Exception ignored) {}
        }
    }

    public void setLevelSong(int lvlIndex) {
        if (levelClips.isEmpty()) {
            // fallback to menu
            if (menuClip != null) playClip(menuClip);
            return;
        }
        int idx = Math.floorMod(lvlIndex, levelClips.size());
        playClip(levelClips.get(idx));
    }

    // Effects
    public void playEffect(int effectId) {
        if (effects == null || effectId < 0 || effectId >= effects.length) return;

        Clip c = effects[effectId];
        if (c == null) return;
        try {
            if (c.isRunning()) {
                try { c.stop(); } catch (Exception ignored) {}
            }
            try { c.setMicrosecondPosition(0); } catch (Exception ignored) {}
            setClipVolume(c, volume);
            c.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toggleEffectMute() {
        effectMute = !effectMute;
        if (effects == null) return;
        for (Clip c : effects) {
            if (c == null) continue;
            try {
                if (c.isControlSupported(BooleanControl.Type.MUTE)) {
                    BooleanControl mute = (BooleanControl) c.getControl(BooleanControl.Type.MUTE);
                    mute.setValue(effectMute);
                }
            } catch (Exception ignored) {}
        }
        if (!effectMute) playEffect(JUMP);
    }

    public void lvlCompleted() {
        stopSong();
        playEffect(LVL_COMPLETED);
    }

    public void playAttackSound() {
        int start = ATTACK_ONE + rand.nextInt(3);
        playEffect(start);
    }

    // === Fragment VO playback with callback ===
    /**
     * Play fragment VO indexed from 1..NUM_FRAGMENTS
     * onComplete.run() is invoked when the clip naturally finishes or when the fallback timer expires.
     * If the clip is absent, onComplete is invoked immediately.
     *
     * Safety notes:
     * - This implementation avoids performing clip.stop()/reset inside the audio event thread.
     * - It guarantees the onComplete runs at most once (AtomicBoolean).
     * - stopAllVO() will stop active VO and prevent the listener/fallback from calling the callback.
     */
    public void playFragmentVO(int fragmentIndex, Runnable onComplete) {
        if (fragmentIndex <= 0 || fragmentIndex > NUM_FRAGMENTS) {
            if (onComplete != null) onComplete.run();
            return;
        }
        int idx = fragmentIndex - 1;
        Clip c = fragmentVOs[idx];
        if (c == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        try {
            // Stop existing VO (if any) and its listeners/fallbacks
            stopAllVO();

            // reset position, set volume
            try { c.stop(); } catch (Exception ignored) {}
            try { c.setMicrosecondPosition(0); } catch (Exception ignored) {}
            setClipVolume(c, volume);

            final AtomicBoolean callbackFired = new AtomicBoolean(false);

            // Create listener that only marks natural completion; heavy lifting performed off-audio-thread
            LineListener listener = new LineListener() {
                @Override
                public void update(LineEvent event) {
                    if (event.getType() == LineEvent.Type.STOP) {
                        // Check position vs length to detect natural end vs manual stop
                        long posUs = -1L;
                        long lenUs = -1L;
                        try { posUs = c.getMicrosecondPosition(); } catch (Exception ignored) {}
                        try { lenUs = c.getMicrosecondLength(); } catch (Exception ignored) {}

                        boolean naturalEnd = false;
                        if (lenUs > 0) {
                            long margin = Math.min(100000L, lenUs / 20L); // margin up to 100ms or 5%
                            if (posUs >= (lenUs - margin)) naturalEnd = true;
                        } else {
                            // Unknown length: assume STOP indicates natural end
                            naturalEnd = true;
                        }

                        if (!naturalEnd) {
                            // STOP likely due to manual stopAllVO(); ignore here
                            return;
                        }

                        // Schedule cleanup and callback on a separate thread to avoid audio-system deadlocks
                        if (callbackFired.compareAndSet(false, true)) {
                            // remove listener and reset clip on separate daemon thread
                            Thread cleanup = new Thread(() -> {
                                try {
                                    try { c.stop(); } catch (Exception ignored) {}
                                    try { c.setMicrosecondPosition(0); } catch (Exception ignored) {}
                                    try { c.removeLineListener(this); } catch (Exception ignored) {}
                                } catch (Exception ignored) {}
                                if (onComplete != null) {
                                    try { onComplete.run(); } catch (Exception ignored) {}
                                }
                            }, "frag-vo-cleanup-listener-" + fragmentIndex);
                            cleanup.setDaemon(true);
                            cleanup.start();
                        }
                    }
                }
            };

            // store listener so stopAllVO can remove it later
            fragmentListeners[idx] = listener;
            c.addLineListener(listener);
            c.start();

            // Fallback timer (clip length + margin). Ensures callback fires even if audio system fails to dispatch STOP.
            long clipLenUs = -1L;
            try { clipLenUs = c.getMicrosecondLength(); } catch (Exception ignored) {}
            if (clipLenUs <= 0) clipLenUs = 5_000_000L; // default 5s

            final long waitMs = (clipLenUs / 1000L) + 250L; // clip length + 250ms margin
            Thread fallback = new Thread(() -> {
                try {
                    Thread.sleep(waitMs);
                    if (callbackFired.compareAndSet(false, true)) {
                        try { c.stop(); } catch (Exception ignored) {}
                        try { c.setMicrosecondPosition(0); } catch (Exception ignored) {}
                        try {
                            LineListener l = fragmentListeners[idx];
                            if (l != null) c.removeLineListener(l);
                        } catch (Exception ignored) {}
                        if (onComplete != null) {
                            try { onComplete.run(); } catch (Exception ignored) {}
                        }
                    }
                } catch (InterruptedException ignored) {
                    // If interrupted (e.g., stopAllVO), we don't call the callback here.
                }
            }, "frag-vo-fallback-" + fragmentIndex);
            fallback.setDaemon(true);
            fragmentFallbackThreads[idx] = fallback;
            fallback.start();

        } catch (Exception e) {
            e.printStackTrace();
            if (onComplete != null) onComplete.run();
        }
    }

    /**
     * Stop any fragment VO currently playing.
     * This also removes listeners and interrupts fallback threads to prevent callbacks.
     */
    public void stopAllVO() {
        for (int i = 0; i < NUM_FRAGMENTS; i++) {
            try {
                // interrupt fallback timer thread if present
                Thread t = fragmentFallbackThreads[i];
                if (t != null && t.isAlive()) {
                    try { t.interrupt(); } catch (Exception ignored) {}
                    fragmentFallbackThreads[i] = null;
                }

                Clip c = fragmentVOs[i];
                if (c != null) {
                    // Remove listener before stopping to avoid race where STOP event is handled as natural end
                    LineListener l = fragmentListeners[i];
                    if (l != null) {
                        try { c.removeLineListener(l); } catch (Exception ignored) {}
                        fragmentListeners[i] = null;
                    }

                    if (c.isRunning()) {
                        try { c.stop(); } catch (Exception ignored) {}
                    }
                    try { c.setMicrosecondPosition(0); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
    }

    // Volume
    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        updateSongVolume();
        updateEffectsVolume();
    }

    private void updateSongVolume() {
        if (currentSongClip != null) setClipVolume(currentSongClip, volume);
    }

    private void updateEffectsVolume() {
        if (effects != null) {
            for (Clip c : effects) {
                if (c == null) continue;
                setClipVolume(c, volume);
            }
        }
        if (fragmentVOs != null) {
            for (Clip c : fragmentVOs) {
                if (c == null) continue;
                setClipVolume(c, volume);
            }
        }
    }

    private void setClipVolume(Clip clip, float vol) {
        if (clip == null) return;
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float min = gain.getMinimum();
                float max = gain.getMaximum();
                float value = min + (max - min) * vol;
                gain.setValue(value);
            }
        } catch (Exception ignored) {}
    }

    // Fade utilities
    public synchronized float getVolume() {
        return volume;
    }

    public synchronized void fadeVolumeTo(float targetVolume, int durationMs, boolean saveCurrent) {
        final float tv = Math.max(0f, Math.min(1f, targetVolume));
        if (saveCurrent) savedVolume = this.volume;

        // stop existing fade
        if (fadeThread != null && fadeThread.isAlive()) {
            fadeThread.interrupt();
        }

        fadeThread = new Thread(() -> {
            try {
                float start = getVolume();
                int steps = Math.max(1, durationMs / 50);
                for (int i = 1; i <= steps; i++) {
                    if (Thread.currentThread().isInterrupted()) return;
                    float v = start + (tv - start) * ((float) i / (float) steps);
                    setVolume(v);
                    Thread.sleep(durationMs / steps);
                }
                setVolume(tv);
            } catch (InterruptedException ignored) {
            }
        });
        fadeThread.setDaemon(true);
        fadeThread.start();
    }

    public synchronized void restoreSavedVolume(int durationMs) {
        float to = savedVolume >= 0f ? savedVolume : 1.0f;
        fadeVolumeTo(to, durationMs, false);
        savedVolume = -1f;
    }

}

package hansung.arcade.outgame.audio;

import javazoom.jl.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedInputStream;
import java.io.InputStream;

/** MP3 재생은 데몬 스레드에서 수행하며 화면 전환 시 이전 트랙을 닫는다. */
public final class Sound {
    private static final Logger LOG = LoggerFactory.getLogger(Sound.class);
    private static final Track BGM = new Track();
    private static final Track EFFECT = new Track();
    private static boolean muted;
    private static String bgm;

    private Sound() { }

    public static synchronized void playBgm(String key) {
        if (key.equals(bgm)) {
            return;
        }
        bgm = key;
        BGM.stop();
        if (!muted) {
            BGM.start(key, true);
        }
    }

    public static synchronized void play(String key) {
        if (!muted) {
            EFFECT.start(key, false);
        }
    }

    public static synchronized boolean isMuted() {
        return muted;
    }

    public static synchronized void setMuted(boolean value) {
        if (muted == value) {
            return;
        }
        muted = value;
        BGM.stop();
        EFFECT.stop();
        if (!muted && bgm != null) {
            BGM.start(bgm, true);
        }
    }

    public static synchronized void stopAll() {
        BGM.stop();
        EFFECT.stop();
        bgm = null;
    }

    private static final class Track {
        private Player player;
        private long generation;

        synchronized void stop() {
            generation++;
            if (player != null) {
                player.close();
                player = null;
            }
        }

        synchronized void start(String key, boolean loop) {
            stop();
            long token = generation;
            Thread worker = new Thread(() -> run(key, loop, token), "sound-" + key);
            worker.setDaemon(true);
            worker.start();
        }

        private void run(String key, boolean loop, long token) {
            do {
                try (InputStream raw = Sound.class.getResourceAsStream("/sounds/" + key + ".mp3")) {
                    if (raw == null) {
                        throw new IllegalArgumentException("사운드 리소스 없음: " + key);
                    }
                    Player next;
                    synchronized (this) {
                        if (generation != token) {
                            return;
                        }
                        next = new Player(new BufferedInputStream(raw));
                        player = next;
                    }
                    try {
                        next.play();
                    } finally {
                        next.close();
                        synchronized (this) {
                            if (player == next) {
                                player = null;
                            }
                        }
                    }
                    synchronized (this) {
                        if (generation != token) {
                            return;
                        }
                    }
                } catch (Exception exception) {
                    LOG.warn("사운드를 재생하지 못했습니다: {}", key, exception);
                    return;
                }
            } while (loop);
        }
    }
}

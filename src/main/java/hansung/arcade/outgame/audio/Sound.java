package hansung.arcade.outgame.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 소리. 곡은 resources/sounds/{이름}.mp3 이다.
 *
 * <p>BGM은 데몬 스레드에서 MP3를 풀어 가며 되풀이해 틀고, 화면이 바뀌면 이전 곡을 닫는다.</p>
 *
 * <p>효과음은 처음 낼 때 MP3를 한 번 PCM으로 풀어 두고({@link #preload}로 미리 풀 수 있다), 낼 때마다 Clip을 새로 열어
 * 여러 소리가 겹쳐 나게 한다. 물풍선이 터지는 중에 아이템을 먹어도 앞 소리가 끊기지 않는다.
 * 한꺼번에 {@value #MAX_EFFECTS}개보다 많이 울리고 있으면 새 소리는 내지 않는다.
 * Clip을 여는 일은 EDT를 막지 않도록 효과음 전용 데몬 스레드에서 한다.</p>
 */
public final class Sound {
    private static final Logger LOG = LoggerFactory.getLogger(Sound.class);
    /** 아웃게임 버튼을 누를 때 나는 짧은 소리 */
    public static final String CLICK = "Bt_c_Pop";
    /** 한꺼번에 울릴 수 있는 효과음 수 */
    static final int MAX_EFFECTS = 16;

    private static final Track BGM = new Track();
    private static final ExecutorService EFFECTS = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "sound-effects");
        thread.setDaemon(true);
        return thread;
    });
    /** 풀어 둔 효과음. 읽지 못한 소리는 빈 값으로 남겨 다시 읽으려 하지 않는다 */
    private static final Map<String, Optional<Pcm>> PCM = new ConcurrentHashMap<>();
    /** 지금 울리고 있는 효과음 */
    private static final Set<Clip> PLAYING = ConcurrentHashMap.newKeySet();
    /** 소리 장치를 열지 못했다고 이미 알렸는지. 장치가 없는 환경에서 효과음마다 경고가 쌓이지 않게 한다 */
    private static final AtomicBoolean DEVICE_WARNED = new AtomicBoolean();
    private static boolean muted;
    private static String bgm;

    private Sound() { }

    /**
     * 화면 이름(SceneManager에 등록한 이름)에 맞는 BGM 곡 이름. 게임 화면은 맵마다 곡이 달라서 {@link #bgmForMap}을 쓴다.
     *
     * @throws IllegalArgumentException 곡을 정하지 않은 화면이면
     */
    public static String bgmFor(String screen) {
        return switch (screen) {
            case "login" -> "01. login";
            case "lobby" -> "02. lobby";
            case "waiting" -> "03. room";
            default -> throw new IllegalArgumentException("Unknown screen: " + screen);
        };
    }

    /**
     * 맵 테마(맵 폴더 이름: camp, patrit, village)에 맞는 게임 BGM 곡 이름. 곡이 없는 테마면 빌리지 곡을 쓴다
     * (새 테마를 넣고 곡을 빠뜨려도 게임은 시작되게).
     */
    public static String bgmForMap(String theme) {
        return switch (theme) {
            case "camp" -> "Camp";
            case "patrit" -> "Patrit";
            case "village" -> "Village";
            default -> {
                LOG.warn("맵 테마 '{}'의 BGM이 없어 빌리지 곡을 틉니다.", theme);
                yield "Village";
            }
        };
    }

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

    /** 효과음을 낸다. 앞서 낸 효과음과 겹쳐 난다 */
    public static synchronized void play(String key) {
        if (!muted) {
            EFFECTS.execute(() -> playNow(key));
        }
    }

    /** 아웃게임 버튼 클릭음 */
    public static void click() {
        play(CLICK);
    }

    /** 효과음들을 미리 PCM으로 풀어 둔다. 처음 낼 때 늦게 나지 않게 화면에 들어가기 전에 부른다 */
    public static void preload(Collection<String> keys) {
        for (String key : keys) {
            EFFECTS.execute(() -> pcm(key));
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
        stopEffects();
        if (!muted && bgm != null) {
            BGM.start(bgm, true);
        }
    }

    public static synchronized void stopAll() {
        BGM.stop();
        stopEffects();
        bgm = null;
    }

    private static void stopEffects() {
        for (Clip clip : PLAYING) {
            clip.close();
        }
        PLAYING.clear();
    }

    /** 효과음 스레드에서 Clip을 열어 낸다. 다 울리면 닫는다 */
    private static void playNow(String key) {
        Optional<Pcm> pcm = pcm(key);
        if (pcm.isEmpty() || PLAYING.size() >= MAX_EFFECTS) {
            return;
        }
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(pcm.get().format(), pcm.get().data(), 0, pcm.get().data().length);
            AtomicBoolean finished = new AtomicBoolean();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && finished.compareAndSet(false, true)) {
                    PLAYING.remove(clip);
                    // 리스너 안에서 바로 닫으면 일부 JDK에서 멈출 수 있어 효과음 스레드로 넘긴다
                    EFFECTS.execute(clip::close);
                }
            });
            PLAYING.add(clip);
            clip.start();
        } catch (Exception | LinkageError exception) {
            if (DEVICE_WARNED.compareAndSet(false, true)) {
                LOG.warn("효과음을 낼 소리 장치를 열지 못했습니다: {}", key, exception);
            }
        }
    }

    private static Optional<Pcm> pcm(String key) {
        return PCM.computeIfAbsent(key, Sound::decode);
    }

    /** resources/sounds/{key}.mp3 를 16비트 PCM으로 푼다. 읽지 못하면 빈 값 */
    static Optional<Pcm> decode(String key) {
        try (InputStream raw = Sound.class.getResourceAsStream("/sounds/" + key + ".mp3")) {
            if (raw == null) {
                LOG.warn("사운드 리소스 없음: {}", key);
                return Optional.empty();
            }
            Bitstream bits = new Bitstream(new BufferedInputStream(raw));
            Decoder decoder = new Decoder();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int rate = 0;
            int channels = 0;
            try {
                Header header;
                while ((header = bits.readFrame()) != null) {
                    SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bits);
                    rate = decoder.getOutputFrequency();
                    channels = decoder.getOutputChannels();
                    short[] buffer = samples.getBuffer();
                    int length = samples.getBufferLength();
                    for (int i = 0; i < length; i++) {
                        out.write(buffer[i] & 0xFF);
                        out.write((buffer[i] >> 8) & 0xFF);
                    }
                    bits.closeFrame();
                }
            } finally {
                bits.close();
            }
            if (rate <= 0 || channels <= 0 || out.size() == 0) {
                LOG.warn("사운드를 풀지 못했습니다(빈 MP3): {}", key);
                return Optional.empty();
            }
            return Optional.of(new Pcm(new AudioFormat(rate, 16, channels, true, false), out.toByteArray()));
        } catch (Exception exception) {
            LOG.warn("사운드를 풀지 못했습니다: {}", key, exception);
            return Optional.empty();
        }
    }

    /** 풀어 둔 효과음 한 개: 16비트 부호 있는 리틀 엔디언 PCM */
    record Pcm(AudioFormat format, byte[] data) {
        /** 소리 길이(초) */
        double seconds() {
            return data.length / (double) (format.getFrameSize() * format.getFrameRate());
        }
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

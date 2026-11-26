package hansung.arcade.outgame.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 소리. 곡은 resources/sounds/{이름}.mp3 이다.
 *
 * <p>BGM은 데몬 스레드에서 MP3를 풀어 가며 되풀이해 틀고, 화면이 바뀌면 이전 곡을 닫는다.</p>
 *
 * <p>효과음은 처음 낼 때 MP3를 한 번 PCM으로 풀어 두고({@link #preload}로 미리 풀 수 있다), 낼 때마다 Clip을 새로 열어
 * 여러 소리가 겹쳐 나게 한다. 물풍선이 터지는 중에 아이템을 먹어도 앞 소리가 끊기지 않는다.
 * 한꺼번에 {@value #MAX_EFFECTS}개보다 많이 울리고 있으면 새 소리는 내지 않는다.
 * Clip을 여는 일은 EDT를 막지 않도록 효과음 전용 데몬 스레드에서 한다.</p>
 *
 * <p>전체 음량({@link #setVolume}, 0~{@value #MAX_VOLUME}%)은 BGM과 효과음에 모두 곱한다. BGM은 MP3 프레임마다 음량을
 * 새로 읽어 틀고 있는 곡에도 곧바로 적용하고({@link VolumeAudioDevice}), 효과음은 낼 때의 음량으로 낸다.
 * 음소거와는 따로라서 음소거를 풀면 정해 둔 음량으로 난다. 정한 음량은 이 컴퓨터에 저장해 다음에 켜도 쓴다({@link SoundSettings}).</p>
 */
public final class Sound {
    private static final Logger LOG = LoggerFactory.getLogger(Sound.class);
    /** 아웃게임 버튼을 누를 때 나는 짧은 소리 */
    public static final String CLICK = "Bt_c_Pop";
    /** 한꺼번에 울릴 수 있는 효과음 수 */
    static final int MAX_EFFECTS = 16;
    /** 전체 음량의 최댓값(%). 저장한 음량이 없으면 이 값으로 시작한다 */
    public static final int MAX_VOLUME = 100;

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
    /** 앱이 쓰는 기본 Clip */
    static final ClipSource SYSTEM_CLIPS = AudioSystem::getClip;
    private static volatile ClipSource clips = SYSTEM_CLIPS;
    /** BGM을 틀 소리 장치를 만드는 곳. 테스트는 장치 대신 쓰인 소리를 받아 보는 것으로 바꿔 끼운다 */
    private static volatile Supplier<AudioDevice> bgmDevices = VolumeAudioDevice::new;
    /** 전체 음량(%). BGM은 프레임마다, 효과음은 낼 때마다 읽는다 */
    private static volatile int volume = MAX_VOLUME;
    /** 전체 음량을 저장할 곳. {@link #useSettings} 전에는 이번 실행 동안만 기억한다 */
    private static volatile SoundSettings settings = SoundSettings.inMemory();
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

    /** 이 컴퓨터에 저장해 둔 전체 음량을 읽어 적용하고, 이후 {@link #saveVolume}이 여기에 적게 한다. 첫 곡을 틀기 전에 부른다 */
    public static void useSettings(SoundSettings saved) {
        settings = saved;
        saved.volume().ifPresent(percent -> {
            setVolume(percent);
            LOG.info("저장해 둔 전체 음량 {}%로 소리를 냅니다.", percent);
        });
    }

    /** 전체 음량(0~{@value #MAX_VOLUME}%) */
    public static int volume() {
        return volume;
    }

    /**
     * 전체 음량을 바꾼다. 틀고 있는 BGM은 다음 프레임부터, 효과음은 다음에 내는 소리부터 이 음량으로 난다.
     * 음소거와는 따로라서 음소거 중에 바꾸면 음소거를 풀 때 이 음량으로 난다. 0%도 음소거로 치지 않는다.
     * 파일에는 {@link #saveVolume}이 적는다 (슬라이더를 움직이는 동안 파일을 거듭 쓰지 않게).
     *
     * @throws IllegalArgumentException 0~100이 아니면
     */
    public static void setVolume(int percent) {
        volume = requireVolume(percent);
    }

    /** 지금 전체 음량을 이 컴퓨터에 저장한다 ({@link #useSettings}로 정한 곳) */
    public static void saveVolume() {
        settings.saveVolume(volume);
    }

    /** 소리에 곱할 진폭 배율 (0~1, {@link Volume#gain}) */
    static double gain() {
        return Volume.gain(volume);
    }

    static boolean validVolume(int percent) {
        return percent >= 0 && percent <= MAX_VOLUME;
    }

    static int requireVolume(int percent) {
        if (!validVolume(percent)) {
            throw new IllegalArgumentException("전체 음량은 0~" + MAX_VOLUME + "%입니다: " + percent);
        }
        return percent;
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

    /**
     * 효과음 스레드에서 Clip을 열어 낸다. 다 울리면 닫는다. 열거나 틀지 못한 Clip은 바로 닫고 울리는 소리로 세지 않는다
     * (세어 두면 {@value #MAX_EFFECTS}번 실패한 뒤로 효과음이 영영 나지 않는다).
     */
    static void playNow(String key) {
        // 줄 서 있는 사이 음소거됐으면 내지 않는다
        if (isMuted()) {
            return;
        }
        Optional<Pcm> pcm = pcm(key);
        // 음량이 0%면 들리지 않으니 Clip을 열지 않는다
        double gain = gain();
        if (pcm.isEmpty() || gain <= 0 || PLAYING.size() >= MAX_EFFECTS) {
            return;
        }
        Clip clip = null;
        try {
            clip = clips.open();
            start(clip, pcm.get(), gain);
        } catch (Exception | LinkageError exception) {
            if (clip != null) {
                PLAYING.remove(clip);
                clip.close();
            }
            if (DEVICE_WARNED.compareAndSet(false, true)) {
                LOG.warn("효과음을 낼 소리 장치를 열지 못했습니다: {}", key, exception);
            }
        }
    }

    /** 전체 음량 배율 gain을 곱한 소리로 Clip을 열어 튼다 */
    private static void start(Clip clip, Pcm pcm, double gain) throws LineUnavailableException {
        byte[] data = Volume.apply(pcm.data(), gain);
        clip.open(pcm.format(), data, 0, data.length);
        AtomicBoolean finished = new AtomicBoolean();
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP && finished.compareAndSet(false, true)) {
                PLAYING.remove(clip);
                // 리스너 안에서 바로 닫으면 일부 JDK에서 멈출 수 있어 효과음 스레드로 넘긴다
                EFFECTS.execute(clip::close);
            }
        });
        // 음소거(setMuted)가 울리는 소리를 다 닫는 것과 엇갈리지 않게 같은 잠금 안에서 확인하고 튼다
        synchronized (Sound.class) {
            if (muted) {
                clip.close();
                return;
            }
            PLAYING.add(clip);
            clip.start();
        }
    }

    /** 지금 울리고 있는 효과음 수 */
    static int playingCount() {
        return PLAYING.size();
    }

    /** 효과음 스레드에 줄 서 있는 일을 다 할 때까지 기다린다 (테스트가 앞 테스트의 소리와 섞이지 않게) */
    static void awaitEffects() throws Exception {
        EFFECTS.submit(() -> { }).get(10, TimeUnit.SECONDS);
    }

    /** 효과음 Clip을 얻는 곳. 테스트는 실패하는 Clip으로 바꿔 끼운다 */
    @FunctionalInterface
    interface ClipSource {
        Clip open() throws LineUnavailableException;
    }

    static void useClips(ClipSource source) {
        clips = source;
    }

    /** BGM을 틀 소리 장치. 곡을 (다시) 틀 때마다 새로 만든다 */
    static void useBgmDevices(Supplier<AudioDevice> devices) {
        bgmDevices = devices;
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
                        next = new Player(new BufferedInputStream(raw), bgmDevices.get());
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

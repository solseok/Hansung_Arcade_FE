package hansung.arcade.outgame.audio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Clip;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 전체 음량은 BGM과 효과음에 모두 곱해진다. 틀고 있는 BGM도 바꾸면 곧바로 작아지고, 효과음은 낼 때의 음량으로 난다.
 * 음소거와는 따로이고, 이 컴퓨터에 저장해 다음에 켜도 쓴다.
 *
 * <p>소리 장치 없이 확인한다: BGM은 jlayer가 장치에 쓰려던 바이트를, 효과음은 Clip에 넘긴 바이트를 받아 본다.</p>
 */
class VolumeTest {
    private static final String LOBBY = "02. lobby";

    /** BGM 장치에 쓰인 MP3 프레임들. 테스트가 한 프레임씩 꺼내야 다음 프레임을 쓴다 */
    private final BlockingQueue<Frame> frames = new SynchronousQueue<>();
    private volatile boolean capturing = true;

    @BeforeEach
    void 앞_테스트의_소리를_비운다() throws Exception {
        Sound.awaitEffects();
        Sound.stopAll();
        Sound.useBgmDevices(() -> new CapturingDevice(this));
    }

    @AfterEach
    void 원래대로() throws Exception {
        // 장치에 쓰려고 기다리는 BGM 스레드를 풀어 준 뒤 끈다
        capturing = false;
        frames.poll(200, TimeUnit.MILLISECONDS);
        Sound.stopAll();
        Sound.awaitEffects();
        Sound.useBgmDevices(VolumeAudioDevice::new);
        Sound.useClips(Sound.SYSTEM_CLIPS);
        Sound.setMuted(false);
        Sound.setVolume(Sound.MAX_VOLUME);
        Sound.useSettings(SoundSettings.inMemory());
    }

    @Test
    void 처음_음량은_100퍼센트다() {
        Sound.useSettings(SoundSettings.inMemory());
        assertEquals(Sound.MAX_VOLUME, Sound.volume());
        assertEquals(1.0, Sound.gain());
    }

    @Test
    void 음량은_제곱한_배율로_곱해_반이면_4분의_1이다() {
        assertEquals(1.0, Volume.gain(100));
        assertEquals(0.25, Volume.gain(50), 1e-9);
        assertEquals(0.0, Volume.gain(0));
        assertEquals(-8192, Volume.apply((short) -32768, 0.25));
        assertEquals(8192, Volume.apply((short) 32767, 0.25));
    }

    @Test
    void 풀어_둔_효과음_배열은_바꾸지_않는다() {
        byte[] pcm = {0x00, 0x40, (byte) 0xFF, 0x7F};
        byte[] before = pcm.clone();
        byte[] scaled = Volume.apply(pcm, 0.25);
        assertArrayEquals(before, pcm);
        assertSame(pcm, Volume.apply(pcm, 1.0));
        assertEquals(0x1000, sample(scaled, 0));
    }

    @Test
    void _0에서_100이_아닌_음량은_받지_않는다() {
        assertThrows(IllegalArgumentException.class, () -> Sound.setVolume(-1));
        assertThrows(IllegalArgumentException.class, () -> Sound.setVolume(101));
        assertEquals(Sound.MAX_VOLUME, Sound.volume());
    }

    // ── 효과음 ──────────────────────────────────────────

    @Test
    void 효과음은_낼_때의_음량을_곱한_소리로_Clip을_연다() {
        List<byte[]> opened = new ArrayList<>();
        Sound.useClips(() -> recordingClip(opened));
        byte[] original = Sound.decode(Sound.CLICK).orElseThrow().data();

        Sound.playNow(Sound.CLICK);
        Sound.setVolume(50);
        Sound.playNow(Sound.CLICK);

        assertEquals(2, opened.size());
        assertArrayEquals(original, opened.get(0), "100%면 원래 소리 그대로 낸다");
        assertScaled(original, opened.get(1), 0.25);
        assertTrue(peak(original) > 1000, "클릭음이 너무 작아 확인할 수 없습니다: " + peak(original));
    }

    @Test
    void 음량을_바꿔도_풀어_둔_효과음은_그대로라_다시_100퍼센트로_내면_원래_크기다() {
        List<byte[]> opened = new ArrayList<>();
        Sound.useClips(() -> recordingClip(opened));
        byte[] original = Sound.decode(Sound.CLICK).orElseThrow().data().clone();

        Sound.setVolume(30);
        Sound.playNow(Sound.CLICK);
        Sound.setVolume(100);
        Sound.playNow(Sound.CLICK);

        assertArrayEquals(original, opened.get(1));
    }

    @Test
    void 음량이_0이면_효과음_Clip을_열지_않는다() {
        List<byte[]> opened = new ArrayList<>();
        Sound.useClips(() -> recordingClip(opened));

        Sound.setVolume(0);
        Sound.playNow(Sound.CLICK);

        assertTrue(opened.isEmpty());
        assertEquals(0, Sound.playingCount());
    }

    // ── BGM ─────────────────────────────────────────────

    @Test
    void 틀고_있는_BGM도_음량을_바꾸면_곧바로_그_크기로_난다() throws Exception {
        Sound.playBgm(LOBBY);
        Frame loud = nextLoudFrame();
        assertArrayEquals(loud.expected(1.0), loud.written(), "100%면 곡을 그대로 낸다");

        Sound.setVolume(50);
        assertNextFramesScaled(0.25);

        Sound.setVolume(0);
        assertNextFramesScaled(0.0);
    }

    @Test
    void 음소거_중에_바꾼_음량은_음소거를_풀면_그_크기로_난다() throws Exception {
        Sound.playBgm(LOBBY);
        nextLoudFrame();
        // 음소거하면 곡을 끈다. 끄는 동안 장치를 기다리는 BGM 스레드가 없게 잠시 받지 않는다
        capturing = false;
        frames.poll(200, TimeUnit.MILLISECONDS);
        Sound.setMuted(true);

        Sound.setVolume(30);
        assertTrue(Sound.isMuted(), "음량을 바꿨다고 음소거가 풀리면 안 됩니다");

        capturing = true;
        Sound.setMuted(false);
        assertEquals(30, Sound.volume(), "음소거를 풀어도 정한 음량은 그대로다");
        Frame frame = nextLoudFrame();
        assertArrayEquals(frame.expected(Volume.gain(30)), frame.written(), "음소거를 풀면 30%(배율 0.09)로 난다");
    }

    @Test
    void 음량_0은_음소거로_치지_않는다() {
        Sound.setVolume(0);
        assertFalse(Sound.isMuted());
        Sound.setMuted(true);
        Sound.setMuted(false);
        assertEquals(0, Sound.volume());
    }

    // ── 저장 ────────────────────────────────────────────

    @Test
    void 저장한_음량으로_시작하고_바꾼_음량을_저장하면_다음에_켜도_그_음량이다(@TempDir Path dir) {
        Path file = dir.resolve("settings.properties");
        SoundSettings.file(file).saveVolume(40);

        Sound.useSettings(SoundSettings.file(file));
        assertEquals(40, Sound.volume());

        Sound.setVolume(65);
        assertEquals(40, SoundSettings.file(file).volume().orElseThrow(), "저장하기 전에는 파일을 바꾸지 않는다");
        Sound.saveVolume();
        assertEquals(65, SoundSettings.file(file).volume().orElseThrow());

        // 다음 실행
        Sound.setVolume(Sound.MAX_VOLUME);
        Sound.useSettings(SoundSettings.file(file));
        assertEquals(65, Sound.volume());
    }

    @Test
    void 저장한_적이_없으면_지금_음량을_그대로_둔다(@TempDir Path dir) {
        Sound.setVolume(80);
        Sound.useSettings(SoundSettings.file(dir.resolve("settings.properties")));
        assertEquals(80, Sound.volume());
    }

    // ── 도우미 ──────────────────────────────────────────

    /** 소리가 큰(조용한 앞부분이 아닌) 다음 프레임 */
    private Frame nextLoudFrame() throws InterruptedException {
        for (int i = 0; i < 2000; i++) {
            Frame frame = take();
            if (peak(frame.samples()) > 1000) {
                return frame;
            }
        }
        throw new AssertionError("곡에서 소리가 큰 프레임을 찾지 못했습니다");
    }

    /**
     * 음량을 바꾼 뒤의 프레임들이 gain을 곱한 소리인지 본다. 음량을 바꾸는 사이 BGM 스레드가 이미 계산해 두고 기다리던
     * 프레임 하나는 앞 음량일 수 있어 건너뛴다.
     */
    private void assertNextFramesScaled(double gain) throws InterruptedException {
        take();
        boolean loud = false;
        for (int i = 0; i < 40; i++) {
            Frame frame = take();
            assertArrayEquals(frame.expected(gain), frame.written(), "배율 " + gain + "로 나지 않은 프레임: " + i);
            loud |= peak(frame.samples()) > 1000;
        }
        assertTrue(loud, "확인한 프레임이 모두 조용해서 음량을 곱했는지 알 수 없습니다");
    }

    private Frame take() throws InterruptedException {
        Frame frame = frames.poll(5, TimeUnit.SECONDS);
        assertNotNull(frame, "BGM이 장치에 소리를 쓰지 않습니다");
        return frame;
    }

    private static void assertScaled(byte[] original, byte[] scaled, double gain) {
        assertEquals(original.length, scaled.length);
        for (int i = 0; i + 1 < original.length; i += 2) {
            short expected = Volume.apply(sample(original, i), gain);
            assertEquals(expected, sample(scaled, i), "바이트 " + i);
        }
    }

    private static short sample(byte[] pcm, int i) {
        return (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
    }

    private static int peak(byte[] pcm) {
        int peak = 0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            peak = Math.max(peak, Math.abs(sample(pcm, i)));
        }
        return peak;
    }

    private static int peak(short[] samples) {
        int peak = 0;
        for (short sample : samples) {
            peak = Math.max(peak, Math.abs(sample));
        }
        return peak;
    }

    /** jlayer가 장치에 쓰려던 프레임: MP3에서 풀린 소리(samples)와 장치에 쓸 바이트(written) */
    private record Frame(short[] samples, byte[] written) {
        /** samples에 gain을 곱해 리틀 엔디언으로 적은 바이트 */
        byte[] expected(double gain) {
            byte[] bytes = new byte[samples.length * 2];
            for (int i = 0; i < samples.length; i++) {
                short scaled = Volume.apply(samples[i], gain);
                bytes[i * 2] = (byte) scaled;
                bytes[i * 2 + 1] = (byte) (scaled >> 8);
            }
            return bytes;
        }
    }

    /** 실제 BGM 장치(음량을 곱하는 부분)를 그대로 쓰되, 소리 장치 대신 테스트에 프레임을 넘긴다 */
    private static final class CapturingDevice extends VolumeAudioDevice {
        private final VolumeTest test;

        CapturingDevice(VolumeTest test) {
            this.test = test;
        }

        @Override
        protected void writeImpl(short[] samples, int offs, int len) {
            if (!test.capturing) {
                return;
            }
            short[] raw = Arrays.copyOfRange(samples, offs, offs + len);
            byte[] written = Arrays.copyOf(toByteArray(samples, offs, len), len * 2);
            try {
                test.frames.offer(new Frame(raw, written), 2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** 열 때 넘긴 소리 바이트를 opened에 적는 Clip (소리 장치 없이) */
    private static Clip recordingClip(List<byte[]> opened) {
        return (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(), new Class<?>[]{Clip.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "open" -> {
                        if (args != null && args.length == 4 && args[0] instanceof AudioFormat) {
                            byte[] data = (byte[]) args[1];
                            opened.add(Arrays.copyOfRange(data, (int) args[2], (int) args[2] + (int) args[3]));
                        }
                        yield null;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "RecordingClip";
                    default -> zero(method.getReturnType());
                });
    }

    private static Object zero(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        return null;
    }
}

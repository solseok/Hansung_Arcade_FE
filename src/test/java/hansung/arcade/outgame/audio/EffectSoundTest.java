package hansung.arcade.outgame.audio;

import hansung.arcade.ingame.GameSound;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 인게임 효과음과 아웃게임 버튼 소리 파일이 모두 있고, 겹쳐 낼 수 있게 PCM으로 풀린다.
 * 소리 장치를 열지 못해도 실패가 쌓여 효과음이 막히지 않고, 음소거하면 줄 서 있던 소리도 내지 않는다.
 */
class EffectSoundTest {

    /** 아웃게임에서 내는 효과음 (버튼·로그인·준비·시작·나가기·경기 끝) */
    private static final List<String> OUTGAME = List.of(
            Sound.CLICK, "Bt_c_GameStart", "Bt_c_GameReadyOn", "Bt_c_GameReadyOff", "Bt_c_Out",
            "Bt_c_SelectAIBattleChar", "Bt_c_CheckBox", "Bt_c_Success", "Bt_c_GameStartNotAble", "Ntc_GameEnd");

    @Test
    void 인게임_효과음이_모두_리소스에_있고_PCM으로_풀린다() {
        for (String key : GameSound.keys()) {
            assertDecodes(key);
        }
    }

    @Test
    void 아웃게임_버튼_소리가_모두_리소스에_있고_PCM으로_풀린다() {
        for (String key : OUTGAME) {
            assertDecodes(key);
        }
    }

    @Test
    void 버튼_클릭음은_카운트다운_삐_소리가_아니다() {
        assertEquals("Bt_c_Pop", Sound.CLICK);
    }

    @Test
    void 없는_소리는_빈_값이라_게임이_멈추지_않는다() {
        assertTrue(Sound.decode("no-such-sound").isEmpty());
    }

    @Nested
    class 재생 {

        @BeforeEach
        void 앞_테스트의_소리를_비운다() throws Exception {
            Sound.awaitEffects();
            Sound.stopAll();
        }

        @AfterEach
        void 원래대로() throws Exception {
            Sound.awaitEffects();
            Sound.useClips(Sound.SYSTEM_CLIPS);
            Sound.setMuted(false);
            Sound.stopAll();
        }

        @Test
        void 열지_못한_효과음은_닫고_울리는_소리로_세지_않아_다음_소리가_난다() {
            AtomicInteger closed = new AtomicInteger();
            Sound.useClips(() -> FakeClip.failing("open", closed));
            for (int i = 0; i <= Sound.MAX_EFFECTS; i++) {
                Sound.playNow(Sound.CLICK);
            }
            assertEquals(Sound.MAX_EFFECTS + 1, closed.get(), "열지 못한 Clip을 닫지 않았습니다");
            assertEquals(0, Sound.playingCount());

            AtomicInteger opened = new AtomicInteger();
            Sound.useClips(() -> {
                opened.incrementAndGet();
                return FakeClip.working(new AtomicInteger());
            });
            Sound.playNow(Sound.CLICK);
            assertEquals(1, opened.get());
            assertEquals(1, Sound.playingCount(), "실패가 쌓여 효과음이 나지 않습니다");
        }

        @Test
        void 틀지_못한_효과음도_닫고_세지_않는다() {
            AtomicInteger closed = new AtomicInteger();
            Sound.useClips(() -> FakeClip.failing("start", closed));
            Sound.playNow(Sound.CLICK);
            assertEquals(1, closed.get());
            assertEquals(0, Sound.playingCount());
        }

        @Test
        void 줄_서_있는_사이_음소거되면_내지_않는다() {
            AtomicInteger opened = new AtomicInteger();
            Sound.useClips(() -> {
                opened.incrementAndGet();
                return FakeClip.working(new AtomicInteger());
            });
            // play로 줄을 세운 뒤 효과음 스레드가 꺼내기 전에 음소거한 것과 같다
            Sound.setMuted(true);
            Sound.playNow(Sound.CLICK);
            assertEquals(0, opened.get());
            assertEquals(0, Sound.playingCount());
        }
    }

    /** 소리 장치 없이 쓰는 Clip. failOn 메서드("open"·"start")에서 실패하고, 닫힌 수를 센다 */
    private static final class FakeClip {
        static Clip working(AtomicInteger closed) {
            return create(null, closed);
        }

        static Clip failing(String failOn, AtomicInteger closed) {
            return create(failOn, closed);
        }

        private static Clip create(String failOn, AtomicInteger closed) {
            return (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(), new Class<?>[]{Clip.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if (name.equals(failOn)) {
                            if (name.equals("open")) {
                                throw new LineUnavailableException("테스트: 소리 장치 없음");
                            }
                            throw new IllegalStateException("테스트: 틀지 못함");
                        }
                        return switch (name) {
                            case "close" -> {
                                closed.incrementAndGet();
                                yield null;
                            }
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            case "toString" -> "FakeClip";
                            default -> defaultValue(method.getReturnType());
                        };
                    });
        }

        private static Object defaultValue(Class<?> type) {
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
            if (type == double.class) {
                return 0d;
            }
            return null;
        }
    }

    private static void assertDecodes(String key) {
        Optional<Sound.Pcm> pcm = Sound.decode(key);
        assertTrue(pcm.isPresent(), "소리를 풀지 못했습니다: " + key);
        assertEquals(16, pcm.get().format().getSampleSizeInBits(), key);
        assertTrue(pcm.get().seconds() > 0.05 && pcm.get().seconds() < 5, key + " 길이 " + pcm.get().seconds());
    }
}

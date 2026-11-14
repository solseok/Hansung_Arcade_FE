package hansung.arcade.core.game;

import hansung.arcade.core.boot.BootException;
import hansung.arcade.core.boot.BootStep;
import hansung.arcade.core.boot.NetworkStep;
import hansung.arcade.core.boot.PropertiesStep;
import hansung.arcade.core.boot.ResourceStep;
import hansung.arcade.core.boot.StartScreenStep;
import hansung.arcade.core.boot.SwingStep;
import org.junit.jupiter.api.Test;

import java.awt.AWTError;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameApplicationTest {

    private record RecordingStep(String name, List<String> log, Runnable action) implements BootStep {
        @Override
        public void run(GameContext context) {
            log.add(name);
            action.run();
        }
    }

    @Test
    void 단계를_순서대로_실행하고_앞_단계가_등록한_객체를_뒤_단계가_쓴다() {
        List<String> log = new ArrayList<>();
        BootStep producer = new BootStep() {
            @Override
            public String name() {
                return "생산";
            }

            @Override
            public void run(GameContext context) {
                log.add("생산");
                context.register(String.class, "준비됨");
            }
        };
        BootStep consumer = new BootStep() {
            @Override
            public String name() {
                return "소비";
            }

            @Override
            public void run(GameContext context) {
                log.add("소비:" + context.get(String.class));
            }
        };

        new GameApplication(GameApplicationTest.class, List.of(producer, consumer)).boot();

        assertEquals(List.of("생산", "소비:준비됨"), log);
    }

    @Test
    void 실패한_단계에서_멈추고_단계_이름을_알려준다() {
        List<String> log = new ArrayList<>();
        List<BootStep> steps = List.of(
                new RecordingStep("첫째", log, () -> {
                }),
                new RecordingStep("둘째", log, () -> {
                    throw new IllegalStateException("고장");
                }),
                new RecordingStep("셋째", log, () -> {
                }));

        BootException e = assertThrows(BootException.class,
                () -> new GameApplication(GameApplicationTest.class, steps).boot());

        assertEquals(List.of("첫째", "둘째"), log);
        assertEquals("둘째", e.stepName());
        assertInstanceOf(IllegalStateException.class, e.getCause());
    }

    @Test
    void 실패_원인에_메시지가_없으면_예외_이름을_알려준다() {
        List<BootStep> steps = List.of(new RecordingStep("고장", new ArrayList<>(), () -> {
            throw new IllegalStateException();
        }));

        BootException e = assertThrows(BootException.class,
                () -> new GameApplication(GameApplicationTest.class, steps).boot());

        assertTrue(e.getMessage().contains("IllegalStateException"), e.getMessage());
        assertFalse(e.getMessage().contains("null"), e.getMessage());
    }

    @Test
    void 화면_연결이나_클래스_초기화_오류도_단계_이름과_함께_알려준다() {
        for (Error error : List.of(new AWTError("화면에 연결할 수 없음"), new NoClassDefFoundError("없는 클래스"))) {
            List<BootStep> steps = List.of(new RecordingStep("고장", new ArrayList<>(), () -> {
                throw error;
            }));

            BootException e = assertThrows(BootException.class,
                    () -> new GameApplication(GameApplicationTest.class, steps).boot());

            assertEquals("고장", e.stepName());
            assertSame(error, e.getCause());
        }
    }

    @Test
    void 메모리_부족_같은_JVM_오류는_감싸지_않고_그대로_던진다() {
        OutOfMemoryError error = new OutOfMemoryError("테스트용");
        List<BootStep> steps = List.of(new RecordingStep("고장", new ArrayList<>(), () -> {
            throw error;
        }));

        assertSame(error, assertThrows(OutOfMemoryError.class,
                () -> new GameApplication(GameApplicationTest.class, steps).boot()));
    }

    @Test
    void 실행_인자가_컨텍스트로_전달된다() {
        List<String> received = new ArrayList<>();
        BootStep step = new BootStep() {
            @Override
            public String name() {
                return "인자 확인";
            }

            @Override
            public void run(GameContext context) {
                received.addAll(context.args());
            }
        };

        new GameApplication(GameApplicationTest.class, List.of(step)).boot("--a=1", "--b=2");

        assertEquals(List.of("--a=1", "--b=2"), received);
    }

    @Test
    void 기본_구동_순서는_설정_네트워크_Swing_리소스_시작화면이다() {
        List<Class<?>> order = GameApplication.defaultSteps().stream()
                .<Class<?>>map(Object::getClass)
                .toList();

        assertEquals(List.of(PropertiesStep.class, NetworkStep.class, SwingStep.class,
                ResourceStep.class, StartScreenStep.class), order);
    }
}

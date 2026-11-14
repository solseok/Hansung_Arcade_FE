package hansung.arcade.core.game;

import hansung.arcade.core.boot.BootException;
import hansung.arcade.core.boot.BootStep;
import hansung.arcade.core.boot.NetworkStep;
import hansung.arcade.core.boot.PropertiesStep;
import hansung.arcade.core.boot.ResourceStep;
import hansung.arcade.core.boot.StartScreenStep;
import hansung.arcade.core.boot.SwingStep;
import hansung.arcade.core.ui.SwingThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.awt.AWTError;
import java.awt.GraphicsEnvironment;
import java.util.List;

/**
 * 게임 구동기. 부트 단계({@link BootStep})를 순서대로 실행해 애플리케이션을 조립한다.
 *
 * <pre>
 * public static void main(String[] args) {
 *     GameApplication.run(HansungArcadeApplication.class, args);
 * }
 * </pre>
 *
 * 단계 중 하나라도 실패하면 어느 단계에서 왜 실패했는지 알리고 종료한다.
 */
public final class GameApplication {

    private static final Logger log = LoggerFactory.getLogger(GameApplication.class);

    private final Class<?> primarySource;
    private final List<BootStep> steps;

    GameApplication(Class<?> primarySource, List<BootStep> steps) {
        this.primarySource = primarySource;
        this.steps = List.copyOf(steps);
    }

    public static void run(Class<?> primarySource, String... args) {
        Banner.print(primarySource, System.out);
        GameApplication application = new GameApplication(primarySource, defaultSteps());
        try {
            application.boot(args);
        } catch (BootException e) {
            try {
                reportFailure(e);
            } finally {
                // 대화상자를 띄우다 실패해도 반쯤 뜬 상태로 남지 않게 반드시 종료한다.
                System.exit(1);
            }
        }
    }

    static List<BootStep> defaultSteps() {
        return List.of(
                new PropertiesStep(),
                new NetworkStep(),
                new SwingStep(),
                new ResourceStep(),
                new StartScreenStep());
    }

    GameContext boot(String... args) {
        GameContext context = new GameContext(primarySource, List.of(args));
        long bootStart = System.nanoTime();
        log.info("{} 구동 시작", primarySource.getSimpleName());

        for (int i = 0; i < steps.size(); i++) {
            BootStep step = steps.get(i);
            long stepStart = System.nanoTime();
            try {
                step.run(context);
            } catch (Exception | LinkageError | AWTError e) {
                // 화면에 연결하지 못하거나(AWTError) 클래스를 초기화하지 못해도(LinkageError) 어느 단계인지 알린다.
                // 메모리 부족 같은 JVM 오류는 감싸지 않는다.
                throw new BootException(step.name(), e);
            }
            log.info("({}/{}) {} 완료 - {} ms", i + 1, steps.size(), step.name(), elapsedMillis(stepStart));
        }

        log.info("구동 완료 - {} ms", elapsedMillis(bootStart));
        return context;
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static void reportFailure(BootException e) {
        log.error(e.getMessage(), e.getCause());
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            SwingThread.runAndWait(() ->
                    JOptionPane.showMessageDialog(null, e.getMessage(), "실행 실패", JOptionPane.ERROR_MESSAGE));
        } catch (RuntimeException | AWTError dialogFailure) {
            // 화면에 연결하지 못해 실패한 경우에는 대화상자도 띄울 수 없다. 원인은 위에서 이미 로그로 남겼다.
            log.warn("실패 대화상자를 띄우지 못했습니다: {}", dialogFailure.toString());
        }
    }
}

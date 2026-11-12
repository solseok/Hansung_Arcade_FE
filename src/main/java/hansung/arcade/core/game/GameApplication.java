package hansung.arcade.core.game;

import hansung.arcade.core.boot.BootException;
import hansung.arcade.core.boot.BootStep;
import hansung.arcade.core.boot.NetworkStep;
import hansung.arcade.core.boot.PropertiesStep;
import hansung.arcade.core.boot.ResourceStep;
import hansung.arcade.core.boot.StartScreenStep;
import hansung.arcade.core.boot.SwingStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
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
            reportFailure(e);
            System.exit(1);
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
            } catch (Exception e) {
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
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "실행 실패", JOptionPane.ERROR_MESSAGE);
        }
    }
}

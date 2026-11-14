package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameProperties;
import hansung.arcade.core.game.GameWindow;
import hansung.arcade.core.ui.SwingThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.GraphicsEnvironment;

/**
 * 3단계: Swing/AWT를 초기화하고 메인 창({@link GameWindow})을 만들어 등록한다. 창은 아직 띄우지 않는다.
 */
public final class SwingStep implements BootStep {

    private static final Logger log = LoggerFactory.getLogger(SwingStep.class);

    @Override
    public String name() {
        return "Swing 초기화";
    }

    @Override
    public void run(GameContext context) {
        String title = context.get(GameProperties.class).get("app.title", defaultTitle(context.primarySource()));

        // AWT가 처음 로드되기 전에 지정해야 적용되는 값들
        System.setProperty("apple.awt.application.name", title);
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("화면이 없는 환경(headless)에서는 게임 창을 띄울 수 없습니다.");
        }

        GameWindow window = SwingThread.callAndWait(() -> {
            applyLookAndFeel();
            return new GameWindow(title);
        });
        context.register(GameWindow.class, window);
    }

    /** 대화상자 등 기본 Swing 컴포넌트를 OS 모양으로 맞춘다. 실패해도 기본 모양으로 계속 진행한다. */
    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            log.warn("시스템 Look and Feel 적용 실패, 기본값 사용: {}", e.getMessage());
        }
    }

    private static String defaultTitle(Class<?> primarySource) {
        return primarySource.getSimpleName().replaceFirst("Application$", "");
    }
}

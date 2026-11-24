package hansung.arcade.core.game;

import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * 게임의 유일한 창. 화면(로비, 대기실, 인게임 ...)을 갈아 끼우는 방식으로 쓴다.
 * 모든 메서드는 EDT에서 호출해야 한다.
 */
public final class GameWindow {

    private final JFrame frame;

    public GameWindow(String title) {
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
    }

    /** 화면을 교체한다. 이전 화면은 창에서 떼어지면서 removeNotify가 호출된다. */
    public void show(JComponent screen) {
        frame.setContentPane(screen);
        frame.pack();
        if (!frame.isVisible()) {
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
        screen.requestFocusInWindow();
    }
}

package hansung.arcade.outgame;

import hansung.arcade.core.game.*;

import java.awt.*;

import javax.swing.*;

/** 이전 결과 화면 진입점과의 호환 어댑터. 실제 경기는 GamePreviewScene의 오버레이를 사용한다. */
public final class ResultPanel extends JPanel implements Scene {
    private final ResultOverlay overlay = new ResultOverlay();

    public ResultPanel(SceneManager scenes) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(800, 600));
        add(overlay);
        overlay.onFinished(() -> scenes.show("waiting", null));
    }

    @Override
    public void enter(Object data) {
        overlay.show((GameResult) data, 1);
        overlay.tick(ResultOverlay.RISE_DELAY_MS + ResultOverlay.RISE_MS);
    }

    @Override
    public JComponent view() {
        return this;
    }
}

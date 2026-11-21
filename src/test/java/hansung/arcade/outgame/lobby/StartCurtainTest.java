package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.ui.WaitingRoomLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/** 방장이 게임을 시작하면 대기실이 0.3초 동안 검게 어두워진 뒤 게임으로 넘어간다 (크아 영상에서 잰 값) */
@ExtendWith(RunOnEdt.class)
class StartCurtainTest {

    @Test
    void 막은_0_3초_동안_투명에서_검정으로_짙어진다() {
        assertEquals(300, WaitingRoomPanel.START_FADE_MS);
        assertEquals(0f, WaitingRoomPanel.Curtain.alphaAt(0));
        assertEquals(0.5f, WaitingRoomPanel.Curtain.alphaAt(150), 1e-6);
        assertEquals(1f, WaitingRoomPanel.Curtain.alphaAt(300));
        assertEquals(1f, WaitingRoomPanel.Curtain.alphaAt(1000));
    }

    @Test
    void 막은_평소에는_숨어_있고_모든_구역보다_위에_있다() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        WaitingRoomPanel waiting = new WaitingRoomPanel(session, new SceneManager(screen -> { }),
                (parent, current) -> Optional.empty());

        JComponent curtain = SwingTree.named(waiting, "start-curtain", JComponent.class);
        assertFalse(curtain.isVisible());
        assertSame(curtain, waiting.getComponent(0));
        assertEquals(WaitingRoomLayout.WIDTH, curtain.getWidth());
        assertEquals(WaitingRoomLayout.HEIGHT, curtain.getHeight());
    }

    @Test
    void 화면에_보이지_않는_대기실은_기다리지_않고_바로_게임으로_넘어간다() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        List<String> shown = new ArrayList<>();
        SceneManager scenes = new SceneManager(screen -> { });
        WaitingRoomPanel waiting = new WaitingRoomPanel(session, scenes, (parent, current) -> Optional.empty());
        scenes.register("waiting", waiting);
        scenes.register("game", JLabel::new);
        scenes.onShow(shown::add);
        scenes.show("waiting", session.create("방", 8));

        SwingTree.named(waiting, "ready", AbstractButton.class).doClick();

        assertEquals(List.of("waiting", "game"), shown);
    }
}

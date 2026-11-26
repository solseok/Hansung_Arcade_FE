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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 방장이 게임을 시작하면 대기실이 0.3초 동안 검게 어두워진 뒤 게임으로 넘어간다 (크아 영상에서 잰 값).
 * 짙어지는 동안 키는 버리고, 그 사이 대기실을 떠나면 게임으로 넘어가지 않는다.
 */
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
    void 짙어지는_동안만_키를_버린다() {
        WaitingRoomPanel.Curtain curtain = new WaitingRoomPanel.Curtain();
        assertFalse(curtain.blocksKeys());

        curtain.fade(() -> { });
        try {
            assertTrue(curtain.isFading());
            assertTrue(curtain.blocksKeys());
        } finally {
            curtain.clear();
        }
        assertFalse(curtain.isFading());
        assertFalse(curtain.blocksKeys());
        assertFalse(curtain.isVisible());
    }

    @Test
    void 막이_짙어지는_사이_대기실을_떠나면_막을_걷고_게임으로_넘어가지_않는다() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        SceneManager scenes = new SceneManager(screen -> { });
        WaitingRoomPanel waiting = new WaitingRoomPanel(session, scenes, (parent, current) -> Optional.empty());
        scenes.register("waiting", waiting);
        scenes.register("lobby", JLabel::new);
        scenes.show("waiting", session.create("방", 8));
        WaitingRoomPanel.Curtain curtain = SwingTree.named(waiting, "start-curtain", WaitingRoomPanel.Curtain.class);
        List<String> launched = new ArrayList<>();
        // 화면에 띄우지 않은 대기실은 막 없이 바로 넘어가서, 막을 직접 짙게 한다
        curtain.fade(() -> launched.add("game"));

        scenes.show("lobby", null);

        assertFalse(curtain.isFading(), "떠난 방의 게임이 0.3초 뒤에 시작됩니다");
        assertFalse(curtain.blocksKeys(), "로비에서 키가 먹히지 않습니다");
        assertFalse(curtain.isVisible());
        assertEquals(List.of(), launched);
    }

    @Test
    void 화면에_보이지_않는_대기실은_기다리지_않고_바로_게임으로_넘어간다() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기", "꾸꾸");
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

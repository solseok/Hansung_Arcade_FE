package hansung.arcade.outgame;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.lobby.LobbyPanel;
import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.lobby.SampleRooms;
import hansung.arcade.outgame.lobby.WaitingRoomPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 아웃게임 버튼은 모두 직접 그린다. 맥 기본(Aqua) 버튼 테두리·바탕이 직접 그린 모양 위에 겹치면 안 된다.
 * 테스트만 보고 만든 최소 구현은 방 칸·◁▷·1P·2P·준비 버튼에서 기본 테두리를 끄지 않았다.
 */
@ExtendWith(RunOnEdt.class)
class ButtonLookTest {

    @Test
    void 로그인_로비_대기실의_버튼은_기본_테두리와_바탕을_그리지_않는다() {
        OutgameSession session = new OutgameSession(new MockLobbyService(SampleRooms.rooms()));
        SceneManager scenes = new SceneManager(screen -> { });
        LoginPanel login = new LoginPanel(session, scenes);
        session.login("부기");
        LobbyPanel lobby = new LobbyPanel(session, scenes, (parent, room) -> true);
        WaitingRoomPanel waiting = new WaitingRoomPanel(session, scenes, (parent, current) -> Optional.empty());
        scenes.register("lobby", lobby);
        scenes.register("waiting", waiting);
        scenes.show("lobby", null);
        scenes.show("waiting", session.create("버튼 확인", 8));

        List<String> problems = new ArrayList<>();
        for (JComponent screen : new JComponent[]{login, lobby, waiting}) {
            List<AbstractButton> buttons = SwingTree.all(screen, AbstractButton.class).stream()
                    // 스크롤 막대·콤보 상자 안의 화살표 버튼은 Swing 부품이라 뺀다
                    .filter(button -> SwingUtilities.getAncestorOfClass(JScrollBar.class, button) == null
                            && SwingUtilities.getAncestorOfClass(JComboBox.class, button) == null)
                    .toList();
            assertFalse(buttons.isEmpty());
            for (AbstractButton button : buttons) {
                if (button.isBorderPainted() || button.isContentAreaFilled()) {
                    problems.add(screen.getClass().getSimpleName() + " " + describe(button));
                }
            }
        }
        assertTrue(problems.isEmpty(), "기본 테두리·바탕을 그리는 버튼: " + problems);
    }

    private static String describe(AbstractButton button) {
        String name = button.getName() != null ? button.getName() : button.getText();
        return button.getClass().getSimpleName() + "(" + name + ")";
    }
}

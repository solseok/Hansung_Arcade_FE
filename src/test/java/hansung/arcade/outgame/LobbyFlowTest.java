package hansung.arcade.outgame;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.outgame.lobby.LobbyPanel;
import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.lobby.Room;
import hansung.arcade.outgame.lobby.WaitingRoomPanel;
import org.junit.jupiter.api.Test;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyFlowTest {
    @Test
    void 입장제한과_중복요청_퇴장시_인원복구() {
        MockLobbyService service = new MockLobbyService();
        assertThrows(IllegalStateException.class, () -> service.join(4));
        assertThrows(IllegalStateException.class, () -> service.join(3));
        assertThrows(IllegalStateException.class, () -> service.join(999));
        assertEquals(2, service.join(1).players());
        assertFalse(service.isHost());
        assertThrows(IllegalStateException.class, () -> service.join(1));
        service.leave();
        service.leave();
        assertEquals(1, service.rooms().getFirst().players());
        assertThrows(IllegalArgumentException.class, () -> service.create("방", 1));
        assertThrows(IllegalArgumentException.class, () -> service.create("방", 9));
        assertEquals(2, service.create("둘이서", 2).capacity());
    }

    @Test
    void 탭필터_채팅_입장_퇴장_재입장() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MockLobbyService service = new MockLobbyService();
            AtomicReference<JComponent> displayed = new AtomicReference<>();
            SceneManager scenes = new SceneManager(displayed::set);
            LobbyPanel lobby = new LobbyPanel(service, scenes);
            WaitingRoomPanel waiting = new WaitingRoomPanel(service, scenes);
            scenes.register("lobby", lobby);
            scenes.register("waiting", waiting);
            scenes.show("lobby", null);
            JTabbedPane tabs = find(lobby, JTabbedPane.class);
            assertFalse(button(lobby, "이전쪽").isEnabled());
            assertTrue(button(lobby, "다음쪽").isEnabled());
            button(lobby, "다음쪽").doClick();
            assertTrue(button(lobby, "이전쪽").isEnabled());
            assertFalse(button(lobby, "다음쪽").isEnabled());
            tabs.setSelectedIndex(1);
            assertFalse(button(lobby, "이전쪽").isEnabled());
            assertFalse(button(lobby, "입장").isEnabled());
            tabs.setSelectedIndex(0);
            assertEquals(null, find(lobby, JTextPane.class));
            button(lobby, "바로시작").doClick();
            assertSame(waiting, displayed.get());
            assertEquals(2, service.rooms().getFirst().players());
            JTextField input = find(waiting, JTextField.class);
            input.setText("안녕하세요");
            input.postActionEvent();
            assertTrue(find(waiting, JTextPane.class).getText().contains("나: 안녕하세요"));
            button(waiting, "로비로 나가기").doClick();
            assertSame(lobby, displayed.get());
            assertEquals(1, service.rooms().getFirst().players());
            Room created = service.create("새 방");
            assertEquals(1, created.players());
            assertEquals(8, created.capacity());
            assertTrue(service.isHost());
            assertThrows(IllegalStateException.class, service::quickJoin);
            service.leave();
            assertFalse(service.isHost());
            assertFalse(service.rooms().stream().anyMatch(room -> room.number() == created.number()));
            assertThrows(IllegalArgumentException.class, () -> service.create(" "));
        });
    }

    private static JButton button(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (child instanceof Container container) {
                JButton found = button(container, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T> T find(Container parent, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T found = find(container, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

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
    void 준비검사와_맵_게임상태_복귀동기화() {
        MockLobbyService service = new MockLobbyService();
        OutgameSession session = new OutgameSession(service);
        assertFalse(session.canStart());
        Room created = service.create("준비 검사", 8);
        // 서버에서 두 참가자 스냅샷을 받은 상황을 목업으로 재현한다.
        session.enter(new Room(created.number(), created.title(), 2, 8, created.map(), created.status()));
        session.setParticipantReady(2, false);
        assertFalse(session.canStart());
        assertThrows(IllegalStateException.class, session::start);
        session.setParticipantReady(2, true);
        assertTrue(session.canStart());
        assertThrows(IllegalArgumentException.class, () -> session.setParticipantReady(9, true));
        session.selectMap("patrit/patrit14");
        assertEquals("패트릿 14", service.rooms().getLast().map());
        session.start();
        assertEquals(Room.Status.PLAYING, service.rooms().getLast().status());
        assertThrows(IllegalStateException.class, session::start);
        session.returnToWaiting();
        assertEquals(Room.Status.WAITING, service.rooms().getLast().status());
        assertTrue(session.canStart());
        session.leave();
        assertFalse(session.canStart());
        service.join(1);
        assertThrows(IllegalStateException.class, () -> service.updateRoom("캠프 08", Room.Status.PLAYING));
    }

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
    void 통합목록_페이지_채팅_입장_퇴장_재입장() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MockLobbyService service = new MockLobbyService();
            OutgameSession session = new OutgameSession(service);
            session.login("부기");
            AtomicReference<JComponent> displayed = new AtomicReference<>();
            SceneManager scenes = new SceneManager(displayed::set);
            LobbyPanel lobby = new LobbyPanel(session, scenes);
            WaitingRoomPanel waiting = new WaitingRoomPanel(session, scenes);
            scenes.register("lobby", lobby);
            scenes.register("waiting", waiting);
            scenes.show("lobby", null);
            assertEquals(null, find(lobby, JTabbedPane.class));
            assertFalse(button(lobby, "이전쪽").isEnabled());
            assertTrue(button(lobby, "다음쪽").isEnabled());
            button(lobby, "다음쪽").doClick();
            assertTrue(button(lobby, "이전쪽").isEnabled());
            assertTrue(button(lobby, "다음쪽").isEnabled());
            assertTrue(hasLabel(lobby, "WAITING"));
            assertTrue(hasLabel(lobby, "PLAYING"));
            button(lobby, "다음쪽").doClick();
            assertFalse(button(lobby, "다음쪽").isEnabled());
            assertFalse(hasLabel(lobby, "WAITING"));
            assertTrue(hasLabel(lobby, "PLAYING"));
            assertEquals(null, button(lobby, "입장"));
            button(lobby, "이전쪽").doClick();
            button(lobby, "이전쪽").doClick();
            assertEquals(null, find(lobby, JTextPane.class));
            button(lobby, "바로시작").doClick();
            assertSame(waiting, displayed.get());
            assertEquals(2, service.rooms().getFirst().players());
            JTextField input = find(waiting, JTextField.class);
            input.setText("안녕하세요");
            input.postActionEvent();
            assertTrue(find(waiting, JTextPane.class).getText().contains("부기: 안녕하세요"));
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

    @Test
    void 로그인검사와_방장시작_결과복귀시_방유지() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MockLobbyService service = new MockLobbyService();
            OutgameSession session = new OutgameSession(service);
            assertThrows(IllegalArgumentException.class, () -> session.login(" "));
            session.login("전솔");
            session.enter(service.join(1));
            assertThrows(IllegalStateException.class, session::start);
            assertThrows(IllegalStateException.class, () -> session.selectMap("camp/camp08"));
            session.leave();
            Room room = service.create("8인 테스트", 8);
            AtomicReference<JComponent> displayed = new AtomicReference<>();
            SceneManager scenes = new SceneManager(displayed::set);
            WaitingRoomPanel waiting = new WaitingRoomPanel(session, scenes);
            ResultPanel result = new ResultPanel(scenes);
            GamePreviewScene game = new GamePreviewScene(scenes,
                    hansung.arcade.ingame.hud.HudSkin.load(hansung.arcade.ingame.hud.HudFonts.load()),
                    hansung.arcade.ingame.render.CharacterSprites.load());
            scenes.register("waiting", waiting);
            scenes.register("game", game);
            scenes.register("result", result);
            scenes.show("waiting", room);
            session.selectMap("village/village10");
            assertEquals("village/village10", session.mapId());
            assertEquals("전솔", session.players().getFirst().nickname());
            button(waiting, "게임 시작").doClick();
            assertSame(game.view(), displayed.get());
            button(game.view(), "목업 결과 확인").doClick();
            assertSame(result, displayed.get());
            button(result, "대기실로 돌아가기").doClick();
            assertSame(waiting, displayed.get());
            assertTrue(service.isHost());
            assertEquals(room.number(), session.room().number());
            assertEquals(1, service.rooms().stream().filter(item -> item.number() == room.number())
                    .findFirst().orElseThrow().players());
        });
    }

    private static boolean hasLabel(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof javax.swing.JLabel label && text.equals(label.getText())) {
                return true;
            }
            if (child instanceof Container container && hasLabel(container, text)) {
                return true;
            }
        }
        return false;
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

package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.ui.WaitingRoomLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 대기실 자리 열기·닫기(오프라인 목업). 방장이 빈 자리를 누르면 X로 닫혀 정원이 하나 줄고, 닫힌 자리를 누르면 다시 열린다.
 * 혼자서는 게임을 시작할 수 없다.
 */
@ExtendWith(RunOnEdt.class)
class WaitingRoomSlotTest {

    private MockLobbyService service;
    private OutgameSession session;
    private SceneManager scenes;
    private WaitingRoomPanel waiting;
    private final List<String> shown = new ArrayList<>();
    /** 맵선택 창에서 고를 맵 */
    private final AtomicReference<String> pick = new AtomicReference<>("village/village10");

    @BeforeEach
    void setUp() {
        service = new MockLobbyService();
        session = new OutgameSession(service);
        session.login("부기");
        scenes = new SceneManager(screen -> { });
        waiting = new WaitingRoomPanel(session, scenes, (parent, current) -> Optional.ofNullable(pick.get()));
        scenes.register("waiting", waiting);
        scenes.register("game", JLabel::new);
        scenes.onShow(shown::add);
    }

    private void enterAsHost(int capacity) {
        scenes.show("waiting", session.create("자리 테스트", capacity));
        waiting.setSize(WaitingRoomLayout.WIDTH, WaitingRoomLayout.HEIGHT);
        SwingTree.layout(waiting);
    }

    private PlayerSlot slot(int index) {
        return SwingTree.named(waiting, "slot-" + index, PlayerSlot.class);
    }

    private String count() {
        return SwingTree.named(waiting, "room-count", JLabel.class).getText();
    }

    private String chatLog() {
        return SwingTree.named(waiting, "chat-log", JTextPane.class).getText();
    }

    private int capacityInLobbyList() {
        int number = session.room().number();
        return service.rooms().stream().filter(room -> room.number() == number).findFirst().orElseThrow().capacity();
    }

    /** 왼쪽 버튼으로 자리 가운데를 눌렀다 뗀다 */
    private static void click(Component target) {
        press(target, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK);
    }

    private static void press(Component target, int button, int downMask) {
        int x = target.getWidth() / 2;
        int y = target.getHeight() / 2;
        long now = System.currentTimeMillis();
        target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, now, downMask, x, y, 1, false, button));
        target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_RELEASED, now, 0, x, y, 1, false, button));
        target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_CLICKED, now, 0, x, y, 1, false, button));
    }

    @Test
    void 방장이_빈_자리를_누르면_X로_닫히고_정원이_하나_준다() {
        enterAsHost(8);

        click(slot(3));

        assertEquals(SlotState.CLOSED, slot(3).state());
        assertEquals("1/7", count());
        assertEquals(7, session.room().capacity());
        assertEquals(7, capacityInLobbyList(), "로비 목록의 정원도 준다");
        assertTrue(session.isSlotClosed(3));
    }

    @Test
    void 닫힌_자리를_다시_누르면_열리고_정원이_하나_는다() {
        enterAsHost(4);
        assertEquals(SlotState.CLOSED, slot(6).state());

        click(slot(6));

        assertEquals(SlotState.EMPTY, slot(6).state());
        assertEquals("1/5", count());
        click(slot(6));
        assertEquals(SlotState.CLOSED, slot(6).state());
        assertEquals("1/4", count());
    }

    @Test
    void 사람이_있는_자리는_눌러도_그대로다() {
        enterAsHost(8);

        click(slot(0));

        assertEquals(SlotState.HOST, slot(0).state());
        assertEquals("1/8", count());
        assertEquals(Cursor.DEFAULT_CURSOR, slot(0).getCursor().getType());
        assertEquals(Cursor.HAND_CURSOR, slot(1).getCursor().getType(), "방장은 빈 자리를 누를 수 있다");
    }

    @Test
    void 오른쪽_버튼으로는_자리를_바꾸지_않는다() {
        enterAsHost(8);

        press(slot(3), MouseEvent.BUTTON3, InputEvent.BUTTON3_DOWN_MASK);

        assertEquals(SlotState.EMPTY, slot(3).state());
    }

    @Test
    void 정원이_2명보다_줄게는_닫히지_않고_까닭을_채팅창에_알린다() {
        enterAsHost(2);

        click(slot(1));

        assertEquals(SlotState.EMPTY, slot(1).state());
        assertEquals("1/2", count());
        assertTrue(chatLog().contains("정원은 2명보다 줄일 수 없습니다."), chatLog());
    }

    @Test
    void 맵_최대_인원_밖_자리는_열리지_않는다() {
        enterAsHost(8);
        SwingTree.named(waiting, "map-select", AbstractButton.class).doClick();
        assertEquals("1/6", count());

        click(slot(6));

        assertEquals(SlotState.CLOSED, slot(6).state());
        assertEquals("1/6", count());
        assertTrue(chatLog().contains("빌리지 10은(는) 최대 6명이라 이 자리는 열 수 없습니다."), chatLog());
        assertEquals(Cursor.DEFAULT_CURSOR, slot(6).getCursor().getType(), "맵 밖 자리는 누를 수 없는 모양이다");
        assertEquals(Cursor.HAND_CURSOR, slot(5).getCursor().getType());
    }

    @Test
    void 방장이_닫은_자리는_맵을_바꿨다_되돌려도_닫힌_채다() {
        enterAsHost(8);
        click(slot(3));
        AbstractButton select = SwingTree.named(waiting, "map-select", AbstractButton.class);

        select.doClick();
        assertEquals("1/5", count());
        pick.set("camp/camp08");
        select.doClick();

        assertEquals(SlotState.CLOSED, slot(3).state());
        assertEquals(SlotState.EMPTY, slot(6).state());
        assertEquals(SlotState.EMPTY, slot(7).state());
        assertEquals("1/7", count());
    }

    @Test
    void 맵을_바꿔_열린_자리가_빠져도_정원은_2명_아래로_떨어지지_않는다() {
        enterAsHost(2);
        click(slot(7));
        click(slot(1));
        assertEquals("1/2", count());

        SwingTree.named(waiting, "map-select", AbstractButton.class).doClick();

        assertEquals("1/2", count());
        assertEquals(SlotState.EMPTY, slot(1).state(), "방장이 닫은 가장 작은 빈자리를 연다");
        assertEquals(SlotState.CLOSED, slot(7).state());
    }

    @Test
    void 손님은_자리를_눌러도_아무_일도_없다() {
        scenes.show("waiting", session.join(2));
        waiting.setSize(WaitingRoomLayout.WIDTH, WaitingRoomLayout.HEIGHT);
        SwingTree.layout(waiting);
        String before = count();

        click(slot(4));

        assertEquals(SlotState.EMPTY, slot(4).state());
        assertEquals(before, count());
        assertEquals(Cursor.DEFAULT_CURSOR, slot(4).getCursor().getType());
        assertFalse(session.toggleSlot(4));
    }

    @Test
    void 목업_서비스도_서버와_같은_규칙으로_거절한다() {
        Room room = service.create("2P 방", 8, 2);

        IllegalStateException occupied = assertThrows(IllegalStateException.class, () -> service.setSlotOpen(1, false));
        assertEquals("사람이 있는 자리는 닫을 수 없습니다.", occupied.getMessage());
        assertThrows(IllegalArgumentException.class, () -> service.setSlotOpen(8, false));
        assertEquals(8, service.setSlotOpen(2, true).capacity(), "이미 열린 자리는 그대로다");
        assertEquals(List.of(), service.closedSlots(room));
        assertEquals(7, service.setSlotOpen(2, false).capacity());
        assertEquals(List.of(2), service.closedSlots(room));

        service.leave();
        service.join(1);
        IllegalStateException guest = assertThrows(IllegalStateException.class, () -> service.setSlotOpen(5, false));
        assertEquals("방장만 자리를 열고 닫을 수 있습니다.", guest.getMessage());
    }

    @Test
    void 혼자면_게임_시작을_눌러도_시작하지_않고_까닭을_채팅창에_알린다() {
        enterAsHost(8);
        AbstractButton start = SwingTree.named(waiting, "ready", AbstractButton.class);
        assertTrue(start.isEnabled(), "방장은 시작을 눌러 까닭을 들을 수 있다");
        assertFalse(session.canStart());

        start.doClick();

        assertEquals(List.of("waiting"), shown);
        assertEquals(Room.Status.WAITING, session.room().status());
        assertTrue(chatLog().contains("2명 이상 모여야 시작할 수 있습니다."), chatLog());
    }

    @Test
    void 한_컴퓨터_2인은_두_명이라_시작할_수_있다() {
        session.login("부기", "꾸꾸");
        enterAsHost(8);
        assertTrue(session.canStart());

        SwingTree.named(waiting, "ready", AbstractButton.class).doClick();

        assertEquals(List.of("waiting", "game"), shown);
    }

    @Test
    void 준비하지_않은_사람이_있으면_시작을_눌러도_까닭을_알린다() {
        // 1P와 목업 참가자 한 명이 있는 방. 목업은 준비한 채로 들어오므로 준비를 푼다
        scenes.show("waiting", service.create("목업과 함께", 8, 2));
        session.setParticipantReady(2, false);
        waiting.setSize(WaitingRoomLayout.WIDTH, WaitingRoomLayout.HEIGHT);
        SwingTree.layout(waiting);

        SwingTree.named(waiting, "ready", AbstractButton.class).doClick();

        assertEquals(List.of("waiting"), shown);
        assertTrue(chatLog().contains("모든 참가자가 준비해야 시작할 수 있습니다."), chatLog());
    }
}

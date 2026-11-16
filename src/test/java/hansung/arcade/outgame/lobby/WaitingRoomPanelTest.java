package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.Team;
import hansung.arcade.outgame.ui.OutgamePalette;
import hansung.arcade.outgame.ui.WaitingRoomLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 8. 대기실을 참고 화면(5번)처럼: 왼쪽에 방 제목줄·8자리·채팅, 오른쪽에 캐릭터·팀·맵·준비.
 * 자리마다 방장인지, 준비했는지를 글자와 색으로 보여 준다.
 */
@ExtendWith(RunOnEdt.class)
class WaitingRoomPanelTest {

    private MockLobbyService service;
    private OutgameSession session;
    private SceneManager scenes;
    private WaitingRoomPanel waiting;

    @BeforeEach
    void setUp() {
        service = new MockLobbyService();
        session = new OutgameSession(service);
        session.login("부기");
        scenes = new SceneManager(screen -> { });
        waiting = new WaitingRoomPanel(session, scenes, (parent, current) -> Optional.of("patrit/patrit14"));
        scenes.register("waiting", waiting);
    }

    private void enterAsHost(int capacity) {
        scenes.show("waiting", session.create("8인 테스트", capacity));
        layout();
    }

    /** 2번 방(목업 2명)에 손님으로 들어간다. 1번 자리가 나, 2번 자리가 목업 방장이다. */
    private void enterAsGuest() {
        scenes.show("waiting", session.join(2));
        layout();
    }

    private void layout() {
        waiting.setSize(WaitingRoomLayout.WIDTH, WaitingRoomLayout.HEIGHT);
        SwingTree.layout(waiting);
    }

    @Test
    void 칸들이_설계한_자리에_놓인다() {
        enterAsHost(8);
        Map<String, Box> expected = new LinkedHashMap<>();
        expected.put("top-bar", WaitingRoomLayout.TOP_BAR);
        expected.put("left-panel", WaitingRoomLayout.LEFT_PANEL);
        expected.put("room-header", WaitingRoomLayout.ROOM_HEADER);
        expected.put("slot-grid", WaitingRoomLayout.SLOT_GRID);
        expected.put("chat", WaitingRoomLayout.CHAT);
        expected.put("right-panel", WaitingRoomLayout.RIGHT_PANEL);
        expected.put("character-section", WaitingRoomLayout.CHARACTER_SECTION);
        expected.put("character-banner", WaitingRoomLayout.CHARACTER_BANNER);
        expected.put("team-section", WaitingRoomLayout.TEAM_SECTION);
        expected.put("map-section", WaitingRoomLayout.MAP_SECTION);
        expected.put("map-preview", WaitingRoomLayout.MAP_PREVIEW);
        expected.put("map-select", WaitingRoomLayout.MAP_SELECT);
        expected.put("ready-section", WaitingRoomLayout.READY_SECTION);
        expected.put("ready", WaitingRoomLayout.READY_BUTTON);
        expected.put("auto-ready", WaitingRoomLayout.AUTO_READY);
        expected.put("bottom-bar", WaitingRoomLayout.BOTTOM_BAR);
        expected.put("leave", WaitingRoomLayout.LEAVE);
        for (int i = 0; i < WaitingRoomLayout.SLOTS; i++) {
            expected.put("slot-" + i, WaitingRoomLayout.slot(i));
        }
        Mascot[] mascots = Mascot.values();
        for (int i = 0; i < mascots.length; i++) {
            expected.put("character-" + mascots[i].id(), WaitingRoomLayout.characterCell(i));
        }
        expected.put("character-random", WaitingRoomLayout.characterCell(mascots.length));
        for (Team team : Team.values()) {
            expected.put("team-" + team.name(), WaitingRoomLayout.teamSwatch(team.ordinal()));
        }
        for (int i = 0; i < WaitingRoomLayout.MAP_INFO_ROWS; i++) {
            expected.put("map-info-" + i, WaitingRoomLayout.mapInfo(i));
        }

        expected.forEach((name, box) -> assertEquals(rect(box),
                SwingTree.boundsIn(SwingTree.named(waiting, name, JComponent.class), waiting), name));
    }

    @Test
    void 구역마다_대기실_색이_칠해진다() {
        enterAsHost(8);
        BufferedImage screen = SwingTree.render(waiting);
        Box slot0 = WaitingRoomLayout.slot(0);
        Box slot1 = WaitingRoomLayout.slot(1);

        assertEquals(OutgamePalette.WAITING_FRAME, rgb(screen, 2, 300));
        assertEquals(OutgamePalette.WAITING_LEFT_PANEL, rgb(screen, 10, 300));
        assertEquals(OutgamePalette.WAITING_SECTION, rgb(screen, 23, 200));
        assertEquals(OutgamePalette.WAITING_RIGHT_PANEL, rgb(screen, 470, 266));
        assertEquals(OutgamePalette.WAITING_SECTION, rgb(screen, 482, 300));
        assertEquals(OutgamePalette.MAP_PREVIEW_BACKGROUND, rgb(screen, 490, 352));
        assertEquals(OutgamePalette.BOTTOM_BAR, rgb(screen, 400, 595));
        assertEquals(OutgamePalette.SLOT_OCCUPIED, rgb(screen, slot0.x() + 4, slot0.y() + 80));
        assertEquals(OutgamePalette.SLOT_NAME, rgb(screen, slot0.x() + 3, slot0.y() + 109));
        assertEquals(OutgamePalette.SLOT_STATUS, rgb(screen, slot0.x() + 3, slot0.y() + 128));
        assertEquals(OutgamePalette.SLOT_EMPTY, rgb(screen, slot1.x() + 4, slot1.y() + 80));
        Color ready = rgb(screen, WaitingRoomLayout.READY_BUTTON.x() + 8, WaitingRoomLayout.READY_BUTTON.y() + 26);
        assertTrue(ready.getRed() > 230 && ready.getGreen() > 140 && ready.getBlue() < 60, "준비 버튼이 주황이 아닙니다: " + ready);
    }

    @Test
    void 방_제목줄에_번호_제목_인원이_있다() {
        Room room = session.create("8인 테스트", 8);
        scenes.show("waiting", room);

        assertEquals("%03d".formatted(room.number()), SwingTree.named(waiting, "room-number", JLabel.class).getText());
        assertEquals("8인 테스트", SwingTree.named(waiting, "room-title", JLabel.class).getText());
        assertEquals("1/8", SwingTree.named(waiting, "room-count", JLabel.class).getText());
    }

    @Test
    void 방장은_방장_표시와_게임_시작_버튼을_본다() {
        enterAsHost(8);
        PlayerSlot me = slot(0);

        assertEquals(SlotState.HOST, me.state());
        assertEquals("부기", me.nickname());
        JLabel status = SwingTree.named(me, "slot-status", JLabel.class);
        assertEquals("방   장", status.getText());
        assertEquals(OutgamePalette.STATUS_HOST, status.getForeground());
        AbstractButton ready = SwingTree.named(waiting, "ready", AbstractButton.class);
        assertEquals("게임 시작", ready.getText());
        assertTrue(ready.isEnabled());
        assertTrue(SwingTree.named(waiting, "map-select", AbstractButton.class).isEnabled());
        assertFalse(SwingTree.named(waiting, "auto-ready", JCheckBox.class).isEnabled(), "방장은 준비할 필요가 없습니다.");
    }

    @Test
    void 손님은_준비를_누르면_준비_완료로_바뀌고_다시_누르면_취소된다() {
        enterAsGuest();
        AbstractButton ready = SwingTree.named(waiting, "ready", AbstractButton.class);

        assertState(0, SlotState.WAITING, "준   비", OutgamePalette.STATUS_IDLE);
        assertState(1, SlotState.HOST, "방   장", OutgamePalette.STATUS_HOST);
        assertState(2, SlotState.READY, "준   비", OutgamePalette.STATUS_READY);
        assertEquals("준비", ready.getText());
        assertFalse(SwingTree.named(waiting, "map-select", AbstractButton.class).isEnabled(), "손님은 맵을 고를 수 없습니다.");

        ready.doClick();
        assertTrue(session.ready());
        assertState(0, SlotState.READY, "준   비", OutgamePalette.STATUS_READY);
        assertEquals("준비 취소", ready.getText());

        ready.doClick();
        assertFalse(session.ready());
        assertState(0, SlotState.WAITING, "준   비", OutgamePalette.STATUS_IDLE);
    }

    /** 14. 상태 글자는 자간을 넓힌 "준   비"·"방   장". 준비했으면 불이 들어온 색, 아니면 꺼진 색이다 */
    @Test
    void 준비는_같은_글자에_불이_켜지고_꺼지는_것으로_구분한다() {
        assertEquals("준   비", SlotState.READY.label());
        assertEquals(SlotState.READY.label(), SlotState.WAITING.label());
        assertEquals("방   장", SlotState.HOST.label());
        assertEquals(OutgamePalette.STATUS_READY, SlotState.READY.color());
        assertEquals(OutgamePalette.STATUS_IDLE, SlotState.WAITING.color());
    }

    @Test
    void 정원_밖_자리는_닫힌_자리_정원_안_빈자리는_빈_자리() {
        enterAsHost(4);

        for (int i = 1; i < 4; i++) {
            assertEquals(SlotState.EMPTY, slot(i).state(), "slot " + i);
        }
        for (int i = 4; i < 8; i++) {
            assertEquals(SlotState.CLOSED, slot(i).state(), "slot " + i);
        }
    }

    @Test
    void 캐릭터를_고르면_세션_배너_칸_선택이_바뀐다() {
        enterAsHost(8);

        SwingTree.named(waiting, "character-kkukku", JToggleButton.class).doClick();

        assertEquals(Mascot.KKUKKU, session.character());
        assertTrue(SwingTree.named(waiting, "character-kkukku", JToggleButton.class).isSelected());
        assertFalse(SwingTree.named(waiting, "character-sangsangbugi", JToggleButton.class).isSelected());
        assertEquals("꾸꾸", SwingTree.named(waiting, "character-name", JLabel.class).getText());
    }

    @Test
    void 랜덤을_고르면_다섯_캐릭터_중_하나가_골라진다() {
        enterAsHost(8);

        SwingTree.named(waiting, "character-random", JToggleButton.class).doClick();

        Mascot picked = session.character();
        assertTrue(SwingTree.named(waiting, "character-" + picked.id(), JToggleButton.class).isSelected());
    }

    @Test
    void 남는_캐릭터_칸은_잠겨_있다() {
        enterAsHost(8);

        for (int i = Mascot.values().length + 1; i < WaitingRoomLayout.CHARACTER_CELLS; i++) {
            AbstractButton locked = SwingTree.named(waiting, "character-locked-" + i, AbstractButton.class);
            assertFalse(locked.isEnabled());
        }
    }

    @Test
    void 팀을_고르면_세션과_내_자리의_팀색이_바뀐다() {
        enterAsHost(8);

        SwingTree.named(waiting, "team-GREEN", JToggleButton.class).doClick();

        assertEquals(Team.GREEN, session.team());
        assertTrue(SwingTree.named(waiting, "team-GREEN", JToggleButton.class).isSelected());
        assertEquals(Team.GREEN.color(), SwingTree.named(slot(0), "slot-team", JLabel.class).getBackground());
    }

    @Test
    void 방장이_맵을_고르면_미리보기와_정보가_바뀐다() {
        enterAsHost(8);

        SwingTree.named(waiting, "map-select", AbstractButton.class).doClick();

        assertEquals("patrit/patrit14", session.mapId());
        assertEquals("패트릿 14", SwingTree.named(waiting, "map-name", JLabel.class).getText());
        assertTrue(SwingTree.named(waiting, "map-info-0", JLabel.class).getText().contains("패트릿 14"));
        assertTrue(SwingTree.named(waiting, "map-info-1", JLabel.class).getText().contains("1/8"));
    }

    @Test
    void 자동_준비를_켜면_손님은_바로_준비된다() {
        enterAsGuest();

        SwingTree.named(waiting, "auto-ready", JCheckBox.class).doClick();

        assertTrue(session.ready());
        assertState(0, SlotState.READY, "준   비", OutgamePalette.STATUS_READY);
    }

    private void assertState(int index, SlotState state, String text, Color color) {
        PlayerSlot slot = slot(index);
        JLabel status = SwingTree.named(slot, "slot-status", JLabel.class);
        assertEquals(state, slot.state(), "slot " + index);
        assertEquals(text, status.getText(), "slot " + index);
        assertEquals(color, status.getForeground(), "slot " + index);
    }

    private PlayerSlot slot(int index) {
        return SwingTree.named(waiting, "slot-" + index, PlayerSlot.class);
    }

    private static Rectangle rect(Box box) {
        return new Rectangle(box.x(), box.y(), box.width(), box.height());
    }

    private static Color rgb(BufferedImage image, int x, int y) {
        return new Color(image.getRGB(x, y) & 0xFFFFFF);
    }
}

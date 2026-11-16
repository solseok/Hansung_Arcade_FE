package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.entity.CharacterColor;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.ingame.render.CharacterRecolor;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.outgame.GameMaps;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.Team;
import hansung.arcade.outgame.ui.OutgamePalette;
import hansung.arcade.outgame.ui.StarRating;
import hansung.arcade.outgame.ui.WaitingRoomLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 8. 대기실을 참고 화면(5번)처럼: 왼쪽에 방 제목줄·8자리·채팅, 오른쪽에 캐릭터·팀·맵·준비.
 * 자리마다 방장인지, 준비했는지를 글자와 색으로 보여 준다.
 */
@ExtendWith(RunOnEdt.class)
class WaitingRoomPanelTest {

    private static final CharacterSprites SPRITES = CharacterSprites.load();

    private MockLobbyService service;
    private OutgameSession session;
    private SceneManager scenes;
    private WaitingRoomPanel waiting;
    /** 맵선택 창에서 고를 맵 */
    private final AtomicReference<String> pick = new AtomicReference<>("patrit/patrit14");

    @BeforeEach
    void setUp() {
        service = new MockLobbyService();
        session = new OutgameSession(service);
        session.login("부기");
        scenes = new SceneManager(screen -> { });
        waiting = new WaitingRoomPanel(session, scenes, (parent, current) -> Optional.ofNullable(pick.get()));
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

    /** 15. 캐릭터 배너에는 이름만 쓰고 능력치(물풍선·물줄기·속도)는 쓰지 않는다 */
    @Test
    void 캐릭터_배너에는_능력치가_없다() {
        enterAsHost(8);
        JComponent banner = SwingTree.named(waiting, "character-banner", JComponent.class);

        assertEquals("상상부기", SwingTree.named(banner, "character-name", JLabel.class).getText());
        for (String text : SwingTree.texts(banner)) {
            assertFalse(text.contains("물풍선") || text.contains("물줄기") || text.contains("속도"), text);
        }
    }

    /** 16. ?는 랜덤 캐릭터 선택이다. 고르면 ?가 골라진 채로 남고, 실제 캐릭터는 게임을 시작할 때 정한다 */
    @Test
    void 물음표는_랜덤_캐릭터_선택이다() {
        enterAsHost(8);
        JToggleButton random = SwingTree.named(waiting, "character-random", JToggleButton.class);

        random.doClick();

        assertTrue(session.randomCharacter());
        assertTrue(random.isSelected());
        for (Mascot mascot : Mascot.values()) {
            assertFalse(SwingTree.named(waiting, "character-" + mascot.id(), JToggleButton.class).isSelected(),
                    mascot + "이(가) 골라져 있으면 안 됩니다.");
        }
        assertEquals("랜덤", SwingTree.named(waiting, "character-name", JLabel.class).getText());
        assertNotNull(session.start().players().getFirst().character(), "게임을 시작할 때는 캐릭터가 정해져야 합니다.");

        SwingTree.named(waiting, "character-kkukku", JToggleButton.class).doClick();
        assertFalse(session.randomCharacter());
        assertEquals(Mascot.KKUKKU, session.character());
    }

    @Test
    void 남는_캐릭터_칸은_잠겨_있다() {
        enterAsHost(8);

        for (int i = Mascot.values().length + 1; i < WaitingRoomLayout.CHARACTER_CELLS; i++) {
            AbstractButton locked = SwingTree.named(waiting, "character-locked-" + i, AbstractButton.class);
            assertFalse(locked.isEnabled());
        }
    }

    /** 19. 팀 색은 자리의 작은 네모가 아니라, 캐릭터 그림의 고정색(옷·모자 등)을 팀 색으로 다시 칠해 보여 준다 */
    @Test
    void 팀을_고르면_내_자리_캐릭터의_고정색이_팀_색으로_바뀐다() {
        enterAsHost(8);

        SwingTree.named(waiting, "team-GREEN", JToggleButton.class).doClick();

        assertEquals(Team.GREEN, session.team());
        assertTrue(SwingTree.named(waiting, "team-GREEN", JToggleButton.class).isSelected());
        assertTrue(SwingTree.find(slot(0), "slot-team").isEmpty(), "팀 색 네모는 두지 않습니다.");
        assertSameImage(recolored(Mascot.SANGSANGBUGI, CharacterColor.GREEN), shownSprite(0));
    }

    @Test
    void 목업_참가자도_자기_팀_색으로_칠한다() {
        enterAsGuest();

        // 2번 자리는 목업 방장(2번 참가자): 꾸꾸, 파랑 팀
        assertSameImage(recolored(Mascot.KKUKKU, CharacterColor.BLUE), shownSprite(1));
    }

    @Test
    void 팀_8색은_캐릭터_고정색_8색과_짝을_이룬다() {
        assertEquals(List.of(CharacterColor.RED, CharacterColor.YELLOW, CharacterColor.ORANGE, CharacterColor.GREEN,
                        CharacterColor.SKY, CharacterColor.BLUE, CharacterColor.PURPLE, CharacterColor.MAGENTA),
                List.of(Team.values()).stream().map(Team::characterColor).toList());
    }

    private BufferedImage shownSprite(int index) {
        JLabel sprite = SwingTree.named(slot(index), "slot-sprite", JLabel.class);
        return (BufferedImage) ((ImageIcon) sprite.getIcon()).getImage();
    }

    private static BufferedImage recolored(Mascot mascot, CharacterColor color) {
        return CharacterRecolor.recolor(SPRITES.frame(mascot, Direction.DOWN, false, 0), mascot, color);
    }

    private static void assertSameImage(BufferedImage expected, BufferedImage actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertEquals(expected.getRGB(x, y), actual.getRGB(x, y), "(" + x + "," + y + ")");
            }
        }
    }

    @Test
    void 방장이_맵을_고르면_미리보기와_정보가_바뀐다() {
        enterAsHost(8);

        SwingTree.named(waiting, "map-select", AbstractButton.class).doClick();

        assertEquals("patrit/patrit14", session.mapId());
        assertEquals("패트릿 14", SwingTree.named(waiting, "map-name", JLabel.class).getText());
        assertTrue(SwingTree.named(waiting, "map-info-0", JLabel.class).getText().contains("패트릿 14"));
        assertEquals("인  원: 8", SwingTree.named(waiting, "map-info-1", JLabel.class).getText());
    }

    /** 17. 맵 설명에 크기는 쓰지 않고, 인기도는 별 5개로 보여 준다 */
    @Test
    void 맵_설명은_이름_인원_테마_인기도_별_5개() {
        enterAsHost(8);

        assertEquals("캠프 08", SwingTree.named(waiting, "map-info-0", JLabel.class).getText());
        assertEquals("인  원: 8", SwingTree.named(waiting, "map-info-1", JLabel.class).getText());
        assertEquals("테  마: 캠프", SwingTree.named(waiting, "map-info-2", JLabel.class).getText());
        StarRating popularity = SwingTree.named(waiting, "map-info-3", StarRating.class);
        assertEquals("인기도:", popularity.getText());
        assertEquals(5, popularity.stars());
        for (int i = 0; i < WaitingRoomLayout.MAP_INFO_ROWS; i++) {
            String text = SwingTree.named(waiting, "map-info-" + i, JLabel.class).getText();
            assertFalse(text.contains("크기") || text.matches(".*\\d+x\\d+.*"), "크기를 쓰지 않아야 합니다: " + text);
        }
    }

    /** 18. 캠프·패트릿은 8명, 빌리지는 6명까지다. 빌리지를 고르면 정원이 6명이 되어 남는 자리가 닫힌다 */
    @Test
    void 빌리지를_고르면_정원이_6명이_되어_남는_자리가_닫힌다() {
        enterAsHost(8);
        AbstractButton select = SwingTree.named(waiting, "map-select", AbstractButton.class);

        pick.set("village/village10");
        select.doClick();

        assertEquals(6, session.room().capacity());
        assertEquals("1/6", SwingTree.named(waiting, "room-count", JLabel.class).getText());
        assertEquals("인  원: 6", SwingTree.named(waiting, "map-info-1", JLabel.class).getText());
        for (int i = 1; i < 6; i++) {
            assertEquals(SlotState.EMPTY, slot(i).state(), "slot " + i);
        }
        assertEquals(SlotState.CLOSED, slot(6).state());
        assertEquals(SlotState.CLOSED, slot(7).state());

        pick.set("camp/camp08");
        select.doClick();
        assertEquals(8, session.room().capacity());
        assertEquals(SlotState.EMPTY, slot(7).state());
    }

    /** 20. 보라 액자는 랜덤 맵일 때만 쓰고, 맵을 고르면 그 맵 그림이 미리보기 칸을 꽉 채운다 (참고 화면 8번) */
    @Test
    void 맵을_고르면_맵_그림이_미리보기_칸을_꽉_채운다() {
        enterAsHost(8);

        MapPreview thumbnail = SwingTree.named(waiting, "map-thumbnail", MapPreview.class);
        BufferedImage screen = SwingTree.render(waiting);

        assertEquals(rect(WaitingRoomLayout.MAP_PREVIEW), SwingTree.boundsIn(thumbnail, waiting));
        assertNotEquals(OutgamePalette.MAP_PREVIEW_BACKGROUND, rgb(screen, 490, 352), "고른 맵인데 보라 액자가 보입니다.");
    }

    @Test
    void 랜덤_맵은_보라_액자_안에_물음표_카드로_보여_주고_시작할_때_맵을_정한다() {
        enterAsHost(8);
        pick.set(GameMaps.RANDOM_ID);

        SwingTree.named(waiting, "map-select", AbstractButton.class).doClick();
        layout();
        BufferedImage screen = SwingTree.render(waiting);

        assertEquals(GameMaps.RANDOM_ID, session.mapId());
        assertEquals(OutgamePalette.MAP_PREVIEW_BACKGROUND, rgb(screen, 490, 352));
        assertEquals(rect(WaitingRoomLayout.MAP_THUMBNAIL),
                SwingTree.boundsIn(SwingTree.named(waiting, "map-thumbnail", MapPreview.class), waiting));
        assertEquals("랜덤 맵", SwingTree.named(waiting, "map-name", JLabel.class).getText());
        assertEquals("랜덤 맵", SwingTree.named(waiting, "map-info-0", JLabel.class).getText());
        String started = session.start().mapId();
        assertTrue(GameMaps.ids().contains(started), "시작할 때는 실제 맵이어야 합니다: " + started);
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

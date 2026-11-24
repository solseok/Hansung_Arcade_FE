package hansung.arcade.outgame.ui;

import hansung.arcade.ingame.hud.Box;
import hansung.arcade.outgame.Team;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static hansung.arcade.outgame.ui.WaitingRoomLayout.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 8. 대기실 디자인 시스템 - 800x600 기준 배치와 색.
 * 참고 화면(1288x964)에서 잰 값을 가로 ÷1.61, 세로 ÷1.607로 옮겼다.
 *
 * <pre>
 * ┌ TOP_BAR(공지) ─────────────┐┌ RIGHT_PANEL ─────────────┐
 * │┌ LEFT_PANEL ─────────────┐ ││ CHARACTER_SECTION        │
 * ││ ROOM_HEADER  번호 제목 인원│ ││  배너 + 캐릭터 4x2          │
 * ││ SLOT_GRID  4x2 자리       │ ││ 팀선택  TEAM_SECTION 8색   │
 * ││  (그림 / 닉네임 / 상태)     │ ││ 맵     MAP_SECTION        │
 * ││ CHAT (탭 / 로그 / 입력)    │ ││  미리보기 | 정보 4줄·맵선택  │
 * │└─────────────────────────┘ ││ READY_SECTION 준비·자동준비  │
 * └ BOTTOM_BAR(로비로 나가기) ───────────────────────────────┘
 * </pre>
 */
class WaitingRoomLayoutTest {

    private static final Box SCREEN = new Box(0, 0, 800, 600);

    @Test
    void 큰_구역_크기() {
        assertEquals(800, WIDTH);
        assertEquals(600, HEIGHT);
        assertEquals(new Box(0, 0, 462, 34), TOP_BAR);
        assertEquals(new Box(7, 39, 453, 518), LEFT_PANEL);
        assertEquals(new Box(464, 3, 333, 557), RIGHT_PANEL);
        assertEquals(new Box(0, 561, 800, 39), BOTTOM_BAR);
    }

    @Test
    void 왼쪽_판_구역_크기() {
        assertEquals(new Box(21, 59, 427, 28), ROOM_HEADER);
        assertEquals(new Box(21, 90, 427, 293), SLOT_GRID);
        assertEquals(new Box(21, 389, 427, 160), CHAT);
        assertEquals(new Box(21, 518, 427, 31), CHAT_INPUT_ROW);
    }

    @Test
    void 자리_한_칸은_95x137이고_그림_95_닉네임_20_상태_17() {
        assertEquals(new Box(29, 96, 95, 137), slot(0));
        assertEquals(new Box(344, 240, 95, 137), slot(7));
        for (int i = 0; i < SLOTS; i++) {
            Box slot = slot(i);
            assertEquals(new Box(slot.x(), slot.y(), 95, 95), slotImage(i));
            assertEquals(new Box(slot.x(), slot.y() + 99, 95, 20), slotName(i));
            assertEquals(new Box(slot.x(), slot.y() + 120, 95, 17), slotStatus(i));
            assertTrue(slot.contains(slotImage(i)) && slot.contains(slotName(i)) && slot.contains(slotStatus(i)));
        }
    }

    @Test
    void 오른쪽_판_구역_크기() {
        assertEquals(new Box(480, 14, 295, 244), CHARACTER_SECTION);
        assertEquals(new Box(488, 23, 278, 84), CHARACTER_BANNER);
        assertEquals(new Box(488, 115, 62, 63), characterCell(0));
        assertEquals(new Box(704, 185, 62, 63), characterCell(7));
        assertEquals(new Box(480, 274, 295, 51), TEAM_SECTION);
        assertEquals(new Box(488, 284, 29, 31), teamSwatch(0));
        assertEquals(new Box(740, 284, 29, 31), teamSwatch(7));
        assertEquals(new Box(480, 340, 295, 144), MAP_SECTION);
        assertEquals(new Box(486, 347, 149, 130), MAP_PREVIEW);
        assertEquals(new Box(641, 347, 127, 16), mapInfo(0));
        assertEquals(new Box(641, 404, 127, 16), mapInfo(3));
        assertEquals(new Box(644, 428, 121, 46), MAP_SELECT);
        assertEquals(new Box(500, 490, 253, 64), READY_SECTION);
        assertEquals(new Box(510, 495, 190, 53), READY_BUTTON);
        assertEquals(new Box(708, 496, 38, 50), AUTO_READY);
    }

    @Test
    void 모든_칸이_화면_안에_있고_묶음_안에_들어간다() {
        for (Box box : List.of(TOP_BAR, LEFT_PANEL, RIGHT_PANEL, BOTTOM_BAR)) {
            assertTrue(SCREEN.contains(box), box.toString());
        }
        for (Box box : List.of(ROOM_HEADER, SLOT_GRID, CHAT)) {
            assertTrue(LEFT_PANEL.contains(box), box.toString());
        }
        assertTrue(TOP_BAR.contains(NOTICE));
        assertTrue(ROOM_HEADER.contains(ROOM_NUMBER) && ROOM_HEADER.contains(ROOM_TITLE) && ROOM_HEADER.contains(ROOM_COUNT));
        assertTrue(CHAT.contains(CHAT_TAB) && CHAT.contains(CHAT_LOG) && CHAT.contains(CHAT_INPUT_ROW));
        for (int i = 0; i < SLOTS; i++) {
            assertTrue(SLOT_GRID.contains(slot(i)), "slot " + i);
        }
        for (Box box : List.of(CHARACTER_SECTION, TEAM_SECTION, MAP_SECTION, READY_SECTION, TEAM_LABEL, MAP_LABEL)) {
            assertTrue(RIGHT_PANEL.contains(box), box.toString());
        }
        assertTrue(CHARACTER_SECTION.contains(CHARACTER_BANNER));
        for (int i = 0; i < CHARACTER_CELLS; i++) {
            assertTrue(CHARACTER_SECTION.contains(characterCell(i)), "character " + i);
        }
        for (int i = 0; i < Team.values().length; i++) {
            assertTrue(TEAM_SECTION.contains(teamSwatch(i)), "team " + i);
        }
        assertTrue(MAP_SECTION.contains(MAP_PREVIEW) && MAP_SECTION.contains(MAP_SELECT));
        assertTrue(MAP_PREVIEW.contains(MAP_THUMBNAIL));
        for (int i = 0; i < MAP_INFO_ROWS; i++) {
            assertTrue(MAP_SECTION.contains(mapInfo(i)), "map info " + i);
        }
        assertTrue(READY_SECTION.contains(READY_BUTTON) && READY_SECTION.contains(AUTO_READY));
        assertTrue(BOTTOM_BAR.contains(LEAVE));
    }

    @Test
    void 같은_줄의_칸끼리_겹치지_않는다() {
        assertNoOverlap(List.of(TOP_BAR, LEFT_PANEL, RIGHT_PANEL, BOTTOM_BAR));
        assertNoOverlap(List.of(ROOM_HEADER, SLOT_GRID, CHAT));
        assertNoOverlap(List.of(CHARACTER_SECTION, TEAM_SECTION, MAP_SECTION, READY_SECTION, TEAM_LABEL, MAP_LABEL));
        List<Box> slots = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            slots.add(slot(i));
        }
        assertNoOverlap(slots);
        List<Box> cells = new ArrayList<>(List.of(CHARACTER_BANNER));
        for (int i = 0; i < CHARACTER_CELLS; i++) {
            cells.add(characterCell(i));
        }
        assertNoOverlap(cells);
        List<Box> swatches = new ArrayList<>();
        for (int i = 0; i < Team.values().length; i++) {
            swatches.add(teamSwatch(i));
        }
        assertNoOverlap(swatches);
        List<Box> map = new ArrayList<>(List.of(MAP_PREVIEW, MAP_SELECT));
        for (int i = 0; i < MAP_INFO_ROWS; i++) {
            map.add(mapInfo(i));
        }
        assertNoOverlap(map);
        assertNoOverlap(List.of(READY_BUTTON, AUTO_READY));
    }

    @Test
    void 대기실_색_토큰() {
        assertEquals(new Color(3, 66, 141), OutgamePalette.WAITING_FRAME);
        assertEquals(new Color(2, 50, 118), OutgamePalette.WAITING_NOTICE);
        assertEquals(new Color(1, 194, 250), OutgamePalette.WAITING_LEFT_PANEL);
        assertEquals(new Color(4, 106, 206), OutgamePalette.WAITING_RIGHT_PANEL);
        assertEquals(new Color(3, 70, 141), OutgamePalette.WAITING_SECTION);
        assertEquals(new Color(2, 146, 216), OutgamePalette.WAITING_HEADER);
        assertEquals(new Color(8, 194, 254), OutgamePalette.SLOT_OCCUPIED);
        assertEquals(new Color(15, 150, 228), OutgamePalette.SLOT_EMPTY);
        assertEquals(new Color(9, 116, 214), OutgamePalette.SLOT_NAME);
        assertEquals(new Color(1, 49, 92), OutgamePalette.SLOT_STATUS);
        assertEquals(new Color(20, 225, 245), OutgamePalette.STATUS_HOST);
        assertEquals(new Color(255, 182, 31), OutgamePalette.STATUS_READY);
        assertEquals(new Color(10, 76, 131), OutgamePalette.STATUS_IDLE);
        assertEquals(new Color(3, 117, 215), OutgamePalette.CHAT_BACKGROUND);
        assertEquals(new Color(0, 78, 172), OutgamePalette.CHAT_INPUT);
        assertEquals(new Color(35, 139, 211), OutgamePalette.CHARACTER_CELL);
        assertEquals(new Color(87, 42, 180), OutgamePalette.MAP_PREVIEW_BACKGROUND);
        assertEquals(new Color(3, 119, 207), OutgamePalette.MAP_INFO);
        assertEquals(new Color(9, 148, 247), OutgamePalette.MAP_SELECT);
        assertEquals(new Color(242, 156, 3), OutgamePalette.READY_TOP);
        assertEquals(new Color(253, 198, 0), OutgamePalette.READY_BOTTOM);
        assertEquals(new Color(7, 62, 130), OutgamePalette.BOTTOM_BAR);
    }

    @Test
    void 방장_준비_대기_상태색은_서로_다르다() {
        assertEquals(3, Set.of(OutgamePalette.STATUS_HOST, OutgamePalette.STATUS_READY, OutgamePalette.STATUS_IDLE).size());
    }

    @Test
    void 팀은_8색이고_참고_화면_순서() {
        List<Color> expected = List.of(new Color(234, 1, 4), new Color(253, 180, 4), new Color(255, 122, 4),
                new Color(96, 179, 1), new Color(2, 182, 194), new Color(0, 92, 228), new Color(98, 20, 224),
                new Color(227, 0, 136));
        assertEquals(expected, List.of(Team.values()).stream().map(Team::color).toList());
        assertEquals(List.of("빨강", "노랑", "주황", "초록", "청록", "파랑", "보라", "분홍"),
                List.of(Team.values()).stream().map(Team::label).toList());
        assertEquals(8, new HashSet<>(expected).size());
    }

    private static void assertNoOverlap(List<Box> boxes) {
        for (int i = 0; i < boxes.size(); i++) {
            for (int j = i + 1; j < boxes.size(); j++) {
                assertFalse(overlaps(boxes.get(i), boxes.get(j)), boxes.get(i) + " 와 " + boxes.get(j) + " 가 겹칩니다.");
            }
        }
    }

    private static boolean overlaps(Box a, Box b) {
        return a.x() < b.right() && b.x() < a.right() && a.y() < b.bottom() && b.y() < a.bottom();
    }
}

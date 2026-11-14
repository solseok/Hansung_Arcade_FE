package hansung.arcade.ingame.hud;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static hansung.arcade.ingame.hud.HudLayout.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 867 x 686 디자인 수치를 800 x 600으로 옮긴 결과가 서로 맞물리는지 확인한다.
 */
class HudLayoutTest {

    private static final Box SCREEN = new Box(0, 0, WIDTH, HEIGHT);

    @Test
    void 모든_영역이_화면_안에_있다() {
        List<Box> boxes = new ArrayList<>(List.of(FRAME, MAP_LABEL, PLAYER_LIST, TIMER, ITEM_PANEL, ITEM_INNER,
                ITEM_VIEW, EXIT, EXIT_BUTTON, ITEM_BAR));
        for (int i = 0; i < PLAYER_SLOTS; i++) {
            boxes.add(playerSlot(i));
        }
        for (int i = 0; i < ITEM_SLOTS; i++) {
            boxes.add(itemSlot(i));
        }
        for (Box box : boxes) {
            assertTrue(SCREEN.contains(box), box::toString);
        }
    }

    @Test
    void 맵_영역은_15x13칸이고_테두리는_3px_4px이다() {
        assertEquals(600, FIELD.width());
        assertEquals(520, FIELD.height());
        assertEquals(FIELD.inset(-7), FRAME);
    }

    @Test
    void 맵_이름표는_왼쪽에서_14px_프레임에서_4px_위에_있다() {
        assertEquals(14, MAP_LABEL.x());
        assertEquals(FRAME.y() - 4, MAP_LABEL.bottom());
        assertEquals(MAP_LABEL.x(), FRAME.x());
        assertTrue(MAP_LABEL.y() >= 0);
    }

    @Test
    void 채팅_바는_맵_영역_왼쪽에서_15px_아래에서_17px에_있고_프레임_폭의_3분의_1에서_끝난다() {
        // 첫 지시: 맵 테두리 안쪽으로 왼쪽 16 (15), 아래 19 (17), 높이 14 (12)
        assertEquals(FIELD.x() + 15, CHAT_BAR.x());
        assertEquals(FIELD.bottom() - 17, CHAT_BAR.bottom());
        assertEquals(12, CHAT_BAR.height());
        assertEquals(CHAT_TEXT_HEIGHT, CHAT_BAR.height());
        assertTrue(Math.abs(CHAT_BAR.right() - (FRAME.x() + FRAME.width() / 3.0)) <= 1, "오른쪽 끝 " + CHAT_BAR.right());
        // 글자 테두리 1px까지 맵 영역 안에 들어온다
        assertTrue(FIELD.contains(CHAT_BAR.inset(-CHAT_OUTLINE)));
    }

    @Test
    void 올라온_채팅은_맵_영역_아래에서_105px_위에_놓이고_입력_바와_겹치지_않는다() {
        // 첫 지시: 채팅이 보이는 곳은 120 (105) 떨어져 있다
        assertEquals(FIELD.bottom() - 105, CHAT_LOG_BOTTOM);
        assertTrue(CHAT_LOG_BOTTOM + CHAT_OUTLINE < CHAT_BAR.y() - CHAT_OUTLINE);
        // 줄 사이에 테두리끼리 겹치지 않는다
        assertEquals(CHAT_TEXT_HEIGHT + 2, CHAT_LINE_PITCH);
    }

    @Test
    void 말풍선은_최대_폭_129px이고_글자_여백은_위아래_9_왼쪽_9_오른쪽_14다() {
        // 지시: 최대 폭 140 (129), 모서리 8 (7), 여백 위아래 10 (9) · 왼쪽 10 (9) · 오른쪽 15 (14)
        assertEquals(129, BUBBLE_MAX_WIDTH);
        assertEquals(7, BUBBLE_RADIUS);
        assertEquals(9, BUBBLE_PADDING_TOP);
        assertEquals(9, BUBBLE_PADDING_BOTTOM);
        assertEquals(9, BUBBLE_PADDING_LEFT);
        assertEquals(14, BUBBLE_PADDING_RIGHT);
        assertEquals(BUBBLE_MAX_WIDTH, BUBBLE_PADDING_LEFT + BUBBLE_TEXT_WIDTH + BUBBLE_PADDING_RIGHT);
    }

    @Test
    void 오른쪽_열은_간격을_지킨_채_나가기가_창_바닥에서_9px_위에_오도록_놓인다() {
        assertEquals(FRAME.right() + 9, PLAYER_LIST.x());
        // 첫 지시 크기: 168 x 417 (155 x 365), 112 (98), 41 (36)
        assertEquals(365, PLAYER_LIST.height());
        assertEquals(98, ITEM_PANEL.height());
        assertEquals(36, EXIT.height());
        assertEquals(PLAYER_LIST.bottom() + 4, ITEM_PANEL.y());
        assertEquals(ITEM_PANEL.bottom() + 4, EXIT.y());
        // 나가기 받침은 창 바닥에서 10 (9) 위
        assertEquals(HEIGHT - 9, EXIT.bottom());
        // 나가기 받침만 오른쪽으로 1px 나온다
        assertEquals(PLAYER_LIST.right() + 1, EXIT.right());
        assertTrue(EXIT.contains(EXIT_BUTTON));
    }

    @Test
    void TIMER_박스와_플레이어_칸_8개가_참고_화면처럼_목록_높이를_채운다() {
        assertTrue(PLAYER_LIST.contains(TIMER));
        assertEquals(playerSlot(0).x(), TIMER.x());
        assertEquals(playerSlot(0).width(), TIMER.width());
        assertEquals(PLAYER_LIST.y() + 12, TIMER.y());
        assertEquals(TIMER.bottom() + 4, playerSlot(0).y());
        // 마지막 칸 아래에는 참고 화면처럼 작은 여백만 남는다
        int bottomMargin = PLAYER_LIST.bottom() - playerSlot(PLAYER_SLOTS - 1).bottom();
        assertTrue(bottomMargin >= 4 && bottomMargin <= 10, "아래 여백 " + bottomMargin);
        for (int i = 0; i < PLAYER_SLOTS; i++) {
            Box slot = playerSlot(i);
            Box portrait = portraitBox(i);
            assertTrue(slot.inset(EDGE).contains(portrait), "얼굴 틀 " + i);
            // 얼굴 틀은 칸 안에서 위아래 여백이 같다
            assertEquals(portrait.y() - slot.y(), slot.bottom() - portrait.bottom(), "얼굴 틀 위아래 여백 " + i);
        }
    }

    @Test
    void 플레이어_슬롯_8칸이_겹치지_않고_목록_안에_들어간다() {
        Box listBody = new Box(PLAYER_LIST.x(), TIMER.bottom(), PLAYER_LIST.width(), PLAYER_LIST.bottom() - TIMER.bottom());
        for (int i = 0; i < PLAYER_SLOTS; i++) {
            assertTrue(listBody.contains(playerSlot(i)), "slot " + i);
            if (i > 0) {
                assertEquals(playerSlot(i - 1).bottom() + PLAYER_SLOT_GAP, playerSlot(i).y());
            }
        }
    }

    @Test
    void TIMER_숫자는_글씨_뒤_남는_자리를_모두_쓴다() {
        assertEquals(TIMER.x() + TIMER_PADDING, TIMER_LABEL.x());
        assertEquals(TIMER_LABEL.right() + TIMER_GAP, TIMER_DIGITS.x());
        assertEquals(TIMER.right() - TIMER_PADDING, TIMER_DIGITS.right());
    }

    @Test
    void 아이템_상자_안쪽_여백이_디자인과_같다() {
        assertEquals(ITEM_PANEL.inset(16, 8, 6, 8), ITEM_INNER);
        // 회색 칸은 A 기준 위아래 13, 왼쪽 15, 오른쪽 63
        assertEquals(13, ITEM_VIEW.y() - ITEM_INNER.y());
        assertEquals(13, ITEM_INNER.bottom() - ITEM_VIEW.bottom());
        assertEquals(15, ITEM_VIEW.x() - ITEM_INNER.x());
        assertEquals(63, ITEM_INNER.right() - ITEM_VIEW.right());
    }

    @Test
    void 아이템_바는_맵_프레임_아래와_4px_겹치고_칸_6개가_바_안에_들어간다() {
        int barCenter = ITEM_BAR.x() + ITEM_BAR.width() / 2;
        int frameCenter = FRAME.x() + FRAME.width() / 2;
        assertTrue(Math.abs(barCenter - frameCenter) <= 1);
        assertEquals(FRAME.bottom() - 4, ITEM_BAR.y());
        // 창 바닥에 붙는다
        assertEquals(HEIGHT, ITEM_BAR.bottom());
        assertEquals(0, ITEM_BAR_CORNERS.bottomLeft());
        assertEquals(0, ITEM_BAR_CORNERS.bottomRight());
        // 겹치는 곳은 프레임 바깥 검은 띠까지이고 맵 영역은 가리지 않는다
        assertTrue(ITEM_BAR.y() >= FIELD.bottom());
        for (int i = 0; i < ITEM_SLOTS; i++) {
            Box slot = itemSlot(i);
            assertTrue(ITEM_BAR.contains(slot), "slot " + i);
            assertTrue(slot.x() >= ITEM_BAR_LABEL.right());
            assertTrue(slot.inset(EDGE).contains(keyBadge(i)), "badge " + i);
        }
        assertFalse(itemSlot(0).right() > itemSlot(1).x());
    }
}

package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.chat.ChatMessage;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.List;

import static hansung.arcade.ingame.hud.HudLayout.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 채팅 목록의 줄 모양과 말풍선 그림을 창 없이 확인한다. 돋움체가 없는 PC와 CI에서도 돌도록
 * Java 기본 고정폭 글꼴을 쓰고, 돋움체 글자 수 검사는 돋움체가 있을 때만 한다.
 */
class ChatViewsTest {

    private static final int WHITE = 0xFFFFFFFF;

    private final OutlineText text = new OutlineText(new Font(Font.MONOSPACED, Font.PLAIN, 12), CHAT_TEXT_HEIGHT,
            Color.WHITE, Color.BLACK);
    private final SpeechBubbleView bubbles = new SpeechBubbleView(text);

    @Test
    void 채팅_목록에서_남의_줄은_닉네임과_내용이고_내_줄은_꺾쇠로_시작한다() {
        assertEquals("other : welcome", ChatLogView.line(new ChatMessage(2, "other", "welcome"), false));
        assertEquals("> Player : Hi", ChatLogView.line(new ChatMessage(1, "Player", "Hi"), true));
    }

    @Test
    void 짧은_말은_한_줄이고_말풍선은_글자에_여백을_더한_크기다() {
        BufferedImage bubble = bubbles.render("Hi");

        assertEquals(List.of("Hi"), bubbles.wrap("Hi"));
        // 왼쪽 10 (9) + 글자 + 오른쪽 15 (14), 위 10 (9) + 글자 칸 + 아래 10 (9)
        assertEquals(9 + text.width("Hi") + 14, bubble.getWidth());
        assertEquals(9 + CHAT_TEXT_HEIGHT + 9, bubble.getHeight());
    }

    @Test
    void 말풍선은_흰_테두리_안을_검은색_20퍼센트로_채우고_모서리가_둥글다() {
        BufferedImage bubble = bubbles.render("Hi");
        int w = bubble.getWidth();
        int h = bubble.getHeight();

        // 네 변 가운데는 흰 1px 테두리
        assertEquals(WHITE, bubble.getRGB(w / 2, 0));
        assertEquals(WHITE, bubble.getRGB(w / 2, h - 1));
        assertEquals(WHITE, bubble.getRGB(0, h / 2));
        assertEquals(WHITE, bubble.getRGB(w - 1, h / 2));
        // 테두리 바로 안쪽은 검은색 20% (255 x 0.2 = 51)
        assertEquals(0x33000000, bubble.getRGB(w / 2, 1));
        assertEquals(0x33000000, bubble.getRGB(1, h / 2));
        assertEquals(0x33000000, bubble.getRGB(w - 4, h - 4));
        // 둥근 모서리 바깥은 비어 있다
        assertEquals(0, bubble.getRGB(0, 0));
        assertEquals(0, bubble.getRGB(w - 1, 0));
        assertEquals(0, bubble.getRGB(0, h - 1));
        assertEquals(0, bubble.getRGB(w - 1, h - 1));
        // 반지름 7 안쪽에서 변이 시작한다
        assertEquals(WHITE, bubble.getRGB(BUBBLE_RADIUS, 0));
        assertEquals(WHITE, bubble.getRGB(0, BUBBLE_RADIUS));
    }

    @Test
    void 글자는_말풍선_안_여백을_지킨다() {
        // 위아래로 칸을 꽉 채우는 글자들
        BufferedImage bubble = bubbles.render("[|]{}");
        // 둥근 모서리의 흰 테두리가 섞이지 않게 모서리 반지름만큼 안쪽에서 글자를 찾는다
        Box ink = whiteBounds(bubble, new Box(0, 0, bubble.getWidth(), bubble.getHeight()).inset(BUBBLE_RADIUS));

        // 글자 픽셀은 칸에서 2px 넘게 벗어나지 않는다 (대신 쓰는 글꼴은 조금 삐져나올 수 있다)
        assertTrue(ink.x() >= BUBBLE_PADDING_LEFT - 2, "왼쪽 " + ink.x());
        assertTrue(ink.y() >= BUBBLE_PADDING_TOP - 2, "위 " + ink.y());
        assertTrue(ink.right() <= bubble.getWidth() - BUBBLE_PADDING_RIGHT + 2, "오른쪽 " + ink.right());
        assertTrue(ink.bottom() <= bubble.getHeight() - BUBBLE_PADDING_BOTTOM + 2, "아래 " + ink.bottom());
    }

    @Test
    void 최대_폭을_넘는_말은_줄을_바꾸고_말풍선은_그만큼_높아진다() {
        String said = "가나다라마바사아자차카타파하 abcdefghijklmnopqrstuvwxyz 0123456789";
        List<String> lines = bubbles.wrap(said);
        BufferedImage bubble = bubbles.render(said);

        assertTrue(lines.size() > 1, lines.toString());
        for (String line : lines) {
            assertTrue(text.width(line) <= BUBBLE_TEXT_WIDTH, line);
            assertFalse(line.isEmpty());
            assertEquals(line.strip(), line, "줄 앞뒤 공백");
        }
        // 글자를 빠뜨리지 않는다 (줄이 바뀐 자리의 공백만 사라진다)
        assertEquals(said.replace(" ", ""), String.join("", lines).replace(" ", ""));
        assertTrue(bubble.getWidth() <= BUBBLE_MAX_WIDTH, "폭 " + bubble.getWidth());
        assertEquals(9 + CHAT_TEXT_HEIGHT + (lines.size() - 1) * CHAT_LINE_PITCH + 9, bubble.getHeight());
    }

    @Test
    void 한_줄에_들어가는_말은_공백이_있어도_줄을_바꾸지_않는다() {
        assertEquals(List.of("a b c"), bubbles.wrap("a b c"));
    }

    @Test
    void 돋움체로는_한_줄에_한글_8자_또는_영문_17자가_들어간다() {
        HudFonts fonts = HudFonts.load();
        assumeTrue(!fonts.missing().contains(HudFonts.DOTUMCHE), "돋움체가 없는 환경");
        SpeechBubbleView dotum = new SpeechBubbleView(new OutlineText(fonts.dotumChe(CHAT_TEXT_HEIGHT),
                CHAT_TEXT_HEIGHT, Color.WHITE, Color.BLACK));

        assertEquals(List.of("가나다라마바사아", "자"), dotum.wrap("가나다라마바사아자"));
        assertEquals(List.of("abcdefghijklmnopq", "r"), dotum.wrap("abcdefghijklmnopqr"));
        // 최대 길이(64바이트)를 다 채워도 4줄이다
        assertEquals(4, dotum.wrap("가".repeat(32)).size());
        assertEquals(4, dotum.wrap("a".repeat(64)).size());
        // 한글 8자 줄: 9 + 96 + 14 = 119 (최대 129)
        assertEquals(119, dotum.render("가".repeat(32)).getWidth());
    }

    @Test
    void 말풍선은_캐릭터_머리_위_가운데에_놓인다() {
        Box place = SpeechBubbleView.place(60, 30, 300, 300);

        assertEquals(300 - 30, place.x());
        assertEquals(300 - BUBBLE_ABOVE_PLAYER, place.bottom());
    }

    @Test
    void 말풍선은_맵_영역_밖으로_나가지_않는다() {
        // 왼쪽 위 구석의 캐릭터
        Box topLeft = SpeechBubbleView.place(100, 30, FIELD.x() + 20, FIELD.y() + 20);
        assertEquals(FIELD.x(), topLeft.x());
        assertEquals(FIELD.y(), topLeft.y());

        // 오른쪽 끝의 캐릭터
        Box right = SpeechBubbleView.place(100, 30, FIELD.right() - 20, 300);
        assertEquals(FIELD.right(), right.right());
        assertEquals(300 - BUBBLE_ABOVE_PLAYER, right.bottom());
    }

    /** area 안의 흰 픽셀을 모두 담는 영역 */
    private static Box whiteBounds(BufferedImage image, Box area) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        for (int y = area.y(); y < area.bottom(); y++) {
            for (int x = area.x(); x < area.right(); x++) {
                if (image.getRGB(x, y) == WHITE) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        assertTrue(maxX >= 0, "흰 픽셀이 없다");
        return new Box(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}

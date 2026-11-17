package hansung.arcade.ingame.hud;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 10. 인게임 오른쪽 플레이어 목록에서 닉네임이 잘리지 않게, 가장 넓은 8자 닉네임이 닉네임 칸 안에 들어가는 크기로 글꼴을 맞춘다.
 * 닉네임 글꼴은 HY견고딕이고, 없으면 운영체제의 한글 고딕 굵게, 그것도 없으면 Java 기본 산세리프 굵게로 대신한다
 * ({@link HudFonts}).
 */
class NicknameFitTest {

    private static HudFonts fonts;
    private static HudSkin skin;

    @BeforeAll
    static void load() {
        fonts = HudFonts.load();
        skin = HudSkin.load(fonts);
    }

    @Test
    void 닉네임_칸은_얼굴_틀_오른쪽부터_칸_테두리_안쪽까지() {
        for (int i = 0; i < HudLayout.PLAYER_SLOTS; i++) {
            Box slot = HudLayout.playerSlot(i);
            Box portrait = HudLayout.portraitBox(i);
            Box name = HudLayout.nicknameBox(i);
            assertEquals(portrait.right() + HudLayout.NICKNAME_GAP, name.x());
            assertEquals(slot.right() - HudLayout.EDGE, name.right());
            assertEquals(slot.y(), name.y());
            assertEquals(slot.height(), name.height());
        }
        assertEquals(72, HudLayout.nicknameBox(0).width());
    }

    @ParameterizedTest
    @ValueSource(strings = {"가나다라마바사아", "뷁뷁뷁뷁뷁뷁뷁뷁", "WWWWWWWW", "MMMMMMMM", "가W가W가W가W", "Player12"})
    void 여덟_자_닉네임이_칸_안에_다_들어간다(String nickname) {
        Box box = HudLayout.nicknameBox(0);
        BufferedImage image = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        skin.nickname().draw(g, nickname, box, TextArt.Align.LEFT);
        g.dispose();

        Box ink = PixelImages.opaqueBounds(image);
        assertTrue(box.contains(ink), nickname + " 이(가) 닉네임 칸 " + box + " 밖으로 나갑니다: " + ink);
    }

    @Test
    void 글자_높이는_닉네임_높이를_넘지_않고_8px_아래로_줄지_않는다() {
        BufferedImage image = new BufferedImage(200, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        // 그림자 없이 글자만 재려고 같은 글꼴로 따로 그린다
        new FontTextArt(nicknameFont().font(), java.awt.Color.WHITE)
                .draw(g, "가", new Box(0, 0, 200, 60), TextArt.Align.LEFT);
        g.dispose();

        int height = PixelImages.opaqueBounds(image).height();
        assertTrue(height <= HudLayout.NICKNAME_HEIGHT, "가 높이 " + height);
        assertTrue(height >= 8, "가 높이 " + height + " - 너무 작아서 읽기 어렵습니다.");
    }

    @Test
    void 닉네임_글꼴은_HY견고딕_없으면_대신하는_글꼴() {
        String expected = fonts.gyeonGothic(12).getFamily(Locale.ENGLISH);

        assertEquals(expected, nicknameFont().font().getFamily(Locale.ENGLISH));
    }

    private static FontTextArt nicknameFont() {
        return (FontTextArt) skin.nickname();
    }
}

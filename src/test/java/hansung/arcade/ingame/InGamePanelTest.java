package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.HudFonts;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudPalette;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.Tileset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static hansung.arcade.ingame.hud.HudLayout.FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 창 없이 인게임 화면을 그림에 그려 맵과 HUD가 겹치는 순서를 확인한다.
 * fixture 테마의 집(H) 그림은 빨간 단색이고 칸보다 20px 높다.
 */
class InGamePanelTest {

    private static final Color HOUSE = new Color(0xC82828);

    private static HudSkin skin;

    @BeforeAll
    static void setUp() {
        skin = HudSkin.load(HudFonts.load());
    }

    @Test
    void 맨_윗줄의_키_큰_그림은_맵_테두리_위에_겹쳐_그린다() {
        BufferedImage screen = render("H.............1");

        // 회색 테두리 띠(맵 영역 바로 위)와 프레임 위 빈 곳까지 집 그림이 올라온다
        assertColor(HOUSE, screen, FIELD.x() + 20, FIELD.y() - 2);
        assertColor(HOUSE, screen, FIELD.x() + 20, HudLayout.FRAME.y() - 2);
        // 집이 없는 옆 칸 위는 테두리 그대로다
        assertColor(HudPalette.FRAME_INNER, screen, FIELD.x() + 60, FIELD.y() - 2);
    }

    @Test
    void 맵_이름표는_솟아오른_그림에_가려지지_않는다() {
        BufferedImage screen = render("H.............1");
        int x = FIELD.x() + 20;
        int y = HudLayout.MAP_LABEL.bottom() - 4;

        assertNotEquals(HOUSE.getRGB() & 0xFFFFFF, screen.getRGB(x, y) & 0xFFFFFF);
    }

    /** 15 x 13칸 맵의 첫 줄만 firstRow로 하고 나머지는 빈 바닥 */
    private static BufferedImage render(String firstRow) {
        List<String> rows = new ArrayList<>();
        rows.add(firstRow);
        for (int i = 1; i < 13; i++) {
            rows.add(".".repeat(15));
        }
        TileMap map = MapLoader.parse("fixture/tall", rows);
        GameWorld world = new GameWorld(map, new PlayerProfile("tester", Mascot.KKUKKU));
        InGamePanel panel = new InGamePanel(world, Tileset.forTheme("fixture"), skin);
        panel.setSize(HudLayout.WIDTH, HudLayout.HEIGHT);

        BufferedImage screen = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screen.createGraphics();
        panel.paint(g);
        g.dispose();
        return screen;
    }

    private static void assertColor(Color expected, BufferedImage screen, int x, int y) {
        assertEquals(String.format("#%06X", expected.getRGB() & 0xFFFFFF),
                String.format("#%06X", screen.getRGB(x, y) & 0xFFFFFF), "(%d, %d)".formatted(x, y));
    }
}

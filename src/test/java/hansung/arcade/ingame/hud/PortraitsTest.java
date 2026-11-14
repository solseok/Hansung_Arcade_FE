package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.CharacterColor;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.CharacterRecolor;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.SpriteSheet;
import hansung.arcade.ingame.render.Tileset;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortraitsTest {

    private static final Portraits PORTRAITS = Portraits.load();

    @Test
    void 모든_캐릭터의_얼굴을_틀_안쪽_크기에_맞춰_만든다() {
        for (Mascot mascot : Mascot.values()) {
            for (CharacterColor color : new CharacterColor[]{null, CharacterColor.ORANGE}) {
                BufferedImage portrait = PORTRAITS.get(mascot, color);
                assertNotNull(portrait, mascot + " " + color);
                assertTrue(portrait.getWidth() <= HudLayout.PORTRAIT_WIDTH - 2 * HudLayout.EDGE);
                assertTrue(portrait.getHeight() <= HudLayout.PORTRAIT_HEIGHT - 2 * HudLayout.EDGE);
            }
        }
    }

    @Test
    void 고정색을_고르면_정면_그림을_칠한_뒤_줄인_얼굴이다() {
        BufferedImage front = SpriteSheet.find(CharacterSprites.DIRECTORY + "sangsangbugi_front.bmp").orElseThrow()
                .frames(CharacterSprites.FRAME_SIZE).get(0);

        assertSamePixels(Portraits.portraitOf(front), PORTRAITS.get(Mascot.SANGSANGBUGI, null));
        assertSamePixels(Portraits.portraitOf(CharacterRecolor.recolor(front, Mascot.SANGSANGBUGI, CharacterColor.YELLOW)),
                PORTRAITS.get(Mascot.SANGSANGBUGI, CharacterColor.YELLOW));
    }

    @Test
    void 얼굴은_캐릭터와_색마다_한_번만_만들어_둔다() {
        BufferedImage green = PORTRAITS.get(Mascot.KKOKKO, CharacterColor.GREEN);

        assertSame(green, PORTRAITS.get(Mascot.KKOKKO, CharacterColor.GREEN));
        assertSame(PORTRAITS.get(Mascot.KKOKKO, null), PORTRAITS.get(Mascot.KKOKKO, null));
        assertNotSame(green, PORTRAITS.get(Mascot.KKOKKO, null));
        assertNotSame(green, PORTRAITS.get(Mascot.KKOKKO, CharacterColor.PURPLE));
    }

    @Test
    void 그림_파일이_없는_캐릭터는_얼굴이_없다() {
        assertNull(new Portraits(Map.of()).get(Mascot.SANGZZI, CharacterColor.RED));
    }

    @Test
    void 플레이어_목록에는_플레이어가_고른_고정색의_얼굴을_그린다() {
        TileMap map = MapLoader.parse("fixture/test01", List.of("1BB..", "....."));
        HudSkin skin = HudSkin.load(HudFonts.load());
        GameWorld world = new GameWorld(map, new PlayerProfile("tester", Mascot.SANGZZI, CharacterColor.MAGENTA));
        InGameHud hud = new InGameHud(skin, map, Tileset.forTheme("fixture"));

        BufferedImage screen = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screen.createGraphics();
        hud.paintBelowWorld(g);
        hud.paintAboveWorld(g, world);
        g.dispose();

        // PlayerList 가 놓는 자리: 얼굴 틀 안쪽 가운데, 아래쪽을 틀 바닥에 맞춘다
        BufferedImage portrait = skin.portraits().get(Mascot.SANGZZI, CharacterColor.MAGENTA);
        Box inside = HudLayout.portraitBox(0).inset(HudLayout.EDGE);
        int left = inside.x() + (inside.width() - portrait.getWidth()) / 2;
        int top = inside.bottom() - portrait.getHeight();
        int compared = 0;
        for (int y = 0; y < portrait.getHeight(); y++) {
            for (int x = 0; x < portrait.getWidth(); x++) {
                int argb = portrait.getRGB(x, y);
                if ((argb >>> 24) == 0xFF) {
                    assertEquals(argb & 0xFFFFFF, screen.getRGB(left + x, top + y) & 0xFFFFFF, "(%d, %d)".formatted(x, y));
                    compared++;
                }
            }
        }
        assertTrue(compared > 300, "비교한 픽셀 " + compared);
    }

    private static void assertSamePixels(BufferedImage expected, BufferedImage actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertEquals(expected.getRGB(x, y), actual.getRGB(x, y), "(%d, %d)".formatted(x, y));
            }
        }
    }
}

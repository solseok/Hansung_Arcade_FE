package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.item.ItemEffects;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.WorldRenderer;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static hansung.arcade.ingame.hud.HudLayout.CHAT_OUTLINE;
import static hansung.arcade.ingame.hud.HudLayout.CHAT_TEXT_HEIGHT;
import static hansung.arcade.ingame.hud.HudLayout.FIELD;
import static hansung.arcade.ingame.hud.HudLayout.MAP_LABEL;
import static hansung.arcade.ingame.hud.HudLayout.PLAYER_TAG_ABOVE_PLAYER;
import static hansung.arcade.ingame.hud.HudLayout.PLAYER_TAG_BELOW_PLAYER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2인 모드의 1P·2P 표시를 빈 그림에 그려, 누구 머리 위에 어떤 자리로 그리는지 확인한다.
 * 돋움체가 없는 PC와 CI에서도 돌도록 Java 기본 고정폭 글꼴을 쓴다.
 */
class PlayerTagViewTest {

    private static final PlayerProfile ONE = new PlayerProfile("one", Mascot.SANGSANGBUGI);
    private static final PlayerProfile TWO = new PlayerProfile("two", Mascot.HANSUNGNYANGI);
    /** 5 x 5칸 빈 맵. 1P는 왼쪽, 2P는 오른쪽 가운데 줄에 선다 */
    private static final TileMap MAP = MapLoader.parse("fixture/tag",
            List.of(".....", ".....", ".1.2.", ".....", "....."));

    private final OutlineText text = new OutlineText(new Font(Font.MONOSPACED, Font.PLAIN, 12), CHAT_TEXT_HEIGHT,
            Color.WHITE, Color.BLACK);
    private final PlayerTagView view = new PlayerTagView(text);

    @Test
    void 표시는_1P부터_차례로_붙인다() {
        assertEquals("1P", PlayerTagView.tagOf(0));
        assertEquals("2P", PlayerTagView.tagOf(1));
    }

    @Test
    void 두_명이면_각자의_머리_위에_자기_표시를_그린다() {
        GameWorld world = new GameWorld(MAP, List.of(ONE, TWO));
        BufferedImage screen = paint(world);

        Player first = world.localPlayers().get(0);
        Player second = world.localPlayers().get(1);
        Box firstTag = tagBox(world, first, "1P");
        Box secondTag = tagBox(world, second, "2P");
        // 글자 칸 아래 끝은 캐릭터 위치에서 52px 위다
        assertEquals(screenY(world, first) - PLAYER_TAG_ABOVE_PLAYER, firstTag.bottom());

        // 각 자리에 그 표시 글자가 그대로 찍히고, 그 밖에는 아무것도 그리지 않는다
        int inFirst = assertTagAt(screen, "1P", firstTag);
        int inSecond = assertTagAt(screen, "2P", secondTag);
        assertEquals(inFirst + inSecond, countDrawn(screen, new Box(0, 0, screen.getWidth(), screen.getHeight())));
    }

    @Test
    void 혼자_하면_표시를_그리지_않는다() {
        BufferedImage screen = paint(new GameWorld(MAP, ONE));

        assertEquals(0, countDrawn(screen, new Box(0, 0, screen.getWidth(), screen.getHeight())));
    }

    @Test
    void 물풍선에_갇혀도_표시를_그리고_탈락하면_거둔다() {
        GameWorld world = new GameWorld(MAP, List.of(ONE, TWO));
        Player first = world.localPlayers().get(0);
        Player second = world.localPlayers().get(1);
        first.trap(second.id());
        second.eliminate();
        BufferedImage screen = paint(world);

        assertTrue(countDrawn(screen, tagBox(world, first, "1P").inset(-OutlineText.MARGIN)) > 20);
        assertEquals(0, countDrawn(screen, tagBox(world, second, "2P").inset(-OutlineText.MARGIN)));
    }

    @Test
    void 말풍선이_떠_있는_동안에는_그_플레이어의_표시를_그리지_않는다() {
        GameWorld world = new GameWorld(MAP, List.of(ONE, TWO));
        world.sendChat("hi");
        BufferedImage screen = paint(world);

        assertEquals(0, countDrawn(screen, tagBox(world, world.localPlayers().get(0), "1P").inset(-OutlineText.MARGIN)));
        assertTrue(countDrawn(screen, tagBox(world, world.localPlayers().get(1), "2P").inset(-OutlineText.MARGIN)) > 20);
    }

    @Test
    void 머리_위_자리가_창_밖이거나_맵_이름표에_닿으면_발_아래에_둔다() {
        int width = 14;
        // 맵 가운데쯤: 머리 위, 가로 가운데
        Box middle = PlayerTagView.place(width, CHAT_TEXT_HEIGHT, 300, 300, 0);
        assertEquals(new Box(300 - width / 2, 300 - PLAYER_TAG_ABOVE_PLAYER - CHAT_TEXT_HEIGHT, width, CHAT_TEXT_HEIGHT),
                middle);

        // 맨 윗줄: 머리 위는 창 밖이다
        int topRow = FIELD.y() + 20;
        assertEquals(topRow + PLAYER_TAG_BELOW_PLAYER, PlayerTagView.place(width, CHAT_TEXT_HEIGHT, 300, topRow, 0).y());

        // 창 안이어도 맵 이름표 밑이면 내리고, 이름표 옆이면 그대로 둔다
        int nearTop = MAP_LABEL.bottom() + PLAYER_TAG_ABOVE_PLAYER + 2;
        assertEquals(nearTop + PLAYER_TAG_BELOW_PLAYER,
                PlayerTagView.place(width, CHAT_TEXT_HEIGHT, MAP_LABEL.x() + 30, nearTop, 0).y());
        assertEquals(nearTop - PLAYER_TAG_ABOVE_PLAYER - CHAT_TEXT_HEIGHT,
                PlayerTagView.place(width, CHAT_TEXT_HEIGHT, MAP_LABEL.right() + 40, nearTop, 0).y());
    }

    @Test
    void 뛰거나_탱크에_타서_솟으면_표시도_그만큼_올리되_창_위_끝에서_멈춘다() {
        int width = 14;
        int lift = WorldRenderer.JUMP_HEIGHT + WorldRenderer.TANK_SEAT;
        assertEquals(300 - lift - PLAYER_TAG_ABOVE_PLAYER - CHAT_TEXT_HEIGHT,
                PlayerTagView.place(width, CHAT_TEXT_HEIGHT, 300, 300, lift).y());

        // 둘째 줄: 서 있으면 머리 위에 자리가 있다. 솟으면 테두리가 창 위 끝에 닿는 자리에서 멈춘다
        int secondRow = FIELD.y() + 60;
        assertEquals(secondRow - PLAYER_TAG_ABOVE_PLAYER - CHAT_TEXT_HEIGHT,
                PlayerTagView.place(width, CHAT_TEXT_HEIGHT, 300, secondRow, 0).y());
        assertEquals(CHAT_OUTLINE, PlayerTagView.place(width, CHAT_TEXT_HEIGHT, 300, secondRow, lift).y());
        // 맵 이름표 밑이어도 서 있을 때 이름표에 닿지 않으면 머리 위다. 솟는 동안은 솟은 머리처럼 이름표 뒤로 지나간다
        assertEquals(CHAT_OUTLINE, PlayerTagView.place(width, CHAT_TEXT_HEIGHT, MAP_LABEL.x() + 30, secondRow, lift).y());

        // 맨 윗줄의 발 아래 표시는 땅의 그림자 밑에 그대로 둔다. 솟는 동안 머리 위로 건너뛰지도 않는다
        int topRow = FIELD.y() + 20;
        for (int up = 0; up <= lift; up++) {
            assertEquals(topRow + PLAYER_TAG_BELOW_PLAYER,
                    PlayerTagView.place(width, CHAT_TEXT_HEIGHT, 300, topRow, up).y());
        }
    }

    @Test
    void 탱크에_탄_플레이어의_표시는_좌석_높이만큼_올라간다() {
        GameWorld world = new GameWorld(MAP, List.of(ONE, TWO));
        Player first = world.localPlayers().get(0);
        ItemEffects.apply(first, world.gear(first), ItemType.TANK);
        BufferedImage screen = paint(world);

        Box firstTag = tagBox(world, first, "1P");
        assertEquals(screenY(world, first) - PLAYER_TAG_ABOVE_PLAYER - WorldRenderer.TANK_SEAT, firstTag.bottom());
        int inFirst = assertTagAt(screen, "1P", firstTag);
        int inSecond = assertTagAt(screen, "2P", tagBox(world, world.localPlayers().get(1), "2P"));
        assertEquals(inFirst + inSecond, countDrawn(screen, new Box(0, 0, screen.getWidth(), screen.getHeight())));
    }

    private BufferedImage paint(GameWorld world) {
        BufferedImage screen = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = screen.createGraphics();
        view.paint(g, world);
        g.dispose();
        return screen;
    }

    /**
     * tag 하나만 빈 그림에 그린 것과 screen의 place 둘레가 픽셀마다 같은지 본다. 찍힌 픽셀 수를 돌려준다.
     */
    private int assertTagAt(BufferedImage screen, String tag, Box place) {
        int margin = OutlineText.MARGIN;
        BufferedImage alone = new BufferedImage(place.width() + 2 * margin, place.height() + 2 * margin,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = alone.createGraphics();
        text.draw(g, tag, margin, margin);
        g.dispose();
        int drawn = 0;
        for (int y = 0; y < alone.getHeight(); y++) {
            for (int x = 0; x < alone.getWidth(); x++) {
                assertEquals(alone.getRGB(x, y), screen.getRGB(place.x() - margin + x, place.y() - margin + y),
                        "%s (%d, %d)".formatted(tag, x, y));
                if ((alone.getRGB(x, y) >>> 24) != 0) {
                    drawn++;
                }
            }
        }
        assertTrue(drawn > 20, tag + " 표시 픽셀 " + drawn);
        return drawn;
    }

    private Box tagBox(GameWorld world, Player player, String tag) {
        int x = HudLayout.mapOriginX(world.map()) + (int) Math.round(player.x());
        return PlayerTagView.place(text.width(tag), text.cellHeight(), x, screenY(world, player),
                WorldRenderer.figureLiftOf(player, world.gear(player)));
    }

    private static int screenY(GameWorld world, Player player) {
        return HudLayout.mapOriginY(world.map()) + (int) Math.round(player.y());
    }

    /** area 안에서 무엇이든 그려진(투명하지 않은) 픽셀 수 */
    private static int countDrawn(BufferedImage image, Box area) {
        int count = 0;
        for (int y = Math.max(0, area.y()); y < Math.min(image.getHeight(), area.bottom()); y++) {
            for (int x = Math.max(0, area.x()); x < Math.min(image.getWidth(), area.right()); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    count++;
                }
            }
        }
        return count;
    }
}

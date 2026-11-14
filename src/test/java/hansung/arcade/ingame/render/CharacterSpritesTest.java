package hansung.arcade.ingame.render;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 캐릭터 그림(resources/sprites/characters)을 읽어 고르는 규칙과 맵 위에 놓이는 자리를 확인한다.
 * 그림 내용은 바뀔 수 있으므로 픽셀 색은 읽은 그림과 비교한다.
 */
class CharacterSpritesTest {

    private static final CharacterSprites SPRITES = CharacterSprites.load();

    @Test
    void 모든_캐릭터의_네_방향_그림을_70x70으로_읽는다() {
        for (Mascot mascot : Mascot.values()) {
            for (Direction direction : Direction.values()) {
                BufferedImage standing = SPRITES.frame(mascot, direction, false, 0);
                BufferedImage walking = SPRITES.frame(mascot, direction, true, 0);
                String what = mascot + " " + direction;
                assertEquals(CharacterSprites.FRAME_SIZE, standing.getWidth(), what);
                assertEquals(CharacterSprites.FRAME_SIZE, standing.getHeight(), what);
                assertEquals(CharacterSprites.FRAME_SIZE, walking.getWidth(), what);
                assertEquals(CharacterSprites.FRAME_SIZE, walking.getHeight(), what);
            }
        }
    }

    @Test
    void 발끝은_그림_아래쪽_여백_바로_위에_있다() {
        for (Mascot mascot : Mascot.values()) {
            BufferedImage front = SPRITES.frame(mascot, Direction.DOWN, false, 0);
            int feet = lowestOpaqueRow(front) + 1;
            assertTrue(Math.abs(CharacterSprites.FEET_Y - feet) <= 2, mascot + " 발끝 " + feet);
        }
    }

    @Test
    void 멈춰_있으면_걸은_시간과_상관없이_서_있는_그림이다() {
        assertSame(SPRITES.frame(Mascot.SANGSANGBUGI, Direction.LEFT, false, 0),
                SPRITES.frame(Mascot.SANGSANGBUGI, Direction.LEFT, false, 0.35));
    }

    @Test
    void 걷는_그림은_0점1초마다_다음_장으로_넘어가고_네_장_뒤에_처음으로_돌아온다() {
        BufferedImage first = walking(0.0);

        assertSame(first, walking(0.09));
        assertNotSame(first, walking(0.11));
        assertNotSame(walking(0.11), walking(0.21));
        assertNotSame(walking(0.21), walking(0.31));
        assertSame(first, walking(0.41));
    }

    @Test
    void 고정_틱으로_걸으면_걷는_그림은_정확히_여섯_틱마다_넘어간다() {
        // 게임 루프처럼 1/60초씩 더한 시간은 0.1초 경계에서 0.0999…가 된다
        List<BufferedImage> cycle = List.of(walking(0.05), walking(0.15), walking(0.25), walking(0.35));
        double walkTime = 0;
        for (int tick = 1; tick <= 240; tick++) {
            walkTime += 1.0 / 60;
            assertSame(cycle.get(tick / 6 % 4), walking(walkTime), tick + "번째 틱");
        }
    }

    @Test
    void 캐릭터와_방향마다_다른_그림을_쓴다() {
        assertNotSame(SPRITES.frame(Mascot.SANGSANGBUGI, Direction.LEFT, false, 0),
                SPRITES.frame(Mascot.SANGSANGBUGI, Direction.RIGHT, false, 0));
        assertNotSame(SPRITES.frame(Mascot.SANGSANGBUGI, Direction.DOWN, false, 0),
                SPRITES.frame(Mascot.KKUKKU, Direction.DOWN, false, 0));
    }

    @Test
    void 맵에는_고른_캐릭터의_그림을_가로_가운데와_발끝_높이를_맞춰_그린다() {
        TileMap map = MapLoader.parse("fixture/character", List.of("...", "...", ".1."));
        // 기본 캐릭터가 아닌 캐릭터로, 고른 캐릭터를 그리는지 본다
        GameWorld world = new GameWorld(map, new PlayerProfile("tester", Mascot.KKUKKU));
        Player player = world.localPlayer();

        // 처음에는 아래를 보고 서 있다
        BufferedImage front = SPRITES.frame(Mascot.KKUKKU, Direction.DOWN, false, 0);
        int top = assertDrawn(front, world);
        // 머리는 캐릭터가 서 있는 칸(2행) 위로 솟는다
        assertTrue(top < 2 * TileMap.TILE_SIZE, "그림 위쪽 " + top);

        // 왼쪽으로 7프레임 걸으면 왼쪽을 보는 걷는 그림의 둘째 장이다
        for (int i = 0; i < 7; i++) {
            world.update(1.0 / 60, Direction.LEFT);
        }
        BufferedImage walkingLeft = SPRITES.frame(Mascot.KKUKKU, Direction.LEFT, true, player.walkTime());
        assertNotSame(SPRITES.frame(Mascot.KKUKKU, Direction.LEFT, true, 0), walkingLeft);
        assertDrawn(walkingLeft, world);
    }

    /** world를 그린 그림에서 캐릭터 자리의 픽셀이 expected와 같은지 본다. 그림을 놓은 위쪽 좌표를 돌려준다. */
    private static int assertDrawn(BufferedImage expected, GameWorld world) {
        TileMap map = world.map();
        Player player = world.localPlayer();
        BufferedImage canvas = new BufferedImage(map.pixelWidth(), map.pixelHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        new WorldRenderer(Tileset.forTheme("fixture"), SPRITES).drawObjects(g, world);
        g.dispose();

        int left = (int) Math.round(player.x()) - CharacterSprites.FRAME_SIZE / 2;
        int top = (int) Math.round(player.y()) + WorldRenderer.FEET_BELOW_CENTER - CharacterSprites.FEET_Y;
        int compared = 0;
        for (int y = 0; y < CharacterSprites.FRAME_SIZE; y++) {
            for (int x = 0; x < CharacterSprites.FRAME_SIZE; x++) {
                int rgb = expected.getRGB(x, y);
                // 그림의 투명한 곳에는 바닥이나 그림자가 비친다
                if ((rgb >>> 24) != 0) {
                    assertEquals(rgb, canvas.getRGB(left + x, top + y), "(%d, %d)".formatted(x, y));
                    compared++;
                }
            }
        }
        assertTrue(compared > 1000, "비교한 픽셀 " + compared);
        return top;
    }

    private static BufferedImage walking(double walkTime) {
        return SPRITES.frame(Mascot.SANGSANGBUGI, Direction.DOWN, true, walkTime);
    }

    private static int lowestOpaqueRow(BufferedImage image) {
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return y;
                }
            }
        }
        return -1;
    }
}

package hansung.arcade.ingame.render;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.balloon.Explosion;
import hansung.arcade.ingame.balloon.WaterBalloon;
import hansung.arcade.ingame.balloon.WaterCell;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 물풍선·물줄기·탈락 연출을 그림에 그려 자리와 시간을 확인한다 */
class WorldRendererBalloonTest {

    private static final int T = TileMap.TILE_SIZE;
    private static final double TICK = 1.0 / 60;
    private static final CharacterSprites CHARACTERS = CharacterSprites.load();
    private static final BalloonSprites BALLOONS = BalloonSprites.load();
    private static final WorldRenderer RENDERER = new WorldRenderer(Tileset.forTheme("fixture"), CHARACTERS, BALLOONS);

    /** 1P가 (1, 0)에 물풍선을 놓고 두 칸 아래로 비켜선 게임 */
    private static GameWorld placedBalloon() {
        GameWorld world = new GameWorld(MapLoader.parse("fixture/balloon", List.of(".1...", ".....", ".....")),
                new PlayerProfile("부기", Mascot.SANGSANGBUGI));
        Player me = world.localPlayer();
        world.update(TICK, Map.of(me.id(), new PlayerCommand(null, true, false)));
        for (int i = 0; i < 30; i++) {
            world.update(TICK, Map.of(me.id(), PlayerCommand.moving(Direction.DOWN)));
        }
        assertEquals(2, me.row());
        return world;
    }

    private static BufferedImage draw(GameWorld world) {
        BufferedImage canvas = new BufferedImage(world.map().pixelWidth(), world.map().pixelHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        RENDERER.drawObjects(g, world);
        g.dispose();
        return canvas;
    }

    /** sprite의 완전히 불투명한 픽셀이 canvas의 (left, top)부터 그대로 찍혔는지. 비교한 픽셀 수를 돌려준다 */
    private static int assertDrawnAt(BufferedImage sprite, BufferedImage canvas, int left, int top) {
        int compared = 0;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                int rgb = sprite.getRGB(x, y);
                int cx = left + x;
                int cy = top + y;
                if ((rgb >>> 24) == 0xFF && cx >= 0 && cy >= 0 && cx < canvas.getWidth() && cy < canvas.getHeight()) {
                    assertEquals(rgb, canvas.getRGB(cx, cy), "(%d, %d)".formatted(x, y));
                    compared++;
                }
            }
        }
        return compared;
    }

    @Test
    void 놓인_물풍선은_몸체_가운데를_칸_가운데에_맞추고_그림자는_반투명하게_그린다() {
        GameWorld world = placedBalloon();
        WaterBalloon balloon = world.balloonField().balloons().get(0);
        BufferedImage canvas = draw(world);

        BufferedImage sprite = BALLOONS.balloon(balloon.age());
        int left = T + T / 2 - BalloonSprites.BALLOON_CENTER_X;
        int top = T / 2 - BalloonSprites.BALLOON_CENTER_Y;
        assertTrue(assertDrawnAt(sprite, canvas, left, top) > 1000);
        // 그림의 반투명한 그림자 자리는 화면에서도 반투명하다
        int shadows = 0;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                int alpha = sprite.getRGB(x, y) >>> 24;
                int cx = left + x;
                int cy = top + y;
                if (alpha > 0 && alpha < 0xFF && cx >= 0 && cy >= 0) {
                    int drawn = canvas.getRGB(cx, cy) >>> 24;
                    assertTrue(Math.abs(drawn - alpha) <= 1, "(%d, %d) 알파 %d → %d".formatted(x, y, alpha, drawn));
                    shadows++;
                }
            }
        }
        assertTrue(shadows > 100, "반투명 픽셀 " + shadows);
    }

    @Test
    void 물줄기는_칸마다_그_칸의_모양_그림을_칸_왼쪽_위에_맞춰_그린다() {
        GameWorld world = placedBalloon();
        while (world.balloonField().explosions().isEmpty()) {
            world.update(TICK, Map.of());
        }
        // 0.15초 뒤 두 번째 장
        for (int i = 0; i < 9; i++) {
            world.update(TICK, Map.of());
        }
        Explosion explosion = world.balloonField().explosions().get(0);
        assertEquals(1, explosion.frame());
        BufferedImage canvas = draw(world);

        for (WaterCell cell : explosion.cells()) {
            if (cell.part() != WaterCell.Part.END || cell.direction() != Direction.DOWN) {
                assertTrue(assertDrawnAt(BALLOONS.water(cell, 1), canvas, cell.col() * T, cell.row() * T) > 300,
                        cell.toString());
            }
        }
        // 위쪽은 맵 끝이라 물줄기가 없다. 아래쪽 끝 칸은 아랫줄로 비켜선 캐릭터의 머리가 덮는다
        assertEquals(4, explosion.cells().size());
    }

    @Test
    void 탈락하면_포물선으로_한_칸_솟았다_1초에_떨어지고_0_1초씩_깜빡이다_사라진다() {
        Player player = new Player(1, 0, 0);
        TileMap map = MapLoader.parse("fixture/open", List.of("1...."));
        player.eliminate();

        assertEquals(0, WorldRenderer.liftOf(player));
        tick(player, map, 15);
        assertEquals(30, WorldRenderer.liftOf(player));
        tick(player, map, 15);
        assertEquals(WorldRenderer.DYING_JUMP_HEIGHT, WorldRenderer.liftOf(player));
        tick(player, map, 30);
        assertEquals(0, WorldRenderer.liftOf(player));
        assertTrue(WorldRenderer.isShown(player));

        // 1초부터 0.1초씩 보임·숨음을 다섯 번 되풀이한다
        boolean[] expected = {true, false, true, false, true, false, true, false, true, false};
        for (boolean shown : expected) {
            assertEquals(shown, WorldRenderer.isShown(player), "탈락 뒤 " + player.stateTime() + "초");
            tick(player, map, 6);
        }
        assertFalse(WorldRenderer.isShown(player));
        assertEquals(0, WorldRenderer.liftOf(player));
    }

    @Test
    void 사라진_캐릭터는_그리지_않는다() {
        GameWorld world = new GameWorld(MapLoader.parse("fixture/gone", List.of(".1...", ".....")),
                new PlayerProfile("부기", Mascot.SANGSANGBUGI));
        BufferedImage before = draw(world);
        assertTrue(countOpaque(before) > 1000);

        world.localPlayer().eliminate();
        for (int i = 0; i < 120; i++) {
            world.update(TICK, Map.of());
        }

        assertEquals(0, countOpaque(draw(world)));
    }

    private static void tick(Player player, TileMap map, int count) {
        for (int i = 0; i < count; i++) {
            player.update(null, TICK, map);
        }
    }

    private static int countOpaque(BufferedImage canvas) {
        int count = 0;
        for (int y = 0; y < canvas.getHeight(); y++) {
            for (int x = 0; x < canvas.getWidth(); x++) {
                if ((canvas.getRGB(x, y) >>> 24) != 0) {
                    count++;
                }
            }
        }
        return count;
    }
}

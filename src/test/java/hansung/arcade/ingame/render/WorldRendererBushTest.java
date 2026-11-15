package hansung.arcade.ingame.render;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.balloon.WaterBalloon;
import hansung.arcade.ingame.block.SlidingBlock;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 덤불 숨김, 바닥 아이템, 밀려 미끄러지는 상자를 그림에 그려 확인한다.
 * fixture 테마에는 덤불 그림이 없어 덤불은 칸 둘레 2px를 비운 임시 상자로 그려진다.
 */
class WorldRendererBushTest {

    private static final int T = TileMap.TILE_SIZE;
    private static final double TICK = 1.0 / 60;
    private static final Tileset TILESET = Tileset.forTheme("fixture");
    private static final ItemSprites ITEMS = ItemSprites.load();
    private static final BalloonSprites BALLOONS = BalloonSprites.load();
    private static final WorldRenderer RENDERER = new WorldRenderer(TILESET, CharacterSprites.load(), BALLOONS, ITEMS);

    private static BufferedImage draw(GameWorld world, Concealment concealment) {
        BufferedImage canvas = new BufferedImage(world.map().pixelWidth(), world.map().pixelHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        RENDERER.drawObjects(g, world, concealment);
        g.dispose();
        return canvas;
    }

    private static int alphaAt(BufferedImage canvas, int x, int y) {
        return canvas.getRGB(x, y) >>> 24;
    }

    /** (x0, y0)부터 폭 w · 높이 h 안의 불투명하거나 반투명한 픽셀 수 */
    private static int paintedIn(BufferedImage canvas, int x0, int y0, int w, int h) {
        int count = 0;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                if (alphaAt(canvas, x, y) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    /** 1P는 (0, 1), 2P는 덤불 (2, 1)에 선 게임. 두 번째 줄에 서야 머리가 윗줄로 솟은 자리를 볼 수 있다 */
    private static GameWorld opponentInBush() {
        TileMap map = MapLoader.parse("fixture/hide", List.of("...", "..T", "[spawn]", "...", "1.2"));
        return new GameWorld(map, List.of(new PlayerProfile("부기", Mascot.SANGSANGBUGI),
                new PlayerProfile("냥이", Mascot.HANSUNGNYANGI)));
    }

    @Test
    void 덤불에_숨은_상대는_칸_밖으로_솟은_머리까지_아예_그리지_않는다() {
        GameWorld world = opponentInBush();
        Player me = world.localPlayers().get(0);
        Player other = world.localPlayers().get(1);

        BufferedImage hidden = draw(world, Concealment.forViewers(world.map(), List.of(me)));
        BufferedImage shown = draw(world, Concealment.forViewers(world.map(), List.of(me, other)));

        // 덤불 칸 바로 윗줄 (2, 0) 칸은 비어 있어 머리를 그렸을 때만 칠해진다
        assertEquals(0, paintedIn(hidden, 2 * T, 0, T, T));
        assertTrue(paintedIn(shown, 2 * T, 0, T, T) > 100);
        // 숨지 않은 나는 그대로 그린다
        assertTrue(paintedIn(hidden, 0, 0, T, 2 * T) > 500);
    }

    @Test
    void 내가_들어간_덤불은_반투명하게_그려_안이_비쳐_보인다() {
        GameWorld world = opponentInBush();
        Player other = world.localPlayers().get(1);
        // 덤불 상자 왼쪽 아래 귀퉁이 안쪽. 캐릭터 그림과 그림자가 닿지 않는 자리다
        int x = 2 * T + 3;
        int y = T + T - 4;

        BufferedImage revealed = draw(world, Concealment.forViewers(world.map(), List.of(other)));
        BufferedImage covered = draw(world, Concealment.forViewers(world.map(), List.of()));

        assertEquals(0xFF, alphaAt(covered, x, y));
        assertEquals(Math.round(0xFF * WorldRenderer.REVEALED_BUSH_ALPHA), alphaAt(revealed, x, y), 1);
    }

    @Test
    void 덤불_안에서_터진_물줄기는_덤불_칸만_안_보이고_밖으로_뻗은_칸은_보인다() {
        TileMap map = MapLoader.parse("fixture/hide-water", List.of(".....", ".T...", ".....",
                "[spawn]", ".....", ".1...", "....."));
        GameWorld world = new GameWorld(map, new PlayerProfile("부기", Mascot.SANGSANGBUGI));
        Player me = world.localPlayer();
        world.update(TICK, Map.of(me.id(), new PlayerCommand(null, true, false)));
        // 물줄기 1칸이 닿지 않게 오른쪽으로 세 칸 비킨다
        for (int i = 0; i < 40; i++) {
            world.update(TICK, Map.of(me.id(), PlayerCommand.moving(Direction.RIGHT)));
        }
        while (world.balloonField().explosions().isEmpty()) {
            world.update(TICK, Map.of());
        }
        assertEquals(4, me.col());
        Concealment fromOutside = Concealment.of(world);
        Concealment fromInside = Concealment.forViewers(map, List.of(new Player(9, 1, 1)));

        BufferedImage outside = draw(world, fromOutside);
        BufferedImage inside = draw(world, fromInside);

        // 덤불 상자가 비워 둔 칸 둘레 2px: 물줄기를 그렸을 때만 칠해진다
        assertEquals(0, paintedIn(outside, T, T, 2, T));
        assertTrue(paintedIn(inside, T, T, 2, T) > 30);
        // 덤불 밖 왼쪽 칸 (0, 1)과 오른쪽 칸 (2, 1)의 물줄기는 둘 다 보인다
        assertTrue(paintedIn(outside, 0, T, T, T) > 1000);
        assertTrue(paintedIn(outside, 2 * T, T, T, T) > 1000);
    }

    @Test
    void 덤불_안의_물풍선과_아이템은_그_덤불에_아무도_없으면_안_보인다() {
        TileMap map = MapLoader.parse("fixture/hide-items", List.of("1..", ".TT", "..."));
        GameWorld world = new GameWorld(map, new PlayerProfile("부기", Mascot.SANGSANGBUGI));
        world.items().place(2, 1, ItemType.FLUID);
        Concealment nobody = Concealment.forViewers(map, List.of());
        Concealment inSecond = Concealment.forViewers(map, List.of(new Player(9, 2, 1)));

        // 아이템 그림은 칸 위쪽으로 떠 있어 덤불 상자 위 2px 틈에 걸친다
        assertEquals(0, paintedIn(draw(world, nobody), 2 * T, T, T, 2));
        assertTrue(paintedIn(draw(world, inSecond), 2 * T, T, T, 2) > 0);
    }

    @Test
    void 던진_물풍선은_숨은_덤불_칸_위를_나는_동안에도_보이고_숨은_덤불에_내려앉으면_숨는다() {
        // 1P가 (0, 1)에 놓고 오른쪽으로 던지면 덤불 (2, 1)~(5, 1) 위를 날아 덤불 (6, 1)에 내려앉는다
        TileMap map = MapLoader.parse("fixture/throw-over-bush", List.of(".......", "1.TTTTT", "......."));
        GameWorld world = new GameWorld(map, new PlayerProfile("부기", Mascot.SANGSANGBUGI));
        Player me = world.localPlayer();
        world.update(TICK, Map.of(me.id(), new PlayerCommand(null, true, false)));
        WaterBalloon balloon = world.balloonField().balloons().get(0);
        assertTrue(world.balloonField().throwBalloon(balloon, Direction.RIGHT));

        int overHiddenBush = 0;
        for (world.update(TICK, Map.of()); balloon.isFlying(); world.update(TICK, Map.of())) {
            Concealment concealment = Concealment.of(world);
            if (concealment.hides(TileMap.colAt(balloon.x()), TileMap.rowAt(balloon.y()))) {
                overHiddenBush++;
            }
            BufferedImage canvas = draw(world, concealment);
            int[] drawn = spritePixels(BALLOONS.balloon(balloon.age()), canvas,
                    (int) Math.round(balloon.x()) - BalloonSprites.BALLOON_CENTER_X,
                    (int) Math.round(balloon.y()) - BalloonSprites.BALLOON_CENTER_Y - (int) Math.round(balloon.height()));
            // 덤불 위라도 날아가는 물풍선 그림은 맨 위에 빠짐없이 그대로 찍힌다
            assertTrue(drawn[1] > 1000, "x " + balloon.x());
            assertEquals(drawn[1], drawn[0], "x " + balloon.x());
        }
        assertTrue(overHiddenBush > 10, "숨은 덤불 위를 난 틱 " + overHiddenBush);

        assertEquals(6, balloon.col());
        int left = 6 * T + T / 2 - BalloonSprites.BALLOON_CENTER_X;
        int top = T + T / 2 - BalloonSprites.BALLOON_CENTER_Y;
        BufferedImage hidden = draw(world, Concealment.of(world));
        BufferedImage revealed = draw(world, Concealment.forViewers(map, List.of(new Player(9, 6, 1))));
        // 숨은 덤불에서는 덤불 상자가 비워 둔 칸 둘레에도 물풍선이 보이지 않는다
        assertEquals(0, spritePixels(BALLOONS.balloon(balloon.age()), hidden, left, top)[0]);
        assertTrue(spritePixels(BALLOONS.balloon(balloon.age()), revealed, left, top)[0] > 0);
    }

    /**
     * sprite의 완전히 불투명한 픽셀 중 canvas 안에 드는 것을 (left, top)부터 놓아 본다.
     *
     * @return {canvas에 같은 색으로 찍힌 픽셀 수, canvas 안에 드는 픽셀 수}
     */
    private static int[] spritePixels(BufferedImage sprite, BufferedImage canvas, int left, int top) {
        int same = 0;
        int inside = 0;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                int rgb = sprite.getRGB(x, y);
                int cx = left + x;
                int cy = top + y;
                if ((rgb >>> 24) == 0xFF && cx >= 0 && cy >= 0 && cx < canvas.getWidth() && cy < canvas.getHeight()) {
                    inside++;
                    if (canvas.getRGB(cx, cy) == rgb) {
                        same++;
                    }
                }
            }
        }
        return new int[]{same, inside};
    }

    @Test
    void 바닥_아이템은_칸_가운데에_떠서_그린다() {
        TileMap map = MapLoader.parse("fixture/item", List.of("1..", "..."));
        GameWorld world = new GameWorld(map, new PlayerProfile("부기", Mascot.SANGSANGBUGI));
        world.items().place(2, 1, ItemType.ROLLER_SHOE);
        BufferedImage canvas = draw(world, Concealment.of(world));

        BufferedImage sprite = ITEMS.image(ItemType.ROLLER_SHOE);
        int left = 2 * T + (T - ItemType.IMAGE_SIZE) / 2;
        int top = WorldRenderer.itemTop(1, world.elapsedSeconds());
        int compared = 0;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                int rgb = sprite.getRGB(x, y);
                if ((rgb >>> 24) == 0xFF) {
                    assertEquals(rgb, canvas.getRGB(left + x, top + y), "(%d, %d)".formatted(x, y));
                    compared++;
                }
            }
        }
        assertTrue(compared > 300);
        assertTrue(top < T + 1, "칸 위로 떠 있다: " + top);
    }

    @Test
    void 아이템은_시간에_따라_위아래로_조금씩_흔들린다() {
        int base = WorldRenderer.itemTop(0, 0);

        assertEquals(base - WorldRenderer.ITEM_BOB, WorldRenderer.itemTop(0, WorldRenderer.ITEM_BOB_SECONDS / 4));
        assertEquals(base + WorldRenderer.ITEM_BOB, WorldRenderer.itemTop(0, WorldRenderer.ITEM_BOB_SECONDS * 3 / 4));
        assertEquals(base + T, WorldRenderer.itemTop(1, 0));
    }

    @Test
    void 밀린_상자는_떠난_칸_쪽으로_비켜_그렸다가_도착_칸에_닿는다() {
        TileMap map = MapLoader.parse("fixture/slide", List.of(".....", ".1P..", "....."));
        GameWorld world = new GameWorld(map, new PlayerProfile("부기", Mascot.SANGSANGBUGI));
        Player me = world.localPlayer();
        while (map.tileAt(3, 1) != Tile.PUSH_BOX) {
            world.update(TICK, Map.of(me.id(), PlayerCommand.moving(Direction.RIGHT)));
        }
        // 미는 사람이 따라오지 않게 손을 뗀다
        world.update(TICK, Map.of());
        SlidingBlock slide = world.boxPusher().slidingInto(3, 1).orElseThrow();
        TileSprite box = TILESET.sprite(Tile.PUSH_BOX, 3, 1);

        BufferedImage canvas = draw(world, Concealment.of(world));

        int left = 3 * T + slide.offsetX() + box.offsetX();
        int top = T + slide.offsetY() + box.offsetY();
        int compared = 0;
        // 상자 그림의 오른쪽 절반만 본다 (왼쪽은 따라오던 캐릭터가 덮을 수 있다)
        for (int y = 0; y < box.image().getHeight(); y++) {
            for (int x = box.image().getWidth() / 2; x < box.image().getWidth(); x++) {
                int rgb = box.image().getRGB(x, y);
                if ((rgb >>> 24) == 0xFF) {
                    assertEquals(rgb, canvas.getRGB(left + x, top + y), "(%d, %d)".formatted(x, y));
                    compared++;
                }
            }
        }
        assertTrue(compared > 200);
        assertTrue(slide.offsetX() < 0);
    }
}

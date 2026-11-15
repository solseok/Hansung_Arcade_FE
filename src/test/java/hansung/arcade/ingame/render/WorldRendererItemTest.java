package hansung.arcade.ingame.render;

import hansung.arcade.ingame.balloon.BalloonField;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.item.ItemActions;
import hansung.arcade.ingame.item.ItemEffects;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.item.PlayerGear;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 아이템 효과를 그릴 때 쓰는 높이·깜빡임 계산 */
class WorldRendererItemTest {

    private static final double TICK = 1.0 / 60;
    private static final TileMap MAP = MapLoader.parse("fixture/items", List.of("1...."));

    /** 0열에 서서 오른쪽을 보는 플레이어 */
    private static Player player() {
        Player player = new Player(1, 0, 0);
        player.update(Direction.RIGHT, 0, MAP);
        return player;
    }

    /** item을 먹고 바로 아이템 키로 쓴다 */
    private static PlayerGear using(Player player, ItemType item) {
        PlayerGear gear = new PlayerGear();
        ItemEffects.apply(player, gear, item);
        assertTrue(new ItemActions(MAP, new BalloonField(MAP)).useItem(player, gear));
        return gear;
    }

    private static void advance(PlayerGear gear, Player player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            gear.advance(TICK, player);
        }
    }

    @Test
    void 스프링_신발로_뛰면_한가운데에서_한_칸_높이까지_솟는다() {
        Player player = player();
        assertEquals(0, WorldRenderer.jumpLiftOf(new PlayerGear()));

        PlayerGear gear = using(player, ItemType.SPRING_SHOE);
        advance(gear, player, 12);

        assertEquals(WorldRenderer.JUMP_HEIGHT, WorldRenderer.jumpLiftOf(gear));
    }

    @Test
    void 그림을_올려_그리는_높이는_뛴_높이에_탱크_좌석_높이를_더한다() {
        Player player = player();
        assertEquals(0, WorldRenderer.figureLiftOf(player, new PlayerGear()));

        PlayerGear jumping = using(player, ItemType.SPRING_SHOE);
        advance(jumping, player, 12);
        assertEquals(WorldRenderer.JUMP_HEIGHT, WorldRenderer.figureLiftOf(player, jumping));

        PlayerGear riding = new PlayerGear();
        ItemEffects.apply(player, riding, ItemType.TANK);
        assertEquals(WorldRenderer.TANK_SEAT, WorldRenderer.figureLiftOf(player, riding));
        // 갇힌 그림은 탱크 없이 땅에 그린다
        player.trap(2);
        assertEquals(0, WorldRenderer.figureLiftOf(player, riding));
    }

    @Test
    void 보호막은_끝나기_직전에만_깜빡인다() {
        Player player = player();
        PlayerGear gear = using(player, ItemType.SHIELD);

        assertTrue(WorldRenderer.isShieldVisible(gear, 0.15));
        advance(gear, player, 150);
        // 0.5초 남았다. 0.1초씩 보였다 숨었다 한다
        assertTrue(WorldRenderer.isShieldVisible(gear, 0.05));
        assertFalse(WorldRenderer.isShieldVisible(gear, 0.15));
    }

    @Test
    void 보호막은_다섯_캐릭터의_서_있는_그림과_걷는_그림을_머리_꼭대기까지_감싼다() {
        double rx = WorldRenderer.SHIELD_RADIUS_X;
        double ry = WorldRenderer.SHIELD_RADIUS_Y;
        for (Mascot mascot : Mascot.values()) {
            for (String pose : List.of("front", "back", "left", "right",
                    "walk_front", "walk_back", "walk_left", "walk_right")) {
                String path = CharacterSprites.DIRECTORY + mascot.id() + "_" + pose + ".bmp";
                for (BufferedImage frame : SpriteSheet.find(path).orElseThrow().frames(CharacterSprites.FRAME_SIZE)) {
                    for (int y = 0; y < frame.getHeight(); y++) {
                        for (int x = 0; x < frame.getWidth(); x++) {
                            if ((frame.getRGB(x, y) >>> 24) == 0) {
                                continue;
                            }
                            // 픽셀 네 귀퉁이가 모두 막 테두리 안이어야 한다. 가로는 그림 가운데, 세로는 막 가운데 기준
                            for (int[] corner : new int[][]{{x, y}, {x + 1, y}, {x, y + 1}, {x + 1, y + 1}}) {
                                double dx = corner[0] - CharacterSprites.FRAME_SIZE / 2.0;
                                double dy = corner[1] - (CharacterSprites.FEET_Y - WorldRenderer.SHIELD_CENTER_ABOVE_FEET);
                                assertTrue((dx / rx) * (dx / rx) + (dy / ry) * (dy / ry) <= 1,
                                        "%s %s (%d, %d)".formatted(mascot, pose, x, y));
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void 탱크가_부서진_뒤_맞지_않는_동안에만_캐릭터가_깜빡인다() {
        PlayerGear gear = new PlayerGear();
        ItemEffects.apply(player(), gear, ItemType.TANK);
        assertFalse(WorldRenderer.isBlinkedOut(gear, 0.15));

        gear.breakTank();

        assertFalse(WorldRenderer.isBlinkedOut(gear, 0.05));
        assertTrue(WorldRenderer.isBlinkedOut(gear, 0.15));
    }
}

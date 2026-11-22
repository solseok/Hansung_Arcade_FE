package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.item.ScriptedRandom;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.match.MatchMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 한 틱에 일어난 일이 알맞은 효과음({@link GameSound})으로 모이는지 */
class GameWorldSoundTest {

    private static final double TICK = 1.0 / 60;
    private static final PlayerCommand BALLOON = new PlayerCommand(null, true, false);
    private static final PlayerCommand ITEM = new PlayerCommand(null, false, true);
    /** 물풍선이 놓인 틱부터 터지는 틱까지 */
    private static final int FUSE_TICKS = 120;

    private static GameWorld solo(RandomGenerator random, String... rows) {
        return new GameWorld(MapLoader.parse("fixture/sound", List.of(rows)),
                List.of(new PlayerProfile("부기", Mascot.SANGSANGBUGI)), MatchMode.SOLO, random);
    }

    /** player가 col번째 칸에 들어설 때까지 오른쪽으로 걷고, 그동안 난 소리를 모은다 */
    private static List<GameSound> walkTo(GameWorld world, Player player, int col) {
        List<GameSound> heard = new ArrayList<>();
        for (int i = 0; i < 120 && player.col() < col; i++) {
            heard.addAll(ticks(world, 1, Map.of(player.id(), PlayerCommand.moving(Direction.RIGHT))));
        }
        assertEquals(col, player.col());
        return heard;
    }

    private static List<GameSound> ticks(GameWorld world, int count, Map<Integer, PlayerCommand> commands) {
        List<GameSound> heard = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            world.update(TICK, commands);
            heard.addAll(world.takeSounds());
        }
        return heard;
    }

    @Test
    void 물풍선을_놓으면_놓는_소리가_나고_터지면_터지는_소리가_난다() {
        GameWorld world = solo(ScriptedRandom.noDrops(), "..1..");
        int me = world.localPlayer().id();

        assertEquals(List.of(GameSound.BALLOON_PLACED), ticks(world, 1, Map.of(me, BALLOON)));
        List<GameSound> later = ticks(world, FUSE_TICKS, Map.of());

        assertTrue(later.contains(GameSound.EXPLODE), later.toString());
        assertFalse(later.contains(GameSound.EXPLODE_MULTI));
    }

    @Test
    void 같은_틱에_여럿이_터지면_연쇄_폭발_소리가_난다() {
        GameWorld world = solo(ScriptedRandom.noDrops(), ".......", "..1....", ".......");
        Player me = world.localPlayer();
        // 첫 물풍선을 놓고 바로 옆 칸에 두 번째를 놓으면, 첫 물풍선이 터지며 두 번째를 같은 틱에 터뜨린다
        me.addBalloons(1);
        ticks(world, 1, Map.of(me.id(), BALLOON));
        walkTo(world, me, 3);
        ticks(world, 1, Map.of(me.id(), BALLOON));
        assertEquals(2, world.balloonField().balloons().size());

        List<GameSound> heard = ticks(world, FUSE_TICKS, Map.of());

        assertTrue(heard.contains(GameSound.EXPLODE_MULTI), heard.toString());
    }

    @Test
    void 블럭에서_아이템이_나오면_나오는_소리가_나고_먹으면_먹는_소리가_난다() {
        GameWorld world = solo(ScriptedRandom.alwaysDrops(ItemType.BUBBLE), "B1...");
        Player me = world.localPlayer();

        ticks(world, 1, Map.of(me.id(), BALLOON));
        // 물풍선에서 비켜 서서 터지기를 기다린다
        ticks(world, 30, Map.of(me.id(), PlayerCommand.moving(Direction.RIGHT)));
        List<GameSound> popped = ticks(world, FUSE_TICKS, Map.of());
        assertTrue(popped.contains(GameSound.ITEM_POP), popped.toString());

        List<GameSound> eaten = ticks(world, 90, Map.of(me.id(), PlayerCommand.moving(Direction.LEFT)));
        assertTrue(eaten.contains(GameSound.ITEM_EATEN), eaten.toString());
        assertTrue(world.items().all().isEmpty());
    }

    @Test
    void 물줄기에_갇히면_갇히는_소리가_나고_시간이_다_돼_탈락하면_탈락_소리가_난다() {
        GameWorld world = solo(ScriptedRandom.noDrops(), "..1..");
        Player me = world.localPlayer();

        ticks(world, 1, Map.of(me.id(), BALLOON));
        List<GameSound> trapped = ticks(world, FUSE_TICKS, Map.of());
        assertEquals(PlayerState.TRAPPED, me.state());
        assertTrue(trapped.contains(GameSound.TRAPPED), trapped.toString());

        List<GameSound> died = ticks(world, (int) Math.round(Player.TRAP_SECONDS / TICK) + 2, Map.of());
        assertFalse(me.isAlive());
        assertTrue(died.contains(GameSound.DIE), died.toString());
    }

    @Test
    void 보호막을_쓰면_보호막_소리가_난다() {
        GameWorld world = solo(ScriptedRandom.noDrops(), "1....");
        Player me = world.localPlayer();
        world.items().place(me.col(), me.row(), ItemType.SHIELD);
        assertEquals(List.of(GameSound.ITEM_EATEN), ticks(world, 1, Map.of()));

        assertEquals(List.of(GameSound.SHIELD), ticks(world, 1, Map.of(me.id(), ITEM)));
    }

    @Test
    void 스프링_신발로_뛰면_스프링_소리가_난다() {
        GameWorld world = solo(ScriptedRandom.noDrops(), "1....", ".....", ".....");
        Player me = world.localPlayer();
        world.items().place(me.col(), me.row(), ItemType.SPRING_SHOE);
        ticks(world, 1, Map.of());
        assertEquals(Direction.DOWN, me.facing());

        assertEquals(List.of(GameSound.SPRING), ticks(world, 1, Map.of(me.id(), ITEM)));
    }

    @Test
    void 탱크가_물줄기를_대신_맞으면_부서지는_소리가_난다() {
        GameWorld world = solo(ScriptedRandom.noDrops(), "..1..");
        Player me = world.localPlayer();
        world.items().place(me.col(), me.row(), ItemType.TANK);
        ticks(world, 1, Map.of());
        assertTrue(world.gear(me).hasTank());

        ticks(world, 1, Map.of(me.id(), BALLOON));
        List<GameSound> heard = ticks(world, FUSE_TICKS, Map.of());

        assertFalse(world.gear(me).hasTank());
        assertEquals(PlayerState.NORMAL, me.state());
        assertTrue(heard.contains(GameSound.TANK_BROKEN), heard.toString());
        assertFalse(heard.contains(GameSound.TRAPPED));
    }

    @Test
    void 덤불에_들어가면_덤불_소리가_난다() {
        GameWorld world = solo(ScriptedRandom.noDrops(), "1T...");
        Player me = world.localPlayer();

        List<GameSound> heard = walkTo(world, me, 1);
        heard.addAll(ticks(world, 30, Map.of()));

        assertEquals(1, me.col());
        assertEquals(1, heard.stream().filter(sound -> sound == GameSound.BUSH).count());
    }

    @Test
    void 비행기가_나오면_비행기_소리가_나고_아이템을_떨어뜨리면_떨어뜨리는_소리가_난다() {
        // 난수 0.9: 7 x 3칸 가운데 18·19번째 칸(맨 아랫줄 4·5열)에 떨어뜨린다
        GameWorld world = solo(ScriptedRandom.noDrops(), "..1....", ".......", ".......");
        List<GameSound> heard = new ArrayList<>();
        int ticks = 0;
        while (!heard.contains(GameSound.AIRPLANE)) {
            assertTrue(++ticks < 70 * 60, "1분 59초가 되어도 비행기 소리가 나지 않습니다");
            heard.addAll(ticks(world, 1, Map.of()));
        }
        assertEquals(119, world.match().clock().remainingSeconds());
        assertEquals(1, world.airDrop().launches());

        heard.clear();
        for (int i = 0; i < 10 * 60 && !world.airDrop().flights().isEmpty(); i++) {
            heard.addAll(ticks(world, 1, Map.of()));
        }

        assertEquals(2, world.airDrop().releases());
        assertEquals(2, heard.stream().filter(sound -> sound == GameSound.AIR_DROP).count(), heard.toString());
        assertFalse(heard.contains(GameSound.AIRPLANE), "비행기가 사라질 때 다시 소리가 났습니다");
    }

    @Test
    void 같은_소리는_한_번_꺼낼_때_한_번만_담기고_꺼내면_비워진다() {
        GameWorld world = solo(ScriptedRandom.noDrops(), ".....", "..1..", ".....");
        Player me = world.localPlayer();
        me.addBalloons(1);
        world.update(TICK, Map.of(me.id(), BALLOON));
        for (int i = 0; i < 20; i++) {
            world.update(TICK, Map.of(me.id(), PlayerCommand.moving(Direction.RIGHT)));
        }
        world.update(TICK, Map.of(me.id(), BALLOON));
        assertEquals(2, world.balloonField().balloons().size());

        assertEquals(List.of(GameSound.BALLOON_PLACED), world.takeSounds());
        assertTrue(world.takeSounds().isEmpty());
    }
}

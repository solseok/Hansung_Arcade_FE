package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.match.MatchClock;
import hansung.arcade.ingame.match.MatchMode;
import hansung.arcade.ingame.match.StartIntro;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 경기 시작 연출 "Ready... Start!": START!! 글자가 다 사라질 때까지는 조작할 수 없고,
 * 경기 시계는 START!! 글자가 뜬 뒤부터 흐른다 (크아처럼 조작할 수 있게 될 때 타이머는 02:59).
 */
class GameWorldIntroTest {

    private static final double TICK = 1.0 / 60;
    private static final int READY_TICKS = (int) Math.round(StartIntro.READY_SECONDS / TICK);
    private static final int START_TICKS = (int) Math.round(StartIntro.START_SECONDS / TICK);
    private static final int INTRO_TICKS = READY_TICKS + START_TICKS;
    private static final PlayerCommand BALLOON = new PlayerCommand(null, true, false);

    private static GameWorld world() {
        return new GameWorld(MapLoader.parse("fixture/intro", List.of("1....", ".....", "....2")),
                List.of(new PlayerProfile("부기", Mascot.SANGSANGBUGI), new PlayerProfile("냥이", Mascot.HANSUNGNYANGI)),
                MatchMode.SOLO);
    }

    /** count틱 동안 1P는 오른쪽 키, 2P는 물풍선 키를 누르고 있고, 그동안 난 소리를 모두 모은다 */
    private static List<GameSound> hold(GameWorld world, int count) {
        List<GameSound> heard = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Player first = world.localPlayers().get(0);
            Player second = world.localPlayers().get(1);
            world.update(TICK, Map.of(first.id(), PlayerCommand.moving(Direction.RIGHT), second.id(), BALLOON));
            heard.addAll(world.takeSounds());
        }
        return heard;
    }

    @Test
    void 연출을_켜지_않으면_바로_조작할_수_있다() {
        GameWorld world = world();
        double startX = world.localPlayer().x();

        hold(world, 10);

        assertFalse(world.intro().isRunning());
        assertTrue(world.localPlayer().x() > startX);
    }

    @Test
    void 연출을_켜면_READY_소리를_내고_START_글자가_사라질_때까지_움직이지도_물풍선을_놓지도_않는다() {
        GameWorld world = world();
        double startX = world.localPlayer().x();

        world.beginIntro();
        assertEquals(List.of(GameSound.READY), world.takeSounds());
        assertEquals(StartIntro.Phase.READY, world.intro().phase());

        List<GameSound> heard = hold(world, INTRO_TICKS - 1);

        assertEquals(StartIntro.Phase.START, world.intro().phase());
        assertEquals(startX, world.localPlayer().x());
        assertTrue(world.balloonField().balloons().isEmpty());
        assertEquals(List.of(GameSound.START), heard, "READY 다음에 START가 한 번 난다");
    }

    @Test
    void START_소리는_화면이_다_밝아져_START_글자가_뜨는_틱에_난다() {
        GameWorld world = world();
        world.beginIntro();
        world.takeSounds();

        assertTrue(hold(world, READY_TICKS - 1).isEmpty());
        assertEquals(List.of(GameSound.START), hold(world, 1));
        assertEquals(StartIntro.Phase.START, world.intro().phase());
    }

    @Test
    void 화면이_밝아지는_동안은_시계가_멈춰_있고_START_글자가_뜬_뒤부터_흐른다() {
        GameWorld world = world();
        world.beginIntro();

        hold(world, READY_TICKS);
        assertEquals(0, world.match().clock().elapsedTicks());
        assertEquals(MatchClock.DURATION_SECONDS, world.match().clock().remainingSeconds());

        hold(world, START_TICKS);
        assertFalse(world.intro().isRunning());
        assertEquals(START_TICKS, world.match().clock().elapsedTicks());
        // 조작할 수 있게 될 때 타이머는 이미 02:59다
        assertEquals(MatchClock.DURATION_SECONDS - 1, world.match().clock().remainingSeconds());
    }

    @Test
    void 연출이_끝난_틱부터_누르고_있던_키로_움직인다() {
        GameWorld world = world();
        double startX = world.localPlayer().x();
        world.beginIntro();

        hold(world, INTRO_TICKS);
        assertFalse(world.intro().isRunning());
        assertEquals(startX, world.localPlayer().x());

        List<GameSound> heard = hold(world, 1);
        assertTrue(world.localPlayer().x() > startX);
        assertEquals(1, world.balloonField().balloons().size());
        assertTrue(heard.contains(GameSound.BALLOON_PLACED));
        // 연출이 끝난 뒤에도 화살표가 잠깐 남는다
        assertTrue(world.intro().showsArrow());
    }

    @Test
    void 연출_중에도_채팅은_올라가고_시간이_흐른다() {
        GameWorld world = world();
        world.beginIntro();
        world.sendChat("안녕");

        assertEquals("안녕", world.speechBubbles().textOf(world.localPlayer().id()));
        hold(world, INTRO_TICKS);
        // 연출 동안에도 게임 시간은 흐른다 (말풍선·채팅이 사라지는 시간)
        assertEquals(StartIntro.TOTAL_SECONDS, world.elapsedSeconds(), 1e-6);
    }
}

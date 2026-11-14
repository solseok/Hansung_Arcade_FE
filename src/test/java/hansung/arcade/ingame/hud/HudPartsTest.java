package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.Tileset;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class HudPartsTest {

    @Test
    void 맵_번호는_이름_끝_숫자_두_자리다() {
        assertEquals("10", MapLabel.numberOf("빌리지 10"));
        assertEquals("08", MapLabel.numberOf("캠프 08"));
        assertEquals("08", MapLabel.numberOf("캠프 8"));
        assertEquals("23", MapLabel.numberOf("맵 123 "));
        assertEquals("", MapLabel.numberOf("이름만"));
    }

    @Test
    void 대표_블록은_테마의_일반_블록이다() {
        Tileset fixture = Tileset.forTheme("fixture");
        TileMap map = MapLoader.parse("fixture/m", List.of("1PP"));

        assertSame(fixture.sprite(Tile.BLOCK, 0, 0), MapLabel.representativeBlock(map, fixture));
    }

    @Test
    void 블록_그림이_하나도_없으면_대표_블록도_없다() {
        Tileset floorOnly = Tileset.forTheme("fixture-floor");
        TileMap map = MapLoader.parse("fixture-floor/m", List.of("1BB"));

        // fixture-floor 에는 블록 그림이 하나도 없다
        assertNull(MapLabel.representativeBlock(map, floorOnly));
    }

    @Test
    void 타이머는_MM_ss_로_99분59초에서_멈춘다() {
        assertEquals("00:00", PlayerList.formatTime(0));
        assertEquals("01:14", PlayerList.formatTime(74));
        assertEquals("03:00", PlayerList.formatTime(180));
        assertEquals("99:59", PlayerList.formatTime(100 * 60));
        assertEquals("00:00", PlayerList.formatTime(-3));
    }

    @Test
    void 타이머는_남은_경기_시간을_03분00초부터_1초씩_줄여_보여_준다() {
        GameWorld world = new GameWorld(MapLoader.parse("fixture/timer", List.of("1....")),
                new PlayerProfile("tester", Mascot.KKUKKU));
        assertEquals("03:00", PlayerList.timerText(world));

        tick(world, 30);
        assertEquals("03:00", PlayerList.timerText(world));
        tick(world, 30);
        assertEquals("02:59", PlayerList.timerText(world));

        // 61초
        tick(world, 60 * 60);
        assertEquals("01:59", PlayerList.timerText(world));

        // 179.5초
        tick(world, 118 * 60 + 30);
        assertEquals("00:01", PlayerList.timerText(world));
        tick(world, 30);
        assertEquals("00:00", PlayerList.timerText(world));

        // 시간이 다 되면 멈춘다
        tick(world, 600);
        assertEquals("00:00", PlayerList.timerText(world));
    }

    /** 게임 루프처럼 1/60초씩 n번 갱신한다 */
    private static void tick(GameWorld world, int n) {
        for (int i = 0; i < n; i++) {
            world.update(1.0 / 60, null);
        }
    }
}

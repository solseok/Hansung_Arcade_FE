package hansung.arcade.ingame.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapLoaderTest {

    @Test
    void 기본_맵을_리소스에서_읽는다() {
        TileMap map = MapLoader.loadResource("camp/camp08");

        assertEquals("캠프 08", map.name());
        assertEquals("camp", map.theme());
        assertEquals(15, map.columns());
        assertEquals(13, map.rows());
        assertEquals(List.of(new Point(0, 6), new Point(3, 3), new Point(0, 1), new Point(2, 12),
                new Point(14, 6), new Point(11, 9), new Point(14, 11), new Point(12, 0)), map.spawnPoints());
    }

    @ParameterizedTest
    @ValueSource(strings = {"camp/camp08", "patrit/patrit14", "village/village10"})
    void 리소스_맵의_스폰_지점은_지나갈_수_있고_상하좌우_중_한_곳_이상은_지나가거나_부술_수_있다(String id) {
        TileMap map = MapLoader.loadResource(id);

        for (Point spawn : map.spawnPoints()) {
            assertFalse(map.isSolid(spawn.x, spawn.y), "스폰 지점이 막혀 있음: " + spawn);
            // 블록·밀기 상자로 둘러싸여도 부수면 나갈 수 있다 (패트릿 6번처럼)
            boolean hasExit = canOpen(map, spawn.x - 1, spawn.y) || canOpen(map, spawn.x + 1, spawn.y)
                    || canOpen(map, spawn.x, spawn.y - 1) || canOpen(map, spawn.x, spawn.y + 1);
            assertTrue(hasExit, "스폰 지점에서 나갈 길이 없음: " + spawn);
        }
    }

    private static boolean canOpen(TileMap map, int col, int row) {
        // 맵 바깥은 부술 수 없는 벽이다
        return map.isInside(col, row) && (!map.isSolid(col, row) || map.tileAt(col, row).isDestructible());
    }

    @Test
    void 글자를_타일로_바꾸고_스폰_번호순으로_정렬한다() {
        TileMap map = MapLoader.parse("test", List.of(
                "name=테스트",
                "2.#",
                "TB1"));

        assertEquals("테스트", map.name());
        assertEquals(Tile.FLOOR, map.tileAt(0, 0));
        assertEquals(Tile.STATUE, map.tileAt(2, 0));
        assertEquals(Tile.BUSH, map.tileAt(0, 1));
        assertEquals(Tile.BLOCK, map.tileAt(1, 1));
        assertEquals(Tile.FLOOR, map.tileAt(2, 1));
        assertEquals(List.of(new Point(2, 1), new Point(0, 0)), map.spawnPoints());
    }

    @Test
    void 배치_규칙의_모든_기호를_읽는다() {
        TileMap map = MapLoader.parse("test", List.of("1B-TPcba><^v#W!MMHY_|=OJK"));

        List<Tile> expected = List.of(Tile.FLOOR, Tile.BLOCK, Tile.SIDE_BLOCK, Tile.BUSH, Tile.PUSH_BOX,
                Tile.HARD_BLOCK_3, Tile.HARD_BLOCK_2, Tile.HARD_BLOCK_1,
                Tile.ONE_WAY_RIGHT, Tile.ONE_WAY_LEFT, Tile.ONE_WAY_UP, Tile.ONE_WAY_DOWN,
                Tile.STATUE, Tile.WIRE, Tile.SPIKE, Tile.MISSILE, Tile.MISSILE, Tile.HOUSE_1, Tile.TREE,
                Tile.ROAD, Tile.ROAD_LANE, Tile.CROSSWALK, Tile.BLOCK_2, Tile.HOUSE_2, Tile.HOUSE_3);
        for (int col = 0; col < expected.size(); col++) {
            assertEquals(expected.get(col), map.tileAt(col, 0), "열 " + col);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"camp/camp08", "patrit/patrit14", "village/village10"})
    void 리소스의_모든_맵을_읽을_수_있다(String id) {
        TileMap map = MapLoader.loadResource(id);

        assertFalse(map.spawnPoints().isEmpty());
        assertEquals(id.substring(0, id.indexOf('/')), map.theme());
    }

    @Test
    void 덤불과_바닥_가시와_길은_지나갈_수_있다() {
        TileMap map = MapLoader.parse("test", List.of("1T!_|=B"));

        for (int col = 1; col <= 5; col++) {
            assertFalse(map.isSolid(col, 0), "열 " + col);
        }
        assertTrue(map.isSolid(6, 0));
    }

    @Test
    void 맵_id의_폴더가_테마가_된다() {
        assertEquals("camp", MapLoader.parse("camp/test", List.of("1")).theme());
        assertEquals("", MapLoader.parse("test", List.of("1")).theme());
    }

    @Test
    void 미사일은_두_칸씩_짝을_지어_첫_칸에서만_그린다() {
        TileMap map = MapLoader.parse("test", List.of("1MMMM"));

        assertTrue(map.isTileOrigin(1, 0));
        assertFalse(map.isTileOrigin(2, 0));
        assertTrue(map.isTileOrigin(3, 0));
        assertFalse(map.isTileOrigin(4, 0));
        assertTrue(map.isTileOrigin(0, 0));
    }

    @Test
    void 미사일이_한_칸만_있으면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1.M.")));
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1MMM")));
    }

    @Test
    void 맵_바깥은_막힌_칸이다() {
        TileMap map = MapLoader.parse("test", List.of("1.", ".."));

        assertTrue(map.isSolid(-1, 0));
        assertTrue(map.isSolid(0, -1));
        assertTrue(map.isSolid(2, 0));
        assertTrue(map.isSolid(0, 2));
    }

    @Test
    void 줄_길이가_다르면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1..", "..")));
    }

    @Test
    void 모르는_글자가_있으면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1?")));
    }

    @Test
    void 스폰_지점이_없으면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("..")));
    }

    @Test
    void 스폰_번호가_1부터_빠짐없이_이어지지_않으면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1.3")));
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of(".2.")));
    }

    @Test
    void 파일_맨_앞의_BOM은_무시한다() {
        TileMap map = MapLoader.parse("test", List.of("\uFEFFname=테스트", "1.#"));

        assertEquals("테스트", map.name());
        assertEquals(3, map.columns());
        assertEquals(1, MapLoader.parse("test", List.of("\uFEFF1")).columns());
    }

    @Test
    void 스폰_층에_적으면_타일은_그대로_두고_스폰_지점만_정한다() {
        TileMap map = MapLoader.parse("test", List.of(
                "._|",
                "T.=",
                "[spawn]",
                ".2.",
                "1.3"));

        assertEquals(Tile.ROAD, map.tileAt(1, 0));
        assertEquals(Tile.ROAD_LANE, map.tileAt(2, 0));
        assertEquals(Tile.BUSH, map.tileAt(0, 1));
        assertEquals(Tile.CROSSWALK, map.tileAt(2, 1));
        assertEquals(List.of(new Point(0, 1), new Point(1, 0), new Point(2, 1)), map.spawnPoints());
    }

    @Test
    void 빌리지_맵의_길_위_스폰은_길_타일을_그대로_둔다() {
        TileMap map = MapLoader.loadResource("village/village10");

        assertEquals(new Point(7, 0), map.spawnPoints().get(4));
        assertEquals(Tile.ROAD_LANE, map.tileAt(7, 0));
        assertEquals(new Point(6, 12), map.spawnPoints().get(2));
        assertEquals(Tile.ROAD, map.tileAt(6, 12));
    }

    @Test
    void 스폰_층_크기가_타일_층과_다르면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("..", "..", "[spawn]", "1.")));
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("..", "..", "[spawn]", "1.", "...")));
    }

    @Test
    void 스폰_층에는_숫자와_점만_쓸_수_있다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("..", "[spawn]", "1B")));
    }

    @Test
    void 지나갈_수_없는_칸에는_스폰을_둘_수_없다() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of(".B", "[spawn]", ".1")));
        assertTrue(e.getMessage().contains("(1, 0)"), e.getMessage());
    }

    @Test
    void 타일_층_숫자와_스폰_층을_함께_쓰면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1.", "[spawn]", ".2")));
    }

    @Test
    void 스폰_층에서도_번호가_겹치거나_빠지면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("...", "[spawn]", "1.1")));
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("...", "[spawn]", "1.3")));
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("...", "[spawn]", "...")));
    }

    @Test
    void 같은_스폰_번호가_두_번_나오면_실패한다() {
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1.1")));
        assertThrows(IllegalArgumentException.class,
                () -> MapLoader.parse("test", List.of("1.2", "..2")));
    }

    private static IllegalArgumentException failure(String... lines) {
        return assertThrows(IllegalArgumentException.class, () -> MapLoader.parse("test", List.of(lines)));
    }

    private static void assertMentions(IllegalArgumentException e, String... parts) {
        for (String part : parts) {
            assertTrue(e.getMessage().contains(part), e.getMessage());
        }
    }

    @Test
    void 층_형식은_블럭_밑_바닥을_바닥_층에서_읽는다() {
        TileMap map = MapLoader.parse("test", List.of(
                "name=층 맵",
                "[floor]",
                "._|=",
                "..!.",
                "[block]",
                ".PBT",
                "..#.",
                "[spawn]",
                "1...",
                "...2"));

        assertEquals("층 맵", map.name());
        assertEquals(Tile.FLOOR, map.tileAt(0, 0));
        assertEquals(Tile.PUSH_BOX, map.tileAt(1, 0));
        assertEquals(Tile.ROAD, map.groundAt(1, 0));
        assertEquals(Tile.BLOCK, map.tileAt(2, 0));
        assertEquals(Tile.ROAD_LANE, map.groundAt(2, 0));
        assertEquals(Tile.BUSH, map.tileAt(3, 0));
        assertEquals(Tile.CROSSWALK, map.groundAt(3, 0));
        assertEquals(Tile.STATUE, map.tileAt(2, 1));
        assertEquals(Tile.SPIKE, map.groundAt(2, 1));
        assertEquals(Tile.FLOOR, map.groundAt(3, 1));
        assertEquals(List.of(new Point(0, 0), new Point(3, 1)), map.spawnPoints());
    }

    @Test
    void 층_순서는_상관없다() {
        TileMap map = MapLoader.parse("test", List.of("[spawn]", "1.", "[block]", ".B", "[floor]", "._"));

        assertEquals(Tile.BLOCK, map.tileAt(1, 0));
        assertEquals(Tile.ROAD, map.groundAt(1, 0));
    }

    @Test
    void 바닥_층에_블럭이나_스폰_글자를_적으면_어느_층에_적을지_알려_준다() {
        assertMentions(failure("[floor]", ".B", "[block]", "..", "[spawn]", "1."),
                "[floor] 층 (1, 0)", "'B'", "[block] 층에 적으세요");
        assertMentions(failure("[floor]", "1.", "[block]", "..", "[spawn]", "1."),
                "[floor] 층 (0, 0)", "[spawn] 층에 적으세요");
        assertMentions(failure("[floor]", ".?", "[block]", "..", "[spawn]", "1."),
                "[floor] 층 (1, 0)", "알 수 없는 글자");
    }

    @Test
    void 블럭_층에_바닥이나_스폰_글자를_적으면_어느_층에_적을지_알려_준다() {
        assertMentions(failure("[floor]", "..", "[block]", "._", "[spawn]", "1."),
                "[block] 층 (1, 0)", "'_'", "[floor] 층에 적고");
        assertMentions(failure("[floor]", "..", "[block]", ".1", "[spawn]", "1."),
                "[block] 층 (1, 0)", "[spawn] 층에 적으세요");
    }

    @Test
    void 층마다_크기가_바닥_층과_다르면_어느_층인지_알려_준다() {
        assertMentions(failure("[floor]", "..", "..", "[block]", "..", "[spawn]", "1.", ".."),
                "[block] 층이 1줄", "2줄");
        assertMentions(failure("[floor]", "..", "[block]", "...", "[spawn]", "1."),
                "[block] 층 1번째 줄 길이가 3");
        assertMentions(failure("[floor]", "..", "[block]", "..", "[spawn]", "1.", ".."),
                "[spawn] 층이 2줄", "[floor] 층과 같은 1줄");
        assertMentions(failure("[floor]", "..", ".", "[block]", "..", "..", "[spawn]", "1.", ".."),
                "[floor] 층 2번째 줄 길이가 1");
    }

    @Test
    void 층_형식에서_층이_하나라도_빠지면_실패한다() {
        assertMentions(failure("[floor]", "..", "[spawn]", "1."), "[block] 층이 없습니다");
        assertMentions(failure("[block]", "..", "[spawn]", "1."), "[floor] 층이 없습니다");
        assertMentions(failure("[floor]", "..", "[block]", ".."), "[spawn] 층이 없습니다");
    }

    @Test
    void 층_이름_없는_줄과_바닥_블럭_층을_섞어_쓰면_실패한다() {
        assertMentions(failure("1.", "[floor]", "..", "[block]", "..", "[spawn]", ".1"),
                "층 이름 없이 적은 줄 '1.'");
    }

    @Test
    void 모르는_층_이름이나_두_번_나온_층은_실패한다() {
        assertMentions(failure("[floors]", "..", "[block]", "..", "[spawn]", "1."), "[floors]");
        assertMentions(failure("[floor]", "..", "[floor]", "..", "[block]", "..", "[spawn]", "1."),
                "[floor] 줄이 두 번");
    }

    @Test
    void 층_형식에서도_스폰은_지나갈_수_없는_블럭_위에_둘_수_없고_덤불_위에는_둘_수_있다() {
        assertMentions(failure("[floor]", "._", "[block]", ".B", "[spawn]", "21"), "(1, 0)", "'B'");

        TileMap map = MapLoader.parse("test", List.of("[floor]", "._", "[block]", ".T", "[spawn]", ".1"));

        assertEquals(List.of(new Point(1, 0)), map.spawnPoints());
        assertEquals(Tile.ROAD, map.groundAt(1, 0));
    }

    @Test
    void 층_형식의_블럭_층에서도_미사일은_두_칸씩_붙여_쓴다() {
        failure("[floor]", "...", "[block]", ".M.", "[spawn]", "1..");

        TileMap map = MapLoader.parse("test", List.of("[floor]", "...", "[block]", ".MM", "[spawn]", "1.."));

        assertTrue(map.isTileOrigin(1, 0));
        assertFalse(map.isTileOrigin(2, 0));
    }
}

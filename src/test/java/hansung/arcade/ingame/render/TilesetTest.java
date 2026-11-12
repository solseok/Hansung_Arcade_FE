package hansung.arcade.ingame.render;

import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 그림 고르는 규칙은 테스트 전용 합성 그림(src/test/resources/image/fixture*)으로 확인한다.
 * 실제 테마 그림은 바뀔 수 있으므로 읽히는지와 위치만 확인한다.
 *
 * <p>fixture: 01_block_normal 3장 [빨강, 초록, 파랑], 03_block_push 2장, 04_block_multi 2장,
 * 06_block_oneway 2장(왼, 오), 11_house 3장 [빨강, 노랑, 파랑], 12_missile 2칸짜리 1장. 바닥·덤불 등은 없다.
 * fixture-floor: 바닥만 있다. tile_01_grass [어두운 초록, 밝은 초록, 흰색], tile_02_road 회색 1장.
 * fixture-floor-single: tile_01_plain 회색 1장.</p>
 */
class TilesetTest {

    private static final int T = TileMap.TILE_SIZE;
    private static final Tileset FIXTURE = Tileset.forTheme("fixture");

    @Test
    void 테마_폴더에_없는_파일의_타일은_그림이_없다() {
        assertNotNull(FIXTURE.sprite(Tile.BLOCK, 0, 0));
        assertNull(FIXTURE.sprite(Tile.BUSH, 0, 0));
        assertNull(FIXTURE.sprite(Tile.FLOOR, 0, 0));
    }

    @Test
    void 그림이_빠진_타일을_맵에서_찾아낸다() {
        TileMap map = MapLoader.parse("fixture/test", List.of("1BPT#W<^"));

        assertEquals(Set.of(Tile.BUSH, Tile.STATUE, Tile.WIRE, Tile.ONE_WAY_UP), FIXTURE.missingArt(map));
    }

    @Test
    void 여러_번_맞는_블록은_남은_횟수가_적을수록_뒤_프레임이고_모자라면_마지막_프레임() {
        TileSprite three = FIXTURE.sprite(Tile.HARD_BLOCK_3, 0, 0);
        TileSprite two = FIXTURE.sprite(Tile.HARD_BLOCK_2, 0, 0);
        TileSprite one = FIXTURE.sprite(Tile.HARD_BLOCK_1, 0, 0);

        assertNotSame(three, two);
        assertSame(two, one);
    }

    @Test
    void 한_방향_블록은_왼쪽_오른쪽_위_아래_순서이고_없는_방향은_그림이_없다() {
        assertNotSame(FIXTURE.sprite(Tile.ONE_WAY_LEFT, 0, 0), FIXTURE.sprite(Tile.ONE_WAY_RIGHT, 0, 0));
        assertNull(FIXTURE.sprite(Tile.ONE_WAY_UP, 0, 0));
        assertNull(FIXTURE.sprite(Tile.ONE_WAY_DOWN, 0, 0));
    }

    @Test
    void 변형은_칸마다_고정되고_고르게_섞인다() {
        int[] counts = new int[3];
        for (int row = 0; row < 13; row++) {
            for (int col = 0; col < 15; col++) {
                TileSprite sprite = FIXTURE.sprite(Tile.PUSH_BOX, col, row);
                assertSame(sprite, FIXTURE.sprite(Tile.PUSH_BOX, col, row), "같은 칸은 늘 같은 변형");
                counts[Tileset.variantIndex(col, row, 3)]++;
            }
        }
        for (int count : counts) {
            assertTrue(count > 195 / 6, "변형이 한쪽으로 쏠림: " + List.of(counts[0], counts[1], counts[2]));
        }
    }

    @Test
    void 변형이_두_장이어도_체크무늬가_되지_않는다() {
        int origin = Tileset.variantIndex(0, 0, 2);
        boolean checkerboard = true;
        for (int row = 0; row < 13; row++) {
            for (int col = 0; col < 15; col++) {
                boolean sameAsOrigin = Tileset.variantIndex(col, row, 2) == origin;
                boolean evenCell = (col + row) % 2 == 0;
                if (sameAsOrigin != evenCell) {
                    checkerboard = false;
                }
            }
        }
        assertFalse(checkerboard);
    }

    @Test
    void 블록과_집은_기호마다_정해진_장을_쓴다() {
        assertEquals(0xC82828, colorOf(FIXTURE.sprite(Tile.BLOCK, 5, 7)), "B = 01_block_normal 0번째");
        assertEquals(0x28A028, colorOf(FIXTURE.sprite(Tile.BLOCK_2, 5, 7)), "O = 01_block_normal 1번째");
        assertEquals(0xC82828, colorOf(FIXTURE.sprite(Tile.HOUSE_1, 5, 7)), "H = 11_house 0번째");
        assertEquals(0xC8C828, colorOf(FIXTURE.sprite(Tile.HOUSE_2, 5, 7)), "J = 11_house 1번째");
        assertEquals(0x2828C8, colorOf(FIXTURE.sprite(Tile.HOUSE_3, 5, 7)), "K = 11_house 2번째");
        for (int col = 0; col < 15; col++) {
            assertSame(FIXTURE.sprite(Tile.BLOCK, 0, 0), FIXTURE.sprite(Tile.BLOCK, col, col % 13), "칸마다 섞지 않음");
        }
    }

    @Test
    void 정해진_장이_파일에_없으면_그림이_없다() {
        Tileset floorOnly = Tileset.forTheme("fixture-floor");

        assertNull(floorOnly.sprite(Tile.BLOCK_2, 0, 0));
        assertNull(floorOnly.sprite(Tile.HOUSE_3, 0, 0));
    }

    private static int colorOf(TileSprite sprite) {
        return sprite.image().getRGB(20, 30) & 0xFFFFFF;
    }

    @Test
    void 두_칸짜리_타일은_80px_프레임을_두_칸_가운데에_세운다() {
        TileSprite missile = FIXTURE.sprite(Tile.MISSILE, 0, 0);

        assertEquals(2 * T, missile.image().getWidth());
        assertEquals(0, missile.offsetX());
        assertEquals(T - 68, missile.offsetY());
    }

    @Test
    void 바닥은_체스판처럼_깔고_0_0_칸이_밝은_장이다() {
        Tileset floorOnly = Tileset.forTheme("fixture-floor");
        int light = 0x3CB43C;   // tile_01_grass 둘째 장 (파일 안에서는 어두운 장이 먼저 온다)
        int dark = 0x28A028;    // tile_01_grass 첫째 장

        for (int row = 0; row < 13; row++) {
            for (int col = 0; col < 15; col++) {
                int expected = (col + row) % 2 == 0 ? light : dark;
                assertEquals(expected, floorOnly.floor(col, row).getRGB(20, 20) & 0xFFFFFF, "(" + col + ", " + row + ")");
            }
        }
    }

    @Test
    void 바닥_그림이_한_장이면_모든_칸에_깔고_파일이_없으면_null() {
        Tileset single = Tileset.forTheme("fixture-floor-single");

        assertSame(single.floor(0, 0), single.floor(1, 0));
        assertFalse(FIXTURE.hasFloor());
        assertNull(FIXTURE.floor(0, 0));
    }

    @Test
    void 길_기호는_tile_02_파일의_장을_순서대로_쓰고_없는_장은_그림이_없다() {
        Tileset floorOnly = Tileset.forTheme("fixture-floor");   // tile_02_road 는 회색 1장뿐
        TileSprite road = floorOnly.sprite(Tile.ROAD, 3, 4);

        assertEquals(0x787878, road.image().getRGB(20, 20) & 0xFFFFFF);
        assertEquals(0, road.offsetX());
        assertEquals(0, road.offsetY());
        assertNull(floorOnly.sprite(Tile.ROAD_LANE, 0, 0));
        assertNull(floorOnly.sprite(Tile.CROSSWALK, 0, 0));
        assertNull(FIXTURE.sprite(Tile.ROAD, 0, 0), "바닥 파일이 없는 테마");
    }

    @Test
    void 리소스_폴더의_파일_이름을_나열한다() {
        assertEquals(List.of("tile_01_grass.bmp", "tile_02_road.bmp"), ResourceFiles.list("/image/fixture-floor/"));
        assertEquals(List.of(), ResourceFiles.list("/image/없는폴더/"));
    }

    @Test
    void 마젠타_배경은_투명하게_읽는다() {
        BufferedImage block = FIXTURE.sprite(Tile.BLOCK, 0, 0).image();

        assertEquals(0, block.getRGB(0, 0) >>> 24, "왼쪽 위 마젠타는 투명");
        assertEquals(0xFF, block.getRGB(20, 30) >>> 24, "나머지는 불투명");
    }

    @Test
    void 살짝_틀어진_마젠타도_배경으로_본다() {
        assertTrue(SpriteSheet.isColorKey(0xFF00FF));
        assertTrue(SpriteSheet.isColorKey(0xFF03FF));
        assertTrue(SpriteSheet.isColorKey(0xFE01FE));
        assertFalse(SpriteSheet.isColorKey(0xE84A5F), "빨강 계열은 배경 아님");
        assertFalse(SpriteSheet.isColorKey(0x8E5BD9), "보라 계열은 배경 아님");
    }

    @Test
    void 그림이_하나도_없는_테마는_실패한다() {
        assertThrows(IllegalArgumentException.class, () -> Tileset.forTheme("없는테마"));
        assertThrows(IllegalArgumentException.class, () -> Tileset.forTheme(""));
    }

    @Test
    void 프레임_폭의_배수가_아닌_그림은_실패하고_없는_파일은_빈_값() {
        SpriteSheet block = SpriteSheet.find("/image/fixture/01_block_normal.bmp").orElseThrow();

        assertEquals(3, block.frames(T).size());
        assertThrows(IllegalArgumentException.class, () -> block.frames(T * 2));
        assertTrue(SpriteSheet.find("/image/fixture/없음.bmp").isEmpty());
    }

    @Test
    void 실제_테마_그림은_모두_읽히고_칸_바닥_가운데에_선다() {
        for (String theme : List.of("camp", "patrit", "village")) {
            Tileset tileset = assertDoesNotThrow(() -> Tileset.forTheme(theme), theme);
            for (Tile tile : Tile.values()) {
                TileSprite sprite = tileset.sprite(tile, 0, 0);
                if (sprite != null) {
                    assertEquals(T, sprite.offsetY() + sprite.image().getHeight(), theme + " " + tile + " 바닥 위치");
                    assertEquals(tile.width() * T, sprite.offsetX() * 2 + sprite.image().getWidth(),
                            theme + " " + tile + " 가로 위치");
                }
            }
        }
    }

    @Test
    void 기본_맵은_캠프_테마로_그릴_수_있다() {
        TileMap map = MapLoader.loadResource("camp/camp08");
        Tileset camp = Tileset.forTheme(map.theme());

        assertNotNull(camp.sprite(Tile.BLOCK, 0, 0));
        assertDoesNotThrow(() -> camp.missingArt(map));
    }
}

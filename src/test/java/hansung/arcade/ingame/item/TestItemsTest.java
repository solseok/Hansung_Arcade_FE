package hansung.arcade.ingame.item;

import hansung.arcade.ingame.item.FieldItems.FieldItem;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestItemsTest {

    @Test
    void 열한_종을_한_개씩_1P_스폰에서_가까운_빈_칸부터_놓는다() {
        TileMap map = MapLoader.parse("fixture/test-items", List.of(
                "......",
                ".1.B..",
                "..T...",
                ".....2"));
        FieldItems items = new FieldItems();

        List<FieldItem> placed = TestItems.scatter(map, items);

        assertEquals(11, placed.size());
        Set<ItemType> types = placed.stream().map(FieldItem::type).collect(Collectors.toSet());
        assertEquals(EnumSet.allOf(ItemType.class), types);
        // 가장 가까운 칸은 바로 위 칸 (거리 1, 위 줄 먼저)
        assertEquals(new FieldItem(1, 0, ItemType.GLOVE), placed.get(0));
        for (FieldItem item : placed) {
            assertTrue(map.tileAt(item.col(), item.row()).isGround(), item.toString());
            assertTrue(Math.abs(item.col() - 1) + Math.abs(item.row() - 1) <= 4, item.toString());
            assertTrue(!(item.col() == 5 && item.row() == 3), "2P 스폰에는 놓지 않는다");
        }
    }

    @Test
    void 빈_칸이_모자라면_들어가는_만큼만_놓는다() {
        TileMap map = MapLoader.parse("fixture/test-items", List.of("1..B"));
        FieldItems items = new FieldItems();

        List<FieldItem> placed = TestItems.scatter(map, items);

        assertEquals(List.of(ItemType.GLOVE, ItemType.SHOE), placed.stream().map(FieldItem::type).toList());
    }
}

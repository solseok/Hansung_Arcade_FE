package hansung.arcade.ingame.item;

import hansung.arcade.ingame.item.FieldItems.FieldItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldItemsTest {

    private final FieldItems items = new FieldItems();

    @Test
    void 놓은_아이템은_그_칸에서_찾는다() {
        items.place(2, 3, ItemType.FLUID);

        assertEquals(Optional.of(new FieldItem(2, 3, ItemType.FLUID)), items.at(2, 3));
        assertEquals(Optional.empty(), items.at(3, 2));
    }

    @Test
    void 아이템이_있는_칸에_놓으면_새_아이템으로_바뀐다() {
        items.place(1, 1, ItemType.BUBBLE);
        items.place(1, 1, ItemType.SHOE);

        assertEquals(List.of(new FieldItem(1, 1, ItemType.SHOE)), items.all());
    }

    @Test
    void 집은_아이템은_맵에서_사라진다() {
        items.place(1, 1, ItemType.BUBBLE);

        assertEquals(Optional.of(ItemType.BUBBLE), items.take(1, 1));
        assertEquals(Optional.empty(), items.take(1, 1));
        assertTrue(items.all().isEmpty());
    }

    @Test
    void 조건에_맞는_칸의_아이템만_없앤다() {
        items.place(0, 0, ItemType.BUBBLE);
        items.place(1, 0, ItemType.FLUID);
        items.place(2, 0, ItemType.ROLLER_SHOE);

        items.removeWhere((col, row) -> col >= 1);

        assertEquals(List.of(new FieldItem(0, 0, ItemType.BUBBLE)), items.all());
        assertFalse(items.remove(2, 0));
        assertTrue(items.remove(0, 0));
    }
}

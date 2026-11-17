package hansung.arcade.ingame.item;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ItemTypeTest {

    @Test
    void 패시브_8종과_액티브_3종이다() {
        List<ItemType> active = Arrays.stream(ItemType.values()).filter(ItemType::isActive).toList();

        assertEquals(11, ItemType.values().length);
        assertEquals(List.of(ItemType.NEEDLE, ItemType.SHIELD, ItemType.SPRING_SHOE), active);
    }

    @Test
    void 그림_경로는_종류별_폴더와_id로_정해진다() {
        assertEquals("/item/passive/roller_shoe.bmp", ItemType.ROLLER_SHOE.imagePath());
        assertEquals("/item/active/spring_shoe.bmp", ItemType.SPRING_SHOE.imagePath());
    }

    @Test
    void 모든_아이템의_그림_파일이_있다() {
        for (ItemType item : ItemType.values()) {
            assertNotNull(ItemType.class.getResource(item.imagePath()), item.imagePath());
        }
    }
}

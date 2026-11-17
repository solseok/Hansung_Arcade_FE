package hansung.arcade.ingame.item;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDropsTest {

    private static Optional<ItemType> roll(double... values) {
        return new ItemDrops(ScriptedRandom.of(values)).rollForBrokenBlock();
    }

    @Test
    void 첫_난수가_33퍼센트_안이면_아이템이_나온다() {
        assertTrue(roll(0.0, 0.0).isPresent());
        assertTrue(roll(0.329, 0.0).isPresent());
        assertEquals(Optional.empty(), roll(0.33));
        assertEquals(Optional.empty(), roll(0.99));
    }

    @Test
    void 나온_아이템은_물풍선_40_물줄기_40_롤러_신발_20으로_나뉜다() {
        assertEquals(Optional.of(ItemType.BUBBLE), roll(0.0, 0.0));
        assertEquals(Optional.of(ItemType.BUBBLE), roll(0.0, 0.399));
        assertEquals(Optional.of(ItemType.FLUID), roll(0.0, 0.4));
        assertEquals(Optional.of(ItemType.FLUID), roll(0.0, 0.799));
        assertEquals(Optional.of(ItemType.ROLLER_SHOE), roll(0.0, 0.8));
        assertEquals(Optional.of(ItemType.ROLLER_SHOE), roll(0.0, 0.999));
    }

    @Test
    void 많이_굴리면_확률대로_나온다() {
        ItemDrops drops = new ItemDrops(new SplittableRandom(20261115));
        int rolls = 200_000;
        Map<ItemType, Integer> counts = new EnumMap<>(ItemType.class);
        int dropped = 0;
        for (int i = 0; i < rolls; i++) {
            Optional<ItemType> item = drops.rollForBrokenBlock();
            if (item.isPresent()) {
                dropped++;
                counts.merge(item.get(), 1, Integer::sum);
            }
        }

        assertEquals(0.33, dropped / (double) rolls, 0.005);
        assertEquals(0.4, counts.get(ItemType.BUBBLE) / (double) dropped, 0.01);
        assertEquals(0.4, counts.get(ItemType.FLUID) / (double) dropped, 0.01);
        assertEquals(0.2, counts.get(ItemType.ROLLER_SHOE) / (double) dropped, 0.01);
        assertEquals(3, counts.size());
    }
}

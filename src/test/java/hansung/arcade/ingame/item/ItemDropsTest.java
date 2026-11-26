package hansung.arcade.ingame.item;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void 블럭에서는_탱크를_뺀_10종이_합_97의_비율로_나온다() {
        assertEquals(EnumSet.complementOf(EnumSet.of(ItemType.TANK)), ItemDrops.WEIGHTS.keySet());
        assertEquals(97, ItemDrops.WEIGHTS.values().stream().mapToInt(Integer::intValue).sum());
        for (ItemType item : ItemDrops.WEIGHTS.keySet()) {
            assertEquals(Optional.of(item), roll(0.0, ScriptedRandom.pickFor(item)), item.id());
        }
    }

    @Test
    void 어떤_난수로도_블럭에서_탱크가_무작위로_나오지_않는다() {
        for (int i = 0; i < 10_000; i++) {
            double pick = i / 10_000.0;
            assertNotEquals(Optional.of(ItemType.TANK), roll(0.0, pick), "난수 " + pick);
        }
    }

    @Test
    void 능력치_아이템이_가장_흔하고_판을_바꾸는_아이템은_드물다() {
        Map<ItemType, Integer> weights = ItemDrops.WEIGHTS;
        assertEquals(22, weights.get(ItemType.BUBBLE));
        assertEquals(22, weights.get(ItemType.FLUID));
        assertEquals(16, weights.get(ItemType.ROLLER_SHOE));
        assertEquals(60, weights.get(ItemType.BUBBLE) + weights.get(ItemType.FLUID) + weights.get(ItemType.ROLLER_SHOE));
        for (ItemType rare : List.of(ItemType.ULTRA, ItemType.RED_DEVIL)) {
            assertTrue(weights.get(rare) < weights.get(ItemType.NEEDLE), rare.id());
        }
    }

    @Test
    void 난수_구간의_양_끝도_빠짐없이_한_아이템이다() {
        assertEquals(Optional.of(ItemType.BUBBLE), roll(0.0, 0.0));
        assertEquals(Optional.of(ItemType.BUBBLE), roll(0.0, 21.999 / 97));
        assertEquals(Optional.of(ItemType.FLUID), roll(0.0, 22.001 / 97));
        // 신발(72~78) 바로 뒤가 탱크 없이 바늘(78~85)이다
        assertEquals(Optional.of(ItemType.SHOE), roll(0.0, 77.999 / 97));
        assertEquals(Optional.of(ItemType.NEEDLE), roll(0.0, 78.001 / 97));
        assertEquals(Optional.of(ItemType.SPRING_SHOE), roll(0.0, 0.9999));
    }

    @Test
    void 많이_굴리면_확률대로_나온다() {
        ItemDrops drops = new ItemDrops(new SplittableRandom(20261115));
        int rolls = 400_000;
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
        for (ItemType item : ItemDrops.WEIGHTS.keySet()) {
            assertEquals(ItemDrops.WEIGHTS.get(item) / 97.0, counts.get(item) / (double) dropped, 0.006, item.id());
        }
        assertEquals(ItemDrops.WEIGHTS.keySet(), counts.keySet());
    }
}

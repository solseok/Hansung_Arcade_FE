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

    private static final String CAMP = "camp/camp08";
    private static final String PATRIT = "patrit/patrit14";
    private static final String VILLAGE = "village/village10";

    private static Optional<ItemType> roll(String mapId, double... values) {
        return new ItemDrops(ScriptedRandom.of(values), mapId).rollForBrokenBlock();
    }

    @Test
    void 첫_난수가_33퍼센트_안이면_아이템이_나온다() {
        for (String mapId : List.of(CAMP, PATRIT, VILLAGE)) {
            assertTrue(roll(mapId, 0.0, 0.0).isPresent(), mapId);
            assertTrue(roll(mapId, 0.329, 0.0).isPresent(), mapId);
            assertEquals(Optional.empty(), roll(mapId, 0.33), mapId);
            assertEquals(Optional.empty(), roll(mapId, 0.99), mapId);
        }
    }

    @Test
    void 캠프_08에서는_그림이_있는_아이템_11종이_모두_블럭에서_나온다() {
        assertEquals(EnumSet.allOf(ItemType.class), ItemDrops.WEIGHTS.keySet());
        assertEquals(100, ItemDrops.WEIGHTS.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(ItemDrops.WEIGHTS, ItemDrops.weightsFor(CAMP));
        for (ItemType item : ItemType.values()) {
            assertEquals(Optional.of(item), roll(CAMP, 0.0, ScriptedRandom.pickFor(item, CAMP)), item.id());
        }
    }

    @Test
    void 캠프_08이_아닌_맵에서는_탱크를_뺀_10종이_같은_비율로_나온다() {
        EnumSet<ItemType> withoutTank = EnumSet.complementOf(EnumSet.of(ItemType.TANK));
        for (String mapId : List.of(PATRIT, VILLAGE, "fixture/anything")) {
            Map<ItemType, Integer> weights = ItemDrops.weightsFor(mapId);
            assertEquals(withoutTank, weights.keySet(), mapId);
            assertEquals(97, weights.values().stream().mapToInt(Integer::intValue).sum(), mapId);
            for (ItemType item : withoutTank) {
                assertEquals(ItemDrops.WEIGHTS.get(item), weights.get(item), item.id());
                assertEquals(Optional.of(item), roll(mapId, 0.0, ScriptedRandom.pickFor(item, mapId)), item.id());
            }
        }
    }

    @Test
    void 캠프_08이_아닌_맵에서는_어떤_난수로도_탱크가_나오지_않는다() {
        for (int i = 0; i < 10_000; i++) {
            double pick = i / 10_000.0;
            assertNotEquals(Optional.of(ItemType.TANK), roll(PATRIT, 0.0, pick), "난수 " + pick);
        }
    }

    @Test
    void 캠프_08에서_탱크가_나올_난수도_다른_맵에서는_다른_아이템이다() {
        double tank = ScriptedRandom.pickFor(ItemType.TANK, CAMP);
        assertEquals(Optional.of(ItemType.TANK), roll(CAMP, 0.0, tank));

        // 탱크 구간(78~81)이 빠져 합 97로 나누므로, 같은 난수 0.795가 77.1이 되어 신발(72~78) 구간이다
        assertEquals(Optional.of(ItemType.SHOE), roll(VILLAGE, 0.0, tank));
    }

    @Test
    void 능력치_아이템이_가장_흔하고_판을_바꾸는_아이템은_드물다() {
        Map<ItemType, Integer> weights = ItemDrops.WEIGHTS;
        assertEquals(22, weights.get(ItemType.BUBBLE));
        assertEquals(22, weights.get(ItemType.FLUID));
        assertEquals(16, weights.get(ItemType.ROLLER_SHOE));
        assertEquals(60, weights.get(ItemType.BUBBLE) + weights.get(ItemType.FLUID) + weights.get(ItemType.ROLLER_SHOE));
        for (ItemType rare : List.of(ItemType.ULTRA, ItemType.RED_DEVIL, ItemType.TANK)) {
            assertTrue(weights.get(rare) < weights.get(ItemType.NEEDLE), rare.id());
        }
    }

    @Test
    void 난수_구간의_양_끝도_빠짐없이_한_아이템이다() {
        assertEquals(Optional.of(ItemType.BUBBLE), roll(CAMP, 0.0, 0.0));
        assertEquals(Optional.of(ItemType.BUBBLE), roll(CAMP, 0.0, 0.2199));
        assertEquals(Optional.of(ItemType.FLUID), roll(CAMP, 0.0, 0.22));
        assertEquals(Optional.of(ItemType.SPRING_SHOE), roll(CAMP, 0.0, 0.9999));

        assertEquals(Optional.of(ItemType.BUBBLE), roll(PATRIT, 0.0, 0.0));
        // 합 97 표에서는 신발(72~78) 바로 뒤가 탱크 없이 바늘(78~85)이다
        assertEquals(Optional.of(ItemType.SHOE), roll(PATRIT, 0.0, 77.999 / 97));
        assertEquals(Optional.of(ItemType.NEEDLE), roll(PATRIT, 0.0, 78.001 / 97));
        assertEquals(Optional.of(ItemType.SPRING_SHOE), roll(PATRIT, 0.0, 0.9999));
    }

    @Test
    void 많이_굴리면_맵마다_확률대로_나온다() {
        for (String mapId : List.of(CAMP, PATRIT)) {
            ItemDrops drops = new ItemDrops(new SplittableRandom(20261115), mapId);
            Map<ItemType, Integer> weights = ItemDrops.weightsFor(mapId);
            double total = weights.values().stream().mapToInt(Integer::intValue).sum();
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

            assertEquals(0.33, dropped / (double) rolls, 0.005, mapId);
            for (ItemType item : weights.keySet()) {
                assertEquals(weights.get(item) / total, counts.get(item) / (double) dropped, 0.006, item.id());
            }
            assertEquals(weights.keySet(), counts.keySet(), mapId);
        }
    }
}

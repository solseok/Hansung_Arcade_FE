package hansung.arcade.ingame.item;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * 블럭이 부서질 때 아이템이 나오는지와 어떤 아이템인지 정한다.
 * {@link #DROP_CHANCE} 확률로 나오고, 나오면 그림이 있는 아이템 11종({@link ItemType}) 가운데 {@link #WEIGHTS} 비율로 하나다.
 *
 * <p>능력치를 한 단계씩 올리는 물풍선·물줄기·롤러 신발이 가장 흔하고(합쳐 60%), 판을 크게 바꾸는 아이템일수록 드물다.</p>
 * <table>
 *   <caption>아이템이 나왔을 때 종류별 비율</caption>
 *   <tr><th>아이템</th><th>%</th></tr>
 *   <tr><td>물풍선 (bubble)</td><td>22</td></tr>
 *   <tr><td>물줄기 (fluid)</td><td>22</td></tr>
 *   <tr><td>롤러 신발 (roller_shoe)</td><td>16</td></tr>
 *   <tr><td>바늘 (needle)</td><td>7</td></tr>
 *   <tr><td>신발 (shoe)</td><td>6</td></tr>
 *   <tr><td>보호막 (shield)</td><td>6</td></tr>
 *   <tr><td>스프링 신발 (spring_shoe)</td><td>6</td></tr>
 *   <tr><td>장갑 (glove)</td><td>5</td></tr>
 *   <tr><td>울트라 (ultra)</td><td>4</td></tr>
 *   <tr><td>빨간 악마 (red_devil)</td><td>3</td></tr>
 *   <tr><td>탱크 (tank)</td><td>3</td></tr>
 * </table>
 *
 * <p>난수는 밖에서 받아서, 테스트는 정해진 값을 넣어 결과를 고정할 수 있다.</p>
 */
public final class ItemDrops {

    /** 블럭이 부서졌을 때 아이템이 나올 확률 */
    public static final double DROP_CHANCE = 0.33;
    /** 아이템이 나왔을 때 종류별 비율(%). 합이 100이고, 이 순서대로 난수 구간을 나눈다 */
    public static final Map<ItemType, Integer> WEIGHTS = weights();

    private final RandomGenerator random;

    public ItemDrops(RandomGenerator random) {
        this.random = Objects.requireNonNull(random);
    }

    /**
     * 블럭 하나가 다 부서졌을 때 나올 아이템. 난수를 두 번까지 쓴다 (나오는지, 무엇인지).
     * 두 번째 난수 [0, 1)을 {@link #WEIGHTS} 순서대로 비율만큼 나눈 구간에서 고른다.
     *
     * @return 나온 아이템. 안 나오면 비어 있다
     */
    public Optional<ItemType> rollForBrokenBlock() {
        if (random.nextDouble() >= DROP_CHANCE) {
            return Optional.empty();
        }
        double pick = random.nextDouble() * 100;
        int bound = 0;
        ItemType last = null;
        for (Map.Entry<ItemType, Integer> entry : WEIGHTS.entrySet()) {
            bound += entry.getValue();
            last = entry.getKey();
            if (pick < bound) {
                return Optional.of(last);
            }
        }
        return Optional.ofNullable(last);
    }

    private static Map<ItemType, Integer> weights() {
        Map<ItemType, Integer> weights = new EnumMap<>(ItemType.class);
        weights.put(ItemType.BUBBLE, 22);
        weights.put(ItemType.FLUID, 22);
        weights.put(ItemType.ULTRA, 4);
        weights.put(ItemType.ROLLER_SHOE, 16);
        weights.put(ItemType.RED_DEVIL, 3);
        weights.put(ItemType.GLOVE, 5);
        weights.put(ItemType.SHOE, 6);
        weights.put(ItemType.TANK, 3);
        weights.put(ItemType.NEEDLE, 7);
        weights.put(ItemType.SHIELD, 6);
        weights.put(ItemType.SPRING_SHOE, 6);
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total != 100 || weights.size() != ItemType.values().length) {
            throw new IllegalStateException("아이템 비율은 11종 모두 합쳐 100이어야 합니다: " + weights);
        }
        return Collections.unmodifiableMap(weights);
    }
}

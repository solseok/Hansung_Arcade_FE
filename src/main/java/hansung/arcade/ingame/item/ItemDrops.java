package hansung.arcade.ingame.item;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * 블럭이 부서질 때 아이템이 나오는지와 어떤 아이템인지 정한다.
 * {@link #DROP_CHANCE} 확률로 나오고, 나오면 탱크를 뺀 아이템 10종 가운데 {@link #WEIGHTS} 비율로 하나다.
 *
 * <p>능력치를 한 단계씩 올리는 물풍선·물줄기·롤러 신발이 가장 흔하고(합쳐 60/97), 판을 크게 바꾸는 아이템일수록 드물다.</p>
 * <table>
 *   <caption>아이템이 나왔을 때 종류별 비율 (합 97)</caption>
 *   <tr><th>아이템</th><th>비율</th></tr>
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
 * </table>
 *
 * <p>탱크는 무작위로 나오지 않는다. 내구도가 있는 상자(캠프 08의 단단한 블럭)에 맵이 정해 둔 고정 드롭으로만 나오고,
 * 그 판정은 서버가 해서 부서진 블럭의 아이템으로 알려 준다(클라이언트 맵 파일에는 고정 드롭이 없어, 서버 없이 도는 경기에는
 * 탱크가 나오지 않는다). 예전에 탱크 몫이던 3을 빼고 합 97 안에서 고르며, 서버도 같은 표로 고른다.</p>
 *
 * <p>난수는 밖에서 받아서, 테스트는 정해진 값을 넣어 결과를 고정할 수 있다.</p>
 */
public final class ItemDrops {

    /** 블럭이 부서졌을 때 아이템이 나올 확률 */
    public static final double DROP_CHANCE = 0.33;
    /** {@link #WEIGHTS}의 합. "무엇인지" 난수 [0, 1)에 곱해 구간을 고른다. {@link #weights()}가 읽으므로 그보다 먼저 정한다 */
    private static final int TOTAL = 97;
    /** 아이템이 나왔을 때 종류별 비율. 탱크를 뺀 10종이고 합이 97이며, 이 순서대로 난수 구간을 나눈다 */
    public static final Map<ItemType, Integer> WEIGHTS = weights();

    private final RandomGenerator random;

    public ItemDrops(RandomGenerator random) {
        this.random = Objects.requireNonNull(random);
    }

    /**
     * 블럭 하나가 다 부서졌을 때 나올 아이템. 난수를 두 번까지 쓴다 (나오는지, 무엇인지).
     * 두 번째 난수 [0, 1)에 비율의 합(97)을 곱해, 비율 순서대로 나눈 구간에서 고른다.
     *
     * @return 나온 아이템. 안 나오면 비어 있다
     */
    public Optional<ItemType> rollForBrokenBlock() {
        if (random.nextDouble() >= DROP_CHANCE) {
            return Optional.empty();
        }
        double pick = random.nextDouble() * TOTAL;
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
        weights.put(ItemType.NEEDLE, 7);
        weights.put(ItemType.SHIELD, 6);
        weights.put(ItemType.SPRING_SHOE, 6);
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total != TOTAL || weights.containsKey(ItemType.TANK) || weights.size() != ItemType.values().length - 1) {
            throw new IllegalStateException("아이템 비율은 탱크를 뺀 10종을 합쳐 97이어야 합니다: " + weights);
        }
        return Collections.unmodifiableMap(weights);
    }
}

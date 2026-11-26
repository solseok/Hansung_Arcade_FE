package hansung.arcade.ingame.item;

import java.util.random.RandomGenerator;

/**
 * 정해 둔 값을 차례로 돌려주는 난수. 끝까지 쓰면 처음부터 되풀이한다. 아이템 드롭 결과를 고정할 때 쓴다.
 * {@link ItemDrops}는 블럭 하나마다 "나오는지"와 "무엇인지"에 난수를 하나씩 쓴다.
 */
public final class ScriptedRandom implements RandomGenerator {

    /** 나온다고 정하는 값 ({@link ItemDrops#DROP_CHANCE}보다 작다) */
    public static final double DROPS = 0.0;
    /** 안 나온다고 정하는 값 */
    public static final double NO_DROP = 0.9;

    private final double[] values;
    private int next;

    private ScriptedRandom(double... values) {
        this.values = values.clone();
    }

    /** values를 차례로 되풀이해 돌려준다 */
    public static ScriptedRandom of(double... values) {
        return new ScriptedRandom(values);
    }

    /** 블럭이 부서질 때마다 아이템이 나오지 않는다 */
    public static ScriptedRandom noDrops() {
        return of(NO_DROP);
    }

    /** 블럭이 부서질 때마다 item이 나온다. "무엇인지" 난수는 {@link ItemDrops#WEIGHTS}에서 item 구간의 가운데 값이다 */
    public static ScriptedRandom alwaysDrops(ItemType item) {
        return of(DROPS, pickFor(item));
    }

    /** {@link ItemDrops}가 item을 고르는 "무엇인지" 난수 [0, 1): 비율의 합(97)으로 나눈 item 구간의 가운데 값 */
    public static double pickFor(ItemType item) {
        int total = ItemDrops.WEIGHTS.values().stream().mapToInt(Integer::intValue).sum();
        int bound = 0;
        for (var entry : ItemDrops.WEIGHTS.entrySet()) {
            if (entry.getKey() == item) {
                return (bound + entry.getValue() / 2.0) / total;
            }
            bound += entry.getValue();
        }
        throw new IllegalArgumentException("블럭에서 무작위로 나오지 않는 아이템입니다: " + item);
    }

    @Override
    public double nextDouble() {
        double value = values[next];
        next = (next + 1) % values.length;
        return value;
    }

    @Override
    public long nextLong() {
        throw new UnsupportedOperationException("아이템 드롭은 nextDouble만 씁니다.");
    }
}

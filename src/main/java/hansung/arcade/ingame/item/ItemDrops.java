package hansung.arcade.ingame.item;

import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * 블럭이 부서질 때 아이템이 나오는지와 어떤 아이템인지 정한다.
 * {@link #DROP_CHANCE} 확률로 나오고, 나오면 물풍선 40% · 물줄기 40% · 롤러 신발 20%다.
 *
 * <p>난수는 밖에서 받아서, 테스트는 정해진 값을 넣어 결과를 고정할 수 있다.</p>
 */
public final class ItemDrops {

    /** 블럭이 부서졌을 때 아이템이 나올 확률 */
    public static final double DROP_CHANCE = 0.33;
    /** 아이템이 나왔을 때 물풍선일 확률 */
    static final double BUBBLE_SHARE = 0.4;
    /** 아이템이 나왔을 때 물줄기일 확률 */
    static final double FLUID_SHARE = 0.4;

    private final RandomGenerator random;

    public ItemDrops(RandomGenerator random) {
        this.random = Objects.requireNonNull(random);
    }

    /**
     * 블럭 하나가 다 부서졌을 때 나올 아이템. 난수를 두 번까지 쓴다 (나오는지, 무엇인지).
     *
     * @return 나온 아이템. 안 나오면 비어 있다
     */
    public Optional<ItemType> rollForBrokenBlock() {
        if (random.nextDouble() >= DROP_CHANCE) {
            return Optional.empty();
        }
        double pick = random.nextDouble();
        if (pick < BUBBLE_SHARE) {
            return Optional.of(ItemType.BUBBLE);
        }
        if (pick < BUBBLE_SHARE + FLUID_SHARE) {
            return Optional.of(ItemType.FLUID);
        }
        return Optional.of(ItemType.ROLLER_SHOE);
    }
}

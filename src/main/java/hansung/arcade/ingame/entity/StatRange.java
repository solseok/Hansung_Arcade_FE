package hansung.arcade.ingame.entity;

/**
 * 능력치 하나의 시작값과, 아이템을 먹어 올릴 수 있는 최댓값.
 */
public record StatRange(int base, int max) {

    public StatRange {
        if (base < 1 || max < base) {
            throw new IllegalArgumentException("능력치 범위가 잘못되었습니다: 시작 %d, 최대 %d".formatted(base, max));
        }
    }

    /** value를 시작값~최댓값 안으로 맞춘다. 더한 값이 int 범위를 넘어도 되도록 long으로 받는다. */
    public int clamp(long value) {
        return (int) Math.max(base, Math.min(max, value));
    }
}

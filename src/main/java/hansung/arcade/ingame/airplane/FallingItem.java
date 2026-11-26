package hansung.arcade.ingame.airplane;

/**
 * 비행기에서 떨어져 칸으로 내려오는 중인 아이템. Swing에 의존하지 않는다.
 *
 * <p>비행기는 지나가는 줄의 위쪽 끝을 날고 그 줄에만 떨어뜨리므로({@link Flight#y()}), 아이템은 어느 줄이든 칸 바로 위
 * {@link #START_HEIGHT}px에서 {@link #GRAVITY}로 곧게 떨어져 {@link #FALL_SECONDS}초 뒤에 닿는다.
 * 온라인에서 서버가 ITEM_DROPPED로 알리는 fallMs(129ms)와 같은 시간이다.</p>
 */
public final class FallingItem {

    /** 떨어지는 가속도(px/s²). 서버 {@code AirDrop.GRAVITY}와 같다 */
    public static final double GRAVITY = 2400;
    /** 떨어지기 시작할 때 칸 바닥 자리보다 떠 있는 높이(px). 비행기 배가 칸 가운데보다 위에 있는 만큼이다 */
    public static final double START_HEIGHT = Flight.DROP_HEIGHT;
    /** 칸에 닿기까지 걸리는 시간(초). 20px을 2400px/s²로 떨어져 약 0.129초다 (서버 {@code AirDrop.FALL_MS}) */
    public static final double FALL_SECONDS = Math.sqrt(2 * START_HEIGHT / GRAVITY);

    private final Drop drop;
    /** 닿았을 때 할 일 (온라인: 서버가 정한 아이템을 놓는다). null이면 {@link AirDrop}이 칸을 보고 놓는다 */
    private final Runnable landed;
    private double age;

    FallingItem(Drop drop) {
        this(drop, null);
    }

    FallingItem(Drop drop, Runnable landed) {
        this.drop = drop;
        this.landed = landed;
    }

    /** 닿았을 때 할 일. 없으면 null */
    Runnable landed() {
        return landed;
    }

    void advance(double dt) {
        age += dt;
    }

    /** 칸에 닿았는지 */
    public boolean hasLanded() {
        return height() == 0;
    }

    /** 떨어지는 칸의 바닥 자리보다 떠 있는 높이(px). {@link #START_HEIGHT}에서 줄어 닿으면 0 */
    public double height() {
        return Math.max(0, START_HEIGHT - GRAVITY * age * age / 2);
    }

    /** 떨어지는 칸과 아이템 */
    public Drop drop() {
        return drop;
    }
}

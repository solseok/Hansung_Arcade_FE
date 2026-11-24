package hansung.arcade.ingame.airplane;

/**
 * 비행기에서 떨어져 칸으로 내려오는 중인 아이템. Swing에 의존하지 않는다.
 *
 * <p>떨어지는 칸 바로 위 {@link #startHeight()}px에서 {@link #GRAVITY}로 곧게 떨어진다. 비행기가 맵 맨 위를 날기 때문에
 * 아래 줄일수록 높이 떨어진다(맨 아랫줄은 약 0.6초). 맨 윗줄처럼 비행기 배보다 위에 있는 칸도 {@link #MIN_HEIGHT}에서 떨어진다.</p>
 */
public final class FallingItem {

    /** 떨어지는 가속도(px/s²) */
    public static final double GRAVITY = 2400;
    /** 가장 낮게 떨어질 때의 높이(px). 비행기 배보다 위쪽 칸도 잠깐은 떨어져 보이게 한다 */
    public static final double MIN_HEIGHT = 12;

    private final Drop drop;
    private final double startHeight;
    /** 닿았을 때 할 일 (온라인: 서버가 정한 아이템을 놓는다). null이면 {@link AirDrop}이 칸을 보고 놓는다 */
    private final Runnable landed;
    private double age;

    FallingItem(Drop drop, double startHeight) {
        this(drop, startHeight, null);
    }

    FallingItem(Drop drop, double startHeight, Runnable landed) {
        this.drop = drop;
        this.startHeight = Math.max(MIN_HEIGHT, startHeight);
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

    /** 떨어지는 칸의 바닥 자리보다 떠 있는 높이(px). 닿으면 0 */
    public double height() {
        return Math.max(0, startHeight - GRAVITY * age * age / 2);
    }

    /** 떨어지기 시작할 때의 높이(px) */
    public double startHeight() {
        return startHeight;
    }

    /** 떨어지는 칸과 아이템 */
    public Drop drop() {
        return drop;
    }
}

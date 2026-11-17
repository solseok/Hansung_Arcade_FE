package hansung.arcade.ingame.airplane;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.map.TileMap;

import java.util.ArrayList;
import java.util.List;

/**
 * 맵 위를 가로지르는 비행기 하나. Swing에 의존하지 않는다.
 *
 * <p>위치 {@link #x()}는 비행기가 아이템을 떨어뜨리는 자리(날개 밑 배 가운데)의 맵 x 좌표(px)이고, 높이는 늘 {@link #CRUISE_Y}다.
 * 맵 한쪽 바깥 {@link #OFFSTAGE}px에서 들어와 {@link #SPEED}로 반대쪽 바깥 {@link #OFFSTAGE}px까지 날아가면 사라진다.
 * 떨어뜨리는 자리가 계획한 칸의 열 가운데를 지나는 순간 그 칸을 떨어뜨릴 차례가 된다.</p>
 */
public final class Flight {

    /** 나는 속도(초당 px). 기본 이동속도(초당 180px)인 캐릭터보다 조금 빠르다. 15칸 맵을 3.5초쯤에 가로지른다 */
    public static final double SPEED = 220;
    /**
     * 떨어뜨리는 자리가 맵 끝에서 이만큼 바깥일 때 들어오기 시작하고, 반대쪽 바깥 이만큼에서 사라진다(px).
     * 비행기 그림(146px)에서 떨어뜨리는 자리는 꼬리에서 78px, 기수에서 68px이라 이만큼 바깥이면 그림이 다 맵 밖이다.
     */
    public static final int OFFSTAGE = 80;
    /**
     * 떨어뜨리는 자리의 높이(맵 y, px). 비행기는 맵 맨 위를 가로질러서, 어느 줄을 고르든 아이템이 비행기 배에서 아래로 떨어진다.
     * 그림은 이 자리가 위쪽 끝에서 44px 아래라서 맵 맨 위에 2px 띄워 놓인다.
     */
    public static final int CRUISE_Y = 46;

    private final FlightPlan plan;
    private final double endX;
    /** 아직 지나가지 않은 칸 */
    private final List<Drop> ahead;
    private double x;
    private double age;

    /** @param map 가로지를 맵 */
    Flight(FlightPlan plan, TileMap map) {
        this.plan = plan;
        boolean eastward = plan.heading() == Direction.RIGHT;
        this.x = eastward ? -OFFSTAGE : map.pixelWidth() + OFFSTAGE;
        this.endX = eastward ? map.pixelWidth() + OFFSTAGE : -OFFSTAGE;
        this.ahead = new ArrayList<>(plan.drops());
    }

    /**
     * dt초만큼 날고, 이번에 떨어뜨리는 자리가 열 가운데를 지나친 칸들을 돌려준다. 칸마다 한 번만 돌려준다.
     * 다 날아가 사라진 뒤에는 움직이지 않는다.
     */
    List<Drop> advance(double dt) {
        if (isGone()) {
            return List.of();
        }
        age += dt;
        int dx = plan.heading().dx();
        x = dx > 0 ? Math.min(endX, x + SPEED * dt) : Math.max(endX, x - SPEED * dt);
        List<Drop> passed = new ArrayList<>();
        for (Drop drop : ahead) {
            if (dx * (x - TileMap.centerX(drop.col())) >= 0) {
                passed.add(drop);
            }
        }
        ahead.removeAll(passed);
        return passed;
    }

    /** 맵 반대쪽 바깥까지 날아가 사라졌는지 */
    public boolean isGone() {
        return x == endX;
    }

    /** 떨어뜨리는 자리의 맵 x 좌표(px) */
    public double x() {
        return x;
    }

    /** 나는 방향. {@link Direction#RIGHT} 또는 {@link Direction#LEFT} */
    public Direction heading() {
        return plan.heading();
    }

    /** 나타난 뒤 지난 시간(초). 프로펠러 그림을 돌릴 때 쓴다 */
    public double age() {
        return age;
    }

    public FlightPlan plan() {
        return plan;
    }
}

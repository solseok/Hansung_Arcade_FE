package hansung.arcade.ingame.balloon;

import hansung.arcade.ingame.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 맵 칸 가운데에 놓인 물풍선 하나. 놓은 뒤 {@link #FUSE_SECONDS}초가 지나면 터진다.
 * 놓인 칸은 막힌 칸이지만, 놓는 순간 그 칸에 몸이 걸쳐 있던 플레이어는 그 칸을 벗어날 때까지 지나갈 수 있다.
 */
public final class WaterBalloon {

    /** 놓은 뒤 터질 때까지의 시간(초) */
    public static final double FUSE_SECONDS = 2.0;
    /** 놓인 물풍선 그림 한 장을 보여 주는 시간(초). 4장을 되풀이한다 */
    public static final double FRAME_SECONDS = 0.2;
    /** 1/60초씩 더한 나이가 경계에서 살짝 못 미치는 것을 메운다 */
    private static final double TIME_SLACK = 1e-6;

    private final int ownerId;
    private final int col;
    private final int row;
    private final int range;
    /** 아직 이 칸을 벗어나지 않아 지나갈 수 있는 플레이어 번호 */
    private final Set<Integer> passers;
    private double age;

    WaterBalloon(int ownerId, int col, int row, int range, Collection<Integer> passers) {
        this.ownerId = ownerId;
        this.col = col;
        this.row = row;
        this.range = range;
        this.passers = new HashSet<>(passers);
    }

    /** 놓은 플레이어 번호 */
    public int ownerId() {
        return ownerId;
    }

    public int col() {
        return col;
    }

    public int row() {
        return row;
    }

    /** 터질 때 상하좌우로 뻗는 물줄기 길이(칸). 놓을 때 주인의 물줄기 길이로 정해진다 */
    public int range() {
        return range;
    }

    /** 놓은 뒤 흐른 시간(초). 그림을 고를 때 쓴다 */
    public double age() {
        return age;
    }

    /** player가 이 칸을 지나갈 수 있는지. 놓는 순간 몸이 걸쳐 있었고 아직 벗어나지 않았으면 지나갈 수 있다 */
    public boolean lets(Player player) {
        return passers.contains(player.id());
    }

    void advance(double dt) {
        age += dt;
    }

    boolean isDue() {
        return age >= FUSE_SECONDS - TIME_SLACK;
    }

    /** 이 칸에서 몸이 완전히 빠져나간 플레이어는 다시 들어오지 못한다 */
    void forgetLeavers(List<Player> players) {
        for (Player player : players) {
            if (!player.overlapsTile(col, row)) {
                passers.remove(player.id());
            }
        }
    }
}

package hansung.arcade.ingame.airplane;

import hansung.arcade.ingame.entity.Direction;

import java.util.List;
import java.util.Objects;

/**
 * 비행기 한 번의 출현 계획: 맵을 가로지르는 방향과, 지나가며 떨어뜨리려는 칸들(최대 {@link AirDrop#MAX_DROPS}개).
 * 계획에 든 칸이라도 지나가는 순간 막혀 있으면 떨어뜨리지 않는다 ({@link AirDrop}).
 *
 * @param heading 나는 방향. {@link Direction#RIGHT}면 왼쪽에서 오른쪽으로, {@link Direction#LEFT}면 반대로 난다
 * @param drops   떨어뜨리려는 칸들. 서로 다른 칸이다
 */
public record FlightPlan(Direction heading, List<Drop> drops) {

    public FlightPlan {
        Objects.requireNonNull(heading);
        if (!heading.isHorizontal()) {
            throw new IllegalArgumentException("비행기는 왼쪽이나 오른쪽으로만 납니다: " + heading);
        }
        drops = List.copyOf(drops);
        if (drops.size() > AirDrop.MAX_DROPS) {
            throw new IllegalArgumentException("한 번에 %d개까지만 떨어뜨립니다: %s".formatted(AirDrop.MAX_DROPS, drops));
        }
        if (drops.stream().map(drop -> List.of(drop.col(), drop.row())).distinct().count() != drops.size()) {
            throw new IllegalArgumentException("같은 칸에 두 번 떨어뜨릴 수 없습니다: " + drops);
        }
    }
}

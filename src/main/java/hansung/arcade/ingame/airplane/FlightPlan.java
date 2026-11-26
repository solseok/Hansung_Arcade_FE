package hansung.arcade.ingame.airplane;

import hansung.arcade.ingame.entity.Direction;

import java.util.List;
import java.util.Objects;

/**
 * 비행기 한 번의 출현 계획: 맵을 가로지르는 방향, 따라 지나가는 줄, 그 줄에서 지나가며 떨어뜨리려는 칸들
 * (최대 {@link AirDrop#MAX_DROPS}개). 계획에 든 칸이라도 지나가는 순간 막혀 있으면 떨어뜨리지 않는다 ({@link AirDrop}).
 *
 * @param heading 나는 방향. {@link Direction#RIGHT}면 왼쪽에서 오른쪽으로, {@link Direction#LEFT}면 반대로 난다
 * @param row     지나가는 줄(0부터). 비행기는 이 줄의 위쪽 끝을 날고({@link Flight#y()}), 이 줄에만 떨어뜨린다
 * @param drops   떨어뜨리려는 칸들. 모두 row 줄에 있고 열이 서로 다르다
 */
public record FlightPlan(Direction heading, int row, List<Drop> drops) {

    public FlightPlan {
        Objects.requireNonNull(heading);
        if (!heading.isHorizontal()) {
            throw new IllegalArgumentException("비행기는 왼쪽이나 오른쪽으로만 납니다: " + heading);
        }
        if (row < 0) {
            throw new IllegalArgumentException("줄은 0부터 셉니다: " + row);
        }
        drops = List.copyOf(drops);
        if (drops.size() > AirDrop.MAX_DROPS) {
            throw new IllegalArgumentException("한 번에 %d개까지만 떨어뜨립니다: %s".formatted(AirDrop.MAX_DROPS, drops));
        }
        if (drops.stream().anyMatch(drop -> drop.row() != row)) {
            throw new IllegalArgumentException("비행기는 지나가는 %d번 줄에만 떨어뜨립니다: %s".formatted(row, drops));
        }
        if (drops.stream().map(Drop::col).distinct().count() != drops.size()) {
            throw new IllegalArgumentException("같은 칸에 두 번 떨어뜨릴 수 없습니다: " + drops);
        }
    }
}

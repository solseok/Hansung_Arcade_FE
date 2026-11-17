package hansung.arcade.ingame.balloon;

import hansung.arcade.ingame.entity.Direction;

/**
 * 터진 물풍선의 물줄기가 덮은 칸 하나와, 그 칸에 그릴 물줄기 모양.
 *
 * @param part      칸의 물줄기 모양
 * @param direction 터진 칸에서 이 칸 쪽으로 뻗은 방향. {@link Part#CENTER}이면 null
 */
public record WaterCell(int col, int row, Part part, Direction direction) {

    public enum Part {
        /** 물풍선이 터진 칸 */
        CENTER,
        /** 물줄기 가운데 (끝이 아닌 칸) */
        MIDDLE,
        /** 물줄기 끝 칸. 물줄기 길이에 닿았거나 다음 칸이 막혀 멈춘 칸이다 */
        END
    }
}

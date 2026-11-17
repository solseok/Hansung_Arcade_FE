package hansung.arcade.ingame.entity;

/**
 * 플레이어가 들어갈 수 없는 칸. 맵의 막힌 칸에 놓인 물풍선처럼 맵 밖에서 생긴 장애물을 더할 때 쓴다.
 */
@FunctionalInterface
public interface Obstacles {

    /** (col, row) 칸이 막혀 있는지. 맵 바깥은 막힌 것으로 본다 */
    boolean blocks(int col, int row);
}

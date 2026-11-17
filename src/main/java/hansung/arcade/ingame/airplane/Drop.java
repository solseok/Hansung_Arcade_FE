package hansung.arcade.ingame.airplane;

import hansung.arcade.ingame.item.ItemType;

import java.util.Objects;

/**
 * 비행기가 떨어뜨리려는 칸 하나와 그 칸에 떨어뜨릴 아이템.
 *
 * @param col  맵 칸의 열
 * @param row  맵 칸의 줄
 * @param item 떨어뜨릴 아이템
 */
public record Drop(int col, int row, ItemType item) {

    public Drop {
        Objects.requireNonNull(item);
    }
}

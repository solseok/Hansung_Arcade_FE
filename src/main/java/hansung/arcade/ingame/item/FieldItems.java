package hansung.arcade.ingame.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * 맵 바닥에 놓인 아이템들. 한 칸에 하나뿐이다. Swing에 의존하지 않는다.
 *
 * <p>블럭이 부서질 때, 비행기가 떨어뜨릴 때 등 아이템을 맵에 내려놓는 쪽은 모두 {@link #place}를 쓴다.
 * 누가 언제 먹는지는 게임 상태(GameWorld)가 정하고 {@link #take}로 가져간다.</p>
 */
public final class FieldItems {

    /** 놓인 순서 */
    private final List<FieldItem> items = new ArrayList<>();

    /** (col, row) 칸에 item을 놓는다. 그 칸에 이미 아이템이 있으면 새 아이템으로 바꾼다 */
    public FieldItem place(int col, int row, ItemType item) {
        remove(col, row);
        FieldItem placed = new FieldItem(col, row, item);
        items.add(placed);
        return placed;
    }

    /** (col, row) 칸에 놓인 아이템 */
    public Optional<FieldItem> at(int col, int row) {
        for (FieldItem item : items) {
            if (item.col() == col && item.row() == row) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /** (col, row) 칸의 아이템을 집어 든다. 집은 아이템은 맵에서 사라진다 */
    public Optional<ItemType> take(int col, int row) {
        Optional<FieldItem> found = at(col, row);
        found.ifPresent(items::remove);
        return found.map(FieldItem::type);
    }

    /** (col, row) 칸의 아이템을 없앤다. 없앴는지 돌려준다 */
    public boolean remove(int col, int row) {
        return items.removeIf(item -> item.col() == col && item.row() == row);
    }

    /** cell(col, row)이 참인 칸의 아이템을 모두 없앤다 (물줄기에 맞은 칸 등) */
    public void removeWhere(BiPredicate<Integer, Integer> cell) {
        items.removeIf(item -> cell.test(item.col(), item.row()));
    }

    /** 놓인 아이템들. 놓은 순서 */
    public List<FieldItem> all() {
        return Collections.unmodifiableList(items);
    }

    /** 맵 칸에 놓인 아이템 하나 */
    public record FieldItem(int col, int row, ItemType type) {
    }
}

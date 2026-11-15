package hansung.arcade.ingame.item;

import hansung.arcade.ingame.item.FieldItems.FieldItem;
import hansung.arcade.ingame.map.TileMap;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 아이템을 직접 시험해 볼 때 쓰는 배치. 블럭·비행기가 떨어뜨리지 않는 아이템도 있어서, 실행 옵션
 * (game.test-items=true)을 켜면 게임을 시작할 때 1P 스폰 둘레 빈 칸에 아이템 11종을 한 개씩 깔아 둔다.
 *
 * <p>1P 스폰에서 가까운 칸(가로·세로 칸 수를 더한 거리, 같으면 위 줄·왼쪽 칸 먼저)부터 {@link #ORDER} 순서로 놓는다.
 * 블럭으로 막힌 칸은 거리에 따라 놓을 뿐 길이 이어졌는지는 보지 않는다. 블럭을 부숴야 닿는 칸에 놓일 수도 있다.</p>
 */
public final class TestItems {

    /**
     * 놓는 순서. 블럭·비행기가 떨어뜨리지 않아 시험해 보기 어려운 아이템을 먼저, 스폰에 가까운 칸에 놓는다.
     * 바늘은 갇혀야 쓸 수 있으니 자기 물풍선에 갇혀 시험한다.
     */
    public static final List<ItemType> ORDER = List.of(
            ItemType.GLOVE, ItemType.SHOE, ItemType.SPRING_SHOE, ItemType.SHIELD, ItemType.NEEDLE, ItemType.TANK,
            ItemType.RED_DEVIL, ItemType.ULTRA, ItemType.BUBBLE, ItemType.FLUID, ItemType.ROLLER_SHOE);

    private TestItems() {
    }

    /**
     * 맵의 1P 스폰 둘레 빈 칸에 아이템을 한 종류씩 놓는다. 빈 칸은 바닥 칸(블럭·덤불이 아닌 칸)이면서 스폰이 아니고
     * 아직 아이템이 없는 칸이다. 빈 칸이 모자라면 들어가는 만큼만 놓는다.
     *
     * @return 놓은 아이템들. 놓은 순서
     */
    public static List<FieldItem> scatter(TileMap map, FieldItems items) {
        Point start = map.spawnPoints().get(0);
        List<Point> free = new ArrayList<>();
        for (int row = 0; row < map.rows(); row++) {
            for (int col = 0; col < map.columns(); col++) {
                Point cell = new Point(col, row);
                if (map.tileAt(col, row).isGround() && !map.spawnPoints().contains(cell) && items.at(col, row).isEmpty()) {
                    free.add(cell);
                }
            }
        }
        free.sort(Comparator.comparingInt((Point cell) -> Math.abs(cell.x - start.x) + Math.abs(cell.y - start.y))
                .thenComparingInt(cell -> cell.y)
                .thenComparingInt(cell -> cell.x));
        List<FieldItem> placed = new ArrayList<>();
        for (int i = 0; i < Math.min(ORDER.size(), free.size()); i++) {
            Point cell = free.get(i);
            placed.add(items.place(cell.x, cell.y, ORDER.get(i)));
        }
        return placed;
    }
}

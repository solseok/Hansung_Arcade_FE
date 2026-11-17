package hansung.arcade.ingame.render;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;

import java.util.Collection;

/**
 * 덤불에 숨은 것을 이 화면에서 볼 수 있는지. 그리는 순간의 맵과 화면을 보는 플레이어들로 만든다. Swing에 의존하지 않는다.
 *
 * <ul>
 *   <li>덤불 칸 안의 것(캐릭터·갇힌 캐릭터·물풍선·아이템·물줄기)은 숨는다. 캐릭터는 충돌 박스 가운데가 있는 칸으로 본다.
 *       숨은 것은 그림이 칸 밖으로 삐져나와도 아예 그리지 않는다.</li>
 *   <li>화면을 보는 플레이어(이 컴퓨터에서 조작하는 플레이어, 2인 모드면 둘 다)가 들어간 덤불은 드러난다.
 *       드러난 덤불은 반투명하게 그리고, 그 안의 것도 모두 보인다.</li>
 * </ul>
 */
public final class Concealment {

    private final TileMap map;
    /** 화면을 보는 플레이어가 들어가 있어 안이 보이는 칸 */
    private final boolean[][] revealed;

    private Concealment(TileMap map, boolean[][] revealed) {
        this.map = map;
        this.revealed = revealed;
    }

    /** 이 컴퓨터에서 조작하는 플레이어들이 보는 화면 */
    public static Concealment of(GameWorld world) {
        return forViewers(world.map(), world.localPlayers());
    }

    /**
     * viewers가 보는 화면. 맵에서 사라진 플레이어는 덤불을 드러내지 않는다.
     *
     * @param viewers 화면을 보는 플레이어들
     */
    public static Concealment forViewers(TileMap map, Collection<Player> viewers) {
        boolean[][] revealed = new boolean[map.rows()][map.columns()];
        for (Player viewer : viewers) {
            int col = viewer.col();
            int row = viewer.row();
            if (viewer.state() != PlayerState.GONE && map.isInside(col, row) && map.tileAt(col, row) == Tile.BUSH) {
                revealed[row][col] = true;
            }
        }
        return new Concealment(map, revealed);
    }

    /** (col, row) 칸 안의 것이 숨어 안 보이는지. 덤불이고 화면을 보는 플레이어가 그 안에 없으면 숨는다 */
    public boolean hides(int col, int row) {
        return map.isInside(col, row) && map.tileAt(col, row) == Tile.BUSH && !revealed[row][col];
    }

    /** player가 덤불에 숨어 안 보이는지 */
    public boolean hides(Player player) {
        return hides(player.col(), player.row());
    }

    /** (col, row) 칸이 화면을 보는 플레이어가 들어가 있어 반투명하게 그릴 덤불인지 */
    public boolean isRevealedBush(int col, int row) {
        return map.isInside(col, row) && map.tileAt(col, row) == Tile.BUSH && revealed[row][col];
    }
}

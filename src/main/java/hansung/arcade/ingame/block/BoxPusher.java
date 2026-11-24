package hansung.arcade.ingame.block;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Obstacles;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 몸으로 미는 블럭({@link Tile#isPushable()}). Swing에 의존하지 않는다.
 *
 * <p>보통 상태의 플레이어가 한 방향으로 걷다가 바로 앞(몸 가운데 칸의 다음 칸) 블럭에 막힌 채 {@link #PUSH_SECONDS}초를
 * 계속 밀면, 블럭이 그 방향으로 한 칸 밀린다. 밀려 갈 칸이 바닥이고(블럭·덤불·맵 밖이 아니고) 물풍선·아이템·플레이어·물줄기가
 * 없어야 밀린다. 맵 칸은 미는 순간 옮기고, 그림만 {@link #SLIDE_SECONDS}초 동안 미끄러진다.
 * 미끄러지는 동안에는 그 블럭을 다시 밀 수 없다.</p>
 *
 * <p>온라인 경기는 서버가 민다. 밀 때가 되면 {@link #requestPush}가 밀어 달라고 할 칸을 돌려주고, 서버가 알린 대로
 * {@link #moveFromServer}로 옮긴다.</p>
 */
public final class BoxPusher {

    /** 블럭이 밀리려면 막힌 채 계속 밀어야 하는 시간(초) */
    public static final double PUSH_SECONDS = 0.25;
    /** 밀린 블럭이 한 칸 미끄러지는 시간(초) */
    public static final double SLIDE_SECONDS = 0.2;
    private static final double TIME_SLACK = 1e-6;

    /** 온라인에서 서버에 밀어 달라고 할 블럭 칸과 미는 방향 (BOX_PUSH) */
    public record PushRequest(int col, int row, Direction direction) {
    }

    private final TileMap map;
    /** 플레이어 번호 → 지금 밀고 있는 블럭과 민 시간 */
    private final Map<Integer, Push> pushes = new HashMap<>();
    private final List<SlidingBlock> slides = new ArrayList<>();
    /** 서버가 블럭을 밀어 넣은 칸 → 그 순간 몸이 걸쳐 있어 벗어날 때까지 지나갈 수 있는 플레이어 번호 */
    private final Map<Point, Set<Integer>> passers = new HashMap<>();

    public BoxPusher(TileMap map) {
        this.map = map;
    }

    /**
     * player가 이번 틱에 input 쪽으로 움직이려 한 뒤에 부른다. 막혀서 못 움직였고 앞 칸이 밀리는 블럭이면 민 시간을 쌓고,
     * 다 찼으면 블럭을 한 칸 민다. 움직였거나, 방향을 바꿨거나, 손을 뗐으면 민 시간은 처음부터 다시 센다.
     *
     * @param occupied 블럭을 밀어 넣을 수 없는 칸 (물풍선·아이템·플레이어·물줄기가 있는 칸). 맵의 블럭·덤불은 여기서 따로 본다
     * @return 이번에 블럭을 밀었으면 그 미끄러짐
     */
    public Optional<SlidingBlock> push(Player player, Direction input, double dt, Obstacles occupied) {
        return due(player, input, dt, occupied).map(push -> slide(push.col(), push.row(),
                push.col() + push.direction().dx(), push.row() + push.direction().dy()));
    }

    /**
     * 온라인 경기에서 {@link #push}처럼 민 시간을 쌓되, 다 찼을 때 블럭을 옮기지 않고 서버에 밀어 달라고 할 칸을 돌려준다.
     * 블럭은 서버가 알린 뒤({@link #moveFromServer}) 옮긴다. 돌려준 뒤 민 시간은 처음부터 다시 세므로, 서버가 밀지 않았으면
     * 계속 밀 때 {@link #PUSH_SECONDS}초 뒤 또 돌려준다.
     *
     * @return 이번에 밀어 달라고 할 블럭
     */
    public Optional<PushRequest> requestPush(Player player, Direction input, double dt, Obstacles occupied) {
        return due(player, input, dt, occupied).map(push -> new PushRequest(push.col(), push.row(), push.direction()));
    }

    /**
     * 서버가 알린 대로 (fromCol, fromRow)의 밀리는 블럭을 (toCol, toRow)로 옮기고 그림을 미끄러뜨린다 (온라인 BOX_MOVED).
     * 맵 칸은 바로 옮긴다. 보강된 상자는 남은 맞을 횟수째 옮겨진다.
     *
     * <p>서버는 아무도 걸치지 않은 칸으로만 밀지만, 알림이 오는 사이 이 컴퓨터의 플레이어가 그 칸에 들어섰을 수 있다.
     * 그 순간 몸이 걸쳐 있는 플레이어는 블럭에 갇히지 않도록 그 칸을 벗어날 때까지 지나갈 수 있다 ({@link #lets}).</p>
     *
     * @param players 맵 위의 플레이어
     * @return 옮겼으면 그 미끄러짐. from 칸에 밀리는 블럭이 없거나 to 칸이 맵 밖이거나 바닥이 아니면 옮기지 않고 비어 있다
     */
    public Optional<SlidingBlock> moveFromServer(int fromCol, int fromRow, int toCol, int toRow, List<Player> players) {
        if (!map.isInside(fromCol, fromRow) || !map.tileAt(fromCol, fromRow).isPushable()
                || !map.isInside(toCol, toRow) || !map.tileAt(toCol, toRow).isGround()) {
            return Optional.empty();
        }
        // 미끄러지던 블럭이 또 밀렸으면 앞의 미끄러짐은 끝낸다
        slides.removeIf(slide -> slide.toCol() == fromCol && slide.toRow() == fromRow);
        Set<Integer> caught = new HashSet<>();
        for (Player player : players) {
            if (player.overlapsTile(toCol, toRow)) {
                caught.add(player.id());
            }
        }
        if (caught.isEmpty()) {
            passers.remove(new Point(toCol, toRow));
        } else {
            passers.put(new Point(toCol, toRow), caught);
        }
        return Optional.of(slide(fromCol, fromRow, toCol, toRow));
    }

    /**
     * 서버가 (col, row)로 블럭을 밀어 넣는 순간 몸이 걸쳐 있던 player가 아직 그 칸에 걸쳐 있어 지나갈 수 있는지.
     * 그 칸을 벗어나면 다시 막힌다.
     */
    public boolean lets(Player player, int col, int row) {
        Point cell = new Point(col, row);
        Set<Integer> ids = passers.get(cell);
        if (ids == null || !ids.contains(player.id())) {
            return false;
        }
        if (player.overlapsTile(col, row)) {
            return true;
        }
        ids.remove(player.id());
        if (ids.isEmpty()) {
            passers.remove(cell);
        }
        return false;
    }

    /** 미끄러지는 그림을 dt초 흘린다. 다 미끄러졌거나 도중에 다 부서진 블럭은 목록에서 뺀다 */
    public void advance(double dt) {
        for (SlidingBlock slide : slides) {
            slide.advance(dt);
        }
        // 보강된 상자는 미끄러지다 맞아도(c → b) 여전히 밀리는 블럭이라 끝까지 미끄러진다
        slides.removeIf(slide -> slide.isFinished() || !map.tileAt(slide.toCol(), slide.toRow()).isPushable());
    }

    /** (col, row) 칸으로 미끄러져 들어오는 중인 블럭 */
    public Optional<SlidingBlock> slidingInto(int col, int row) {
        for (SlidingBlock slide : slides) {
            if (slide.toCol() == col && slide.toRow() == row) {
                return Optional.of(slide);
            }
        }
        return Optional.empty();
    }

    /** 미끄러지는 중인 블럭들 */
    public List<SlidingBlock> slides() {
        return Collections.unmodifiableList(slides);
    }

    /**
     * 민 시간을 dt초 쌓고, 블럭을 밀 때가 됐으면 그 블럭과 방향을 돌려준다. 돌려줄 때는 민 시간을 지운다.
     * 밀 수 없는 때(움직였음, 손을 뗌, 앞이 밀리는 블럭이 아님, 그 블럭이 미끄러지는 중)는 민 시간을 지운다.
     */
    private Optional<Push> due(Player player, Direction input, double dt, Obstacles occupied) {
        if (input == null || player.state() != PlayerState.NORMAL || player.isMoving()) {
            pushes.remove(player.id());
            return Optional.empty();
        }
        int col = player.col() + input.dx();
        int row = player.row() + input.dy();
        if (!map.isInside(col, row) || !map.tileAt(col, row).isPushable() || slidingInto(col, row).isPresent()) {
            pushes.remove(player.id());
            return Optional.empty();
        }
        Push previous = pushes.get(player.id());
        double time = previous != null && previous.isSame(input, col, row) ? previous.seconds() + dt : dt;
        Push push = new Push(input, col, row, time);
        if (time < PUSH_SECONDS - TIME_SLACK || !canReceive(col + input.dx(), row + input.dy(), occupied)) {
            // 밀려 갈 칸이 막혀 있으면 민 시간을 그대로 들고 있다가 칸이 비는 대로 민다
            pushes.put(player.id(), push);
            return Optional.empty();
        }
        pushes.remove(player.id());
        return Optional.of(push);
    }

    /** 맵에서 블럭을 옮기고 그림을 미끄러뜨린다 */
    private SlidingBlock slide(int col, int row, int toCol, int toRow) {
        Tile tile = map.tileAt(col, row);
        map.moveBlock(col, row, toCol, toRow);
        SlidingBlock slide = new SlidingBlock(tile, col, row, toCol, toRow);
        slides.add(slide);
        return slide;
    }

    private boolean canReceive(int col, int row, Obstacles occupied) {
        return map.isInside(col, row) && map.tileAt(col, row).isGround() && !occupied.blocks(col, row);
    }

    /** 한 플레이어가 direction 쪽으로 (col, row) 블럭을 seconds초 밀었다 */
    private record Push(Direction direction, int col, int row, double seconds) {
        boolean isSame(Direction otherDirection, int otherCol, int otherRow) {
            return direction == otherDirection && col == otherCol && row == otherRow;
        }
    }
}

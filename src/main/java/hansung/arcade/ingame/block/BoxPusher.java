package hansung.arcade.ingame.block;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Obstacles;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 몸으로 미는 블럭({@link Tile#isPushable()}). Swing에 의존하지 않는다.
 *
 * <p>보통 상태의 플레이어가 한 방향으로 걷다가 바로 앞(몸 가운데 칸의 다음 칸) 블럭에 막힌 채 {@link #PUSH_SECONDS}초를
 * 계속 밀면, 블럭이 그 방향으로 한 칸 밀린다. 밀려 갈 칸이 바닥이고(블럭·덤불·맵 밖이 아니고) 물풍선·아이템·플레이어·물줄기가
 * 없어야 밀린다. 맵 칸은 미는 순간 옮기고, 그림만 {@link #SLIDE_SECONDS}초 동안 미끄러진다.
 * 미끄러지는 동안에는 그 블럭을 다시 밀 수 없다.</p>
 */
public final class BoxPusher {

    /** 블럭이 밀리려면 막힌 채 계속 밀어야 하는 시간(초) */
    public static final double PUSH_SECONDS = 0.25;
    /** 밀린 블럭이 한 칸 미끄러지는 시간(초) */
    public static final double SLIDE_SECONDS = 0.2;
    private static final double TIME_SLACK = 1e-6;

    private final TileMap map;
    /** 플레이어 번호 → 지금 밀고 있는 블럭과 민 시간 */
    private final Map<Integer, Push> pushes = new HashMap<>();
    private final List<SlidingBlock> slides = new ArrayList<>();

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
        int toCol = col + input.dx();
        int toRow = row + input.dy();
        if (time < PUSH_SECONDS - TIME_SLACK || !canReceive(toCol, toRow, occupied)) {
            // 밀려 갈 칸이 막혀 있으면 민 시간을 그대로 들고 있다가 칸이 비는 대로 민다
            pushes.put(player.id(), new Push(input, col, row, time));
            return Optional.empty();
        }
        pushes.remove(player.id());
        Tile tile = map.tileAt(col, row);
        map.moveBlock(col, row, toCol, toRow);
        SlidingBlock slide = new SlidingBlock(tile, col, row, toCol, toRow);
        slides.add(slide);
        return Optional.of(slide);
    }

    /** 미끄러지는 그림을 dt초 흘린다. 다 미끄러졌거나 도중에 부서진 블럭은 목록에서 뺀다 */
    public void advance(double dt) {
        for (SlidingBlock slide : slides) {
            slide.advance(dt);
        }
        slides.removeIf(slide -> slide.isFinished() || map.tileAt(slide.toCol(), slide.toRow()) != slide.tile());
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

package hansung.arcade.ingame.airplane;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * 경기 중 정해진 때 맵 위로 비행기를 띄워 아이템을 떨어뜨린다. Swing에 의존하지 않는다.
 *
 * <ul>
 *   <li>TIMER의 남은 시간이 {@link #APPEARANCES}(1:59, 1:36, 1:14, 0:51, 0:29, 0:07)가 되는 순간 한 대씩 나온다.
 *       경기가 이미 끝났으면 나오지 않는다. 방향은 번갈아 바뀐다: 첫 비행기는 왼쪽에서 오른쪽으로, 다음은 반대로.</li>
 *   <li>나올 때 맵의 모든 칸 가운데 서로 다른 칸 {@link #MAX_DROPS}개를 무작위로 고르고, 칸마다 {@link #CARGO}
 *       (물풍선 · 신발 · 물약 병) 중 하나를 무작위로 정한다.</li>
 *   <li>비행기가 고른 칸의 열을 지나는 순간 그 칸을 본다. 블럭(지나갈 수 없는 칸)·캐릭터·물풍선·이미 놓인 아이템이 있으면
 *       떨어뜨리지 않는다. 그래서 한 번에 0~2개가 떨어진다. 덤불은 지나갈 수 있는 칸이라 떨어뜨리고, 떨어진 아이템은 덤불에 숨는다.
 *       아이템이 있는 칸에 떨어뜨리면 그 아이템을 바꿔 버리므로 떨어뜨리지 않는다.</li>
 *   <li>떨어뜨린 아이템은 짧게 떨어지는 연출({@link FallingItem}) 뒤 칸에 놓인다. 그 사이 밀린 블럭이 들어와 칸이 막혔으면 놓지 않는다.</li>
 *   <li>날고 있는 중에 경기가 끝나면 비행기는 마저 지나가지만 더 떨어뜨리지 않는다. 이미 떨어지던 아이템은 놓인다.</li>
 * </ul>
 *
 * <p>난수는 밖에서 받아서 테스트는 결과를 고정할 수 있다. 한 번 나올 때 {@link RandomGenerator#nextDouble()}을
 * 칸 · 아이템 · 칸 · 아이템 순서로 네 번 쓴다.</p>
 */
public final class AirDrop {

    /** 비행기가 나오는 남은 시간(초). 1:59, 1:36, 1:14, 0:51, 0:29, 0:07 */
    public static final List<Integer> APPEARANCES = List.of(119, 96, 74, 51, 29, 7);
    /** 비행기가 떨어뜨리는 아이템: 물풍선(water_balloon) · 신발(shoe) · 물약 병(flask) */
    public static final List<ItemType> CARGO = List.of(ItemType.BUBBLE, ItemType.SHOE, ItemType.FLUID);
    /** 한 번 나올 때 떨어뜨리는 최대 개수 */
    public static final int MAX_DROPS = 2;

    private static final Logger log = LoggerFactory.getLogger(AirDrop.class);

    private final TileMap map;
    private final RandomGenerator random;
    private final List<Flight> flights = new ArrayList<>();
    private final List<FallingItem> falling = new ArrayList<>();
    private int launches;

    /**
     * @param map    비행기가 가로지를 맵
     * @param random 떨어뜨릴 칸과 아이템을 고를 난수
     */
    public AirDrop(TileMap map, RandomGenerator random) {
        this.map = Objects.requireNonNull(map);
        this.random = Objects.requireNonNull(random);
    }

    /**
     * dt초(한 틱) 진행한다. 경기 시계를 흘린 뒤에 불러야 이번 틱에 남은 시간이 출현 시각이 됐는지 알 수 있다.
     * 비행기를 띄우고, 날리고, 지나친 칸에 아이템을 떨어뜨리고, 닿은 아이템을 맵에 놓는다.
     */
    public void update(double dt, GameWorld world) {
        Match match = world.match();
        if (!match.isOver()) {
            for (int seconds : APPEARANCES) {
                if (match.clock().justReached(seconds)) {
                    launch(plan());
                }
            }
        }
        for (Iterator<Flight> it = flights.iterator(); it.hasNext(); ) {
            Flight flight = it.next();
            for (Drop drop : flight.advance(dt)) {
                release(drop, world);
            }
            if (flight.isGone()) {
                it.remove();
            }
        }
        for (Iterator<FallingItem> it = falling.iterator(); it.hasNext(); ) {
            FallingItem item = it.next();
            item.advance(dt);
            if (item.hasLanded()) {
                land(item.drop(), world);
                it.remove();
            }
        }
    }

    /** 이번에 나올 비행기의 계획을 난수로 정한다. 방향은 나온 차례로 번갈아 정한다 */
    FlightPlan plan() {
        Direction heading = launches % 2 == 0 ? Direction.RIGHT : Direction.LEFT;
        int cells = map.columns() * map.rows();
        List<Drop> drops = new ArrayList<>();
        List<Integer> taken = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_DROPS, cells); i++) {
            // 아직 안 고른 칸들 가운데 하나. 고른 칸 번호보다 크거나 같으면 그만큼 건너뛴다
            int cell = pick(cells - taken.size());
            for (int chosen : taken.stream().sorted().toList()) {
                if (cell >= chosen) {
                    cell++;
                }
            }
            taken.add(cell);
            drops.add(new Drop(cell % map.columns(), cell / map.columns(), CARGO.get(pick(CARGO.size()))));
        }
        return new FlightPlan(heading, drops);
    }

    /** 0 이상 bound 미만의 정수 하나 */
    private int pick(int bound) {
        return Math.min(bound - 1, (int) (random.nextDouble() * bound));
    }

    /**
     * plan대로 비행기를 바로 띄운다. 경기 시계와 상관없이 띄우므로 출현 시각은 {@link #update}가 맡는다.
     * 정해 둔 계획을 그대로 재연할 때(테스트, 이후 네트워크 동기화) 쓴다.
     */
    public void launch(FlightPlan plan) {
        flights.add(new Flight(plan, map));
        launches++;
        log.debug("비행기 출현: {} 쪽으로, 떨어뜨릴 칸 {}", plan.heading(), plan.drops());
    }

    /** 비행기가 칸 위를 지나는 순간. 칸이 비어 있고 경기가 진행 중이면 떨어뜨린다 */
    private void release(Drop drop, GameWorld world) {
        if (world.match().isOver() || !canDropAt(world, drop.col(), drop.row())) {
            log.debug("떨어뜨리지 않음: {}", drop);
            return;
        }
        falling.add(new FallingItem(drop, TileMap.centerY(drop.row()) - Flight.CRUISE_Y));
    }

    /** 떨어지던 아이템이 칸에 닿는 순간. 그 사이 블럭이 밀려 들어와 막혔으면 놓지 않는다 */
    private void land(Drop drop, GameWorld world) {
        if (map.isSolid(drop.col(), drop.row())) {
            log.debug("막힌 칸에 떨어져 사라짐: {}", drop);
            return;
        }
        world.items().place(drop.col(), drop.row(), drop.item());
    }

    /**
     * (col, row) 칸에 아이템을 떨어뜨릴 수 있는지. 블럭(지나갈 수 없는 칸)·물풍선·이미 놓인 아이템·캐릭터(충돌 박스 가운데가
     * 그 칸에 있는, 맵에서 사라지지 않은 플레이어)가 있으면 떨어뜨리지 않는다. 덤불 칸은 떨어뜨린다.
     */
    static boolean canDropAt(GameWorld world, int col, int row) {
        TileMap map = world.map();
        if (!map.isInside(col, row) || map.isSolid(col, row)) {
            return false;
        }
        if (world.balloonField().balloonAt(col, row).isPresent() || world.items().at(col, row).isPresent()) {
            return false;
        }
        return world.players().stream()
                .noneMatch(p -> p.state() != PlayerState.GONE && p.col() == col && p.row() == row);
    }

    /** 지금 맵 위를 날고 있는 비행기들. 나온 순서 */
    public List<Flight> flights() {
        return Collections.unmodifiableList(flights);
    }

    /** 비행기에서 떨어져 내려오는 중인 아이템들. 떨어뜨린 순서 */
    public List<FallingItem> fallingItems() {
        return Collections.unmodifiableList(falling);
    }
}

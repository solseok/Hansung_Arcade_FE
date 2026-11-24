package hansung.arcade.ingame.map;

import hansung.arcade.ingame.entity.Direction;

/**
 * 맵 한 칸의 종류. 맵 파일에서는 {@link #symbol} 한 글자로 표기한다.
 * 기호의 뜻은 테마와 상관없이 같고, 어떤 그림으로 보일지는 테마별 타일셋이 정한다.
 *
 * <ul>
 *   <li>바닥 타일({@link #isGround()})은 아무것도 막지 않고 그 위에 블럭·덤불이 놓일 수 있다.</li>
 *   <li>부서지는 블럭({@link #isDestructible()})은 물줄기에 맞으면 {@link #afterHit()}로 바뀌거나 부서진다.</li>
 *   <li>밀리는 블럭({@link #isPushable()})은 몸으로 밀면 한 칸 밀린다.</li>
 *   <li>덤불({@link #BUSH})은 지나갈 수 있고, 물줄기도 지나가며 부서지지 않는다. 안에 든 것을 가린다.</li>
 *   <li>가시 블럭({@link #thornSide()})은 가시 난 면에 밀려 온 물풍선을 바로 터뜨린다.</li>
 * </ul>
 */
public enum Tile {
    /** 바닥 (풀, 흙) */
    FLOOR('.', false, false),
    /** 일반 길 (도로, 흙길). 지나갈 수 있는 바닥 */
    ROAD('_', false, false),
    /** 세로 점선 차선이 그려진 도로. 지나갈 수 있는 바닥 */
    ROAD_LANE('|', false, false),
    /** 가로로 놓인 횡단보도. 지나갈 수 있는 바닥 */
    CROSSWALK('=', false, false),
    /** 물줄기로 부술 수 있는 일반 블록 (빌리지: 빨강 레고) */
    BLOCK('B', true, true),
    /** 일반 블록의 두 번째 모양 (빌리지: 주황 레고) */
    BLOCK_2('O', true, true),
    /** 일반 블록의 또 다른 모양 (캠프: 얼룩무늬 블럭). 일반 블록처럼 한 번에 부서지고 밀리지 않는다 */
    SIDE_BLOCK('-', true, true),
    /** 덤불. 지나갈 수 있고 안에 들어간 엔터티를 가린다 */
    BUSH('T', false, false),
    /** 몸으로 밀 수 있고 한 번 맞으면 부서지는 상자 */
    PUSH_BOX('P', true, true),
    /** 세 번 맞아야 부서지는 블록. 맞을 때마다 {@link #HARD_BLOCK_2} → {@link #HARD_BLOCK_1} 로 바뀐다 */
    HARD_BLOCK_3('c', true, true),
    /** 두 번 더 맞아야 부서지는 블록 */
    HARD_BLOCK_2('b', true, true),
    /** 한 번 더 맞으면 부서지는 블록 */
    HARD_BLOCK_1('a', true, true),
    /** 오른쪽 면에 가시가 난 블록. 왼쪽으로 밀려 와 오른쪽 면에 닿은 물풍선은 바로 터진다 */
    ONE_WAY_RIGHT('>', true, true),
    /** 왼쪽 면에 가시가 난 블록. 오른쪽으로 밀려 와 왼쪽 면에 닿은 물풍선은 바로 터진다 */
    ONE_WAY_LEFT('<', true, true),
    /** 위쪽 면에 가시가 난 블록. 아래로 밀려 와 위쪽 면에 닿은 물풍선은 바로 터진다 */
    ONE_WAY_UP('^', true, true),
    /** 아래쪽 면에 가시가 난 블록. 위로 밀려 와 아래쪽 면에 닿은 물풍선은 바로 터진다 */
    ONE_WAY_DOWN('v', true, true),
    /** 부서지지 않는 석상 */
    STATUE('#', true, false),
    /** 부서지지 않는 철조망 */
    WIRE('W', true, false),
    /** 바닥 가시. 지나갈 수 있고, 이 칸에 놓이거나 들어온 물풍선은 바로 터진다 (BalloonField) */
    SPIKE('!', false, false),
    /** 부서지지 않는 미사일. 가로 2칸을 차지하므로 맵에서 MM 처럼 붙여 쓴다 */
    MISSILE('M', true, false, 2),
    /** 부서지지 않는 집 (빌리지: 빨간 지붕) */
    HOUSE_1('H', true, false),
    /** 부서지지 않는 집의 두 번째 모양 (빌리지: 노란 지붕) */
    HOUSE_2('J', true, false),
    /** 부서지지 않는 집의 세 번째 모양 (빌리지: 파란 지붕) */
    HOUSE_3('K', true, false),
    /** 부서지지 않는 나무 */
    TREE('Y', true, false);

    private final char symbol;
    private final boolean solid;
    private final boolean destructible;
    private final int width;

    Tile(char symbol, boolean solid, boolean destructible) {
        this(symbol, solid, destructible, 1);
    }

    Tile(char symbol, boolean solid, boolean destructible, int width) {
        this.symbol = symbol;
        this.solid = solid;
        this.destructible = destructible;
        this.width = width;
    }

    public char symbol() {
        return symbol;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isDestructible() {
        return destructible;
    }

    /** 가로로 차지하는 칸 수. 2 이상이면 맵에서 같은 글자를 그 수만큼 이어 쓴다. */
    public int width() {
        return width;
    }

    /**
     * 바닥에 붙은 타일인지 (바닥, 길, 차선, 횡단보도, 바닥 가시). 아무것도 막거나 가리지 않고,
     * 그 위에 블럭이 놓였다가 치워지면 다시 드러난다.
     */
    public boolean isGround() {
        return switch (this) {
            case FLOOR, ROAD, ROAD_LANE, CROSSWALK, SPIKE -> true;
            default -> false;
        };
    }

    /**
     * 몸으로 밀어 한 칸씩 옮길 수 있는 블럭인지. 상자와 철로 보강된 상자(c·b·a)다 (네프 API BOX_PUSH, 서버 Tile.isPushable과 같다).
     * 보강된 상자는 타일째 옮기므로 남은 맞을 횟수도 그대로 따라간다.
     */
    public boolean isPushable() {
        return switch (this) {
            case PUSH_BOX, HARD_BLOCK_3, HARD_BLOCK_2, HARD_BLOCK_1 -> true;
            default -> false;
        };
    }

    /**
     * 물줄기에 한 번 맞은 뒤 남는 블럭. 여러 번 맞아야 하는 블럭은 한 단계 약해진 블럭이고,
     * 이번에 부서지면 null이다. 부서지지 않는 칸은 그대로 자기 자신이다.
     */
    public Tile afterHit() {
        if (!destructible) {
            return this;
        }
        return switch (this) {
            case HARD_BLOCK_3 -> HARD_BLOCK_2;
            case HARD_BLOCK_2 -> HARD_BLOCK_1;
            default -> null;
        };
    }

    /** 가시가 난 면의 방향 (예: {@link #ONE_WAY_LEFT}면 왼쪽). 가시 블럭이 아니면 null */
    public Direction thornSide() {
        return switch (this) {
            case ONE_WAY_LEFT -> Direction.LEFT;
            case ONE_WAY_RIGHT -> Direction.RIGHT;
            case ONE_WAY_UP -> Direction.UP;
            case ONE_WAY_DOWN -> Direction.DOWN;
            default -> null;
        };
    }

    /**
     * travel 방향으로 밀려 오던 물풍선이 이 블럭에 닿았을 때 바로 터지는지. 가시 난 면에 닿았을 때만 터진다.
     * 예를 들어 {@link #ONE_WAY_LEFT}는 오른쪽으로 밀려 와 왼쪽 면에 닿은 물풍선을 터뜨린다.
     * 물풍선을 미끄러뜨리는 쪽(신발 아이템 등)이 블럭 앞에 멈출 때 이 블럭에 묻는다.
     */
    public boolean popsBalloonMoving(Direction travel) {
        Direction thorn = thornSide();
        return thorn != null && thorn == travel.opposite();
    }

    /** 맵 파일 글자에 해당하는 타일. 없으면 null. */
    public static Tile fromSymbol(char symbol) {
        for (Tile tile : values()) {
            if (tile.symbol == symbol) {
                return tile;
            }
        }
        return null;
    }
}

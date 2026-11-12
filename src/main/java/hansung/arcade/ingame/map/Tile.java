package hansung.arcade.ingame.map;

/**
 * 맵 한 칸의 종류. 맵 파일에서는 {@link #symbol} 한 글자로 표기한다.
 * 기호의 뜻은 테마와 상관없이 같고, 어떤 그림으로 보일지는 테마별 타일셋이 정한다.
 *
 * <p>파괴·밀기·숨기·방향 제한 같은 동작은 이후 티켓에서 구현한다. 지금은 충돌 여부만 쓴다.</p>
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
    /** 좌우 양방향에서만 상호작용하는 블록 */
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
    /** 오른쪽에서만 상호작용하는 블록 */
    ONE_WAY_RIGHT('>', true, true),
    /** 왼쪽에서만 상호작용하는 블록 */
    ONE_WAY_LEFT('<', true, true),
    /** 위쪽에서만 상호작용하는 블록 */
    ONE_WAY_UP('^', true, true),
    /** 아래쪽에서만 상호작용하는 블록 */
    ONE_WAY_DOWN('v', true, true),
    /** 부서지지 않는 석상 */
    STATUE('#', true, false),
    /** 부서지지 않는 철조망 */
    WIRE('W', true, false),
    /** 바닥 가시. 지나갈 수 있다 */
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

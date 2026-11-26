package hansung.arcade.ingame.item;

import hansung.arcade.ingame.balloon.BalloonField;
import hansung.arcade.ingame.balloon.WaterBalloon;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.map.TileMap;

import java.util.List;
import java.util.Optional;

/**
 * 아이템으로 생긴 능력을 쓰는 일: 물풍선 밀기(신발·빨간 악마)·던지기(장갑)와 액티브 아이템(바늘·보호막·스프링 신발).
 * 게임 상태(GameWorld)가 틱마다 플레이어의 명령을 보고 부른다. Swing에 의존하지 않는다.
 */
public final class ItemActions {

    /** 스프링 신발로 뛰는 거리(칸). 바로 앞 한 칸을 넘어 그 너머 칸에 내려앉는다 */
    public static final int JUMP_TILES = 2;

    private final TileMap map;
    private final BalloonField balloonField;

    public ItemActions(TileMap map, BalloonField balloonField) {
        this.map = map;
        this.balloonField = balloonField;
    }

    /**
     * 물풍선 키를 막 눌렀다. 장갑이 있으면 선 칸의 물풍선을, 없으면 바라보는 앞 칸의 물풍선을 바라보는 쪽으로 던진다.
     * 던질 물풍선이 없거나 장갑이 없으면 선 칸에 물풍선을 놓는다. 점프해 공중에 있으면 아무것도 하지 않는다.
     *
     * @param players 맵 위의 모든 플레이어
     * @return 물풍선을 놓았는지·던졌는지 (효과음을 고를 때 쓴다)
     */
    public BalloonKeyResult pressBalloonKey(Player player, PlayerGear gear, List<Player> players) {
        if (gear.isAirborne()) {
            return BalloonKeyResult.NOTHING;
        }
        if (gear.canThrowBalloons() && player.state() == PlayerState.NORMAL && throwNearby(player)) {
            return BalloonKeyResult.THROWN;
        }
        return balloonField.place(player, players).isPresent() ? BalloonKeyResult.PLACED : BalloonKeyResult.NOTHING;
    }

    /** 물풍선 키를 눌러 일어난 일 */
    public enum BalloonKeyResult {
        /** 아무 일도 없었다 (공중에 있거나, 갇혔거나, 놓을 수 있는 개수가 찼거나, 칸이 막혔다) */
        NOTHING,
        /** 선 칸에 물풍선을 놓았다 */
        PLACED,
        /** 장갑으로 물풍선을 던졌다 */
        THROWN
    }

    private boolean throwNearby(Player player) {
        return balloonToThrow(player).map(found -> balloonField.throwBalloon(found, player.facing())).orElse(false);
    }

    /**
     * 장갑으로 물풍선 키를 누르면 집어 들 물풍선: 선 칸(몸 가운데 칸)에 가만히 놓인 물풍선, 없으면 바라보는 앞 칸의 것.
     * 던질 수 있는지({@link BalloonField#canThrow})는 따로 본다. 선 칸의 물풍선을 던질 수 없으면 앞 칸의 것도 던지지 않는다.
     * 온라인에서는 이 물풍선을 던져 달라고 서버에 보낸다.
     */
    public Optional<WaterBalloon> balloonToThrow(Player player) {
        Direction facing = player.facing();
        return restingAt(player.col(), player.row())
                .or(() -> restingAt(player.col() + facing.dx(), player.row() + facing.dy()));
    }

    private Optional<WaterBalloon> restingAt(int col, int row) {
        return balloonField.balloonAt(col, row).filter(WaterBalloon::isResting);
    }

    /**
     * player가 move 쪽으로 걸으려다 막혔을 때 부른다. 물풍선을 밀 수 있고 바로 앞(몸 가운데 칸의 다음 칸)이
     * 가만히 놓인 물풍선이면 그 방향으로 민다.
     *
     * @return 물풍선을 밀었는지
     */
    public boolean kick(Player player, PlayerGear gear, Direction move) {
        return balloonToKick(player, gear, move).map(balloon -> balloonField.push(balloon, move)).orElse(false);
    }

    /**
     * player가 move 쪽으로 걸으려다 막혔을 때 찰 물풍선: 물풍선을 밀 수 있고(신발·빨간 악마), 땅에 선 보통 상태로 움직이지 못했고,
     * 바로 앞(몸 가운데 칸의 다음 칸)에 가만히 놓인 물풍선이 player를 막고 있다. 미끄러질 수 있는지({@link BalloonField#canKick})는
     * 따로 본다. 온라인에서는 이 물풍선을 차 달라고 서버에 보낸다.
     */
    public Optional<WaterBalloon> balloonToKick(Player player, PlayerGear gear, Direction move) {
        if (move == null || !gear.canPushBalloons() || gear.isAirborne()
                || player.state() != PlayerState.NORMAL || player.isMoving()) {
            return Optional.empty();
        }
        return restingAt(player.col() + move.dx(), player.row() + move.dy())
                .filter(balloon -> !balloon.lets(player));
    }

    /**
     * 아이템 키를 막 눌렀다. 칸에 든 액티브 아이템을 쓸 수 있으면 쓰고 하나 줄인다. 쓸 수 없는 때면 아이템은 그대로 남는다.
     * <ul>
     *   <li>바늘: 물풍선에 갇혀 있을 때만. 바로 빠져나오기 시작한다 ({@link Player#escape()}).</li>
     *   <li>보호막: 보통 상태일 때. {@link PlayerGear#SHIELD_SECONDS}초 동안 물줄기에 맞지 않는다. 쓰는 중에는 또 쓰지 않는다.</li>
     *   <li>스프링 신발: 보통 상태일 때. 바라보는 쪽으로 {@link #JUMP_TILES}칸 앞 칸에 뛰어 내려앉는다.
     *       바로 앞 칸에 무엇이 있든 넘고, 내려앉을 칸이 맵 밖·통과할 수 없는 블럭이거나 물풍선이 차지한 칸
     *       ({@link BalloonField#holds}: 미끄러져 막 들어오거나 던져져 내려앉을 칸 포함)이면 쓰지 않는다.</li>
     * </ul>
     *
     * @return 아이템을 썼는지
     */
    public boolean useItem(Player player, PlayerGear gear) {
        Optional<ItemType> item = gear.activeItem();
        if (item.isEmpty()) {
            return false;
        }
        boolean used = switch (item.get()) {
            case NEEDLE -> player.escape();
            case SHIELD -> raiseShield(player, gear);
            case SPRING_SHOE -> jump(player, gear);
            default -> false;
        };
        if (used) {
            gear.useOne();
        }
        return used;
    }

    private static boolean raiseShield(Player player, PlayerGear gear) {
        if (player.state() != PlayerState.NORMAL || gear.isShielded() || gear.isAirborne()) {
            return false;
        }
        gear.raiseShield();
        return true;
    }

    private boolean jump(Player player, PlayerGear gear) {
        if (player.state() != PlayerState.NORMAL || gear.isAirborne()) {
            return false;
        }
        Direction facing = player.facing();
        int col = player.col() + facing.dx() * JUMP_TILES;
        int row = player.row() + facing.dy() * JUMP_TILES;
        if (map.isSolid(col, row) || balloonField.holds(col, row)) {
            return false;
        }
        gear.startJump(player, col, row);
        return true;
    }
}

package hansung.arcade.ingame.render;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.balloon.BalloonField;
import hansung.arcade.ingame.balloon.Explosion;
import hansung.arcade.ingame.balloon.WaterBalloon;
import hansung.arcade.ingame.balloon.WaterCell;
import hansung.arcade.ingame.block.BoxPusher;
import hansung.arcade.ingame.block.SlidingBlock;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.item.FieldItems.FieldItem;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.item.PlayerGear;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 맵은 테마 타일셋의 BMP 그림으로, 캐릭터는 고른 마스코트의 BMP 그림({@link CharacterSprites})을 고른 고정색으로 칠해 그린다.
 * 물풍선과 물줄기는 {@link BalloonSprites}, 바닥에 떨어진 아이템은 {@link ItemSprites} 그림이다.
 * 바닥 그림이 없는 테마는 단색 바닥을, 그림이 없는 타일은 기호를 적은 임시 상자를 그린다.
 * 호출 전에 Graphics2D의 원점이 맵 왼쪽 위로 옮겨져 있어야 한다.
 *
 * <p>바닥({@link #drawGround})과 그 위 물체({@link #drawObjects})를 따로 그린다. 블록·나무·집 그림은 칸보다 키가 커서
 * 맨 윗줄에서는 맵 위로 솟으므로, 화면은 바닥만 맵 영역 안으로 자르고 물체는 맵 테두리 위에 겹쳐 그릴 수 있다.</p>
 *
 * <p>캐릭터는 상태에 따라 다른 그림이다. 갇히면 물풍선 속 그림, 빠져나오는 중이면 바늘 그림, 탈락하면 쓰러진 그림이
 * {@link Player#DYING_JUMP_SECONDS}초 동안 포물선으로 {@link #DYING_JUMP_HEIGHT}px 솟았다 떨어진 뒤,
 * {@link Player#DYING_BLINK_SECONDS}초 동안 {@link #BLINK_SECONDS}초씩 보였다 숨었다 하고 사라진다.</p>
 *
 * <p>덤불에 숨은 캐릭터·물풍선·아이템·물줄기는 {@link Concealment}를 보고 아예 그리지 않는다.
 * 화면을 보는 플레이어가 들어간 덤불만 {@link #REVEALED_BUSH_ALPHA} 불투명도로 그려 안이 비쳐 보이게 한다.</p>
 *
 * <p>아이템으로 생긴 것({@link PlayerGear}): 탱크에 타면 캐릭터를 {@link #TANK_SEAT}px 올리고 탱크 그림을 캐릭터 아랫몸 앞에
 * {@link #TANK_SIZE}px로 그려 탱크에 들어앉은 모습으로 보인다.
 * 보호막을 쓰는 중이면 캐릭터를 반투명한 물방울 막으로 감싸고, 끝나기 {@link #SHIELD_WARNING_SECONDS}초 전부터 깜빡인다.
 * 탱크가 막 부서진 뒤 맞지 않는 동안은 캐릭터가 깜빡인다. 스프링 신발로 뛰면 그림자는 땅에 두고 캐릭터만 포물선으로
 * {@link #JUMP_HEIGHT}px 솟는다. 밀려 미끄러지는 물풍선은 칸 사이에, 던진 물풍선은 모든 줄을 그린 뒤 맨 위에
 * 땅의 그림자와 함께 떠 있는 높이만큼 올려 그린다. 날아가는 물풍선도 땅 위 자리가 숨은 덤불이면 그리지 않는다.</p>
 */
public final class WorldRenderer {

    private static final int T = TileMap.TILE_SIZE;

    private static final Color FLOOR_LIGHT = new Color(0x8BC662);
    private static final Color FLOOR_DARK = new Color(0x7FBA56);
    private static final Color MISSING_FILL = new Color(0x5C626B);
    private static final Color MISSING_TEXT = new Color(0xFFE066);
    private static final Font MISSING_FONT = new Font(Font.MONOSPACED, Font.BOLD, 16);

    private static final Color SHADOW = new Color(0, 0, 0, 55);
    /**
     * 캐릭터 위치(충돌 박스 가운데)에서 발끝까지의 거리. 칸 가운데에 서면 발끝이 칸 바닥보다 6px 위에 와서,
     * 아래 칸 블록 그림이 칸 위로 솟은 부분에 발이 가려지지 않는다.
     */
    public static final int FEET_BELOW_CENTER = T / 2 - 6;
    /** 탈락한 캐릭터가 솟아오르는 가장 높은 높이(px). 한 칸 */
    public static final int DYING_JUMP_HEIGHT = T;
    /** 탈락 연출 끝에 깜빡일 때 한 번 보이거나 숨는 시간(초). 보임·숨음 한 쌍이 0.2초다 */
    public static final double BLINK_SECONDS = 0.1;
    /** 화면을 보는 플레이어가 들어간 덤불의 불투명도. 안에 든 것이 비쳐 보인다 */
    public static final float REVEALED_BUSH_ALPHA = 0.5f;
    /** 바닥에 떨어진 아이템이 칸 가운데보다 떠 있는 높이(px) */
    static final int ITEM_LIFT = 4;
    /** 떠 있는 아이템이 위아래로 흔들리는 폭(px). 가운데 높이에서 위아래로 이만큼 */
    static final int ITEM_BOB = 2;
    /** 아이템이 한 번 오르내리는 시간(초) */
    static final double ITEM_BOB_SECONDS = 1.0;
    /** 스프링 신발로 뛴 캐릭터가 가장 높이 솟는 높이(px). 한 칸 */
    public static final int JUMP_HEIGHT = T;
    /** 탱크에 탄 캐릭터를 땅보다 올려 그리는 높이(px) */
    public static final int TANK_SEAT = 6;
    /** 탱크 그림을 그리는 크기(px). 아이템 그림(38)보다 조금 크게 */
    public static final int TANK_SIZE = 46;
    /** 탱크 그림 아래 끝이 발끝보다 낮은 거리(px) */
    static final int TANK_BELOW_FEET = 5;
    /** 보호막 물방울의 반지름(px). 캐릭터 몸을 다 감싼다 */
    static final int SHIELD_RADIUS = 27;
    /** 보호막 물방울 가운데가 발끝보다 높은 거리(px) */
    static final int SHIELD_CENTER_ABOVE_FEET = 26;
    /** 보호막이 이만큼 남으면 깜빡여 곧 끝난다고 알린다(초) */
    public static final double SHIELD_WARNING_SECONDS = 0.6;
    private static final Color SHIELD_FILL = new Color(120, 200, 255, 85);
    private static final Color SHIELD_EDGE = new Color(225, 245, 255, 220);
    private static final Color SHIELD_SHINE = new Color(255, 255, 255, 170);
    private static final Stroke SHIELD_STROKE = new BasicStroke(2f);

    private static final double FRAME_BOUNDARY_SLACK = 1e-6;

    private final Tileset tileset;
    private final CharacterSprites characters;
    private final BalloonSprites balloons;
    private final ItemSprites items;

    public WorldRenderer(Tileset tileset, CharacterSprites characters, BalloonSprites balloons, ItemSprites items) {
        this.tileset = Objects.requireNonNull(tileset);
        this.characters = Objects.requireNonNull(characters);
        this.balloons = Objects.requireNonNull(balloons);
        this.items = Objects.requireNonNull(items);
    }

    /** 아이템 그림을 기본 위치에서 새로 읽는다. 미리 읽어 둔 그림이 없는 테스트에서 쓴다. */
    public WorldRenderer(Tileset tileset, CharacterSprites characters, BalloonSprites balloons) {
        this(tileset, characters, balloons, ItemSprites.load());
    }

    /** 물풍선·아이템 그림을 기본 위치에서 새로 읽는다. 미리 읽어 둔 그림이 없는 테스트에서 쓴다. */
    public WorldRenderer(Tileset tileset, CharacterSprites characters) {
        this(tileset, characters, BalloonSprites.load());
    }

    /** 이 컴퓨터에서 조작하는 플레이어들이 보는 화면으로 그린다. {@link #drawObjects(Graphics2D, GameWorld, Concealment)} 참고 */
    public void drawObjects(Graphics2D g, GameWorld world) {
        drawObjects(g, world, Concealment.of(world));
    }

    /**
     * 블록·덤불 등 물체, 아이템, 물줄기, 물풍선, 캐릭터. 바닥을 먼저 그린 뒤에 부른다.
     *
     * <p>위쪽 줄부터 그려서 아래쪽 물체가 앞에 보이게 한다. 한 줄 안에서는 블록 → 아이템 → 물줄기 → 물풍선 → 캐릭터 → 덤불 순서다.
     * 그래서 아래 줄 블록이 칸 위로 솟은 부분은 윗줄의 아이템·물줄기·물풍선·캐릭터를 가린다.
     * 덤불 칸 안의 것은 concealment가 숨기는 칸이면 그리지 않고, 드러난 덤불은 반투명하게 덮는다.</p>
     *
     * @param concealment 덤불 안의 것을 이 화면에서 볼 수 있는지
     */
    public void drawObjects(Graphics2D g, GameWorld world, Concealment concealment) {
        TileMap map = world.map();
        BalloonField field = world.balloonField();
        List<Player> players = world.players();
        for (int row = 0; row < map.rows(); row++) {
            drawBlocks(g, map, world.boxPusher(), row);
            drawItems(g, world, row, concealment);
            drawWater(g, field, row, concealment);
            drawBalloons(g, field, row, concealment);
            for (Player player : players) {
                if (player.row() == row && !concealment.hides(player)) {
                    drawPlayer(g, player, world.gear(player), world.elapsedSeconds());
                }
            }
            drawBushes(g, map, row, concealment);
        }
        drawFlyingBalloons(g, field, concealment);
    }

    /** row줄의 덤불이 아닌 물체. 밀려 미끄러지는 블럭은 떠난 칸과 도착 칸 사이에 그린다 */
    private void drawBlocks(Graphics2D g, TileMap map, BoxPusher pusher, int row) {
        for (int col = 0; col < map.columns(); col++) {
            Tile tile = map.tileAt(col, row);
            if (tile.isGround() || tile == Tile.BUSH) {
                continue;
            }
            Optional<SlidingBlock> slide = pusher.slidingInto(col, row);
            int dx = slide.map(SlidingBlock::offsetX).orElse(0);
            int dy = slide.map(SlidingBlock::offsetY).orElse(0);
            drawTile(g, map, tile, col, row, col * T + dx, row * T + dy);
        }
    }

    /** row줄의 덤불. 화면을 보는 플레이어가 들어간 덤불은 반투명하게 그려 안에 든 것이 비쳐 보인다 */
    private void drawBushes(Graphics2D g, TileMap map, int row, Concealment concealment) {
        for (int col = 0; col < map.columns(); col++) {
            if (map.tileAt(col, row) != Tile.BUSH) {
                continue;
            }
            if (concealment.isRevealedBush(col, row)) {
                Composite composite = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, REVEALED_BUSH_ALPHA));
                drawTile(g, map, Tile.BUSH, col, row);
                g.setComposite(composite);
            } else {
                drawTile(g, map, Tile.BUSH, col, row);
            }
        }
    }

    /**
     * row줄의 바닥 아이템. 그림 가로 가운데를 칸 가운데에 맞추고, 칸 바닥에 그림자를 깐 채
     * {@link #ITEM_LIFT}px 떠서 위아래로 {@link #ITEM_BOB}px씩 흔들린다.
     */
    private void drawItems(Graphics2D g, GameWorld world, int row, Concealment concealment) {
        int top = itemTop(row, world.elapsedSeconds());
        for (FieldItem item : world.items().all()) {
            if (item.row() != row || concealment.hides(item.col(), item.row())) {
                continue;
            }
            int left = item.col() * T + (T - ItemType.IMAGE_SIZE) / 2;
            g.setColor(SHADOW);
            g.fillOval(item.col() * T + 8, row * T + T - 9, T - 16, 7);
            g.drawImage(items.image(item.type()), left, top, null);
        }
    }

    /** seconds초에 row줄 아이템 그림의 위쪽 끝 y(px) */
    static int itemTop(int row, double seconds) {
        int bob = (int) Math.round(ITEM_BOB * Math.sin(2 * Math.PI * seconds / ITEM_BOB_SECONDS));
        return row * T + (T - ItemType.IMAGE_SIZE) / 2 - ITEM_LIFT - bob;
    }

    /** row줄의 물줄기 칸. 늦게 터진 물줄기가 위에 온다. 숨은 덤불 칸의 물줄기는 그리지 않는다 */
    private void drawWater(Graphics2D g, BalloonField field, int row, Concealment concealment) {
        for (Explosion explosion : field.explosions()) {
            int frame = explosion.frame();
            for (WaterCell cell : explosion.cells()) {
                if (cell.row() == row && !concealment.hides(cell.col(), cell.row())) {
                    g.drawImage(balloons.water(cell, frame), cell.col() * T, row * T, null);
                }
            }
        }
    }

    /**
     * row줄의 놓인 물풍선. 몸체 가운데({@link BalloonSprites#BALLOON_CENTER_X}, {@link BalloonSprites#BALLOON_CENTER_Y})를
     * 칸 가운데에 맞춘다. 반투명 그림자는 그림에 들어 있어서 왼쪽 아래로 칸 밖까지 몇 px 깔린다.
     */
    private void drawBalloons(Graphics2D g, BalloonField field, int row, Concealment concealment) {
        for (WaterBalloon balloon : field.balloons()) {
            if (!balloon.isFlying() && balloon.row() == row && !concealment.hides(balloon.col(), balloon.row())) {
                drawBalloon(g, balloon, 0);
            }
        }
    }

    /** 몸체 가운데를 물풍선의 땅 위 자리에서 lift px 위에 맞춰 그린다 */
    private void drawBalloon(Graphics2D g, WaterBalloon balloon, int lift) {
        g.drawImage(balloons.balloon(balloon.age()),
                (int) Math.round(balloon.x()) - BalloonSprites.BALLOON_CENTER_X,
                (int) Math.round(balloon.y()) - BalloonSprites.BALLOON_CENTER_Y - lift, null);
    }

    /**
     * 던져져 날아가는 물풍선. 공중에 있으니 모든 줄을 그린 뒤 맨 위에 그린다. 땅 위 자리에 그림자를 깔고,
     * 떠 있는 높이가 높을수록 그림자를 작게 한다.
     */
    private void drawFlyingBalloons(Graphics2D g, BalloonField field, Concealment concealment) {
        for (WaterBalloon balloon : field.balloons()) {
            if (!balloon.isFlying()
                    || concealment.hides(TileMap.colAt(balloon.x()), TileMap.rowAt(balloon.y()))) {
                continue;
            }
            int x = (int) Math.round(balloon.x());
            int ground = (int) Math.round(balloon.y()) + T / 2 - 6;
            double rise = balloon.height() / WaterBalloon.FLIGHT_HEIGHT;
            int width = (int) Math.round(28 - 10 * rise);
            g.setColor(SHADOW);
            g.fillOval(x - width / 2, ground - 4, width, 7);
            drawBalloon(g, balloon, (int) Math.round(balloon.height()));
        }
    }

    /**
     * 바닥 그림과, 바닥에 붙어 있어 아무것도 가리지 않는 타일(길, 바닥 가시). 맵 칸 밖으로 나가지 않는다.
     * 블럭 밑에 깔린 바닥({@link TileMap#groundAt})도 그려 두어서, 블럭이 밀려 나가는 동안 드러나는 자리가 비지 않는다.
     */
    public void drawGround(Graphics2D g, GameWorld world) {
        TileMap map = world.map();
        for (int row = 0; row < map.rows(); row++) {
            for (int col = 0; col < map.columns(); col++) {
                BufferedImage floor = tileset.floor(col, row);
                if (floor != null) {
                    g.drawImage(floor, col * T, row * T, null);
                } else {
                    g.setColor((col + row) % 2 == 0 ? FLOOR_LIGHT : FLOOR_DARK);
                    g.fillRect(col * T, row * T, T, T);
                }
                drawTile(g, map, map.groundAt(col, row), col, row);
            }
        }
    }

    private void drawTile(Graphics2D g, TileMap map, Tile tile, int col, int row) {
        drawTile(g, map, tile, col, row, col * T, row * T);
    }

    /** (col, row) 칸의 tile 그림을 칸 왼쪽 위가 (px, py)인 자리에 그린다 */
    private void drawTile(Graphics2D g, TileMap map, Tile tile, int col, int row, int px, int py) {
        if (tile == Tile.FLOOR || !map.isTileOrigin(col, row)) {
            return;
        }
        TileSprite sprite = tileset.sprite(tile, col, row);
        if (sprite != null) {
            sprite.draw(g, px, py);
        } else {
            drawMissing(g, tile, px, py);
        }
    }

    /** 그림 파일이 아직 없는 타일. 맵을 확인할 수 있도록 기호를 적은 상자로 대신한다. */
    private void drawMissing(Graphics2D g, Tile tile, int px, int py) {
        int width = tile.width() * T;
        g.setColor(MISSING_FILL);
        g.fillRoundRect(px + 2, py + 2, width - 4, T - 4, 8, 8);
        g.setColor(MISSING_TEXT);
        g.setFont(MISSING_FONT);
        FontMetrics fm = g.getFontMetrics();
        String text = String.valueOf(tile.symbol());
        g.drawString(text, px + (width - fm.stringWidth(text)) / 2, py + (T - fm.getHeight()) / 2 + fm.getAscent());
    }

    /**
     * 발밑에 그림자를 깔고, 그림의 가로 가운데를 캐릭터 위치에 맞춰 그린다. 머리는 윗칸으로 솟는다.
     * 탈락 연출 중에는 그림자는 땅에 두고 그림만 솟았다 떨어지며, 깜빡이는 동안 숨는 차례에는 그림자째 그리지 않는다.
     */
    private void drawPlayer(Graphics2D g, Player player, PlayerGear gear, double seconds) {
        if (!isShown(player)) {
            return;
        }
        int x = (int) Math.round(player.x());
        int feet = (int) Math.round(player.y()) + FEET_BELOW_CENTER;
        int jump = jumpLiftOf(gear);

        // 뛰어 솟을수록 그림자가 작아진다
        int shadow = 28 - 12 * jump / JUMP_HEIGHT;
        g.setColor(SHADOW);
        g.fillOval(x - shadow / 2, feet - 6, shadow, 9);

        boolean riding = gear.hasTank() && isUpright(player);
        if (!isBlinkedOut(gear, seconds)) {
            BufferedImage frame = frameOf(player);
            int lift = liftOf(player) + jump + (riding ? TANK_SEAT : 0);
            g.drawImage(frame, x - CharacterSprites.FRAME_SIZE / 2, feet - CharacterSprites.FEET_Y - lift, null);
        }
        if (riding) {
            g.drawImage(items.image(ItemType.TANK), x - TANK_SIZE / 2, feet + TANK_BELOW_FEET - TANK_SIZE - jump,
                    TANK_SIZE, TANK_SIZE, null);
        }
        if (gear.isShielded() && isShieldVisible(gear, seconds)) {
            drawShield(g, x, feet - SHIELD_CENTER_ABOVE_FEET - jump - (riding ? TANK_SEAT : 0));
        }
    }

    /** 반투명한 물방울 막. 테두리와 왼쪽 위 반짝임을 그려 막처럼 보이게 한다 */
    private static void drawShield(Graphics2D g, int centerX, int centerY) {
        int r = SHIELD_RADIUS;
        g.setColor(SHIELD_FILL);
        g.fillOval(centerX - r, centerY - r, 2 * r, 2 * r);
        Stroke stroke = g.getStroke();
        g.setStroke(SHIELD_STROKE);
        g.setColor(SHIELD_EDGE);
        g.drawOval(centerX - r, centerY - r, 2 * r, 2 * r);
        g.setColor(SHIELD_SHINE);
        g.drawArc(centerX - r + 6, centerY - r + 6, 2 * r - 12, 2 * r - 12, 110, 50);
        g.setStroke(stroke);
    }

    /** 탱크에 타 있는 모습으로 그릴 상태인지. 갇히거나 쓰러진 그림은 탱크 없이 그린다 */
    private static boolean isUpright(Player player) {
        return player.state() == PlayerState.NORMAL;
    }

    /** 스프링 신발로 뛰어 솟은 높이(px). 포물선을 그리고, 공중이 아니면 0이다 */
    static int jumpLiftOf(PlayerGear gear) {
        if (!gear.isAirborne()) {
            return 0;
        }
        double t = gear.jumpProgress();
        return (int) Math.round(JUMP_HEIGHT * 4 * t * (1 - t));
    }

    /** 탱크가 막 부서져 맞지 않는 동안, {@link #BLINK_SECONDS}초씩 숨는 차례인지 */
    static boolean isBlinkedOut(PlayerGear gear, double seconds) {
        return gear.isRecoveringFromTank() && (int) Math.floor(seconds / BLINK_SECONDS + FRAME_BOUNDARY_SLACK) % 2 == 1;
    }

    /** 보호막을 보이는 차례인지. 끝나기 {@link #SHIELD_WARNING_SECONDS}초 전부터 {@link #BLINK_SECONDS}초씩 깜빡인다 */
    static boolean isShieldVisible(PlayerGear gear, double seconds) {
        return gear.shieldSecondsLeft() > SHIELD_WARNING_SECONDS
                || (int) Math.floor(seconds / BLINK_SECONDS + FRAME_BOUNDARY_SLACK) % 2 == 0;
    }

    private BufferedImage frameOf(Player player) {
        PlayerProfile profile = player.profile();
        double seconds = player.stateTime();
        return switch (player.state()) {
            case NORMAL -> characters.frame(profile.mascot(), profile.color(), player.facing(), player.isMoving(),
                    player.walkTime());
            case TRAPPED -> characters.trapped(profile.mascot(), profile.color(), seconds);
            case ESCAPING -> characters.escaping(profile.mascot(), profile.color(), seconds);
            case DYING, GONE -> characters.dying(profile.mascot(), profile.color(), seconds);
        };
    }

    /** 이번 장면에 캐릭터를 그리는지. 사라졌거나, 탈락 연출 끝에 깜빡이며 숨는 차례면 그리지 않는다 */
    static boolean isShown(Player player) {
        return switch (player.state()) {
            case GONE -> false;
            case DYING -> {
                double blinking = player.stateTime() - Player.DYING_JUMP_SECONDS;
                yield blinking < 0 || (int) Math.floor(blinking / BLINK_SECONDS + FRAME_BOUNDARY_SLACK) % 2 == 0;
            }
            default -> true;
        };
    }

    /** 탈락 연출로 그림이 땅에서 솟은 높이(px). 솟는 동안 포물선을 그리고, 그 밖에는 0이다 */
    static int liftOf(Player player) {
        double t = player.stateTime() / Player.DYING_JUMP_SECONDS;
        if (player.state() != PlayerState.DYING || t >= 1) {
            return 0;
        }
        return (int) Math.round(DYING_JUMP_HEIGHT * 4 * t * (1 - t));
    }
}

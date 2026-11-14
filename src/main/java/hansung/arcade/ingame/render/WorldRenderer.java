package hansung.arcade.ingame.render;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 맵은 테마 타일셋의 BMP 그림으로, 캐릭터는 아직 Java2D 도형으로 그린다.
 * 바닥 그림이 없는 테마는 단색 바닥을, 그림이 없는 타일은 기호를 적은 임시 상자를 그린다.
 * 호출 전에 Graphics2D의 원점이 맵 왼쪽 위로 옮겨져 있어야 한다.
 *
 * <p>바닥({@link #drawGround})과 그 위 물체({@link #drawObjects})를 따로 그린다. 블록·나무·집 그림은 칸보다 키가 커서
 * 맨 윗줄에서는 맵 위로 솟으므로, 화면은 바닥만 맵 영역 안으로 자르고 물체는 맵 테두리 위에 겹쳐 그릴 수 있다.</p>
 */
public final class WorldRenderer {

    private static final int T = TileMap.TILE_SIZE;

    private static final Color FLOOR_LIGHT = new Color(0x8BC662);
    private static final Color FLOOR_DARK = new Color(0x7FBA56);
    private static final Color MISSING_FILL = new Color(0x5C626B);
    private static final Color MISSING_TEXT = new Color(0xFFE066);
    private static final Font MISSING_FONT = new Font(Font.MONOSPACED, Font.BOLD, 16);

    private static final Set<Tile> GROUND_LEVEL = EnumSet.of(Tile.ROAD, Tile.ROAD_LANE, Tile.CROSSWALK, Tile.SPIKE);

    private static final Color SHADOW = new Color(0, 0, 0, 55);
    private static final Color[] PLAYER_COLORS = {
            new Color(0xE84A5F), new Color(0x3D8BFD), new Color(0xF2B33D), new Color(0x8E5BD9),
            new Color(0x2BB673), new Color(0xF27A3D), new Color(0x35C2C9), new Color(0xE05BB5),
    };
    private static final Font NAME_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 10);
    private static final Stroke BODY_OUTLINE = new BasicStroke(1.5f);

    private final Tileset tileset;

    public WorldRenderer(Tileset tileset) {
        this.tileset = Objects.requireNonNull(tileset);
    }

    public static Color playerColor(int playerId) {
        return PLAYER_COLORS[Math.floorMod(playerId - 1, PLAYER_COLORS.length)];
    }

    /** 블록·덤불 등 물체와 캐릭터. 바닥을 먼저 그린 뒤에 부른다. */
    public void drawObjects(Graphics2D g, GameWorld world) {
        TileMap map = world.map();
        // 위쪽 줄부터 그려서 아래쪽 물체가 앞에 보이게 한다.
        // 덤불은 같은 줄 캐릭터보다 나중에 그려서 안에 들어간 캐릭터를 가린다.
        List<Player> players = world.players();
        for (int row = 0; row < map.rows(); row++) {
            drawRow(g, map, row, false);
            for (Player player : players) {
                if (player.row() == row) {
                    drawPlayer(g, player);
                }
            }
            drawRow(g, map, row, true);
        }
    }

    /** 바닥 그림과, 바닥에 붙어 있어 아무것도 가리지 않는 타일(길, 바닥 가시). 맵 칸 밖으로 나가지 않는다. */
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
                if (isGroundLevel(map.tileAt(col, row))) {
                    drawTile(g, map, col, row);
                }
            }
        }
    }

    private void drawRow(Graphics2D g, TileMap map, int row, boolean overPlayers) {
        for (int col = 0; col < map.columns(); col++) {
            Tile tile = map.tileAt(col, row);
            if (!isGroundLevel(tile) && isOverPlayers(tile) == overPlayers) {
                drawTile(g, map, col, row);
            }
        }
    }

    private void drawTile(Graphics2D g, TileMap map, int col, int row) {
        Tile tile = map.tileAt(col, row);
        if (tile == Tile.FLOOR || !map.isTileOrigin(col, row)) {
            return;
        }
        TileSprite sprite = tileset.sprite(tile, col, row);
        if (sprite != null) {
            sprite.draw(g, col * T, row * T);
        } else {
            drawMissing(g, tile, col * T, row * T);
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

    private static boolean isGroundLevel(Tile tile) {
        return GROUND_LEVEL.contains(tile);
    }

    private static boolean isOverPlayers(Tile tile) {
        return tile == Tile.BUSH;
    }

    private void drawPlayer(Graphics2D g, Player player) {
        double x = player.x();
        double y = player.y();
        double bob = player.isMoving() ? Math.abs(Math.sin(player.walkTime() * 14)) * 3 : 0;
        Color color = playerColor(player.id());

        g.setColor(SHADOW);
        g.fill(new Ellipse2D.Double(x - 13, y + 9, 26, 9));

        // 발: 걸을 때 번갈아 움직인다
        double step = player.isMoving() ? Math.sin(player.walkTime() * 14) * 3 : 0;
        g.setColor(color.darker().darker());
        g.fill(new Ellipse2D.Double(x - 11, y + 7 + step, 9, 6));
        g.fill(new Ellipse2D.Double(x + 2, y + 7 - step, 9, 6));

        double bodyY = y - 20 - bob;
        Shape body = new Ellipse2D.Double(x - 15, bodyY, 30, 30);
        g.setColor(color);
        g.fill(body);
        g.setColor(new Color(255, 255, 255, 90));
        g.fill(new Ellipse2D.Double(x - 10, bodyY + 4, 9, 7));
        g.setColor(color.darker().darker());
        // 선 굵기를 되돌려 놓지 않으면 뒤에 그리는 HUD 테두리까지 굵어진다.
        Stroke previous = g.getStroke();
        g.setStroke(BODY_OUTLINE);
        g.draw(body);
        g.setStroke(previous);

        drawFace(g, player.facing(), x, bodyY + 15);
        drawNameTag(g, "P" + player.id(), x, bodyY - 4, color);
    }

    private void drawFace(Graphics2D g, Direction facing, double cx, double cy) {
        if (facing == Direction.UP) {
            return; // 뒷모습
        }
        double shift = switch (facing) {
            case LEFT -> -5;
            case RIGHT -> 5;
            default -> 0;
        };
        double spread = facing == Direction.DOWN ? 5.5 : 4;
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx + shift - spread - 3.5, cy - 4, 7, 9));
        g.fill(new Ellipse2D.Double(cx + shift + spread - 3.5, cy - 4, 7, 9));
        g.setColor(new Color(0x222222));
        double pupil = shift / 3;
        g.fill(new Ellipse2D.Double(cx + shift - spread - 2 + pupil, cy - 1, 4, 5));
        g.fill(new Ellipse2D.Double(cx + shift + spread - 2 + pupil, cy - 1, 4, 5));
    }

    private void drawNameTag(Graphics2D g, String text, double cx, double bottom, Color color) {
        g.setFont(NAME_FONT);
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 8;
        int h = fm.getHeight();
        int left = (int) Math.round(cx - w / 2.0);
        int top = (int) Math.round(bottom - h);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(left, top, w, h, 8, 8);
        g.setColor(color.brighter());
        g.drawString(text, left + 4, top + fm.getAscent());
    }
}

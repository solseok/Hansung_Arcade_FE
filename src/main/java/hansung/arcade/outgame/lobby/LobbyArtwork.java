package hansung.arcade.outgame.lobby;

import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.Tileset;
import hansung.arcade.ingame.render.TileSprite;
import hansung.arcade.outgame.GameMaps;
import hansung.arcade.outgame.ui.GlossButton;
import hansung.arcade.outgame.ui.OutgameFonts;
import hansung.arcade.outgame.ui.Shapes;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** 기존 게임 타일을 사용하는 로비 맵 그림과 버튼. EDT에서 사용한다. */
public final class LobbyArtwork {
    private static final Map<String, BufferedImage> MAPS = new HashMap<>();

    private LobbyArtwork() { }

    private static BufferedImage buttonArt(String text) {
        String file = switch (text) {
            case "방만들기" -> "create-room";
            case "바로시작" -> "quick-join";
            default -> null;
        };
        if (file == null) {
            return null;
        }
        try (var input = LobbyArtwork.class.getResourceAsStream("/ui/" + file + ".png")) {
            if (input == null) {
                throw new IllegalStateException("버튼 이미지가 없습니다: " + file);
            }
            return javax.imageio.ImageIO.read(input);
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    /** 맵 화면 이름(캠프 08 등)의 맵 전체 그림 (타일 원본 크기, 1칸 40px). 랜덤 맵은 물음표 그림 */
    static BufferedImage map(String name) {
        return MAPS.computeIfAbsent(name, key -> {
            GameMaps.GameMap game = GameMaps.byName(key);
            if (game.isRandom()) {
                return randomArt();
            }
            TileMap map = MapLoader.loadResource(game.id());
            Tileset tiles = Tileset.forTheme(map.theme());
            BufferedImage image = new BufferedImage(map.pixelWidth(), map.pixelHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(new Color(0x87B44B));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            int size = TileMap.TILE_SIZE;
            for (int y = 0; y < map.rows(); y++) {
                for (int x = 0; x < map.columns(); x++) {
                    g.drawImage(tiles.floor(x, y), x * size, y * size, null);
                }
            }
            for (int y = 0; y < map.rows(); y++) {
                for (int x = 0; x < map.columns(); x++) {
                    TileSprite sprite = tiles.sprite(map.tileAt(x, y), x, y);
                    if (sprite != null && map.isTileOrigin(x, y)) {
                        g.drawImage(sprite.image(), x * size + sprite.offsetX(), y * size + sprite.offsetY(), null);
                    }
                }
            }
            g.dispose();
            return image;
        });
    }

    /** 랜덤 맵 그림: 파란 바탕에 큰 물음표와 그림자 (참고 화면 6번의 "?" 미리보기) */
    private static BufferedImage randomArt() {
        int w = 240;
        int h = 240;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Shapes.hints(g);
        g.setPaint(new GradientPaint(0, 0, new Color(60, 120, 220), 0, h, new Color(30, 70, 170)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(20, 50, 130));
        g.fillOval(50, 168, 140, 34);
        Font font = OutgameFonts.label(170);
        Shapes.text(g, "?", font, new Color(170, 230, 255), new Color(60, 150, 240), new Color(25, 70, 160), 4, null,
                0, 0, w, h - 20, javax.swing.SwingConstants.CENTER);
        g.dispose();
        return image;
    }

    /** 맵 최대 인원 (랜덤은 가장 큰 맵) */
    static int maxPlayers(String name) {
        return GameMaps.byName(name).maxPlayers();
    }

    /** 광택 버튼. 방만들기·바로시작은 그림 버튼이고, gold면 금색, 아니면 파란색이다. */
    public static JButton button(String text, boolean gold) {
        GlossButton button = new GlossButton(text, gold ? GlossButton.GOLD : GlossButton.BLUE, buttonArt(text));
        button.setFont(OutgameFonts.label(gold ? 18 : 13));
        return button;
    }
}

package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.map.Tile;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.TileSprite;
import hansung.arcade.ingame.render.Tileset;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hansung.arcade.ingame.hud.HudLayout.MAP_ICON_SIZE;
import static hansung.arcade.ingame.hud.HudLayout.MAP_LABEL;
import static hansung.arcade.ingame.hud.HudLayout.MAP_LABEL_PADDING;

/**
 * 맵 프레임 위 이름표. 전광판 바탕에 그 맵의 대표 블록 그림과 맵 이름 끝 숫자 두 자리를 넣는다.
 * 맨 윗줄의 키 큰 그림이 프레임 위로 솟아도 가려지지 않게 맨 위 층에 그린다.
 */
final class MapLabel implements HudPart {

    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)\\s*$");

    private final TextArt digits;
    private final String number;
    private final BufferedImage icon;

    MapLabel(HudSkin skin, TileMap map, Tileset tileset) {
        this.digits = skin.digits();
        this.number = numberOf(map.name());
        TileSprite sprite = representativeBlock(map, tileset);
        this.icon = sprite == null ? null : PixelImages.shrinkToFit(sprite.image(), MAP_ICON_SIZE, MAP_ICON_SIZE);
    }

    /** 맵 이름 끝의 숫자 두 자리. "빌리지 10" → "10", "캠프 8" → "08". 숫자가 없으면 빈 문자열. */
    static String numberOf(String mapName) {
        Matcher m = TRAILING_NUMBER.matcher(mapName);
        if (!m.find()) {
            return "";
        }
        String n = m.group(1);
        return n.length() >= 2 ? n.substring(n.length() - 2) : "0" + n;
    }

    /**
     * 맵을 대표하는 블록 그림. 테마의 일반 블록(B)을 쓰고, 그 그림이 없으면 맵에 가장 많이 놓인 블록을 쓴다.
     */
    static TileSprite representativeBlock(TileMap map, Tileset tileset) {
        TileSprite block = tileset.sprite(Tile.BLOCK, 0, 0);
        if (block != null) {
            return block;
        }
        Map<Tile, Integer> counts = new EnumMap<>(Tile.class);
        for (int row = 0; row < map.rows(); row++) {
            for (int col = 0; col < map.columns(); col++) {
                Tile tile = map.tileAt(col, row);
                if (tile.isSolid() && tileset.sprite(tile, col, row) != null) {
                    counts.merge(tile, 1, Integer::sum);
                }
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> tileset.sprite(e.getKey(), 0, 0))
                .orElse(null);
    }

    @Override
    public void paintOverlay(PixelCanvas canvas) {
        canvas.fillOutlined(MAP_LABEL, Corners.all(HudLayout.MAP_LABEL_RADIUS), HudLayout.EDGE,
                HudPalette.edgeOf(HudPalette.DISPLAY), HudPalette.displayGrid(MAP_LABEL.x(), MAP_LABEL.y()));

        Box iconBox = MAP_LABEL.child(MAP_LABEL_PADDING, (MAP_LABEL.height() - MAP_ICON_SIZE) / 2,
                MAP_ICON_SIZE, MAP_ICON_SIZE);
        Box numberBox = new Box(iconBox.right(), MAP_LABEL.y(),
                MAP_LABEL.right() - MAP_LABEL_PADDING - iconBox.right(), MAP_LABEL.height());
        Graphics2D g = canvas.graphics();
        try {
            if (icon != null) {
                g.drawImage(icon, iconBox.x() + (iconBox.width() - icon.getWidth()) / 2,
                        iconBox.y() + (iconBox.height() - icon.getHeight()) / 2, null);
            }
            digits.draw(g, number, numberBox, TextArt.Align.CENTER);
        } finally {
            g.dispose();
        }
    }
}

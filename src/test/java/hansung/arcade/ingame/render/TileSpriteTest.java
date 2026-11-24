package hansung.arcade.ingame.render;

import hansung.arcade.ingame.map.TileMap;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TileSpriteTest {

    private static final int T = TileMap.TILE_SIZE;
    private static final int RED = 0xFFC82828;

    @Test
    void 여백이_있는_그림은_잘라서_칸_너비를_채우고_칸_바닥에_세운다() {
        // 40 x 60 프레임 안에 왼쪽 2px, 오른쪽 3px, 아래 5px 투명 여백이 있는 그림
        BufferedImage frame = frame(40, 60, 2, 0, 35, 55);

        TileSprite sprite = TileSprite.fitted(frame, 1);

        assertEquals(T, sprite.image().getWidth());
        assertEquals(0, sprite.offsetX());
        assertEquals(T, sprite.offsetY() + sprite.image().getHeight());
        // 비율을 지켜 키운다: 35 x 55 → 40 x 63
        assertEquals(Math.round(55 * (40 / 35.0)), sprite.image().getHeight());
        // 가장자리 픽셀까지 채워져 틈이 없다
        int bottom = sprite.image().getHeight() - 1;
        assertEquals(RED, sprite.image().getRGB(0, bottom));
        assertEquals(RED, sprite.image().getRGB(T - 1, bottom));
    }

    @Test
    void 여백이_없는_그림은_그대로_세운다() {
        BufferedImage frame = frame(40, 46, 0, 0, 40, 46);

        TileSprite sprite = TileSprite.fitted(frame, 1);

        assertEquals(frame.getRGB(0, 0), sprite.image().getRGB(0, 0));
        assertEquals(46, sprite.image().getHeight());
        assertEquals(T - 46, sprite.offsetY());
    }

    @Test
    void 두_칸짜리는_두_칸_너비를_채운다() {
        BufferedImage frame = frame(80, 68, 1, 2, 78, 64);

        TileSprite sprite = TileSprite.fitted(frame, 2);

        assertEquals(2 * T, sprite.image().getWidth());
        assertEquals(T, sprite.offsetY() + sprite.image().getHeight());
    }

    /** width x height 투명 그림의 (x, y)부터 inkWidth x inkHeight 를 빨갛게 칠한 것 */
    private static BufferedImage frame(int width, int height, int x, int y, int inkWidth, int inkHeight) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(RED, true));
        g.fillRect(x, y, inkWidth, inkHeight);
        g.dispose();
        return image;
    }
}

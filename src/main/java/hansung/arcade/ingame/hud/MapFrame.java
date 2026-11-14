package hansung.arcade.ingame.hud;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import static hansung.arcade.ingame.hud.HudLayout.FIELD;
import static hansung.arcade.ingame.hud.HudLayout.FRAME;
import static hansung.arcade.ingame.hud.HudLayout.FRAME_OUTER_EDGE;
import static hansung.arcade.ingame.hud.HudLayout.FRAME_RADIUS;

/**
 * 맵 둘레 테두리. 바깥 검은 띠(#000206) 안에 회색 띠(#68616F)가 있고 모서리는 같은 중심으로 둥글다.
 *
 * <p>맵 바닥은 사각형으로 잘라 그리므로, 맵 영역 네 귀퉁이의 둥근 부분은 바닥을 그린 뒤
 * {@link #paintFieldCorners}로 회색을 다시 덮는다. 블록·캐릭터는 그 다음에 그려서 테두리 위에 겹칠 수 있다.</p>
 */
final class MapFrame implements HudPart {

    private static final Corners OUTER = Corners.all(FRAME_RADIUS);
    private static final Box INNER_BAND = FRAME.inset(FRAME_OUTER_EDGE);
    private static final Corners INNER_BAND_CORNERS = OUTER.shrink(FRAME_OUTER_EDGE);
    private static final Corners FIELD_CORNERS = INNER_BAND_CORNERS.shrink(HudLayout.FRAME_INNER_EDGE);

    /** 맵 영역 안에서 둥근 귀퉁이로 깎여 회색으로 덮어야 하는 픽셀들 (귀퉁이마다 몇 개뿐이다) */
    private final List<CornerPixel> fieldCorners = cornerPixels();

    @Override
    public void paintBase(PixelCanvas canvas) {
        canvas.fill(FRAME, OUTER, HudPalette.FRAME_OUTER);
        canvas.fill(INNER_BAND, INNER_BAND_CORNERS, HudPalette.FRAME_INNER);
        // 맵이 15 x 13칸보다 작을 때 빈 곳에 보이는 바닥
        canvas.fill(FIELD, FIELD_CORNERS, HudPalette.FRAME_OUTER);
    }

    /** 맵 바닥을 그린 직후, 물체를 그리기 전에 부른다. 바꾼 색은 되돌려 뒤에 그리는 쪽에 영향을 주지 않는다. */
    void paintFieldCorners(Graphics2D g) {
        Color previous = g.getColor();
        for (CornerPixel pixel : fieldCorners) {
            g.setColor(pixel.color());
            g.fillRect(pixel.x(), pixel.y(), 1, 1);
        }
        g.setColor(previous);
    }

    private static List<CornerPixel> cornerPixels() {
        List<CornerPixel> pixels = new ArrayList<>();
        Color gray = HudPalette.FRAME_INNER;
        for (int y = FIELD.y(); y < FIELD.bottom(); y++) {
            for (int x = FIELD.x(); x < FIELD.right(); x++) {
                double alpha = PixelCanvas.quantize(1 - PixelCanvas.coverage(FIELD, FIELD_CORNERS, x, y));
                if (alpha > 0) {
                    pixels.add(new CornerPixel(x, y, new Color(gray.getRed(), gray.getGreen(), gray.getBlue(),
                            (int) Math.round(alpha * 255))));
                }
            }
        }
        return List.copyOf(pixels);
    }

    private record CornerPixel(int x, int y, Color color) {
    }
}

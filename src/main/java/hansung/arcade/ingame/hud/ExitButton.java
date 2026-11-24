package hansung.arcade.ingame.hud;

import java.awt.Color;
import java.awt.Graphics2D;

import static hansung.arcade.ingame.hud.HudLayout.EDGE;
import static hansung.arcade.ingame.hud.HudLayout.EXIT;
import static hansung.arcade.ingame.hud.HudLayout.EXIT_BUTTON;
import static hansung.arcade.ingame.hud.HudLayout.EXIT_BUTTON_RADIUS;

/**
 * 오른쪽 맨 아래 나가기 버튼. 받침 상자 안에 버튼이 있다.
 *
 * <p>버튼은 맨 위에 밝은 띠가 있고, 그 아래부터 바닥 쪽으로 갈수록 점점 밝아지는 그라데이션이다
 * (위쪽 절반은 기본색보다 어둡고 아래쪽 절반은 밝다). 테두리 바로 안쪽에는 버튼 가운데 높이의 색으로
 * 그림자가 한 줄 진다.</p>
 */
final class ExitButton implements HudPart {

    private final TextArt label;

    ExitButton(HudSkin skin) {
        this.label = skin.exitLabel();
    }

    @Override
    public void paintBase(PixelCanvas canvas) {
        canvas.fillOutlined(EXIT, Corners.all(HudLayout.SIDE_RADIUS), EDGE, HudPalette.EXIT_EDGE, HudPalette.EXIT);

        Corners corners = Corners.all(EXIT_BUTTON_RADIUS);
        Box face = EXIT_BUTTON.inset(EDGE);
        Corners faceCorners = corners.shrink(EDGE);
        // 밝은 띠 아래부터 바닥까지 한 줄마다 색이 조금씩 밝아진다
        int top = face.y() + 1 + HudLayout.EXIT_HIGHLIGHT;
        int bottom = face.bottom() - 2;
        canvas.fillOutlined(EXIT_BUTTON, corners, EDGE, HudPalette.EXIT_BUTTON_EDGE,
                (x, y) -> gradient(y, top, bottom).getRGB());
        // 테두리 안쪽 그림자: 버튼 가운데 높이의 색
        canvas.ring(face, faceCorners, face.inset(1), faceCorners.shrink(1), gradient((top + bottom) / 2, top, bottom));
        // 맨 위 밝은 띠
        Box highlight = face.inset(1).child(0, 0, face.width() - 2, HudLayout.EXIT_HIGHLIGHT);
        canvas.fill(highlight, Corners.top(Math.max(0, faceCorners.topLeft() - 1)), HudPalette.EXIT_BUTTON_HIGHLIGHT);

        Graphics2D g = canvas.graphics();
        try {
            label.draw(g, "나가기", EXIT_BUTTON, TextArt.Align.CENTER);
        } finally {
            g.dispose();
        }
    }

    /** y 줄의 버튼 색. top 줄은 가장 어둡고 bottom 줄로 갈수록 밝아진다. */
    static Color gradient(int y, int top, int bottom) {
        double t = bottom <= top ? 1 : Math.max(0, Math.min(1, (y - top) / (double) (bottom - top)));
        Color from = HudPalette.EXIT_BUTTON_DARK;
        Color to = HudPalette.EXIT_BUTTON_LIGHT;
        return new Color((int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t));
    }
}

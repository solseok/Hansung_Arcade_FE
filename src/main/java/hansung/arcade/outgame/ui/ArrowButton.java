package hansung.arcade.outgame.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * 글자 없이 ◁ · ▷ 삼각형만 그리는 쪽 넘기기 버튼 (참고 화면 6번). 글자({@link #getText})는 보조 기술이 읽는 이름으로만 쓴다.
 * 바탕과 광택은 좌우 대칭이라, 왼쪽 버튼과 오른쪽 버튼은 서로 거울에 비친 모양이다.
 */
public class ArrowButton extends GlossButton {

    public enum Direction {
        LEFT, RIGHT
    }

    private final Direction direction;

    public ArrowButton(String name, Direction direction) {
        super(name, GlossButton.PAGER);
        this.direction = direction;
        getAccessibleContext().setAccessibleName(name);
    }

    public Direction direction() {
        return direction;
    }

    @Override
    protected void paintContent(Graphics2D g, int w, int h) {
        double cx = w / 2.0;
        double cy = h / 2.0 + (pressed() ? 1 : 0);
        double half = Math.min(h * 0.28, 7);
        double depth = half * 1.15;
        double tip = direction == Direction.LEFT ? cx - depth / 2 : cx + depth / 2;
        double base = direction == Direction.LEFT ? cx + depth / 2 : cx - depth / 2;
        Path2D triangle = new Path2D.Double();
        triangle.moveTo(base, cy - half);
        triangle.lineTo(tip, cy);
        triangle.lineTo(base, cy + half);
        triangle.closePath();
        g.setColor(isEnabled() ? OutgamePalette.PAGER_ARROW : OutgamePalette.mix(OutgamePalette.PAGER_ARROW, Color.GRAY, 0.5));
        g.fill(triangle);
        g.setColor(isEnabled() ? OutgamePalette.PAGER_ARROW_EDGE : Color.GRAY);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(triangle);
    }
}

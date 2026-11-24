package hansung.arcade.outgame.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;

/**
 * 매끈하게(안티앨리어싱) 그리는 라벨. 둥근 바탕({@link #setFill}), 바탕 테두리·광택, 글자 테두리({@link #setOutline}),
 * 글자 위아래 그러데이션({@link #setGradient}), 그림자를 줄 수 있다. 기본 글꼴은 굵은 게임 글꼴이다.
 * 글자는 세로 가운데, 가로는 {@link #getHorizontalAlignment}를 따른다. 아이콘은 쓰지 않는다 (그림은 일반 JLabel로).
 */
public class TextLabel extends JLabel {

    private Color fill;
    private Color fillBottom;
    private int radius;
    private Color edge;
    private float edgeWidth;
    private float gloss;
    private Color outline;
    private int outlineWidth;
    /** 칸을 넘는 글자를 "…"로 줄일지. 방 제목처럼 길이가 정해지지 않은 글자에 켠다 */
    private boolean ellipsis;
    private Color textBottom;
    private Color shadow;
    private BufferedImage background;

    public TextLabel() {
        this("");
    }

    public TextLabel(String text) {
        this(text, SwingConstants.LEFT);
    }

    public TextLabel(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
        setFont(OutgameFonts.label(12));
        setOpaque(false);
    }

    /** 반지름 radius로 모서리를 깎은 fill 바탕을 깐다. */
    public void setFill(Color fill, int radius) {
        this.fill = fill;
        this.radius = radius;
        setBackground(fill);
        invalidateBackground();
    }

    /** 바탕을 위(fill) → 아래(bottom) 그러데이션으로 */
    public void setFillBottom(Color bottom) {
        this.fillBottom = bottom;
        invalidateBackground();
    }

    /** 바탕 안쪽 가장자리에 thickness 두께 테두리 */
    public void setEdge(Color edge, float thickness) {
        this.edge = edge;
        this.edgeWidth = thickness;
        invalidateBackground();
    }

    /** 바탕 위쪽 절반에 흰 광택 (strength: 가장 밝은 곳의 흰색 알파, 0~1) */
    public void setGloss(float strength) {
        this.gloss = strength;
        invalidateBackground();
    }

    public int radius() {
        return radius;
    }

    /** 글자 둘레에 width px 테두리. 없애려면 null */
    public void setOutline(Color outline, int width) {
        this.outline = outline;
        this.outlineWidth = outline == null ? 0 : width;
        repaint();
    }

    public Color outline() {
        return outline;
    }

    public int outlineWidth() {
        return outlineWidth;
    }

    /** 글자를 위(전경색) → 아래(bottom) 그러데이션으로 칠한다 */
    public void setGradient(Color bottom) {
        this.textBottom = bottom;
        repaint();
    }

    /** 글자 1px 아래에 깔 그림자 색. 없으면 null */
    public void setShadow(Color shadow) {
        this.shadow = shadow;
        repaint();
    }

    private void invalidateBackground() {
        background = null;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        Insets insets = getInsets();
        int[] size = Shapes.textSize(getText() == null ? "" : getText(), getFont(), outlineWidth);
        return new Dimension(size[0] + insets.left + insets.right, size[1] + insets.top + insets.bottom);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            Shapes.hints(g);
            paintBackground(g, w, h);
            paintText(g, w, h);
        } finally {
            g.dispose();
        }
    }

    /** 둥근 바탕(있으면) */
    protected void paintBackground(Graphics2D g, int w, int h) {
        if (fill == null) {
            return;
        }
        if (background == null || background.getWidth() != w || background.getHeight() != h) {
            background = Shapes.paint(w, h, m -> m.fill(Shapes.round(0, 0, w, h, radius)),
                    fillBottom == null ? fill : Shapes.vertical(h, fill, fillBottom), d -> {
                        if (gloss > 0) {
                            Shapes.gloss(d, w, h, gloss);
                        }
                        Shapes.edge(d, w, h, radius, edge, edgeWidth);
                    });
        }
        g.drawImage(background, 0, 0, null);
    }

    /** 글자 */
    protected void paintText(Graphics2D g, int w, int h) {
        Insets in = getInsets();
        Color color = isEnabled() ? getForeground() : OutgamePalette.DISABLED_TEXT;
        int width = w - in.left - in.right;
        String text = ellipsis
                ? Shapes.fit(getText(), getFont(), g.getFontRenderContext(), width - 2f * outlineWidth)
                : getText();
        Shapes.text(g, text, getFont(), color, isEnabled() ? textBottom : null, outline, outlineWidth, shadow,
                in.left, in.top, width, h - in.top - in.bottom, getHorizontalAlignment());
    }

    /** 칸을 넘는 글자를 뒤에서 줄이고 "…"를 붙여 그린다. 글자 자체(getText)는 그대로 둔다 */
    public TextLabel ellipsis() {
        ellipsis = true;
        repaint();
        return this;
    }
}

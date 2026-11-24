package hansung.arcade.outgame.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 모서리를 매끈하게(안티앨리어싱) 깎은 바탕 판. 레이아웃 관리자가 없어서 자식은 setBounds로 놓는다.
 * 바탕색은 {@link #getBackground()}이고, 테두리·위아래 그러데이션·위쪽 안테두리 빛·광택을 더할 수 있다.
 * 모양을 바꾸려면 {@link #fillShape}를 고쳐 쓴다.
 */
public class RoundPanel extends JPanel {

    private final int radius;
    private Color bottom;
    private Color edge;
    private float edgeWidth;
    private Color shine;
    private int shineWidth;
    private float gloss;
    private BufferedImage cached;

    public RoundPanel(Color fill, int radius) {
        super(null);
        this.radius = radius;
        setBackground(fill);
        setOpaque(false);
    }

    /** 안쪽 가장자리에 width px 테두리 */
    public RoundPanel edge(Color edge, float width) {
        this.edge = edge;
        this.edgeWidth = width;
        cached = null;
        return this;
    }

    /** 바탕을 위(배경색) → 아래(bottom) 그러데이션으로 */
    public RoundPanel gradient(Color bottom) {
        this.bottom = bottom;
        cached = null;
        return this;
    }

    /** 위쪽·왼쪽 안테두리를 light 색으로 width px 빛나게 */
    public RoundPanel shine(Color light, int width) {
        this.shine = light;
        this.shineWidth = width;
        cached = null;
        return this;
    }

    /** 위쪽 절반에 흰 광택 */
    public RoundPanel gloss(float strength) {
        this.gloss = strength;
        cached = null;
        return this;
    }

    public int radius() {
        return radius;
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        cached = null;
    }

    /** 판 모양을 불투명한 색으로 칠한다. 칠한 알파가 판의 알파가 된다 */
    protected void fillShape(Graphics2D g, int width, int height) {
        g.fill(Shapes.round(0, 0, width, height, radius));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (cached == null || cached.getWidth() != w || cached.getHeight() != h) {
            cached = Shapes.paint(w, h, m -> fillShape(m, w, h),
                    bottom == null ? getBackground() : Shapes.vertical(h, getBackground(), bottom), d -> {
                        if (shine != null) {
                            Shapes.shine(d, w, h, shine, shineWidth);
                        }
                        if (gloss > 0) {
                            Shapes.gloss(d, w, h, gloss);
                        }
                        Shapes.edge(d, w, h, radius, edge, edgeWidth);
                    });
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.drawImage(cached, 0, 0, null);
        } finally {
            g.dispose();
        }
    }
}

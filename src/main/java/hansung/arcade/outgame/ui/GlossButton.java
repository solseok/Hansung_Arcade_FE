package hansung.arcade.outgame.ui;

import javax.swing.JButton;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 광택 버튼: 테두리를 두른 둥근 그러데이션 바탕 + 위쪽 절반 광택 + 테두리 두른 굵은 글자.
 * 그림(art)을 주면 둥근 모서리 밖을 지운 그림을 그린다. 마우스를 올리면 밝아지고, 누르는 동안 어두워지며 1px 내려간다.
 */
public class GlossButton extends JButton {

    /**
     * 버튼 모양
     *
     * @param textOutline 글자 테두리 색. 없으면 null
     * @param gloss       위쪽 광택 세기(흰색 알파 0~1)
     */
    public record Style(Color top, Color bottom, Color edge, float edgeWidth, int radius, Color text,
                        Color textOutline, float gloss) {
    }

    public static final Style BLUE = new Style(OutgamePalette.BUTTON_BLUE_TOP, OutgamePalette.BUTTON_BLUE_BOTTOM,
            OutgamePalette.BUTTON_BLUE_EDGE, 1.5f, 5, Color.WHITE, OutgamePalette.BUTTON_BLUE_EDGE, 0.45f);
    public static final Style GOLD = new Style(OutgamePalette.BUTTON_GOLD_TOP, OutgamePalette.BUTTON_GOLD_BOTTOM,
            OutgamePalette.BUTTON_GOLD_EDGE, 1.5f, 7, Color.WHITE, OutgamePalette.BUTTON_GOLD_EDGE, 0.5f);
    public static final Style READY = new Style(OutgamePalette.READY_TOP, OutgamePalette.READY_BOTTOM,
            OutgamePalette.READY_EDGE, 2.5f, 9, Color.WHITE, OutgamePalette.READY_TEXT_SHADOW, 0.4f);
    public static final Style MAP_SELECT = new Style(OutgamePalette.MAP_SELECT_BOTTOM, OutgamePalette.MAP_SELECT,
            OutgamePalette.WAITING_SECTION, 1.5f, 6, Color.WHITE, OutgamePalette.MAP_SELECT_TEXT, 0.45f);
    /** 쪽 넘기기 ◁ ▷ */
    public static final Style PAGER = new Style(OutgamePalette.PAGER_TOP, OutgamePalette.PAGER_BOTTOM,
            OutgamePalette.PAGER_EDGE, 1.5f, 4, Color.WHITE, null, 0.45f);

    /** 버튼 그림(방만들기·바로시작)의 둥근 모서리 반지름 */
    private static final int ART_RADIUS = 6;

    private final Style style;
    private final BufferedImage art;

    public GlossButton(String text, Style style) {
        this(text, style, null);
    }

    /** @param art 버튼 그림. 하늘색 바탕에서 잘라낸 그림이면 모서리 밖을 지워서 쓴다 */
    public GlossButton(String text, Style style, BufferedImage art) {
        super(text);
        this.style = style;
        this.art = art == null ? null : Shapes.cutCorners(art, ART_RADIUS);
        setFont(OutgameFonts.label(13));
        setForeground(style.text());
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
    }

    public Style style() {
        return style;
    }

    protected boolean pressed() {
        return getModel().isArmed() && getModel().isPressed();
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
            BufferedImage body = art != null ? artBody(w, h) : body(w, h);
            g.drawImage(body, 0, pressed() ? 1 : 0, null);
            paintContent(g, w, h);
        } finally {
            g.dispose();
        }
    }

    /** 바탕 위에 올리는 내용. 기본은 테두리 두른 글자 */
    protected void paintContent(Graphics2D g, int w, int h) {
        if (art != null) {
            return;
        }
        Color text = isEnabled() ? style.text() : OutgamePalette.mix(style.text(), Color.GRAY, 0.5);
        Shapes.text(g, getText(), getFont(), text, isEnabled() ? style.textOutline() : null, 1, null,
                0, pressed() ? 1 : 0, w, h, CENTER);
        if (isFocusOwner()) {
            g.setColor(new Color(255, 255, 255, 180));
            g.draw(Shapes.round(3.5, 3.5, w - 7, h - 7, Math.max(0, style.radius() - 3)));
        }
    }

    private BufferedImage body(int w, int h) {
        Color top = style.top();
        Color bottom = style.bottom();
        if (!isEnabled()) {
            top = OutgamePalette.mix(top, Color.GRAY, 0.55);
            bottom = OutgamePalette.mix(bottom, Color.GRAY, 0.55);
        } else if (pressed()) {
            top = OutgamePalette.mix(top, Color.BLACK, 0.2);
            bottom = OutgamePalette.mix(bottom, Color.BLACK, 0.2);
        } else if (getModel().isRollover()) {
            top = OutgamePalette.mix(top, Color.WHITE, 0.18);
            bottom = OutgamePalette.mix(bottom, Color.WHITE, 0.12);
        }
        Color finalTop = top;
        Color finalBottom = bottom;
        return Shapes.paint(w, h, m -> m.fill(Shapes.round(0, 0, w, h, style.radius())),
                Shapes.vertical(h, finalTop, finalBottom), d -> {
                    if (style.gloss() > 0) {
                        // 꺼진 버튼도 광택은 남기고(조금 옅게) 바탕만 회색 쪽으로 바랜다
                        float strength = !isEnabled() ? style.gloss() * 0.7f : pressed() ? style.gloss() * 0.5f : style.gloss();
                        Shapes.gloss(d, w, h, strength);
                    }
                    Shapes.edge(d, w, h, style.radius(), style.edge(), style.edgeWidth());
                });
    }

    private BufferedImage artBody(int w, int h) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            Shapes.hints(g);
            g.drawImage(art.getWidth() == w && art.getHeight() == h ? art : Shapes.smooth(art, w, h), 0, 0, null);
            g.setComposite(AlphaComposite.SrcAtop);
            if (!isEnabled() || pressed()) {
                g.setColor(new Color(0, 0, 0, 70));
                g.fillRect(0, 0, w, h);
            } else if (getModel().isRollover()) {
                g.setColor(new Color(255, 255, 255, 45));
                g.fillRect(0, 0, w, h);
            }
        } finally {
            g.dispose();
        }
        return image;
    }
}

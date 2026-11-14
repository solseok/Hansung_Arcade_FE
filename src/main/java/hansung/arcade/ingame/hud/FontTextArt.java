package hansung.arcade.ingame.hud;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 글꼴로 그리는 {@link TextArt}. 글자를 한 번 그림으로 만든 뒤 가장자리를 도트 단계로 줄여서
 * 다른 HUD 장식과 같은 픽셀 느낌을 낸다. 같은 글자는 다시 만들지 않고 그림을 재사용한다.
 *
 * <p>그림 캐시가 스레드 안전하지 않으므로 {@link #draw}는 EDT에서만 부른다.</p>
 */
public final class FontTextArt implements TextArt {

    /** 타이머처럼 매초 바뀌는 글자도 있으므로 최근 것만 남긴다 */
    private static final int CACHE_SIZE = 64;
    /** 글자가 글꼴 상자 밖으로 조금 삐져나오거나 그림자가 붙을 자리 */
    private static final int PAD = 2;

    private final Font font;
    private final Color color;
    private final Color shadow;
    private final Map<String, Rendered> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Rendered> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    public FontTextArt(Font font, Color color) {
        this(font, color, null);
    }

    /** @param shadow 1px 아래에 깔 그림자 색. 없으면 null */
    public FontTextArt(Font font, Color color, Color shadow) {
        this.font = Objects.requireNonNull(font);
        this.color = Objects.requireNonNull(color);
        this.shadow = shadow;
    }

    public Font font() {
        return font;
    }

    @Override
    public void draw(Graphics2D g, String text, Box area, Align align, VerticalAlign verticalAlign) {
        if (text.isEmpty()) {
            return;
        }
        Rendered r = cache.computeIfAbsent(text, this::render);
        int left = switch (align) {
            case LEFT -> area.x() - r.ink().x();
            case CENTER -> area.x() + (area.width() - r.ink().width()) / 2 - r.ink().x();
            case RIGHT -> area.right() - (PAD + r.advance());
        };
        int top = switch (verticalAlign) {
            case TOP -> area.y() - r.ink().y();
            case MIDDLE -> area.y() + (area.height() - r.ink().height()) / 2 - r.ink().y();
            case BOTTOM -> area.bottom() - r.ink().bottom();
        };
        g.drawImage(r.image(), left, top, null);
    }

    private Rendered render(String text) {
        FontMetrics fm = metrics();
        int advance = fm.stringWidth(text);
        int width = advance + PAD * 2;
        int height = fm.getAscent() + fm.getDescent() + PAD * 2;
        int baseline = PAD + fm.getAscent();

        BufferedImage glyphs = PixelImages.quantizeAlpha(mask(text, width, height, baseline, color));
        Box ink = PixelImages.opaqueBounds(glyphs);
        if (shadow == null) {
            return new Rendered(glyphs, ink, advance);
        }
        BufferedImage shadowGlyphs = PixelImages.quantizeAlpha(mask(text, width, height, baseline + 1, shadow));
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        g.drawImage(shadowGlyphs, 0, 0, null);
        g.drawImage(glyphs, 0, 0, null);
        g.dispose();
        return new Rendered(combined, ink, advance);
    }

    private BufferedImage mask(String text, int width, int height, int baseline, Color fill) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setFont(font);
        g.setColor(fill);
        g.drawString(text, PAD, baseline);
        g.dispose();
        return image;
    }

    private FontMetrics metrics() {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = probe.createGraphics();
        try {
            return g.getFontMetrics(font);
        } finally {
            g.dispose();
        }
    }

    /** @param ink 그림 안에서 실제 글자 픽셀이 있는 영역 (그림자 제외) */
    private record Rendered(BufferedImage image, Box ink, int advance) {
    }
}

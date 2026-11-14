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
 * 테두리를 두른 도트 글자. 배경 없이 맵 위에 바로 얹어도 읽히도록 글자 둘레 1px을 테두리색으로 감싼다.
 *
 * <p>글자는 안티앨리어싱 없이 찍어서 픽셀마다 채움색이거나 비어 있고 중간색이 없다. 돋움체처럼 작은 크기에
 * 도트 글자(비트맵)가 들어 있는 글꼴은 그 도트가 그대로 나온다.</p>
 *
 * <p>다만 macOS의 Java는 안티앨리어싱을 꺼도 도트가 없는 글자(이 글꼴 파일에서는 영문·숫자·기호)를 회색 단계로
 * 부드럽게 그린다. 그래서 그린 뒤에 {@link #OPAQUE} 이상으로 덮인 픽셀만 남겨 흑백으로 바꾼다.
 * 이런 글자는 도트로 디자인된 것이 아니라 모양이 조금 거칠 수 있다.</p>
 *
 * <p>글자마다 실제 픽셀 높이가 달라도 줄이 흔들리지 않게, 높이가 {@link #cellHeight()}인 글자 칸을 기준으로 놓는다.
 * 테두리는 칸 바깥으로 1px 나간다.</p>
 *
 * <p>그림 캐시가 스레드 안전하지 않으므로 {@link #draw}는 EDT에서만 부른다.</p>
 */
public final class OutlineText {

    private static final int CACHE_SIZE = 32;
    /**
     * 그림이 글자 칸 바깥으로 나갈 수 있는 폭. 테두리 1px에, 글자가 칸 밖으로 1~2px 나가는 경우
     * (돋움체의 갱·냉 같은 글자, 대신 쓰는 글꼴)를 더했다. 글자를 잘리지 않게 담으려면 칸 둘레로 이만큼 비워 둔다.
     */
    public static final int MARGIN = 3;
    /** 칸 안에서 글자 높이를 재는 글자들. 위로 가장 높은 글자와 아래로 가장 내려가는 글자를 넣었다 */
    private static final String CELL_SAMPLE = "가뷁gjy|[]";
    /**
     * 글자 픽셀로 치는 최소 알파. 절반(0x80)으로 잡으면 가는 획이 두 픽셀에 반씩 걸칠 때 둘 다 빠져서
     * 괄호나 소문자의 획이 끊긴다. 그보다 낮춰 획이 이어지게 하되, 더 낮추면 글자가 뭉개진다.
     */
    private static final int OPAQUE = 0x50;

    private final Font font;
    private final int cellHeight;
    private final Color fill;
    private final Color outline;
    private final FontMetrics metrics;
    /** 칸 위쪽에서 글자 기준선까지 */
    private final int baseline;
    private final Map<String, BufferedImage> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    /** @param cellHeight 글자 칸 높이. 글꼴의 글자가 이 높이에 맞는 크기여야 한다 */
    public OutlineText(Font font, int cellHeight, Color fill, Color outline) {
        this.font = Objects.requireNonNull(font);
        this.cellHeight = cellHeight;
        this.fill = Objects.requireNonNull(fill);
        this.outline = Objects.requireNonNull(outline);
        Graphics2D g = dotGraphics(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
        this.metrics = g.getFontMetrics(font);
        g.dispose();
        this.baseline = baselineInCell();
    }

    public Font font() {
        return font;
    }

    public int cellHeight() {
        return cellHeight;
    }

    /** 테두리를 뺀 글자 폭 */
    public int width(String text) {
        return metrics.stringWidth(text);
    }

    /** 글자 칸의 왼쪽 위를 (x, y)에 두고 그린다. */
    public void draw(Graphics2D g, String text, int x, int y) {
        if (text.isEmpty()) {
            return;
        }
        g.drawImage(cache.computeIfAbsent(text, this::render), x - MARGIN, y - MARGIN, null);
    }

    /** 글자와 같은 색·테두리의 세로 막대(입력 커서). 칸의 왼쪽 위가 (x, y), 폭이 width다. */
    public void drawBar(Graphics2D g, int x, int y, int width) {
        Color previous = g.getColor();
        g.setColor(outline);
        g.fillRect(x - 1, y - 1, width + 2, cellHeight + 2);
        g.setColor(fill);
        g.fillRect(x, y, width, cellHeight);
        g.setColor(previous);
    }

    private BufferedImage render(String text) {
        BufferedImage glyphs = glyphs(text, width(text) + 2 * MARGIN, cellHeight + 2 * MARGIN, MARGIN, MARGIN + baseline);
        BufferedImage out = new BufferedImage(glyphs.getWidth(), glyphs.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int fillArgb = fill.getRGB();
        int outlineArgb = outline.getRGB();
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                if (isGlyph(glyphs, x, y)) {
                    out.setRGB(x, y, fillArgb);
                } else if (touchesGlyph(glyphs, x, y)) {
                    out.setRGB(x, y, outlineArgb);
                }
            }
        }
        return out;
    }

    /** 글자 픽셀과 맞닿은 자리인지. 대각선으로 닿아도 테두리를 찍어야 글자가 끊긴 곳 없이 감싸진다. */
    private static boolean touchesGlyph(BufferedImage glyphs, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (isGlyph(glyphs, x + dx, y + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isGlyph(BufferedImage glyphs, int x, int y) {
        return x >= 0 && y >= 0 && x < glyphs.getWidth() && y < glyphs.getHeight()
                && (glyphs.getRGB(x, y) >>> 24) != 0;
    }

    /** 글자 칸 가운데에 글자가 오게 하는 기준선. 글자 높이가 칸 높이와 같으면 위아래로 딱 맞는다. */
    private int baselineInCell() {
        int probeBaseline = MARGIN + metrics.getMaxAscent();
        int height = probeBaseline + metrics.getMaxDescent() + MARGIN;
        Box ink = PixelImages.opaqueBounds(glyphs(CELL_SAMPLE, width(CELL_SAMPLE) + 2 * MARGIN, height, MARGIN, probeBaseline));
        int above = probeBaseline - ink.y();
        return (cellHeight - ink.height()) / 2 + above;
    }

    /** text를 흑백으로 찍은 그림. 글자 픽셀은 불투명한 흰색, 나머지는 완전히 투명하다. */
    private BufferedImage glyphs(String text, int width, int height, int x, int baselineY) {
        BufferedImage image = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dotGraphics(image);
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.drawString(text, x, baselineY);
        g.dispose();
        for (int py = 0; py < image.getHeight(); py++) {
            for (int px = 0; px < image.getWidth(); px++) {
                image.setRGB(px, py, (image.getRGB(px, py) >>> 24) >= OPAQUE ? 0xFFFFFFFF : 0);
            }
        }
        return image;
    }

    private static Graphics2D dotGraphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        return g;
    }
}

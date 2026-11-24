package hansung.arcade.outgame.ui;

import javax.swing.SwingConstants;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * 아웃게임 그리기 도구. 도형과 글자를 안티앨리어싱으로 매끈하게 그린다.
 *
 * <p>둥근 판은 {@link #paint}로 먼저 모양(알파)을 칠한 뒤 그 위에 바탕색·광택·테두리를 덧칠한다. 덧칠은 이미 칠한 알파를
 * 바꾸지 않으므로(SrcAtop) 모서리의 매끈한 가장자리가 광택이나 테두리 때문에 진해지지 않는다.</p>
 */
public final class Shapes {

    private Shapes() {
    }

    /** 도형·글자 안티앨리어싱을 켜고, 모서리 좌표를 그대로 쓰는(보정하지 않는) 그리기 설정 */
    public static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    /** 반지름 radius인 둥근 사각형 (Java2D의 모서리 지름은 2 x radius) */
    public static RoundRectangle2D round(double x, double y, double width, double height, double radius) {
        return new RoundRectangle2D.Double(x, y, width, height, 2 * radius, 2 * radius);
    }

    /**
     * width x height 그림에 mask로 모양을 칠하고, 그 모양 안을 fill로 채운 뒤 decorate로 덧칠한다.
     * mask는 불투명한 색으로 모양만 칠하면 되고, 그 알파가 그대로 결과의 알파가 된다.
     */
    public static BufferedImage paint(int width, int height, Consumer<Graphics2D> mask, Paint fill,
                                      Consumer<Graphics2D> decorate) {
        BufferedImage image = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            hints(g);
            g.setColor(Color.BLACK);
            mask.accept(g);
            g.setComposite(AlphaComposite.SrcIn);
            g.setPaint(fill);
            g.fillRect(0, 0, width, height);
            g.setComposite(AlphaComposite.SrcAtop);
            decorate.accept(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** 반지름 radius로 모서리를 깎은 fill 판 */
    public static BufferedImage roundRect(int width, int height, int radius, Paint fill) {
        return paint(width, height, g -> g.fill(round(0, 0, width, height, radius)), fill, g -> { });
    }

    /** 위(top) → 아래(bottom) 세로 그러데이션 */
    public static Paint vertical(int height, Color top, Color bottom) {
        return top.equals(bottom) ? top : new GradientPaint(0, 0, top, 0, Math.max(1, height - 1), bottom);
    }

    /** 위 → 가운데 → 아래 세 색 세로 그러데이션 */
    public static Paint vertical(int height, Color top, Color middle, Color bottom) {
        return new java.awt.LinearGradientPaint(0, 0, 0, Math.max(1, height - 1), new float[]{0f, 0.5f, 1f},
                new Color[]{top, middle, bottom});
    }

    /** 모양 안쪽 가장자리를 따라 width 두께로 테두리를 그린다. {@link #paint}의 덧칠 안에서 쓴다 */
    public static void edge(Graphics2D g, int width, int height, int radius, Color color, float thickness) {
        if (color == null || thickness <= 0) {
            return;
        }
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        double half = thickness / 2.0;
        g.draw(round(half, half, width - thickness, height - thickness, Math.max(0, radius - half)));
    }

    /** 위쪽 절반에 흰 광택을 입힌다 (위가 가장 밝고 가운데로 갈수록 옅어진다) */
    public static void gloss(Graphics2D g, int width, int height, float strength) {
        int half = Math.max(1, height / 2);
        g.setPaint(new GradientPaint(0, 0, new Color(1f, 1f, 1f, strength), 0, half,
                new Color(1f, 1f, 1f, strength * 0.15f)));
        g.fillRect(0, 0, width, half);
    }

    /** 위쪽과 왼쪽 안테두리를 밝게 빛나게 한다 (바깥 테두리 1px 안쪽에 thickness 두께) */
    public static void shine(Graphics2D g, int width, int height, Color light, int thickness) {
        g.setColor(light);
        g.fillRect(1, 1, width - 2, thickness);
        g.fillRect(1, 1, thickness, height - 2);
    }

    /** 글자를 그렸을 때 차지하는 크기 [폭, 높이]. 테두리 두께만큼 둘레가 더 크다 */
    public static int[] textSize(String text, Font font, int outline) {
        FontMetrics fm = metrics(font);
        return new int[]{fm.stringWidth(text) + 2 * outline + 2, fm.getAscent() + fm.getDescent() + 2 * outline};
    }

    public static FontMetrics metrics(Font font) {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = probe.createGraphics();
        hints(g);
        try {
            return g.getFontMetrics(font);
        } finally {
            g.dispose();
        }
    }

    /** 한 색 글자를 그린다. {@link #text(Graphics2D, String, Font, Color, Color, Color, int, Color, int, int, int, int, int)} 참고 */
    public static void text(Graphics2D g, String text, Font font, Color fill, Color outline, int outlineWidth,
                            Color shadow, int x, int y, int width, int height, int align) {
        text(g, text, font, fill, null, outline, outlineWidth, shadow, x, y, width, height, align);
    }

    /**
     * text를 (x, y, width, height) 칸의 세로 가운데에 align대로 그린다.
     *
     * @param fill         글자 색. fillBottom이 있으면 글자 위쪽 색
     * @param fillBottom   글자 아래쪽 색(위 → 아래 그러데이션). 한 색이면 null
     * @param outline      글자 둘레에 두를 테두리 색. 없으면 null
     * @param outlineWidth 테두리 두께(px)
     * @param shadow       1px 아래에 깔 그림자 색. 없으면 null
     */
    /**
     * 칸 폭(available)을 넘는 글자는 뒤를 줄이고 "…"를 붙인다. 긴 방 제목이 칸 경계에서 글자 중간이 잘려 보이지 않게 한다.
     */
    public static String fit(String text, Font font, FontRenderContext frc, float available) {
        if (text == null || text.isEmpty() || available <= 0
                || font.getStringBounds(text, frc).getWidth() <= available + 0.5f) {
            return text;
        }
        int[] points = text.codePoints().toArray();
        for (int keep = points.length - 1; keep > 0; keep--) {
            String shorter = new String(points, 0, keep).stripTrailing() + "…";
            if (font.getStringBounds(shorter, frc).getWidth() <= available + 0.5f) {
                return shorter;
            }
        }
        return "…";
    }

    public static void text(Graphics2D g, String text, Font font, Color fill, Color fillBottom, Color outline,
                            int outlineWidth, Color shadow, int x, int y, int width, int height, int align) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Graphics2D t = (Graphics2D) g.create();
        try {
            hints(t);
            FontRenderContext frc = t.getFontRenderContext();
            TextLayout layout = new TextLayout(text, font, frc);
            float advance = layout.getAdvance();
            float left = switch (align) {
                case SwingConstants.LEFT, SwingConstants.LEADING -> x + outlineWidth;
                case SwingConstants.RIGHT, SwingConstants.TRAILING -> x + width - advance - outlineWidth;
                default -> x + (width - advance) / 2f;
            };
            float baseline = y + (height - (layout.getAscent() + layout.getDescent())) / 2f + layout.getAscent();
            Shape glyphs = layout.getOutline(AffineTransform.getTranslateInstance(left, baseline));
            if (shadow != null) {
                t.setColor(shadow);
                t.fill(AffineTransform.getTranslateInstance(0, 1).createTransformedShape(glyphs));
            }
            if (outline != null && outlineWidth > 0) {
                t.setColor(outline);
                t.setStroke(new BasicStroke(2f * outlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                t.draw(glyphs);
            }
            if (fillBottom == null) {
                t.setColor(fill);
            } else {
                var bounds = glyphs.getBounds2D();
                t.setPaint(new GradientPaint(0, (float) bounds.getMinY(), fill, 0, (float) bounds.getMaxY(), fillBottom));
            }
            // 테두리는 글자 모양을 따라 그리고, 속은 글꼴 힌팅이 들어가는 drawString으로 채워 작은 글자도 또렷하게 한다
            t.setFont(font);
            t.drawString(text, left, baseline);
        } finally {
            t.dispose();
        }
    }

    /** 부드럽게(겹치는 넓이 평균에 가깝게) width x height로 줄이거나 키운 그림. 큰 그림을 작게 줄일 때 쓴다 */
    public static BufferedImage smooth(BufferedImage source, int width, int height) {
        BufferedImage current = source;
        int w = source.getWidth();
        int h = source.getHeight();
        // 한 번에 크게 줄이면 이중선형 보간이 픽셀을 건너뛰므로 반씩 여러 번 줄인다
        while (w / 2 >= width && h / 2 >= height) {
            w /= 2;
            h /= 2;
            current = resize(current, w, h);
        }
        return resize(current, Math.max(1, width), Math.max(1, height));
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    /** 비율을 지키며 width x height를 꽉 채우도록 부드럽게 바꾸고 가운데를 남긴 그림 */
    public static BufferedImage cover(BufferedImage source, int width, int height) {
        double scale = Math.max(width / (double) source.getWidth(), height / (double) source.getHeight());
        int cropW = (int) Math.min(source.getWidth(), Math.round(width / scale));
        int cropH = (int) Math.min(source.getHeight(), Math.round(height / scale));
        BufferedImage crop = source.getSubimage((source.getWidth() - cropW) / 2, (source.getHeight() - cropH) / 2,
                cropW, cropH);
        return smooth(crop, width, height);
    }

    /**
     * 하늘색 바탕에서 잘라낸 버튼 그림의 둥근 모서리 밖을 투명하게 지운다.
     * 반지름 radius인 안티앨리어싱 둥근 사각형 밖의 픽셀은 덮인 만큼만 남기고, 모서리 칸에 남은 바탕 하늘색 픽셀은 지운다.
     */
    public static BufferedImage cutCorners(BufferedImage art, int radius) {
        int w = art.getWidth();
        int h = art.getHeight();
        BufferedImage mask = paint(w, h, g -> g.fill(round(0, 0, w, h, radius)), Color.BLACK, g -> { });
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int zone = radius + 2;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = art.getRGB(x, y);
                int alpha = ((argb >>> 24) * (mask.getRGB(x, y) >>> 24)) / 255;
                boolean corner = (x < zone || x >= w - zone) && (y < zone || y >= h - zone);
                if (corner && isBackdrop(argb)) {
                    alpha = 0;
                }
                out.setRGB(x, y, (alpha << 24) | (argb & 0xFFFFFF));
            }
        }
        return out;
    }

    /** 버튼 그림을 잘라낸 원래 바탕(빨강이 거의 없는 밝은 하늘색) */
    private static boolean isBackdrop(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return r < 90 && g > 120 && b > 190;
    }
}

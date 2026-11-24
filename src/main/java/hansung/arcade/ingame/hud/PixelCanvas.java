package hansung.arcade.ingame.hud;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * 도트 그림처럼 픽셀 단위로 칠하는 캔버스. 둥근 모서리는 픽셀마다 덮인 비율을 계산한 뒤
 * {@link #quantize 몇 단계로만} 나눠 칠한다. 그래서 매끈한 안티앨리어싱 대신 계단 모양에
 * 중간색 한 단계가 섞인, 손으로 찍은 도트 같은 가장자리가 된다.
 *
 * <p>화면 장식처럼 바뀌지 않는 부분을 한 번 그려 두고 매 프레임 이미지로 붙이는 용도라 속도보다 정확도를 택했다.</p>
 */
public final class PixelCanvas {

    /** 한 픽셀을 SUB x SUB 점으로 나눠 덮인 비율을 잰다 */
    private static final int SUB = 4;

    private final BufferedImage image;

    public PixelCanvas(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public BufferedImage image() {
        return image;
    }

    /**
     * 덮인 비율(0~1)을 도트 단계로 줄인다. 1/4 미만은 비우고, 3/4 미만은 반투명 한 단계, 나머지는 꽉 채운다.
     */
    public static double quantize(double coverage) {
        if (coverage < 0.25) {
            return 0;
        }
        return coverage < 0.75 ? 0.5 : 1;
    }

    public void fill(Box box, Corners corners, Color color) {
        int argb = color.getRGB();
        fill(box, corners, (x, y) -> argb);
    }

    /** 칸마다 색이 다른 채우기 (격자 무늬 등) */
    public void fill(Box box, Corners corners, PixelPaint paint) {
        for (int y = Math.max(0, box.y()); y < Math.min(image.getHeight(), box.bottom()); y++) {
            for (int x = Math.max(0, box.x()); x < Math.min(image.getWidth(), box.right()); x++) {
                double alpha = quantize(coverage(box, corners, x, y));
                if (alpha > 0) {
                    blend(x, y, paint.argbAt(x, y), alpha);
                }
            }
        }
    }

    /** 바깥 도형에서 안쪽 도형을 뺀 띠만 칠한다. 안쪽은 원래 픽셀이 그대로 남는다. */
    public void ring(Box outer, Corners outerCorners, Box inner, Corners innerCorners, Color color) {
        int argb = color.getRGB();
        for (int y = Math.max(0, outer.y()); y < Math.min(image.getHeight(), outer.bottom()); y++) {
            for (int x = Math.max(0, outer.x()); x < Math.min(image.getWidth(), outer.right()); x++) {
                double band = coverage(outer, outerCorners, x, y) - coverage(inner, innerCorners, x, y);
                double alpha = quantize(band);
                if (alpha > 0) {
                    blend(x, y, argb, alpha);
                }
            }
        }
    }

    /**
     * 테두리가 있는 도형. 바깥 도형을 테두리색으로 칠하고 그 위에 안쪽 도형을 채움색으로 덮는다.
     * 띠와 속을 따로 칠하면 경계의 반투명 픽셀 사이로 배경이 비치므로 이 순서로 겹친다.
     */
    public void fillOutlined(Box box, Corners corners, int thickness, Color edge, Color fill) {
        fill(box, corners, edge);
        fill(box.inset(thickness), corners.shrink(thickness), fill);
    }

    public void fillOutlined(Box box, Corners corners, int thickness, Color edge, PixelPaint fill) {
        fill(box, corners, edge);
        fill(box.inset(thickness), corners.shrink(thickness), fill);
    }

    /** 이미지·글자를 얹을 때 쓰는 Graphics2D. 안티앨리어싱을 끈 상태다. 쓰고 나면 dispose 해야 한다. */
    public Graphics2D graphics() {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        return g;
    }

    /**
     * 픽셀 (px, py) 한 칸이 둥근 사각형에 덮인 비율. 모서리 원호 근처만 잘게 나눠 재고 나머지는 0 또는 1이다.
     */
    static double coverage(Box box, Corners corners, int px, int py) {
        if (!box.contains(px, py)) {
            return 0;
        }
        corners = fit(corners, box);
        int radius;
        int cx;
        int cy;
        if (px < box.x() + corners.topLeft() && py < box.y() + corners.topLeft()) {
            radius = corners.topLeft();
            cx = box.x() + radius;
            cy = box.y() + radius;
        } else if (px >= box.right() - corners.topRight() && py < box.y() + corners.topRight()) {
            radius = corners.topRight();
            cx = box.right() - radius;
            cy = box.y() + radius;
        } else if (px >= box.right() - corners.bottomRight() && py >= box.bottom() - corners.bottomRight()) {
            radius = corners.bottomRight();
            cx = box.right() - radius;
            cy = box.bottom() - radius;
        } else if (px < box.x() + corners.bottomLeft() && py >= box.bottom() - corners.bottomLeft()) {
            radius = corners.bottomLeft();
            cx = box.x() + radius;
            cy = box.bottom() - radius;
        } else {
            return 1;
        }
        double r2 = (double) radius * radius;
        int inside = 0;
        for (int i = 0; i < SUB; i++) {
            for (int j = 0; j < SUB; j++) {
                double dx = px + (i + 0.5) / SUB - cx;
                double dy = py + (j + 0.5) / SUB - cy;
                if (dx * dx + dy * dy <= r2) {
                    inside++;
                }
            }
        }
        return inside / (double) (SUB * SUB);
    }

    /** 반지름이 상자 폭·높이의 절반을 넘으면 모서리 영역끼리 겹치므로 절반으로 줄인다. */
    private static Corners fit(Corners corners, Box box) {
        int max = Math.min(box.width(), box.height()) / 2;
        if (corners.topLeft() <= max && corners.topRight() <= max
                && corners.bottomRight() <= max && corners.bottomLeft() <= max) {
            return corners;
        }
        return new Corners(Math.min(corners.topLeft(), max), Math.min(corners.topRight(), max),
                Math.min(corners.bottomRight(), max), Math.min(corners.bottomLeft(), max));
    }

    /** 원래 픽셀 위에 argb 색을 alpha 비율로 덮는다 (SrcOver). */
    private void blend(int x, int y, int argb, double alpha) {
        if (alpha >= 1 && (argb >>> 24) == 0xFF) {
            image.setRGB(x, y, argb);
            return;
        }
        double srcA = ((argb >>> 24) / 255.0) * alpha;
        if (srcA <= 0) {
            return;
        }
        int dst = image.getRGB(x, y);
        double dstA = (dst >>> 24) / 255.0;
        double outA = srcA + dstA * (1 - srcA);
        int r = mix((argb >> 16) & 0xFF, (dst >> 16) & 0xFF, srcA, dstA, outA);
        int g = mix((argb >> 8) & 0xFF, (dst >> 8) & 0xFF, srcA, dstA, outA);
        int b = mix(argb & 0xFF, dst & 0xFF, srcA, dstA, outA);
        int a = (int) Math.round(outA * 255);
        image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
    }

    private static int mix(int src, int dst, double srcA, double dstA, double outA) {
        return (int) Math.round((src * srcA + dst * dstA * (1 - srcA)) / outA);
    }

    /** 칸 좌표에 따라 색(0xAARRGGBB)을 정한다 */
    @FunctionalInterface
    public interface PixelPaint {
        int argbAt(int x, int y);
    }
}

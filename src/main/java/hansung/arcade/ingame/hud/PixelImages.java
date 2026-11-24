package hansung.arcade.ingame.hud;

import java.awt.image.BufferedImage;

/**
 * 작게 줄인 그림과 글자를 {@link PixelCanvas}와 같은 도트 느낌으로 맞추는 도구.
 */
public final class PixelImages {

    private PixelImages() {
    }

    /** 반투명 가장자리를 {@link PixelCanvas#quantize 도트 단계}로 줄인 새 그림. 색은 그대로 둔다. */
    public static BufferedImage quantizeAlpha(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = source.getRGB(x, y);
                int alpha = (int) Math.round(PixelCanvas.quantize((argb >>> 24) / 255.0) * 255);
                out.setRGB(x, y, alpha == 0 ? 0 : (alpha << 24) | (argb & 0xFFFFFF));
            }
        }
        return out;
    }

    /** 투명하지 않은 픽셀을 모두 담는 가장 작은 영역. 전부 투명하면 그림 전체를 돌려준다. */
    public static Box opaqueBounds(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return new Box(0, 0, image.getWidth(), image.getHeight());
        }
        return new Box(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /**
     * 비율을 지키며 maxWidth x maxHeight 안에 들어가게 줄인 그림. 픽셀이 겹치는 넓이만큼 평균을 내고
     * (투명한 배경색이 가장자리에 번지지 않게 알파를 곱해서 평균), 가장자리는 도트 단계로 줄인다.
     * 원본이 이미 작으면 키우지 않는다.
     */
    public static BufferedImage shrinkToFit(BufferedImage source, int maxWidth, int maxHeight) {
        double scale = Math.min(1.0, Math.min(maxWidth / (double) source.getWidth(),
                maxHeight / (double) source.getHeight()));
        int w = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(source.getHeight() * scale));
        return quantizeAlpha(areaAverage(source, w, h));
    }

    private static BufferedImage areaAverage(BufferedImage source, int width, int height) {
        double stepX = source.getWidth() / (double) width;
        double stepY = source.getHeight() / (double) height;
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int dy = 0; dy < height; dy++) {
            double y0 = dy * stepY;
            double y1 = y0 + stepY;
            for (int dx = 0; dx < width; dx++) {
                double x0 = dx * stepX;
                double x1 = x0 + stepX;
                double a = 0;
                double r = 0;
                double g = 0;
                double b = 0;
                // 나눗셈 오차로 끝이 원본 크기를 살짝 넘을 수 있어 범위를 자른다.
                int yEnd = Math.min(source.getHeight(), (int) Math.ceil(y1));
                int xEnd = Math.min(source.getWidth(), (int) Math.ceil(x1));
                for (int sy = (int) y0; sy < yEnd; sy++) {
                    double wy = Math.min(y1, sy + 1) - Math.max(y0, sy);
                    for (int sx = (int) x0; sx < xEnd; sx++) {
                        double weight = wy * (Math.min(x1, sx + 1) - Math.max(x0, sx));
                        int argb = source.getRGB(sx, sy);
                        double alpha = (argb >>> 24) / 255.0 * weight;
                        a += alpha;
                        r += ((argb >> 16) & 0xFF) * alpha;
                        g += ((argb >> 8) & 0xFF) * alpha;
                        b += (argb & 0xFF) * alpha;
                    }
                }
                double area = stepX * stepY;
                int outA = (int) Math.round(a / area * 255);
                if (outA == 0) {
                    continue;
                }
                out.setRGB(dx, dy, (outA << 24)
                        | ((int) Math.round(r / a) << 16) | ((int) Math.round(g / a) << 8) | (int) Math.round(b / a));
            }
        }
        return out;
    }
}

package hansung.arcade.ingame.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 물줄기 그림에 마젠타 바탕이 번져 남은 분홍·보라빛 픽셀을 둘레의 물 색으로 메운다.
 *
 * <p>물줄기 그림은 마젠타 바탕과 섞인 채 저장되어 있어서, 칸 가장자리 두 줄과 하이라이트 둘레에 분홍·보라빛 픽셀이
 * 1px씩 박혀 있다. Center 두 번째 장은 맨 위 두 줄이 통째로 물들어 있다. 물 색은 청록~파랑(색상각 140~215도)과
 * 흰빛뿐이므로, 파랑을 지나 보라·분홍 쪽으로 기운 색({@link #isSpill})은 번진 자국으로 본다.
 * 실제로 물줄기 그림에서 색상각이 240도를 넘는 색은 80% 가까이가 칸 가장자리 두 줄이나 투명한 바탕 둘레 두 줄 안에 있다
 * (청록~파랑 물 색은 13~34%).</p>
 * <ol>
 *   <li>번진 픽셀을 상하좌우·대각선 8칸 중 깨끗한 불투명 이웃의 평균 색으로 메운다. 깨끗한 이웃이 많은 픽셀부터
 *       차례로 메우므로, 여러 줄이 물든 곳도 물 쪽에서부터 한 줄씩 채워진다. 이미 메운 픽셀도 깨끗한 이웃으로 친다.</li>
 *   <li>깨끗한 이웃이 하나도 없어 메우지 못한 픽셀은 빨강을 초록만큼 낮춰 파랑 쪽으로 돌린다.</li>
 * </ol>
 * <p>알파는 건드리지 않으므로 물줄기 모양은 그대로다.</p>
 */
final class MagentaSpill {

    /** 가장 밝은 채널과 가장 어두운 채널의 차가 이보다 작은 흰빛·회색빛은 번진 자국으로 보지 않는다 */
    static final int MIN_CHROMA = 16;

    private static final int[][] AROUND = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private MagentaSpill() {
    }

    /** 프레임 한 장의 번진 픽셀을 메운 새 그림. 원본은 그대로 둔다 */
    static BufferedImage clean(BufferedImage frame) {
        int w = frame.getWidth();
        int h = frame.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        boolean[] spilled = new boolean[w * h];
        int left = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = frame.getRGB(x, y);
                out.setRGB(x, y, argb);
                if ((argb >>> 24) != 0 && isSpill(argb)) {
                    spilled[y * w + x] = true;
                    left++;
                }
            }
        }
        while (left > 0) {
            List<int[]> fills = nextFills(out, spilled);
            if (fills.isEmpty()) {
                break;
            }
            for (int[] fill : fills) {
                int at = fill[0];
                int alpha = out.getRGB(at % w, at / w) & 0xFF000000;
                out.setRGB(at % w, at / w, alpha | fill[1]);
                spilled[at] = false;
            }
            left -= fills.size();
        }
        for (int at = 0; at < w * h; at++) {
            if (spilled[at]) {
                out.setRGB(at % w, at / w, toBlue(out.getRGB(at % w, at / w)));
            }
        }
        return out;
    }

    /**
     * 파랑을 지나 보라~분홍~빨강 쪽(색상각 240도 초과 360도 이하)으로 기운 색인지.
     * 물 색은 초록이 빨강보다 밝으므로, 초록이 가장 어둡고 빨강이 초록보다 밝으면 마젠타가 섞인 색이다.
     * 하이라이트의 노르스름한 흰빛(파랑이 가장 어두움)은 물 그림에 원래 있는 색이라 들지 않는다.
     */
    static boolean isSpill(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return g < r && g <= b && Math.max(r, b) - g >= MIN_CHROMA;
    }

    /** 깨끗한 이웃이 가장 많은 번진 픽셀들과 그 이웃 평균 색 ({위치, RGB}) */
    private static List<int[]> nextFills(BufferedImage out, boolean[] spilled) {
        int w = out.getWidth();
        int h = out.getHeight();
        List<int[]> fills = new ArrayList<>();
        int most = 0;
        for (int at = 0; at < w * h; at++) {
            if (!spilled[at]) {
                continue;
            }
            int count = 0;
            int r = 0;
            int g = 0;
            int b = 0;
            for (int[] step : AROUND) {
                int nx = at % w + step[0];
                int ny = at / w + step[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h || spilled[ny * w + nx]) {
                    continue;
                }
                int argb = out.getRGB(nx, ny);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                count++;
                r += (argb >> 16) & 0xFF;
                g += (argb >> 8) & 0xFF;
                b += argb & 0xFF;
            }
            if (count == 0 || count < most) {
                continue;
            }
            if (count > most) {
                most = count;
                fills.clear();
            }
            int half = count / 2;
            fills.add(new int[]{at, ((r + half) / count) << 16 | ((g + half) / count) << 8 | (b + half) / count});
        }
        // 평균이 우연히 다시 보라빛이면 파랑 쪽으로 돌린다
        fills.replaceAll(fill -> isSpill(fill[1]) ? new int[]{fill[0], toBlue(fill[1]) & 0xFFFFFF} : fill);
        return fills;
    }

    /** 빨강을 초록만큼 낮춘 색. 알파는 그대로 */
    private static int toBlue(int argb) {
        int g = (argb >> 8) & 0xFF;
        return (argb & 0xFF00FFFF) | g << 16;
    }
}

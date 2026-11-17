package hansung.arcade.ingame.render;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 검정 바탕에 그려진 그림(물줄기 끝 그림)의 바탕을 투명하게 지운다.
 *
 * <p>이 그림들은 마젠타 바탕을 검정으로 바꾸는 과정에서 물줄기 둘레에 어두운 색·회색빛·분홍빛 잡티가 남았다.
 * 그래서 완전한 검정(0, 0, 0)만 지우면 풀밭 위에서 테두리가 지저분하게 보인다.</p>
 * <ol>
 *   <li>완전한 검정을 지운다.</li>
 *   <li>지운 칸에 상하좌우로 이어진 잡티({@link #isFringe})를 차례로 지워 나간다. 물줄기 안쪽의 밝은 색에 막혀 멈추므로
 *       물줄기 안의 같은 색 픽셀은 남는다.</li>
 *   <li>그러고도 떨어져 떠다니는 {@link #SPECK_LIMIT}픽셀 미만의 작은 덩어리를 지운다.</li>
 * </ol>
 */
final class BlackBackground {

    /** 이보다 작은 외딴 덩어리는 잡티로 보고 지운다 */
    static final int SPECK_LIMIT = 8;

    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] AROUND = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private BlackBackground() {
    }

    /** 프레임 한 장의 검정 바탕과 잡티를 지운 새 그림. 원본은 그대로 둔다 */
    static BufferedImage clear(BufferedImage frame) {
        int w = frame.getWidth();
        int h = frame.getHeight();
        boolean[] cleared = new boolean[w * h];
        Deque<Integer> queue = new ArrayDeque<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((frame.getRGB(x, y) & 0xFFFFFF) == 0) {
                    cleared[y * w + x] = true;
                    queue.add(y * w + x);
                }
            }
        }
        while (!queue.isEmpty()) {
            int at = queue.poll();
            for (int[] side : SIDES) {
                int nx = at % w + side[0];
                int ny = at / w + side[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h || cleared[ny * w + nx]) {
                    continue;
                }
                if (isFringe(frame.getRGB(nx, ny))) {
                    cleared[ny * w + nx] = true;
                    queue.add(ny * w + nx);
                }
            }
        }
        clearSpecks(cleared, w, h);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setRGB(x, y, cleared[y * w + x] ? 0 : frame.getRGB(x, y) | 0xFF000000);
            }
        }
        return out;
    }

    /** 물줄기 둘레의 잡티 색인지: 아주 어둡거나, 어두운 회색빛이거나, 분홍빛 */
    static boolean isFringe(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        boolean dark = max < 90;
        boolean grayish = max - min < 55 && max < 170;
        boolean pinkish = r - g > 25 && b - g > 25 && r > b - 60;
        return dark || grayish || pinkish;
    }

    /** 대각선까지 이어진 남은 픽셀 덩어리 중 {@link #SPECK_LIMIT}개 미만인 것을 지운다 */
    private static void clearSpecks(boolean[] cleared, int w, int h) {
        boolean[] seen = new boolean[w * h];
        for (int start = 0; start < w * h; start++) {
            if (cleared[start] || seen[start]) {
                continue;
            }
            List<Integer> piece = new ArrayList<>();
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(start);
            seen[start] = true;
            while (!queue.isEmpty()) {
                int at = queue.poll();
                piece.add(at);
                for (int[] step : AROUND) {
                    int nx = at % w + step[0];
                    int ny = at / w + step[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                        continue;
                    }
                    int next = ny * w + nx;
                    if (!cleared[next] && !seen[next]) {
                        seen[next] = true;
                        queue.add(next);
                    }
                }
            }
            if (piece.size() < SPECK_LIMIT) {
                piece.forEach(at -> cleared[at] = true);
            }
        }
    }
}

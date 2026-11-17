package hansung.arcade.ingame.render;

import hansung.arcade.ingame.entity.Direction;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 아이템을 떨어뜨리는 비행기 그림. resources/airplane/ 의 airplane_right.bmp(오른쪽으로 나는 그림)와
 * airplane_left.bmp(왼쪽으로 나는 그림)로, 146 x 76 프레임 네 장에 프로펠러가 돌고 꼬리 연기가 피어오른다.
 *
 * <p>airplane_left.bmp는 오른쪽 그림을 좌우로 뒤집으며 프레임 순서도 거꾸로 놓였다. 그래서 왼쪽 그림은 뒤에서부터 돌려
 * 두 방향이 같은 차례(연기가 작게 나왔다 커지는 차례)로 움직이게 한다.</p>
 *
 * <p>연기와 몸체 둘레에는 마젠타 바탕이 섞인 분홍 픽셀이 남아 있어서, 바탕을 투명하게 바꾼 뒤
 * 투명한 곳에 이어진 분홍 픽셀({@link #isMagentaFringe})을 차례로 지운다.</p>
 */
public final class AirplaneSprites {

    public static final int FRAME_WIDTH = 146;
    public static final int FRAME_HEIGHT = 76;
    /** 오른쪽으로 나는 그림에서 왼쪽 끝부터 아이템을 떨어뜨리는 자리(날개 밑 배 가운데)까지(px) */
    public static final int CARGO_X = 78;
    /** 그림 위쪽 끝부터 아이템을 떨어뜨리는 자리까지(px) */
    public static final int CARGO_Y = 44;
    /** 비행기 그림이 들어 있는 리소스 폴더 */
    public static final String DIRECTORY = "/airplane/";
    /** 프레임 한 장을 보여 주는 시간(초). 네 장이 0.4초에 한 바퀴 돈다 */
    static final double FRAME_SECONDS = 0.1;
    /** 1/60초씩 더한 시간이 장이 넘어갈 경계에 살짝 못 미쳐도 넘어가게 주는 여유 */
    private static final double FRAME_BOUNDARY_SLACK = 1e-6;

    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** 오른쪽으로 나는 그림. 돌리는 차례 */
    private final List<BufferedImage> eastward;
    /** 왼쪽으로 나는 그림. 돌리는 차례 */
    private final List<BufferedImage> westward;

    private AirplaneSprites(List<BufferedImage> eastward, List<BufferedImage> westward) {
        this.eastward = eastward;
        this.westward = westward;
    }

    /** 두 방향 그림을 읽는다. 파일이 없으면 실패한다. */
    public static AirplaneSprites load() {
        return new AirplaneSprites(frames("airplane_right.bmp"), frames("airplane_left.bmp").reversed());
    }

    /**
     * 지금 그릴 프레임.
     *
     * @param heading 나는 방향. {@link Direction#RIGHT} 또는 {@link Direction#LEFT}
     * @param seconds 비행기가 나타난 뒤 지난 시간(초)
     */
    public BufferedImage frame(Direction heading, double seconds) {
        List<BufferedImage> frames = switch (heading) {
            case RIGHT -> eastward;
            case LEFT -> westward;
            default -> throw new IllegalArgumentException("비행기는 왼쪽이나 오른쪽으로만 납니다: " + heading);
        };
        int elapsedFrames = (int) Math.floor(seconds / FRAME_SECONDS + FRAME_BOUNDARY_SLACK);
        return frames.get(elapsedFrames % frames.size());
    }

    /** heading 쪽으로 나는 그림에서 왼쪽 끝부터 아이템을 떨어뜨리는 자리까지(px). 왼쪽으로 나는 그림은 좌우가 뒤집혀 있다 */
    public static int cargoX(Direction heading) {
        return heading == Direction.LEFT ? FRAME_WIDTH - CARGO_X : CARGO_X;
    }

    private static List<BufferedImage> frames(String fileName) {
        String path = DIRECTORY + fileName;
        return SpriteSheet.find(path)
                .orElseThrow(() -> new IllegalStateException("비행기 그림 파일이 없습니다: " + path))
                .frames(FRAME_WIDTH).stream()
                .map(AirplaneSprites::clearFringe)
                .toList();
    }

    /** 투명한 곳에서 상하좌우로 이어진 분홍 픽셀을 지운 새 그림. 몸체의 빨강·주황·파랑에 막혀 안쪽으로는 번지지 않는다 */
    static BufferedImage clearFringe(BufferedImage frame) {
        int w = frame.getWidth();
        int h = frame.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Deque<int[]> queue = new ArrayDeque<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = frame.getRGB(x, y);
                out.setRGB(x, y, argb);
                if (argb >>> 24 == 0) {
                    queue.add(new int[]{x, y});
                }
            }
        }
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            for (int[] side : SIDES) {
                int nx = at[0] + side[0];
                int ny = at[1] + side[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                    continue;
                }
                int argb = out.getRGB(nx, ny);
                if (argb >>> 24 != 0 && isMagentaFringe(argb)) {
                    out.setRGB(nx, ny, 0);
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return out;
    }

    /** 마젠타 바탕이 섞인 색인지: 빨강과 파랑이 비슷하게 높고 초록만 눈에 띄게 낮다 (예: f546f5, e86be8) */
    static boolean isMagentaFringe(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r >= 150 && b >= 150 && Math.abs(r - b) <= 50 && Math.min(r, b) - g >= 30;
    }
}

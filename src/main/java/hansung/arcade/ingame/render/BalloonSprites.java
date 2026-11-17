package hansung.arcade.ingame.render;

import hansung.arcade.ingame.balloon.WaterBalloon;
import hansung.arcade.ingame.balloon.WaterCell;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.map.TileMap;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 놓인 물풍선과 터진 물줄기 그림. 파일은 resources/water_balloon/ 에 있다.
 *
 * <ul>
 *   <li>Balloon.png: 놓인 물풍선 4장 (47 x 47). 바탕은 투명하고 그림자는 반투명 알파로 들어 있다.
 *       {@link WaterBalloon#FRAME_SECONDS}초씩 되풀이한다.</li>
 *   <li>Center.bmp: 터진 칸 물줄기 3장 (40 x 40).</li>
 *   <li>Up/Down/Left/Right.bmp: 그 방향 물줄기 가운데 칸. 8장 중 앞 3장만 쓴다.</li>
 *   <li>UpLast/DownLast/LeftLast/RightLast.bmp: 그 방향 물줄기 끝 칸. 8장 중 앞 3장만 쓰고,
 *       검정 바탕과 둘레 잡티는 {@link BlackBackground}로 지운다.</li>
 * </ul>
 * <p>물줄기 그림에는 모두 마젠타 바탕이 번진 분홍·보라빛 픽셀이 박혀 있어서 {@link MagentaSpill}로 둘레 물 색으로 메운다.</p>
 */
public final class BalloonSprites {

    /** 물풍선·물줄기 그림이 들어 있는 리소스 폴더 */
    public static final String DIRECTORY = "/water_balloon/";
    /** 놓인 물풍선 그림 한 장의 가로·세로 크기 */
    public static final int BALLOON_SIZE = 47;
    /** 놓인 물풍선 그림에서 몸체(원) 가운데의 가로 위치. 첫 장 기준이고, 이 점을 칸 가운데에 맞춰 그린다 */
    public static final int BALLOON_CENTER_X = 24;
    /** 놓인 물풍선 그림에서 몸체(원) 가운데의 세로 위치 */
    public static final int BALLOON_CENTER_Y = 20;
    /** 물줄기 그림 장 수. 파일에 더 있어도 앞에서부터 이만큼만 쓴다 */
    private static final int WATER_FRAMES = 3;
    private static final double FRAME_BOUNDARY_SLACK = 1e-6;
    private static final int T = TileMap.TILE_SIZE;

    private final List<BufferedImage> balloon;
    private final List<BufferedImage> center;
    private final Map<Direction, List<BufferedImage>> middle;
    private final Map<Direction, List<BufferedImage>> end;

    private BalloonSprites(List<BufferedImage> balloon, List<BufferedImage> center,
                           Map<Direction, List<BufferedImage>> middle, Map<Direction, List<BufferedImage>> end) {
        this.balloon = balloon;
        this.center = center;
        this.middle = middle;
        this.end = end;
    }

    /** 그림을 모두 읽는다. 빠진 파일이 있으면 실패한다. */
    public static BalloonSprites load() {
        Map<Direction, List<BufferedImage>> middle = new EnumMap<>(Direction.class);
        Map<Direction, List<BufferedImage>> end = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            String name = fileName(direction);
            middle.put(direction, waterSheet(name + ".bmp").stream()
                    .map(MagentaSpill::clean)
                    .toList());
            // 검정 바탕을 먼저 지워야 바탕에 닿은 분홍 잡티가 바탕과 함께 지워진다. 남은 것만 물 색으로 메운다
            end.put(direction, waterSheet(name + "Last.bmp").stream()
                    .map(BlackBackground::clear)
                    .map(frame -> sealInnerEdge(frame, direction))
                    .map(MagentaSpill::clean)
                    .toList());
        }
        List<BufferedImage> center = waterSheet("Center.bmp").stream()
                .map(MagentaSpill::clean)
                .toList();
        return new BalloonSprites(sheet("Balloon.png", BALLOON_SIZE), center, middle, end);
    }

    /** 놓인 지 age초 된 물풍선 그림 */
    public BufferedImage balloon(double age) {
        int index = (int) Math.floor(age / WaterBalloon.FRAME_SECONDS + FRAME_BOUNDARY_SLACK);
        return balloon.get(index % balloon.size());
    }

    /** 물줄기 칸 하나의 frame번째(0부터) 그림. 40 x 40이고 칸 왼쪽 위에 맞춰 그린다 */
    public BufferedImage water(WaterCell cell, int frame) {
        return switch (cell.part()) {
            case CENTER -> center.get(frame);
            case MIDDLE -> middle.get(cell.direction()).get(frame);
            case END -> end.get(cell.direction()).get(frame);
        };
    }

    /**
     * 물줄기 끝 그림에서 가운데 칸과 맞닿는 안쪽 가장자리 줄의 빈 픽셀을 바로 안쪽 픽셀 색으로 메운다.
     * LeftLast 첫 장은 그 줄이 검정이라 지우고 나면 가운데 칸과의 사이에 1px 틈이 보이기 때문이다.
     *
     * @param direction 물줄기가 뻗은 방향. 안쪽 가장자리는 그 반대쪽이다 (왼쪽으로 뻗은 끝 칸이면 오른쪽 끝 줄)
     */
    static BufferedImage sealInnerEdge(BufferedImage frame, Direction direction) {
        int w = frame.getWidth();
        int h = frame.getHeight();
        int length = direction.isHorizontal() ? h : w;
        for (int k = 0; k < length; k++) {
            // 가장자리 픽셀과 그 바로 안쪽 픽셀 (방향의 반대쪽 끝에서 한 칸 들어간 자리)
            int edgeX = switch (direction) {
                case LEFT -> w - 1;
                case RIGHT -> 0;
                default -> k;
            };
            int edgeY = switch (direction) {
                case UP -> h - 1;
                case DOWN -> 0;
                default -> k;
            };
            int innerX = edgeX + direction.dx();
            int innerY = edgeY + direction.dy();
            if ((frame.getRGB(edgeX, edgeY) >>> 24) == 0 && (frame.getRGB(innerX, innerY) >>> 24) != 0) {
                frame.setRGB(edgeX, edgeY, frame.getRGB(innerX, innerY));
            }
        }
        return frame;
    }

    /** 물줄기 그림 파일의 앞 {@link #WATER_FRAMES}장 */
    private static List<BufferedImage> waterSheet(String fileName) {
        return sheet(fileName, T).subList(0, WATER_FRAMES);
    }

    private static List<BufferedImage> sheet(String fileName, int frameWidth) {
        String path = DIRECTORY + fileName;
        return SpriteSheet.find(path)
                .orElseThrow(() -> new IllegalStateException("물풍선 그림 파일이 없습니다: " + path))
                .frames(frameWidth);
    }

    private static String fileName(Direction direction) {
        return switch (direction) {
            case UP -> "Up";
            case DOWN -> "Down";
            case LEFT -> "Left";
            case RIGHT -> "Right";
        };
    }
}

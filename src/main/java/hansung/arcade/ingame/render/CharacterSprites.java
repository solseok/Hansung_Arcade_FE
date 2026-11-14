package hansung.arcade.ingame.render;

import hansung.arcade.ingame.entity.CharacterColor;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 맵 위를 돌아다니는 캐릭터 그림. 캐릭터와 방향마다 서 있는 그림 한 장과 걷는 그림 여러 장이 있다.
 * 파일은 resources/sprites/characters/ 의 {id}_{front|back|left|right}.bmp 와 {id}_walk_{...}.bmp 다.
 *
 * <p>고정색({@link CharacterColor})을 고르면 그 캐릭터의 그림 전부를 {@link CharacterRecolor}로 다시 칠해 쓴다.
 * 다시 칠한 그림은 (캐릭터, 색)마다 처음 쓸 때 한 번만 만들어 두고, 그 뒤로는 만들어 둔 그림을 돌려준다.</p>
 */
public final class CharacterSprites {

    /** 그림 한 장의 가로·세로 크기. 캐릭터는 그림 가운데에 서 있다. */
    public static final int FRAME_SIZE = 70;
    /** 그림 위쪽 끝에서 발끝까지의 높이. 발 아래로는 투명한 여백이 남아 있다. */
    public static final int FEET_Y = 65;

    /** 캐릭터 그림이 들어 있는 리소스 폴더 */
    public static final String DIRECTORY = "/sprites/characters/";
    /** 걷는 그림 한 장을 보여 주는 시간(초) */
    private static final double WALK_FRAME_SECONDS = 0.1;
    /**
     * 걸은 시간은 1/60초씩 더한 값이라 장이 넘어갈 경계에서 0.0999…처럼 살짝 못 미친다.
     * 그대로 자르면 장마다 보이는 틱 수가 5~7로 흔들리므로, 경계에 이만큼 여유를 준다.
     */
    private static final double FRAME_BOUNDARY_SLACK = 1e-6;

    /** 원래 색 그림 */
    private final Map<Mascot, Map<Direction, Pose>> poses;
    /** 다시 칠한 그림. 게임 루프(EDT) 밖에서 그리는 테스트도 있어서 동시에 불려도 안전한 맵을 쓴다 */
    private final Map<Colored, Map<Direction, Pose>> recolored = new ConcurrentHashMap<>();

    private CharacterSprites(Map<Mascot, Map<Direction, Pose>> poses) {
        this.poses = poses;
    }

    /** 모든 캐릭터의 그림을 읽는다. 빠진 파일이 있으면 실패한다. */
    public static CharacterSprites load() {
        Map<Mascot, Map<Direction, Pose>> poses = new EnumMap<>(Mascot.class);
        for (Mascot mascot : Mascot.values()) {
            Map<Direction, Pose> byDirection = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                String side = fileSuffix(direction);
                byDirection.put(direction, new Pose(
                        frames(mascot.id() + "_" + side + ".bmp").get(0),
                        frames(mascot.id() + "_walk_" + side + ".bmp")));
            }
            poses.put(mascot, byDirection);
        }
        return new CharacterSprites(poses);
    }

    /**
     * 지금 그릴 그림. 멈춰 있으면 서 있는 그림, 걷는 중이면 걸은 시간에 맞는 걷는 그림이다.
     *
     * @param walkTime 걷기 시작한 뒤 지난 시간(초)
     */
    public BufferedImage frame(Mascot mascot, Direction facing, boolean moving, double walkTime) {
        return frame(mascot, null, facing, moving, walkTime);
    }

    /**
     * 고정색을 color로 칠한 그림 중 지금 그릴 그림. 고르는 규칙은 {@link #frame(Mascot, Direction, boolean, double)}과 같다.
     *
     * @param color 고정색. null이면 원래 색 그림이다
     */
    public BufferedImage frame(Mascot mascot, CharacterColor color, Direction facing, boolean moving, double walkTime) {
        Pose pose = posesOf(mascot, color).get(facing);
        if (!moving) {
            return pose.standing();
        }
        List<BufferedImage> walking = pose.walking();
        int elapsedFrames = (int) Math.floor(walkTime / WALK_FRAME_SECONDS + FRAME_BOUNDARY_SLACK);
        return walking.get(elapsedFrames % walking.size());
    }

    private Map<Direction, Pose> posesOf(Mascot mascot, CharacterColor color) {
        Map<Direction, Pose> original = poses.get(mascot);
        if (color == null) {
            return original;
        }
        return recolored.computeIfAbsent(new Colored(mascot, color), key -> recolor(original, mascot, color));
    }

    private static Map<Direction, Pose> recolor(Map<Direction, Pose> original, Mascot mascot, CharacterColor color) {
        Map<Direction, Pose> result = new EnumMap<>(Direction.class);
        original.forEach((direction, pose) -> result.put(direction, new Pose(
                CharacterRecolor.recolor(pose.standing(), mascot, color),
                pose.walking().stream().map(frame -> CharacterRecolor.recolor(frame, mascot, color)).toList())));
        return result;
    }

    private static List<BufferedImage> frames(String fileName) {
        String path = DIRECTORY + fileName;
        return SpriteSheet.find(path)
                .orElseThrow(() -> new IllegalStateException("캐릭터 그림 파일이 없습니다: " + path))
                .frames(FRAME_SIZE);
    }

    private static String fileSuffix(Direction direction) {
        return switch (direction) {
            case DOWN -> "front";
            case UP -> "back";
            case LEFT -> "left";
            case RIGHT -> "right";
        };
    }

    private record Pose(BufferedImage standing, List<BufferedImage> walking) {
    }

    private record Colored(Mascot mascot, CharacterColor color) {
    }
}

package hansung.arcade.ingame.render;

import hansung.arcade.ingame.entity.CharacterColor;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.Player;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 맵 위의 캐릭터 그림. 파일은 resources/sprites/characters/ 의 {id}_*.bmp 이고 모두 70 x 70 프레임 가로 띠다.
 *
 * <ul>
 *   <li>{id}_{front|back|left|right}.bmp: 방향마다 서 있는 그림 한 장, {id}_walk_{...}.bmp: 걷는 그림 여러 장</li>
 *   <li>{id}_bubble_1~3.bmp: 물풍선에 갇힌 그림. 갇힌 시간을 셋으로 나눠 1→2→3 순서로 쓰고(뒤로 갈수록 캐릭터가 흐려진다),
 *       각 파일의 4장을 {@link #BUBBLE_FRAME_SECONDS}초씩 되풀이한다.</li>
 *   <li>{id}_needle.bmp: 갇힌 물풍선이 깨져 빠져나오는 그림 5장. 빠져나오는 동안 한 번 보여 준다.</li>
 *   <li>{id}_die_2.bmp: 쓰러져 머리 위에 별이 도는 그림 3장. 탈락 연출 동안 {@link #DIZZY_FRAME_SECONDS}초씩 되풀이한다.</li>
 * </ul>
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
    /** 갇힌 그림 한 장을 보여 주는 시간(초) */
    static final double BUBBLE_FRAME_SECONDS = 0.2;
    /** 갇힌 그림 파일 수 (bubble_1 ~ bubble_3) */
    private static final int BUBBLE_STAGES = 3;
    /** 탈락 연출의 쓰러진 그림 한 장을 보여 주는 시간(초) */
    static final double DIZZY_FRAME_SECONDS = 0.15;
    /**
     * 걸은 시간은 1/60초씩 더한 값이라 장이 넘어갈 경계에서 0.0999…처럼 살짝 못 미친다.
     * 그대로 자르면 장마다 보이는 틱 수가 5~7로 흔들리므로, 경계에 이만큼 여유를 준다.
     */
    private static final double FRAME_BOUNDARY_SLACK = 1e-6;

    /** 원래 색 그림 */
    private final Map<Mascot, Looks> looks;
    /** 다시 칠한 그림. 게임 루프(EDT) 밖에서 그리는 테스트도 있어서 동시에 불려도 안전한 맵을 쓴다 */
    private final Map<Colored, Looks> recolored = new ConcurrentHashMap<>();

    private CharacterSprites(Map<Mascot, Looks> looks) {
        this.looks = looks;
    }

    /** 모든 캐릭터의 그림을 읽는다. 빠진 파일이 있으면 실패한다. */
    public static CharacterSprites load() {
        Map<Mascot, Looks> looks = new EnumMap<>(Mascot.class);
        for (Mascot mascot : Mascot.values()) {
            String id = mascot.id();
            Map<Direction, Pose> poses = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                String side = fileSuffix(direction);
                poses.put(direction, new Pose(
                        frames(id + "_" + side + ".bmp").get(0),
                        frames(id + "_walk_" + side + ".bmp")));
            }
            List<List<BufferedImage>> bubbles = new ArrayList<>();
            for (int stage = 1; stage <= BUBBLE_STAGES; stage++) {
                bubbles.add(frames(id + "_bubble_" + stage + ".bmp"));
            }
            looks.put(mascot, new Looks(poses, List.copyOf(bubbles), frames(id + "_needle.bmp"),
                    frames(id + "_die_2.bmp")));
        }
        return new CharacterSprites(looks);
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
        Pose pose = looksOf(mascot, color).poses().get(facing);
        if (!moving) {
            return pose.standing();
        }
        List<BufferedImage> walking = pose.walking();
        return walking.get(elapsedFrames(walkTime, WALK_FRAME_SECONDS) % walking.size());
    }

    /**
     * 물풍선에 갇힌 지 seconds초 된 그림. 갇힌 시간({@link Player#TRAP_SECONDS})을 셋으로 나눠 bubble_1, 2, 3 파일을 차례로 쓴다.
     *
     * @param color 고정색. null이면 원래 색 그림이다
     */
    public BufferedImage trapped(Mascot mascot, CharacterColor color, double seconds) {
        List<List<BufferedImage>> bubbles = looksOf(mascot, color).bubbles();
        double stageSeconds = Player.TRAP_SECONDS / BUBBLE_STAGES;
        int stage = Math.min(BUBBLE_STAGES - 1, elapsedFrames(seconds, stageSeconds));
        List<BufferedImage> frames = bubbles.get(stage);
        return frames.get(elapsedFrames(seconds, BUBBLE_FRAME_SECONDS) % frames.size());
    }

    /**
     * 갇힌 물풍선이 깨져 빠져나오기 시작한 지 seconds초 된 그림. 빠져나오는 시간({@link Player#ESCAPE_SECONDS}) 동안
     * needle 그림을 한 번 보여 주고, 그 뒤로는 마지막 장(서 있는 모습)이다.
     */
    public BufferedImage escaping(Mascot mascot, CharacterColor color, double seconds) {
        List<BufferedImage> needle = looksOf(mascot, color).needle();
        int index = elapsedFrames(seconds, Player.ESCAPE_SECONDS / needle.size());
        return needle.get(Math.min(needle.size() - 1, index));
    }

    /** 탈락한 지 seconds초 된 쓰러진 그림. 머리 위에 별이 돈다 */
    public BufferedImage dying(Mascot mascot, CharacterColor color, double seconds) {
        List<BufferedImage> dizzy = looksOf(mascot, color).dizzy();
        return dizzy.get(elapsedFrames(seconds, DIZZY_FRAME_SECONDS) % dizzy.size());
    }

    private static int elapsedFrames(double seconds, double frameSeconds) {
        return (int) Math.floor(seconds / frameSeconds + FRAME_BOUNDARY_SLACK);
    }

    private Looks looksOf(Mascot mascot, CharacterColor color) {
        Looks original = looks.get(mascot);
        if (color == null) {
            return original;
        }
        return recolored.computeIfAbsent(new Colored(mascot, color), key -> original.recolor(mascot, color));
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

    /** 한 캐릭터의 그림 전부 */
    private record Looks(Map<Direction, Pose> poses, List<List<BufferedImage>> bubbles, List<BufferedImage> needle,
                         List<BufferedImage> dizzy) {

        Looks recolor(Mascot mascot, CharacterColor color) {
            Map<Direction, Pose> recoloredPoses = new EnumMap<>(Direction.class);
            poses.forEach((direction, pose) -> recoloredPoses.put(direction, new Pose(
                    CharacterRecolor.recolor(pose.standing(), mascot, color), recolor(pose.walking(), mascot, color))));
            return new Looks(recoloredPoses, bubbles.stream().map(frames -> recolor(frames, mascot, color)).toList(),
                    recolor(needle, mascot, color), recolor(dizzy, mascot, color));
        }

        private static List<BufferedImage> recolor(List<BufferedImage> frames, Mascot mascot, CharacterColor color) {
            return frames.stream().map(frame -> CharacterRecolor.recolor(frame, mascot, color)).toList();
        }
    }

    private record Colored(Mascot mascot, CharacterColor color) {
    }
}

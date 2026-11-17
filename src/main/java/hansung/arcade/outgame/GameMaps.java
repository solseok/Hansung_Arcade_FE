package hansung.arcade.outgame;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * 아웃게임에서 고를 수 있는 맵: 맵 파일 id(resources/maps/{id}.txt), 화면에 쓰는 이름, 테마, 최대 인원.
 * 캠프·패트릿은 8명, 빌리지는 6명까지다. 랜덤 맵은 게임을 시작할 때 사람 수가 들어가는 맵 중 하나로 정한다.
 */
public final class GameMaps {

    public record GameMap(String id, String name, String theme, int maxPlayers) {
        public boolean isRandom() {
            return RANDOM_ID.equals(id);
        }
    }

    public static final List<GameMap> ALL = List.of(
            new GameMap("camp/camp08", "캠프 08", "캠프", 8),
            new GameMap("patrit/patrit14", "패트릿 14", "패트릿", 8),
            new GameMap("village/village10", "빌리지 10", "빌리지", 6));
    public static final String DEFAULT_ID = "camp/camp08";
    public static final String RANDOM_ID = "random";
    /** 랜덤 맵. 어느 맵이 될지 모르므로 최대 인원은 가장 큰 맵에 맞춘다 */
    public static final GameMap RANDOM = new GameMap(RANDOM_ID, "랜덤 맵", "??",
            ALL.stream().mapToInt(GameMap::maxPlayers).max().orElse(8));

    private GameMaps() {
    }

    /** 실제 맵 id (랜덤 제외) */
    public static List<String> ids() {
        return ALL.stream().map(GameMap::id).toList();
    }

    /** 맵선택에서 고를 수 있는 것: 실제 맵들 + 랜덤 */
    public static List<GameMap> choices() {
        return Stream.concat(ALL.stream(), Stream.of(RANDOM)).toList();
    }

    public static GameMap byId(String id) {
        return choices().stream().filter(map -> map.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 맵입니다: " + id));
    }

    /** 화면 이름(캠프 08 등)의 맵. 모르는 이름이면 기본 맵 */
    public static GameMap byName(String name) {
        return choices().stream().filter(map -> map.name().equals(name)).findFirst().orElse(byId(DEFAULT_ID));
    }

    /** players명이 들어가는 실제 맵 중 하나 */
    public static GameMap pick(int players, Random random) {
        List<GameMap> fits = ALL.stream().filter(map -> map.maxPlayers() >= players).toList();
        return fits.get(random.nextInt(fits.size()));
    }
}

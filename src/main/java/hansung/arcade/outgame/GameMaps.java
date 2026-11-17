package hansung.arcade.outgame;

import java.util.List;

public final class GameMaps {
    public static final String RANDOM_ID = "random";

    public record MapInfo(String id, String label, String theme, int maxPlayers) {}

    public static final MapInfo RANDOM = new MapInfo(RANDOM_ID, "랜덤 맵", "??", 8);
    private static final List<MapInfo> MAPS =
            List.of(
                    new MapInfo("camp/camp08", "캠프 08", "캠프", 8),
                    new MapInfo("patrit/patrit14", "패트릿 14", "패트릿", 8),
                    new MapInfo("village/village10", "빌리지 10", "빌리지", 6),
                    RANDOM);

    private GameMaps() {}

    public static List<String> ids() {
        return MAPS.stream().map(MapInfo::id).toList();
    }

    public static MapInfo byId(String id) {
        return MAPS.stream()
                .filter(m -> m.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 맵입니다."));
    }

    public static MapInfo byLabel(String label) {
        return MAPS.stream()
                .filter(m -> m.label().equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 맵입니다."));
    }
}

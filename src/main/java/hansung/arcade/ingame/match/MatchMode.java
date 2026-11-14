package hansung.arcade.ingame.match;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 경기 방식. 승패를 가르는 "편"이 다르다: 개인전은 플레이어 한 명이 한 편, 팀전은 같은 팀 번호끼리 한 편이다.
 */
public enum MatchMode {
    /** 개인전. 마지막까지 살아남은 한 명만 이긴다 */
    SOLO("solo", "개인전"),
    /** 팀전. 마지막까지 살아남은 팀이 이긴다 (먼저 탈락한 팀원도 함께 이긴다) */
    TEAM("team", "팀전");

    private final String id;
    private final String label;

    MatchMode(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /** 설정값에 쓰는 이름 */
    public String id() {
        return id;
    }

    /** 로그 등에 쓰는 한국어 이름 */
    public String label() {
        return label;
    }

    /** 설정값 등에 적힌 id(대소문자 무시)에 해당하는 경기 방식 */
    public static MatchMode fromId(String id) {
        String wanted = id.strip().toLowerCase(Locale.ROOT);
        for (MatchMode mode : values()) {
            if (mode.id.equals(wanted)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("알 수 없는 경기 방식 '%s'입니다. 가능한 값: %s".formatted(id,
                Arrays.stream(values()).map(MatchMode::id).collect(Collectors.joining(", "))));
    }
}

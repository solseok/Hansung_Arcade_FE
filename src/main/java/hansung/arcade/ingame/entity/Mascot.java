package hansung.arcade.ingame.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 고를 수 있는 마스코트 캐릭터. {@link #id()}는 resources/sprites/characters/ 의 파일 이름 앞부분이다.
 * 캐릭터마다 물풍선 개수·물줄기 길이·이동속도 단계의 시작값과 아이템으로 올릴 수 있는 최댓값이 다르다.
 */
public enum Mascot {
    // 능력치는 (시작값, 최댓값) 순서로 물풍선 개수, 물줄기 길이(칸), 이동속도 단계
    SANGSANGBUGI("sangsangbugi", new StatRange(1, 6), new StatRange(1, 7), new StatRange(5, 9)),
    KKUKKU("kkukku", new StatRange(1, 10), new StatRange(1, 8), new StatRange(4, 8)),
    KKOKKO("kkokko", new StatRange(1, 8), new StatRange(1, 5), new StatRange(5, 8)),
    SANGZZI("sangzzi", new StatRange(1, 6), new StatRange(2, 7), new StatRange(5, 8)),
    HANSUNGNYANGI("hansungnyangi", new StatRange(2, 7), new StatRange(1, 9), new StatRange(4, 8));

    /** 캐릭터를 고르지 않았을 때 쓰는 기본 캐릭터 */
    public static final Mascot DEFAULT = SANGSANGBUGI;

    private final String id;
    private final StatRange balloons;
    private final StatRange waterRange;
    private final StatRange speed;

    Mascot(String id, StatRange balloons, StatRange waterRange, StatRange speed) {
        if (speed.base() < SpeedLevel.MIN || speed.max() > SpeedLevel.MAX) {
            throw new IllegalArgumentException("%s 의 이동속도 단계가 %d~%d 를 벗어납니다."
                    .formatted(id, SpeedLevel.MIN, SpeedLevel.MAX));
        }
        this.id = id;
        this.balloons = balloons;
        this.waterRange = waterRange;
        this.speed = speed;
    }

    public String id() {
        return id;
    }

    /** 한 번에 놓아 둘 수 있는 물풍선 개수 */
    public StatRange balloons() {
        return balloons;
    }

    /** 물풍선이 터질 때 상하좌우로 뻗는 물줄기 길이(칸) */
    public StatRange waterRange() {
        return waterRange;
    }

    /**
     * 이동속도 단계. 실제 속도는 {@link SpeedLevel}로 바꾼다.
     * 다른 능력치와 달리 느려질 때는 시작값 아래({@link SpeedLevel#MIN})까지 내려갈 수 있다.
     * 그래서 범위를 맞출 때 {@link StatRange#clamp}가 아니라 {@link Player#addSpeedLevel}의 규칙을 따른다.
     */
    public StatRange speed() {
        return speed;
    }

    /** 설정값 등에 적힌 id(대소문자 무시)에 해당하는 캐릭터 */
    public static Mascot fromId(String id) {
        String wanted = id.strip().toLowerCase(Locale.ROOT);
        for (Mascot mascot : values()) {
            if (mascot.id.equals(wanted)) {
                return mascot;
            }
        }
        throw new IllegalArgumentException("알 수 없는 캐릭터 '%s'입니다. 가능한 값: %s".formatted(id,
                Arrays.stream(values()).map(Mascot::id).collect(Collectors.joining(", "))));
    }
}

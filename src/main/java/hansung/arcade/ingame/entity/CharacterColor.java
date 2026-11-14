package hansung.arcade.ingame.entity;

import hansung.arcade.core.ui.Oklch;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 캐릭터의 고정색(남색 모자·몸, 빨간 가방처럼 캐릭터마다 정해진 부분)을 바꿔 칠할 색.
 * 크레이지아케이드에서 팀 색에 따라 옷 색이 바뀌는 것과 같다. 색을 고르지 않으면 원래 그림 색 그대로다.
 *
 * <p>하늘과 마젠타는 정해 준 sRGB 값이다. 나머지는 OKLCH로 정했는데, 두 색처럼 sRGB로 낼 수 있는 가장 진한 채도의
 * 70~90% 정도로 맞추고 밝기는 색마다 그 색으로 보이는 값을 골랐다(노랑은 밝아야 노랑으로 보여 0.88).
 * 각 줄 주석의 16진수는 그 OKLCH 값을 sRGB로 바꾼 결과다.</p>
 */
public enum CharacterColor {
    /** OKLCH(0.60, 0.21, 27) → #E23532 */
    RED("red", new Oklch(0.60, 0.21, 27)),
    /** OKLCH(0.88, 0.165, 97) → #F6D73C */
    YELLOW("yellow", new Oklch(0.88, 0.165, 97)),
    /** OKLCH(0.72, 0.17, 52) → #F58028 */
    ORANGE("orange", new Oklch(0.72, 0.17, 52)),
    /** OKLCH(0.68, 0.18, 145) → #3DB44A */
    GREEN("green", new Oklch(0.68, 0.18, 145)),
    /** sRGB(83, 182, 195) #53B6C3 = OKLCH(0.722, 0.094, 206.9) */
    SKY("sky", Oklch.fromRgb(0x53B6C3)),
    /** OKLCH(0.55, 0.20, 262) → #2867E4 */
    BLUE("blue", new Oklch(0.55, 0.20, 262)),
    /** OKLCH(0.55, 0.21, 302) → #8A45D5 */
    PURPLE("purple", new Oklch(0.55, 0.21, 302)),
    /** sRGB(213, 47, 143) #D52F8F = OKLCH(0.595, 0.217, 350.7) */
    MAGENTA("magenta", Oklch.fromRgb(0xD52F8F));

    private final String id;
    private final Oklch oklch;

    CharacterColor(String id, Oklch oklch) {
        if (!oklch.isInGamut()) {
            throw new IllegalArgumentException(id + " 색이 sRGB 범위를 벗어납니다: " + oklch);
        }
        this.id = id;
        this.oklch = oklch;
    }

    /** 설정값(player.color)에 쓰는 이름 */
    public String id() {
        return id;
    }

    public Oklch oklch() {
        return oklch;
    }

    /** sRGB 0xRRGGBB */
    public int rgb() {
        return oklch.toRgb();
    }

    /** 설정값 등에 적힌 id(대소문자 무시)에 해당하는 색 */
    public static CharacterColor fromId(String id) {
        String wanted = id.strip().toLowerCase(Locale.ROOT);
        for (CharacterColor color : values()) {
            if (color.id.equals(wanted)) {
                return color;
            }
        }
        throw new IllegalArgumentException("알 수 없는 색 '%s'입니다. 가능한 값: %s".formatted(id,
                Arrays.stream(values()).map(CharacterColor::id).collect(Collectors.joining(", "))));
    }
}

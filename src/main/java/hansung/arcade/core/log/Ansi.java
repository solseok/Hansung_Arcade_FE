package hansung.arcade.core.log;

import hansung.arcade.core.ui.HsuColor;

/**
 * 터미널 글자색(ANSI 24비트 트루컬러) 출력.
 * 색이 깨져 보이는 터미널에서는 환경변수 NO_COLOR=1 로 끌 수 있다 (https://no-color.org).
 */
public final class Ansi {

    private static final String ESC = "\u001b[";
    private static final String RESET = ESC + "0m";
    private static final boolean ENABLED = isBlank(System.getenv("NO_COLOR"));

    private Ansi() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    /** 색이 켜져 있으면 글자를 칠하고, 꺼져 있거나 빈 문자열이면 그대로 돌려준다. */
    public static String paint(String text, AnsiStyle style) {
        if (!ENABLED || text.isEmpty()) {
            return text;
        }
        return code(style) + text + RESET;
    }

    public static String paint(String text, HsuColor color) {
        return paint(text, AnsiStyle.of(color));
    }

    static String code(AnsiStyle style) {
        int rgb = style.rgb();
        return ESC + (style.bold() ? "1;" : "")
                + "38;2;" + ((rgb >> 16) & 0xFF) + ";" + ((rgb >> 8) & 0xFF) + ";" + (rgb & 0xFF) + "m";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

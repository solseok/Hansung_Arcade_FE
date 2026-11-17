package hansung.arcade.outgame;

import java.text.Normalizer;

public final class NicknameRule {
    public static final int MAX_LENGTH = 8;
    public static final String DIGITS_MESSAGE = "숫자만 사용할 수 없습니다.";
    public static final String LENGTH_MESSAGE = "닉네임은 2~8자로 입력해 주세요.";
    public static final String HANGUL_MESSAGE = "완성된 한글로 입력해 주세요.";

    private NicknameRule() {}

    public static String validate(String input) {
        String name = Normalizer.normalize(input == null ? "" : input.strip(), Normalizer.Form.NFC);
        int length = name.codePointCount(0, name.length());
        if (length < 2 || length > 8) throw new IllegalArgumentException(LENGTH_MESSAGE);
        if (name.codePoints()
                .anyMatch(
                        c ->
                                c >= 0x1100 && c <= 0x11ff
                                        || c >= 0x3130 && c <= 0x318f
                                        || c >= 0xa960 && c <= 0xa97f
                                        || c >= 0xd7b0 && c <= 0xd7ff
                                        || c >= 0xffa0 && c <= 0xffdc))
            throw new IllegalArgumentException(HANGUL_MESSAGE);
        if (name.codePoints().allMatch(Character::isDigit))
            throw new IllegalArgumentException(DIGITS_MESSAGE);
        return name;
    }
}

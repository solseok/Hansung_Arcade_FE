package hansung.arcade.outgame;

import java.text.Normalizer;

/**
 * 닉네임 규칙: 앞뒤 공백을 뗀 뒤 2~8자, 자음·모음 낱자(ㅁㄴㅇㄹ, ㅏ 등)가 없는 완성된 글자이고 숫자로만 되어 있지 않다.
 * 8자는 인게임 플레이어 목록의 닉네임 칸에 들어가는 길이다.
 * 오류 문구는 로그인 입력 영역 아래 한 줄에 그대로 보여 주므로 짧게 쓴다.
 */
public final class NicknameRule {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 8;
    public static final String LENGTH_MESSAGE = "닉네임은 2~8자로 입력해 주세요.";
    public static final String HANGUL_MESSAGE = "낱자 없이 완성된 한글로 입력해 주세요.";
    public static final String DIGITS_MESSAGE = "숫자로만 된 닉네임은 쓸 수 없습니다.";

    private NicknameRule() {
    }

    /**
     * 규칙에 맞으면 다듬은 닉네임을 돌려준다.
     * macOS에서 붙여 넣은 글은 완성 글자가 자모로 풀려(NFD) 들어올 수 있어서 먼저 합친다(NFC).
     *
     * @throws IllegalArgumentException 규칙에 어긋나면. 메시지를 그대로 화면에 보여 줄 수 있다
     */
    public static String validate(String raw) {
        String nickname = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFC).strip();
        int length = nickname.codePointCount(0, nickname.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException(LENGTH_MESSAGE);
        }
        if (nickname.codePoints().anyMatch(NicknameRule::isJamo)) {
            throw new IllegalArgumentException(HANGUL_MESSAGE);
        }
        // 전각 숫자(１２３)처럼 아라비아 숫자가 아닌 숫자도 숫자로 본다
        if (nickname.codePoints().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException(DIGITS_MESSAGE);
        }
        return nickname;
    }

    /** 한글 자모 낱자: 조합용(첫가끝), 호환용(ㄱ~ㅣ), 확장 A·B, 반각 */
    static boolean isJamo(int codePoint) {
        return (codePoint >= 0x1100 && codePoint <= 0x11FF)
                || (codePoint >= 0x3130 && codePoint <= 0x318F)
                || (codePoint >= 0xA960 && codePoint <= 0xA97F)
                || (codePoint >= 0xD7B0 && codePoint <= 0xD7FF)
                || (codePoint >= 0xFFA0 && codePoint <= 0xFFDC);
    }
}

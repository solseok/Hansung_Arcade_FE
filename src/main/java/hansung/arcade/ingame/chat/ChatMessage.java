package hansung.arcade.ingame.chat;

import java.text.Normalizer;
import java.util.Objects;

/**
 * 채팅 한 줄.
 *
 * @param senderId 보낸 플레이어의 번호. 내 채팅인지 가리고 말풍선을 띄울 플레이어를 찾는 데 쓴다
 * @param sender   보낸 플레이어의 닉네임
 * @param text     내용
 */
public record ChatMessage(int senderId, String sender, String text) {

    /** 한 번에 보낼 수 있는 내용의 길이. {@link #byteLength}로 센다 */
    public static final int MAX_BYTES = 64;

    public ChatMessage {
        Objects.requireNonNull(sender);
        Objects.requireNonNull(text);
    }

    /**
     * 입력 제한에 쓰는 길이. 옛 한글 인코딩(EUC-KR)처럼 영문·숫자·기호(ASCII)는 1, 한글을 비롯한 그 밖의 글자는 2로 센다.
     * UTF-16 단위로 세므로 이모지 같은 보충 문자는 4가 된다. 한글과 영문은 돋움체에서 차지하는 칸 수(영문 1칸, 한글 2칸)와 같다.
     *
     * <p>서버로 보낼 때의 실제 바이트 수와는 별개다. UTF-8로 보내면 한글 한 글자가 3바이트라 64를 넘을 수 있다.</p>
     */
    public static int byteLength(String text) {
        int bytes = 0;
        for (int i = 0; i < text.length(); i++) {
            bytes += text.charAt(i) < 0x80 ? 1 : 2;
        }
        return bytes;
    }

    /**
     * 채팅 목록에 올릴 수 있는 모양으로 다듬은 내용: {@link #singleLine 한 줄로 만들고}, 앞뒤의 공백과 빈칸으로 보이는 글자를 떼고,
     * {@link #MAX_BYTES}를 넘는 뒷부분은 자른다. 다듬고 나서 비어 있으면 올릴 것이 없다.
     */
    public static String clean(String raw) {
        return trimBlank(truncateToBytes(trimBlank(singleLine(raw)), MAX_BYTES));
    }

    /**
     * 한 줄로 만들고 보이지 않는 글자를 뺀다. 채팅은 한 줄이라 붙여넣은 여러 줄 글도 한 줄로 만든다.
     * <ul>
     *   <li>자모를 풀어쓴 한글(NFD, 맥의 파일 이름 등)은 한 글자로 합친다</li>
     *   <li>줄바꿈·탭 같은 제어 문자와 줄·문단 구분 문자는 공백으로 바꾼다</li>
     *   <li>서식 문자(폭 없는 공백, 글자 방향 바꾸기 등)와 짝이 맞지 않는 서로게이트는 지운다.
     *       이모지를 이어 붙이는 문자(ZWJ)도 서식 문자라, 이어 붙인 이모지는 낱개 이모지로 나뉜다</li>
     * </ul>
     */
    public static String singleLine(String text) {
        String composed = Normalizer.normalize(text, Normalizer.Form.NFC);
        StringBuilder line = new StringBuilder(composed.length());
        composed.codePoints().forEach(codePoint -> {
            switch (Character.getType(codePoint)) {
                case Character.CONTROL, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR -> line.append(' ');
                case Character.FORMAT, Character.SURROGATE -> {
                    // 지운다
                }
                default -> line.appendCodePoint(codePoint);
            }
        });
        return line.toString();
    }

    /** text 앞쪽에서 maxBytes 바이트 안에 들어가는 만큼. 이모지 같은 서로게이트 쌍의 가운데는 자르지 않는다. */
    public static String truncateToBytes(String text, int maxBytes) {
        int bytes = 0;
        int end = 0;
        while (end < text.length()) {
            int next = text.offsetByCodePoints(end, 1);
            bytes += byteLength(text.substring(end, next));
            if (bytes > maxBytes) {
                break;
            }
            end = next;
        }
        return text.substring(0, end);
    }

    /** 앞뒤에서 {@link #isBlank 빈칸으로 보이는 글자}를 뗀다. 그런 글자뿐인 글은 빈 글이 된다. */
    private static String trimBlank(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && isBlank(text.codePointAt(start))) {
            start += Character.charCount(text.codePointAt(start));
        }
        while (end > start && isBlank(text.codePointBefore(end))) {
            end -= Character.charCount(text.codePointBefore(end));
        }
        return text.substring(start, end);
    }

    /** 공백(줄 바꿈 없는 공백 포함)이거나, 글자로 분류되지만 아무것도 찍히지 않는 채움 문자인지 */
    private static boolean isBlank(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)
                || codePoint == 0x3164      // 한글 채움 문자
                || codePoint == 0x115F      // 한글 초성 채움 문자
                || codePoint == 0x1160      // 한글 중성 채움 문자
                || codePoint == 0xFFA0      // 반각 한글 채움 문자
                || codePoint == 0x2800;     // 점자 빈칸
    }
}

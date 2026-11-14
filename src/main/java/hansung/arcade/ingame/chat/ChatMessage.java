package hansung.arcade.ingame.chat;

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
     * 옛 한글 인코딩(EUC-KR)처럼 영문·숫자·기호는 1바이트, 한글을 비롯한 그 밖의 글자는 2바이트로 센 길이.
     * 돋움체에서 글자가 차지하는 칸 수(영문 1칸, 한글 2칸)와 같아서 화면에 그려지는 폭과도 맞는다.
     */
    public static int byteLength(String text) {
        int bytes = 0;
        for (int i = 0; i < text.length(); i++) {
            bytes += text.charAt(i) < 0x80 ? 1 : 2;
        }
        return bytes;
    }

    /**
     * 보낼 수 있는 모양으로 다듬은 내용: 줄바꿈·탭 같은 제어 문자는 공백으로 바꾸고, 앞뒤 공백을 떼고,
     * {@link #MAX_BYTES}를 넘는 뒷부분은 자른다. 다듬고 나서 비어 있으면 보낼 것이 없다.
     */
    public static String clean(String raw) {
        return truncateToBytes(singleLine(raw).strip(), MAX_BYTES).stripTrailing();
    }

    /** 줄바꿈·탭 같은 제어 문자를 공백으로 바꾼다. 채팅은 한 줄이라 붙여넣은 여러 줄 글도 한 줄로 만든다. */
    public static String singleLine(String text) {
        StringBuilder line = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            line.append(Character.isISOControl(c) ? ' ' : c);
        }
        return line.toString();
    }

    /** text 앞쪽에서 maxBytes 바이트 안에 들어가는 만큼. 이모지처럼 두 칸짜리 글자의 가운데는 자르지 않는다. */
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
}

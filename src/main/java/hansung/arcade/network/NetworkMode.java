package hansung.arcade.network;

import java.util.Locale;

public enum NetworkMode {
    /** 서버 없이 혼자 실행 */
    OFFLINE,
    /** 게임 서버에 접속 */
    ONLINE;

    public static NetworkMode from(String value) {
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("network.mode는 offline 또는 online이어야 합니다: " + value, e);
        }
    }
}

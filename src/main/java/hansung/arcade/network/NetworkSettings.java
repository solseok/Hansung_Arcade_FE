package hansung.arcade.network;

import java.util.Objects;

/**
 * 서버 접속 설정.
 */
public record NetworkSettings(NetworkMode mode, String host, int port) {

    public NetworkSettings {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(host);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("network.port는 1~65535 사이여야 합니다: " + port);
        }
    }

    public boolean isOnline() {
        return mode == NetworkMode.ONLINE;
    }
}

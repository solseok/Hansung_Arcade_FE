package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameProperties;
import hansung.arcade.network.NetworkMode;
import hansung.arcade.network.NetworkSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 2단계: 네트워크 설정을 읽어 {@link NetworkSettings}를 등록한다.
 * 서버 접속(클라이언트 소켓 생성·연결)은 서버가 준비되면 이 단계에 붙인다.
 */
public final class NetworkStep implements BootStep {

    private static final Logger log = LoggerFactory.getLogger(NetworkStep.class);

    @Override
    public String name() {
        return "네트워크 준비";
    }

    @Override
    public void run(GameContext context) {
        GameProperties properties = context.get(GameProperties.class);
        NetworkSettings settings = new NetworkSettings(
                NetworkMode.from(properties.get("network.mode", "offline")),
                properties.get("network.host", "localhost"),
                properties.getInt("network.port", 9000));
        context.register(NetworkSettings.class, settings);

        if (!settings.isOnline()) {
            log.info("오프라인 모드: 서버에 접속하지 않습니다.");
            return;
        }
        throw new UnsupportedOperationException(
                "온라인 모드(%s:%d)는 아직 구현되지 않았습니다. network.mode=offline으로 실행하세요."
                        .formatted(settings.host(), settings.port()));
    }
}

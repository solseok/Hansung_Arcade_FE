package hansung.arcade.core.boot;

import hansung.arcade.core.game.GameContext;
import hansung.arcade.core.game.GameProperties;
import hansung.arcade.network.NetworkMode;
import hansung.arcade.network.NetworkSettings;
import hansung.arcade.network.ServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 2단계: 네트워크 설정을 읽어 {@link NetworkSettings}를 등록한다.
 *
 * <p>온라인 모드면 {@link ServerClient}도 등록한다. 접속은 여기서 하지 않고 로그인할 때 한다
 * (서버가 꺼져 있어도 게임 창은 뜨고, 로그인 화면에서 이유를 보여 준다). 게임을 끝내면 BYE를 보내고 연결을 닫는다.
 * 온라인 모드인데 서버 주소·포트(network.host·network.port, 보통 .env의 SERVER_HOST·SERVER_PORT)가 없으면 실패한다.</p>
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
        NetworkMode mode = NetworkMode.from(properties.get("network.mode", "offline"));
        if (mode == NetworkMode.OFFLINE) {
            // 오프라인은 주소를 쓰지 않으므로 .env가 없어도 된다
            context.register(NetworkSettings.class, new NetworkSettings(mode,
                    properties.get("network.host", "localhost"), properties.getInt("network.port", 9000)));
            log.info("오프라인 모드: 서버에 접속하지 않습니다.");
            return;
        }

        NetworkSettings settings = new NetworkSettings(mode,
                properties.get("network.host"), properties.getInt("network.port"));
        context.register(NetworkSettings.class, settings);
        ServerClient client = new ServerClient(settings);
        context.register(ServerClient.class, client);
        Runtime.getRuntime().addShutdownHook(new Thread(client::close, "server-bye"));
        log.info("온라인 모드: 로그인할 때 서버 {}:{}에 접속합니다.", settings.host(), settings.port());
    }
}

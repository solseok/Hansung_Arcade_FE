package hansung.arcade.outgame;

import hansung.arcade.network.Message;
import hansung.arcade.network.ServerClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 서버에 새로 접속해 LOGIN을 보낸다. 로그인 화면에서 시작할 때마다 새 연결을 쓰고, 실패하면 그 연결을 닫는다
 * (서버가 실패한 연결을 로그인 전 상태로 들고 있지 않게).
 *
 * <pre>
 * → {"type":"LOGIN","id":1,"body":{"nicknames":["상상부기","꾸꾸"]}}
 * ← {"type":"LOGIN","id":1,"ok":true,"body":{"players":[{"playerId":12,"nickname":"상상부기"},{"playerId":13,"nickname":"꾸꾸"}]}}
 * ← {"type":"LOGIN","id":1,"ok":false,"error":{"code":"NICKNAME_DUPLICATE","message":"이미 쓰고 있는 닉네임입니다: 꾸꾸"}}
 * </pre>
 */
public final class ServerLoginService implements LoginService {

    public static final String LOGIN = "LOGIN";

    private record LoginResponse(List<LoginPlayer> players) {
    }

    private final ServerClient client;

    public ServerLoginService(ServerClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public CompletableFuture<List<LoginPlayer>> login(List<String> nicknames) {
        return client.connect()
                .thenCompose(connection -> connection.request(LOGIN, Map.of("nicknames", List.copyOf(nicknames))))
                .thenApply(body -> List.copyOf(Message.read(body, LoginResponse.class).players()))
                .whenComplete((players, failure) -> {
                    if (failure != null) {
                        client.disconnect();
                    }
                });
    }
}

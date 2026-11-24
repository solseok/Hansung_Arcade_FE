package hansung.arcade.outgame;

import hansung.arcade.network.Message;
import hansung.arcade.network.ServerClient;
import hansung.arcade.network.ServerException;

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
        return client.connect().thenCompose(connection -> connection
                .request(LOGIN, Map.of("nicknames", List.copyOf(nicknames)))
                .thenApply(body -> players(body, nicknames))
                // 실패하면 이 로그인에서 연 연결만 닫는다 (그사이 다른 연결이 생겼으면 건드리지 않는다)
                .whenComplete((players, failure) -> {
                    if (failure != null) {
                        client.disconnect(connection);
                    }
                }));
    }

    /** 서버가 보낸 플레이어. 보낸 닉네임 수와 다르면 서버 응답이 잘못된 것으로 보고 실패한다 */
    private static List<LoginPlayer> players(com.fasterxml.jackson.databind.JsonNode body, List<String> nicknames) {
        List<LoginPlayer> players;
        try {
            players = Message.read(body, LoginResponse.class).players();
        } catch (IllegalArgumentException e) {
            players = null;
        }
        if (players == null || players.size() != nicknames.size()) {
            throw new ServerException("BAD_RESPONSE", "서버 응답이 올바르지 않습니다.");
        }
        return List.copyOf(players);
    }
}

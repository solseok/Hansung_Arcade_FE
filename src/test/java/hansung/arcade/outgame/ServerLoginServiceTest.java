package hansung.arcade.outgame;

import com.fasterxml.jackson.databind.JsonNode;
import hansung.arcade.network.FakeServer;
import hansung.arcade.network.NetworkMode;
import hansung.arcade.network.NetworkSettings;
import hansung.arcade.network.ServerClient;
import hansung.arcade.network.ServerConnection;
import hansung.arcade.network.ServerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerLoginServiceTest {

    private FakeServer server;
    private ServerClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new FakeServer();
        client = new ServerClient(new NetworkSettings(NetworkMode.ONLINE, server.host(), server.port()),
                (host, port) -> ServerConnection.open(host, port, new ServerConnection.Timing(1_000, 1_000, 5_000, 15_000)));
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        server.close();
    }

    @Test
    void 닉네임을_LOGIN으로_보내고_서버가_정한_플레이어를_받는다() throws Exception {
        server.respond(request -> FakeServer.ok(request,
                "{\"players\":[{\"playerId\":12,\"nickname\":\"상상부기\"},{\"playerId\":13,\"nickname\":\"꾸꾸\"}]}"));

        List<LoginService.LoginPlayer> players = new ServerLoginService(client)
                .login(List.of("상상부기", "꾸꾸")).get(3, TimeUnit.SECONDS);

        JsonNode sent = server.next("LOGIN");
        assertEquals("[\"상상부기\",\"꾸꾸\"]", sent.path("body").path("nicknames").toString());
        assertEquals(List.of(new LoginService.LoginPlayer(12, "상상부기"), new LoginService.LoginPlayer(13, "꾸꾸")), players);
        assertTrue(client.connection().isPresent(), "로그인한 연결은 로비에서 계속 쓴다");
    }

    @Test
    void 서버가_거절하면_서버_문구로_실패하고_연결을_닫는다() throws Exception {
        server.respond(request -> FakeServer.fail(request, "NICKNAME_DUPLICATE", "이미 쓰고 있는 닉네임입니다: 부기"));

        var result = new ServerLoginService(client).login(List.of("부기"));

        CompletionException thrown = assertThrows(CompletionException.class, () -> result.orTimeout(3, TimeUnit.SECONDS).join());
        ServerException failure = assertInstanceOf(ServerException.class, ServerException.from(thrown));
        assertEquals("NICKNAME_DUPLICATE", failure.code());
        assertEquals("이미 쓰고 있는 닉네임입니다: 부기", failure.getMessage());
        server.next("BYE");
        assertFalse(client.connection().isPresent());
    }

    @Test
    void 서버가_보낸_인원이_다르면_잘못된_응답으로_실패하고_연결을_닫는다() throws Exception {
        server.respond(request -> FakeServer.ok(request, "{\"players\":[]}"));

        var result = new ServerLoginService(client).login(List.of("부기"));

        CompletionException thrown = assertThrows(CompletionException.class, () -> result.orTimeout(3, TimeUnit.SECONDS).join());
        assertEquals("BAD_RESPONSE", ServerException.from(thrown).code());
        server.next("BYE");
        assertFalse(client.connection().isPresent());
    }

    @Test
    void 다시_로그인하면_이전_연결을_닫고_새로_접속한다() throws Exception {
        server.respond(request -> FakeServer.ok(request, "{\"players\":[{\"playerId\":1,\"nickname\":\"부기\"}]}"));
        ServerLoginService login = new ServerLoginService(client);
        login.login(List.of("부기")).get(3, TimeUnit.SECONDS);
        var first = client.connection().orElseThrow();

        login.login(List.of("부기")).get(3, TimeUnit.SECONDS);

        server.next("BYE");
        assertFalse(first.isOpen());
        assertTrue(client.connection().orElseThrow().isOpen());
    }
}

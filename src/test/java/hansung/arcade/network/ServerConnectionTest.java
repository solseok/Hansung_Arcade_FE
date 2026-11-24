package hansung.arcade.network;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConnectionTest {

    /** 응답 0.3초, PING 0.1초 간격으로 줄여서 시험한다 */
    private static final ServerConnection.Timing FAST = new ServerConnection.Timing(1_000, 300, 100, 2_000);

    private FakeServer server;
    private ServerConnection connection;

    @BeforeEach
    void setUp() throws IOException {
        server = new FakeServer();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (connection != null) {
            connection.close();
        }
        server.close();
    }

    private ServerConnection open() {
        connection = ServerConnection.open(server.host(), server.port(), FAST);
        return connection;
    }

    private static ServerException failure(CompletableFuture<?> future) {
        CompletionException thrown = assertThrows(CompletionException.class, () -> future.orTimeout(3, TimeUnit.SECONDS).join());
        return assertInstanceOf(ServerException.class, thrown.getCause());
    }

    @Test
    void 요청에_id를_붙이고_같은_id의_응답_body로_끝난다() throws Exception {
        server.respond(request -> FakeServer.ok(request, "{\"echo\":\"" + request.path("body").path("text").asText() + "\"}"));
        open();

        CompletableFuture<JsonNode> reply = connection.request("ECHO", Map.of("text", "안녕"));

        JsonNode sent = server.next("ECHO");
        assertTrue(sent.path("id").isIntegralNumber());
        assertEquals("안녕", sent.path("body").path("text").asText());
        assertEquals("안녕", reply.get(3, TimeUnit.SECONDS).path("echo").asText());
    }

    @Test
    void 응답이_뒤섞여_와도_id로_짝짓는다() throws Exception {
        AtomicReference<JsonNode> first = new AtomicReference<>();
        server.respond(request -> {
            if (first.compareAndSet(null, request)) {
                return null;
            }
            // 두 번째 요청을 받으면 두 번째 답을 먼저, 첫 번째 답을 나중에 보낸다
            return FakeServer.ok(request, "{\"n\":2}") + "\n" + FakeServer.ok(first.get(), "{\"n\":1}");
        });
        open();

        CompletableFuture<JsonNode> one = connection.request("A", null);
        CompletableFuture<JsonNode> two = connection.request("A", null);

        assertEquals(1, one.get(3, TimeUnit.SECONDS).path("n").asInt());
        assertEquals(2, two.get(3, TimeUnit.SECONDS).path("n").asInt());
    }

    @Test
    void ok_false면_서버의_code와_문구로_실패한다() {
        server.respond(request -> FakeServer.fail(request, "NICKNAME_DUPLICATE", "이미 쓰고 있는 닉네임입니다: 부기"));
        open();

        ServerException failure = failure(connection.request("LOGIN", Map.of("nicknames", List.of("부기"))));

        assertEquals("NICKNAME_DUPLICATE", failure.code());
        assertEquals("이미 쓰고 있는 닉네임입니다: 부기", failure.getMessage());
    }

    @Test
    void 응답이_없으면_시간_초과로_실패한다() {
        open();

        ServerException failure = failure(connection.request("SILENT", null));

        assertEquals(ServerException.TIMEOUT, failure.code());
    }

    @Test
    void 서버가_먼저_보낸_메시지를_type별로_받는다() throws Exception {
        open();
        CompletableFuture<JsonNode> pushed = new CompletableFuture<>();
        connection.onPush("ROOM_STATE", pushed::complete);

        server.push("{\"type\":\"ROOM_STATE\",\"body\":{\"hostId\":7}}");

        assertEquals(7, pushed.get(3, TimeUnit.SECONDS).path("hostId").asInt());
    }

    @Test
    void 응답_없는_메시지는_id_없이_보낸다() throws Exception {
        open();

        connection.send("READY", Map.of("ready", true));

        JsonNode sent = server.next("READY");
        assertFalse(sent.has("id"));
        assertTrue(sent.path("body").path("ready").asBoolean());
    }

    @Test
    void 정해진_간격마다_PING을_보낸다() throws Exception {
        open();

        server.next("PING");
        server.next("PING");
    }

    @Test
    void 닫으면_BYE를_보내고_기다리던_요청은_실패하며_끊김으로_알리지_않는다() throws Exception {
        open();
        List<ServerException> disconnects = new CopyOnWriteArrayList<>();
        connection.onDisconnect(disconnects::add);
        CompletableFuture<JsonNode> waiting = connection.request("SILENT", null);

        connection.close();

        server.next("BYE");
        assertEquals(ServerException.DISCONNECTED, failure(waiting).code());
        assertFalse(connection.isOpen());
        Thread.sleep(100);
        assertTrue(disconnects.isEmpty());
    }

    @Test
    void 보내는_메시지에는_공통_규칙의_필드만_있다() {
        var request = Message.request("PING", 1, null).toJson();
        var notice = Message.notice("BYE", null).toJson();

        assertEquals("{\"type\":\"PING\",\"id\":1,\"body\":{}}", request);
        assertEquals("{\"type\":\"BYE\",\"body\":{}}", notice);
    }

    @Test
    void 서버가_한도를_넘는_줄을_보내면_끊는다() throws Exception {
        open();
        CompletableFuture<ServerException> disconnected = new CompletableFuture<>();
        connection.onDisconnect(disconnected::complete);

        server.push("{\"type\":\"BIG\",\"body\":{\"x\":\"" + "가".repeat(Protocol.MAX_LINE_BYTES) + "\"}}");

        assertEquals(ServerException.DISCONNECTED, disconnected.get(3, TimeUnit.SECONDS).code());
        assertFalse(connection.isOpen());
    }

    @Test
    void 받은_메시지_처리가_Error로_죽어도_연결을_닫고_끊김을_알린다() throws Exception {
        open();
        CompletableFuture<ServerException> disconnected = new CompletableFuture<>();
        connection.onDisconnect(disconnected::complete);
        connection.onPush("BOOM", body -> {
            throw new AssertionError("처리 중 오류");
        });

        server.push("{\"type\":\"BOOM\",\"body\":{}}");

        assertEquals(ServerException.DISCONNECTED, disconnected.get(3, TimeUnit.SECONDS).code());
        assertFalse(connection.isOpen());
    }

    @Test
    void 서버가_끊으면_끊김을_알리고_기다리던_요청은_실패한다() throws Exception {
        // 응답 시간 초과(0.3초)와 끊김 감지가 겹치지 않게 응답 대기를 넉넉히 둔다
        connection = ServerConnection.open(server.host(), server.port(),
                new ServerConnection.Timing(1_000, 5_000, 1_000, 5_000));
        CompletableFuture<ServerException> disconnected = new CompletableFuture<>();
        connection.onDisconnect(disconnected::complete);
        CompletableFuture<JsonNode> waiting = connection.request("SILENT", null);
        server.next("SILENT");

        server.dropClients();

        ServerException reason = disconnected.get(3, TimeUnit.SECONDS);
        assertEquals(ServerException.DISCONNECTED, reason.code());
        assertEquals("서버와 연결이 끊겼습니다.", reason.getMessage());
        assertEquals(ServerException.DISCONNECTED, failure(waiting).code());
    }

    @Test
    void 메시지_모양_버전이_다르면_접속하지_않는다() {
        server.hello("{\"type\":\"HELLO\",\"body\":{\"protocolVersion\":2}}");

        ServerException failure = assertThrows(ServerException.class, this::open);

        assertEquals(ServerException.VERSION_MISMATCH, failure.code());
    }

    @Test
    void HELLO가_오지_않으면_시간_초과다() {
        server.hello(null);

        ServerException failure = assertThrows(ServerException.class, this::open);

        assertEquals(ServerException.TIMEOUT, failure.code());
    }

    @Test
    void 아무도_듣지_않는_포트면_연결할_수_없다고_알린다() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            closedPort = socket.getLocalPort();
        }

        ServerException failure = assertThrows(ServerException.class,
                () -> ServerConnection.open(server.host(), closedPort, FAST));

        assertEquals(ServerException.CONNECT_FAILED, failure.code());
        assertEquals("서버에 연결할 수 없습니다.", failure.getMessage());
    }
}

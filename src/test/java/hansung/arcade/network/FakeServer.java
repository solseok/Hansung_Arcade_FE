package hansung.arcade.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 테스트용 가짜 게임 서버. 접속하면 HELLO를 보내고, 받은 줄을 모아 두며, {@link #respond}로 정한 답을 보낸다.
 * PING에는 기본으로 성공 응답을 보낸다.
 */
public final class FakeServer implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ServerSocket server;
    private final BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();
    private final List<Socket> clients = new CopyOnWriteArrayList<>();
    private volatile String hello = "{\"type\":\"HELLO\",\"body\":{\"protocolVersion\":1}}";
    private volatile Function<JsonNode, String> responder = request -> null;

    public FakeServer() throws IOException {
        server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread acceptor = new Thread(this::acceptLoop, "fake-server-accept");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    public String host() {
        return InetAddress.getLoopbackAddress().getHostAddress();
    }

    public int port() {
        return server.getLocalPort();
    }

    /** 접속 직후 보낼 줄 (null이면 아무것도 보내지 않는다) */
    public FakeServer hello(String line) {
        hello = line;
        return this;
    }

    /** 받은 요청마다 보낼 한 줄. null을 돌려주면 답하지 않는다 (PING은 이것과 상관없이 답한다) */
    public FakeServer respond(Function<JsonNode, String> responder) {
        this.responder = responder;
        return this;
    }

    /** 가장 최근 접속에 한 줄을 보낸다 */
    public void push(String line) throws IOException {
        write(clients.getLast(), line);
    }

    /** 모든 접속을 끊는다 */
    public void dropClients() throws IOException {
        for (Socket client : clients) {
            client.close();
        }
    }

    /** type 메시지가 올 때까지 기다린다 (다른 type은 건너뛴다) */
    public JsonNode next(String type) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline) {
            JsonNode message = received.poll(50, TimeUnit.MILLISECONDS);
            if (message != null && type.equals(message.path("type").asText())) {
                return message;
            }
        }
        throw new AssertionError(type + " 메시지가 오지 않았습니다.");
    }

    public static String ok(JsonNode request, String body) {
        return "{\"type\":\"%s\",\"id\":%d,\"ok\":true,\"body\":%s}"
                .formatted(request.path("type").asText(), request.path("id").asLong(), body);
    }

    public static String fail(JsonNode request, String code, String message) {
        return "{\"type\":\"%s\",\"id\":%d,\"ok\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\"}}"
                .formatted(request.path("type").asText(), request.path("id").asLong(), code, message);
    }

    private void acceptLoop() {
        while (!server.isClosed()) {
            try {
                Socket client = server.accept();
                clients.add(client);
                Thread reader = new Thread(() -> serve(client), "fake-server-client");
                reader.setDaemon(true);
                reader.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    private void serve(Socket client) {
        try (client) {
            if (hello != null) {
                write(client, hello);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                JsonNode message = JSON.readTree(line);
                received.add(message);
                String reply = "PING".equals(message.path("type").asText())
                        ? ok(message, "{}")
                        : responder.apply(message);
                if (reply != null) {
                    write(client, reply);
                }
            }
        } catch (IOException e) {
            // 클라이언트가 끊었다
        }
    }

    private static synchronized void write(Socket client, String line) throws IOException {
        OutputStream out = client.getOutputStream();
        out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    @Override
    public void close() throws IOException {
        server.close();
        dropClients();
    }
}

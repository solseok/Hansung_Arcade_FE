package hansung.arcade.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 설정({@link NetworkSettings})의 서버와의 접속을 들고 있다. 로그인할 때 {@link #connect}로 새로 접속하고,
 * 끊기면 다음 로그인 때 다시 접속한다. 로비·대기실·인게임은 {@link #connection()}으로 같은 연결을 쓴다.
 * 서버가 먼저 보내는 메시지는 {@link #onPush}로 한 번 구독해 두면 다시 접속해도 계속 받는다.
 */
public final class ServerClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ServerClient.class);

    /** 접속하는 방법. 테스트에서 바꿔 끼운다 */
    @FunctionalInterface
    public interface Opener {
        ServerConnection open(String host, int port);
    }

    private final NetworkSettings settings;
    private final Opener opener;
    /** 접속은 기다리는 일이라 EDT가 아닌 이 스레드에서 한다 */
    private final ExecutorService connector = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "server-connect");
        thread.setDaemon(true);
        return thread;
    });
    private final List<Consumer<ServerException>> disconnectListeners = new CopyOnWriteArrayList<>();
    private final Map<String, List<Consumer<JsonNode>>> pushListeners = new ConcurrentHashMap<>();
    private volatile ServerConnection current;

    public ServerClient(NetworkSettings settings) {
        this(settings, ServerConnection::open);
    }

    public ServerClient(NetworkSettings settings, Opener opener) {
        this.settings = Objects.requireNonNull(settings);
        this.opener = Objects.requireNonNull(opener);
    }

    public NetworkSettings settings() {
        return settings;
    }

    /**
     * 새로 접속한다. 들고 있던 연결이 있으면 BYE를 보내고 닫는다. 접속과 HELLO 확인은 백그라운드 스레드에서 한다.
     * 실패하면 {@link ServerException}으로 끝난다.
     */
    public CompletableFuture<ServerConnection> connect() {
        return CompletableFuture.supplyAsync(() -> {
            disconnect();
            ServerConnection connection = opener.open(settings.host(), settings.port());
            connection.onAnyPush((type, body) -> {
                if (current == connection) {
                    pushListeners.getOrDefault(type, List.of()).forEach(listener -> listener.accept(body));
                }
            });
            connection.onDisconnect(reason -> {
                if (current == connection) {
                    current = null;
                    disconnectListeners.forEach(listener -> listener.accept(reason));
                }
            });
            current = connection;
            return connection;
        }, connector);
    }

    /**
     * 서버가 먼저 보내는 type 메시지의 body를 받는다. 지금 연결과 앞으로 다시 접속할 연결 모두에 적용된다.
     * 받는 스레드(server-recv)에서 불리므로 화면을 바꿀 때는 EDT로 넘긴다.
     */
    public void onPush(String type, Consumer<JsonNode> listener) {
        pushListeners.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).add(Objects.requireNonNull(listener));
    }

    /**
     * 지금 연결로 요청을 보내고 응답을 기다린다 (부른 스레드가 멈춘다). 응답은 {@link ServerConnection}의 시간 안에 온다.
     *
     * @throws ServerException 연결이 없거나, 서버가 거절했거나, 시간이 지나면. 문구는 화면에 그대로 띄울 수 있다
     */
    public JsonNode call(String type, Object body) {
        ServerConnection connection = connection()
                .orElseThrow(() -> new ServerException(ServerException.DISCONNECTED, "서버와 연결이 끊겼습니다."));
        try {
            return connection.request(type, body).get();
        } catch (java.util.concurrent.ExecutionException e) {
            throw ServerException.from(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerException(ServerException.DISCONNECTED, "요청을 기다리다 멈췄습니다.", e);
        }
    }

    /** 지금 연결로 응답이 없는 메시지를 보낸다. 연결이 없으면 버린다 */
    public void send(String type, Object body) {
        connection().ifPresentOrElse(connection -> connection.send(type, body),
                () -> log.debug("연결이 없어 {}를 보내지 않습니다.", type));
    }

    /** 지금 열려 있는 연결 */
    public Optional<ServerConnection> connection() {
        ServerConnection connection = current;
        return connection != null && connection.isOpen() ? Optional.of(connection) : Optional.empty();
    }

    /** 들고 있는 연결을 BYE를 보내고 닫는다. 이렇게 닫은 것은 끊김으로 알리지 않는다 */
    public void disconnect() {
        ServerConnection connection = current;
        current = null;
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * 들고 있던 연결이 {@link #disconnect} 없이 끊기면 그 이유를 받는다 (서버가 꺼짐, 15초 무응답 등).
     * EDT가 아닌 스레드에서 불린다.
     */
    public void onDisconnected(Consumer<ServerException> listener) {
        disconnectListeners.add(Objects.requireNonNull(listener));
    }

    /** 게임을 끝낼 때: BYE를 보내고 닫는다 */
    @Override
    public void close() {
        if (current != null) {
            log.info("게임을 끝내며 서버 연결을 닫습니다.");
        }
        disconnect();
        connector.shutdownNow();
    }
}

package hansung.arcade.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 설정({@link NetworkSettings})의 서버와의 접속을 들고 있다. 로그인할 때 {@link #connect}로 새로 접속하고,
 * 끊기면 다음 로그인 때 다시 접속한다. 로비·대기실·인게임은 {@link #connection()}으로 같은 연결을 쓴다.
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
    private final AtomicReference<ServerConnection> current = new AtomicReference<>();

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
            connection.onDisconnect(reason -> {
                // 지금 들고 있는 연결일 때만 알린다 (그사이 새로 접속했으면 옛 연결의 끊김은 알리지 않는다)
                if (current.compareAndSet(connection, null)) {
                    disconnectListeners.forEach(listener -> listener.accept(reason));
                }
            });
            ServerConnection previous = current.getAndSet(connection);
            if (previous != null) {
                previous.close();
            }
            return connection;
        }, connector);
    }

    /** 지금 열려 있는 연결 */
    public Optional<ServerConnection> connection() {
        ServerConnection connection = current.get();
        return connection != null && connection.isOpen() ? Optional.of(connection) : Optional.empty();
    }

    /** 들고 있는 연결을 BYE를 보내고 닫는다. 이렇게 닫은 것은 끊김으로 알리지 않는다 */
    public void disconnect() {
        ServerConnection connection = current.getAndSet(null);
        if (connection != null) {
            connection.close();
        }
    }

    /** connection만 BYE를 보내고 닫는다. 그사이 다른 연결로 바뀌었으면 그 연결은 그대로 둔다 */
    public void disconnect(ServerConnection connection) {
        current.compareAndSet(connection, null);
        connection.close();
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
        if (current.get() != null) {
            log.info("게임을 끝내며 서버 연결을 닫습니다.");
        }
        disconnect();
        connector.shutdownNow();
    }
}

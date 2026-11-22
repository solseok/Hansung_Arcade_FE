package hansung.arcade.network;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 게임 서버와의 TCP 연결 하나. 한 줄에 JSON 메시지 하나를 주고받는다 (네프 API 공통 규칙).
 *
 * <ul>
 *   <li>{@link #open}: 접속하고 서버의 HELLO(메시지 모양 버전)를 확인한다</li>
 *   <li>{@link #request}: id를 붙여 보내고, 같은 id의 응답을 {@link CompletableFuture}로 돌려준다.
 *       {@code ok:false}면 서버의 code·문구를 담은 {@link ServerException}으로 실패한다</li>
 *   <li>{@link #send}: 응답이 없는 메시지(MOVE 등)를 보낸다</li>
 *   <li>{@link #onPush}: 서버가 먼저 보내는 메시지(ROOM_STATE 등)를 받는다</li>
 *   <li>5초마다 PING을 보내고, 15초 동안 아무 줄도 못 받으면 끊긴 것으로 본다. {@link #close}는 BYE를 보내고 닫는다</li>
 * </ul>
 *
 * <p>받는 스레드(server-recv)와 보내는 스레드(server-send)를 따로 둔다. 보낼 줄은 큐에 넣기만 하므로 어느 스레드에서 보내도
 * 기다리지 않고, 넣은 순서대로 나간다. 응답 future와 푸시 listener는 받는 스레드에서 불린다.
 * 화면을 바꿀 때는 EDT로 넘겨야 한다.</p>
 */
public final class ServerConnection implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ServerConnection.class);
    /** 보내는 스레드를 멈추는 표시 (같은 객체인지로 본다) */
    private static final String STOP = new String("STOP");

    /**
     * 기다리는 시간들.
     *
     * @param connectMs  접속과 HELLO를 기다리는 시간
     * @param responseMs 요청의 응답을 기다리는 시간
     * @param pingMs     PING 간격
     * @param idleMs     이 시간 동안 아무 줄도 못 받으면 끊긴 것으로 본다
     */
    public record Timing(int connectMs, int responseMs, int pingMs, int idleMs) {
        public static final Timing DEFAULT = new Timing(3_000, 5_000, Protocol.PING_INTERVAL_MS, Protocol.IDLE_TIMEOUT_MS);
    }

    private record Pending(String type, CompletableFuture<JsonNode> future) {
    }

    private final Socket socket;
    private final BufferedReader reader;
    private final Writer writer;
    private final String address;
    private final Timing timing;
    private final BlockingQueue<String> outbox = new LinkedBlockingQueue<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final Map<Long, Pending> pending = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<JsonNode>>> pushListeners = new ConcurrentHashMap<>();
    private final List<BiConsumer<String, JsonNode>> anyPushListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<ServerException>> disconnectListeners = new CopyOnWriteArrayList<>();
    private final ScheduledThreadPoolExecutor timer;
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile boolean closing;
    private final Thread receiver;
    private final Thread sender;

    private ServerConnection(Socket socket, BufferedReader reader, Writer writer, String address, Timing timing) {
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
        this.address = address;
        this.timing = timing;
        this.timer = new ScheduledThreadPoolExecutor(1, task -> daemon(task, "server-timer"));
        this.timer.setRemoveOnCancelPolicy(true);
        this.receiver = daemon(this::receiveLoop, "server-recv");
        this.sender = daemon(this::sendLoop, "server-send");
    }

    public static ServerConnection open(String host, int port) {
        return open(host, port, Timing.DEFAULT);
    }

    /**
     * 접속하고 HELLO를 받을 때까지 기다린다 (그동안 부른 스레드가 멈춘다. EDT에서 부르지 않는다).
     *
     * @throws ServerException 접속하지 못했거나, HELLO가 오지 않았거나, 서버의 메시지 모양 버전이 다르면
     */
    public static ServerConnection open(String host, int port, Timing timing) {
        String address = host + ":" + port;
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timing.connectMs());
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(timing.connectMs());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            Writer writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            readHello(reader, address);
            socket.setSoTimeout(timing.idleMs());
            ServerConnection connection = new ServerConnection(socket, reader, writer, address, timing);
            connection.start();
            log.info("서버 {}에 접속했습니다.", address);
            return connection;
        } catch (IOException | RuntimeException e) {
            closeQuietly(socket);
            ServerException failure = ServerException.from(e);
            log.warn("서버 {}에 접속하지 못했습니다: {}", address, e.toString());
            throw failure;
        }
    }

    private static void readHello(BufferedReader reader, String address) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new ServerException(ServerException.DISCONNECTED, "서버와 연결이 끊겼습니다.");
        }
        Message hello = Message.parse(line);
        if (!Protocol.HELLO.equals(hello.type())) {
            throw new ServerException(ServerException.VERSION_MISMATCH, "게임 서버가 아닙니다.");
        }
        int version = hello.body().path("protocolVersion").asInt(-1);
        if (version != Protocol.VERSION) {
            log.warn("서버 {}의 메시지 모양 버전 {}이(가) 클라이언트 버전 {}과 다릅니다.", address, version, Protocol.VERSION);
            throw new ServerException(ServerException.VERSION_MISMATCH, "서버와 게임 버전이 맞지 않습니다.");
        }
    }

    private void start() {
        receiver.start();
        sender.start();
        timer.scheduleAtFixedRate(this::ping, timing.pingMs(), timing.pingMs(), TimeUnit.MILLISECONDS);
    }

    /**
     * id를 붙여 요청을 보낸다. 같은 id의 응답이 오면 body로 끝나고, {@code ok:false}·시간 초과·끊김이면
     * {@link ServerException}으로 실패한다.
     *
     * @param body JSON 객체가 되는 record·Map·{@link JsonNode}. null이면 {@code {}}
     */
    public CompletableFuture<JsonNode> request(String type, Object body) {
        long id = nextId.getAndIncrement();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        String line = Message.request(type, id, body).toJson();
        if (!fits(line)) {
            future.completeExceptionally(new IllegalArgumentException(type + " 메시지가 한 줄 최대 길이를 넘습니다."));
            return future;
        }
        pending.put(id, new Pending(type, future));
        if (closed.get()) {
            // 닫히는 중에 넣었으면 닫는 쪽이 이미 pending을 비웠을 수 있다
            failPending(id, disconnected());
            return future;
        }
        outbox.add(line);
        try {
            timer.schedule(() -> failPending(id, new ServerException(ServerException.TIMEOUT, "서버가 응답하지 않습니다.")),
                    timing.responseMs(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            // 그사이 닫혀 시간 재기가 멈췄다
            failPending(id, disconnected());
        }
        return future;
    }

    /** 응답이 없는 메시지를 보낸다. 닫혔으면 버린다 */
    public void send(String type, Object body) {
        String line = Message.notice(type, body).toJson();
        if (!fits(line)) {
            throw new IllegalArgumentException(type + " 메시지가 한 줄 최대 길이를 넘습니다.");
        }
        if (closed.get() || closing) {
            log.debug("닫힌 연결이라 {}를 보내지 않습니다.", type);
            return;
        }
        outbox.add(line);
    }

    /** 서버가 먼저 보내는 type 메시지의 body를 받는다. 받는 스레드에서 불린다 */
    public void onPush(String type, Consumer<JsonNode> listener) {
        pushListeners.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /** 서버가 먼저 보내는 모든 메시지를 (type, body)로 받는다. 받는 스레드에서 불린다 */
    public void onAnyPush(BiConsumer<String, JsonNode> listener) {
        anyPushListeners.add(listener);
    }

    /** {@link #close}를 부르지 않았는데 끊기면 그 이유를 받는다. 받는 스레드나 끊김을 알아챈 스레드에서 한 번 불린다 */
    public void onDisconnect(Consumer<ServerException> listener) {
        disconnectListeners.add(listener);
    }

    public boolean isOpen() {
        return !closed.get();
    }

    /** "host:port" */
    public String address() {
        return address;
    }

    /** BYE를 보내고 닫는다. 기다리던 요청은 모두 실패한다. 여러 번 불러도 된다 */
    @Override
    public void close() {
        if (closed.get() || closing) {
            return;
        }
        closing = true;
        outbox.add(Message.notice(Protocol.BYE, null).toJson());
        outbox.add(STOP);
        if (Thread.currentThread() != sender) {
            try {
                sender.join(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        shutdown(new ServerException(ServerException.DISCONNECTED, "서버와 연결을 닫았습니다."));
    }

    private void ping() {
        if (!closed.get() && !closing) {
            request(Protocol.PING, null).exceptionally(failure -> null);
        }
    }

    private void receiveLoop() {
        ServerException reason;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                dispatch(line);
            }
            reason = disconnected();
        } catch (SocketTimeoutException e) {
            reason = new ServerException(ServerException.TIMEOUT, "서버가 응답하지 않습니다.", e);
        } catch (IOException e) {
            reason = new ServerException(ServerException.DISCONNECTED, "서버와 연결이 끊겼습니다.", e);
        }
        shutdown(reason);
    }

    private void dispatch(String line) {
        Message message;
        try {
            message = Message.parse(line);
        } catch (IllegalArgumentException e) {
            log.warn("읽을 수 없는 줄을 버립니다: {}", e.getMessage());
            return;
        }
        if (message.isResponse()) {
            Pending waiting = pending.remove(message.id());
            if (waiting == null) {
                log.debug("기다리지 않는 응답을 버립니다: {}", message);
                return;
            }
            if (Boolean.TRUE.equals(message.ok())) {
                waiting.future().complete(message.body());
            } else {
                Message.Error error = message.error() == null ? new Message.Error("", "") : message.error();
                log.info("{} 실패: {} {}", waiting.type(), error.code(), error.message());
                waiting.future().completeExceptionally(new ServerException(error.code(), error.message()));
            }
            return;
        }
        if (Protocol.ERROR.equals(message.type())) {
            log.warn("서버 ERROR: {}", message.body());
        }
        List<Consumer<JsonNode>> listeners = pushListeners.getOrDefault(message.type(), List.of());
        if (listeners.isEmpty() && anyPushListeners.isEmpty()) {
            log.debug("받을 곳이 없는 메시지를 버립니다: {}", message.type());
            return;
        }
        for (Consumer<JsonNode> listener : listeners) {
            try {
                listener.accept(message.body());
            } catch (RuntimeException e) {
                log.error("{} 메시지를 처리하다 실패했습니다.", message.type(), e);
            }
        }
        for (BiConsumer<String, JsonNode> listener : anyPushListeners) {
            try {
                listener.accept(message.type(), message.body());
            } catch (RuntimeException e) {
                log.error("{} 메시지를 처리하다 실패했습니다.", message.type(), e);
            }
        }
    }

    private void sendLoop() {
        try {
            while (true) {
                String line = outbox.take();
                if (line == STOP) {
                    break;
                }
                writer.write(line);
                writer.write('\n');
                if (outbox.isEmpty()) {
                    writer.flush();
                }
            }
            writer.flush();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.debug("서버 {}에 보내지 못했습니다: {}", address, e.toString());
        }
        // 보내는 쪽이 멈추면 소켓을 닫아 받는 스레드도 끝나게 한다
        closeQuietly(socket);
    }

    /** 한 번만 정리한다: 기다리던 요청 실패, PING·시간 재기 멈춤, 소켓 닫기, 끊김 알리기 */
    private void shutdown(ServerException reason) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        timer.shutdownNow();
        outbox.add(STOP);
        closeQuietly(socket);
        for (Long id : List.copyOf(pending.keySet())) {
            failPending(id, reason);
        }
        if (closing) {
            log.info("서버 {}와의 연결을 닫았습니다.", address);
            return;
        }
        log.warn("서버 {}와의 연결이 끊겼습니다: {}", address, reason.getMessage());
        for (Consumer<ServerException> listener : disconnectListeners) {
            try {
                listener.accept(reason);
            } catch (RuntimeException e) {
                log.error("끊김을 처리하다 실패했습니다.", e);
            }
        }
    }

    private void failPending(long id, ServerException reason) {
        Pending waiting = pending.remove(id);
        if (waiting != null) {
            waiting.future().completeExceptionally(reason);
        }
    }

    private static ServerException disconnected() {
        return new ServerException(ServerException.DISCONNECTED, "서버와 연결이 끊겼습니다.");
    }

    private static boolean fits(String line) {
        return line.getBytes(StandardCharsets.UTF_8).length <= Protocol.MAX_LINE_BYTES;
    }

    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // 닫는 중의 실패는 할 일이 없다
        }
    }
}

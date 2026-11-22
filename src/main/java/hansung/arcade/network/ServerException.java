package hansung.arcade.network;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 서버와 주고받다가 생긴 실패. 서버가 {@code ok:false}로 거절했으면 서버의 code와 문구를, 연결 문제면 이 클래스의 code를 담는다.
 * {@link #getMessage()}는 화면에 그대로 띄울 수 있는 한국어 한 줄이다.
 */
public final class ServerException extends RuntimeException {

    /** 서버에 연결하지 못함 (주소·포트가 틀렸거나 서버가 꺼져 있음) */
    public static final String CONNECT_FAILED = "CONNECT_FAILED";
    /** 정해진 시간 안에 응답이 없음 */
    public static final String TIMEOUT = "TIMEOUT";
    /** 연결이 끊김 */
    public static final String DISCONNECTED = "DISCONNECTED";
    /** 서버와 메시지 모양 버전이 다름 */
    public static final String VERSION_MISMATCH = "VERSION_MISMATCH";

    private final String code;

    public ServerException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ServerException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * 실패를 화면에 띄울 한 줄로 바꾼다. CompletableFuture가 감싼 예외도 벗겨서 본다.
     */
    public static ServerException from(Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof ServerException server) {
            return server;
        }
        if (cause instanceof TimeoutException || cause instanceof SocketTimeoutException) {
            return new ServerException(TIMEOUT, "서버가 응답하지 않습니다.", cause);
        }
        if (cause instanceof ConnectException || cause instanceof UnknownHostException
                || cause instanceof NoRouteToHostException) {
            return new ServerException(CONNECT_FAILED, "서버에 연결할 수 없습니다.", cause);
        }
        if (cause instanceof IOException) {
            return new ServerException(DISCONNECTED, "서버와 연결이 끊겼습니다.", cause);
        }
        return new ServerException("CLIENT_ERROR", "서버와 주고받지 못했습니다.", cause);
    }
}

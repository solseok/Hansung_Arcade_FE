package hansung.arcade.network;

/**
 * 네프 API 공통 규칙의 숫자와 공통 메시지 type. 서버(Hansung_Arcade_BE)의 core.net.Protocol과 같아야 한다.
 */
public final class Protocol {

    /** 서버가 HELLO로 알리는 메시지 모양 버전. 다르면 접속하지 않는다 */
    public static final int VERSION = 1;

    /** 한 줄 최대 길이 (UTF-8 바이트, \n 제외). 서버는 넘으면 끊는다 */
    public static final int MAX_LINE_BYTES = 32 * 1024;

    /** 서버는 이 시간 동안 아무 줄도 못 받으면 끊는다. 클라이언트도 이만큼 못 받으면 끊긴 것으로 본다 */
    public static final int IDLE_TIMEOUT_MS = 15_000;

    /** 클라이언트가 PING을 보내는 간격 */
    public static final int PING_INTERVAL_MS = 5_000;

    /** 서버 → 클라: 접속 직후 한 번 */
    public static final String HELLO = "HELLO";
    /** 클라 → 서버 → 클라: 연결 확인 (요청 → 응답) */
    public static final String PING = "PING";
    /** 클라 → 서버: 정상 종료 (응답 없음) */
    public static final String BYE = "BYE";
    /** 서버 → 클라: id가 없는 메시지의 실패, 읽을 수 없는 줄 */
    public static final String ERROR = "ERROR";

    private Protocol() {
    }
}

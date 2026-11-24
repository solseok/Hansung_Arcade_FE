package hansung.arcade.ingame.match;

/**
 * 경기 시작 연출 "Ready... Start!". Swing에 의존하지 않는다. 크레이지아케이드 플레이 영상(두 판의 시작을 0.05~0.1초 간격으로 본 것)에서
 * 잰 흐름을 따른다.
 *
 * <ol>
 *   <li>READY({@link #READY_SECONDS}초): 화면이 완전히 검다. Ready 소리가 난다. 글자는 없다.</li>
 *   <li>START({@link #START_SECONDS}초): START!! 글자가 뜬 채로 {@link #FADE_IN_SECONDS}초 동안 검은 화면에서 맵이 밝아지고,
 *       처음부터 {@link #START_HOLD_SECONDS}초가 지나면 남은 시간 동안 왼쪽 글자부터 한 자씩 위로 튀어 올라 사라진다.
 *       Start 소리가 나고, 이 단계부터 경기 시계가 흐른다.</li>
 *   <li>DONE: START!! 글자가 다 사라진 순간부터 플레이어가 조작할 수 있다. 내 캐릭터를 가리키는 화살표는
 *       {@link #ARROW_LINGER_SECONDS}초 더 남았다가 사라진다.</li>
 * </ol>
 *
 * <p>만들 때는 연출을 하지 않은 상태라 바로 경기한다(테스트·이전 동작). 실제 게임은 화면에 띄우기 전에 {@link #begin()}을 부른다.
 * 연출 중에는 키를 눌러도 움직이거나 물풍선을 놓지 않는다 (GameWorld가 맡는다).</p>
 */
public final class StartIntro {

    /** 화면이 완전히 검은 시간(초). Ready 소리가 이 동안 난다 */
    public static final double READY_SECONDS = 0.5;
    /** START!! 글자가 뜬 뒤 검은 화면에서 맵이 다 밝아지는 데 걸리는 시간(초) */
    public static final double FADE_IN_SECONDS = 0.5;
    /** START!! 글자가 뜬 뒤 흩어지기 시작할 때까지(초). 밝아지는 시간을 포함한다 */
    public static final double START_HOLD_SECONDS = 0.75;
    /** START!! 글자가 한 자씩 튀어 올라 다 사라지는 데 걸리는 시간(초) */
    public static final double START_EXIT_SECONDS = 0.75;
    /** START!! 글자가 보이는 시간(초). 이 시간이 지나 글자가 다 사라지면 조작할 수 있다 */
    public static final double START_SECONDS = START_HOLD_SECONDS + START_EXIT_SECONDS;
    /** 연출 전체 길이(초) */
    public static final double TOTAL_SECONDS = READY_SECONDS + START_SECONDS;
    /** START!! 글자가 사라진 뒤에도 내 캐릭터 화살표가 남아 있는 시간(초) */
    public static final double ARROW_LINGER_SECONDS = 0.8;
    /** 1/60초를 계속 더하면 경계에 살짝 못 미칠 수 있어 그만큼은 넘은 것으로 친다 */
    private static final double SLACK = 1e-9;

    /** 연출의 단계 */
    public enum Phase {
        /** 검은 화면에서 밝아지는 중 */
        READY,
        /** START!! 글자 */
        START,
        /** 연출이 끝나 경기 중 */
        DONE
    }

    private boolean begun;
    private double elapsed;

    /** 연출을 처음부터 시작한다 */
    public void begin() {
        begun = true;
        elapsed = 0;
    }

    /**
     * 연출을 바로 끝낸다: 검은 화면·START!! 글자·화살표가 모두 사라진다. 연출 중에 경기를 끝낼 때(목업 버튼) 쓴다.
     * 시작하지 않은 연출은 그대로 둔다.
     */
    public void finish() {
        if (begun) {
            elapsed = Math.max(elapsed, TOTAL_SECONDS + ARROW_LINGER_SECONDS);
        }
    }

    /** 연출 시간을 dt초 흘린다. 연출이 끝난 뒤에도 화살표가 사라질 때까지는 센다 */
    public void advance(double dt) {
        if (begun && dt > 0 && elapsed < TOTAL_SECONDS + ARROW_LINGER_SECONDS) {
            elapsed += dt;
        }
    }

    public Phase phase() {
        if (!begun) {
            return Phase.DONE;
        }
        if (elapsed + SLACK < READY_SECONDS) {
            return Phase.READY;
        }
        if (elapsed + SLACK < TOTAL_SECONDS) {
            return Phase.START;
        }
        return Phase.DONE;
    }

    /** 아직 연출 중이라 조작할 수 없는지 */
    public boolean isRunning() {
        return phase() != Phase.DONE;
    }

    /** 경기 시계가 흐르는 단계인지: START!! 글자가 뜬 뒤부터 */
    public boolean isClockRunning() {
        return phase() != Phase.READY;
    }

    /** 지금 단계가 시작된 뒤 흐른 시간(초). 글자가 나타나고 흩어지는 움직임에 쓴다 */
    public double phaseSeconds() {
        return switch (phase()) {
            case READY -> elapsed;
            case START -> Math.max(0, elapsed - READY_SECONDS);
            case DONE -> begun ? Math.max(0, elapsed - TOTAL_SECONDS) : 0;
        };
    }

    /**
     * 화면을 덮는 검은 막의 불투명도(0~1). READY 동안 1이고, START!! 글자가 뜬 뒤 {@link #FADE_IN_SECONDS}초 동안 0으로 줄어
     * 글자와 맵이 함께 밝아진다. 그 뒤로는 0이다.
     */
    public double blackout() {
        return switch (phase()) {
            case READY -> 1;
            case START -> Math.max(0, 1 - phaseSeconds() / FADE_IN_SECONDS);
            case DONE -> 0;
        };
    }

    /** 내 캐릭터를 가리키는 화살표를 보일 때인지: START!! 글자가 뜬 뒤부터 사라지고 {@link #ARROW_LINGER_SECONDS}초 뒤까지 */
    public boolean showsArrow() {
        return begun && elapsed + SLACK >= READY_SECONDS && elapsed + SLACK < TOTAL_SECONDS + ARROW_LINGER_SECONDS;
    }

    /** 연출을 시작한 뒤 흐른 시간(초) */
    public double elapsedSeconds() {
        return elapsed;
    }
}

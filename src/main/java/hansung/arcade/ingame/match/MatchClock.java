package hansung.arcade.ingame.match;

/**
 * 한 판의 경기 시계. 남은 시간이 {@link #DURATION_SECONDS}초(3:00)에서 시작해 1초씩 줄어든다.
 *
 * <p>화면에 보이는 값({@link #remainingSeconds()})은 남은 시간을 초 단위로 올림한 값이다. 그래서 시작 후 첫 1초 동안은
 * 180(03:00), 1초가 지나는 순간 179(02:59)가 되고, 180초가 지나는 순간 0(00:00)이 되며 시간이 다 된다.</p>
 *
 * <p>시간은 1/{@link #TICKS_PER_SECOND}초(게임 루프 한 번) 단위 정수로 센다. 1/60초를 실수로 계속 더하면
 * 60번 더해도 1.0에 살짝 못 미쳐 표시가 한 틱 늦게 바뀌는 일이 생기므로, 받은 시간을 틱으로 바꿀 때만
 * {@link #TICK_SLACK 여유}를 두고 그 뒤로는 정수로 비교한다. 1초 경계는 언제나 60번째 틱에 정확히 떨어진다.</p>
 *
 * <p>다른 기능은 {@link #remainingSeconds()}로 지금 보이는 남은 초를, {@link #justReached}로 표시가 어떤 값이 된 순간을
 * 알 수 있다. 예) 남은 시간이 1:59가 되는 순간 비행기를 띄우려면 갱신마다 {@code clock.justReached(119)}를 본다.</p>
 */
public final class MatchClock {

    /** 한 판의 길이(초). 3분 */
    public static final int DURATION_SECONDS = 180;
    /** 1초에 세는 틱 수. 게임 루프의 고정 갱신 간격(1/60초)과 같다 */
    public static final int TICKS_PER_SECOND = 60;
    private static final int DURATION_TICKS = DURATION_SECONDS * TICKS_PER_SECOND;
    /**
     * 받은 시간을 틱으로 바꿀 때 경계에 주는 여유(틱). 1/60초를 틱으로 바꾸면 0.9999…처럼 한 틱에 살짝 못 미칠 수 있어,
     * 이만큼 모자라도 한 틱으로 친다. 남은 조각은 다음 갱신으로 넘겨서 쌓인 시간이 틀어지지 않는다.
     */
    private static final double TICK_SLACK = 1e-6;

    private int elapsedTicks;
    /** 아직 한 틱이 되지 않은 시간(틱). 여유를 두고 넘긴 만큼 아주 조금 음수일 수 있다 */
    private double pendingTicks;
    /** 바로 전 {@link #advance} 앞의 표시값. 표시가 바뀐 순간을 알아낼 때 쓴다 */
    private int previousRemaining = DURATION_SECONDS;
    private boolean stopped;

    /**
     * 시간을 dt초만큼 흘린다. 경기 상태({@link Match})가 갱신마다 한 번 부른다.
     * 멈췄거나 시간이 다 됐으면 흐르지 않지만, "이번 갱신에 표시가 바뀌었는지"는 매번 새로 정한다.
     */
    void advance(double dt) {
        previousRemaining = remainingSeconds();
        if (stopped || dt <= 0 || isTimeUp()) {
            return;
        }
        pendingTicks += dt * TICKS_PER_SECOND;
        double whole = Math.floor(pendingTicks + TICK_SLACK);
        if (whole >= 1) {
            elapsedTicks = (int) Math.min(DURATION_TICKS, elapsedTicks + whole);
            pendingTicks -= whole;
        }
    }

    /** 시계를 멈춘다. 경기가 일찍 끝났을 때 부르며, 다시 흐르지 않는다. */
    void stop() {
        stopped = true;
    }

    /**
     * 화면에 보이는 남은 시간(초). 남은 시간을 초 단위로 올림한 값이라 180에서 시작해 1초마다 1씩 줄고,
     * 시간이 다 되면 0이다.
     */
    public int remainingSeconds() {
        int remainingTicks = DURATION_TICKS - elapsedTicks;
        return (remainingTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }

    /** 경기 시작 후 흐른 시간(초). 틱 단위로 끊긴 값이고 시계가 멈추면 더 늘지 않는다. */
    public double elapsedSeconds() {
        return elapsedTicks / (double) TICKS_PER_SECOND;
    }

    /** 경기 시작 후 흐른 틱 수 */
    public int elapsedTicks() {
        return elapsedTicks;
    }

    /** 남은 시간이 0이 됐는지 */
    public boolean isTimeUp() {
        return elapsedTicks >= DURATION_TICKS;
    }

    /** 시간이 다 됐거나 경기가 끝나 더는 흐르지 않는지 */
    public boolean isStopped() {
        return stopped || isTimeUp();
    }

    /** 바로 전 갱신에서 표시값({@link #remainingSeconds()})이 바뀌었는지 */
    public boolean secondChanged() {
        return remainingSeconds() != previousRemaining;
    }

    /**
     * 바로 전 갱신에서 표시값이 seconds가 됐는지. 한 번에 여러 초가 흘러 그 값을 건너뛰었어도 지나쳤으면 참이다.
     * 같은 값에 대해 한 판에 한 번만 참이 된다.
     */
    public boolean justReached(int seconds) {
        return previousRemaining > seconds && remainingSeconds() <= seconds;
    }
}

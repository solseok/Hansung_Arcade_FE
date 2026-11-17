package hansung.arcade.ingame.match;

import java.util.List;
import java.util.Objects;

/**
 * 끝난 한 판의 결과. 아직 서버가 없어 로그로만 남기지만, 서버가 생기면 이 값을 그대로 보내 kill·save를 기록한다.
 *
 * @param mode          경기 방식
 * @param playedSeconds 시작부터 끝날 때까지 흐른 시간(초). 시간이 다 돼서 끝났으면 {@link MatchClock#DURATION_SECONDS}
 * @param players       플레이어별 기록. 플레이어 번호 순이다
 */
public record MatchResult(MatchMode mode, double playedSeconds, List<PlayerResult> players) {

    public MatchResult {
        Objects.requireNonNull(mode);
        players = List.copyOf(players);
    }

    /** playerId 플레이어의 기록 */
    public PlayerResult of(int playerId) {
        return players.stream()
                .filter(player -> player.playerId() == playerId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("이번 판에 없는 플레이어입니다: " + playerId));
    }

    /** playerId 플레이어의 승패 */
    public MatchOutcome outcomeOf(int playerId) {
        return of(playerId).outcome();
    }
}

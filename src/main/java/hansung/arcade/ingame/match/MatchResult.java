package hansung.arcade.ingame.match;

import java.util.List;
import java.util.Objects;

/**
 * 이 컴퓨터가 가린 한 판의 결과 (오프라인). 로그로 남긴다. 온라인 경기는 서버가 승패·kill·save를 가려 GAME_OVER로 알린다.
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

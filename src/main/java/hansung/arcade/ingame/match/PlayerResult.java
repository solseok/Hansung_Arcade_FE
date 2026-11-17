package hansung.arcade.ingame.match;

import java.util.Objects;

/**
 * 한 판이 끝났을 때 플레이어 한 명의 기록. 서버로 그대로 보낼 수 있게 객체 대신 플레이어 번호와 숫자만 담는다.
 *
 * @param playerId 플레이어 번호 ({@code Player.id()})
 * @param outcome  승패
 * @param kills    물풍선에 갇힌 상대를 터뜨린 수
 * @param saves    물풍선에 갇힌 팀원을 구한 수
 */
public record PlayerResult(int playerId, MatchOutcome outcome, int kills, int saves) {

    public PlayerResult {
        Objects.requireNonNull(outcome);
        if (kills < 0 || saves < 0) {
            throw new IllegalArgumentException("kill·save 수는 음수일 수 없습니다: %d, %d".formatted(kills, saves));
        }
    }
}

package hansung.arcade.outgame;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 끝난 한 판의 결과. 결과 모달은 | NAME | KILL | SAVE | 만 보여 주므로 플레이 시간은 두지 않는다.
 * 인게임 Match의 PlayerResult(승패·kill·save)와 같은 항목이다.
 */
public record GameResult(List<Standing> standings) {

    public GameResult {
        standings = List.copyOf(standings);
    }

    public enum Outcome {
        WIN, LOSE, DRAW;

        /** 줄 세우는 순서: 이김 → 비김 → 짐 */
        int order() {
            return switch (this) {
                case WIN -> 0;
                case DRAW -> 1;
                case LOSE -> 2;
            };
        }
    }

    /**
     * @param kills 물풍선에 갇힌 상대를 터뜨린 수
     * @param saves 물풍선에 갇힌 팀원을 구한 수
     */
    public record Standing(int playerId, String nickname, Outcome outcome, int kills, int saves) {
        public Standing {
            Objects.requireNonNull(nickname);
            Objects.requireNonNull(outcome);
            if (kills < 0 || saves < 0) {
                throw new IllegalArgumentException("kill·save 수는 음수일 수 없습니다: %d, %d".formatted(kills, saves));
            }
        }
    }

    /** 순위대로: 승패 → kill 많은 순 → save 많은 순 → 플레이어 번호 */
    public List<Standing> ranked() {
        return standings.stream()
                .sorted(Comparator.comparingInt((Standing s) -> s.outcome().order())
                        .thenComparing(Comparator.comparingInt(Standing::kills).reversed())
                        .thenComparing(Comparator.comparingInt(Standing::saves).reversed())
                        .thenComparingInt(Standing::playerId))
                .toList();
    }
}

package hansung.arcade.outgame;

import java.util.List;

public record GameResult(List<Standing> standings, int winnerId, long playSeconds) {
    public GameResult {
        standings = List.copyOf(standings);
        if (playSeconds < 0) {
            throw new IllegalArgumentException("플레이 시간은 음수일 수 없습니다.");
        }
    }

    public record Standing(int playerId, String nickname, int rank) { }
}

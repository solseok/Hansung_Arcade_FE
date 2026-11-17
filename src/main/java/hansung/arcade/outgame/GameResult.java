package hansung.arcade.outgame;

import java.util.List;

/** 서버가 확정한 참가자별 경기 결과. */
public record GameResult(List<Standing> standings) {
    public GameResult {
        standings = List.copyOf(standings);
    }

    public enum Outcome {
        WIN,
        LOSE
    }

    public record Standing(int playerId, String nickname, Outcome outcome, int kills, int saves) {}
}

package hansung.arcade.outgame;

import java.util.List;
import hansung.arcade.ingame.entity.Mascot;

/**
 * 대기실에서 게임으로 넘기는 정보.
 *
 * @param localPlayers 이 컴퓨터에서 하는 사람 수(1P 또는 2P). players 앞쪽부터 그 수만큼이 이 컴퓨터의 플레이어다
 */
public record GameStartInfo(String mapId, List<PlayerInfo> players, int localPlayers) {
    public GameStartInfo {
        players = List.copyOf(players);
        if (localPlayers < 1 || localPlayers > players.size()) {
            throw new IllegalArgumentException("이 컴퓨터의 플레이어 수가 맞지 않습니다: " + localPlayers);
        }
    }

    public record PlayerInfo(int id, String nickname, Mascot character, String team) { }
}

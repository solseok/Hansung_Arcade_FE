package hansung.arcade.outgame;

import java.util.List;
import hansung.arcade.ingame.entity.Mascot;

/**
 * 대기실에서 게임으로 넘기는 정보.
 *
 * @param localPlayers 이 컴퓨터에서 하는 사람 수(1P 또는 2P). players 앞쪽부터 그 수만큼이 이 컴퓨터의 플레이어다
 * @param online       서버가 연 경기인지 (GAME_STARTED). 그러면 판정은 서버가 하고 players의 번호·스폰은 서버가 정한 값이다
 */
public record GameStartInfo(String mapId, List<PlayerInfo> players, int localPlayers, boolean online) {
    public GameStartInfo {
        players = List.copyOf(players);
        if (localPlayers < 1 || localPlayers > players.size()) {
            throw new IllegalArgumentException("이 컴퓨터의 플레이어 수가 맞지 않습니다: " + localPlayers);
        }
    }

    /** 이 컴퓨터에서 하는 오프라인 게임 */
    public GameStartInfo(String mapId, List<PlayerInfo> players, int localPlayers) {
        this(mapId, players, localPlayers, false);
    }

    /** @param spawn 맵 스폰 번호(1부터). 온라인이면 서버가 정하고, 오프라인이면 0이다 (players 순서대로 선다) */
    public record PlayerInfo(int id, String nickname, Mascot character, String team, int spawn) {
        public PlayerInfo(int id, String nickname, Mascot character, String team) {
            this(id, nickname, character, team, 0);
        }
    }
}

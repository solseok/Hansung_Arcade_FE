package hansung.arcade.outgame;

import java.util.List;
import hansung.arcade.ingame.entity.Mascot;

public record GameStartInfo(String mapId, List<PlayerInfo> players) {
    public GameStartInfo {
        players = List.copyOf(players);
    }

    public record PlayerInfo(int id, String nickname, Mascot character, String team) { }
}

package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.Mascot;

import java.util.List;

public record GameStartInfo(String mapId, List<PlayerInfo> players, int localPlayers) {
    public GameStartInfo(String mapId, List<PlayerInfo> players) {
        this(mapId, players, 1);
    }

    public GameStartInfo {
        players = List.copyOf(players);
    }

    public record PlayerInfo(int id, String nickname, Mascot character, String team) {}
}

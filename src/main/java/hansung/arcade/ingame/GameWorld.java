package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.map.TileMap;

import java.awt.Point;
import java.util.List;

/**
 * 인게임 상태(맵 + 플레이어). Swing에 의존하지 않아서 테스트와 이후 네트워크 동기화에 그대로 쓸 수 있다.
 */
public final class GameWorld {

    private final TileMap map;
    private final Player localPlayer;
    private double elapsedSeconds;

    /** @param localProfile 이 컴퓨터에서 조작하는 플레이어의 닉네임·캐릭터 */
    public GameWorld(TileMap map, PlayerProfile localProfile) {
        this.map = map;
        Point spawn = map.spawnPoints().get(0);
        this.localPlayer = new Player(1, localProfile, spawn.x, spawn.y);
    }

    public void update(double dt, Direction localInput) {
        elapsedSeconds += dt;
        localPlayer.update(localInput, dt, map);
    }

    public TileMap map() {
        return map;
    }

    public Player localPlayer() {
        return localPlayer;
    }

    public List<Player> players() {
        return List.of(localPlayer);
    }

    public double elapsedSeconds() {
        return elapsedSeconds;
    }
}


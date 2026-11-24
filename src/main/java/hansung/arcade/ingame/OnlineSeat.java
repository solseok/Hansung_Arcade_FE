package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.PlayerProfile;

import java.util.Objects;

/**
 * 온라인 경기에 들어온 플레이어 한 명 (GAME_STARTED의 players 한 줄).
 *
 * @param playerId 서버가 정한 플레이어 번호
 * @param spawn    맵 스폰 번호 (1부터). 서버가 정한다
 * @param local    이 컴퓨터에서 조작하는 플레이어인지 (1P·2P)
 */
public record OnlineSeat(int playerId, PlayerProfile profile, int spawn, boolean local) {

    public OnlineSeat {
        Objects.requireNonNull(profile);
    }
}

package hansung.arcade.ingame.entity;

import java.util.Objects;

/**
 * 게임에 들어오기 전에 정해지는 플레이어 정보(닉네임, 고른 캐릭터, 팀 번호, 고정색). 게임 중에는 바뀌지 않는다.
 *
 * @param team 팀전에서 속한 팀 번호(1부터). 개인전에서는 판정에 쓰지 않는다 (플레이어마다 따로 한 팀으로 친다)
 * @param color 캐릭터 고정색 부분({@link CharacterColor})을 칠할 색. null이면 원래 그림 색 그대로다
 */
public record PlayerProfile(String nickname, Mascot mascot, int team, CharacterColor color) {

    /** 팀을 정하지 않았을 때의 팀 번호 */
    public static final int DEFAULT_TEAM = 1;

    public PlayerProfile {
        Objects.requireNonNull(nickname);
        Objects.requireNonNull(mascot);
        nickname = nickname.strip();
        if (nickname.isEmpty()) {
            throw new IllegalArgumentException("닉네임이 비어 있습니다.");
        }
        if (team < 1) {
            throw new IllegalArgumentException("팀 번호는 1 이상이어야 합니다: " + team);
        }
    }

    /** 팀과 고정색을 정하지 않은 정보. 팀은 {@link #DEFAULT_TEAM}, 색은 원래 그림 색이다. */
    public PlayerProfile(String nickname, Mascot mascot) {
        this(nickname, mascot, DEFAULT_TEAM, null);
    }

    /** 고정색을 바꾸지 않고 원래 그림 색으로 그리는 플레이어 */
    public PlayerProfile(String nickname, Mascot mascot, int team) {
        this(nickname, mascot, team, null);
    }

    /** 팀을 정하지 않고({@link #DEFAULT_TEAM}) 고정색만 고른 플레이어 */
    public PlayerProfile(String nickname, Mascot mascot, CharacterColor color) {
        this(nickname, mascot, DEFAULT_TEAM, color);
    }

    /** 아웃게임에서 정보를 받기 전에 쓰는 임시 정보. 캐릭터는 기본 캐릭터({@link Mascot#DEFAULT}), 색은 원래 색이다. */
    public static PlayerProfile placeholder(int playerId) {
        return new PlayerProfile("P" + playerId, Mascot.DEFAULT);
    }
}

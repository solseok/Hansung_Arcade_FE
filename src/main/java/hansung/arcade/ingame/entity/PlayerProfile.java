package hansung.arcade.ingame.entity;

import java.util.Objects;

/**
 * 게임에 들어오기 전에 정해지는 플레이어 정보(닉네임, 고른 캐릭터). 게임 중에는 바뀌지 않는다.
 */
public record PlayerProfile(String nickname, Mascot mascot) {

    public PlayerProfile {
        Objects.requireNonNull(nickname);
        Objects.requireNonNull(mascot);
        nickname = nickname.strip();
        if (nickname.isEmpty()) {
            throw new IllegalArgumentException("닉네임이 비어 있습니다.");
        }
    }

    /** 아웃게임에서 정보를 받기 전에 쓰는 임시 정보. 캐릭터는 기본 캐릭터({@link Mascot#DEFAULT})다. */
    public static PlayerProfile placeholder(int playerId) {
        return new PlayerProfile("P" + playerId, Mascot.DEFAULT);
    }
}

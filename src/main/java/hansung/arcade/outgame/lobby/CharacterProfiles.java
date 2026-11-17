package hansung.arcade.outgame.lobby;

import hansung.arcade.ingame.entity.Mascot;

/** 대기실 캐릭터 배너에 쓰는 이름 */
final class CharacterProfiles {

    private CharacterProfiles() {
    }

    static String name(Mascot mascot) {
        return switch (mascot) {
            case SANGSANGBUGI -> "상상부기";
            case KKUKKU -> "꾸꾸";
            case KKOKKO -> "꼬꼬";
            case SANGZZI -> "상찌";
            case HANSUNGNYANGI -> "한성냥이";
        };
    }
}

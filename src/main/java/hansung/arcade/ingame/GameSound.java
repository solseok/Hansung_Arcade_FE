package hansung.arcade.ingame;

import java.util.Arrays;
import java.util.List;

/**
 * 인게임에서 나는 효과음. 소리 파일은 resources/sounds/{@link #key()}.mp3 이다.
 *
 * <p>게임 상태({@link GameWorld})는 Swing·소리 장치를 모르고, 한 틱에 일어난 일을 이 값으로 모아 두기만 한다
 * ({@link GameWorld#takeSounds()}). 화면({@link InGamePanel})이 프레임마다 모인 소리를 꺼내 밖에서 넘겨준 곳으로 보내 낸다.</p>
 */
public enum GameSound {
    /** 경기 시작 연출의 "Ready" */
    READY("Ntc_GameReady"),
    /** 경기 시작 연출의 "Start!". 이 글자가 사라지면 조작할 수 있다 */
    START("Ntc_GameStart"),
    /** 물풍선을 놓았다 */
    BALLOON_PLACED("Cha_BombIgnite"),
    /** 물풍선 하나가 터졌다 */
    EXPLODE("Cha_BombExplode"),
    /** 한 틱에 물풍선 여럿이 함께(연쇄로) 터졌다 */
    EXPLODE_MULTI("Cha_BombExplodeMulti"),
    /** 신발로 물풍선을 찼거나 장갑으로 던졌다 */
    KICK("Cha_KickBomb"),
    /** 물줄기에 맞아 물풍선에 갇혔다 */
    TRAPPED("Cha_LockedIn"),
    /**
     * 같은 편이 닿아 갇힌 사람을 구했다 (온라인은 PLAYER_RESCUED). 바늘로 스스로 빠져나올 때는 나지 않는다.
     *
     * <p>resources/sounds에 구출 전용 소리가 없어서, 로그인에 성공할 때 내는 짧은(0.3초) 성공 소리를 쓴다.
     * "해냈다"는 느낌이 살려 낸 순간과 맞고, 게임 화면에서는 다른 일에 쓰지 않아 헷갈리지 않는다.</p>
     */
    RESCUE("Bt_c_Success"),
    /**
     * 상대가 닿아 갇힌 사람을 터뜨렸다 (온라인은 PLAYER_POPPED reason TOUCH). 탈락이라 {@link #DIE}도 함께 난다.
     * 갇힌 채 시간이 다 돼 탈락할 때는 {@link #DIE}만 난다.
     *
     * <p>resources/sounds에 물풍선을 터뜨리는 전용 소리가 없어서, 이름 그대로 톡 터지는 짧은(0.18초) Bt_c_Pop을 쓴다.
     * 아웃게임 버튼을 누를 때도 나는 소리지만 게임 화면에는 그 버튼이 없다. 물풍선 폭발(Cha_BombExplode)은
     * 길고 물풍선이 터질 때마다 나서, 닿아서 터뜨린 순간을 따로 알아듣기 어렵다.</p>
     */
    POP("Bt_c_Pop"),
    /** 탈락했다 */
    DIE("Cha_Die"),
    /** 부서진 블럭에서 아이템이 나왔다 */
    ITEM_POP("Itm_ItemPop"),
    /** 아이템을 먹었다 */
    ITEM_EATEN("Itm_ItemEaten"),
    /** 보호막을 썼다 */
    SHIELD("Itm_ShieldFlash"),
    /** 탱크가 물줄기를 대신 맞고 부서졌다 */
    TANK_BROKEN("Itm_ShieldProtect"),
    /** 스프링 신발로 뛰었다 */
    SPRING("Itm_Spring"),
    /** 덤불에 들어갔다 */
    BUSH("Obj_Herb"),
    /** 아이템 비행기가 나왔다 */
    AIRPLANE("Obj_ItemShip00"),
    /** 비행기가 아이템을 떨어뜨렸다 */
    AIR_DROP("Obj_ItemShip01"),
    /** 나가기 버튼을 눌렀다 */
    EXIT("Bt_c_Out");

    private final String key;

    GameSound(String key) {
        this.key = key;
    }

    /** 소리 파일 이름(확장자 빼고) */
    public String key() {
        return key;
    }

    /** 모든 효과음의 파일 이름. 게임에 들어가기 전에 미리 풀어 둘 때 쓴다 */
    public static List<String> keys() {
        return Arrays.stream(values()).map(GameSound::key).toList();
    }
}

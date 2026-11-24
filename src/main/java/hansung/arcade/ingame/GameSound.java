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

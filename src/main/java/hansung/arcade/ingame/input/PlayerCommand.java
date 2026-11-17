package hansung.arcade.ingame.input;

import hansung.arcade.ingame.entity.Direction;

/**
 * 플레이어 한 명이 한 틱(1/60초) 동안 할 일. 키 입력({@link LocalInput})에서 틱마다 만들어
 * {@link hansung.arcade.ingame.GameWorld#update}에 넘긴다. 키보드와 Swing에 의존하지 않는다.
 *
 * @param move         누르고 있는 이동 방향. 없으면 null
 * @param placeBalloon 물풍선 키를 막 눌렀는지. 누른 뒤 첫 틱에만 true이고, 누르고 있어도 다음 틱부터는 false다
 * @param useItem      아이템 키를 막 눌렀는지. placeBalloon과 같이 누른 뒤 첫 틱에만 true다
 */
public record PlayerCommand(Direction move, boolean placeBalloon, boolean useItem) {

    /** 아무것도 하지 않는 틱 */
    public static final PlayerCommand IDLE = new PlayerCommand(null, false, false);

    /** direction 쪽으로 움직이기만 하는 틱 */
    public static PlayerCommand moving(Direction direction) {
        return new PlayerCommand(direction, false, false);
    }
}

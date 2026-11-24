package hansung.arcade.ingame;

import hansung.arcade.ingame.entity.Direction;

/**
 * 온라인 경기에서 이 컴퓨터의 플레이어가 한 일을 서버로 보내는 곳. 인게임은 네트워크를 모르므로 보내는 방법은 밖에서 정한다.
 * 모두 응답이 없는 메시지이고, 서버가 받아들이면 결과가 푸시로 돌아온다 ({@link ServerRules}).
 */
public interface ServerOutbox {

    /** MOVE. 충돌 박스 중심의 맵 픽셀 좌표와 바라보는 방향, 걷는 중인지 */
    void move(int playerId, double x, double y, Direction facing, boolean moving);

    /** BALLOON_PLACE. 서버가 받아들이면 BALLOON_PLACED가 온다 */
    void placeBalloon(int playerId, int col, int row);

    /** ITEM_PICKUP. 서버가 받아들이면 ITEM_TAKEN이 온다 */
    void pickUpItem(int playerId, int itemId);

    /** ITEM_USE. 손에 든 쓰는 아이템(needle, shield, spring_shoe)을 쓴다. 서버가 받아들이면 ITEM_USED가 온다 */
    void useItem(int playerId, String item);

    /**
     * BOX_PUSH. (col, row)의 밀리는 상자를 direction 쪽으로 한 칸 밀어 달라고 한다. 서버가 받아들이면 BOX_MOVED가 온다.
     * 서버는 마지막 MOVE가 멈춘 채 direction을 보고 상자 바로 앞 칸에 있을 때만 민다.
     */
    void pushBox(int playerId, int col, int row, Direction direction);

    /** CHAT. 서버가 방 전체에 CHAT_MESSAGE로 돌려준다 */
    void chat(int playerId, String text);
}

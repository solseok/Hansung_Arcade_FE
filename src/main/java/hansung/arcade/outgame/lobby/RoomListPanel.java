package hansung.arcade.outgame.lobby;

import hansung.arcade.ingame.hud.Box;
import hansung.arcade.outgame.ui.LobbyLayout;
import hansung.arcade.outgame.ui.OutgamePalette;
import hansung.arcade.outgame.ui.RoundPanel;
import hansung.arcade.outgame.ui.Shapes;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * 방 목록 판. 본체 아래쪽은 가운데 쪽 넘기기 자리({@link LobbyLayout#PAGER_TAB})만 튀어나오고 나머지는 들어가 있다
 * (참고 화면 6번). 들어간 자리는 투명해서 로비 바탕이 보인다.
 */
public final class RoomListPanel extends RoundPanel {

    public RoomListPanel() {
        super(OutgamePalette.ROOM_LIST, LobbyLayout.ROOM_LIST_RADIUS);
        setName("room-list");
    }

    /** 튀어나온 자리를 뺀 본체 높이 */
    public int mainHeight() {
        return LobbyLayout.ROOM_LIST_MAIN_HEIGHT;
    }

    /** 방 목록 기준 튀어나온 쪽 넘기기 자리 */
    public Rectangle tabBounds() {
        Box tab = LobbyLayout.PAGER_TAB;
        return new Rectangle(tab.x(), tab.y(), tab.width(), tab.height());
    }

    @Override
    protected void fillShape(Graphics2D g, int width, int height) {
        int radius = radius();
        g.fill(Shapes.round(0, 0, width, mainHeight(), radius));
        Rectangle tab = tabBounds();
        g.fill(Shapes.round(tab.x, tab.y, tab.width, tab.height, radius));
    }
}

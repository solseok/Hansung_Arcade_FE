package hansung.arcade.ingame.airplane;

import hansung.arcade.ingame.item.CellOccupancy;
import hansung.arcade.ingame.item.CellOccupancy.Occupant;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.match.MatchClock;

import java.util.Set;

/**
 * 비행기({@link AirDrop})가 게임에서 보고 바꾸는 것만 모은 창구: 출현 시각을 가릴 경기 시계와 경기가 끝났는지,
 * 떨어뜨릴 칸에 무엇이 있는지, 바닥에 아이템 놓기. 게임 상태(GameWorld)가 이어 주고, 비행기는 이것만 보므로
 * 게임 상태 없이도 진행해 볼 수 있다.
 */
public interface DropZone {

    /** 경기 시계. 남은 시간이 출현 시각이 됐는지 볼 때 쓴다 */
    MatchClock clock();

    /** 경기가 끝났는지. 끝나면 비행기가 더 나오지 않고 아이템도 떨어뜨리지 않는다 */
    boolean isMatchOver();

    /** (col, row) 칸에 occupants 가운데 하나라도 있는지 ({@link CellOccupancy#hasAny}) */
    boolean holdsAny(int col, int row, Set<Occupant> occupants);

    /** (col, row) 칸 바닥에 item을 놓는다 */
    void placeItem(int col, int row, ItemType item);
}

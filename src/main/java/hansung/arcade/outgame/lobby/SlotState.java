package hansung.arcade.outgame.lobby;

import hansung.arcade.outgame.ui.OutgamePalette;
import java.awt.Color;

/**
 * 대기실 자리 하나의 상태. 상태 줄에 쓰는 글자와 색을 함께 정한다.
 * 참고 화면처럼 자간을 넓힌 "준   비" 한 가지 글자를 쓰고, 준비했으면 불이 들어온 색, 아니면 꺼진 색으로 구분한다.
 */
public enum SlotState {
    /** 방장: 하늘색 "방   장" */
    HOST("방   장", OutgamePalette.STATUS_HOST),
    /** 준비함: 불이 들어온 주황 "준   비" */
    READY("준   비", OutgamePalette.STATUS_READY),
    /** 아직 준비하지 않음: 불이 꺼진 흐린 남색 "준   비" */
    WAITING("준   비", OutgamePalette.STATUS_IDLE),
    /** 열린 빈자리. 방장이 누르면 닫힌다 */
    EMPTY("", OutgamePalette.STATUS_IDLE),
    /** 닫힌 자리 (X). 방장이 닫았거나 맵 최대 인원 밖이다. 방장이 누르면 열린다 */
    CLOSED("", OutgamePalette.STATUS_IDLE);

    private final String label;
    private final Color color;

    SlotState(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public Color color() {
        return color;
    }

    public boolean occupied() {
        return this == HOST || this == READY || this == WAITING;
    }
}

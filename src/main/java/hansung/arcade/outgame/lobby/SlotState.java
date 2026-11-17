package hansung.arcade.outgame.lobby;

import static hansung.arcade.outgame.ui.OutgamePalette.*;

import java.awt.Color;

public enum SlotState {
    HOST("방   장", STATUS_HOST),
    READY("준   비", STATUS_READY),
    WAITING("준   비", STATUS_IDLE),
    EMPTY("", STATUS_IDLE),
    CLOSED("닫힘", STATUS_IDLE);
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
}

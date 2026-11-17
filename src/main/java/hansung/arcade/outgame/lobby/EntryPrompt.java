package hansung.arcade.outgame.lobby;

import hansung.arcade.outgame.ui.TextLabel;
import hansung.arcade.outgame.ui.OutgameFonts;
import javax.swing.JOptionPane;
import java.awt.Component;

/** 방 칸을 눌렀을 때 들어갈지 묻는 모달. 테스트에서는 바로 답하는 것으로 바꿔 끼운다. */
@FunctionalInterface
public interface EntryPrompt {

    String QUESTION = "입장하시겠습니까?";

    /** room에 들어가려면 true */
    boolean confirm(Component parent, Room room);

    /** 예/아니오 모달 */
    static EntryPrompt dialog() {
        return (parent, room) -> {
            TextLabel message = new TextLabel("%03d번 방 · %s  -  %s".formatted(room.number(), room.title(), QUESTION));
            message.setFont(OutgameFonts.label(13));
            return JOptionPane.showConfirmDialog(parent, message, "방 입장", JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE) == JOptionPane.YES_OPTION;
        };
    }
}

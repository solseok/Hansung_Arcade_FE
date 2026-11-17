package hansung.arcade.outgame.lobby;

import java.awt.Component;

import javax.swing.JOptionPane;

@FunctionalInterface
public interface EntryPrompt {
    String QUESTION = "입장하시겠습니까?";

    boolean confirm(Component parent, Room room);

    static EntryPrompt dialog() {
        return (parent, room) ->
                JOptionPane.showConfirmDialog(parent, QUESTION, "방 입장", JOptionPane.YES_NO_OPTION)
                        == JOptionPane.YES_OPTION;
    }
}

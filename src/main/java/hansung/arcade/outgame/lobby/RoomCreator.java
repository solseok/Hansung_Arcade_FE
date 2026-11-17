package hansung.arcade.outgame.lobby;

import java.awt.*;
import java.util.Optional;

import javax.swing.*;

@FunctionalInterface
public interface RoomCreator {
    record RoomRequest(String title, int capacity) {}

    Optional<RoomRequest> create(Component parent);

    static RoomCreator dialog() {
        return parent -> {
            JTextField title = new JTextField();
            title.setName("room-title-input");
            JComboBox<Integer> count = new JComboBox<>(new Integer[] {2, 3, 4, 5, 6, 7, 8});
            count.setSelectedItem(8);
            JPanel form = new JPanel(new GridLayout(4, 1, 4, 4));
            form.add(new JLabel("방 제목"));
            form.add(title);
            form.add(new JLabel("최대 인원"));
            form.add(count);
            return JOptionPane.showConfirmDialog(parent, form, "방만들기", JOptionPane.OK_CANCEL_OPTION)
                            == JOptionPane.OK_OPTION
                    ? Optional.of(
                            new RoomRequest(title.getText(), (Integer) count.getSelectedItem()))
                    : Optional.empty();
        };
    }
}

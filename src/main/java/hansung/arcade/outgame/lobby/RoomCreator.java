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
            form.add(new JLabel("방 제목 (비우면 ‘즐거운 한 판’으로 생성)"));
            form.add(title);
            form.add(new JLabel("최대 인원"));
            form.add(count);
            JLabel error = new JLabel();
            error.setForeground(new Color(160, 35, 25));
            JPanel content = new JPanel(new BorderLayout(0, 4));
            content.add(form, BorderLayout.CENTER);
            content.add(error, BorderLayout.SOUTH);
            while (JOptionPane.showConfirmDialog(parent, content, "방만들기",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                String roomTitle = title.getText().strip();
                if (roomTitle.length() <= 30) {
                    return Optional.of(new RoomRequest(roomTitle.isEmpty() ? "즐거운 한 판" : roomTitle,
                            (Integer) count.getSelectedItem()));
                }
                error.setText("방 제목을 30자 이하로 줄여 주세요.");
                title.selectAll();
            }
            return Optional.empty();
        };
    }
}

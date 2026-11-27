package hansung.arcade.outgame.lobby;

import java.awt.*;
import java.util.Optional;

import javax.swing.*;

@FunctionalInterface
public interface RoomCreator {
    /** @param mode 경기 방식 (개인전·팀전) */
    record RoomRequest(String title, int capacity, RoomMode mode) {
        public RoomRequest {
            mode = mode == null ? RoomMode.SOLO : mode;
        }

        /** 개인전 방 */
        public RoomRequest(String title, int capacity) {
            this(title, capacity, RoomMode.SOLO);
        }
    }

    Optional<RoomRequest> create(Component parent);

    static RoomCreator dialog() {
        return parent -> {
            JTextField title = new JTextField();
            title.setName("room-title-input");
            JComboBox<Integer> count = new JComboBox<>(new Integer[] {2, 3, 4, 5, 6, 7, 8});
            count.setSelectedItem(8);
            JComboBox<RoomMode> mode = new JComboBox<>(RoomMode.values());
            mode.setName("room-mode-input");
            mode.setSelectedItem(RoomMode.SOLO);
            JPanel form = new JPanel(new GridLayout(6, 1, 4, 4));
            form.add(new JLabel("방 제목 (비우면 ‘즐거운 한 판’으로 생성)"));
            form.add(title);
            form.add(new JLabel("최대 인원"));
            form.add(count);
            form.add(new JLabel("경기 방식 (팀전은 빨강·파랑·노랑·초록, 캠프 08은 빨강·파랑)"));
            form.add(mode);
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
                            (Integer) count.getSelectedItem(), (RoomMode) mode.getSelectedItem()));
                }
                error.setText("방 제목을 30자 이하로 줄여 주세요.");
                title.selectAll();
            }
            return Optional.empty();
        };
    }
}

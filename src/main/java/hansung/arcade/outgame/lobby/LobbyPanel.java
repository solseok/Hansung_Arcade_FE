package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.Scene;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.HsuColor;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import hansung.arcade.outgame.audio.Sound;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Supplier;

public final class LobbyPanel extends JPanel implements Scene {
    private static final int PAGE_SIZE = 8;
    private final LobbyService service;
    private final SceneManager scenes;
    private final JPanel cards = new JPanel(new GridLayout(4, 2, 6, 6));
    private final JLabel pageLabel = new JLabel();
    private final JLabel feedback = new JLabel(" ", SwingConstants.CENTER);
    private final JButton previous = LobbyArtwork.button("이전쪽", false);
    private final JButton next = LobbyArtwork.button("다음쪽", false);
    private int page;
    private final JLabel nickname = new JLabel("오프라인 목업");

    public LobbyPanel(LobbyService service, SceneManager scenes) {
        super(new BorderLayout(8, 8));
        this.service = service;
        this.scenes = scenes;
        setPreferredSize(new Dimension(800, 600));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 18));
        setBackground(HsuColor.BLUE.toAwt());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        JButton create = LobbyArtwork.button("방만들기", true);
        JButton quick = LobbyArtwork.button("바로시작", true);
        for (JButton button : new JButton[]{create, quick}) {
            button.setPreferredSize(new Dimension(155, 62));
            button.setFont(button.getFont().deriveFont(Font.BOLD, 25));
            button.setBackground(HsuColor.LIGHT_YELLOW.toAwt());
            actions.add(button);
        }
        create.addActionListener(event -> createRoom());
        quick.addActionListener(event -> enterRoom(service::quickJoin));
        nickname.setForeground(new Color(0x003C70));
        actions.add(nickname);
        add(actions, BorderLayout.NORTH);
        JPanel content = LobbyArtwork.panel(new BorderLayout(6, 6), new Color(0x03C8EF), new Color(0x009CD9));
        content.setBackground(new Color(0x0099D4));
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(154, 227, 255), 2),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        cards.setOpaque(false);
        content.add(cards, BorderLayout.CENTER);
        JPanel pagination = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        previous.addActionListener(event -> {
            page--;
            refresh();
        });
        next.addActionListener(event -> {
            page++;
            refresh();
        });
        pagination.setOpaque(false);
        pageLabel.setForeground(Color.WHITE);
        pagination.add(previous);
        pagination.add(pageLabel);
        pagination.add(next);
        JPanel status = new JPanel(new BorderLayout(4, 4));
        status.setOpaque(false);
        feedback.setForeground(Color.WHITE);
        status.add(pagination, BorderLayout.CENTER);
        status.add(feedback, BorderLayout.SOUTH);
        content.add(status, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setPreferredSize(new Dimension(760, 30));
        JButton menu = LobbyArtwork.button("메뉴", false);
        JPopupMenu popup = new JPopupMenu();
        JMenuItem reload = new JMenuItem("방 목록 새로고침");
        reload.addActionListener(event -> refresh());
        popup.add(reload);
        JCheckBoxMenuItem mute = new JCheckBoxMenuItem("음소거", Sound.isMuted());
        mute.addActionListener(event -> Sound.setMuted(mute.isSelected()));
        popup.add(mute);
        menu.addActionListener(event -> popup.show(menu, 0, -popup.getPreferredSize().height));
        JButton exit = LobbyArtwork.button("나가기", false);
        exit.addActionListener(event -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
            }
        });
        menu.setPreferredSize(new Dimension(86, 28));
        exit.setPreferredSize(new Dimension(86, 28));
        footer.add(menu, BorderLayout.WEST);
        footer.add(exit, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    public void setNickname(String value) {
        nickname.setText(value + " 님");
    }

    private void createRoom() {
        JTextField title = new JTextField();
        JComboBox<Integer> capacity = new JComboBox<>(new Integer[]{2, 3, 4, 5, 6, 7, 8});
        capacity.setSelectedItem(8);
        JPanel form = new JPanel(new GridLayout(4, 1, 6, 6));
        form.add(new JLabel("방 제목 (1~30자)"));
        form.add(title);
        form.add(new JLabel("최대 인원"));
        form.add(capacity);
        if (JOptionPane.showConfirmDialog(this, form, "방만들기",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            enterRoom(() -> service.create(title.getText(), (Integer) capacity.getSelectedItem()));
        }
    }

    private void enterRoom(Supplier<Room> action) {
        Room room;
        try {
            room = action.get();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            refresh();
            feedback.setText(exception.getMessage());
            return;
        }
        scenes.show("waiting", room);
    }

    private void refresh() {
        List<Room> rooms = service.rooms().stream()
                .sorted(java.util.Comparator.comparing((Room room) -> room.status() == Room.Status.PLAYING)
                        .thenComparingInt(Room::number)).toList();
        int pages = Math.max(1, (rooms.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));
        cards.removeAll();
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int index = page * PAGE_SIZE + slot;
            cards.add(index < rooms.size() ? roomCard(rooms.get(index)) : emptyCard());
        }
        previous.setEnabled(page > 0);
        next.setEnabled(page + 1 < pages);
        pageLabel.setText((page + 1) + " / " + pages);
        feedback.setText(rooms.isEmpty() ? "표시할 방이 없습니다." : " ");
        cards.revalidate();
        cards.repaint();
    }

    private JPanel emptyCard() {
        JPanel panel = LobbyArtwork.panel(new BorderLayout(), new Color(0x1DE1F5), new Color(0x02C7EC));
        panel.setBorder(LobbyArtwork.frame(4));
        JLabel thumbnail = new JLabel();
        thumbnail.setPreferredSize(new Dimension(62, 60));
        thumbnail.setOpaque(true);
        thumbnail.setBackground(new Color(0x13A5E8));
        thumbnail.setBorder(BorderFactory.createLineBorder(new Color(0x0872BA)));
        panel.add(thumbnail, BorderLayout.WEST);
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setOpaque(false);
        placeholder.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        JLabel bar = new JLabel(" ");
        bar.setOpaque(true);
        bar.setBackground(new Color(0x079BD5));
        placeholder.add(bar, BorderLayout.NORTH);
        panel.add(placeholder, BorderLayout.CENTER);
        return panel;
    }

    private JPanel roomCard(Room room) {
        JPanel panel = emptyCard();
        panel.removeAll();
        JLabel thumbnail = new JLabel(new ImageIcon(LobbyArtwork.map(room.map())
                .getScaledInstance(60, 60, java.awt.Image.SCALE_SMOOTH)));
        thumbnail.setBorder(BorderFactory.createLineBorder(new Color(0x155A76), 2));
        panel.add(thumbnail, BorderLayout.WEST);
        JPanel info = new JPanel(new BorderLayout(4, 3));
        info.setOpaque(false);
        info.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        JLabel title = new JLabel(room.title());
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        title.setForeground(Color.WHITE);
        title.setOpaque(true);
        title.setBackground(new Color(0x008FCB));
        title.setToolTipText(room.title());
        JPanel heading = new JPanel(new BorderLayout(5, 0));
        heading.setBackground(new Color(0x008FCB));
        JLabel number = new JLabel(String.format(" %03d ", room.number()));
        number.setForeground(new Color(0xFFF263));
        number.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        number.setBorder(BorderFactory.createLineBorder(new Color(0xFFF263)));
        heading.add(number, BorderLayout.WEST);
        heading.add(title, BorderLayout.CENTER);
        info.add(heading, BorderLayout.NORTH);
        JLabel map = new JLabel(room.map());
        map.setForeground(new Color(0x285579));
        map.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        info.add(map, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(5, 0));
        bottom.setOpaque(false);
        JLabel state = LobbyArtwork.outlined(room.status() == Room.Status.WAITING ? "WAITING" : "PLAYING",
                17, room.status() == Room.Status.WAITING ? new Color(0xFFF393) : Color.WHITE,
                room.status() == Room.Status.WAITING ? new Color(0x793B16) : new Color(0x226198));
        state.setPreferredSize(new Dimension(98, 24));

        state.setForeground(room.status() == Room.Status.WAITING ? new Color(0x005487) : new Color(0x505B8C));
        bottom.add(state, BorderLayout.WEST);
        JPanel entry = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        entry.setOpaque(false);
        entry.add(new JLabel(room.players() + "/" + room.capacity()));
        JButton join = LobbyArtwork.button("입장", false);
        join.setPreferredSize(new Dimension(55, 23));
        join.setEnabled(room.canJoin());
        join.addActionListener(event -> enterRoom(() -> service.join(room.number())));
        entry.add(join);
        bottom.add(entry, BorderLayout.EAST);
        info.add(bottom, BorderLayout.SOUTH);
        panel.add(info, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(new Color(0x00BCEB));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(12, 45, 115, 225));
        g.fillRect(0, getHeight() - 45, getWidth(), 45);
        g.dispose();
    }

    @Override
    public JComponent view() {
        return this;
    }

    @Override
    public void enter(Object data) {
        refresh();
    }
}

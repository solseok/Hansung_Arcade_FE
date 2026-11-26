package hansung.arcade.outgame.lobby;

import hansung.arcade.outgame.ui.OutgameFonts;
import hansung.arcade.outgame.ui.OutgamePalette;
import hansung.arcade.outgame.ui.Shapes;
import hansung.arcade.outgame.ui.TextLabel;
import hansung.arcade.outgame.ui.WaitingRoomLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static hansung.arcade.outgame.ui.WaitingRoomLayout.SLOT_IMAGE_HEIGHT;
import static hansung.arcade.outgame.ui.WaitingRoomLayout.SLOT_NAME_HEIGHT;
import static hansung.arcade.outgame.ui.WaitingRoomLayout.SLOT_NAME_TOP;
import static hansung.arcade.outgame.ui.WaitingRoomLayout.SLOT_STATUS_HEIGHT;
import static hansung.arcade.outgame.ui.WaitingRoomLayout.SLOT_STATUS_TOP;
import static hansung.arcade.outgame.ui.WaitingRoomLayout.SLOT_WIDTH;

/**
 * 대기실 자리 하나 (95x137): 그림 칸 95 + 닉네임 줄 20 + 상태 줄 17.
 * 그림 칸에는 팀 색으로 고정색을 다시 칠한 캐릭터(1배)를 그리고, 상태 줄에는 방장·준비(불 켜짐·꺼짐)를 보여 준다.
 * 빈자리는 그림 칸만 어둡게, 닫힌 자리(방장이 닫았거나 맵 최대 인원 밖)는 X를 그린다.
 */
public final class PlayerSlot extends JComponent {

    private final JLabel sprite = new JLabel();
    private final TextLabel name = new TextLabel("", SwingConstants.CENTER);
    private final TextLabel status = new TextLabel("", SwingConstants.CENTER);
    private SlotState state = SlotState.EMPTY;
    private String nickname = "";

    public PlayerSlot(int index) {
        setName("slot-" + index);
        setLayout(null);
        sprite.setName("slot-sprite");
        sprite.setHorizontalAlignment(SwingConstants.CENTER);
        sprite.setVerticalAlignment(SwingConstants.BOTTOM);
        sprite.setBounds(0, 0, SLOT_WIDTH, SLOT_IMAGE_HEIGHT - 6);
        name.setName("slot-name");
        // 굴림체 12는 한글이 12px 폭이라 8자(96px)가 자리 폭 95px에 꽉 차므로 11로 쓴다
        name.setFont(OutgameFonts.gulimCheBold(11));
        name.setForeground(Color.WHITE);
        name.setOutline(OutgamePalette.SLOT_STATUS, 1);
        name.setBounds(0, SLOT_NAME_TOP, SLOT_WIDTH, SLOT_NAME_HEIGHT);
        status.setName("slot-status");
        status.setFont(OutgameFonts.label(12));
        status.setOutline(OutgamePalette.WAITING_FRAME, 1);
        status.setBounds(0, SLOT_STATUS_TOP, SLOT_WIDTH, SLOT_STATUS_HEIGHT);
        add(sprite);
        add(name);
        add(status);
        display(SlotState.EMPTY, "", null);
    }

    /** @param character 사람이 있는 자리면 그대로(1배) 그릴 캐릭터 그림, 없으면 null */
    public void display(SlotState state, String nickname, BufferedImage character) {
        this.state = state;
        this.nickname = nickname;
        sprite.setIcon(character == null ? null : new ImageIcon(character));
        name.setText(nickname);
        status.setText(state.label());
        status.setForeground(state.color());
        repaint();
    }

    public SlotState state() {
        return state;
    }

    public String nickname() {
        return nickname;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            Shapes.hints(g);
            boolean occupied = state.occupied();
            Color image = occupied ? OutgamePalette.SLOT_OCCUPIED : OutgamePalette.SLOT_EMPTY;
            Color edge = occupied ? OutgamePalette.SLOT_EDGE : OutgamePalette.SLOT_CLOSED_MARK;
            g.drawImage(Shapes.paint(SLOT_WIDTH, SLOT_IMAGE_HEIGHT,
                    m -> m.fill(Shapes.round(0, 0, SLOT_WIDTH, SLOT_IMAGE_HEIGHT, WaitingRoomLayout.SLOT_RADIUS)), image,
                    d -> Shapes.edge(d, SLOT_WIDTH, SLOT_IMAGE_HEIGHT, WaitingRoomLayout.SLOT_RADIUS, edge, 1.5f)), 0, 0, null);
            if (state == SlotState.CLOSED) {
                g.setColor(OutgamePalette.SLOT_CLOSED_MARK);
                g.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(26, 26, SLOT_WIDTH - 27, SLOT_IMAGE_HEIGHT - 27);
                g.drawLine(SLOT_WIDTH - 27, 26, 26, SLOT_IMAGE_HEIGHT - 27);
            }
            g.drawImage(Shapes.roundRect(SLOT_WIDTH, SLOT_NAME_HEIGHT, WaitingRoomLayout.ROW_RADIUS,
                    OutgamePalette.SLOT_NAME), 0, SLOT_NAME_TOP, null);
            g.drawImage(Shapes.roundRect(SLOT_WIDTH, SLOT_STATUS_HEIGHT, WaitingRoomLayout.ROW_RADIUS,
                    OutgamePalette.SLOT_STATUS), 0, SLOT_STATUS_TOP, null);
        } finally {
            g.dispose();
        }
    }
}

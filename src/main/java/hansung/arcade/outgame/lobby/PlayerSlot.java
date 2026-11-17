package hansung.arcade.outgame.lobby;

import hansung.arcade.ingame.entity.*;
import hansung.arcade.ingame.render.*;
import hansung.arcade.outgame.*;
import hansung.arcade.outgame.ui.*;

import java.awt.*;

import javax.swing.*;

public final class PlayerSlot extends JPanel {
    private final SlotState state;
    private final String nickname;
    private static final CharacterSprites SPRITES = CharacterSprites.load();

    public PlayerSlot(GameStartInfo.PlayerInfo player, SlotState state) {
        super(null);
        this.state = state;
        nickname = player == null ? "" : player.nickname();
        setOpaque(false);
        JLabel sprite = new JLabel();
        sprite.setName("slot-sprite");
        sprite.setFont(OutgameFonts.label(16));
        sprite.setHorizontalAlignment(JLabel.CENTER);
        sprite.setOpaque(true);
        sprite.setBackground(
                player == null ? OutgamePalette.SLOT_EMPTY : OutgamePalette.SLOT_OCCUPIED);
        sprite.setBounds(0, 0, 95, 95);
        if (player != null)
            sprite.setIcon(
                    new ImageIcon(
                            CharacterRecolor.recolor(
                                    SPRITES.frame(player.character(), Direction.DOWN, false, 0),
                                    player.character(),
                                    Team.byLabel(player.team()).characterColor())));
        else if (state == SlotState.CLOSED) sprite.setText("×");
        add(sprite);
        TextLabel name = new TextLabel(nickname);
        name.setName("slot-name");
        name.setOpaque(true);
        name.setBackground(OutgamePalette.SLOT_NAME);
        name.setHorizontalAlignment(JLabel.CENTER);
        name.setBounds(0, 99, 95, 20);
        add(name);
        TextLabel status = new TextLabel(state.label());
        status.setName("slot-status");
        status.setFont(OutgameFonts.label(12));
        status.setForeground(state.color());
        status.setOpaque(true);
        status.setBackground(OutgamePalette.SLOT_STATUS);
        status.setHorizontalAlignment(JLabel.CENTER);
        status.setBounds(0, 120, 95, 17);
        add(status);
    }

    public SlotState state() {
        return state;
    }

    public String nickname() {
        return nickname;
    }
}

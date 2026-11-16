package hansung.arcade.outgame.chat;

import hansung.arcade.core.ui.HsuColor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** 로비와 대기실에서 재사용하는 채팅 표시/입력 컴포넌트. */
public final class ChatPanel extends JPanel {
    private final JTextPane messages = new JTextPane();
    private final JTextField input = new JTextField();
    private final JLabel feedback = new JLabel(" ");
    private final ChatSender sender;

    public ChatPanel(ChatSender sender) {
        super(new BorderLayout(6, 6));
        this.sender = sender;
        setBackground(new java.awt.Color(0x078FCE));
        setBorder(javax.swing.BorderFactory.createTitledBorder(
                hansung.arcade.outgame.lobby.LobbyArtwork.frame(5), "방 채팅",
                0, 0, new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 12), java.awt.Color.WHITE));
        messages.setBackground(new java.awt.Color(0x0778BE));
        input.setBackground(new java.awt.Color(0xA9F2FF));
        input.setForeground(new java.awt.Color(0x003E6C));
        feedback.setForeground(java.awt.Color.WHITE);
        setPreferredSize(new Dimension(760, 190));
        messages.setEditable(false);
        add(new JScrollPane(messages), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(6, 4));
        bottom.setOpaque(false);
        JButton send = hansung.arcade.outgame.lobby.LobbyArtwork.button("전송", false);
        input.setToolTipText("메시지를 입력하고 Enter를 누르세요");
        input.addActionListener(event -> send());
        send.addActionListener(event -> send());
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(send, BorderLayout.EAST);
        bottom.add(feedback, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    private void send() {
        String message = input.getText().strip();
        if (message.isEmpty()) {
            return;
        }
        try {
            sender.send(message);
            input.setText("");
            feedback.setText(" ");
        } catch (RuntimeException exception) {
            feedback.setText("전송하지 못했습니다. 다시 시도해 주세요.");
        }
    }

    public void message(String nickname, String text) {
        append(nickname + ": " + text, false);
    }

    public void systemMessage(String text) {
        append(text, true);
    }

    private void append(String text, boolean system) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> append(text, system));
            return;
        }
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, system ? new java.awt.Color(0xFFE979) : java.awt.Color.WHITE);
        StyleConstants.setItalic(style, system);
        try {
            messages.getStyledDocument().insertString(messages.getDocument().getLength(), text + "\n", style);
            messages.setCaretPosition(messages.getDocument().getLength());
        } catch (BadLocationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public void clear() {
        messages.setText("");
        input.setText("");
        feedback.setText(" ");
    }
}

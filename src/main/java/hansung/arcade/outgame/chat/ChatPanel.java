package hansung.arcade.outgame.chat;

import hansung.arcade.outgame.ui.Shapes;
import hansung.arcade.outgame.ui.GlossButton;
import hansung.arcade.outgame.ui.TextLabel;
import hansung.arcade.outgame.ui.OutgameFonts;
import hansung.arcade.outgame.ui.OutgamePalette;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 대기실 채팅: 위에 "전체 채팅" 탭, 가운데 로그, 아래 입력 줄(입력 칸 + 전송).
 * 일반 메시지는 흰 글자, 시스템 안내는 노란 글자다. 사용자가 쓰는 채팅 글자는 굴림체로 매끈하게 그린다.
 *
 * <p>글자 커서와 키보드 포커스는 입력 칸만 갖는다. 로그는 포커스를 받지 않아 눌러도 캐럿·선택이 생기지 않고,
 * 로그를 누르면 입력 칸으로 포커스를 넘긴다 (JDK는 편집할 수 없는 글 칸도 포커스를 받으면 캐럿을 보인다).
 * 전송 버튼도 포커스를 받지 않아, 눌러 보낸 뒤에도 입력 칸에서 이어 칠 수 있다.</p>
 */
public final class ChatPanel extends JPanel {
    /** 탭 높이, 입력 줄 높이 */
    private static final int TAB_HEIGHT = 20;
    private static final int INPUT_ROW_HEIGHT = 31;
    private static final int GAP = 2;

    private final TextLabel tab = new TextLabel("전체 채팅", SwingConstants.CENTER);
    private final JTextPane messages = new JTextPane();
    private final JScrollPane scroll = new JScrollPane(messages);
    private final JTextField input = new JTextField();
    private final JButton send = new GlossButton("전송", GlossButton.BLUE);
    private final ChatSender sender;

    public ChatPanel(ChatSender sender) {
        super(null);
        this.sender = sender;
        setOpaque(false);
        setPreferredSize(new Dimension(427, 160));
        tab.setName("chat-tab");
        tab.setFont(OutgameFonts.label(13));
        tab.setForeground(Color.WHITE);
        tab.setOutline(OutgamePalette.WAITING_SECTION, 1);
        tab.setFill(OutgamePalette.CHAT_BACKGROUND, 4);
        add(tab);
        messages.setName("chat-log");
        messages.setEditable(false);
        messages.setOpaque(true);
        messages.setBackground(OutgamePalette.CHAT_BACKGROUND);
        messages.setFont(OutgameFonts.gulimChe(12));
        messages.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        messages.setFocusable(false);
        messages.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                focusInput();
            }
        });
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(OutgamePalette.CHAT_BACKGROUND);
        add(scroll);
        input.setName("chat-input");
        input.setFont(OutgameFonts.gulimChe(12));
        input.setBackground(OutgamePalette.CHAT_INPUT);
        input.setForeground(Color.WHITE);
        input.setCaretColor(Color.WHITE);
        input.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        input.setToolTipText("메시지를 입력하고 Enter를 누르세요");
        input.addActionListener(event -> send());
        send.addActionListener(event -> send());
        send.setFocusable(false);
        add(input);
        add(send);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        tab.setBounds(0, 0, width / 2, TAB_HEIGHT);
        int logHeight = height - TAB_HEIGHT - INPUT_ROW_HEIGHT - GAP;
        scroll.setBounds(0, TAB_HEIGHT, width, logHeight);
        int rowTop = height - INPUT_ROW_HEIGHT;
        input.setBounds(8, rowTop + 4, width - 8 - 8 - 50, INPUT_ROW_HEIGHT - 8);
        send.setBounds(width - 8 - 44, rowTop + 4, 44, INPUT_ROW_HEIGHT - 8);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            Shapes.hints(g);
            int rowTop = getHeight() - INPUT_ROW_HEIGHT;
            g.drawImage(Shapes.roundRect(getWidth(), INPUT_ROW_HEIGHT, 4, OutgamePalette.CHAT_INPUT_ROW), 0, rowTop, null);
        } finally {
            g.dispose();
        }
    }

    /** 입력 칸에 키보드 포커스를 준다. 대기실에 들어오면 바로 칠 수 있게 부른다 */
    public void focusInput() {
        input.requestFocusInWindow();
    }

    private void send() {
        String message = input.getText().strip();
        if (message.isEmpty()) {
            return;
        }
        try {
            sender.send(message);
            input.setText("");
        } catch (RuntimeException exception) {
            systemMessage("전송하지 못했습니다. 다시 시도해 주세요.");
        }
    }

    public void message(String nickname, String text) {
        append(nickname + ": " + text, OutgamePalette.CHAT_TEXT);
    }

    public void systemMessage(String text) {
        append(text, OutgamePalette.CHAT_SYSTEM);
    }

    private void append(String text, Color color) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> append(text, color));
            return;
        }
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, color);
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
    }
}

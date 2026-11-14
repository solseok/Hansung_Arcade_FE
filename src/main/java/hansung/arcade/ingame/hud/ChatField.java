package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.chat.ChatMessage;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position;
import javax.swing.text.StyleConstants;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Objects;
import java.util.function.LongSupplier;

import static hansung.arcade.ingame.hud.HudLayout.CHAT_BAR;
import static hansung.arcade.ingame.hud.HudLayout.CHAT_OUTLINE;

/**
 * 맵 위 채팅 입력칸. 자리는 {@link HudLayout#CHAT_BAR}이고, 배경과 테두리 없이
 * 치고 있는 글자와 커서만 올라온 채팅과 같은 {@link OutlineText}로 그린다.
 *
 * <p>한글 조합·붙여넣기·지우기는 JTextField가 하던 대로 두고 그리기만 바꿨다. 조합 중인 글자도 문서에 들어 있어
 * 그대로 보인다. 커서를 글 끝에만 그리므로 실제 커서도 늘 끝에 있게 막았고(그래서 선택도 없다),
 * 마우스는 받지 않고 아래 게임 화면으로 흘려보낸다.</p>
 *
 * <p>글은 {@link ChatMessage#MAX_BYTES}바이트까지만 들어간다. 바 폭보다 길어지면 앞쪽 글자를 숨기고 끝쪽만 보여 준다.</p>
 */
public final class ChatField extends JTextField {

    private static final int CARET_WIDTH = 1;
    /** 글 끝과 커서 사이. 마지막 글자의 테두리가 지나가는 자리다 */
    private static final int CARET_GAP = CHAT_OUTLINE;
    private static final long BLINK_NANOS = 500_000_000L;

    private final OutlineText art;
    /** 바 안에 보이는 부분. 글이 바뀔 때만 다시 구한다 */
    private String visibleText = "";
    /** 커서 깜빡임을 재는 시계 (나노초) */
    private LongSupplier blinkClock = System::nanoTime;
    private long blinkStart = blinkClock.getAsLong();

    public ChatField(OutlineText art) {
        this.art = art;
        setFont(art.font());
        setOpaque(false);
        setBorder(null);
        // Tab으로 포커스가 빠져나가 입력칸만 떠 있는 일이 없게 한다
        setFocusTraversalKeysEnabled(false);
        // 커서는 직접 그리므로 JTextField 커서의 깜빡임 타이머는 끈다
        getCaret().setBlinkRate(0);
        setNavigationFilter(new CaretAtEnd());
        ((AbstractDocument) getDocument()).setDocumentFilter(new LengthLimit());
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                textChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                textChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                textChanged();
            }
        });
        // 글자 테두리와 칸 위아래로 조금 나가는 글자가 잘리지 않게 바보다 넓게 잡는다
        Box bounds = CHAT_BAR.inset(-OutlineText.MARGIN);
        setBounds(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        setVisible(false);
    }

    /** 바 안에 보이는 부분. 글이 길면 앞쪽이 잘린 끝쪽이다. */
    String visibleText() {
        return visibleText;
    }

    /**
     * 커서 깜빡임을 재는 시계를 바꾼다. 커서는 그리는 시점에 따라 켜져 있기도 꺼져 있기도 하므로,
     * 테스트에서는 멈춘 시계를 넣어 언제 그려도 커서가 켜진 그림을 얻는다.
     */
    public void setBlinkClock(LongSupplier nanoClock) {
        blinkClock = Objects.requireNonNull(nanoClock);
        blinkStart = blinkClock.getAsLong();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // 바의 왼쪽 위(글자 칸이 시작하는 곳)는 이 컴포넌트 안에서 (MARGIN, MARGIN)이다
        art.draw(g2, visibleText, OutlineText.MARGIN, OutlineText.MARGIN);
        if ((blinkClock.getAsLong() - blinkStart) / BLINK_NANOS % 2 == 0) {
            art.drawBar(g2, OutlineText.MARGIN + art.width(visibleText) + CARET_GAP, OutlineText.MARGIN, CARET_WIDTH);
        }
    }

    /** 열자마자 커서가 보이도록 깜빡임을 처음부터 센다. 배경이 없어 커서가 꺼져 있으면 열렸는지 알 수 없다. */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            blinkStart = blinkClock.getAsLong();
        }
        super.setVisible(visible);
    }

    /** 마우스가 닿지 않는 것으로 쳐서 클릭이 게임 화면으로 간다. */
    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    private void textChanged() {
        visibleText = tailThatFits(getText());
        // 치는 동안에는 커서가 꺼지지 않게 깜빡임을 처음부터 다시 센다
        blinkStart = blinkClock.getAsLong();
        repaint();
    }

    /** 커서까지 바 폭에 들어갈 때까지 앞에서 한 글자씩 떼어 낸 나머지 */
    private String tailThatFits(String text) {
        int room = CHAT_BAR.width() - CARET_GAP - CARET_WIDTH;
        int start = 0;
        while (start < text.length() && art.width(text.substring(start)) > room) {
            start = text.offsetByCodePoints(start, 1);
        }
        return text.substring(start);
    }

    /** 커서를 어디로 옮기려 하든 글 끝에 둔다. */
    private final class CaretAtEnd extends NavigationFilter {

        @Override
        public void setDot(FilterBypass fb, int dot, Position.Bias bias) {
            fb.setDot(getDocument().getLength(), Position.Bias.Forward);
        }

        @Override
        public void moveDot(FilterBypass fb, int dot, Position.Bias bias) {
            fb.setDot(getDocument().getLength(), Position.Bias.Forward);
        }
    }

    /** 글 길이를 {@link ChatMessage#MAX_BYTES}바이트로 막는다. 넘치는 만큼은 잘라 내고 넣는다. */
    private static final class LengthLimit extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            fb.insertString(offset, fit(fb.getDocument(), offset, 0, text, attr), attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            fb.replace(offset, length, fit(fb.getDocument(), offset, length, text, attrs), attrs);
        }

        /** offset부터 length 글자를 text로 바꿀 때, 바이트 제한 안에 들어가는 text의 앞부분 */
        private static String fit(Document document, int offset, int length, String text, AttributeSet attr)
                throws BadLocationException {
            if (text == null) {
                return null;
            }
            // 조합 중인 글자는 JTextField가 넣은 자리를 기억했다가 지우므로 자르지 않고 그대로 넣는다.
            // 조합이 끝나 확정될 때 다시 이 필터를 거친다.
            if (attr != null && attr.isDefined(StyleConstants.ComposedTextAttribute)) {
                return text;
            }
            int after = offset + length;
            String kept = document.getText(0, offset) + document.getText(after, document.getLength() - after);
            // 필터를 달면 JTextField가 하던 줄바꿈 → 공백 처리를 건너뛰므로 여기서 한다 (여러 줄 글 붙여넣기)
            return ChatMessage.truncateToBytes(ChatMessage.singleLine(text),
                    ChatMessage.MAX_BYTES - ChatMessage.byteLength(kept));
        }
    }
}

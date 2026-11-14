package hansung.arcade.ingame.chat;

import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.OutlineText;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.text.BadLocationException;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.InputMethodEvent;
import java.awt.font.TextHitInfo;
import java.text.AttributedString;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 창 없이 입력칸에 글을 넣어 길이 제한(64바이트), 커서 위치, 한글 조합 처리를 확인한다.
 * 글꼴은 어디에나 있는 Java 기본 고정폭 글꼴을 쓴다.
 */
@ExtendWith(RunOnEdt.class)
class ChatFieldTest {

    private OutlineText art;
    private ChatField field;

    @BeforeEach
    void setUp() {
        art = new OutlineText(new Font(Font.MONOSPACED, Font.PLAIN, 12), HudLayout.CHAT_TEXT_HEIGHT,
                Color.WHITE, Color.BLACK);
        field = new ChatField(art);
    }

    @Test
    void 채팅_바_자리에_글자_둘레_여유만큼_넓게_놓이고_처음에는_보이지_않는다() {
        int margin = OutlineText.MARGIN;
        assertEquals(HudLayout.CHAT_BAR.x() - margin, field.getX());
        assertEquals(HudLayout.CHAT_BAR.y() - margin, field.getY());
        assertEquals(HudLayout.CHAT_BAR.width() + 2 * margin, field.getWidth());
        assertEquals(HudLayout.CHAT_BAR.height() + 2 * margin, field.getHeight());
        // 넓힌 만큼도 맵 영역 안이다
        assertTrue(HudLayout.FIELD.contains(HudLayout.CHAT_BAR.inset(-margin)));
        assertFalse(field.isVisible());
        assertFalse(field.isOpaque());
    }

    @Test
    void 글은_64바이트에서_잘린다_한글_32자_영문_64자() throws BadLocationException {
        field.setText("가".repeat(40));
        assertEquals("가".repeat(32), field.getText());

        field.setText("a".repeat(100));
        assertEquals("a".repeat(64), field.getText());

        // 한글 30자(60바이트) 뒤에는 4바이트만 더 들어간다
        field.setText("가".repeat(30));
        field.getDocument().insertString(field.getDocument().getLength(), "ab다cd", null);
        assertEquals("가".repeat(30) + "ab다", field.getText());
        assertEquals(ChatMessage.MAX_BYTES, ChatMessage.byteLength(field.getText()));
    }

    @Test
    void 꽉_찬_뒤에도_지우고_다시_칠_수_있다() throws BadLocationException {
        field.setText("a".repeat(64));

        field.getDocument().remove(60, 4);
        field.getDocument().insertString(60, "가나다", null);

        assertEquals("a".repeat(60) + "가나", field.getText());
    }

    @Test
    void 붙여넣은_글의_줄바꿈과_탭은_공백이_된다() {
        field.setText("ab");
        field.replaceSelection("c\nd\te\r\n");

        assertEquals("abc d e  ", field.getText());

        field.setText("한\n줄");
        assertEquals("한 줄", field.getText());
    }

    @Test
    void 커서는_어디로_옮겨도_글_끝에_있고_선택되지_않는다() {
        field.setText("안녕하세요");

        field.setCaretPosition(0);
        assertEquals(5, field.getCaretPosition());

        field.selectAll();
        assertEquals(5, field.getCaretPosition());
        assertEquals(field.getSelectionStart(), field.getSelectionEnd());
    }

    @Test
    void 바에_들어가는_글은_전부_보이고_긴_글은_끝쪽만_보인다() {
        field.setText("짧은 글");
        assertEquals("짧은 글", field.visibleText());

        String longText = "가나다라마바사아자차카타파하".repeat(2) + "끝";
        assertTrue(ChatMessage.byteLength(longText) <= ChatMessage.MAX_BYTES);
        field.setText(longText);
        String visible = field.visibleText();

        assertTrue(visible.length() < longText.length(), visible);
        assertTrue(longText.endsWith(visible), visible);
        // 커서(1px)와 그 앞 1px까지 바 폭 안에 들어온다
        assertTrue(art.width(visible) + 2 <= HudLayout.CHAT_BAR.width(), "폭 " + art.width(visible));
        // 한 글자라도 더 보이면 넘친다
        String oneMore = longText.substring(longText.length() - visible.length() - 1);
        assertTrue(art.width(oneMore) + 2 > HudLayout.CHAT_BAR.width());
    }

    @Test
    void 조합_중인_한글도_보이고_확정되면_글에_남는다() {
        field.setText("안");

        compose("ㄴ", 0);
        assertEquals("안ㄴ", field.getText());
        assertEquals("안ㄴ", field.visibleText());
        compose("녀", 0);
        assertEquals("안녀", field.getText());
        compose("녕", 1);

        assertEquals("안녕", field.getText());
        assertEquals(2, field.getCaretPosition());
    }

    @Test
    void 최대_길이에서_조합한_글자는_확정될_때_들어가지_않고_앞_글자도_지워지지_않는다() {
        String full = "가".repeat(ChatMessage.MAX_BYTES / 2);
        field.setText(full);

        compose("한", 0);
        compose("한", 1);

        assertEquals(full, field.getText());
    }

    /** 입력기가 보내는 것처럼 조합 글자를 넣는다. committed는 text 앞에서부터 확정된 글자 수다. */
    private void compose(String text, int committed) {
        field.dispatchEvent(new InputMethodEvent(field, InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                new AttributedString(text).getIterator(), committed, TextHitInfo.trailing(text.length() - 1), null));
    }
}

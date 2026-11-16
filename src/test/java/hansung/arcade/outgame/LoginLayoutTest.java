package hansung.arcade.outgame;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.lobby.MockLobbyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.imageio.ImageIO;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 로그인 화면 배치를 참고 화면(640x477 로그인)처럼: 아래 가운데 주황 입력 영역, 그 양옆에 붙은 1P·2P,
 * 입력 영역 아래 한 줄의 [시작하기] [종   료] 버튼. 참고 화면 값을 800x600으로 옮겼다 (가로 x1.25, 세로 x1.258).
 * 배경은 800x600 그림(resources/etc/hsu_arcade.png)을 아래 여백 없이 창 전체에 깔고, 그 위에 로그인 영역을 올린다.
 * 입력 영역은 닉네임 칸과 오류 문구 한 줄만 담을 만큼만 높고, 오른쪽 아래 로고는 두지 않는다.
 * 닉네임 입력 칸은 가장 넓은 8자가 꽉 차는 폭이고, 입력 영역은 그 칸을 감쌀 만큼만 넓다 (2P면 칸 둘).
 */
@ExtendWith(RunOnEdt.class)
class LoginLayoutTest {

    /** 입력 영역 위치·높이: 아래 버튼 줄 바로 위, 닉네임 칸과 오류 한 줄만 담는 높이 */
    private static final int FORM_Y = 448;
    private static final int FORM_HEIGHT = 100;
    /** 입력 영역 안쪽 좌우 여백 합 상한: 닉네임 칸을 감쌀 만큼만 넓다 */
    private static final int FORM_SIDES = 30;

    private LoginPanel login;

    @BeforeEach
    void setUp() {
        login = new LoginPanel(new OutgameSession(new MockLobbyService()), new SceneManager(screen -> { }));
        login.setSize(800, 600);
        SwingTree.layout(login);
    }

    @Test
    void 배경은_800x600_그림을_여백_없이_창_전체에_깐다() throws Exception {
        BufferedImage art = art();
        assertEquals(800, art.getWidth());
        assertEquals(600, art.getHeight());

        assertEquals(new Rectangle(0, 0, 800, 600), bounds("login-background"));
        BufferedImage screen = SwingTree.render(login);
        // 입력 영역·버튼이 없는 자리는 그림 그대로 보인다 (늘이거나 줄이지 않는다)
        for (int[] p : new int[][]{{10, 10}, {400, 100}, {790, 200}, {60, 380}, {400, 380}, {20, 590}}) {
            assertSameAsArt(art, screen, p[0], p[1]);
        }
        assertTrue(bounds("login-form").y >= 384, "입력 영역은 그림 아래쪽(참고 화면 3:2 아래)에 올린다.");
    }

    @Test
    void 일P_모드의_입력_영역은_아래_가운데에_닉네임_칸_하나를_감쌀_만큼만_넓다() {
        Rectangle form = bounds("login-form");
        Rectangle section = bounds("nickname-1-section");

        assertEquals(FORM_Y, form.y);
        assertEquals(FORM_HEIGHT, form.height);
        assertNear(400, form.x + form.width / 2, 1, "가운데 정렬");
        assertTrue(form.width - section.width <= FORM_SIDES, "입력 영역이 닉네임 칸보다 너무 넓습니다: " + form + " / " + section);
    }

    @Test
    void 닉네임_칸은_가장_넓은_8자가_꽉_차는_폭() {
        for (String mode : new String[]{"mode-1p", "mode-2p"}) {
            SwingTree.named(login, mode, JToggleButton.class).doClick();
            for (String name : mode.equals("mode-1p") ? new String[]{"nickname-1"} : new String[]{"nickname-1", "nickname-2"}) {
                JTextField field = SwingTree.named(login, name, JTextField.class);
                FontMetrics metrics = field.getFontMetrics(field.getFont());
                Insets insets = field.getInsets();
                int inner = field.getWidth() - insets.left - insets.right;
                int widest = widestNickname(metrics);

                assertTrue(inner >= widest, mode + " " + name + "에 가장 넓은 8자(" + widest + "px)가 다 보이지 않습니다: " + inner);
                assertTrue(inner <= widest + 4, mode + " " + name + "이 8자보다 너무 넓습니다: " + inner + " / " + widest);
            }
        }
    }

    @Test
    void 입력_영역_아래에_큰_빈_자리가_없다() {
        for (String mode : new String[]{"mode-1p", "mode-2p"}) {
            SwingTree.named(login, mode, JToggleButton.class).doClick();
            Rectangle form = bounds("login-form");
            Rectangle section = bounds("nickname-1-section");
            Rectangle message = bounds("login-message");

            assertEquals(FORM_HEIGHT, form.height, mode + " 입력 영역 높이");
            int below = form.y + form.height - (section.y + section.height);
            assertTrue(below <= 28, mode + " 닉네임 칸 아래 빈 자리가 큽니다: " + below + "px");
            assertTrue(form.contains(message) && message.y >= section.y + section.height,
                    mode + " 오류 문구는 닉네임 칸 아래 남은 자리에 들어가야 합니다: " + message);
        }
    }

    @Test
    void 줄어든_자리에도_오류_문구가_한_줄로_다_들어간다() {
        JLabel message = SwingTree.named(login, "login-message", JLabel.class);
        FontMetrics metrics = message.getFontMetrics(message.getFont());

        assertTrue(message.getHeight() >= metrics.getAscent() + metrics.getDescent(), "오류 문구 줄이 글자보다 낮습니다.");
        for (String text : new String[]{NicknameRule.LENGTH_MESSAGE, NicknameRule.HANGUL_MESSAGE, NicknameRule.DIGITS_MESSAGE}) {
            assertTrue(metrics.stringWidth(text) <= message.getWidth(), "1P 입력 영역에 다 들어가지 않습니다: " + text);
        }
    }

    @Test
    void 일P_이P_버튼은_입력_영역_양옆에_붙어_있다() {
        Rectangle form = bounds("login-form");
        Rectangle one = bounds("mode-1p");
        Rectangle two = bounds("mode-2p");

        // 참고 화면 버튼 62x79 → 77x99
        assertEquals(77, one.width);
        assertEquals(99, one.height);
        assertEquals(one.getSize(), two.getSize());
        assertTrue(one.x + one.width <= form.x && form.x - (one.x + one.width) <= 12, "1P가 입력 영역 왼쪽에 붙어야 합니다: " + one);
        assertTrue(two.x >= form.x + form.width && two.x - (form.x + form.width) <= 12, "2P가 입력 영역 오른쪽에 붙어야 합니다: " + two);
        int middle = form.y + form.height / 2;
        assertTrue(Math.abs(one.y + one.height / 2 - middle) <= 10, "1P가 입력 영역과 세로 가운데가 맞지 않습니다.");
        assertTrue(Math.abs(two.y + two.height / 2 - middle) <= 10, "2P가 입력 영역과 세로 가운데가 맞지 않습니다.");
    }

    @Test
    void 일P_이P_글자는_버튼_한가운데에_있다() {
        // 고른 쪽과 고르지 않은 쪽을 모두 본다
        for (String selected : new String[]{"mode-1p", "mode-2p"}) {
            SwingTree.named(login, selected, JToggleButton.class).doClick();
            for (String name : new String[]{"mode-1p", "mode-2p"}) {
                JToggleButton button = SwingTree.named(login, name, JToggleButton.class);
                Rectangle ink = ink(button);
                String state = name + (button.isSelected() ? "(고름)" : "(안 고름)");

                assertNear(button.getWidth() / 2, ink.x + ink.width / 2, 2, state + " 글자 가로 가운데");
                assertNear(button.getHeight() / 2, ink.y + ink.height / 2, 2, state + " 글자 세로 가운데");
            }
        }
    }

    @Test
    void 시작하기와_종료는_입력_영역_아래_한_줄이다() {
        Rectangle form = bounds("login-form");
        AbstractButton start = SwingTree.button(login, "시작하기");
        AbstractButton exit = SwingTree.button(login, "종   료");
        Rectangle startBox = SwingTree.boundsIn(start, login);
        Rectangle exitBox = SwingTree.boundsIn(exit, login);

        assertTrue(SwingTree.findButton(login, "로비 입장").isEmpty(), "'로비 입장' 버튼은 '시작하기'로 바꿉니다.");
        assertTrue(SwingTree.findButton(login, "종료").isEmpty(), "'종료' 버튼은 '종   료'로 띄어 씁니다.");
        assertNotSame(SwingTree.named(login, "login-form", JComponent.class), start.getParent(), "시작하기는 입력 영역 밖에 둡니다.");
        assertTrue(startBox.y >= form.y + form.height && exitBox.y >= form.y + form.height, "버튼이 입력 영역 아래에 있어야 합니다.");
        assertTrue(startBox.y - (form.y + form.height) <= 12, "버튼 줄이 입력 영역 바로 아래에 붙어야 합니다: " + startBox);
        assertEquals(startBox.y, exitBox.y, "두 버튼은 한 줄입니다.");
        assertEquals(35, startBox.height);
        assertTrue(startBox.x + startBox.width <= exitBox.x, "시작하기가 종료 왼쪽에 있어야 합니다.");
        int formCenter = form.x + form.width / 2;
        int rowCenter = (startBox.x + exitBox.x + exitBox.width) / 2;
        assertTrue(Math.abs(rowCenter - formCenter) <= 2, "버튼 줄이 입력 영역 가운데 아래에 와야 합니다.");
        assertTrue(startBox.y + startBox.height <= 600);
    }

    @Test
    void 이P_모드에서는_입력_영역이_칸_둘만큼_넓어지고_1P_2P가_바깥으로_간다() {
        Rectangle single = bounds("login-form");
        SwingTree.named(login, "mode-2p", JToggleButton.class).doClick();
        Rectangle form = bounds("login-form");
        Rectangle first = bounds("nickname-1-section");
        Rectangle second = bounds("nickname-2-section");
        Rectangle one = bounds("mode-1p");
        Rectangle two = bounds("mode-2p");

        assertTrue(form.contains(first) && form.contains(second) && first.x + first.width <= second.x,
                "두 닉네임 칸이 나란히 들어가야 합니다: " + first + " / " + second);
        assertTrue(form.width - (first.width + second.width) <= FORM_SIDES + 10, "입력 영역이 칸 둘보다 너무 넓습니다: " + form);
        assertEquals(single.y, form.y);
        assertEquals(single.height, form.height);
        assertNear(400, form.x + form.width / 2, 1, "가운데 정렬");
        assertTrue(one.x >= 0 && one.x + one.width <= form.x, one.toString());
        assertTrue(two.x >= form.x + form.width && two.x + two.width <= 800, two.toString());

        SwingTree.named(login, "mode-1p", JToggleButton.class).doClick();
        assertEquals(single, bounds("login-form"));
    }

    @Test
    void 닉네임_규칙_안내_문구는_없다() {
        for (String text : SwingTree.texts(login)) {
            assertTrue(!text.contains("2~8자") && !text.contains("영문, 숫자"), "안내 문구가 남아 있습니다: " + text);
        }
    }

    @Test
    void 오른쪽_아래_로고는_없다() throws Exception {
        assertTrue(SwingTree.find(login, "login-logo").isEmpty(), "로고 컴포넌트가 남아 있습니다.");
        assertTrue(SwingTree.all(login, JLabel.class).stream().noneMatch(label -> "Hansung Arcade".equals(label.getText())),
                "'Hansung Arcade' 글자가 남아 있습니다.");

        BufferedImage art = art();
        BufferedImage screen = SwingTree.render(login);
        for (int[] p : new int[][]{{600, 570}, {700, 576}, {785, 590}}) {
            assertSameAsArt(art, screen, p[0], p[1]);
        }
    }

    private Rectangle bounds(String name) {
        return SwingTree.boundsIn(SwingTree.named(login, name, JComponent.class), login);
    }

    /** 닉네임으로 받는 글자(완성 한글 가~힣, 영문, 숫자) 가운데 가장 넓은 글자 8개 폭 */
    private static int widestNickname(FontMetrics metrics) {
        int widest = 0;
        for (char c = '가'; c <= '힣'; c++) {
            widest = Math.max(widest, metrics.charWidth(c));
        }
        for (char c = '0'; c <= 'z'; c++) {
            if (Character.isLetterOrDigit(c)) {
                widest = Math.max(widest, metrics.charWidth(c));
            }
        }
        return widest * NicknameRule.MAX_LENGTH;
    }

    /** 버튼을 글자 있이·없이 그려 달라진 픽셀이 차지하는 칸 (테두리 포함한 글자 자리) */
    private static Rectangle ink(JToggleButton button) {
        String text = button.getText();
        BufferedImage with = SwingTree.render(button);
        button.setText("");
        BufferedImage without = SwingTree.render(button);
        button.setText(text);

        Rectangle ink = null;
        for (int y = 0; y < with.getHeight(); y++) {
            for (int x = 0; x < with.getWidth(); x++) {
                if (with.getRGB(x, y) != without.getRGB(x, y)) {
                    Rectangle pixel = new Rectangle(x, y, 1, 1);
                    ink = ink == null ? pixel : ink.union(pixel);
                }
            }
        }
        assertNotNull(ink, button.getName() + "에 글자가 그려지지 않았습니다.");
        return ink;
    }

    private static BufferedImage art() throws Exception {
        try (var input = LoginPanel.class.getResourceAsStream("/etc/hsu_arcade.png")) {
            assertNotNull(input, "배경 그림 resources/etc/hsu_arcade.png가 없습니다.");
            return ImageIO.read(input);
        }
    }

    private static void assertSameAsArt(BufferedImage art, BufferedImage screen, int x, int y) {
        assertEquals(art.getRGB(x, y) | 0xFF000000, screen.getRGB(x, y) | 0xFF000000,
                "(" + x + "," + y + ")가 배경 그림과 다릅니다.");
    }

    private static void assertNear(int expected, int actual, int delta, String message) {
        assertTrue(Math.abs(expected - actual) <= delta, message + ": " + actual);
    }
}

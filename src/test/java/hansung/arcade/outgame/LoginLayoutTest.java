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
import javax.swing.JToggleButton;
import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 로그인 화면 배치를 참고 화면(640x477 로그인)처럼: 아래 가운데 주황 입력 영역, 그 양옆에 붙은 1P·2P,
 * 입력 영역 아래 한 줄의 버튼, 오른쪽 아래 로고. 참고 화면 값을 800x600으로 옮겼다 (가로 x1.25, 세로 x1.258).
 * 배경은 800x600 그림(resources/etc/hsu_arcade.png)을 아래 여백 없이 창 전체에 깔고, 그 위에 로그인 영역을 올린다.
 */
@ExtendWith(RunOnEdt.class)
class LoginLayoutTest {

    private LoginPanel login;

    @BeforeEach
    void setUp() {
        login = new LoginPanel(new OutgameSession(new MockLobbyService()), new SceneManager(screen -> { }));
        login.setSize(800, 600);
        SwingTree.layout(login);
    }

    @Test
    void 배경은_800x600_그림을_여백_없이_창_전체에_깐다() throws Exception {
        BufferedImage art;
        try (var input = LoginPanel.class.getResourceAsStream("/etc/hsu_arcade.png")) {
            assertNotNull(input, "배경 그림 resources/etc/hsu_arcade.png가 없습니다.");
            art = ImageIO.read(input);
        }
        assertEquals(800, art.getWidth());
        assertEquals(600, art.getHeight());

        assertEquals(new Rectangle(0, 0, 800, 600), bounds("login-background"));
        BufferedImage screen = SwingTree.render(login);
        // 입력 영역·버튼·로고가 없는 자리는 그림 그대로 보인다 (늘이거나 줄이지 않는다)
        for (int[] p : new int[][]{{10, 10}, {400, 100}, {790, 200}, {60, 380}, {400, 380}, {20, 590}}) {
            assertEquals(art.getRGB(p[0], p[1]) | 0xFF000000, screen.getRGB(p[0], p[1]) | 0xFF000000,
                    "(" + p[0] + "," + p[1] + ")가 배경 그림과 다릅니다.");
        }
        assertTrue(bounds("login-form").y >= 384, "입력 영역은 그림 아래쪽(참고 화면 3:2 아래)에 올린다.");
    }

    @Test
    void 일P_모드의_입력_영역은_아래_가운데() {
        // 참고 화면 184~447 x 312~437 → 230~559 x 392~550
        assertEquals(new Rectangle(230, 392, 330, 158), bounds("login-form"));
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
    void 로비_입장과_종료는_입력_영역_아래_한_줄이다() {
        Rectangle form = bounds("login-form");
        AbstractButton enter = SwingTree.button(login, "로비 입장");
        AbstractButton exit = SwingTree.button(login, "종료");
        Rectangle enterBox = SwingTree.boundsIn(enter, login);
        Rectangle exitBox = SwingTree.boundsIn(exit, login);

        assertNotSame(SwingTree.named(login, "login-form", JComponent.class), enter.getParent(), "로비 입장은 입력 영역 밖에 둡니다.");
        assertTrue(enterBox.y >= form.y + form.height && exitBox.y >= form.y + form.height, "버튼이 입력 영역 아래에 있어야 합니다.");
        assertEquals(enterBox.y, exitBox.y, "두 버튼은 한 줄입니다.");
        assertEquals(35, enterBox.height);
        assertTrue(enterBox.x + enterBox.width <= exitBox.x, "로비 입장이 종료 왼쪽에 있어야 합니다.");
        int formCenter = form.x + form.width / 2;
        int rowCenter = (enterBox.x + exitBox.x + exitBox.width) / 2;
        assertTrue(Math.abs(rowCenter - formCenter) <= 2, "버튼 줄이 입력 영역 가운데 아래에 와야 합니다.");
        assertTrue(enterBox.y + enterBox.height <= 600);
    }

    @Test
    void 이P_모드에서는_입력_영역이_넓어지고_1P_2P가_바깥으로_간다() {
        SwingTree.named(login, "mode-2p", JToggleButton.class).doClick();
        Rectangle form = bounds("login-form");
        Rectangle one = bounds("mode-1p");
        Rectangle two = bounds("mode-2p");

        assertTrue(form.width >= 560, "두 닉네임 칸이 들어가게 넓어져야 합니다: " + form);
        assertNear(400, form.x + form.width / 2, 2, "가운데 정렬");
        assertTrue(one.x >= 0 && one.x + one.width <= form.x, one.toString());
        assertTrue(two.x >= form.x + form.width && two.x + two.width <= 800, two.toString());

        SwingTree.named(login, "mode-1p", JToggleButton.class).doClick();
        assertEquals(new Rectangle(230, 392, 330, 158), bounds("login-form"));
    }

    @Test
    void 로고는_오른쪽_아래() {
        JLabel logo = SwingTree.named(login, "login-logo", JLabel.class);
        Rectangle box = SwingTree.boundsIn(logo, login);

        assertEquals("Hansung Arcade", logo.getText());
        assertTrue(box.x >= 560 && box.y >= 550 && box.x + box.width <= 800 && box.y + box.height <= 600, box.toString());
    }

    private Rectangle bounds(String name) {
        return SwingTree.boundsIn(SwingTree.named(login, name, JComponent.class), login);
    }

    private static void assertNear(int expected, int actual, int delta, String message) {
        assertTrue(Math.abs(expected - actual) <= delta, message + ": " + actual);
    }
}

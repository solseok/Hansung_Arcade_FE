package hansung.arcade.outgame;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.ui.OutgamePalette;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static hansung.arcade.outgame.P3.assertP3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2. 로그인 화면을 참고 화면(1번)처럼: 주황 그러데이션 입력 영역, 칸 위의 노란 그러데이션 탭,
 * 어두운 회색 틀 안의 파란 이름표와 흰 입력 칸, 광택이 도는 1P·2P 버튼. 색은 참고 화면(Display P3)에서 읽었다.
 */
@ExtendWith(RunOnEdt.class)
class LoginLookTest {

    private LoginPanel login;

    @BeforeEach
    void setUp() {
        login = new LoginPanel(new OutgameSession(new MockLobbyService()), new SceneManager(screen -> { }));
        login.setSize(800, 600);
        SwingTree.layout(login);
    }

    @Test
    void 참고_화면에서_읽은_색_토큰() {
        assertP3(248, 215, 113, OutgamePalette.LOGIN_PANEL_TOP);
        assertP3(238, 127, 49, OutgamePalette.LOGIN_PANEL_BOTTOM);
        assertP3(183, 90, 34, OutgamePalette.LOGIN_PANEL_EDGE);
        assertP3(251, 232, 174, OutgamePalette.LOGIN_TAB_TOP);
        assertP3(255, 255, 84, OutgamePalette.LOGIN_TAB_BOTTOM);
        assertP3(209, 86, 38, OutgamePalette.LOGIN_TAB_TEXT);
        assertP3(98, 96, 98, OutgamePalette.LOGIN_SECTION);
        assertP3(48, 111, 192, OutgamePalette.LOGIN_LABEL_BOX);
    }

    @Test
    void 입력_영역은_위가_밝은_주황_그러데이션() {
        BufferedImage screen = SwingTree.render(login);
        Rectangle form = bounds("login-form");

        Color top = color(screen, form.x + 8, form.y + 10);
        Color bottom = color(screen, form.x + 8, form.y + form.height - 10);

        assertTrue(top.getRed() > 220 && bottom.getRed() > 220, "주황이 아닙니다: " + top + " / " + bottom);
        assertTrue(top.getGreen() > bottom.getGreen() + 40, "위가 더 밝은 노랑 쪽이어야 합니다: " + top + " / " + bottom);
    }

    @Test
    void 닉네임_칸_위에_노란_그러데이션_탭() {
        BufferedImage screen = SwingTree.render(login);
        JLabel tab = SwingTree.named(login, "nickname-1-tab", JLabel.class);
        Rectangle box = bounds("nickname-1-tab");

        Color top = color(screen, box.x + 5, box.y + 3);
        Color bottom = color(screen, box.x + 5, box.y + box.height - 3);

        assertEquals("닉네임", tab.getText());
        for (Color c : new Color[]{top, bottom}) {
            assertTrue(c.getRed() > 220 && c.getGreen() > 170 && c.getBlue() < 200, "노랑이 아닙니다: " + c);
        }
        assertTrue(Math.abs(top.getBlue() - bottom.getBlue()) > 20 || Math.abs(top.getGreen() - bottom.getGreen()) > 20,
                "그러데이션이 아닙니다: " + top + " / " + bottom);
        assertTrue(bounds("nickname-1-section").y >= box.y + box.height - 2, "탭이 닉네임 칸 위에 있어야 합니다.");
    }

    @Test
    void 어두운_회색_틀_안에_파란_이름표와_흰_입력_칸() {
        BufferedImage screen = SwingTree.render(login);
        Rectangle section = bounds("nickname-1-section");
        Rectangle label = bounds("nickname-1-label");
        Rectangle field = bounds("nickname-1");

        assertEquals(OutgamePalette.LOGIN_SECTION, color(screen, section.x + 4, section.y + section.height / 2));
        assertEquals(OutgamePalette.LOGIN_LABEL_BOX, color(screen, label.x + 3, label.y + label.height - 4));
        assertEquals(Color.WHITE, color(screen, field.x + 3, field.y + field.height / 2));
        assertTrue(section.contains(label) && section.contains(field));
        assertTrue(label.x + label.width <= field.x, "이름표가 입력 칸 왼쪽에 있어야 합니다.");
    }

    @Test
    void 일P_이P_버튼은_위쪽에_광택이_돈다() {
        SwingTree.named(login, "mode-2p", JToggleButton.class).doClick();
        for (String name : new String[]{"mode-1p", "mode-2p"}) {
            JToggleButton button = SwingTree.named(login, name, JToggleButton.class);
            button.setSelected(true);
            BufferedImage image = SwingTree.render(button);
            int x = image.getWidth() / 2;
            double shine = 0;
            for (int y = image.getHeight() / 10; y < image.getHeight() * 3 / 10; y++) {
                shine = Math.max(shine, luminance(color(image, x, y)));
            }
            double middle = luminance(color(image, x, image.getHeight() * 55 / 100));
            assertTrue(shine > middle + 30, name + " 위쪽 광택이 없습니다: " + shine + " / " + middle);
        }
    }

    private Rectangle bounds(String name) {
        return SwingTree.boundsIn(SwingTree.named(login, name, JComponent.class), login);
    }

    private static Color color(BufferedImage image, int x, int y) {
        return new Color(image.getRGB(x, y) & 0xFFFFFF);
    }

    private static double luminance(Color c) {
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }
}

package hansung.arcade.outgame;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.ui.OutgamePalette;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * 로그인 화면에서 기존 테스트가 묶지 않던 모양과 동작.
 * 테스트만 보고 만든 최소 구현(탭 글자·입력 영역 테두리·고른 표시를 그리지 않고, Enter·종료가 동작하지 않음)을 잡는다.
 */
@ExtendWith(RunOnEdt.class)
class LoginDetailTest {

    private LoginPanel login;
    private final AtomicReference<JComponent> displayed = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        SceneManager scenes = new SceneManager(displayed::set);
        scenes.register("lobby", JLabel::new);
        login = new LoginPanel(new OutgameSession(new MockLobbyService()), scenes);
        login.setSize(800, 600);
        SwingTree.layout(login);
    }

    @Test
    void 노란_탭에_닉네임_글자를_주황_빨강으로_쓴다() {
        BufferedImage tab = SwingTree.render(SwingTree.named(login, "nickname-1-tab", JComponent.class));

        int text = count(tab, c -> near(c, OutgamePalette.LOGIN_TAB_TEXT, 40));
        assertTrue(text >= 15, "탭에 '닉네임' 글자가 보이지 않습니다: " + text + "px");
    }

    @Test
    void 입력_영역에는_진한_주황_테두리가_있다() {
        BufferedImage screen = SwingTree.render(login);
        Rectangle form = bounds("login-form");
        int middle = form.y + form.height / 2;

        Color left = new Color(screen.getRGB(form.x + 1, middle) & 0xFFFFFF);
        Color right = new Color(screen.getRGB(form.x + form.width - 2, middle) & 0xFFFFFF);
        assertTrue(near(left, OutgamePalette.LOGIN_PANEL_EDGE, 30), "왼쪽 테두리 색이 아닙니다: " + left);
        assertTrue(near(right, OutgamePalette.LOGIN_PANEL_EDGE, 30), "오른쪽 테두리 색이 아닙니다: " + right);
    }

    @Test
    void 고르지_않은_1P_2P는_고른_쪽보다_흐리다() {
        JToggleButton two = SwingTree.named(login, "mode-2p", JToggleButton.class);
        two.doClick();
        double selected = saturation(SwingTree.render(two));
        SwingTree.named(login, "mode-1p", JToggleButton.class).doClick();
        double unselected = saturation(SwingTree.render(two));

        assertTrue(unselected < selected - 0.15,
                "고르지 않은 2P가 고른 2P와 다르지 않습니다: 채도 " + unselected + " / " + selected);
    }

    @Test
    void 일P_모드에서는_2P_탭과_칸을_숨긴다() {
        for (String name : new String[]{"nickname-2-tab", "nickname-2-section", "nickname-2"}) {
            assertFalse(SwingTree.visibleIn(SwingTree.named(login, name, JComponent.class), login), name + "이 보입니다.");
        }
        SwingTree.named(login, "mode-2p", JToggleButton.class).doClick();
        for (String name : new String[]{"nickname-2-tab", "nickname-2-section", "nickname-2"}) {
            assertTrue(SwingTree.visibleIn(SwingTree.named(login, name, JComponent.class), login), name + "이 보이지 않습니다.");
        }
    }

    @Test
    void 닉네임_칸에서_Enter를_누르면_시작한다() {
        JTextField field = SwingTree.named(login, "nickname-1", JTextField.class);
        field.setText("부기");

        field.postActionEvent();

        assertNotNull(displayed.get(), "Enter로 로비에 들어가지 않았습니다.");
    }

    @Test
    void 규칙에_어긋나면_Enter를_눌러도_머문다() {
        JTextField field = SwingTree.named(login, "nickname-1", JTextField.class);
        field.setText("1234");

        field.postActionEvent();

        assertNull(displayed.get());
        assertTrue(SwingTree.named(login, "login-message", JLabel.class).getText().contains("숫자"));
    }

    @Test
    void 종료를_누르면_창을_닫는다() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "창이 있어야 하는 테스트");
        JFrame frame = new JFrame();
        AtomicBoolean closing = new AtomicBoolean();
        try {
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    closing.set(true);
                }
            });
            frame.add(login);

            SwingTree.button(login, "종   료").doClick();

            assertTrue(closing.get(), "종료를 눌러도 창을 닫지 않습니다.");
        } finally {
            frame.dispose();
        }
    }

    private Rectangle bounds(String name) {
        return SwingTree.boundsIn(SwingTree.named(login, name, JComponent.class), login);
    }

    /** 몸통 아래쪽 왼편(글자·광택 밖) 평균 채도 */
    private static double saturation(BufferedImage image) {
        double sum = 0;
        int n = 0;
        for (int y = image.getHeight() * 6 / 10; y < image.getHeight() * 85 / 100; y++) {
            for (int x = image.getWidth() / 8; x < image.getWidth() / 4; x++) {
                Color c = new Color(image.getRGB(x, y));
                sum += Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null)[1];
                n++;
            }
        }
        return sum / n;
    }

    private static int count(BufferedImage image, java.util.function.Predicate<Color> test) {
        int found = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) > 200 && test.test(new Color(argb & 0xFFFFFF))) {
                    found++;
                }
            }
        }
        return found;
    }

    private static boolean near(Color a, Color b, int tolerance) {
        return Math.abs(a.getRed() - b.getRed()) <= tolerance
                && Math.abs(a.getGreen() - b.getGreen()) <= tolerance
                && Math.abs(a.getBlue() - b.getBlue()) <= tolerance;
    }
}

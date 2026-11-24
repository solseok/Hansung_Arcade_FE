package hansung.arcade.outgame;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.lobby.SampleRooms;
import hansung.arcade.outgame.lobby.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2. 로그인 화면 왼쪽에 1P, 오른쪽에 2P를 두고, 1P면 닉네임 입력 칸 1개, 2P면 2개를 보여 준다.
 * 아이디·비밀번호·서버 선택·게임아이디/이메일아이디 탭은 없다.
 */
@ExtendWith(RunOnEdt.class)
class LoginModeTest {

    private MockLobbyService service;
    private OutgameSession session;
    private AtomicReference<JComponent> displayed;
    private JPanel lobbyView;
    private LoginPanel login;

    @BeforeEach
    void setUp() {
        service = new MockLobbyService(SampleRooms.rooms());
        session = new OutgameSession(service);
        displayed = new AtomicReference<>();
        SceneManager scenes = new SceneManager(displayed::set);
        lobbyView = new JPanel();
        scenes.register("lobby", () -> lobbyView);
        login = new LoginPanel(session, scenes);
        login.setSize(800, 600);
        SwingTree.layout(login);
    }

    @Test
    void 처음에는_1P가_골라져_있고_닉네임_칸이_하나다() {
        JToggleButton one = SwingTree.named(login, "mode-1p", JToggleButton.class);
        JToggleButton two = SwingTree.named(login, "mode-2p", JToggleButton.class);

        assertEquals("1P", one.getText());
        assertEquals("2P", two.getText());
        assertTrue(one.isSelected());
        assertEquals(List.of("nickname-1"), visibleNicknameFields());
    }

    @Test
    void 모드_2P를_고르면_닉네임_칸이_둘_1P로_돌아오면_하나() {
        SwingTree.named(login, "mode-2p", JToggleButton.class).doClick();
        assertEquals(List.of("nickname-1", "nickname-2"), visibleNicknameFields());

        SwingTree.named(login, "mode-1p", JToggleButton.class).doClick();
        assertEquals(List.of("nickname-1"), visibleNicknameFields());
    }

    @Test
    void 버튼_1P는_입력_영역_왼쪽_2P는_오른쪽에_있다() {
        Rectangle form = SwingTree.boundsIn(SwingTree.named(login, "login-form", JComponent.class), login);
        Rectangle one = SwingTree.boundsIn(SwingTree.named(login, "mode-1p", JToggleButton.class), login);
        Rectangle two = SwingTree.boundsIn(SwingTree.named(login, "mode-2p", JToggleButton.class), login);

        assertTrue(one.x + one.width <= form.x, "1P가 입력 영역 왼쪽에 있어야 합니다: " + one + " / " + form);
        assertTrue(two.x >= form.x + form.width, "2P가 입력 영역 오른쪽에 있어야 합니다: " + two + " / " + form);
        assertTrue(new Rectangle(0, 0, 800, 600).contains(one.union(two).union(form)));
    }

    @Test
    void 각_입력_칸에는_몇_번째_플레이어의_닉네임인지_적혀_있다() {
        SwingTree.named(login, "mode-2p", JToggleButton.class).doClick();

        assertEquals("1P 닉네임", SwingTree.named(login, "nickname-1-label", JLabel.class).getText());
        assertEquals("2P 닉네임", SwingTree.named(login, "nickname-2-label", JLabel.class).getText());
    }

    @Test
    void 서버선택_계정탭_아이디_비밀번호가_없다() {
        SwingTree.named(login, "mode-2p", JToggleButton.class).doClick();

        assertTrue(SwingTree.all(login, JPasswordField.class).isEmpty(), "비밀번호 칸이 있습니다.");
        assertTrue(SwingTree.all(login, JRadioButton.class).isEmpty(), "서버 선택 라디오 버튼이 있습니다.");
        assertTrue(SwingTree.all(login, JCheckBox.class).isEmpty(), "서버 선택 체크박스가 있습니다.");
        assertTrue(SwingTree.all(login, JTabbedPane.class).isEmpty(), "게임아이디/이메일아이디 탭이 있습니다.");
        List<String> texts = SwingTree.texts(login);
        for (String banned : List.of("서버", "아이디", "비밀번호", "이메일")) {
            assertTrue(texts.stream().noneMatch(text -> text.contains(banned)), banned + " 글자가 있습니다: " + texts);
        }
    }

    @Test
    void 모드_2P로_로그인하면_두_닉네임을_들고_로비로_간다() {
        SwingTree.named(login, "mode-2p", JToggleButton.class).doClick();
        SwingTree.named(login, "nickname-1", JTextField.class).setText("부기");
        SwingTree.named(login, "nickname-2", JTextField.class).setText("꾸꾸");
        SwingTree.button(login, "시작하기").doClick();

        assertSame(lobbyView, displayed.get());
        assertEquals(List.of("부기", "꾸꾸"), session.nicknames());
        assertEquals(2, session.localPlayers());
    }

    @Test
    void 모드_1P로_돌아가면_2P_칸에_적어_둔_닉네임은_쓰지_않는다() {
        SwingTree.named(login, "mode-2p", JToggleButton.class).doClick();
        SwingTree.named(login, "nickname-1", JTextField.class).setText("부기");
        SwingTree.named(login, "nickname-2", JTextField.class).setText("꾸꾸");
        SwingTree.named(login, "mode-1p", JToggleButton.class).doClick();
        SwingTree.button(login, "시작하기").doClick();

        assertEquals(List.of("부기"), session.nicknames());
    }

    @Test
    void 닉네임_1P와_2P가_같으면_로비로_가지_않는다() {
        SwingTree.named(login, "mode-2p", JToggleButton.class).doClick();
        SwingTree.named(login, "nickname-1", JTextField.class).setText("부기");
        SwingTree.named(login, "nickname-2", JTextField.class).setText("부기");
        SwingTree.button(login, "시작하기").doClick();

        assertNull(displayed.get());
        assertTrue(SwingTree.named(login, "login-message", JLabel.class).getText().contains("같"));
    }

    @Test
    void 모드_2P는_방에_들어가면_두_자리를_차지하고_나오면_두_자리를_비운다() {
        session.login("부기", "꾸꾸");
        int before = service.rooms().getFirst().players();

        Room room = session.join(1);

        assertEquals(before + 2, room.players());
        assertEquals(List.of("부기", "꾸꾸"), session.players().subList(0, 2).stream()
                .map(GameStartInfo.PlayerInfo::nickname).toList());
        session.leave();
        assertEquals(before, service.rooms().getFirst().players());
    }

    private List<String> visibleNicknameFields() {
        return SwingTree.all(login, JTextField.class).stream()
                .filter(field -> SwingTree.visibleIn(field, login))
                .map(JTextField::getName)
                .sorted()
                .toList();
    }
}

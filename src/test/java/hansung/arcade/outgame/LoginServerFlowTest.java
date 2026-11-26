package hansung.arcade.outgame;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.network.ServerException;
import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.lobby.SampleRooms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 온라인 로그인: 서버 답을 기다리는 동안 입력을 막고, 서버가 받아들이면 로비로, 거절하면 서버 문구를 보여 준다.
 * 서버 답은 EDT 밖에서 오므로 테스트도 EDT 밖에서 돌리고 화면 조작만 EDT에 넘긴다.
 */
class LoginServerFlowTest {

    /** 답을 테스트가 정하는 로그인 */
    private static final class ManualLogin implements LoginService {
        final AtomicReference<List<String>> requested = new AtomicReference<>();
        final CompletableFuture<List<LoginPlayer>> result = new CompletableFuture<>();

        @Override
        public CompletableFuture<List<LoginPlayer>> login(List<String> nicknames) {
            requested.set(nicknames);
            return result;
        }
    }

    private final ManualLogin service = new ManualLogin();
    private final AtomicReference<JComponent> displayed = new AtomicReference<>();
    private final JPanel lobbyView = new JPanel();
    private OutgameSession session;
    private LoginPanel login;

    @BeforeEach
    void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            session = new OutgameSession(new MockLobbyService(SampleRooms.rooms()));
            SceneManager scenes = new SceneManager(displayed::set);
            scenes.register("lobby", () -> lobbyView);
            login = new LoginPanel(session, scenes, service);
            login.setSize(800, 600);
            SwingTree.layout(login);
        });
    }

    private static void onEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private void typeAndStart(String nickname) throws Exception {
        onEdt(() -> {
            SwingTree.named(login, "nickname-1", JTextField.class).setText(nickname);
            SwingTree.button(login, "시작하기").doClick();
        });
    }

    private String message() throws Exception {
        AtomicReference<String> text = new AtomicReference<>();
        onEdt(() -> text.set(SwingTree.named(login, "login-message", JLabel.class).getText()));
        return text.get();
    }

    private boolean startEnabled() throws Exception {
        AtomicReference<Boolean> enabled = new AtomicReference<>();
        onEdt(() -> enabled.set(SwingTree.button(login, "시작하기").isEnabled()));
        return enabled.get();
    }

    @Test
    void 서버_답을_기다리는_동안_입력을_막고_접속_중이라고_알린다() throws Exception {
        typeAndStart(" 상상부기 ");

        assertEquals(List.of("상상부기"), service.requested.get());
        assertEquals(LoginPanel.CONNECTING_MESSAGE, message());
        assertFalse(startEnabled());
        onEdt(() -> {
            assertFalse(SwingTree.named(login, "nickname-1", JTextField.class).isEditable());
            assertFalse(SwingTree.named(login, "mode-2p", AbstractButton.class).isEnabled());
        });
        assertNull(displayed.get());
    }

    @Test
    void 서버가_받아들이면_서버가_정한_번호로_로비에_들어간다() throws Exception {
        typeAndStart("상상부기");

        service.result.complete(List.of(new LoginService.LoginPlayer(12, "상상부기")));
        onEdt(() -> { });

        assertSame(lobbyView, displayed.get());
        assertEquals(List.of("상상부기"), session.nicknames());
        assertEquals(List.of(12L), session.playerIds());
        assertTrue(startEnabled());
    }

    @Test
    void 서버가_거절하면_서버_문구를_보여_주고_다시_입력할_수_있다() throws Exception {
        typeAndStart("상상부기");

        service.result.completeExceptionally(new ServerException("NICKNAME_DUPLICATE", "이미 쓰고 있는 닉네임입니다: 상상부기"));
        onEdt(() -> { });

        assertNull(displayed.get());
        assertEquals("이미 쓰고 있는 닉네임입니다: 상상부기", message());
        assertTrue(startEnabled());
        onEdt(() -> assertTrue(SwingTree.named(login, "nickname-1", JTextField.class).isEditable()));
    }

    @Test
    void 서버에_연결할_수_없으면_그렇게_알린다() throws Exception {
        typeAndStart("상상부기");

        service.result.completeExceptionally(new ConnectException("Connection refused"));
        onEdt(() -> { });

        assertEquals("서버에 연결할 수 없습니다.", message());
    }

    @Test
    void 규칙에_어긋난_닉네임은_서버에_보내지_않는다() throws Exception {
        typeAndStart("ㅁㄴㅇㄹ");

        assertNull(service.requested.get());
        assertTrue(message().contains("완성된 한글"));
    }

    @Test
    void 서버의_긴_문구는_1P_폭에서_두_줄로_나눠_다_보여_준다() throws Exception {
        String longest = "닉네임에는 공백이나 특수문자 없이 한글, 영문, 숫자만 쓸 수 있습니다.";
        onEdt(() -> {
            JLabel label = SwingTree.named(login, "login-message", JLabel.class);
            var frc = new java.awt.font.FontRenderContext(null, true, true);

            var layout = LoginPanel.MessageLine.layout(longest, label.getFont(), frc, label.getWidth(), label.getHeight());

            assertEquals(2, layout.lines().size(), layout.toString());
            assertEquals(longest, String.join(" ", layout.lines()));
            assertTrue(layout.font().getSize2D() >= LoginPanel.MessageLine.MIN_TWO_LINES);
            for (String line : layout.lines()) {
                assertTrue(layout.font().getStringBounds(line, frc).getWidth() <= label.getWidth(), line);
            }
            var short1 = LoginPanel.MessageLine.layout(NicknameRule.LENGTH_MESSAGE, label.getFont(), frc,
                    label.getWidth(), label.getHeight());
            assertEquals(List.of(NicknameRule.LENGTH_MESSAGE), short1.lines());
            assertEquals(label.getFont().getSize2D(), short1.font().getSize2D());
        });
    }

    @Test
    void 연결이_끊겨_돌아오면_이유를_보여_준다() throws Exception {
        onEdt(() -> login.enter("서버와 연결이 끊겼습니다."));

        assertEquals("서버와 연결이 끊겼습니다.", message());
    }
}

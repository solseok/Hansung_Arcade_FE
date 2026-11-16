package hansung.arcade.outgame;

import hansung.arcade.HansungArcadeApplication;
import hansung.arcade.core.game.GameProperties;
import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.lobby.MockLobbyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.JTextField;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1. 로그인 화면에서 서버 주소·포트 입력을 없앤다.
 * 서버 주소와 포트는 application.properties(나중에 .env로 덮어쓰기)에서 정하므로 사용자가 입력하지 않는다.
 */
@ExtendWith(RunOnEdt.class)
class LoginServerFieldsTest {

    @Test
    void 로그인_화면에_서버주소와_포트_입력이_없다() {
        LoginPanel login = new LoginPanel(new OutgameSession(new MockLobbyService()), new SceneManager(screen -> { }));

        List<String> texts = SwingTree.texts(login);
        assertTrue(texts.stream().noneMatch(text -> text.contains("서버") || text.contains("포트")), texts.toString());
        for (JTextField field : SwingTree.all(login, JTextField.class)) {
            assertTrue(field.getName() != null && field.getName().startsWith("nickname-"),
                    "닉네임 말고 다른 입력 칸이 있습니다: " + field.getName() + " = " + field.getText());
        }
    }

    @Test
    void 세션은_서버주소와_포트를_받지도_들고있지도_않는다() {
        Set<String> methods = Arrays.stream(OutgameSession.class.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertFalse(methods.contains("host"), "OutgameSession.host()가 남아 있습니다.");
        assertFalse(methods.contains("port"), "OutgameSession.port()가 남아 있습니다.");

        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        assertEquals(List.of("부기"), session.nicknames());
        assertEquals("부기", session.nickname());
    }

    @Test
    void 서버주소와_포트는_설정_파일에서_읽는다() {
        GameProperties properties = GameProperties.load(HansungArcadeApplication.class, List.of());

        assertFalse(properties.get("network.host").isBlank());
        assertTrue(properties.getInt("network.port", -1) > 0);
    }
}

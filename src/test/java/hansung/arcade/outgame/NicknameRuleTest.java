package hansung.arcade.outgame;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.lobby.MockLobbyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.text.Normalizer;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 3. 닉네임은 2~8자이고, ㅁㄴㅇㄹ처럼 완성되지 않은 한글(자음·모음 낱자)은 받지 않는다.
 */
@ExtendWith(RunOnEdt.class)
class NicknameRuleTest {

    @ParameterizedTest
    @ValueSource(strings = {"가나", "부기", "ab", "a1", "가나다라마바사아", "Player12", "한성Bugi"})
    void 두자에서_여덟자까지_받는다(String nickname) {
        assertEquals(nickname, NicknameRule.validate(nickname));
    }

    @Test
    void 앞뒤_공백은_떼고_센다() {
        assertEquals("가나", NicknameRule.validate("  가나  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "가", "a", "가나다라마바사아자", "Player123"})
    void 두자보다_짧거나_여덟자보다_길면_받지_않는다(String nickname) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> NicknameRule.validate(nickname));
        assertTrue(e.getMessage().contains("2~8자"), e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ㅁㄴㅇㄹ",       // 호환용 자모 자음
            "ㅁㄴㅇㄹㅁㄴㅇㄹ", // 인게임에서 잘렸던 닉네임
            "ㅏㅏ",          // 모음만
            "가ㅁ",          // 완성 글자 뒤 낱자
            "부기ㅋㅋ",       // 섞여 있어도 안 된다
            "ᄀᄀ", // 첫가끝 자모(조합용) 낱자
            "ﾡﾡ"  // 반각 자모
    })
    void 완성되지_않은_한글은_받지_않는다(String nickname) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> NicknameRule.validate(nickname));
        assertTrue(e.getMessage().contains("완성된 한글"), e.getMessage());
    }

    @Test
    void 자모로_풀어_쓴_완성_글자는_합쳐서_받는다() {
        // macOS에서 붙여 넣은 글은 "가"가 ㄱ+ㅏ 조합용 자모(NFD)로 들어올 수 있다. 합치면 완성 글자이므로 받는다.
        String decomposed = Normalizer.normalize("가나", Normalizer.Form.NFD);
        assertEquals(4, decomposed.length());

        assertEquals("가나", NicknameRule.validate(decomposed));
    }

    @Test
    void 세션_로그인도_같은_규칙을_쓴다() {
        OutgameSession session = new OutgameSession(new MockLobbyService());

        assertThrows(IllegalArgumentException.class, () -> session.login("가"));
        assertThrows(IllegalArgumentException.class, () -> session.login("ㅁㄴㅇㄹ"));
        assertThrows(IllegalArgumentException.class, () -> session.login("부기", "ㅋㅋ"));
        session.login("가나다라마바사아");
        assertEquals(List.of("가나다라마바사아"), session.nicknames());
    }

    @Test
    void 로그인_화면은_규칙에_어긋나면_이유를_보여주고_머문다() {
        AtomicReference<JComponent> displayed = new AtomicReference<>();
        SceneManager scenes = new SceneManager(displayed::set);
        scenes.register("lobby", JLabel::new);
        LoginPanel login = new LoginPanel(new OutgameSession(new MockLobbyService()), scenes);

        SwingTree.named(login, "nickname-1", JTextField.class).setText("ㅁㄴㅇㄹ");
        SwingTree.button(login, "시작하기").doClick();

        assertNull(displayed.get());
        assertTrue(SwingTree.named(login, "login-message", JLabel.class).getText().contains("완성된 한글"));
    }

    @Test
    void 안내_문구에_허용_범위가_적혀_있다() {
        LoginPanel login = new LoginPanel(new OutgameSession(new MockLobbyService()), new SceneManager(c -> { }));

        assertTrue(SwingTree.texts(login).stream().anyMatch(text -> text.contains("2~8자")), SwingTree.texts(login).toString());
    }
}

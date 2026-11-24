package hansung.arcade.outgame;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.lobby.LobbyPanel;
import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.lobby.WaitingRoomPanel;
import hansung.arcade.outgame.ui.OutgameFonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import java.awt.Container;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1. 굴림체 + 도트 렌더링은 읽기 어렵다. 사용자가 바꿀 수 있는 문구(방 제목, 닉네임, 채팅)만 굴림체로 쓰고 도트로 찍지 않는다.
 * 바꿀 수 없는 문구(버튼·상태·안내 글자)는 참고 화면처럼 굵은 게임 글꼴로 쓴다.
 */
@ExtendWith(RunOnEdt.class)
class FontRoleTest {

    /** 사용자가 바꿀 수 있는 문구를 쓰는 컴포넌트 이름 */
    private static final Set<String> CHANGEABLE = Set.of(
            "room-title", "lobby-nickname", "slot-name", "chat-log", "chat-input", "nickname-1", "nickname-2");
    private static final String GULIM = OutgameFonts.gulimChe(12).getFamily(Locale.ENGLISH);

    @Test
    void 고정_문구_글꼴은_굴림체가_아닌_굵은_글꼴() {
        Font label = OutgameFonts.label(14);

        assertTrue(label.isBold());
        assertNotEquals(GULIM, label.getFamily(Locale.ENGLISH));
        assertEquals(14f, label.getSize2D());
    }

    @Test
    void 로그인_화면() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        LoginPanel login = new LoginPanel(session, new SceneManager(screen -> { }));

        assertRoles(login);
    }

    @Test
    void 로비() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        SceneManager scenes = new SceneManager(screen -> { });
        LobbyPanel lobby = new LobbyPanel(session, scenes);
        scenes.register("lobby", lobby);
        scenes.show("lobby", null);

        assertRoles(lobby);
        assertEquals("부기 님", SwingTree.named(lobby, "lobby-nickname", JLabel.class).getText());
    }

    @Test
    void 대기실() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        SceneManager scenes = new SceneManager(screen -> { });
        WaitingRoomPanel waiting = new WaitingRoomPanel(session, scenes);
        scenes.register("waiting", waiting);
        scenes.show("waiting", session.join(2));

        assertRoles(waiting);
    }

    @Test
    void 바꿀_수_있는_문구는_도트가_아니라_매끈하게_그린다() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        SceneManager scenes = new SceneManager(screen -> { });
        LobbyPanel lobby = new LobbyPanel(session, scenes);
        scenes.register("lobby", lobby);
        scenes.show("lobby", null);
        JLabel title = SwingTree.named(lobby, "room-title", JLabel.class);
        title.setSize(title.getPreferredSize());

        BufferedImage image = SwingTree.render(title);

        assertTrue(hasPartialAlpha(image), "제목 글자 가장자리에 중간 알파가 없습니다. 아직 도트로 찍고 있습니다.");
        WaitingRoomPanel waiting = new WaitingRoomPanel(session, scenes);
        for (JTextComponent field : SwingTree.all(waiting, JTextComponent.class)) {
            assertNotEquals(RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
                    field.getClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING), field.getName());
        }
    }

    /** 바꿀 수 있는 문구는 굴림체, 나머지 글자는 굵은 게임 글꼴인지 */
    private static void assertRoles(Container root) {
        String label = OutgameFonts.label(12).getFamily(Locale.ENGLISH);
        for (JComponent component : SwingTree.all(root, JComponent.class)) {
            String name = component.getName();
            if (name != null && CHANGEABLE.contains(name)) {
                assertEquals(GULIM, component.getFont().getFamily(Locale.ENGLISH), name + "은(는) 굴림체여야 합니다.");
                continue;
            }
            if (component instanceof JTextComponent) {
                throw new AssertionError("이름이 정해지지 않은 입력 칸이 있습니다: " + name);
            }
            String text = SwingTree.text(component);
            if ((component instanceof JLabel || component instanceof AbstractButton) && text != null && !text.isBlank()) {
                Font font = component.getFont();
                assertEquals(label, font.getFamily(Locale.ENGLISH), "'" + text + "'(" + name + ")은(는) 게임 글꼴이어야 합니다.");
                assertTrue(font.isBold(), "'" + text + "'(" + name + ")은(는) 굵어야 합니다.");
            }
        }
    }

    private static boolean hasPartialAlpha(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha > 0 && alpha < 255) {
                    return true;
                }
            }
        }
        return false;
    }
}

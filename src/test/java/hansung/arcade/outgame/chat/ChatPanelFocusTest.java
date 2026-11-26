package hansung.arcade.outgame.chat;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.lobby.MockLobbyService;
import hansung.arcade.outgame.lobby.WaitingRoomPanel;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 채팅의 글자 커서와 키보드 포커스는 입력 칸만 갖는다. 받은 글 목록(chat-log)은 편집할 수 없지만 JDK는 포커스를 받은
 * 편집 불가 글 칸에도 캐럿을 보이므로, 목록이 포커스를 받지 않고 누르면 입력 칸으로 넘겨야 한다.
 */
class ChatPanelFocusTest {

    @Test
    void 받은_글_목록은_포커스를_받지_않아_캐럿이_생기지_않는다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ChatPanel chat = new ChatPanel(message -> { });
            JTextPane log = find(chat, JTextPane.class);

            assertFalse(log.isFocusable());
            press(log);
            assertFalse(log.getCaret().isVisible());
            assertFalse(log.getCaret().isSelectionVisible());
        });
    }

    @Test
    void 목록을_누르면_포커스가_입력_칸으로_간다() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "화면이 없는 환경이라 실제 포커스는 확인하지 않습니다.");
        AtomicReference<JFrame> frame = new AtomicReference<>();
        AtomicReference<ChatPanel> chat = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                ChatPanel panel = new ChatPanel(message -> { });
                panel.setBounds(0, 0, 427, 160);
                JFrame window = new JFrame("chat focus");
                window.setLayout(null);
                window.getContentPane().setPreferredSize(panel.getPreferredSize());
                window.getContentPane().add(panel);
                window.pack();
                window.setVisible(true);
                window.toFront();
                frame.set(window);
                chat.set(panel);
            });
            waitUntil(() -> frame.get().isFocused());
            // 창을 앞으로 가져오지 못하는 환경(다른 앱이 앞에 있음 등)에서는 포커스를 확인할 수 없다
            assumeTrue(frame.get().isFocused(), "창이 포커스를 받지 못해 확인하지 않습니다.");
            JTextPane log = find(chat.get(), JTextPane.class);
            JTextField input = find(chat.get(), JTextField.class);
            JButton send = find(chat.get(), JButton.class);
            // 포커스를 다른 곳(전송 버튼)에 두고 목록을 누른다
            SwingUtilities.invokeAndWait(send::requestFocusInWindow);
            waitUntil(() -> focusOwner() == send);
            assumeTrue(focusOwner() == send, "전송 버튼으로 포커스를 옮기지 못해 확인하지 않습니다.");

            SwingUtilities.invokeAndWait(() -> press(log));
            waitUntil(() -> focusOwner() == input);

            SwingUtilities.invokeAndWait(() -> {
                assertSame(input, focusOwner());
                assertFalse(log.getCaret().isVisible());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame.get() != null) {
                    frame.get().dispose();
                }
            });
        }
    }

    @Test
    void 대기실에_들어오면_입력_칸이_포커스를_갖는다() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "화면이 없는 환경이라 실제 포커스는 확인하지 않습니다.");
        AtomicReference<JFrame> frame = new AtomicReference<>();
        AtomicReference<WaitingRoomPanel> waiting = new AtomicReference<>();
        AtomicReference<SceneManager> scenes = new AtomicReference<>();
        AtomicReference<OutgameSession> session = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame window = new JFrame("waiting focus");
                JPanel blank = new JPanel();
                blank.setPreferredSize(new Dimension(800, 600));
                window.setContentPane(blank);
                window.pack();
                window.setVisible(true);
                window.toFront();
                OutgameSession user = new OutgameSession(new MockLobbyService());
                user.login("부기");
                // GameWindow처럼 화면을 갈아 끼우고 화면 자체에 포커스를 달라고 한다
                SceneManager manager = new SceneManager(screen -> {
                    window.setContentPane(screen);
                    window.pack();
                    screen.requestFocusInWindow();
                });
                WaitingRoomPanel room = new WaitingRoomPanel(user, manager);
                manager.register("waiting", room);
                frame.set(window);
                waiting.set(room);
                scenes.set(manager);
                session.set(user);
            });
            waitUntil(() -> frame.get().isFocused());
            assumeTrue(frame.get().isFocused(), "창이 포커스를 받지 못해 확인하지 않습니다.");

            SwingUtilities.invokeAndWait(() -> scenes.get().show("waiting", session.get().quickJoin()));
            JTextField input = find(waiting.get(), JTextField.class);
            waitUntil(() -> focusOwner() == input);

            SwingUtilities.invokeAndWait(() -> assertSame(input, focusOwner()));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame.get() != null) {
                    frame.get().dispose();
                }
            });
        }
    }

    private static Component focusOwner() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    }

    private static void press(Component target) {
        long now = System.currentTimeMillis();
        int x = target.getWidth() / 2;
        int y = target.getHeight() / 2;
        target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, now, InputEvent.BUTTON1_DOWN_MASK,
                x, y, 1, false, MouseEvent.BUTTON1));
        target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_RELEASED, now + 50, 0, x, y, 1, false,
                MouseEvent.BUTTON1));
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws Exception {
        for (int i = 0; i < 40; i++) {
            AtomicReference<Boolean> done = new AtomicReference<>(false);
            SwingUtilities.invokeAndWait(() -> done.set(condition.getAsBoolean()));
            if (done.get()) {
                return;
            }
            Thread.sleep(50);
        }
    }

    private static <T> T find(Container parent, Class<T> type) {
        T found = search(parent, type);
        if (found == null) {
            throw new AssertionError(type.getSimpleName() + " 없음");
        }
        return found;
    }

    private static <T> T search(Container parent, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T found = search(container, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

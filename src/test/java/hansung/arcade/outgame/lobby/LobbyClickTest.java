package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * 12. 로비의 방만들기·바로시작·방 칸이 실제로 눌려야 한다. doClick이 아니라 창을 띄워 화면 좌표로 마우스 누르기·떼기를
 * 보내서 확인한다(화면이 없는 환경에서는 건너뛴다). 누르는 동안·마우스를 올렸을 때 모양도 바뀌어서 눌린 것이 보여야 한다.
 */
class LobbyClickTest {

    private JFrame frame;
    private LobbyPanel lobby;
    private OutgameSession session;
    private final AtomicReference<JComponent> displayed = new AtomicReference<>();
    private final List<Integer> asked = new ArrayList<>();
    private final JLabel waitingView = new JLabel("waiting");

    @AfterEach
    void close() throws Exception {
        if (frame != null) {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void 실제_창에서_방_칸을_누르면_입장을_묻는다() throws Exception {
        open();

        click(center("room-card-1"));

        assertEquals(List.of(1), asked);
    }

    @Test
    void 실제_창에서_방_칸의_제목_위를_눌러도_입장을_묻는다() throws Exception {
        open();

        click(center("room-title"));

        assertEquals(List.of(1), asked);
    }

    @Test
    void 실제_창에서_바로시작을_누르면_대기실로_간다() throws Exception {
        open();

        click(center("quick-join"));

        assertSame(waitingView, displayed.get());
    }

    @Test
    void 실제_창에서_방만들기를_누르면_방을_만들고_대기실로_간다() throws Exception {
        open();

        click(center("create-room"));

        assertSame(waitingView, displayed.get());
        assertEquals("클릭 테스트", session.room().title());
        assertEquals(4, session.room().capacity());
    }

    @Test
    void 누르는_동안_방_칸이_눌린_모양이_된다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RoomCard card = RoomCard.of(new Room(1, "누름", 1, 8, "캠프 08", Room.Status.WAITING));
            card.setSize(367, 90);
            BufferedImage normal = SwingTree.render(card);
            card.getModel().setRollover(true);
            BufferedImage hover = SwingTree.render(card);
            card.getModel().setArmed(true);
            card.getModel().setPressed(true);
            BufferedImage pressed = SwingTree.render(card);

            int x = 88;
            int y = 45;
            assertNotEquals(normal.getRGB(x, y), hover.getRGB(x, y), "마우스를 올려도 모양이 그대로입니다.");
            assertTrue(luminance(pressed.getRGB(x, y)) < luminance(normal.getRGB(x, y)) - 10, "눌러도 어두워지지 않습니다.");
        });
    }

    private void open() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "화면이 없는 환경이라 실제 창 클릭은 확인하지 않습니다.");
        SwingUtilities.invokeAndWait(() -> {
            session = new OutgameSession(new MockLobbyService(SampleRooms.rooms()));
            session.login("부기");
            frame = new JFrame("클릭 확인");
            SceneManager scenes = new SceneManager(view -> {
                displayed.set(view);
                frame.setContentPane(view);
                frame.pack();
            });
            lobby = new LobbyPanel(session, scenes, (parent, room) -> {
                asked.add(room.number());
                return false;
            }, parent -> Optional.of(new RoomCreator.RoomRequest("클릭 테스트", 4)));
            scenes.register("lobby", lobby);
            scenes.register("waiting", () -> waitingView);
            scenes.show("lobby", null);
            frame.setVisible(true);
        });
        // 창이 뜨고 배치가 끝날 때까지 기다린다
        SwingUtilities.invokeAndWait(() -> { });
    }

    private Point center(String name) throws Exception {
        AtomicReference<Point> point = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            Rectangle box = SwingTree.boundsIn(SwingTree.named(lobby, name, JComponent.class), lobby);
            point.set(new Point(box.x + box.width / 2, box.y + box.height / 2));
        });
        return point.get();
    }

    /** 로비 좌표 (x, y)를 마우스로 누르고 뗀다. 이벤트는 창으로 보내 Swing이 실제처럼 대상 컴포넌트를 찾게 한다 */
    private void click(Point at) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Point origin = SwingUtilities.convertPoint(frame.getContentPane(), 0, 0, frame);
            int x = origin.x + at.x;
            int y = origin.y + at.y;
            long now = System.currentTimeMillis();
            EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
            queue.postEvent(new MouseEvent(frame, MouseEvent.MOUSE_MOVED, now, 0, x, y, 0, false));
            queue.postEvent(new MouseEvent(frame, MouseEvent.MOUSE_PRESSED, now + 1, InputEvent.BUTTON1_DOWN_MASK,
                    x, y, 1, false, MouseEvent.BUTTON1));
            queue.postEvent(new MouseEvent(frame, MouseEvent.MOUSE_RELEASED, now + 2, 0, x, y, 1, false,
                    MouseEvent.BUTTON1));
            queue.postEvent(new MouseEvent(frame, MouseEvent.MOUSE_CLICKED, now + 2, 0, x, y, 1, false,
                    MouseEvent.BUTTON1));
        });
        // 위에서 넣은 이벤트가 모두 처리될 때까지 기다린다
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static double luminance(int argb) {
        Color c = new Color(argb & 0xFFFFFF);
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }
}

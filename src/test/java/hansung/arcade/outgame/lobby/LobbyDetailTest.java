package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.Dialogs;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.MenuSelectionManager;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * 로비에서 기존 테스트가 묶지 않던 모양과 동작.
 * 테스트만 보고 만든 최소 구현(방 상태 글자·입장 표시 사람·맵 그림이 없고, 입장을 묻지 않고, 방만들기가 늘 취소이고,
 * 바로시작 실패를 잡지 않고, 메뉴·나가기·빈 목록 안내가 없음)을 잡는다.
 */
@ExtendWith(RunOnEdt.class)
class LobbyDetailTest {

    private final AtomicReference<JComponent> displayed = new AtomicReference<>();
    private StubService service;
    private OutgameSession session;
    private SceneManager scenes;
    private LobbyPanel lobby;

    private void open(LobbyPanel panel) {
        lobby = panel;
        scenes.register("lobby", lobby);
        scenes.register("waiting", JLabel::new);
        scenes.show("lobby", null);
        displayed.set(null);
        lobby.setSize(800, 600);
        SwingTree.layout(lobby);
    }

    private void open() {
        service = new StubService();
        session = new OutgameSession(service);
        session.login("부기");
        scenes = new SceneManager(displayed::set);
        open(new LobbyPanel(session, scenes, (parent, room) -> true));
    }

    @Test
    void 방_칸마다_WAITING_PLAYING_상태_글자가_있고_빈_칸은_비어_있다() {
        open();
        Set<String> seen = new HashSet<>();
        for (RoomCard card : allCards()) {
            String status = SwingTree.named(card, "room-status", JLabel.class).getText();
            if (card.isSkeleton()) {
                assertTrue(status == null || status.isBlank(), card.getName() + ": " + status);
            } else {
                String expected = card.room().orElseThrow().status() == Room.Status.PLAYING ? "PLAYING" : "WAITING";
                assertEquals(expected, status, card.getName());
                seen.add(status);
            }
        }
        assertEquals(Set.of("WAITING", "PLAYING"), seen);
    }

    @Test
    void 들어갈_수_있는_방의_입장_표시에는_노란_사람이_있고_못_들어가면_없다() {
        open();
        RoomCard open = card(room -> room.canJoin());
        RoomCard full = card(room -> !room.canJoin());

        int openYellow = yellow(SwingTree.render(SwingTree.named(open, "room-entry", JComponent.class)));
        int fullYellow = yellow(SwingTree.render(SwingTree.named(full, "room-entry", JComponent.class)));
        assertTrue(openYellow >= 10, "들어갈 수 있는 방 입장 표시에 노란 사람이 없습니다: " + openYellow);
        assertEquals(0, fullYellow, "못 들어가는 방 입장 표시에 노란 사람이 있습니다.");
    }

    @Test
    void 방_칸의_맵_미리보기는_맵_그림을_그린다() {
        open();
        BufferedImage preview = SwingTree.render(SwingTree.named(card(room -> true), "map-preview", JComponent.class));

        // 아래 맵 이름 띠를 빼고 위쪽만 본다. 단색이면 색이 몇 개뿐이다
        Set<Integer> colors = new HashSet<>();
        for (int y = 2; y < preview.getHeight() * 6 / 10; y++) {
            for (int x = 2; x < preview.getWidth() - 2; x++) {
                colors.add(preview.getRGB(x, y));
            }
        }
        assertTrue(colors.size() >= 30, "맵 그림이 아니라 단색입니다: 색 " + colors.size() + "개");
    }

    @Test
    void 방번호는_번호_상자_가운데에_쓴다() {
        open();
        JLabel number = SwingTree.named(card(room -> true), "room-number", JLabel.class);
        Rectangle ink = ink(number);

        assertTrue(Math.abs(ink.x + ink.width / 2 - number.getWidth() / 2) <= 2,
                "방번호가 가로 가운데가 아닙니다: " + ink + " / 폭 " + number.getWidth());
        assertTrue(Math.abs(ink.y + ink.height / 2 - number.getHeight() / 2) <= 2,
                "방번호가 세로 가운데가 아닙니다: " + ink + " / 높이 " + number.getHeight());
    }

    @Test
    void 이P면_로비에_두_닉네임을_모두_보여_준다() {
        service = new StubService();
        session = new OutgameSession(service);
        session.login("부기", "꾸꾸");
        scenes = new SceneManager(displayed::set);
        open(new LobbyPanel(session, scenes, (parent, room) -> true));

        String text = SwingTree.named(lobby, "lobby-nickname", JLabel.class).getText();
        assertTrue(text.contains("부기") && text.contains("꾸꾸"), text);
    }

    @Test
    void 방이_하나도_없으면_안내한다() {
        service = new StubService();
        service.empty = true;
        session = new OutgameSession(service);
        session.login("부기");
        scenes = new SceneManager(displayed::set);
        open(new LobbyPanel(session, scenes, (parent, room) -> true));

        assertEquals("표시할 방이 없습니다.", SwingTree.named(lobby, "lobby-message", JLabel.class).getText());
    }

    @Test
    void 바로시작에_실패하면_이유를_보여_주고_로비에_머문다() {
        open();
        service.quickJoinError = new IllegalStateException("들어갈 수 있는 방이 없습니다.");

        SwingTree.named(lobby, "quick-join", AbstractButton.class).doClick();

        assertNull(displayed.get(), "실패했는데 화면이 바뀌었습니다.");
        assertEquals("들어갈 수 있는 방이 없습니다.", SwingTree.named(lobby, "lobby-message", JLabel.class).getText());
    }

    @Test
    void 기본_입장_질문은_창을_띄워_묻고_아니오면_들어가지_않는다() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "창이 있어야 하는 테스트");
        service = new StubService();
        session = new OutgameSession(service);
        session.login("부기");
        scenes = new SceneManager(displayed::set);
        open(new LobbyPanel(session, scenes));

        List<String> asked = Dialogs.answer(pane -> pane.setValue(JOptionPane.NO_OPTION));
        card(Room::canJoin).doClick();

        assertTrue(asked.stream().anyMatch(text -> text.contains(EntryPrompt.QUESTION)), "입장을 묻지 않았습니다: " + asked);
        assertNull(displayed.get(), "아니오를 골랐는데 들어갔습니다.");

        Dialogs.answer(pane -> pane.setValue(JOptionPane.YES_OPTION));
        card(Room::canJoin).doClick();
        assertNotNull(displayed.get(), "예를 골랐는데 들어가지 않았습니다.");
    }

    @Test
    void 기본_방만들기는_창에서_제목과_정원을_받아_방을_만든다() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "창이 있어야 하는 테스트");
        service = new StubService();
        session = new OutgameSession(service);
        session.login("부기");
        scenes = new SceneManager(displayed::set);
        open(new LobbyPanel(session, scenes, (parent, room) -> true));

        List<String> asked = Dialogs.answer(pane -> {
            SwingTree.named(pane, "room-title-input", JTextField.class).setText("새로 만든 방");
            SwingTree.all(pane, JComboBox.class).getFirst().setSelectedItem(4);
            pane.setValue(JOptionPane.OK_OPTION);
        });
        SwingTree.named(lobby, "create-room", AbstractButton.class).doClick();

        assertFalse(asked.isEmpty(), "방만들기 창이 뜨지 않았습니다.");
        assertNotNull(displayed.get(), "방을 만들고 대기실로 가지 않았습니다.");
        assertEquals("새로 만든 방", session.room().title());
        assertEquals(4, session.room().capacity());
    }

    @Test
    void 아래_띠에_메뉴와_나가기가_있다() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "창이 있어야 하는 테스트");
        open();
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
            frame.add(lobby);
            frame.pack();
            frame.setVisible(true);

            SwingTree.button(lobby, "메뉴").doClick();
            List<String> items = new ArrayList<>();
            for (Window window : Window.getWindows()) {
                SwingTree.all(window, JMenuItem.class).stream().filter(Component::isShowing)
                        .forEach(item -> items.add(item.getText()));
            }
            assertTrue(items.contains("방 목록 새로고침") && items.contains("음소거"), "메뉴 항목: " + items);
            MenuSelectionManager.defaultManager().clearSelectedPath();

            SwingTree.button(lobby, "나가기").doClick();
            assertTrue(closing.get(), "나가기를 눌러도 창을 닫지 않습니다.");
        } finally {
            frame.dispose();
        }
    }

    // ── 도우미 ───────────────────────────────────────────

    /** 쪽을 넘기며 모은 모든 방 칸·빈 칸 */
    private List<RoomCard> allCards() {
        List<RoomCard> cards = new ArrayList<>(SwingTree.all(lobby, RoomCard.class));
        AbstractButton next = SwingTree.named(lobby, "page-next", AbstractButton.class);
        while (next.isEnabled()) {
            next.doClick();
            SwingTree.layout(lobby);
            cards.addAll(SwingTree.all(lobby, RoomCard.class));
        }
        return cards;
    }

    /** 첫 쪽부터 찾아 조건에 맞는 방 칸 (찾은 쪽에 머문다) */
    private RoomCard card(java.util.function.Predicate<Room> test) {
        AbstractButton prev = SwingTree.named(lobby, "page-prev", AbstractButton.class);
        while (prev.isEnabled()) {
            prev.doClick();
        }
        AbstractButton next = SwingTree.named(lobby, "page-next", AbstractButton.class);
        while (true) {
            SwingTree.layout(lobby);
            for (RoomCard card : SwingTree.all(lobby, RoomCard.class)) {
                if (card.room().filter(test).isPresent()) {
                    return card;
                }
            }
            if (!next.isEnabled()) {
                throw new AssertionError("조건에 맞는 방이 없습니다.");
            }
            next.doClick();
        }
    }

    /** 글자 있이·없이 그려 달라진 칸 */
    private static Rectangle ink(JLabel label) {
        String text = label.getText();
        BufferedImage with = SwingTree.render(label);
        label.setText("");
        BufferedImage without = SwingTree.render(label);
        label.setText(text);
        Rectangle ink = null;
        for (int y = 0; y < with.getHeight(); y++) {
            for (int x = 0; x < with.getWidth(); x++) {
                if (with.getRGB(x, y) != without.getRGB(x, y)) {
                    Rectangle pixel = new Rectangle(x, y, 1, 1);
                    ink = ink == null ? pixel : ink.union(pixel);
                }
            }
        }
        assertNotNull(ink, label.getName() + "에 글자가 그려지지 않았습니다.");
        return ink;
    }

    private static int yellow(BufferedImage image) {
        int found = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                Color c = new Color(argb & 0xFFFFFF);
                if ((argb >>> 24) > 200 && c.getRed() > 200 && c.getGreen() > 160 && c.getBlue() < 120) {
                    found++;
                }
            }
        }
        return found;
    }

    /** 목업 서비스를 감싸 방 목록을 비우거나 바로시작을 실패시킬 수 있는 서비스 */
    private static final class StubService implements LobbyService {
        private final MockLobbyService delegate = new MockLobbyService(SampleRooms.rooms());
        boolean empty;
        RuntimeException quickJoinError;

        @Override
        public List<Room> rooms() {
            return empty ? List.of() : delegate.rooms();
        }

        @Override
        public Room join(int roomNumber, int seats) {
            return delegate.join(roomNumber, seats);
        }

        @Override
        public Room create(String title, int capacity, int seats) {
            return delegate.create(title, capacity, seats);
        }

        @Override
        public boolean isHost() {
            return delegate.isHost();
        }

        @Override
        public Room quickJoin(int seats) {
            if (quickJoinError != null) {
                throw quickJoinError;
            }
            return delegate.quickJoin(seats);
        }

        @Override
        public void leave() {
            delegate.leave();
        }

        @Override
        public Room updateRoom(String map, Room.Status status) {
            return delegate.updateRoom(map, status);
        }
    }
}

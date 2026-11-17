package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 5. 방 칸의 "입장"(…) 버튼을 없애고, 방 칸 자체를 누르면 "입장하시겠습니까?"를 묻는 모달을 띄운다.
 */
@ExtendWith(RunOnEdt.class)
class LobbyEntryTest {

    private MockLobbyService service;
    private OutgameSession session;
    private AtomicReference<JComponent> displayed;
    private JLabel waitingView;
    private LobbyPanel lobby;
    private final List<Integer> asked = new ArrayList<>();
    private final AtomicBoolean answer = new AtomicBoolean();

    @BeforeEach
    void setUp() {
        service = new MockLobbyService();
        session = new OutgameSession(service);
        session.login("부기");
        displayed = new AtomicReference<>();
        SceneManager scenes = new SceneManager(displayed::set);
        waitingView = new JLabel();
        lobby = new LobbyPanel(session, scenes, (parent, room) -> {
            asked.add(room.number());
            return answer.get();
        });
        scenes.register("lobby", lobby);
        scenes.register("waiting", () -> waitingView);
        scenes.show("lobby", null);
    }

    @Test
    void 방_칸_안에_입장_버튼이_없다() {
        for (RoomCard card : SwingTree.all(lobby, RoomCard.class)) {
            assertTrue(SwingTree.all(card, AbstractButton.class).isEmpty(),
                    card.getName() + " 안에 따로 누르는 버튼이 있습니다.");
        }
        List<String> buttonTexts = SwingTree.all(lobby, AbstractButton.class).stream().map(AbstractButton::getText).toList();
        assertTrue(buttonTexts.stream().noneMatch(text -> "입장".equals(text) || "...".equals(text) || "…".equals(text)),
                buttonTexts.toString());
    }

    @Test
    void 질문은_입장하시겠습니까() {
        assertEquals("입장하시겠습니까?", EntryPrompt.QUESTION);
    }

    @Test
    void 방_칸을_누르면_묻고_예를_고르면_대기실로_간다() {
        answer.set(true);

        SwingTree.named(lobby, "room-card-1", RoomCard.class).doClick();

        assertEquals(List.of(1), asked);
        assertSame(waitingView, displayed.get());
        assertEquals(2, room(1).players());
        assertEquals(1, session.room().number());
    }

    @Test
    void 아니오를_고르면_로비에_머물고_인원도_그대로다() {
        answer.set(false);

        SwingTree.named(lobby, "room-card-1", RoomCard.class).doClick();

        assertEquals(List.of(1), asked);
        assertSame(lobby, displayed.get());
        assertEquals(1, room(1).players());
    }

    @Test
    void 게임_중인_방은_묻지_않고_안내만_한다() {
        answer.set(true);
        RoomCard playing = playingCard();

        playing.doClick();

        assertTrue(asked.isEmpty());
        assertSame(lobby, displayed.get());
        assertTrue(SwingTree.named(lobby, "lobby-message", JLabel.class).getText().contains("게임 중"));
    }

    @Test
    void 가득_찬_방은_묻지_않고_안내만_한다() {
        answer.set(true);

        cardWhere(room -> room.number() == 3, "가득 찬 3번 방").doClick();

        assertTrue(asked.isEmpty());
        assertSame(lobby, displayed.get());
        assertTrue(SwingTree.named(lobby, "lobby-message", JLabel.class).getText().contains("가득"));
    }

    @Test
    void 빈_칸은_눌러도_아무_일이_없다() {
        answer.set(true);
        AbstractButton next = SwingTree.button(lobby, "다음쪽");
        while (next.isEnabled()) {
            next.doClick();
        }
        RoomCard skeleton = (RoomCard) SwingTree.namedStartingWith(lobby, "room-skeleton-").getFirst();

        skeleton.doClick();

        assertTrue(asked.isEmpty());
        assertSame(lobby, displayed.get());
    }

    private RoomCard playingCard() {
        return cardWhere(room -> room.status() == Room.Status.PLAYING, "게임 중인 방");
    }

    /** 방 목록 순서와 상관없이 쪽을 넘기며 condition에 맞는 방 칸을 찾는다. */
    private RoomCard cardWhere(java.util.function.Predicate<Room> condition, String what) {
        AbstractButton next = SwingTree.button(lobby, "다음쪽");
        while (true) {
            for (RoomCard card : SwingTree.all(lobby, RoomCard.class)) {
                if (card.room().filter(condition).isPresent()) {
                    return card;
                }
            }
            if (!next.isEnabled()) {
                throw new AssertionError(what + " 칸이 없습니다.");
            }
            next.doClick();
        }
    }

    private Room room(int number) {
        return service.rooms().stream().filter(room -> room.number() == number).findFirst().orElseThrow();
    }
}

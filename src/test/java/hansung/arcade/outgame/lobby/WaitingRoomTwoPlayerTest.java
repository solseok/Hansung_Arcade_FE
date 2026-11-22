package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.Team;
import hansung.arcade.outgame.ui.TextLabel;
import hansung.arcade.outgame.ui.WaitingRoomLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 2P(한 컴퓨터 2인)면 대기실의 캐릭터·팀 칸을 왼쪽 클릭은 1P, 오른쪽 클릭(맥의 Ctrl+클릭 포함)은 2P가 고른다 */
@ExtendWith(RunOnEdt.class)
class WaitingRoomTwoPlayerTest {

    private OutgameSession session;
    private WaitingRoomPanel waiting;

    private void enter(String... nicknames) {
        session = new OutgameSession(new MockLobbyService());
        session.login(nicknames);
        SceneManager scenes = new SceneManager(screen -> { });
        waiting = new WaitingRoomPanel(session, scenes, (parent, current) -> Optional.empty());
        scenes.register("waiting", waiting);
        scenes.register("lobby", JLabel::new);
        scenes.show("waiting", session.create("둘이서", 8));
        waiting.setSize(WaitingRoomLayout.WIDTH, WaitingRoomLayout.HEIGHT);
        SwingTree.layout(waiting);
    }

    private AbstractButton button(String name) {
        return SwingTree.named(waiting, name, AbstractButton.class);
    }

    /** 오른쪽 버튼으로 칸 가운데를 눌렀다 뗀다 */
    private static void rightClick(Component cell) {
        int x = cell.getWidth() / 2;
        int y = cell.getHeight() / 2;
        long now = System.currentTimeMillis();
        cell.dispatchEvent(new MouseEvent(cell, MouseEvent.MOUSE_PRESSED, now, InputEvent.BUTTON3_DOWN_MASK, x, y, 1,
                true, MouseEvent.BUTTON3));
        cell.dispatchEvent(new MouseEvent(cell, MouseEvent.MOUSE_RELEASED, now, 0, x, y, 1, false, MouseEvent.BUTTON3));
    }

    @Test
    void 왼쪽_클릭은_1P의_캐릭터를_오른쪽_클릭은_2P의_캐릭터를_고른다() {
        enter("부기", "꾸꾸");
        Mascot second = session.character(1);

        button("character-" + Mascot.KKUKKU.id()).doClick();
        assertEquals(Mascot.KKUKKU, session.character(0));
        assertEquals(second, session.character(1));

        rightClick(button("character-" + Mascot.KKOKKO.id()));
        assertEquals(Mascot.KKUKKU, session.character(0));
        assertEquals(Mascot.KKOKKO, session.character(1));

        // 두 사람이 고른 칸이 모두 골라져 보인다
        assertTrue(button("character-" + Mascot.KKUKKU.id()).isSelected());
        assertTrue(button("character-" + Mascot.KKOKKO.id()).isSelected());
        assertFalse(button("character-" + Mascot.SANGSANGBUGI.id()).isSelected());
        // 배너는 마지막으로 고른 2P의 캐릭터다
        assertEquals("꼬꼬", SwingTree.named(waiting, "character-name", JLabel.class).getText());
    }

    @Test
    void 오른쪽_클릭으로_2P의_팀_색과_랜덤을_고른다() {
        enter("부기", "꾸꾸");
        Team first = session.team(0);

        rightClick(button("team-" + Team.GREEN.name()));
        assertEquals(first, session.team(0));
        assertEquals(Team.GREEN, session.team(1));
        assertTrue(button("team-" + Team.GREEN.name()).isSelected());
        assertTrue(button("team-" + first.name()).isSelected());

        button("team-" + Team.PINK.name()).doClick();
        assertEquals(Team.PINK, session.team(0));
        assertFalse(button("team-" + first.name()).isSelected());

        rightClick(button("character-random"));
        assertTrue(session.randomCharacter(1));
        assertFalse(session.randomCharacter(0));
    }

    /** 왼쪽 버튼으로 칸 가운데를 눌렀다 뗀다. ctrlOnPress·ctrlOnRelease는 누를 때·뗄 때 Ctrl을 쥐었는지 */
    private static void leftClick(Component cell, boolean ctrlOnPress, boolean ctrlOnRelease) {
        int x = cell.getWidth() / 2;
        int y = cell.getHeight() / 2;
        long now = System.currentTimeMillis();
        int press = InputEvent.BUTTON1_DOWN_MASK | (ctrlOnPress ? InputEvent.CTRL_DOWN_MASK : 0);
        int release = ctrlOnRelease ? InputEvent.CTRL_DOWN_MASK : 0;
        // 맥은 Ctrl+클릭을 누를 때 팝업 트리거로 보낸다
        cell.dispatchEvent(new MouseEvent(cell, MouseEvent.MOUSE_PRESSED, now, press, x, y, 1, ctrlOnPress,
                MouseEvent.BUTTON1));
        cell.dispatchEvent(new MouseEvent(cell, MouseEvent.MOUSE_RELEASED, now, release, x, y, 1, false,
                MouseEvent.BUTTON1));
        cell.dispatchEvent(new MouseEvent(cell, MouseEvent.MOUSE_CLICKED, now, release, x, y, 1, false,
                MouseEvent.BUTTON1));
    }

    @Test
    void 맥의_Ctrl_클릭은_오른쪽_클릭처럼_2P가_고른다() {
        enter("부기", "꾸꾸");
        Mascot first = session.character(0);
        Team firstTeam = session.team(0);

        leftClick(button("character-" + Mascot.KKOKKO.id()), true, true);
        leftClick(button("team-" + Team.GREEN.name()), true, true);

        assertEquals(first, session.character(0));
        assertEquals(Mascot.KKOKKO, session.character(1));
        assertEquals(firstTeam, session.team(0));
        assertEquals(Team.GREEN, session.team(1));
        assertFalse(button("character-" + Mascot.KKOKKO.id()).getModel().isPressed(), "칸이 눌린 채 남았습니다");
    }

    @Test
    void Ctrl은_누를_때_쥐었는지로_정한다() {
        enter("부기", "꾸꾸");
        Mascot second = session.character(1);

        // 누른 뒤에 Ctrl을 쥐어도 왼쪽 클릭(1P)이다
        leftClick(button("character-" + Mascot.KKOKKO.id()), false, true);
        assertEquals(Mascot.KKOKKO, session.character(0));
        assertEquals(second, session.character(1));

        // Ctrl을 쥐고 누른 뒤 떼고 손을 놓아도 오른쪽 클릭(2P)이고, 칸이 눌린 채 남지 않는다
        AbstractButton cell = button("character-" + Mascot.SANGZZI.id());
        leftClick(cell, true, false);
        assertEquals(Mascot.KKOKKO, session.character(0));
        assertEquals(Mascot.SANGZZI, session.character(1));
        assertFalse(cell.getModel().isPressed());
        assertFalse(cell.getModel().isArmed());

        // 그다음 Ctrl 없는 클릭은 다시 1P다
        leftClick(cell, false, false);
        assertEquals(Mascot.SANGZZI, session.character(0));
    }

    @Test
    void 둘이_같은_칸을_골라도_된다() {
        enter("부기", "꾸꾸");

        button("character-" + Mascot.SANGZZI.id()).doClick();
        rightClick(button("character-" + Mascot.SANGZZI.id()));

        assertEquals(Mascot.SANGZZI, session.character(0));
        assertEquals(Mascot.SANGZZI, session.character(1));
        assertTrue(button("character-" + Mascot.SANGZZI.id()).isSelected());
    }

    @Test
    void 둘이_들어오면_알림_줄에_클릭_안내를_보여_준다() {
        enter("부기", "꾸꾸");
        assertEquals(WaitingRoomPanel.NOTICE_TEXT_2P, SwingTree.named(waiting, "notice", TextLabel.class).getText());
    }

    @Test
    void 혼자면_오른쪽_클릭은_아무것도_고르지_않는다() {
        enter("부기");
        Mascot before = session.character();
        Team team = session.team();

        rightClick(button("character-" + Mascot.KKOKKO.id()));
        rightClick(button("team-" + Team.GREEN.name()));
        leftClick(button("character-" + Mascot.KKOKKO.id()), true, true);

        assertEquals(before, session.character());
        assertEquals(team, session.team());
        assertFalse(button("character-" + Mascot.KKOKKO.id()).isSelected());
    }
}

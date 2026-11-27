package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.Team;
import hansung.arcade.outgame.ui.WaitingRoomLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 대기실 팀 칸: 팀전이면 고를 수 있는 색만 켠다 */
@ExtendWith(RunOnEdt.class)
class WaitingRoomTeamModeTest {

    private OutgameSession session;
    private SceneManager scenes;
    private WaitingRoomPanel waiting;

    @BeforeEach
    void setUp() {
        session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        scenes = new SceneManager(screen -> { });
        waiting = new WaitingRoomPanel(session, scenes, (parent, current) -> Optional.empty());
        scenes.register("waiting", waiting);
        scenes.register("lobby", JLabel::new);
    }

    private void enter(RoomMode mode) {
        scenes.show("waiting", session.create("한 판", 8, mode));
        waiting.setSize(WaitingRoomLayout.WIDTH, WaitingRoomLayout.HEIGHT);
        waiting.doLayout();
    }

    private Set<Team> enabledTeams() {
        Set<Team> enabled = EnumSet.noneOf(Team.class);
        for (Team team : Team.values()) {
            if (SwingTree.named(waiting, "team-" + team.name(), AbstractButton.class).isEnabled()) {
                enabled.add(team);
            }
        }
        return enabled;
    }

    @Test
    void 개인전_대기실은_팀_칸_8개가_모두_켜진다() {
        enter(RoomMode.SOLO);

        assertEquals(EnumSet.allOf(Team.class), enabledTeams());
        assertEquals("한 판", SwingTree.named(waiting, "room-title", JLabel.class).getText());
    }

    @Test
    void 팀전_캠프08_대기실은_빨강_파랑_칸만_켜지고_나머지는_고를_수_없다고_알린다() {
        enter(RoomMode.TEAM);

        assertEquals(EnumSet.of(Team.RED, Team.BLUE), enabledTeams());
        AbstractButton yellow = SwingTree.named(waiting, "team-" + Team.YELLOW.name(), AbstractButton.class);
        assertEquals("노랑 (팀전에서는 고를 수 없음)", yellow.getToolTipText());
        assertEquals("빨강", SwingTree.named(waiting, "team-" + Team.RED.name(), AbstractButton.class).getToolTipText());
        assertEquals("[팀전] 한 판", SwingTree.named(waiting, "room-title", JLabel.class).getText());
    }
}

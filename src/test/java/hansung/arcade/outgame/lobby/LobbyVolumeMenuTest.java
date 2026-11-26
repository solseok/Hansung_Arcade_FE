package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.audio.Sound;
import hansung.arcade.outgame.ui.VolumeControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** 로비 아래 띠의 메뉴에서 음소거 아래에 전체 음량 줄이 있고, 지금 음량을 보여 준다 */
class LobbyVolumeMenuTest {

    @AfterEach
    void 원래대로() {
        MenuSelectionManager.defaultManager().clearSelectedPath();
        Sound.setVolume(Sound.MAX_VOLUME);
    }

    @Test
    void 메뉴를_열면_음소거_아래에_전체_음량_줄이_있다() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "창이 있어야 하는 테스트");
        Sound.setVolume(70);
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        LobbyPanel lobby = new LobbyPanel(session, new SceneManager(view -> { }), (parent, room) -> false);
        JFrame frame = new JFrame();
        try {
            frame.add(lobby);
            frame.pack();
            frame.setVisible(true);

            SwingTree.button(lobby, "메뉴").doClick();

            List<VolumeControl> shown = new ArrayList<>();
            for (Window window : Window.getWindows()) {
                SwingTree.all(window, VolumeControl.class).stream().filter(Component::isShowing).forEach(shown::add);
            }
            assertEquals(1, shown.size(), "메뉴에 전체 음량 줄이 없습니다");
            VolumeControl volume = shown.get(0);
            JPopupMenu popup = (JPopupMenu) SwingUtilities.getAncestorOfClass(JPopupMenu.class, volume);
            Component[] items = popup.getComponents();
            int index = List.of(items).indexOf(volume);
            assertTrue(index > 0 && items[index - 1] instanceof JCheckBoxMenuItem mute
                    && mute.getText().equals("음소거"), "전체 음량 줄이 음소거 바로 아래에 있어야 합니다");
            assertEquals(70, SwingTree.named(volume, "volume-slider", JSlider.class).getValue());
            assertSame(popup, volume.getParent());
        } finally {
            frame.dispose();
        }
    }
}

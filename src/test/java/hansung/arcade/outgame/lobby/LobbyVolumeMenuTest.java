package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.audio.Sound;
import hansung.arcade.outgame.audio.SoundSettings;
import hansung.arcade.outgame.ui.VolumeControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * 로비 아래 띠의 메뉴에서 음소거 아래에 전체 음량 줄이 있고, 메뉴를 만들 때의 음량을 보여 준다.
 * 슬라이더를 끄는 중에 메뉴가 닫혀도 바꾼 음량을 저장한다.
 */
class LobbyVolumeMenuTest {

    @TempDir
    Path dir;

    @AfterEach
    void 원래대로() throws Exception {
        SwingUtilities.invokeAndWait(() -> MenuSelectionManager.defaultManager().clearSelectedPath());
        Sound.setVolume(Sound.MAX_VOLUME);
        Sound.useSettings(SoundSettings.inMemory());
        Sound.setMuted(false);
    }

    @Test
    void 메뉴의_음소거_아래에_전체_음량_줄이_있고_끄다가_메뉴가_닫혀도_저장한다() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "창이 있어야 하는 테스트");
        Path file = dir.resolve("settings.properties");
        Sound.useSettings(SoundSettings.file(file));
        Sound.setVolume(70);
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        LobbyPanel lobby = new LobbyPanel(session, new SceneManager(view -> { }), (parent, room) -> false);
        JFrame frame = new JFrame();
        try {
            frame.add(lobby);
            frame.pack();
            frame.setVisible(true);
            // 메뉴 버튼 클릭음이 개발 PC 스피커로 나지 않게 (음소거 칸은 꺼진 채로 만들어 두었다)
            Sound.setMuted(true);

            SwingUtilities.invokeAndWait(() -> SwingTree.button(lobby, "메뉴").doClick());

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
            JSlider slider = SwingTree.named(volume, "volume-slider", JSlider.class);
            assertEquals(70, slider.getValue());

            // 끄는 중에 메뉴가 닫혔다 (손 뗀 것을 받지 못함)
            SwingUtilities.invokeAndWait(() -> {
                slider.setValueIsAdjusting(true);
                slider.setValue(35);
                MenuSelectionManager.defaultManager().clearSelectedPath();
            });
            SwingUtilities.invokeAndWait(() -> { });

            assertTrue(!popup.isVisible(), "메뉴가 닫히지 않았습니다");
            assertEquals(OptionalInt.of(35), SoundSettings.file(file).volume(), "메뉴가 닫혔는데 바꾼 음량을 저장하지 않았습니다");
        } finally {
            frame.dispose();
        }
    }
}

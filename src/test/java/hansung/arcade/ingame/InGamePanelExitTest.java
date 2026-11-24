package hansung.arcade.ingame;

import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.ingame.hud.HudFonts;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.render.Tileset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static java.awt.event.KeyEvent.KEY_LOCATION_STANDARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 오른쪽 아래 나가기 버튼을 누르면 게임을 멈추고 나가는 소리를 낸 뒤 밖에서 정한 일을 한다 */
@ExtendWith(RunOnEdt.class)
class InGamePanelExitTest {

    private static HudSkin skin;

    @BeforeAll
    static void setUp() {
        skin = HudSkin.load(HudFonts.load());
    }

    private static GameWorld world() {
        return new GameWorld(MapLoader.parse("fixture/exit", List.of("1....", ".....", ".....")),
                new PlayerProfile("부기", Mascot.SANGSANGBUGI));
    }

    private static void mouse(Component target, int id, int button, int x, int y) {
        int mask = button == MouseEvent.BUTTON1 ? InputEvent.BUTTON1_DOWN_MASK : InputEvent.BUTTON3_DOWN_MASK;
        int modifiers = id == MouseEvent.MOUSE_RELEASED ? 0 : mask;
        target.dispatchEvent(new MouseEvent(target, id, System.currentTimeMillis(), modifiers, x, y, 1, false, button));
    }

    private static void click(Component target, int button, int x, int y) {
        mouse(target, MouseEvent.MOUSE_PRESSED, button, x, y);
        mouse(target, MouseEvent.MOUSE_RELEASED, button, x, y);
    }

    @Test
    void 나가기_버튼을_누르면_나가는_소리를_내고_밖에서_정한_일을_한다() {
        GameWorld world = world();
        InGamePanel panel = TestPanels.panel(world, Tileset.forTheme("fixture"), skin);
        List<GameSound> sounds = new ArrayList<>();
        int[] exits = {0};
        panel.setSoundSink(sounds::add);
        panel.setOnExit(() -> exits[0]++);
        Box button = HudLayout.EXIT_BUTTON;

        click(panel, MouseEvent.BUTTON1, button.x() + button.width() / 2, button.y() + button.height() / 2);

        assertEquals(1, exits[0]);
        assertEquals(List.of(GameSound.EXIT), sounds);
    }

    @Test
    void 나가면_누르고_있던_키를_비워_게임이_더_움직이지_않는다() {
        GameWorld world = world();
        InGamePanel panel = TestPanels.panel(world, Tileset.forTheme("fixture"), skin);
        // 창 없이 포커스가 없으므로 키 리스너를 직접 부른다 (InGamePanelKeysTest와 같은 방식)
        KeyEvent down = new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED,
                KEY_LOCATION_STANDARD);
        for (KeyListener listener : panel.getKeyListeners()) {
            listener.keyPressed(down);
        }
        assertEquals(Direction.DOWN, panel.heldDirection());

        panel.exit();

        assertEquals(null, panel.heldDirection());
    }

    @Test
    void 버튼_밖을_누르거나_오른쪽_버튼으로_누르면_나가지_않는다() {
        InGamePanel panel = TestPanels.panel(world(), Tileset.forTheme("fixture"), skin);
        int[] exits = {0};
        panel.setOnExit(() -> exits[0]++);
        Box button = HudLayout.EXIT_BUTTON;

        click(panel, MouseEvent.BUTTON1, button.x() - 3, button.y() + 3);
        click(panel, MouseEvent.BUTTON1, HudLayout.FIELD.x() + 40, HudLayout.FIELD.y() + 40);
        click(panel, MouseEvent.BUTTON3, button.x() + 5, button.y() + 5);
        // 버튼 위에서 눌렀다가 밖으로 끌고 나가 떼면 누르지 않은 것이다
        mouse(panel, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, button.x() + 5, button.y() + 5);
        mouse(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, button.x() - 20, button.y() - 20);

        assertEquals(0, exits[0]);
        assertTrue(button.width() > 0);
    }
}

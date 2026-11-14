package hansung.arcade.ingame;

import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.HudFonts;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.render.Tileset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import static java.awt.event.KeyEvent.KEY_LOCATION_STANDARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 창 없이 키 리스너를 직접 불러, 키가 플레이어별 명령으로 바뀌어 틱마다 게임에 들어가는지 확인한다.
 */
@ExtendWith(RunOnEdt.class)
class InGamePanelKeysTest {

    private static HudSkin skin;

    @BeforeAll
    static void setUp() {
        skin = HudSkin.load(HudFonts.load());
    }

    @Test
    void 막_눌린_물풍선_키는_틱이_돌_때까지_남아_있다() {
        InGamePanel panel = panel(soloWorld());

        assertTrue(press(panel, KeyEvent.VK_SPACE).isConsumed());
        // 프레임 사이 시간이 한 틱보다 짧아 틱이 돌지 않았다
        panel.advance(InGamePanel.FIXED_STEP / 2);

        assertTrue(myCommand(panel).placeBalloon());
    }

    @Test
    void 한_프레임에_틱이_여러_번_돌면_막_눌린_키는_첫_틱이_쓰고_없어진다() {
        GameWorld world = soloWorld();
        InGamePanel panel = panel(world);

        press(panel, KeyEvent.VK_SPACE);
        panel.advance(3.5 * InGamePanel.FIXED_STEP);

        assertFalse(myCommand(panel).placeBalloon());
        assertEquals(3 * InGamePanel.FIXED_STEP, world.elapsedSeconds(), 1e-9);
    }

    @Test
    void 혼자_할_때는_방향키로_움직이고_WASD는_받지_않는다() {
        GameWorld world = soloWorld();
        InGamePanel panel = panel(world);
        double startX = world.localPlayer().x();

        assertFalse(press(panel, KeyEvent.VK_D).isConsumed());
        panel.advance(10.5 * InGamePanel.FIXED_STEP);
        assertEquals(startX, world.localPlayer().x());

        press(panel, KeyEvent.VK_RIGHT);
        panel.advance(10.5 * InGamePanel.FIXED_STEP);
        assertTrue(world.localPlayer().x() > startX);
    }

    @Test
    void 입력칸이_열려_있으면_Space와_Shift는_글자로_가고_물풍선이나_아이템이_되지_않는다() {
        InGamePanel panel = panel(soloWorld());
        press(panel, KeyEvent.VK_ENTER);
        Component chat = panel.chatField();

        assertFalse(press(chat, KeyEvent.VK_SPACE).isConsumed());
        assertFalse(press(chat, KeyEvent.VK_SHIFT, KeyEvent.KEY_LOCATION_LEFT, KeyEvent.SHIFT_DOWN_MASK).isConsumed());

        assertEquals(PlayerCommand.IDLE, myCommand(panel));
    }

    /** 빈 바닥뿐인 15 x 13칸 맵의 가운데에 나 혼자 선다 */
    private static GameWorld soloWorld() {
        return new GameWorld(MapLoader.parse("fixture/keys", openRows(7, 6, '1')), new PlayerProfile("tester", Mascot.KKUKKU));
    }

    /** 빈 바닥뿐인 15 x 13칸 맵. row번째 줄 col번째 칸에 spawn을 적는다 */
    private static List<String> openRows(int col, int row, char spawn) {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            rows.add(i == row ? ".".repeat(col) + spawn + ".".repeat(14 - col) : ".".repeat(15));
        }
        return rows;
    }

    private static InGamePanel panel(GameWorld world) {
        InGamePanel panel = new InGamePanel(world, Tileset.forTheme("fixture"), skin);
        panel.setSize(HudLayout.WIDTH, HudLayout.HEIGHT);
        return panel;
    }

    private static PlayerCommand myCommand(InGamePanel panel) {
        return panel.input().takeCommands().get(1);
    }

    private static KeyEvent press(Component target, int keyCode) {
        return press(target, keyCode, KEY_LOCATION_STANDARD, 0);
    }

    private static KeyEvent press(Component target, int keyCode, int location, int modifiersEx) {
        KeyEvent event = new KeyEvent(target, KeyEvent.KEY_PRESSED, 0, modifiersEx, keyCode, KeyEvent.CHAR_UNDEFINED, location);
        for (KeyListener listener : target.getKeyListeners()) {
            listener.keyPressed(event);
        }
        return event;
    }
}

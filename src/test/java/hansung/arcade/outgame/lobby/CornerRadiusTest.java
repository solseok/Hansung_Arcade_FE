package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.hud.Box;
import hansung.arcade.ingame.hud.Corners;
import hansung.arcade.ingame.hud.PixelCanvas;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.ui.DotPanel;
import hansung.arcade.outgame.ui.LobbyLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 6. 모서리 둥글기: 맵 미리보기 4, 그것을 감싸는 방 칸 5, 방 칸들을 감싸는 방 목록 6.
 * 모서리는 인게임 HUD와 같은 도트 방식({@link PixelCanvas})으로 깎는다.
 */
@ExtendWith(RunOnEdt.class)
class CornerRadiusTest {

    private LobbyPanel lobby;

    @BeforeEach
    void setUp() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        SceneManager scenes = new SceneManager(screen -> { });
        lobby = new LobbyPanel(session, scenes);
        scenes.register("lobby", lobby);
        scenes.show("lobby", null);
        lobby.setSize(800, 600);
        SwingTree.layout(lobby);
    }

    @Test
    void 반지름_값() {
        assertEquals(4, LobbyLayout.MAP_PREVIEW_RADIUS);
        assertEquals(5, LobbyLayout.ROOM_RADIUS);
        assertEquals(6, LobbyLayout.ROOM_LIST_RADIUS);
    }

    @Test
    void 방_목록은_6() {
        DotPanel list = SwingTree.named(lobby, "room-list", DotPanel.class);

        assertEquals(6, list.radius());
        assertCorners(list, 6);
    }

    @Test
    void 방_칸과_스켈레톤은_5() {
        checkCards();
        AbstractButton next = SwingTree.button(lobby, "다음쪽");
        while (next.isEnabled()) {
            next.doClick();
        }
        SwingTree.layout(lobby);
        assertFalse(SwingTree.namedStartingWith(lobby, "room-skeleton-").isEmpty());
        checkCards();
    }

    @Test
    void 맵_미리보기는_4() {
        for (MapPreview preview : SwingTree.all(lobby, MapPreview.class)) {
            assertEquals(4, preview.radius());
            assertCorners(preview, 4);
        }
    }

    private void checkCards() {
        for (RoomCard card : SwingTree.all(lobby, RoomCard.class)) {
            assertEquals(5, card.radius(), card.getName());
            assertCorners(card, 5);
        }
    }

    /**
     * 네 모서리 (radius+1) x (radius+1) 칸의 알파가 같은 크기·반지름으로 {@link PixelCanvas}에 칠한 것과 같은지.
     * 다르면 반지름이 다르거나 매끈한 안티앨리어싱으로 깎은 것이다.
     */
    private static void assertCorners(JComponent component, int radius) {
        BufferedImage actual = SwingTree.render(component);
        int w = actual.getWidth();
        int h = actual.getHeight();
        PixelCanvas expected = new PixelCanvas(w, h);
        expected.fill(new Box(0, 0, w, h), Corners.all(radius), Color.BLACK);
        int size = radius + 1;
        int[][] origins = {{0, 0}, {w - size, 0}, {0, h - size}, {w - size, h - size}};
        for (int[] origin : origins) {
            for (int y = origin[1]; y < origin[1] + size; y++) {
                for (int x = origin[0]; x < origin[0] + size; x++) {
                    assertEquals(SwingTree.alpha(expected.image(), x, y), SwingTree.alpha(actual, x, y),
                            component.getName() + " (" + x + "," + y + ") 모서리 알파가 반지름 " + radius + "과 다릅니다.");
                }
            }
        }
    }
}

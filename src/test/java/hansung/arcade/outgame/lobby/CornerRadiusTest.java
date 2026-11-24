package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.ui.LobbyLayout;
import hansung.arcade.outgame.ui.RoundPanel;
import hansung.arcade.outgame.ui.Shapes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 6. 모서리 둥글기: 맵 미리보기 4, 그것을 감싸는 방 칸 5, 방 칸들을 감싸는 방 목록 6.
 * 모서리는 안티앨리어싱으로 매끈하게 깎는다 (반지름 r이면 Java2D 둥근 사각형의 모서리 지름 2r).
 */
@ExtendWith(RunOnEdt.class)
class CornerRadiusTest {

    private LobbyPanel lobby;

    @BeforeEach
    void setUp() {
        OutgameSession session = new OutgameSession(new MockLobbyService(SampleRooms.rooms()));
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
        RoundPanel list = SwingTree.named(lobby, "room-list", RoundPanel.class);

        assertEquals(6, list.radius());
        // 아래쪽은 쪽 넘기기 자리만 튀어나와 있어서 위쪽 두 모서리만 본다
        assertCorners(list, 6, true);
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
            assertCorners(preview, 4, false);
        }
    }

    private void checkCards() {
        for (RoomCard card : SwingTree.all(lobby, RoomCard.class)) {
            assertEquals(5, card.radius(), card.getName());
            assertCorners(card, 5, false);
        }
    }

    /**
     * 모서리 (radius+1) x (radius+1) 칸의 알파가 같은 크기·반지름의 안티앨리어싱 둥근 사각형과 같은지.
     * 다르면 반지름이 다르거나 매끈하게 깎지 않은 것이다.
     */
    private static void assertCorners(JComponent component, int radius, boolean topOnly) {
        BufferedImage actual = SwingTree.render(component);
        int w = actual.getWidth();
        int h = actual.getHeight();
        BufferedImage expected = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = expected.createGraphics();
        Shapes.hints(g);
        g.setColor(Color.BLACK);
        g.fill(new RoundRectangle2D.Double(0, 0, w, h, 2.0 * radius, 2.0 * radius));
        g.dispose();
        int size = radius + 1;
        int[][] origins = topOnly
                ? new int[][]{{0, 0}, {w - size, 0}}
                : new int[][]{{0, 0}, {w - size, 0}, {0, h - size}, {w - size, h - size}};
        for (int[] origin : origins) {
            for (int y = origin[1]; y < origin[1] + size; y++) {
                for (int x = origin[0]; x < origin[0] + size; x++) {
                    assertEquals(SwingTree.alpha(expected, x, y), SwingTree.alpha(actual, x, y),
                            component.getName() + " (" + x + "," + y + ") 모서리 알파가 반지름 " + radius + "과 다릅니다.");
                }
            }
        }
    }
}

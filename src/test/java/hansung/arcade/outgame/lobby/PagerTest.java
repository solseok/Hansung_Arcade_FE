package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.ui.ArrowButton;
import hansung.arcade.outgame.ui.OutgamePalette;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 13. 이전쪽·다음쪽은 참고 화면(6번)처럼 ◁ ▷ 삼각형 버튼이고, 방 목록 아래쪽은 이 버튼을 담는 가운데 자리만
 * 튀어나오고 나머지는 들어가 있다.
 */
@ExtendWith(RunOnEdt.class)
class PagerTest {

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
    void 이전쪽은_왼쪽_다음쪽은_오른쪽_삼각형() {
        ArrowButton prev = SwingTree.named(lobby, "page-prev", ArrowButton.class);
        ArrowButton next = SwingTree.named(lobby, "page-next", ArrowButton.class);

        assertEquals(ArrowButton.Direction.LEFT, prev.direction());
        assertEquals(ArrowButton.Direction.RIGHT, next.direction());
        // 화면에는 쓰지 않지만 보조 기술과 테스트가 찾는 이름
        assertEquals("이전쪽", prev.getText());
        assertEquals("다음쪽", next.getText());
    }

    @Test
    void 글자_없이_삼각형만_그려서_두_버튼이_좌우_대칭이다() {
        ArrowButton prev = SwingTree.named(lobby, "page-prev", ArrowButton.class);
        ArrowButton next = SwingTree.named(lobby, "page-next", ArrowButton.class);
        prev.setEnabled(true);
        next.setEnabled(true);
        next.setSize(prev.getSize());

        BufferedImage left = SwingTree.render(prev);
        BufferedImage right = SwingTree.render(next);

        int w = left.getWidth();
        int differing = 0;
        for (int y = 0; y < left.getHeight(); y++) {
            for (int x = 0; x < w; x++) {
                if (!close(left.getRGB(x, y), right.getRGB(w - 1 - x, y))) {
                    differing++;
                }
            }
        }
        assertTrue(differing < w * left.getHeight() / 50, "좌우 대칭이 아닙니다(글자를 쓰고 있거나 화살표 모양이 다릅니다): " + differing);
    }

    @Test
    void 쪽_번호_글자는_없다() {
        // 방 인원(1/8)과 헷갈리지 않게, 쪽 번호처럼 빗금 양옆에 띄어쓰기가 있는 글자("1 / 3")를 찾는다
        assertTrue(SwingTree.texts(lobby).stream().noneMatch(text -> text.matches("\\s*\\d+ / \\d+\\s*")),
                SwingTree.texts(lobby).toString());
    }

    @Test
    void 버튼_자리만_목록_아래로_튀어나온다() {
        RoomListPanel list = SwingTree.named(lobby, "room-list", RoomListPanel.class);
        Rectangle box = SwingTree.boundsIn(list, lobby);
        Rectangle tab = list.tabBounds();
        BufferedImage screen = SwingTree.render(lobby);

        assertEquals(list.getHeight(), tab.y + tab.height, "튀어나온 자리가 목록 맨 아래까지 와야 합니다.");
        assertTrue(tab.y + tab.height > list.mainHeight() + 10, "버튼 자리가 목록 아래로 튀어나와야 합니다.");
        int y = box.y + (list.mainHeight() + tab.y + tab.height) / 2;
        assertEquals(OutgamePalette.ROOM_LIST, rgb(screen, box.x + tab.x + 3, y), "튀어나온 자리");
        assertEquals(OutgamePalette.LOBBY_BACKGROUND, rgb(screen, box.x + 20, y), "들어간 자리");
        for (String name : new String[]{"page-prev", "page-next"}) {
            Rectangle button = SwingTree.boundsIn(SwingTree.named(lobby, name, ArrowButton.class), list);
            assertTrue(tab.contains(button), name + "이(가) 튀어나온 자리 밖에 있습니다: " + button + " / " + tab);
        }
    }

    private static boolean close(int a, int b) {
        for (int shift = 0; shift <= 24; shift += 8) {
            if (Math.abs(((a >>> shift) & 0xFF) - ((b >>> shift) & 0xFF)) > 8) {
                return false;
            }
        }
        return true;
    }

    private static Color rgb(BufferedImage image, int x, int y) {
        return new Color(image.getRGB(x, y) & 0xFFFFFF);
    }
}

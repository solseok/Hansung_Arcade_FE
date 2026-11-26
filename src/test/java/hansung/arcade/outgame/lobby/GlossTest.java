package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 7. 참고 화면(6번)의 광택: 방 칸은 위쪽 안테두리가 밝게 빛나고, 방번호 상자·입장 표시 버튼·쪽 넘기기 버튼은
 * 위쪽 절반에 밝은 광택이 돈다.
 */
@ExtendWith(RunOnEdt.class)
class GlossTest {

    private LobbyPanel lobby;
    private RoomCard card;

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
        card = SwingTree.named(lobby, "room-card-1", RoomCard.class);
    }

    @Test
    void 방_칸은_위쪽_안테두리가_밝게_빛난다() {
        BufferedImage image = SwingTree.render(card);
        // 맵 미리보기(왼쪽)와 제목줄 사이 빈 칸
        int x = 88;

        assertBrighter(image, x, 2, x, image.getHeight() / 2, "방 칸");
    }

    @Test
    void 방번호_상자는_위쪽에_광택이_돈다() {
        assertGloss(SwingTree.named(card, "room-number", JComponent.class), "방번호 상자");
    }

    @Test
    void 입장_표시_버튼은_위쪽에_광택이_돈다() {
        assertGloss(SwingTree.named(card, "room-entry", JComponent.class), "입장 표시");
    }

    @Test
    void 쪽_넘기기_버튼은_위쪽에_광택이_돈다() {
        assertGloss(SwingTree.named(lobby, "page-next", JComponent.class), "다음쪽");
        assertGloss(SwingTree.named(lobby, "page-prev", JComponent.class), "이전쪽");
    }

    /** 왼쪽 가장자리 근처(글자·그림이 없는 곳)에서 위쪽이 아래쪽보다 밝은지 */
    private static void assertGloss(JComponent component, String what) {
        BufferedImage image = SwingTree.render(component);
        int x = 3;
        assertBrighter(image, x, 3, x, image.getHeight() - 4, what);
    }

    private static void assertBrighter(BufferedImage image, int x1, int y1, int x2, int y2, String what) {
        double top = luminance(image.getRGB(x1, y1));
        double bottom = luminance(image.getRGB(x2, y2));
        assertTrue(top > bottom + 20, what + " 위쪽 광택이 없습니다: 위 " + top + ", 아래 " + bottom);
    }

    private static double luminance(int argb) {
        Color c = new Color(argb & 0xFFFFFF);
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }
}

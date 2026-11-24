package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.ui.OutgameFonts;
import hansung.arcade.outgame.ui.Shapes;
import hansung.arcade.outgame.ui.TextLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 6. 로비 컴포넌트를 도트(비움·반투명·채움 세 단계)가 아니라 안티앨리어싱으로 매끈하게 그린다.
 * 픽셀 그림인 캐릭터는 그대로 1배로 그린다.
 */
@ExtendWith(RunOnEdt.class)
class AntialiasingTest {

    @Test
    void 그리기_설정은_안티앨리어싱을_켠다() {
        Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        Shapes.hints(g);

        assertEquals(RenderingHints.VALUE_ANTIALIAS_ON, g.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
        assertEquals(RenderingHints.VALUE_TEXT_ANTIALIAS_ON, g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
        g.dispose();
    }

    @Test
    void 방_칸의_둥근_모서리는_여러_단계로_매끈하다() {
        RoomCard card = RoomCard.of(new Room(1, "매끈한 방", 1, 8, "캠프 08", Room.Status.WAITING));
        card.setSize(367, 90);

        Set<Integer> alphas = cornerAlphas(SwingTree.render(card), 6);

        assertTrue(alphas.size() > 4, "모서리 알파가 도트처럼 몇 단계뿐입니다: " + alphas);
        assertTrue(alphas.stream().anyMatch(a -> a != 0 && a != 127 && a != 128 && a != 255), alphas.toString());
    }

    @Test
    void 고정_문구도_매끈하게_그린다() {
        TextLabel label = new TextLabel("WAITING");
        label.setFont(OutgameFonts.label(16));
        label.setForeground(Color.ORANGE);
        label.setOutline(Color.BLACK, 1);
        label.setSize(label.getPreferredSize());

        BufferedImage image = SwingTree.render(label);

        boolean partial = false;
        for (int y = 0; y < image.getHeight() && !partial; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = SwingTree.alpha(image, x, y);
                if (alpha > 0 && alpha < 255) {
                    partial = true;
                    break;
                }
            }
        }
        assertTrue(partial, "글자 가장자리에 중간 알파가 없습니다.");
    }

    @Test
    void 대기실_자리의_캐릭터는_그림_그대로_1배() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        SceneManager scenes = new SceneManager(screen -> { });
        WaitingRoomPanel waiting = new WaitingRoomPanel(session, scenes);
        scenes.register("waiting", waiting);
        scenes.show("waiting", session.create("1배", 8));

        JLabel sprite = SwingTree.named(SwingTree.named(waiting, "slot-0", PlayerSlot.class), "slot-sprite", JLabel.class);
        BufferedImage shown = (BufferedImage) ((ImageIcon) sprite.getIcon()).getImage();

        assertEquals(CharacterSprites.FRAME_SIZE, shown.getWidth());
        assertEquals(CharacterSprites.FRAME_SIZE, shown.getHeight());
    }

    private static Set<Integer> cornerAlphas(BufferedImage image, int size) {
        Set<Integer> alphas = new TreeSet<>();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                alphas.add(SwingTree.alpha(image, x, y));
            }
        }
        return alphas;
    }
}

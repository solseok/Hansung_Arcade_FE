package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.outgame.LoginPanel;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.ui.Dot;
import hansung.arcade.outgame.ui.DotLabel;
import hansung.arcade.outgame.ui.OutgameFonts;
import hansung.arcade.outgame.ui.OutgamePalette;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 9. 인게임 HUD에 들인 도트 렌더링을 아웃게임(로그인·로비·대기실)에도 쓴다.
 * <ul>
 *   <li>글자는 안티앨리어싱 없이 찍고 흑백으로 바꿔 중간색이 없다 ({@code OutlineText}와 같은 방식)</li>
 *   <li>둥근 모서리는 {@code PixelCanvas}처럼 비움·반투명 한 단계·채움 세 단계뿐이다</li>
 *   <li>그림을 키우고 줄일 때 가장 가까운 픽셀을 써서 원본에 없는 색이 생기지 않는다</li>
 * </ul>
 */
@ExtendWith(RunOnEdt.class)
class DotRenderingTest {

    private static final Set<Integer> DOT_ALPHAS = Set.of(0, 127, 128, 255);
    private static CharacterSprites sprites;

    @BeforeAll
    static void loadSprites() {
        sprites = CharacterSprites.load();
    }

    @Test
    void 그림을_키우거나_줄여도_원본에_없는_색이_생기지_않는다() {
        BufferedImage source = sprites.frame(Mascot.KKUKKU, Direction.DOWN, false, 0);
        Set<Integer> palette = colors(source);

        for (double scale : List.of(0.7, 1.5, 2.0)) {
            BufferedImage scaled = Dot.scale(source, (int) (source.getWidth() * scale), (int) (source.getHeight() * scale));
            assertEquals((int) (source.getWidth() * scale), scaled.getWidth());
            assertTrue(palette.containsAll(colors(scaled)), "x" + scale + " 에서 원본에 없는 색이 생겼습니다.");
        }
    }

    @Test
    void 글자에는_글자색과_투명만_있다() {
        Color ink = new Color(10, 20, 30);
        DotLabel label = new DotLabel("1/8 캠프 08 Ab");
        label.setFont(OutgameFonts.gulimChe(12));
        label.setForeground(ink);

        BufferedImage image = SwingTree.render(label);

        boolean drawn = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                assertTrue(alpha == 0 || alpha == 255, "(" + x + "," + y + ") 반투명 글자 픽셀 " + alpha);
                if (alpha == 255) {
                    assertEquals(ink.getRGB(), argb, "(" + x + "," + y + ") 글자색이 아닌 중간색");
                    drawn = true;
                }
            }
        }
        assertTrue(drawn, "글자가 그려지지 않았습니다.");
    }

    @Test
    void 배경을_깐_라벨의_둥근_모서리는_세_단계() {
        DotLabel chip = new DotLabel("1/8");
        chip.setFont(OutgameFonts.gulimChe(12));
        chip.setFill(OutgamePalette.PLAYER_COUNT, 3);
        chip.setSize(56, 22);

        assertAlphas(SwingTree.render(chip), "인원 라벨");
    }

    @Test
    void 로비_방_칸은_모서리가_세_단계이고_맵_그림은_원본_색만_쓴다() {
        LobbyPanel lobby = lobby();
        RoomCard card = SwingTree.named(lobby, "room-card-1", RoomCard.class);
        MapPreview preview = SwingTree.named(card, "map-preview", MapPreview.class);

        assertAlphas(SwingTree.render(card), card.getName());
        assertNotNull(preview.thumbnail());
        assertTrue(colors(LobbyArtwork.map("캠프 08")).containsAll(colors(preview.thumbnail())),
                "맵 미리보기를 줄이면서 원본에 없는 색이 생겼습니다.");
    }

    @Test
    void 대기실_자리의_캐릭터도_원본_색만_쓴다() {
        MockLobbyService service = new MockLobbyService();
        OutgameSession session = new OutgameSession(service);
        session.login("부기");
        SceneManager scenes = new SceneManager(screen -> { });
        WaitingRoomPanel waiting = new WaitingRoomPanel(session, scenes);
        scenes.register("waiting", waiting);
        scenes.show("waiting", session.create("도트", 8));

        JLabel sprite = SwingTree.named(SwingTree.named(waiting, "slot-0", PlayerSlot.class), "slot-sprite", JLabel.class);
        BufferedImage shown = (BufferedImage) ((ImageIcon) sprite.getIcon()).getImage();
        BufferedImage source = sprites.frame(session.character(), Direction.DOWN, false, 0);

        // 70x70 그림의 2배는 자리 그림 칸(95x95)에 들어가지 않으므로, 도트가 고르게 보이는 정수 배(1배)로 그린다
        assertEquals(0, shown.getWidth() % source.getWidth(), "정수 배가 아닙니다: " + shown.getWidth());
        assertEquals(shown.getWidth() / source.getWidth(), shown.getHeight() / source.getHeight());
        assertTrue(colors(source).containsAll(colors(shown)), "캐릭터를 그리면서 원본에 없는 색이 생겼습니다.");
    }

    @Test
    void 입력_칸과_채팅_글자도_안티앨리어싱을_끈다() {
        MockLobbyService service = new MockLobbyService();
        OutgameSession session = new OutgameSession(service);
        LoginPanel login = new LoginPanel(session, new SceneManager(screen -> { }));
        session.login("부기");
        WaitingRoomPanel waiting = new WaitingRoomPanel(session, new SceneManager(screen -> { }));

        List<JTextComponent> fields = new java.util.ArrayList<>(SwingTree.all(login, JTextField.class));
        fields.addAll(SwingTree.all(waiting, JTextComponent.class));
        assertFalse(fields.isEmpty());
        for (JTextComponent field : fields) {
            assertEquals(RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
                    field.getClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING),
                    field.getClass().getSimpleName() + " " + field.getName());
        }
    }

    private static LobbyPanel lobby() {
        OutgameSession session = new OutgameSession(new MockLobbyService());
        session.login("부기");
        SceneManager scenes = new SceneManager(screen -> { });
        LobbyPanel lobby = new LobbyPanel(session, scenes);
        scenes.register("lobby", lobby);
        scenes.show("lobby", null);
        lobby.setSize(800, 600);
        SwingTree.layout(lobby);
        return lobby;
    }

    private static void assertAlphas(BufferedImage image, String what) {
        Set<Integer> alphas = new TreeSet<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                alphas.add(image.getRGB(x, y) >>> 24);
            }
        }
        assertTrue(DOT_ALPHAS.containsAll(alphas), what + "의 알파 단계가 도트 세 단계를 넘습니다: " + alphas);
    }

    private static Set<Integer> colors(BufferedImage image) {
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                // 완전히 투명한 픽셀은 색이 의미가 없으므로 하나로 친다
                colors.add((argb >>> 24) == 0 ? 0 : argb);
            }
        }
        return colors;
    }
}

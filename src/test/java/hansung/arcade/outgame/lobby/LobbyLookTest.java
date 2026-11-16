package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.ui.RoundPanel;
import hansung.arcade.outgame.ui.OutgameFonts;
import hansung.arcade.outgame.ui.OutgamePalette;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;

import static hansung.arcade.outgame.P3.assertP3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 4. 로비 색·글꼴·인원 표시·맵 이름 자리.
 * <ul>
 *   <li>배경 60,128,235 / 방 목록 섹션 88,192,239 / 방 제목 헤더 63,142,206 / 방 한 칸 103,219,251</li>
 *   <li>인원(1/8)은 배경 82,168,233을 깐 라벨에 쓰고, 빈 칸(스켈레톤)에도 같은 라벨이 있다</li>
 *   <li>맵 미리보기는 78,167,248, 아래 띠는 69,147,227이고 맵 이름은 그 띠에 쓴다 (제목 아래가 아니다)</li>
 *   <li>글꼴은 굴림체</li>
 * </ul>
 */
@ExtendWith(RunOnEdt.class)
class LobbyLookTest {

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

    /** 스크린샷(Display P3)에서 잰 값으로 정한 색. 토큰은 sRGB라 Display P3로 바꿔 비교한다 */
    @Test
    void 로비_색_토큰() {
        assertP3(46, 107, 192, OutgamePalette.LOBBY_BACKGROUND);
        assertP3(88, 192, 242, OutgamePalette.ROOM_LIST);
        assertEquals(new Color(63, 142, 206), OutgamePalette.ROOM_HEADER);
        assertP3(103, 219, 251, OutgamePalette.ROOM_ITEM);
        assertEquals(new Color(82, 168, 233), OutgamePalette.PLAYER_COUNT);
        assertP3(78, 167, 248, OutgamePalette.MAP_PREVIEW);
        assertP3(69, 147, 227, OutgamePalette.MAP_NAME_STRIP);
    }

    @Test
    void 배경_목록_방칸_헤더가_토큰_색으로_칠해진다() {
        BufferedImage screen = SwingTree.render(lobby);
        RoundPanel list = SwingTree.named(lobby, "room-list", RoundPanel.class);
        RoomCard card = SwingTree.named(lobby, "room-card-1", RoomCard.class);
        JLabel header = SwingTree.named(card, "room-header", JLabel.class);
        Rectangle listBox = SwingTree.boundsIn(list, lobby);
        Rectangle cardBox = SwingTree.boundsIn(card, lobby);
        Rectangle headerBox = SwingTree.boundsIn(header, lobby);

        assertEquals(OutgamePalette.LOBBY_BACKGROUND, rgb(screen, 2, listBox.y + listBox.height / 2));
        assertEquals(OutgamePalette.ROOM_LIST, rgb(screen, listBox.x + 4, listBox.y + listBox.height / 2));
        assertEquals(OutgamePalette.ROOM_ITEM, rgb(screen, cardBox.x + 2, cardBox.y + cardBox.height / 2));
        assertEquals(OutgamePalette.ROOM_HEADER, rgb(screen, headerBox.x + 2, headerBox.y + headerBox.height / 2));
        assertTrue(header.getText().contains("함께 한 판 해요"), header.getText());
    }

    @Test
    void 인원수는_배경을_깐_라벨에_쓴다() {
        BufferedImage screen = SwingTree.render(lobby);
        RoomCard card = SwingTree.named(lobby, "room-card-1", RoomCard.class);
        JLabel count = SwingTree.named(card, "player-count", JLabel.class);
        Rectangle box = SwingTree.boundsIn(count, lobby);

        assertEquals("1/8", count.getText());
        assertEquals(OutgamePalette.PLAYER_COUNT, count.getBackground());
        assertEquals(OutgamePalette.PLAYER_COUNT, rgb(screen, box.x + 1, box.y + box.height / 2));
        assertTrue(SwingTree.boundsIn(card, lobby).contains(box));
    }

    @Test
    void 빈_칸_스켈레톤에도_인원_라벨과_맵_미리보기_띠가_있다() {
        lastPage();
        BufferedImage screen = SwingTree.render(lobby);
        List<Component> skeletons = SwingTree.namedStartingWith(lobby, "room-skeleton-");
        assertFalse(skeletons.isEmpty(), "마지막 쪽에 빈 칸이 없습니다.");

        for (Component component : skeletons) {
            RoomCard skeleton = (RoomCard) component;
            assertTrue(skeleton.isSkeleton());
            JLabel count = SwingTree.named(skeleton, "player-count", JLabel.class);
            Rectangle countBox = SwingTree.boundsIn(count, lobby);
            assertTrue(countBox.width > 0 && countBox.height > 0, "스켈레톤의 인원 라벨 크기가 0입니다.");
            assertEquals(OutgamePalette.PLAYER_COUNT, rgb(screen, countBox.x + 1, countBox.y + countBox.height / 2));

            MapPreview preview = SwingTree.named(skeleton, "map-preview", MapPreview.class);
            Rectangle p = SwingTree.boundsIn(preview, lobby);
            assertEquals(OutgamePalette.MAP_PREVIEW, rgb(screen, p.x + p.width / 2, p.y + 3));
            assertEquals(OutgamePalette.MAP_NAME_STRIP, rgb(screen, p.x + p.width / 2, p.y + p.height - 3));
            assertEquals(OutgamePalette.ROOM_ITEM, rgb(screen, SwingTree.boundsIn(skeleton, lobby).x + 2,
                    p.y + p.height / 2));
        }
    }

    @Test
    void 맵_이름은_미리보기_아래_띠에_쓴다() {
        RoomCard card = SwingTree.named(lobby, "room-card-1", RoomCard.class);
        MapPreview preview = SwingTree.named(card, "map-preview", MapPreview.class);
        JLabel mapName = SwingTree.named(preview, "map-name", JLabel.class);

        assertEquals("캠프 08", mapName.getText());
        int stripTop = preview.getHeight() - preview.stripHeight();
        assertTrue(mapName.getY() >= stripTop && mapName.getY() + mapName.getHeight() <= preview.getHeight(),
                "맵 이름이 띠 밖에 있습니다: " + mapName.getBounds() + ", 띠 시작 " + stripTop);
        // 참고 그림(114x110)에서 띠는 미리보기 높이의 약 22%
        double ratio = preview.stripHeight() / (double) preview.getHeight();
        assertTrue(ratio >= 0.18 && ratio <= 0.28, "띠 높이 비율 " + ratio);
        // 제목 아래 따로 맵 이름 줄을 두지 않는다
        for (JLabel label : SwingTree.all(card, JLabel.class)) {
            if (label != mapName && label.getText() != null) {
                assertFalse(label.getText().contains("캠프 08"), "맵 이름이 미리보기 밖에도 있습니다: " + label.getName());
            }
        }
    }

    @Test
    void 굴림체가_있으면_굴림체를_쓴다() {
        assumeTrue(OutgameFonts.available(), "이 PC에는 굴림체(gulim.ttc)가 없습니다.");

        assertEquals("GulimChe", OutgameFonts.gulimChe(12).getFamily(Locale.ENGLISH));
        assertEquals(13f, OutgameFonts.gulimChe(13).getSize2D());
    }

    private void lastPage() {
        AbstractButton next = SwingTree.button(lobby, "다음쪽");
        while (next.isEnabled()) {
            next.doClick();
        }
        SwingTree.layout(lobby);
    }

    private static Color rgb(BufferedImage image, int x, int y) {
        return new Color(image.getRGB(x, y) & 0xFFFFFF);
    }
}

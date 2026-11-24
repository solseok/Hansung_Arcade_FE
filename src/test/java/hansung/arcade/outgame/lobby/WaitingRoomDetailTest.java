package hansung.arcade.outgame.lobby;

import hansung.arcade.core.game.SceneManager;
import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.outgame.Dialogs;
import hansung.arcade.outgame.GameMaps;
import hansung.arcade.outgame.OutgameSession;
import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.Team;
import hansung.arcade.outgame.ui.StarRating;
import hansung.arcade.outgame.ui.WaitingRoomLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * 대기실에서 기존 테스트가 묶지 않던 모양과 동작.
 * 테스트만 보고 만든 최소 구현(캐릭터 이름·테마 하드코딩, 별·버튼 글자·캐릭터 그림·팀 색을 그리지 않음,
 * 처음 고른 표시·채팅 안내·공지·제목 글자가 없음, 기본 맵선택이 늘 취소)을 잡는다.
 */
@ExtendWith(RunOnEdt.class)
class WaitingRoomDetailTest {

    private static final Map<Mascot, String> NAMES = Map.of(
            Mascot.SANGSANGBUGI, "상상부기",
            Mascot.KKUKKU, "꾸꾸",
            Mascot.KKOKKO, "꼬꼬",
            Mascot.SANGZZI, "상찌",
            Mascot.HANSUNGNYANGI, "한성냥이");

    private OutgameSession session;
    private SceneManager scenes;
    private WaitingRoomPanel waiting;
    private final AtomicReference<String> pick = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        session = new OutgameSession(new MockLobbyService(SampleRooms.rooms()));
        session.login("부기");
        scenes = new SceneManager(screen -> { });
        waiting = new WaitingRoomPanel(session, scenes, (parent, current) -> Optional.ofNullable(pick.get()));
        scenes.register("waiting", waiting);
        scenes.register("lobby", JLabel::new);
    }

    private void enterAsHost() {
        scenes.show("waiting", session.create("8인 테스트", 8));
        layout();
    }

    private void layout() {
        waiting.setSize(WaitingRoomLayout.WIDTH, WaitingRoomLayout.HEIGHT);
        SwingTree.layout(waiting);
    }

    @Test
    void 캐릭터_다섯의_이름을_모두_보여_준다() {
        enterAsHost();
        for (Mascot mascot : Mascot.values()) {
            button("character-" + mascot.id()).doClick();
            assertEquals(NAMES.get(mascot), SwingTree.named(waiting, "character-name", JLabel.class).getText(),
                    mascot + " 이름");
        }
    }

    @Test
    void 맵_테마는_맵마다_다르다() {
        enterAsHost();
        Map<String, String> themes = Map.of(
                "camp/camp08", "테  마: 캠프",
                "patrit/patrit14", "테  마: 패트릿",
                "village/village10", "테  마: 빌리지",
                GameMaps.RANDOM_ID, "테  마: ??");
        for (var entry : themes.entrySet()) {
            pick.set(entry.getKey());
            button("map-select").doClick();
            assertEquals(entry.getValue(), SwingTree.named(waiting, "map-info-2", JLabel.class).getText(), entry.getKey());
        }
    }

    @Test
    void 인기도는_별_다섯_개를_그린다() {
        enterAsHost();
        StarRating rating = SwingTree.all(waiting, StarRating.class).getFirst();
        BufferedImage image = SwingTree.render(rating);

        // "인기도:" 글자 오른쪽에서 별 색 픽셀을 세로로 모아 가로로 끊어지는 덩어리 수를 센다
        int textEnd = rating.getInsets().left + rating.getFontMetrics(rating.getFont()).stringWidth(rating.getText());
        boolean[] columns = new boolean[image.getWidth()];
        for (int x = textEnd + 1; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int argb = image.getRGB(x, y);
                Color c = new Color(argb & 0xFFFFFF);
                if ((argb >>> 24) > 200 && c.getBlue() > 220 && c.getGreen() > 190 && c.getRed() < 180) {
                    columns[x] = true;
                }
            }
        }
        // 별 꼭짓점 세로줄은 테두리 색이라 한 별이 1~2px 끊길 수 있다. 그만큼의 틈은 한 별로 본다
        int runs = 0;
        int lastStar = -100;
        for (int x = 0; x < columns.length; x++) {
            if (columns[x]) {
                if (x - lastStar > 3) {
                    runs++;
                }
                lastStar = x;
            }
        }
        assertEquals(5, runs, "별이 다섯 개로 그려지지 않았습니다.");
    }

    @Test
    void 준비_게임시작_버튼에_글자를_그린다() {
        enterAsHost();
        AbstractButton ready = button("ready");
        String text = ready.getText();
        BufferedImage with = SwingTree.render(ready);
        ready.setText("");
        BufferedImage without = SwingTree.render(ready);
        ready.setText(text);

        assertTrue(differences(with, without) >= 40, "'" + text + "' 글자가 버튼에 그려지지 않았습니다.");
    }

    @Test
    void 캐릭터_칸에는_캐릭터_그림_랜덤_칸에는_물음표_잠긴_칸은_다르게_그린다() {
        enterAsHost();
        // 칸마다 그 캐릭터 그림을 그리므로 캐릭터끼리 칸 그림이 서로 다르다 (빈 버튼이면 모두 같다)
        List<BufferedImage> cells = new java.util.ArrayList<>();
        for (Mascot mascot : Mascot.values()) {
            cells.add(SwingTree.render(button("character-" + mascot.id())));
        }
        for (int i = 0; i < cells.size(); i++) {
            for (int j = i + 1; j < cells.size(); j++) {
                assertTrue(differences(cells.get(i), cells.get(j)) >= 100,
                        Mascot.values()[i] + "와 " + Mascot.values()[j] + " 칸이 거의 같게 그려집니다.");
            }
        }

        BufferedImage random = SwingTree.render(button("character-random"));
        JComponent lockedCell = (JComponent) SwingTree.namedStartingWith(waiting, "character-locked-").getFirst();
        BufferedImage locked = SwingTree.render(lockedCell);
        assertTrue(differences(random, locked) >= 50, "랜덤 칸과 잠긴 칸이 똑같이 그려집니다.");
    }

    @Test
    void 팀_칸은_그_팀_색으로_칠한다() {
        enterAsHost();
        for (Team team : Team.values()) {
            BufferedImage image = SwingTree.render(button("team-" + team.name()));
            Color center = new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2) & 0xFFFFFF);
            // 칸은 위가 30% 밝은 팀 색 → 아래가 팀 색인 그러데이션이라 가운데는 15% 밝다
            Color expected = lighter(team.color(), 0.15);
            assertTrue(near(center, expected, 45), team + " 칸 색이 " + center + "입니다 (팀 색 " + team.color() + ")");
        }
    }

    @Test
    void 처음_들어가면_지금_캐릭터와_팀이_골라져_있다() {
        session.selectTeam(Team.values()[3]);
        enterAsHost();

        assertTrue(button("character-" + session.character().id()).isSelected(), "지금 캐릭터 칸이 골라져 있지 않습니다.");
        assertTrue(button("team-" + session.team().name()).isSelected(), "지금 팀 칸이 골라져 있지 않습니다.");

        session.selectRandomCharacter();
        session.leave();
        enterAsHost();
        assertTrue(button("character-random").isSelected(), "랜덤을 고른 채 들어오면 물음표 칸이 골라져 있어야 합니다.");
    }

    @Test
    void 들어오면_목업_안내_없이_비어_있고_나갔다_오면_이전_채팅이_지워진다() {
        enterAsHost();
        JTextComponent log = SwingTree.named(waiting, "chat-log", JTextComponent.class);
        assertFalse(log.getText().contains("목업"), "목업 안내가 남아 있습니다: " + log.getText());

        JTextField input = SwingTree.named(waiting, "chat-input", JTextField.class);
        input.setText("이전 방 채팅");
        input.postActionEvent();
        assertTrue(log.getText().contains("이전 방 채팅"));

        button("leave").doClick();
        enterAsHost();
        assertFalse(log.getText().contains("이전 방 채팅"), "다른 방에 들어왔는데 이전 채팅이 남아 있습니다.");
        assertFalse(log.getText().contains("목업"));
    }

    @Test
    void 공지_채팅_탭_팀선택_맵_제목이_있다() {
        enterAsHost();
        JLabel notice = SwingTree.named(waiting, "notice", JLabel.class);
        assertFalse(notice.getText().isBlank(), "공지 줄이 비어 있습니다.");

        List<String> texts = SwingTree.texts(waiting);
        for (String title : new String[]{"전체 채팅", "팀선택", "맵"}) {
            assertTrue(texts.contains(title), "'" + title + "' 제목이 없습니다: " + texts);
        }
        JLabel chatTab = SwingTree.all(waiting, JLabel.class).stream()
                .filter(label -> "전체 채팅".equals(label.getText())).findFirst().orElseThrow();
        Rectangle tab = SwingTree.boundsIn(chatTab, waiting);
        Rectangle chat = new Rectangle(WaitingRoomLayout.CHAT.x(), WaitingRoomLayout.CHAT.y(),
                WaitingRoomLayout.CHAT.width(), WaitingRoomLayout.CHAT.height());
        assertTrue(chat.contains(tab) && tab.y <= chat.y + 4, "채팅 탭은 채팅 칸 맨 위에 있어야 합니다: " + tab);
    }

    @Test
    void 기본_맵선택은_창에서_고른다() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "창이 있어야 하는 테스트");
        WaitingRoomPanel withDialog = new WaitingRoomPanel(session, new SceneManager(screen -> { }));
        withDialog.enter(session.create("맵 고르기", 8));
        withDialog.setSize(WaitingRoomLayout.WIDTH, WaitingRoomLayout.HEIGHT);
        SwingTree.layout(withDialog);

        List<String> asked = Dialogs.answer(pane -> {
            pane.setInputValue("패트릿 14");
            pane.setValue(JOptionPane.OK_OPTION);
        });
        SwingTree.named(withDialog, "map-select", AbstractButton.class).doClick();

        assertFalse(asked.isEmpty(), "맵선택 창이 뜨지 않았습니다.");
        assertEquals("patrit/patrit14", session.mapId());
    }

    @Test
    void 자동_준비를_켜_두면_다른_방에_새로_들어가도_준비한_채로_들어간다() {
        scenes.show("waiting", session.join(1));
        layout();
        button("auto-ready").doClick();
        assertTrue(session.ready(), "자동 준비를 켜면 바로 준비해야 합니다.");
        session.leave();

        scenes.show("waiting", session.join(2));
        layout();

        assertTrue(button("auto-ready").isSelected());
        assertTrue(session.ready(), "자동 준비가 켜져 있으면 새로 들어간 방에서도 준비해야 합니다.");
    }

    private AbstractButton button(String name) {
        return SwingTree.named(waiting, name, AbstractButton.class);
    }

    private static int differences(BufferedImage a, BufferedImage b) {
        int found = 0;
        for (int y = 0; y < Math.min(a.getHeight(), b.getHeight()); y++) {
            for (int x = 0; x < Math.min(a.getWidth(), b.getWidth()); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    found++;
                }
            }
        }
        return found;
    }

    private static Color lighter(Color color, double amount) {
        return new Color((int) Math.round(color.getRed() + (255 - color.getRed()) * amount),
                (int) Math.round(color.getGreen() + (255 - color.getGreen()) * amount),
                (int) Math.round(color.getBlue() + (255 - color.getBlue()) * amount));
    }

    private static boolean near(Color a, Color b, int tolerance) {
        return Math.abs(a.getRed() - b.getRed()) <= tolerance
                && Math.abs(a.getGreen() - b.getGreen()) <= tolerance
                && Math.abs(a.getBlue() - b.getBlue()) <= tolerance;
    }
}

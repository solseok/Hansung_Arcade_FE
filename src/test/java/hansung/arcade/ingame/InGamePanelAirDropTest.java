package hansung.arcade.ingame;

import hansung.arcade.core.ui.RunOnEdt;
import hansung.arcade.ingame.airplane.Drop;
import hansung.arcade.ingame.airplane.FallingItem;
import hansung.arcade.ingame.airplane.Flight;
import hansung.arcade.ingame.airplane.FlightPlan;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.hud.HudFonts;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.ingame.hud.HudSkin;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.render.AirplaneSprites;
import hansung.arcade.ingame.render.BalloonSprites;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.ItemSprites;
import hansung.arcade.ingame.render.Tileset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static hansung.arcade.ingame.hud.HudLayout.FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 비행기와 비행기에서 떨어지는 아이템을 맵 위에 그리는지. 같은 시간만큼 진행한 두 게임을 그려,
 * 비행기를 띄운 쪽과 띄우지 않은 쪽을 비교한다. 맵은 15 x 13칸이라 맵 왼쪽 위가 맵 영역 왼쪽 위다.
 * fixture 테마의 집(H) 그림은 빨간 단색이고 칸보다 20px 높다.
 */
@ExtendWith(RunOnEdt.class)
class InGamePanelAirDropTest {

    private static final double TICK = 1.0 / 60;
    /** fixture 테마 집 그림의 색 */
    private static final int HOUSE = 0xC82828;

    private static HudSkin skin;
    private static CharacterSprites characters;
    private static BalloonSprites balloons;
    private static ItemSprites items;
    private static AirplaneSprites airplanes;

    @BeforeAll
    static void setUp() {
        skin = HudSkin.load(HudFonts.load());
        characters = CharacterSprites.load();
        balloons = BalloonSprites.load();
        items = ItemSprites.load();
        airplanes = AirplaneSprites.load();
    }

    /** 맨 윗줄이 집으로 가득 찬 맵. 1P는 맨 아랫줄 오른쪽 끝에 선다 */
    private static GameWorld world() {
        List<String> rows = new ArrayList<>();
        rows.add("H".repeat(15));
        for (int i = 1; i < 12; i++) {
            rows.add(".".repeat(15));
        }
        rows.add(".".repeat(14) + "1");
        return new GameWorld(MapLoader.parse("fixture/sky", rows), new PlayerProfile("tester", Mascot.KKUKKU));
    }

    private static void run(GameWorld world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.update(TICK, Map.of());
        }
    }

    private static BufferedImage paint(GameWorld world) {
        InGamePanel panel = new InGamePanel(world, Tileset.forTheme("fixture"), skin, characters, balloons, items,
                airplanes);
        panel.setSize(HudLayout.WIDTH, HudLayout.HEIGHT);
        // 언제 그려도 입력칸의 커서가 같은 그림이 나오게 깜빡임 시계를 멈춘다
        panel.chatField().setBlinkClock(() -> 0L);
        BufferedImage screen = new BufferedImage(HudLayout.WIDTH, HudLayout.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screen.createGraphics();
        panel.paint(g);
        g.dispose();
        return screen;
    }

    /** 비행기가 있는 게임과 없는 게임에서 달라진 픽셀이 모두 맵 영역 안이고, 하나 이상 있는지 */
    private static void assertChangedOnlyInsideField(BufferedImage with, BufferedImage without) {
        int changed = 0;
        for (int y = 0; y < HudLayout.HEIGHT; y++) {
            for (int x = 0; x < HudLayout.WIDTH; x++) {
                if (with.getRGB(x, y) != without.getRGB(x, y)) {
                    assertTrue(FIELD.contains(x, y), "맵 영역 밖 (%d, %d)".formatted(x, y));
                    changed++;
                }
            }
        }
        assertTrue(changed > 0);
    }

    @Test
    void 비행기는_맨_윗줄_집_위에_겹쳐_그린다() {
        GameWorld world = world();
        world.airDrop().launch(new FlightPlan(Direction.RIGHT, List.of()));
        Flight flight = world.airDrop().flights().get(0);
        while (flight.x() < 300) {
            run(world, 1);
        }

        BufferedImage screen = paint(world);

        // 집 그림이 덮는 맨 윗줄(맵 y 0~39)의 y 22에서, 떨어뜨리는 자리와 같은 열에 집 대신 비행기 몸체가 보인다
        int mapY = 22;
        int x = FIELD.x() + (int) Math.round(flight.x());
        int y = FIELD.y() + mapY;
        BufferedImage frame = airplanes.frame(Direction.RIGHT, flight.age());
        int expected = frame.getRGB(AirplaneSprites.CARGO_X, mapY - (Flight.CRUISE_Y - AirplaneSprites.CARGO_Y));
        assertEquals(0xFF, expected >>> 24, "비행기 그림의 불투명한 곳을 본다");
        assertEquals(HOUSE, paint(world()).getRGB(x, y) & 0xFFFFFF, "비행기가 없으면 집이 보이는 자리다");
        assertEquals(expected & 0xFFFFFF, screen.getRGB(x, y) & 0xFFFFFF);
    }

    @Test
    void 들어오고_나가는_비행기는_맵_테두리_밖으로_삐져나오지_않는다() {
        GameWorld withPlane = world();
        GameWorld withoutPlane = world();
        withPlane.airDrop().launch(new FlightPlan(Direction.RIGHT, List.of()));
        withPlane.airDrop().launch(new FlightPlan(Direction.LEFT, List.of()));

        // 0.3초 뒤: 두 비행기 모두 기수만 맵 안으로 들어와 그림 대부분이 맵 왼쪽·오른쪽 바깥에 있다
        run(withPlane, 18);
        run(withoutPlane, 18);
        for (Flight flight : withPlane.airDrop().flights()) {
            int left = (int) Math.round(flight.x()) - AirplaneSprites.cargoX(flight.heading());
            assertTrue(left < 0 || left + AirplaneSprites.FRAME_WIDTH > FIELD.width(), "맵 끝에 걸쳐 있다");
        }

        assertChangedOnlyInsideField(paint(withPlane), paint(withoutPlane));
    }

    @Test
    void 떨어지는_아이템은_떨어질_칸_위_공중에_그린다() {
        GameWorld withDrop = world();
        GameWorld withoutDrop = world();
        withDrop.airDrop().launch(new FlightPlan(Direction.RIGHT, List.of(new Drop(7, 10, ItemType.BUBBLE))));
        int ticks = 0;
        while (withDrop.airDrop().fallingItems().isEmpty()) {
            run(withDrop, 1);
            ticks++;
        }
        run(withDrop, 10);
        run(withoutDrop, ticks + 10);
        FallingItem falling = withDrop.airDrop().fallingItems().get(0);
        int height = (int) Math.round(falling.height());
        assertTrue(height > 100, "아직 한참 떠 있다: " + height);

        BufferedImage screen = paint(withDrop);

        // 칸에 놓였을 때(칸 위쪽 끝에서 3px 위)보다 높이만큼 위, 가로는 칸 가운데
        int left = FIELD.x() + 7 * 40 + 1;
        int top = FIELD.y() + 10 * 40 + 1 - 4 - height;
        BufferedImage image = items.image(ItemType.BUBBLE);
        int center = ItemType.IMAGE_SIZE / 2;
        assertEquals(image.getRGB(center, center) & 0xFFFFFF, screen.getRGB(left + center, top + center) & 0xFFFFFF);
        assertChangedOnlyInsideField(screen, paint(withoutDrop));
    }
}

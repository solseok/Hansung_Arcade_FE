package hansung.arcade.ingame;

import hansung.arcade.core.ui.RunOnEdt;
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
 * 비행기는 지나가는 줄의 위쪽 끝(줄 × 40)에 배가 오게 그린다.
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

    /** row 줄을 오른쪽으로 지나는 비행기를 띄워 떨어뜨리는 자리가 맵 x 300을 넘을 때까지 날린다. 그림이 좌우로 다 맵 안에 있다 */
    private static Flight flyEastAlong(GameWorld world, int row) {
        world.airDrop().launch(new FlightPlan(Direction.RIGHT, row, List.of()));
        Flight flight = world.airDrop().flights().get(0);
        while (flight.x() < 300) {
            run(world, 1);
        }
        return flight;
    }

    /**
     * 비행기 그림의 불투명한 픽셀이 모두 화면에서 그림 왼쪽 위를 맵의 (left, top)에 둔 자리에 그대로 보이는지.
     * 하나라도 잘리거나 다른 것에 가렸으면 실패한다.
     */
    private static void assertWholePlaneAt(BufferedImage screen, BufferedImage frame, int left, int top) {
        int opaque = 0;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                int argb = frame.getRGB(x, y);
                if (argb >>> 24 != 0xFF) {
                    continue;
                }
                opaque++;
                assertEquals(argb & 0xFFFFFF, screen.getRGB(FIELD.x() + left + x, FIELD.y() + top + y) & 0xFFFFFF,
                        "그림 (%d, %d)".formatted(x, y));
            }
        }
        assertTrue(opaque > 1000, "불투명한 픽셀 " + opaque);
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
    void 비행기는_지나가는_줄의_위쪽_끝에_배가_오게_그린다() {
        GameWorld world = world();
        Flight flight = flyEastAlong(world, 9);
        assertEquals(360, flight.y());

        BufferedImage screen = paint(world);

        // 그림 위쪽 끝은 배(그림 위쪽 끝에서 44px 아래)가 9번 줄 위쪽 끝(360)에 오는 316
        int left = (int) Math.round(flight.x()) - AirplaneSprites.CARGO_X;
        BufferedImage frame = airplanes.frame(Direction.RIGHT, flight.age());
        assertWholePlaneAt(screen, frame, left, 360 - AirplaneSprites.CARGO_Y);
        int belly = frame.getRGB(AirplaneSprites.CARGO_X, AirplaneSprites.CARGO_Y);
        assertEquals(belly & 0xFFFFFF,
                screen.getRGB(FIELD.x() + (int) Math.round(flight.x()), FIELD.y() + 360) & 0xFFFFFF);
    }

    @Test
    void 맨_윗줄을_지나는_비행기는_잘리지_않게_그림_위쪽_끝을_맵_맨_위에_붙여_집_위에_그린다() {
        GameWorld world = world();
        Flight flight = flyEastAlong(world, 0);
        assertEquals(0, flight.y());

        BufferedImage screen = paint(world);

        // 배를 y 0에 맞추면 그림 위쪽 44px이 맵 밖이라, 그림 위쪽 끝을 맵 맨 위(y 0)에 붙여 통째로 보인다
        int left = (int) Math.round(flight.x()) - AirplaneSprites.CARGO_X;
        BufferedImage frame = airplanes.frame(Direction.RIGHT, flight.age());
        assertWholePlaneAt(screen, frame, left, 0);

        // 집 그림이 덮는 맨 윗줄(맵 y 0~39)의 y 22에서, 떨어뜨리는 자리와 같은 열에 집 대신 비행기 몸체가 보인다
        int x = FIELD.x() + (int) Math.round(flight.x());
        int y = FIELD.y() + 22;
        int expected = frame.getRGB(AirplaneSprites.CARGO_X, 22);
        assertEquals(0xFF, expected >>> 24, "비행기 그림의 불투명한 곳을 본다");
        assertEquals(HOUSE, paint(world()).getRGB(x, y) & 0xFFFFFF, "비행기가 없으면 집이 보이는 자리다");
        assertEquals(expected & 0xFFFFFF, screen.getRGB(x, y) & 0xFFFFFF);
    }

    @Test
    void 들어오고_나가는_비행기는_맵_테두리_밖으로_삐져나오지_않는다() {
        GameWorld withPlane = world();
        GameWorld withoutPlane = world();
        // 맨 윗줄과 맨 아랫줄
        withPlane.airDrop().launch(new FlightPlan(Direction.RIGHT, 0, List.of()));
        withPlane.airDrop().launch(new FlightPlan(Direction.LEFT, 12, List.of()));

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
        // 비행기가 떨어뜨린 아이템은 떨어지는 동안 비행기 몸체 밑에 가려지므로, 비행기 없이 서버가 떨어뜨린 것처럼 넣어 본다
        withDrop.airDrop().dropFromServer(7, 10, ItemType.BUBBLE, () -> { });
        run(withDrop, 3);
        run(withoutDrop, 3);
        FallingItem falling = withDrop.airDrop().fallingItems().get(0);
        int height = (int) Math.round(falling.height());
        // 비행기 배 높이 20px에서 0.05초 동안 3px 내려왔다
        assertEquals(17, height);

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

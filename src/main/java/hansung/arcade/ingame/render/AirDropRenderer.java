package hansung.arcade.ingame.render;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.airplane.AirDrop;
import hansung.arcade.ingame.airplane.Drop;
import hansung.arcade.ingame.airplane.FallingItem;
import hansung.arcade.ingame.airplane.Flight;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.map.TileMap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Objects;

/**
 * 맵 위를 나는 비행기({@link AirplaneSprites})와 비행기에서 떨어지는 아이템({@link ItemSprites}).
 * 블럭·캐릭터 등 맵의 모든 물체를 그린 뒤에 그 위에 그린다. 호출 전에 Graphics2D의 원점이 맵 왼쪽 위로 옮겨져 있어야 한다.
 *
 * <p>떨어지는 아이템은 떨어질 칸 바닥에 그림자를 깔고, 칸에 놓였을 때와 같은 가로 자리에서 높이만큼 위에 그린다.
 * 그림자는 아이템이 내려올수록 커지고 진해진다. 아이템은 비행기보다 먼저 그려서 비행기 배 밑에서 빠져나오는 것처럼 보인다.
 * 공중에 있는 동안은 덤불 칸 위라도 보이고, 닿아서 맵에 놓이면 덤불에 숨는다.</p>
 */
public final class AirDropRenderer {

    private static final int T = TileMap.TILE_SIZE;
    /** 다 내려왔을 때 그림자. 바닥에 놓인 아이템의 그림자와 같다 */
    private static final int SHADOW_ALPHA = 55;
    private static final int SHADOW_WIDTH = T - 16;
    private static final int SHADOW_HEIGHT = 7;
    /** 떨어지기 시작할 때 그림자 크기·진하기의 비율 */
    private static final double SHADOW_START_SCALE = 0.4;

    private final AirplaneSprites airplanes;
    private final ItemSprites items;

    public AirDropRenderer(AirplaneSprites airplanes, ItemSprites items) {
        this.airplanes = Objects.requireNonNull(airplanes);
        this.items = Objects.requireNonNull(items);
    }

    /** 떨어지는 아이템, 그 위에 비행기 */
    public void draw(Graphics2D g, GameWorld world) {
        AirDrop airDrop = world.airDrop();
        for (FallingItem item : airDrop.fallingItems()) {
            drawFalling(g, item);
        }
        for (Flight flight : airDrop.flights()) {
            g.drawImage(airplanes.frame(flight.heading(), flight.age()),
                    (int) Math.round(flight.x()) - AirplaneSprites.cargoX(flight.heading()),
                    Flight.CRUISE_Y - AirplaneSprites.CARGO_Y, null);
        }
    }

    private void drawFalling(Graphics2D g, FallingItem item) {
        Drop drop = item.drop();
        double height = item.height();
        double scale = SHADOW_START_SCALE + (1 - SHADOW_START_SCALE) * (1 - height / item.startHeight());
        int shadowWidth = (int) Math.round(SHADOW_WIDTH * scale);
        int shadowHeight = (int) Math.max(2, Math.round(SHADOW_HEIGHT * scale));
        // 바닥 아이템의 그림자(칸 바닥에서 9px 위, 높이 7)와 가운데를 맞춘다
        int shadowCenterY = drop.row() * T + T - 9 + SHADOW_HEIGHT / 2;
        g.setColor(new Color(0, 0, 0, (int) Math.round(SHADOW_ALPHA * scale)));
        g.fillOval(drop.col() * T + (T - shadowWidth) / 2, shadowCenterY - shadowHeight / 2, shadowWidth, shadowHeight);

        int left = drop.col() * T + (T - ItemType.IMAGE_SIZE) / 2;
        int restingTop = drop.row() * T + (T - ItemType.IMAGE_SIZE) / 2 - WorldRenderer.ITEM_LIFT;
        g.drawImage(items.image(drop.item()), left, restingTop - (int) Math.round(height), null);
    }
}

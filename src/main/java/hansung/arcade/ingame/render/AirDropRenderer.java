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
 * <p>비행기는 지나가는 줄을 따라 그 줄의 위쪽 끝 높이로 그린다. 맨 윗줄처럼 그림이 맵 밖으로 나갈 때는 {@link #planeTop}을 본다.</p>
 *
 * <p>떨어지는 아이템은 떨어질 칸 바닥에 그림자를 깔고, 칸에 놓였을 때와 같은 가로 자리에서 높이만큼 위에 그린다.
 * 그림자는 아이템이 내려올수록 커지고 진해진다. 아이템은 비행기보다 먼저 그린다. 배에서 칸까지 20px만 떨어지므로 떨어지는 동안은
 * 대개 비행기 몸체에 가려지고, 비행기가 지나가면 몸체 밑에서 빠져나와 칸에 놓인 것처럼 드러난다.
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
                    planeTop(flight.y()), null);
        }
    }

    /**
     * 비행기 그림 위쪽 끝의 맵 y(px). 떨어뜨리는 자리(그림 위쪽 끝에서 {@link AirplaneSprites#CARGO_Y}px 아래인 배)가
     * 지나가는 줄의 위쪽 끝 bellyY에 오게 그리되, 그림이 맵 위쪽 끝을 넘으면 넘는 만큼 내려 그린다.
     *
     * <p>맨 윗줄(배가 y 0)은 그대로 그리면 그림 76px 가운데 위쪽 44px(날개 윗면과 몸체 위쪽)이 맵 위로 나간다.
     * 비행기는 맵 테두리와 그 바깥 HUD를 덮지 않도록 맵 영역 밖을 잘라 그리므로, 그대로 두면 아랫부분만 남은 비행기가 되고,
     * 자르지 않으면 맵 위 테두리(7px)를 넘어 그 바깥까지 덮는다. 그래서 그림 위쪽 끝을 맵 맨 위(y 0)에 붙여 44px 내려 그린다.
     * 그래도 그림이 y 0~76을 덮어 맨 윗줄(y 0~40) 칸은 모두 비행기 밑이다. 아이템은 비행기 몸체 뒤에서 떨어져
     * (떨어지는 0.13초 동안 몸체 위로 윗부분이 살짝 보인다) 비행기가 지나가면 그 칸에 드러난다.
     * 줄을 고르기 전 비행기가 늘 맵 맨 위(그림 위쪽 끝 y 2)를 날며 맨 윗줄에 떨어뜨리던 모습(12px에서 떨어짐)과 거의 같다.
     * 둘째 줄은 4px 넘쳐서 4px만 내리고, 셋째 줄부터는 배가 줄 위쪽 끝에 그대로 온다.
     * 아래쪽은 넘을 일이 없다: 셋째 줄부터는 그림 아래쪽 끝이 배보다 32px 아래라 그 줄 안(줄 아래쪽 끝보다 8px 위)이고,
     * 맨 윗줄·둘째 줄은 그림이 y 76까지라 맵 안이다.</p>
     *
     * @param bellyY 떨어뜨리는 자리의 맵 y ({@link Flight#y()})
     */
    static int planeTop(double bellyY) {
        return Math.max(0, (int) Math.round(bellyY) - AirplaneSprites.CARGO_Y);
    }

    private void drawFalling(Graphics2D g, FallingItem item) {
        Drop drop = item.drop();
        double height = item.height();
        double scale = SHADOW_START_SCALE + (1 - SHADOW_START_SCALE) * (1 - height / FallingItem.START_HEIGHT);
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

package hansung.arcade.ingame.hud;

import java.awt.Graphics2D;

import static hansung.arcade.ingame.hud.HudLayout.EDGE;
import static hansung.arcade.ingame.hud.HudLayout.ITEM_INNER;
import static hansung.arcade.ingame.hud.HudLayout.ITEM_PANEL;
import static hansung.arcade.ingame.hud.HudLayout.ITEM_VIEW;

/**
 * 플레이어 목록 아래 아이템 상자. 바깥 상자 안에 상자 A가 있고, A 안 왼쪽에 회색 아이템 칸이 있다.
 * A 위쪽 띠에 "ITEM" 글씨를 A 바로 위에 붙여 쓴다.
 */
final class ItemPanel implements HudPart {

    private final TextArt label;

    ItemPanel(HudSkin skin) {
        this.label = skin.itemLabel();
    }

    @Override
    public void paintBase(PixelCanvas canvas) {
        canvas.fillOutlined(ITEM_PANEL, Corners.all(HudLayout.SIDE_RADIUS), EDGE,
                HudPalette.edgeOf(HudPalette.ITEM_PANEL), HudPalette.ITEM_PANEL);
        canvas.fillOutlined(ITEM_INNER, Corners.all(HudLayout.ITEM_INNER_RADIUS), EDGE,
                HudPalette.edgeOf(HudPalette.ITEM_INNER), HudPalette.ITEM_INNER);
        canvas.fillOutlined(ITEM_VIEW, Corners.all(HudLayout.ITEM_VIEW_RADIUS), EDGE,
                HudPalette.edgeOf(HudPalette.ITEM_VIEW), HudPalette.ITEM_VIEW);
        Graphics2D g = canvas.graphics();
        try {
            label.draw(g, "ITEM", HudLayout.ITEM_LABEL, TextArt.Align.LEFT, TextArt.VerticalAlign.BOTTOM);
        } finally {
            g.dispose();
        }
    }
}

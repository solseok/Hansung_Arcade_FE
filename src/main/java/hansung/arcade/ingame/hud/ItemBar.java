package hansung.arcade.ingame.hud;

import java.awt.Graphics2D;

import static hansung.arcade.ingame.hud.HudLayout.EDGE;
import static hansung.arcade.ingame.hud.HudLayout.ITEM_BAR;
import static hansung.arcade.ingame.hud.HudLayout.ITEM_SLOTS;

/**
 * 화면 아래 "내 아이템" 바. 왼쪽에 글씨, 오른쪽에 아이템 칸 6개가 한 줄로 있다.
 *
 * <p>칸 왼쪽 위 단축키 번호(1~6)는 어떤 숫자 키로 아이템을 쓰는지 알려 주므로, 아이템 그림에 가려지지 않게
 * 맨 위 층({@link #paintOverlay})에 그린다. 아이템 그림은 이후 {@link #paint}에서 칸마다 그리면 된다.</p>
 */
final class ItemBar implements HudPart {

    private final HudSkin skin;

    ItemBar(HudSkin skin) {
        this.skin = skin;
    }

    @Override
    public void paintBase(PixelCanvas canvas) {
        canvas.fillOutlined(ITEM_BAR, HudLayout.ITEM_BAR_CORNERS, EDGE,
                HudPalette.edgeOf(HudPalette.ITEM_BAR), HudPalette.ITEM_BAR);
        for (int i = 0; i < ITEM_SLOTS; i++) {
            canvas.fillOutlined(HudLayout.itemSlot(i), HudLayout.ITEM_SLOT_CORNERS, EDGE,
                    HudPalette.edgeOf(HudPalette.ITEM_SLOT), HudPalette.ITEM_SLOT);
        }
        Graphics2D g = canvas.graphics();
        try {
            skin.itemBarLabel().draw(g, "내 아이템", HudLayout.ITEM_BAR_LABEL, TextArt.Align.CENTER);
        } finally {
            g.dispose();
        }
    }

    @Override
    public void paintOverlay(PixelCanvas canvas) {
        for (int i = 0; i < ITEM_SLOTS; i++) {
            canvas.fill(HudLayout.keyBadge(i), HudLayout.KEY_BADGE_CORNERS, HudPalette.KEY_BADGE);
        }
        Graphics2D g = canvas.graphics();
        try {
            for (int i = 0; i < ITEM_SLOTS; i++) {
                skin.keyNumber().draw(g, String.valueOf(i + 1), HudLayout.keyBadge(i), TextArt.Align.CENTER);
            }
        } finally {
            g.dispose();
        }
    }
}

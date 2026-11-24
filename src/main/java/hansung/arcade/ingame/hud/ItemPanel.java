package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.item.ItemType;
import hansung.arcade.ingame.item.PlayerGear;
import hansung.arcade.ingame.render.ItemSprites;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import static hansung.arcade.ingame.hud.HudLayout.EDGE;
import static hansung.arcade.ingame.hud.HudLayout.ITEM_INNER;
import static hansung.arcade.ingame.hud.HudLayout.ITEM_PANEL;
import static hansung.arcade.ingame.hud.HudLayout.ITEM_VIEW;

/**
 * 플레이어 목록 아래 아이템 상자. 바깥 상자 안에 상자 A가 있고, A 안에 회색 아이템 칸이 있다.
 * A 위쪽 띠에 "ITEM" 글씨를 A 바로 위에 붙여 쓴다.
 *
 * <p>아이템 칸에는 이 컴퓨터 플레이어가 먹은 액티브 아이템(바늘·보호막·스프링 신발)을 그린다. 혼자 하면 칸이 하나({@link HudLayout#ITEM_VIEW}),
 * 한 키보드 2인 모드면 1P·2P 칸 두 개를 나란히 두고({@link HudLayout#duoItemView}) 칸 위에 "1P"·"2P"를 쓴다.
 * 칸 안 위쪽에는 그 아이템을 쓰는 키 이름을 흐리게 적고(1P·혼자는 왼쪽, 2P는 오른쪽), 아이템 그림은 칸 가운데에,
 * 같은 아이템을 몇 개 들고 있는지는 칸 오른쪽 아래 테두리에 걸쳐 "×02"처럼 적는다.</p>
 */
final class ItemPanel implements HudPart {

    private final HudSkin skin;
    private final ItemSprites items;
    /** 칸마다 적을 아이템 키 이름. 칸 수 = 이 컴퓨터에서 하는 인원 */
    private final List<String> keyNames;

    /** @param keyNames 이 컴퓨터 플레이어마다(1P, 2P 순서) 아이템 키 이름. 한 명이면 칸 하나, 두 명이면 칸 두 개 */
    ItemPanel(HudSkin skin, ItemSprites items, List<String> keyNames) {
        if (keyNames.isEmpty() || keyNames.size() > 2) {
            throw new IllegalArgumentException("아이템 칸은 1~2개만 그릴 수 있습니다: " + keyNames.size());
        }
        this.skin = skin;
        this.items = items;
        this.keyNames = List.copyOf(keyNames);
    }

    /** index번째(0이 1P) 플레이어의 아이템 칸 */
    private Box view(int index) {
        return keyNames.size() == 1 ? ITEM_VIEW : HudLayout.duoItemView(index);
    }

    @Override
    public void paintBase(PixelCanvas canvas) {
        canvas.fillOutlined(ITEM_PANEL, Corners.all(HudLayout.SIDE_RADIUS), EDGE,
                HudPalette.edgeOf(HudPalette.ITEM_PANEL), HudPalette.ITEM_PANEL);
        canvas.fillOutlined(ITEM_INNER, Corners.all(HudLayout.ITEM_INNER_RADIUS), EDGE,
                HudPalette.edgeOf(HudPalette.ITEM_INNER), HudPalette.ITEM_INNER);
        for (int i = 0; i < keyNames.size(); i++) {
            canvas.fillOutlined(view(i), Corners.all(HudLayout.ITEM_VIEW_RADIUS), EDGE,
                    HudPalette.edgeOf(HudPalette.ITEM_VIEW), HudPalette.ITEM_VIEW);
        }
        Graphics2D g = canvas.graphics();
        try {
            skin.itemLabel().draw(g, "ITEM", HudLayout.ITEM_LABEL, TextArt.Align.LEFT, TextArt.VerticalAlign.BOTTOM);
            boolean duo = keyNames.size() == 2;
            for (int i = 0; i < keyNames.size(); i++) {
                Box view = view(i);
                if (duo) {
                    skin.itemSlotLabel().draw(g, (i + 1) + "P", HudLayout.itemSlotLabel(view), TextArt.Align.CENTER,
                            TextArt.VerticalAlign.BOTTOM);
                }
                // 2P 칸은 키보드 오른쪽 키라 이름도 오른쪽에 붙인다
                TextArt.Align side = duo && i == 1 ? TextArt.Align.RIGHT : TextArt.Align.LEFT;
                skin.itemKey().draw(g, keyNames.get(i), HudLayout.itemKey(view), side, TextArt.VerticalAlign.TOP);
            }
        } finally {
            g.dispose();
        }
    }

    @Override
    public void paint(Graphics2D g, GameWorld world) {
        List<Player> players = world.localPlayers();
        for (int i = 0; i < Math.min(players.size(), keyNames.size()); i++) {
            PlayerGear gear = world.gear(players.get(i));
            Optional<ItemType> item = gear.activeItem();
            if (item.isEmpty()) {
                continue;
            }
            Box view = view(i);
            BufferedImage image = items.image(item.get());
            // 키 이름 아래에 그린다
            g.drawImage(image, view.x() + (view.width() - image.getWidth()) / 2, view.y() + HudLayout.ITEM_IMAGE_TOP, null);
            skin.itemCount().draw(g, countText(gear.activeCount()), HudLayout.itemCount(view), TextArt.Align.RIGHT);
        }
    }

    /** 개수 글씨. 두 자리로 맞춘다 (예: ×02) */
    static String countText(int count) {
        return "×%02d".formatted(count);
    }
}

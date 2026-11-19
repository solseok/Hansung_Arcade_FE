package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.input.KeyBindings;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.ItemSprites;
import hansung.arcade.ingame.render.Tileset;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 맵을 뺀 인게임 화면 전체(맵 프레임, 맵 이름표, 플레이어 목록, 아이템 상자, 나가기, 내 아이템 바, 맵 위에 올라온 채팅과 말풍선,
 * 2인 모드의 1P·2P 표시).
 *
 * <p>바뀌지 않는 부분은 만들 때 두 장의 그림(맵 아래 층, 맨 위 층)으로 한 번만 그려 두고,
 * 매 프레임에는 그 그림을 붙인 뒤 타이머·플레이어처럼 바뀌는 부분만 그린다.</p>
 *
 * <p>한 프레임의 순서: {@link #paintBelowWorld} → 맵 바닥 → {@link #paintFieldCorners} → 블록·캐릭터
 * → {@link #paintAboveWorld}.</p>
 */
public final class InGameHud {

    private final MapFrame frame = new MapFrame();
    private final List<HudPart> parts;
    private final AllyArrowView allyArrows = new AllyArrowView();
    private final BufferedImage base;
    private final BufferedImage overlay;

    /** 혼자 하는 화면. 아이템 칸 하나에 혼자 할 때의 아이템 키 이름을 적고, 아이템 그림은 기본 위치에서 새로 읽는다 */
    public InGameHud(HudSkin skin, TileMap map, Tileset tileset) {
        this(skin, map, tileset, ItemSprites.load(), List.of(KeyBindings.SOLO.itemKeyName()));
    }

    /**
     * @param items        아이템 칸에 그릴 액티브 아이템 그림
     * @param itemKeyNames 이 컴퓨터 플레이어마다(1P, 2P 순서) 아이템 키 이름. 인원만큼 아이템 칸을 그린다
     */
    public InGameHud(HudSkin skin, TileMap map, Tileset tileset, ItemSprites items, List<String> itemKeyNames) {
        this.parts = List.of(
                frame,
                new MapLabel(skin, map, tileset),
                new PlayerList(skin),
                new ItemPanel(skin, items, itemKeyNames),
                new ExitButton(skin),
                new ItemBar(skin),
                new ChatLogView(skin.chatText(), skin.myChatText()),
                new PlayerTagView(skin.chatText()),
                new SpeechBubbleView(skin.chatText()));

        PixelCanvas baseCanvas = new PixelCanvas(HudLayout.WIDTH, HudLayout.HEIGHT);
        Graphics2D g = baseCanvas.graphics();
        g.setColor(HudPalette.BACKGROUND);
        g.fillRect(0, 0, HudLayout.WIDTH, HudLayout.HEIGHT);
        g.dispose();
        parts.forEach(part -> part.paintBase(baseCanvas));
        this.base = baseCanvas.image();

        PixelCanvas overlayCanvas = new PixelCanvas(HudLayout.WIDTH, HudLayout.HEIGHT);
        parts.forEach(part -> part.paintOverlay(overlayCanvas));
        this.overlay = overlayCanvas.image();
    }

    /** 맵보다 먼저 그리는 바탕 */
    public void paintBelowWorld(Graphics2D g) {
        g.drawImage(base, 0, 0, null);
    }

    /** 맵 영역의 둥근 귀퉁이. 맵 바닥을 그린 직후, 블록·캐릭터를 그리기 전에 부른다. */
    public void paintFieldCorners(Graphics2D g) {
        frame.paintFieldCorners(g);
    }

    /** 맵을 그린 뒤 그 위에 그리는 HUD 내용 */
    public void paintAboveWorld(Graphics2D g, GameWorld world) {
        for (HudPart part : parts) {
            if (!(part instanceof SpeechBubbleView)) part.paint(g, world);
        }
        g.drawImage(overlay, 0, 0, null);
        allyArrows.paint(g, world);
        for (HudPart part : parts) {
            if (part instanceof SpeechBubbleView) part.paint(g, world);
        }
    }
}

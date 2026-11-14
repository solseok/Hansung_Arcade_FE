package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.Tileset;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 맵을 뺀 인게임 화면 전체(맵 프레임, 맵 이름표, 플레이어 목록, 아이템 상자, 나가기, 내 아이템 바, 맵 위에 올라온 채팅과 말풍선).
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
    private final BufferedImage base;
    private final BufferedImage overlay;

    public InGameHud(HudSkin skin, TileMap map, Tileset tileset) {
        this.parts = List.of(
                frame,
                new MapLabel(skin, map, tileset),
                new PlayerList(skin),
                new ItemPanel(skin),
                new ExitButton(skin),
                new ItemBar(skin),
                new ChatLogView(skin.chatText(), skin.myChatText()),
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
            part.paint(g, world);
        }
        g.drawImage(overlay, 0, 0, null);
    }
}

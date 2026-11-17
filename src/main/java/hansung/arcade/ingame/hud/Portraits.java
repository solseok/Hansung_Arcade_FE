package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.entity.CharacterColor;
import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.render.CharacterRecolor;
import hansung.arcade.ingame.render.CharacterSprites;
import hansung.arcade.ingame.render.SpriteSheet;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 플레이어 목록에 넣을 캐릭터 얼굴. 캐릭터 정면 그림의 머리 쪽을 얼굴 틀 크기로 줄인 그림이다.
 * 고정색을 고르면 정면 그림을 먼저 {@link CharacterRecolor}로 다시 칠한 뒤 줄여서, 맵 위 캐릭터와 같은 색이 된다.
 * (캐릭터, 색)마다 처음 쓸 때 한 번만 만들어 두고 그 뒤로는 만들어 둔 그림을 돌려준다.
 */
public final class Portraits {

    /** 원래 색 정면 그림. 그림 파일이 없는 캐릭터는 빠져 있다 */
    private final Map<Mascot, BufferedImage> fronts;
    private final Map<Key, BufferedImage> made = new ConcurrentHashMap<>();

    Portraits(Map<Mascot, BufferedImage> fronts) {
        this.fronts = Map.copyOf(fronts);
    }

    /** resources/sprites/characters/ 의 {id}_front.bmp 를 읽는다. 파일이 없는 캐릭터는 얼굴 없이 둔다. */
    public static Portraits load() {
        Map<Mascot, BufferedImage> fronts = new EnumMap<>(Mascot.class);
        for (Mascot mascot : Mascot.values()) {
            SpriteSheet.find(CharacterSprites.DIRECTORY + mascot.id() + "_front.bmp")
                    .map(sheet -> sheet.frames(CharacterSprites.FRAME_SIZE).get(0))
                    .ifPresent(front -> fronts.put(mascot, front));
        }
        return new Portraits(fronts);
    }

    /**
     * 얼굴 그림. 그림 파일이 없는 캐릭터면 null이다.
     *
     * @param color 고정색. null이면 원래 색이다
     */
    public BufferedImage get(Mascot mascot, CharacterColor color) {
        BufferedImage front = fronts.get(mascot);
        if (front == null) {
            return null;
        }
        return made.computeIfAbsent(new Key(mascot, color),
                key -> portraitOf(CharacterRecolor.recolor(front, mascot, color)));
    }

    /**
     * 정면 그림에서 머리 쪽을 얼굴 틀 안쪽과 같은 비율로 잘라 틀 크기로 줄인다.
     * 캐릭터 폭보다 조금 넓게 잘라서 얼굴이 틀을 꽉 채우되 양옆이 잘리지 않게 한다.
     */
    static BufferedImage portraitOf(BufferedImage front) {
        int targetWidth = HudLayout.PORTRAIT_WIDTH - 2 * HudLayout.EDGE;
        int targetHeight = HudLayout.PORTRAIT_HEIGHT - 2 * HudLayout.EDGE;
        Box body = PixelImages.opaqueBounds(front);
        int width = Math.min(front.getWidth(), Math.max(targetWidth, body.width() + 4));
        int height = Math.min(front.getHeight() - body.y(), (int) Math.round(width * targetHeight / (double) targetWidth));
        int left = Math.max(0, Math.min(front.getWidth() - width, body.x() + (body.width() - width) / 2));
        BufferedImage head = front.getSubimage(left, body.y(), width, height);
        return PixelImages.shrinkToFit(head, targetWidth, targetHeight);
    }

    private record Key(Mascot mascot, CharacterColor color) {
    }
}

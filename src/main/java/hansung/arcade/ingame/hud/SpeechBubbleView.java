package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Player;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static hansung.arcade.ingame.hud.HudLayout.BUBBLE_ABOVE_PLAYER;
import static hansung.arcade.ingame.hud.HudLayout.BUBBLE_EDGE;
import static hansung.arcade.ingame.hud.HudLayout.BUBBLE_PADDING_BOTTOM;
import static hansung.arcade.ingame.hud.HudLayout.BUBBLE_PADDING_LEFT;
import static hansung.arcade.ingame.hud.HudLayout.BUBBLE_PADDING_RIGHT;
import static hansung.arcade.ingame.hud.HudLayout.BUBBLE_PADDING_TOP;
import static hansung.arcade.ingame.hud.HudLayout.BUBBLE_RADIUS;
import static hansung.arcade.ingame.hud.HudLayout.BUBBLE_TEXT_WIDTH;
import static hansung.arcade.ingame.hud.HudLayout.CHAT_LINE_PITCH;
import static hansung.arcade.ingame.hud.HudLayout.FIELD;

/**
 * 채팅을 친 플레이어 머리 위에 뜨는 말풍선. 둥근 사각형(검은색 20% 바탕, 흰 1px 테두리) 안에 친 말을 적고,
 * 캐릭터를 따라다닌다. 말이 {@link HudLayout#BUBBLE_MAX_WIDTH}보다 길면 줄을 바꾼다.
 *
 * <p>말풍선은 캐릭터 가로 가운데에 맞추되 맵 영역 밖으로 나가지 않게 안쪽으로 민다.
 * 그래서 맨 윗줄이나 가장자리에 있는 캐릭터는 말풍선이 머리와 겹치거나 한쪽으로 치우칠 수 있다.</p>
 */
final class SpeechBubbleView implements HudPart {

    /** 플레이어 수(8)만큼만 떠 있을 수 있으므로 그 두 배면 넉넉하다 */
    private static final int CACHE_SIZE = 2 * HudLayout.PLAYER_SLOTS;

    private final OutlineText text;
    private final Map<String, BufferedImage> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    SpeechBubbleView(OutlineText text) {
        this.text = text;
    }

    @Override
    public void paint(Graphics2D g, GameWorld world) {
        int originX = HudLayout.mapOriginX(world.map());
        int originY = HudLayout.mapOriginY(world.map());
        for (Player player : world.players()) {
            String said = world.speechBubbles().textOf(player.id());
            if (said == null) {
                continue;
            }
            BufferedImage bubble = cache.computeIfAbsent(said, this::render);
            Box place = place(bubble.getWidth(), bubble.getHeight(),
                    originX + (int) Math.round(player.x()), originY + (int) Math.round(player.y()));
            g.drawImage(bubble, place.x(), place.y(), null);
        }
    }

    /** 화면 좌표 (playerX, playerY)에 있는 캐릭터의 머리 위, 폭 width · 높이 height 말풍선이 놓일 자리 */
    static Box place(int width, int height, int playerX, int playerY) {
        int x = Math.max(FIELD.x(), Math.min(FIELD.right() - width, playerX - width / 2));
        int y = Math.max(FIELD.y(), Math.min(FIELD.bottom() - height, playerY - BUBBLE_ABOVE_PLAYER - height));
        return new Box(x, y, width, height);
    }

    /** 말풍선 한 장. 폭은 가장 긴 줄에 맞추고 높이는 줄 수에 맞춘다. */
    BufferedImage render(String said) {
        List<String> lines = wrap(said);
        int textWidth = lines.stream().mapToInt(text::width).max().orElse(0);
        int width = BUBBLE_PADDING_LEFT + textWidth + BUBBLE_PADDING_RIGHT;
        int height = BUBBLE_PADDING_TOP + text.cellHeight() + (lines.size() - 1) * CHAT_LINE_PITCH
                + BUBBLE_PADDING_BOTTOM;

        PixelCanvas canvas = new PixelCanvas(width, height);
        Box box = new Box(0, 0, width, height);
        Corners corners = Corners.all(BUBBLE_RADIUS);
        // 바탕이 반투명이라 테두리색 위에 덮어 칠하지 않고, 테두리 띠와 속을 따로 칠한다
        canvas.ring(box, corners, box.inset(BUBBLE_EDGE), corners.shrink(BUBBLE_EDGE), HudPalette.BUBBLE_EDGE);
        canvas.fill(box.inset(BUBBLE_EDGE), corners.shrink(BUBBLE_EDGE), HudPalette.BUBBLE);

        Graphics2D g = canvas.graphics();
        for (int i = 0; i < lines.size(); i++) {
            text.draw(g, lines.get(i), BUBBLE_PADDING_LEFT, BUBBLE_PADDING_TOP + i * CHAT_LINE_PITCH);
        }
        g.dispose();
        return canvas.image();
    }

    /**
     * 한 줄 폭({@link HudLayout#BUBBLE_TEXT_WIDTH})을 넘기 직전에 글자 단위로 줄을 바꾼다.
     * 줄이 바뀐 자리의 공백은 버려서 다음 줄이 공백으로 시작하지 않는다.
     */
    List<String> wrap(String said) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < said.length(); ) {
            int codePoint = said.codePointAt(i);
            i += Character.charCount(codePoint);
            if (!line.isEmpty() && text.width(line + Character.toString(codePoint)) > BUBBLE_TEXT_WIDTH) {
                lines.add(line.toString().stripTrailing());
                line.setLength(0);
            }
            if (line.isEmpty() && !lines.isEmpty() && Character.isWhitespace(codePoint)) {
                continue;
            }
            line.appendCodePoint(codePoint);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }
}

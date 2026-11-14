package hansung.arcade.ingame.hud;

import java.awt.Color;

/**
 * 인게임 화면 색.
 *
 * <p>디자인에 테두리 색이 없는 상자는 나가기 버튼(#0754BB → #004AA8)처럼 같은 계열의 더 어두운 색
 * 1px 테두리를 둘렀다. 그 색은 {@link #edgeOf}로 채움색을 어둡게 만들어 쓴다.</p>
 */
public final class HudPalette {

    /** 맵 프레임을 뺀 화면 바탕 */
    public static final Color BACKGROUND = new Color(0x0B62B6);

    public static final Color FRAME_INNER = new Color(0x68616F);
    public static final Color FRAME_OUTER = new Color(0x000206);

    /** 맵 이름표·TIMER 줄의 전광판 바탕과 격자선 */
    public static final Color DISPLAY = new Color(0x083B84);
    public static final Color DISPLAY_GRID = new Color(0x06306D);
    public static final Color DISPLAY_LABEL = new Color(0x8FD4FF);
    public static final Color DISPLAY_DIGIT = new Color(0xFFFFFF);
    /** 전광판 격자 간격. 2면 1px 칸마다 1px 선이 지나간다 */
    public static final int DISPLAY_GRID_PITCH = 2;

    public static final Color PLAYER_LIST = new Color(0x027FD8);
    public static final Color PLAYER_SLOT = new Color(0x0965C8);
    /** 플레이어 칸 안 얼굴 틀: 칸보다 어두운 바탕 */
    public static final Color PORTRAIT_BOX = edgeOf(PLAYER_SLOT);
    public static final Color NICKNAME = Color.WHITE;

    public static final Color ITEM_PANEL = new Color(0x01A0F1);
    public static final Color ITEM_INNER = new Color(0x0070CA);
    public static final Color ITEM_VIEW = new Color(0x8B8B8B);
    public static final Color ITEM_LABEL = Color.WHITE;

    public static final Color EXIT = new Color(0x0754BB);
    public static final Color EXIT_EDGE = new Color(0x004AA8);
    public static final Color EXIT_BUTTON = new Color(0x0093E9);
    public static final Color EXIT_BUTTON_EDGE = new Color(0x004296);
    /** 버튼 그라데이션 위쪽 끝(밝은 띠 바로 아래): 기본색보다 어둡다 */
    public static final Color EXIT_BUTTON_DARK = new Color(0x0080D2);
    /** 버튼 그라데이션 아래쪽 끝: 기본색보다 밝다 */
    public static final Color EXIT_BUTTON_LIGHT = new Color(0x1FA3F0);
    public static final Color EXIT_BUTTON_HIGHLIGHT = new Color(0x6CC9F7);
    public static final Color EXIT_TEXT = Color.WHITE;

    public static final Color ITEM_BAR = new Color(0x0792EA);
    public static final Color ITEM_BAR_LABEL = Color.WHITE;
    public static final Color ITEM_SLOT = new Color(0x0156B0);
    /** 단축키 번호 상자는 일단 아이템 칸과 같은 색 */
    public static final Color KEY_BADGE = ITEM_SLOT;
    public static final Color KEY_DIGIT = Color.WHITE;

    /** 맵 위 채팅 글자와 그 둘레 1px 테두리 */
    public static final Color CHAT_TEXT = new Color(0xFFFFFF);
    public static final Color CHAT_OUTLINE = new Color(0x000000);
    /** 채팅 목록에서 내가 친 줄 */
    public static final Color CHAT_TEXT_MINE = new Color(0xDFDB86);
    /** 말풍선: 검은색을 20%만 덮은 바탕에 흰 테두리 */
    public static final Color BUBBLE = new Color(0, 0, 0, 51);
    public static final Color BUBBLE_EDGE = new Color(0xFFFFFF);

    /** 결과 배너 띠: 전광판 격자색을 80% 덮은 바탕에 위아래 밝은 선. 맵이 살짝 비친다 */
    public static final Color RESULT_BAND = new Color(0x06, 0x30, 0x6D, 0xCC);
    public static final Color RESULT_BAND_EDGE = DISPLAY_LABEL;
    /** 결과 글씨 그림자 (검은색 50%) */
    public static final Color RESULT_SHADOW = new Color(0, 0, 0, 0x80);
    /** 결과 글씨 윗변에 한 도트씩 넣는 밝은 줄 */
    public static final Color RESULT_HIGHLIGHT = Color.WHITE;
    /** 이겼을 때: 위에서 아래로 노랑 → 주황, 짙은 갈색 테두리 */
    public static final Color WIN_TOP = new Color(0xFFF59A);
    public static final Color WIN_BOTTOM = new Color(0xFFA514);
    public static final Color WIN_OUTLINE = new Color(0x5A2600);
    /** 졌을 때: 위에서 아래로 연하늘 → 파랑, 남색 테두리 */
    public static final Color LOSE_TOP = new Color(0xE2F0FF);
    public static final Color LOSE_BOTTOM = new Color(0x6A96DE);
    public static final Color LOSE_OUTLINE = new Color(0x13255A);
    /** 비겼을 때: 위에서 아래로 연두 → 초록, 짙은 초록 테두리 */
    public static final Color DRAW_TOP = new Color(0xEEFFE2);
    public static final Color DRAW_BOTTOM = new Color(0x5FCB82);
    public static final Color DRAW_OUTLINE = new Color(0x114728);

    private HudPalette() {
    }

    /** 테두리 색이 따로 없는 상자의 1px 테두리. 채움색을 72% 밝기로 어둡게 한다. */
    public static Color edgeOf(Color fill) {
        return new Color((int) (fill.getRed() * 0.72), (int) (fill.getGreen() * 0.72), (int) (fill.getBlue() * 0.72));
    }

    /** 전광판 바탕: 격자 간격마다 어두운 선이 지나가는 무늬. (originX, originY)에서 칸이 시작한다. */
    public static PixelCanvas.PixelPaint displayGrid(int originX, int originY) {
        int fill = DISPLAY.getRGB();
        int line = DISPLAY_GRID.getRGB();
        return (x, y) -> Math.floorMod(x - originX, DISPLAY_GRID_PITCH) == DISPLAY_GRID_PITCH - 1
                || Math.floorMod(y - originY, DISPLAY_GRID_PITCH) == DISPLAY_GRID_PITCH - 1 ? line : fill;
    }
}

package hansung.arcade.outgame.ui;

import hansung.arcade.ingame.hud.Box;

/**
 * 로비 배치 (800x600). 방 목록 안 상자는 방 목록 기준, 방 칸 안 상자는 방 칸 기준 좌표다.
 *
 * <pre>
 * [방만들기][바로시작]                         닉네임 님
 * ┌ ROOM_LIST (r6) ────────────────────────────────┐
 * │ ┌ 방 칸 367x90 (r5) ────────┐ ┌ 방 칸 ───────────┐ │  4줄 x 2칸 = 8칸
 * │ │ [미리보기 r4] [[001] 제목         ]  │ │                  │ │
 * │ │ [ 맵 이름 띠 ] WAITING    [1/8][사람] │ │                  │ │
 * │ └──────────────────────────┘ └──────────────────┘ │
 * └──────────────┐   [ ◁ ][ ▷ ]   ┌──────────────┘  ← 쪽 넘기기 자리만 튀어나온다
 *                └────────────────┘
 *                          안내 문구
 * [메뉴]                                          [나가기]
 * </pre>
 */
public final class LobbyLayout {

    private LobbyLayout() {
    }

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    /** 모서리 반지름: 맵 미리보기 4 ⊂ 방 칸 5 ⊂ 방 목록 6 */
    public static final int MAP_PREVIEW_RADIUS = 4;
    public static final int ROOM_RADIUS = 5;
    public static final int ROOM_LIST_RADIUS = 6;
    /** 방 칸 안 작은 상자(번호·인원·입장 표시·제목줄) 모서리 */
    public static final int CARD_PART_RADIUS = 4;

    public static final Box CREATE_BUTTON = new Box(18, 12, 134, 61);
    public static final Box QUICK_BUTTON = new Box(162, 12, 123, 61);
    public static final Box NICKNAME = new Box(300, 30, 482, 24);

    /** 방 목록. 아래쪽은 쪽 넘기기 자리({@link #PAGER_TAB})만 튀어나와 있다 */
    public static final Box ROOM_LIST = new Box(18, 84, 764, 436);
    /** 방 목록에서 튀어나온 자리를 뺀 본체 높이 */
    public static final int ROOM_LIST_MAIN_HEIGHT = 404;
    public static final int PAGE_SIZE = 8;
    public static final int ROOM_COLUMNS = 2;
    public static final int ROOM_WIDTH = 367;
    public static final int ROOM_HEIGHT = 90;
    private static final int LIST_PADDING = 10;
    private static final int ROOM_GAP_X = 10;
    private static final int ROOM_GAP_Y = 8;
    /** 방 목록 기준: 쪽 넘기기 버튼을 담고 아래로 튀어나온 자리 */
    public static final Box PAGER_TAB = new Box(272, 396, 220, 40);
    public static final Box PAGE_PREV = new Box(286, 404, 92, 26);
    public static final Box PAGE_NEXT = new Box(386, 404, 92, 26);

    public static final Box MESSAGE = new Box(18, 524, 764, 26);
    public static final Box FOOTER = new Box(0, 556, 800, 44);
    public static final Box MENU = new Box(14, 563, 90, 30);
    public static final Box EXIT = new Box(696, 563, 90, 30);

    // ── 방 칸 안 (방 칸 기준) ──
    public static final Box CARD_PREVIEW = new Box(6, 6, 78, 78);
    public static final Box CARD_HEADER = new Box(92, 6, 269, 26);
    /** 제목줄 기준: 방번호 상자와 제목 */
    public static final Box CARD_NUMBER = new Box(4, 3, 46, 20);
    public static final Box CARD_TITLE = new Box(54, 0, 211, 26);
    public static final Box CARD_STATUS = new Box(92, 56, 140, 28);
    public static final Box CARD_COUNT = new Box(262, 58, 62, 26);
    public static final Box CARD_ENTRY = new Box(330, 58, 31, 26);

    /** 맵 이름 띠 높이 / 미리보기 높이. 참고 그림(114x110)에서 띠가 미리보기의 약 22% */
    public static final double MAP_NAME_STRIP_RATIO = 0.22;

    /** 방 목록 기준 slot번째(0부터, 왼쪽→오른쪽, 위→아래) 방 칸 */
    public static Box room(int slot) {
        int column = slot % ROOM_COLUMNS;
        int row = slot / ROOM_COLUMNS;
        return new Box(LIST_PADDING + column * (ROOM_WIDTH + ROOM_GAP_X),
                LIST_PADDING + row * (ROOM_HEIGHT + ROOM_GAP_Y), ROOM_WIDTH, ROOM_HEIGHT);
    }

    /** 높이 previewHeight인 미리보기의 맵 이름 띠 높이 */
    public static int stripHeight(int previewHeight) {
        return (int) Math.round(previewHeight * MAP_NAME_STRIP_RATIO);
    }
}

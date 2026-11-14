package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.map.TileMap;

/**
 * 인게임 화면(800 x 600)의 모든 위치와 크기.
 *
 * <p>디자인 수치는 867 x 686 화면에서 잰 값이라 가로는 800/867(약 0.923), 세로는 600/686(약 0.875)을 곱해
 * 반올림했다. 주석의 괄호 안이 원래 수치다. 모서리 반지름처럼 가로세로 구분이 없는 값은 두 비율의
 * 기하평균(약 0.898)을 곱했고, 맵 테두리 두께(3px + 4px)는 네 변이 같은 굵기로 보이도록 그대로 두었다.</p>
 *
 * <p>맵 칸은 40px 그대로라서 15 x 13칸 맵이 600 x 520 에 딱 맞는다.</p>
 */
public final class HudLayout {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    // ── 세로 기준: 내 아이템 바가 창 바닥에 붙고, 맵 프레임 아래쪽과 4 (5) 겹친다 ──
    /** 내 아이템 바 높이 (40) */
    public static final int ITEM_BAR_HEIGHT = 35;
    /** 내 아이템 바가 맵 프레임 아래쪽과 겹치는 높이 (5). 바를 프레임보다 위에 그린다 */
    public static final int ITEM_BAR_OVERLAP = 4;

    // ── 맵 프레임과 맵 이름표 ─────────────────────────────────────
    /** 맵 영역 바로 바깥 회색 테두리 두께 (3) */
    public static final int FRAME_INNER_EDGE = 3;
    /** 가장 바깥 검은 테두리 두께 (4) */
    public static final int FRAME_OUTER_EDGE = 4;
    /** 맵 프레임. 창 왼쪽에서 14, 아래쪽은 내 아이템 바에 4만큼 덮인다 */
    public static final Box FRAME = frameAbove(HEIGHT - ITEM_BAR_HEIGHT + ITEM_BAR_OVERLAP);
    /** 캐릭터가 움직이는 맵 영역 (15 x 13칸) */
    public static final Box FIELD = FRAME.inset(FRAME_INNER_EDGE + FRAME_OUTER_EDGE);
    /** 프레임 바깥 모서리 (10) */
    public static final int FRAME_RADIUS = 9;

    /** 맵 이름표: 창 왼쪽에서 14 (15), 프레임에서 4 (5) 위. 95 x 22 (103 x 25) */
    public static final Box MAP_LABEL = new Box(14, FRAME.y() - 4 - 22, 95, 22);
    public static final int MAP_LABEL_RADIUS = 7;                                   // (8)
    /** 대표 블록 그림이 들어갈 정사각형 한 변 (14) */
    public static final int MAP_ICON_SIZE = 13;
    public static final int MAP_LABEL_PADDING = 5;

    // ── 오른쪽 열: 플레이어 목록 → 아이템 상자 → 나가기 ──────────────────────
    // 세 상자는 크기와 간격을 그대로 둔 채 한 덩어리로, 나가기 받침이 창 바닥에서 9 (10) 위에 오도록 놓는다.
    /** 프레임 검은 테두리에서 9 (10) 떨어진 오른쪽 열의 왼쪽 끝 */
    public static final int SIDE_X = FRAME.right() + 9;
    public static final int SIDE_WIDTH = 155;                                       // (168)
    /** 오른쪽 열 상자들의 모서리 (아이템 상자 8에 맞춤) */
    public static final int SIDE_RADIUS = 7;
    /** 목록·아이템 상자·나가기 사이 간격 (5). 참고 화면에서도 두 간격이 같다 */
    public static final int SIDE_GAP = 4;
    /** 나가기 받침 아래 끝과 창 바닥 사이 (10) */
    public static final int EXIT_BOTTOM_MARGIN = 9;
    public static final int PLAYER_LIST_HEIGHT = 365;                               // (417)
    public static final int ITEM_PANEL_HEIGHT = 98;                                 // (112)
    public static final int EXIT_HEIGHT = 36;                                       // (41)

    /** 플레이어 목록 155 x 365 (168 x 417). 위치는 나가기를 바닥에 맞춘 뒤 거꾸로 쌓아 정해진다 */
    public static final Box PLAYER_LIST = new Box(SIDE_X,
            HEIGHT - EXIT_BOTTOM_MARGIN - EXIT_HEIGHT - SIDE_GAP - ITEM_PANEL_HEIGHT - SIDE_GAP - PLAYER_LIST_HEIGHT,
            SIDE_WIDTH, PLAYER_LIST_HEIGHT);

    // 목록 안: TIMER 박스 → 플레이어 칸 8개가 목록 높이를 채운다.
    // 세로 수치는 참고 화면(목록 716px)을 목록 높이 365에 맞춰 옮겼다 (화면 px x 365/716).
    public static final int PLAYER_SLOTS = 8;
    /** 목록 안 왼쪽 여백 (15). TIMER 박스도 같다 */
    public static final int PLAYER_SLOT_LEFT = 14;
    public static final int PLAYER_SLOT_WIDTH = 125;                               // (135)
    /** 목록 위에서 TIMER 박스까지 (참고 화면 23px) */
    public static final int PLAYER_LIST_PADDING_TOP = 12;
    /** TIMER 박스와 첫 플레이어 칸 사이 (참고 화면 8px) */
    public static final int TIMER_SLOT_GAP = 4;
    public static final int PLAYER_SLOT_GAP = 2;                                   // (2)
    /** 칸 높이: 참고 화면 78px. TIMER 아래 남는 높이를 8칸이 채운다 (첫 지시의 34는 화면과 맞지 않아 화면을 따름) */
    public static final int PLAYER_SLOT_HEIGHT = 38;
    public static final int PLAYER_SLOT_RADIUS = 3;
    /** 칸 안 왼쪽 캐릭터 얼굴 틀: 칸 기준 왼쪽 8, 41 x 28 (참고 화면 14, 75 x 59px). 칸 안에서 세로 가운데 */
    public static final int PORTRAIT_LEFT = 8;
    public static final int PORTRAIT_WIDTH = 41;
    public static final int PORTRAIT_HEIGHT = 28;
    /** 얼굴 틀 위아래 여백. 위아래가 같도록 칸 높이에서 틀 높이를 뺀 나머지를 반씩 */
    public static final int PORTRAIT_TOP = (PLAYER_SLOT_HEIGHT - PORTRAIT_HEIGHT) / 2;
    /** 닉네임 글자 높이 (참고 화면 20px) */
    public static final int NICKNAME_HEIGHT = 10;

    /** 플레이어 칸들 맨 위의 TIMER 박스. 플레이어 칸과 같은 왼쪽 여백·폭, 높이 23 (26) */
    public static final Box TIMER = PLAYER_LIST.child(PLAYER_SLOT_LEFT, PLAYER_LIST_PADDING_TOP, PLAYER_SLOT_WIDTH, 23);
    public static final int TIMER_RADIUS = 5;
    /** TIMER 박스 안쪽 좌우 여백 (15) */
    public static final int TIMER_PADDING = 14;
    /** "TIMER" 글씨 자리 폭 (40) */
    public static final int TIMER_LABEL_WIDTH = 37;
    /** "TIMER" 글씨와 숫자 사이 (7) */
    public static final int TIMER_GAP = 6;
    public static final Box TIMER_LABEL = TIMER.inset(0, TIMER_PADDING, 0, 0)
            .child(0, 0, TIMER_LABEL_WIDTH, TIMER.height());
    /** 숫자 MM:ss 자리. 글씨 뒤 남는 자리 전부 (58) */
    public static final Box TIMER_DIGITS = new Box(TIMER_LABEL.right() + TIMER_GAP, TIMER.y(),
            TIMER.right() - TIMER_PADDING - (TIMER_LABEL.right() + TIMER_GAP), TIMER.height());

    /** 플레이어 목록 바로 아래 아이템 상자 (168 x 112) */
    public static final Box ITEM_PANEL = new Box(SIDE_X, PLAYER_LIST.bottom() + SIDE_GAP, SIDE_WIDTH, ITEM_PANEL_HEIGHT);
    /** 아이템 상자 안쪽 상자 A. 여백 위 16 (18), 좌우 8 (9), 아래 6 (7) */
    public static final Box ITEM_INNER = ITEM_PANEL.inset(16, 8, 6, 8);
    public static final int ITEM_INNER_RADIUS = 4;
    /** A 안의 회색 아이템 칸 61 x 50 (66 x 57). A 기준 위 13 (15), 왼쪽 15 (16) */
    public static final Box ITEM_VIEW = ITEM_INNER.child(15, 13, 61, 50);
    public static final int ITEM_VIEW_RADIUS = 3;
    /** ITEM 글씨 아래 끝과 상자 A 사이 (참고 화면 3.5) */
    public static final int ITEM_LABEL_GAP = 3;
    /**
     * A 위쪽 띠에 들어가는 ITEM 글씨 자리. 상자 왼쪽에서 14 (15), 글자 높이 9 (10).
     * 글씨는 이 자리 바닥(A에서 3 위)에 붙여 그린다.
     */
    public static final Box ITEM_LABEL = new Box(ITEM_PANEL.x() + 14, ITEM_PANEL.y(),
            ITEM_INNER.right() - ITEM_PANEL.x() - 14, ITEM_INNER.y() - ITEM_LABEL_GAP - ITEM_PANEL.y());
    public static final int ITEM_LABEL_HEIGHT = 9;

    /** 아이템 상자에서 4 (5) 아래 나가기 받침. 156 x 36 (169 x 41)이라 열보다 오른쪽으로 1px 나온다 */
    public static final Box EXIT = new Box(SIDE_X, ITEM_PANEL.bottom() + SIDE_GAP, 156, EXIT_HEIGHT);
    /** 받침 안 버튼 140 x 29 (152 x 33). 받침 기준 위 3 (4), 왼쪽 7 (8) */
    public static final Box EXIT_BUTTON = EXIT.child(7, 3, 140, 29);
    public static final int EXIT_BUTTON_RADIUS = 4;
    /** 버튼 맨 위 밝은 띠 두께 (4) */
    public static final int EXIT_HIGHLIGHT = 3;
    /** "나가기" 글자 높이 (20) */
    public static final int EXIT_TEXT_HEIGHT = 17;

    // ── 아래쪽 내 아이템 바 ──────────────────────────────────────
    /** 창 왼쪽에서 154 (167), 332 x 35 (360 x 40). 창 바닥에 붙는다 */
    public static final Box ITEM_BAR = new Box(154, HEIGHT - ITEM_BAR_HEIGHT, 332, ITEM_BAR_HEIGHT);
    /** 위쪽 두 모서리만 둥글다 (8). 아래는 창 바닥에 붙어 각져 있다 */
    public static final Corners ITEM_BAR_CORNERS = Corners.top(7);
    /** 바 왼쪽 "내 아이템" 글씨 자리 (96) */
    public static final Box ITEM_BAR_LABEL = ITEM_BAR.child(0, 0, 89, ITEM_BAR.height());
    public static final int ITEM_SLOTS = 6;
    public static final int ITEM_SLOT_WIDTH = 38;                                   // (41)
    public static final int ITEM_SLOT_HEIGHT = 28;                                  // (32)
    public static final int ITEM_SLOT_GAP = 2;                                      // (2.5)
    /** 아이템 칸 모서리: 오른쪽 위·왼쪽 아래 4 (5), 왼쪽 위·오른쪽 아래 3 (3) */
    public static final Corners ITEM_SLOT_CORNERS = new Corners(3, 4, 3, 4);
    /** 아이템 칸·상자의 1px 테두리 */
    public static final int EDGE = 1;
    /** 칸 왼쪽 위 단축키 번호 상자 13 x 10 (14 x 12). 칸 테두리 안쪽에 붙는다 */
    public static final int KEY_BADGE_WIDTH = 13;
    public static final int KEY_BADGE_HEIGHT = 10;
    public static final Corners KEY_BADGE_CORNERS = new Corners(ITEM_SLOT_CORNERS.topLeft() - EDGE, 0, 3, 0);

    private HudLayout() {
    }

    /** 바닥이 bottom인 맵 프레임 (15 x 13칸 + 테두리) */
    private static Box frameAbove(int bottom) {
        int edges = 2 * (FRAME_INNER_EDGE + FRAME_OUTER_EDGE);
        int height = 13 * TileMap.TILE_SIZE + edges;
        return new Box(14, bottom - height, 15 * TileMap.TILE_SIZE + edges, height);
    }

    /** index번째(0부터) 플레이어 슬롯 */
    public static Box playerSlot(int index) {
        int top = TIMER.bottom() + TIMER_SLOT_GAP + index * (PLAYER_SLOT_HEIGHT + PLAYER_SLOT_GAP);
        return new Box(PLAYER_LIST.x() + PLAYER_SLOT_LEFT, top, PLAYER_SLOT_WIDTH, PLAYER_SLOT_HEIGHT);
    }

    /** index번째 플레이어 칸 안의 캐릭터 얼굴 틀 */
    public static Box portraitBox(int index) {
        return playerSlot(index).child(PORTRAIT_LEFT, PORTRAIT_TOP, PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
    }

    /** index번째(0부터) 아이템 칸. 숫자 키 index + 1 로 쓴다 */
    public static Box itemSlot(int index) {
        int top = ITEM_BAR.y() + (ITEM_BAR.height() - ITEM_SLOT_HEIGHT) / 2;
        return new Box(ITEM_BAR_LABEL.right() + index * (ITEM_SLOT_WIDTH + ITEM_SLOT_GAP), top,
                ITEM_SLOT_WIDTH, ITEM_SLOT_HEIGHT);
    }

    /** 아이템 칸 왼쪽 위 단축키 번호 상자 */
    public static Box keyBadge(int index) {
        return itemSlot(index).inset(EDGE).child(0, 0, KEY_BADGE_WIDTH, KEY_BADGE_HEIGHT);
    }
}

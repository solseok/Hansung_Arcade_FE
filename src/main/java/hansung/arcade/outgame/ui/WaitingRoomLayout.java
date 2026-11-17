package hansung.arcade.outgame.ui;

import hansung.arcade.ingame.hud.Box;

/**
 * 대기실 배치 (800x600, 창 기준 절대 좌표). 참고 화면(1288x964)에서 잰 값을 가로 ÷1.61, 세로 ÷1.607로 옮겼다.
 * 괄호 안은 참고 화면에서 잰 값이다.
 *
 * <pre>
 * ┌ TOP_BAR(공지) ─────────────┐┌ RIGHT_PANEL ─────────────┐
 * │┌ LEFT_PANEL ─────────────┐ ││ CHARACTER_SECTION        │
 * ││ ROOM_HEADER 번호 제목 인원 │ ││  배너 + 캐릭터 4x2          │
 * ││ SLOT_GRID 4x2 자리        │ ││ 팀선택  TEAM_SECTION 8색   │
 * ││  (그림 / 닉네임 / 상태)     │ ││ 맵     MAP_SECTION        │
 * ││ CHAT (탭 / 로그 / 입력)    │ ││  미리보기 | 정보 4줄·맵선택  │
 * │└─────────────────────────┘ ││ READY_SECTION 준비·자동준비  │
 * └ BOTTOM_BAR(로비로 나가기) ───────────────────────────────┘
 * </pre>
 */
public final class WaitingRoomLayout {

    private WaitingRoomLayout() {
    }

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    /** 판(왼쪽·오른쪽) 모서리 */
    public static final int PANEL_RADIUS = 8;
    /** 칸 묶음(자리 판·팀·맵·준비) 모서리 */
    public static final int SECTION_RADIUS = 6;
    /** 자리 그림 칸 모서리 */
    public static final int SLOT_RADIUS = 5;
    /** 작은 줄·라벨 모서리 */
    public static final int ROW_RADIUS = 3;

    // ── 큰 구역 ──
    /** 위 공지 줄 (0~740 x 0~50) */
    public static final Box TOP_BAR = new Box(0, 0, 462, 34);
    /** 공지 글 칸 (45~700 x 5~45) */
    public static final Box NOTICE = new Box(28, 5, 407, 24);
    /** 왼쪽 판 (12~740 x 63~895) */
    public static final Box LEFT_PANEL = new Box(7, 39, 453, 518);
    /** 오른쪽 판 (745~1283 x 5~900) */
    public static final Box RIGHT_PANEL = new Box(464, 3, 333, 557);
    /** 아래 줄 (0~1288 x 902~964) */
    public static final Box BOTTOM_BAR = new Box(0, 561, 800, 39);
    /** 로비로 나가기 (아래 줄 왼쪽) */
    public static final Box LEAVE = new Box(14, 566, 110, 28);

    // ── 왼쪽 판 ──
    /** 방 제목줄 (34~722 x 95~140) */
    public static final Box ROOM_HEADER = new Box(21, 59, 427, 28);
    /** 방 번호 배지 (42~118 x 105~133) */
    public static final Box ROOM_NUMBER = new Box(26, 63, 48, 20);
    public static final Box ROOM_TITLE = new Box(80, 59, 276, 28);
    /** 인원 (방정보변경 버튼 자리 552~712) */
    public static final Box ROOM_COUNT = new Box(362, 63, 80, 20);
    /** 자리 판 (34~722 x 145~615) */
    public static final Box SLOT_GRID = new Box(21, 90, 427, 293);
    public static final int SLOTS = 8;
    private static final int SLOT_COLUMNS = 4;
    /** 자리 한 칸 폭 (153) */
    public static final int SLOT_WIDTH = 95;
    /** 그림 95 (153) + 4 + 닉네임 20 (32) + 1 + 상태 17 (27) */
    public static final int SLOT_HEIGHT = 137;
    public static final int SLOT_IMAGE_HEIGHT = 95;
    public static final int SLOT_NAME_TOP = 99;
    public static final int SLOT_NAME_HEIGHT = 20;
    public static final int SLOT_STATUS_TOP = 120;
    public static final int SLOT_STATUS_HEIGHT = 17;
    /** 자리 사이 간격 포함 (170, 231) */
    private static final int SLOT_PITCH_X = 105;
    private static final int SLOT_PITCH_Y = 144;
    private static final int SLOT_LEFT = 29;
    private static final int SLOT_TOP = 96;
    /** 채팅 (35~720 x 625~880) */
    public static final Box CHAT = new Box(21, 389, 427, 160);
    /** 채팅 탭 (35~375 x 625~657) */
    public static final Box CHAT_TAB = new Box(21, 389, 213, 20);
    /** 채팅 로그 (657~827) */
    public static final Box CHAT_LOG = new Box(21, 409, 427, 107);
    /** 채팅 입력 줄 (832~880) */
    public static final Box CHAT_INPUT_ROW = new Box(21, 518, 427, 31);

    // ── 오른쪽 판 ──
    /** 캐릭터 묶음 (772~1247 x 22~415) */
    public static final Box CHARACTER_SECTION = new Box(480, 14, 295, 244);
    /** 고른 캐릭터 배너 (785~1232 x 37~172) */
    public static final Box CHARACTER_BANNER = new Box(488, 23, 278, 84);
    /** 캐릭터 칸 4x2: 마스코트 5 + 랜덤 1 + 잠김 2 */
    public static final int CHARACTER_CELLS = 8;
    private static final int CHARACTER_COLUMNS = 4;
    /** 캐릭터 칸 (100 x 65, 간격 115) */
    public static final int CHARACTER_CELL_WIDTH = 62;
    public static final int CHARACTER_CELL_HEIGHT = 63;
    private static final int CHARACTER_PITCH_X = 72;
    private static final int CHARACTER_PITCH_Y = 70;
    private static final int CHARACTER_TOP = 115;
    /** "팀선택" (782~840 x 420~437) */
    public static final Box TEAM_LABEL = new Box(486, 260, 60, 12);
    /** 팀 묶음 (772~1247 x 440~522) */
    public static final Box TEAM_SECTION = new Box(480, 274, 295, 51);
    /** 팀 색 칸 (47 x 50, 간격 57.5) */
    private static final int TEAM_SWATCH_WIDTH = 29;
    private static final int TEAM_SWATCH_HEIGHT = 31;
    private static final int TEAM_PITCH = 36;
    /** "맵" (782 x 530~545) */
    public static final Box MAP_LABEL = new Box(486, 328, 60, 12);
    /** 맵 묶음 (772~1247 x 547~777) */
    public static final Box MAP_SECTION = new Box(480, 340, 295, 144);
    /** 맵 미리보기 보라 칸 (782~1022 x 557~767) */
    public static final Box MAP_PREVIEW = new Box(486, 347, 149, 130);
    /** 미리보기 안 맵 그림 (834~972 x 590~735) */
    public static final Box MAP_THUMBNAIL = new Box(511, 356, 99, 112);
    /** 맵 정보 줄: 이름·인원·테마·크기 (1032~1237, 557부터 30 간격) */
    public static final int MAP_INFO_ROWS = 4;
    /** 맵선택 버튼 (1037~1232 x 687~762) */
    public static final Box MAP_SELECT = new Box(644, 428, 121, 46);
    /** 준비 묶음 (805~1212 x 787~890) */
    public static final Box READY_SECTION = new Box(500, 490, 253, 64);
    /** 준비/게임 시작 버튼 (822~1127 x 795~880) */
    public static final Box READY_BUTTON = new Box(510, 495, 190, 53);
    /** 자동 준비 (1142~1200 x 797~877) */
    public static final Box AUTO_READY = new Box(708, 496, 38, 50);

    /** index번째(0부터, 왼쪽→오른쪽, 위→아래) 자리 */
    public static Box slot(int index) {
        return new Box(SLOT_LEFT + (index % SLOT_COLUMNS) * SLOT_PITCH_X,
                SLOT_TOP + (index / SLOT_COLUMNS) * SLOT_PITCH_Y, SLOT_WIDTH, SLOT_HEIGHT);
    }

    public static Box slotImage(int index) {
        return slot(index).child(0, 0, SLOT_WIDTH, SLOT_IMAGE_HEIGHT);
    }

    public static Box slotName(int index) {
        return slot(index).child(0, SLOT_NAME_TOP, SLOT_WIDTH, SLOT_NAME_HEIGHT);
    }

    public static Box slotStatus(int index) {
        return slot(index).child(0, SLOT_STATUS_TOP, SLOT_WIDTH, SLOT_STATUS_HEIGHT);
    }

    public static Box characterCell(int index) {
        return new Box(CHARACTER_BANNER.x() + (index % CHARACTER_COLUMNS) * CHARACTER_PITCH_X,
                CHARACTER_TOP + (index / CHARACTER_COLUMNS) * CHARACTER_PITCH_Y,
                CHARACTER_CELL_WIDTH, CHARACTER_CELL_HEIGHT);
    }

    public static Box teamSwatch(int index) {
        return new Box(CHARACTER_BANNER.x() + index * TEAM_PITCH, 284, TEAM_SWATCH_WIDTH, TEAM_SWATCH_HEIGHT);
    }

    public static Box mapInfo(int index) {
        return new Box(641, 347 + index * 19, 127, 16);
    }

    /** parent 기준 상대 좌표로 옮긴 child */
    public static Box relative(Box child, Box parent) {
        return new Box(child.x() - parent.x(), child.y() - parent.y(), child.width(), child.height());
    }
}

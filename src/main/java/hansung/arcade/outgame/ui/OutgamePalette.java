package hansung.arcade.outgame.ui;

import java.awt.Color;

/**
 * 아웃게임(로그인·로비·대기실·결과) 색 토큰.
 *
 * <p>맥 스크린샷은 Display P3 색 공간이라, Java가 sRGB로 그린 색이 스크린샷에서는 다른 값으로 재진다
 * (로비 배경 sRGB 60,128,235가 스크린샷에서 78,126,228). 그래서 사용자가 스크린샷에서 잰 색과 참고 화면 PNG(로그인·로비 그림)에서
 * 읽은 색은 {@link #p3}로 sRGB로 바꿔 넣는다. 그러면 다시 스크린샷을 찍었을 때 그 값으로 재진다.
 * 대기실 색은 sRGB인 참고 그림(webp)에서 읽어 그대로 넣었다. 괄호 안은 읽은 자리다.</p>
 */
public final class OutgamePalette {

    private OutgamePalette() {
    }

    /** Display P3(맥 스크린샷에서 잰 값) (r, g, b)를 같은 색으로 보이는 sRGB로. sRGB로 나타낼 수 없으면 가장 가까운 값으로 자른다 */
    public static Color p3(int r, int g, int b) {
        double pr = toLinear(r);
        double pg = toLinear(g);
        double pb = toLinear(b);
        return new Color(
                toByte(1.2249401 * pr - 0.2249404 * pg),
                toByte(-0.0420569 * pr + 1.0420571 * pg),
                toByte(-0.0196376 * pr - 0.0786361 * pg + 1.0982735 * pb));
    }

    /** sRGB color가 맥 스크린샷(Display P3)에서 재지는 값 [r, g, b] */
    public static int[] toDisplayP3(Color color) {
        double r = toLinear(color.getRed());
        double g = toLinear(color.getGreen());
        double b = toLinear(color.getBlue());
        return new int[]{
                toByte(0.8224621 * r + 0.1775380 * g),
                toByte(0.0331941 * r + 0.9668058 * g),
                toByte(0.0170827 * r + 0.0723974 * g + 0.9105199 * b)};
    }

    /** 0~255 감마 값을 빛의 세기(0~1)로. sRGB와 Display P3는 같은 감마 곡선을 쓴다 */
    private static double toLinear(int channel) {
        double v = channel / 255.0;
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static int toByte(double linear) {
        double v = Math.clamp(linear, 0.0, 1.0);
        double encoded = v <= 0.0031308 ? v * 12.92 : 1.055 * Math.pow(v, 1 / 2.4) - 0.055;
        return (int) Math.round(Math.clamp(encoded, 0.0, 1.0) * 255);
    }

    // ── 로비 (스크린샷·참고 화면 6번에서 잰 Display P3) ─────────────────
    /** 로비 바탕 */
    public static final Color LOBBY_BACKGROUND = p3(46, 107, 192);
    /** 바탕보다 한 단계 위인 방 목록 섹션 */
    public static final Color ROOM_LIST = p3(88, 192, 242);
    /** 글이 적히는 헤더(방 제목 줄) */
    public static final Color ROOM_HEADER = p3(67, 148, 216);
    /** 방 목록 안 방 한 칸 */
    public static final Color ROOM_ITEM = p3(103, 219, 251);
    /** 방 칸 바깥 테두리와 위·왼쪽 안쪽의 밝은 빛 */
    public static final Color ROOM_ITEM_EDGE = p3(62, 124, 187);
    public static final Color ROOM_ITEM_SHINE = p3(185, 240, 252);
    /** 방 인원(1/8) 라벨 바탕 */
    public static final Color PLAYER_COUNT = p3(82, 168, 233);
    /** 인원 글자: 연노랑에 어두운 테두리 */
    public static final Color PLAYER_COUNT_TEXT = p3(250, 244, 170);
    /** 맵 미리보기(그림이 없는 스켈레톤일 때 그대로 보인다) */
    public static final Color MAP_PREVIEW = p3(78, 167, 248);
    /** 맵 미리보기 테두리 */
    public static final Color MAP_PREVIEW_EDGE = p3(51, 114, 196);
    /** 맵 미리보기 아래 맵 이름 띠 */
    public static final Color MAP_NAME_STRIP = p3(69, 147, 227);
    /**
     * 맵 그림 위에 까는 반투명 띠(알파 128). {@link #MAP_PREVIEW} 위에 깔면 {@link #MAP_NAME_STRIP}이 되는 색이라
     * 그림 위에서도 띠 색은 비슷하고 그림이 살짝 비쳐 보인다.
     */
    public static final Color MAP_NAME_STRIP_OVERLAY = new Color(
            Math.clamp(2 * MAP_NAME_STRIP.getRed() - MAP_PREVIEW.getRed(), 0, 255),
            Math.clamp(2 * MAP_NAME_STRIP.getGreen() - MAP_PREVIEW.getGreen(), 0, 255),
            Math.clamp(2 * MAP_NAME_STRIP.getBlue() - MAP_PREVIEW.getBlue(), 0, 255), 128);
    /** 방번호 상자 (참고 화면 6번 "029" 상자): 바탕·테두리·노란 숫자·숫자 테두리 */
    public static final Color ROOM_NUMBER_BOX = p3(72, 160, 238);
    public static final Color ROOM_NUMBER_BOX_EDGE = p3(49, 111, 194);
    public static final Color ROOM_NUMBER_DIGITS = p3(244, 207, 71);
    public static final Color ROOM_NUMBER_DIGITS_OUTLINE = p3(40, 64, 91);
    /** 방 제목 글자 (검은 1px 테두리를 두른다) */
    public static final Color ROOM_TITLE_TEXT = p3(215, 208, 76);
    /** WAITING 글자: 위 연노랑 → 주황 → 아래 갈색, 어두운 갈색 테두리 */
    public static final Color WAITING_LABEL_TOP = p3(250, 240, 170);
    public static final Color WAITING_LABEL_BOTTOM = p3(220, 110, 30);
    public static final Color WAITING_LABEL_OUTLINE = p3(70, 30, 8);
    /** PLAYING 글자: 위 흰빛 → 아래 회청색 */
    public static final Color PLAYING_LABEL_TOP = p3(235, 240, 250);
    public static final Color PLAYING_LABEL_BOTTOM = p3(120, 140, 175);
    public static final Color PLAYING_LABEL_OUTLINE = p3(35, 45, 75);
    /** 입장 표시 버튼(사람 모양): 들어갈 수 있으면 파란 광택 바탕에 노란 사람, 아니면 회색 */
    public static final Color ENTRY_TOP = p3(83, 181, 245);
    public static final Color ENTRY_BOTTOM = p3(64, 137, 213);
    public static final Color ENTRY_EDGE = p3(49, 111, 193);
    public static final Color ENTRY_PERSON_TOP = p3(246, 206, 70);
    public static final Color ENTRY_PERSON_BOTTOM = p3(239, 150, 52);
    public static final Color ENTRY_CLOSED_TOP = p3(175, 177, 175);
    public static final Color ENTRY_CLOSED_BOTTOM = p3(125, 127, 125);
    public static final Color ENTRY_CLOSED_EDGE = p3(100, 102, 100);
    public static final Color ENTRY_CLOSED_PERSON = p3(210, 210, 210);
    /** 쪽 넘기기 버튼 (참고 화면 6번 아래 ◁ ▷) */
    public static final Color PAGER_TOP = p3(97, 187, 245);
    public static final Color PAGER_BOTTOM = p3(73, 151, 223);
    public static final Color PAGER_EDGE = p3(42, 91, 170);
    public static final Color PAGER_ARROW = p3(64, 139, 230);
    public static final Color PAGER_ARROW_EDGE = p3(25, 70, 160);
    /** 밝은 칸 위 글자 */
    public static final Color LOBBY_TEXT = new Color(16, 64, 128);
    /** 로비 아래 줄 */
    public static final Color LOBBY_FOOTER = p3(36, 82, 160);
    /** 잘못된 입력 안내. 주황 입력 영역 위에서도 읽히는 진한 빨강 */
    public static final Color ERROR_TEXT = new Color(140, 20, 20);
    /** 글자 테두리(검정) */
    public static final Color TEXT_OUTLINE = Color.BLACK;

    // ── 버튼 ─────────────────────────────────────────────
    public static final Color BUTTON_BLUE_TOP = new Color(120, 205, 255);
    public static final Color BUTTON_BLUE_BOTTOM = new Color(30, 110, 215);
    public static final Color BUTTON_BLUE_EDGE = new Color(18, 62, 120);
    public static final Color BUTTON_GOLD_TOP = new Color(255, 236, 140);
    public static final Color BUTTON_GOLD_BOTTOM = new Color(236, 150, 22);
    public static final Color BUTTON_GOLD_EDGE = new Color(150, 70, 0);

    // ── 로그인 (참고 화면 1번에서 잰 Display P3) ────────────────────
    public static final Color LOGIN_BACKGROUND = p3(62, 135, 235);
    /** 주황 입력 영역: 위 → 아래 그러데이션과 테두리 */
    public static final Color LOGIN_PANEL_TOP = p3(248, 215, 113);
    public static final Color LOGIN_PANEL_BOTTOM = p3(238, 127, 49);
    public static final Color LOGIN_PANEL_EDGE = p3(183, 90, 34);
    /** 노란 탭: 위 밝은 빛 → 아래 형광 노랑 그러데이션, 테두리, 빨간 글자 */
    public static final Color LOGIN_TAB_TOP = p3(251, 232, 174);
    public static final Color LOGIN_TAB_BOTTOM = p3(255, 255, 84);
    public static final Color LOGIN_TAB_EDGE = p3(205, 121, 45);
    public static final Color LOGIN_TAB_TEXT = p3(209, 86, 38);
    /** 어두운 회색 칸과 테두리 */
    public static final Color LOGIN_SECTION = p3(98, 96, 98);
    public static final Color LOGIN_SECTION_EDGE = p3(50, 48, 50);
    /** 파란 이름표 */
    public static final Color LOGIN_LABEL_BOX = p3(48, 111, 192);
    public static final Color LOGIN_LABEL_EDGE = p3(30, 70, 140);
    public static final Color LOGIN_LABEL_PLAYER = p3(251, 232, 115);
    /** 1P·2P 버튼 받침(회색)과 흰 테두리, 유리 같은 몸통 */
    public static final Color LOGIN_SOCKET = p3(118, 118, 118);
    public static final Color LOGIN_SOCKET_EDGE = p3(49, 52, 57);
    public static final Color LOGIN_RING = p3(230, 251, 254);
    public static final Color PLAYER_ONE_TOP = p3(20, 58, 190);
    public static final Color PLAYER_ONE_BOTTOM = p3(116, 223, 249);
    public static final Color PLAYER_ONE_TEXT_OUTLINE = p3(14, 38, 151);
    public static final Color PLAYER_TWO_TOP = p3(40, 118, 28);
    public static final Color PLAYER_TWO_BOTTOM = p3(155, 237, 83);
    public static final Color PLAYER_TWO_TEXT_OUTLINE = p3(30, 90, 20);

    // ── 대기실 (참고 화면에서 읽은 색) ────────────────────────────
    /** 바깥 남색 바탕 (top 바 가장자리 20,25) */
    public static final Color WAITING_FRAME = new Color(3, 66, 141);
    /** 위 공지 칸 (300,25) */
    public static final Color WAITING_NOTICE = new Color(2, 50, 118);
    /** 왼쪽 큰 판 (25,300) */
    public static final Color WAITING_LEFT_PANEL = new Color(1, 194, 250);
    /** 오른쪽 큰 판 (775,420) */
    public static final Color WAITING_RIGHT_PANEL = new Color(4, 106, 206);
    /** 칸을 묶는 남색 섹션: 자리 판·팀·맵·준비 (40,400) */
    public static final Color WAITING_SECTION = new Color(3, 70, 141);
    /** 방 제목줄 (300,118) */
    public static final Color WAITING_HEADER = new Color(2, 146, 216);
    /** 방 번호 배지 */
    public static final Color ROOM_NUMBER_BADGE = new Color(1, 49, 92);
    public static final Color ROOM_NUMBER_TEXT = new Color(255, 214, 50);
    /** 사람이 있는 자리 그림 칸 (60,170) */
    public static final Color SLOT_OCCUPIED = new Color(8, 194, 254);
    /** 자리 그림 칸 밝은 테두리 (49,230) */
    public static final Color SLOT_EDGE = new Color(142, 239, 254);
    /** 빈 자리·닫힌 자리 그림 칸 (70,400) */
    public static final Color SLOT_EMPTY = new Color(15, 150, 228);
    /** 닫힌 자리의 X (120,460) */
    public static final Color SLOT_CLOSED_MARK = new Color(10, 126, 205);
    /** 자리 닉네임 줄 (60,330) */
    public static final Color SLOT_NAME = new Color(9, 116, 214);
    /** 자리 상태 줄 (60,360) */
    public static final Color SLOT_STATUS = new Color(1, 49, 92);
    /** 상태 글자: 방장(하늘), 준비 완료(주황), 대기 중(흐린 남색) */
    public static final Color STATUS_HOST = new Color(20, 225, 245);
    public static final Color STATUS_READY = new Color(255, 182, 31);
    public static final Color STATUS_IDLE = new Color(10, 76, 131);
    /** 채팅 탭·로그 (200,642) */
    public static final Color CHAT_BACKGROUND = new Color(3, 117, 215);
    /** 채팅 입력 줄 바탕 */
    public static final Color CHAT_INPUT_ROW = new Color(3, 98, 201);
    /** 채팅 입력 칸 (400,855) */
    public static final Color CHAT_INPUT = new Color(0, 78, 172);
    public static final Color CHAT_TEXT = Color.WHITE;
    /** 시스템 안내 글자(노랑) */
    public static final Color CHAT_SYSTEM = new Color(255, 226, 70);
    /** 캐릭터 칸 (800,195) */
    public static final Color CHARACTER_CELL = new Color(35, 139, 211);
    /** 고른 칸 테두리 */
    public static final Color SELECTED_EDGE = new Color(255, 240, 214);
    /** 고른 칸의 체크 표시 */
    public static final Color CHECK_MARK = new Color(20, 235, 240);
    /** 캐릭터 배너(주황) */
    public static final Color BANNER_TOP = new Color(255, 196, 60);
    public static final Color BANNER_BOTTOM = new Color(246, 112, 22);
    /** 팀선택·맵 같은 섹션 이름 */
    public static final Color SECTION_LABEL = new Color(150, 210, 255);
    /** 맵 미리보기 칸 바탕(보라) (800,600) */
    public static final Color MAP_PREVIEW_BACKGROUND = new Color(87, 42, 180);
    /** 맵 정보 줄 (1130,570) */
    public static final Color MAP_INFO = new Color(3, 119, 207);
    public static final Color MAP_INFO_TEXT = new Color(176, 228, 255);
    /** 맵선택 버튼 (1135,710) */
    public static final Color MAP_SELECT = new Color(9, 148, 247);
    public static final Color MAP_SELECT_BOTTOM = new Color(22, 172, 233);
    public static final Color MAP_SELECT_TEXT = new Color(0, 58, 138);
    /** 준비 버튼 위·아래 (900,815)·(900,860) */
    public static final Color READY_TOP = new Color(242, 156, 3);
    public static final Color READY_BOTTOM = new Color(253, 198, 0);
    public static final Color READY_EDGE = new Color(255, 250, 235);
    public static final Color READY_TEXT_SHADOW = new Color(170, 80, 0);
    /** 아래 줄 (600,935) */
    public static final Color BOTTOM_BAR = new Color(7, 62, 130);
    /** 꺼진 버튼을 덮는 색 */
    public static final Color DISABLED_TEXT = new Color(120, 150, 185);

    /** a와 b를 t(0~1)만큼 섞은 색 */
    public static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}

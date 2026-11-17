package hansung.arcade.outgame.ui;

import hansung.arcade.ingame.hud.Box;

public final class WaitingRoomLayout {
    public static final int WIDTH = 800,
            HEIGHT = 600,
            SLOTS = 8,
            CHARACTER_CELLS = 8,
            MAP_INFO_ROWS = 4;
    public static final Box SCREEN = new Box(0, 0, 800, 600);
    public static final Box TOP_BAR = new Box(0, 0, 462, 34);
    public static final Box LEFT_PANEL = new Box(7, 39, 453, 518);
    public static final Box RIGHT_PANEL = new Box(464, 3, 333, 557);
    public static final Box BOTTOM_BAR = new Box(0, 561, 800, 39);
    public static final Box ROOM_HEADER = new Box(21, 59, 427, 28);
    public static final Box SLOT_GRID = new Box(21, 90, 427, 293);
    public static final Box CHAT = new Box(21, 389, 427, 160);
    public static final Box CHAT_INPUT_ROW = new Box(21, 518, 427, 31);
    public static final Box CHARACTER_SECTION = new Box(480, 14, 295, 244);
    public static final Box CHARACTER_BANNER = new Box(488, 23, 278, 84);
    public static final Box TEAM_SECTION = new Box(480, 274, 295, 51);
    public static final Box MAP_SECTION = new Box(480, 340, 295, 144);
    public static final Box MAP_PREVIEW = new Box(486, 347, 149, 130);
    public static final Box MAP_SELECT = new Box(644, 428, 121, 46);
    public static final Box READY_SECTION = new Box(500, 490, 253, 64);
    public static final Box READY_BUTTON = new Box(510, 495, 190, 53);
    public static final Box AUTO_READY = new Box(708, 496, 38, 50);
    public static final Box NOTICE = new Box(14, 5, 430, 24);
    public static final Box ROOM_NUMBER = new Box(25, 62, 42, 22);
    public static final Box ROOM_TITLE = new Box(72, 62, 300, 22);
    public static final Box ROOM_COUNT = new Box(390, 62, 52, 22);
    public static final Box CHAT_TAB = new Box(26, 392, 100, 20);
    public static final Box CHAT_LOG = new Box(26, 416, 417, 98);
    public static final Box TEAM_LABEL = new Box(484, 258, 160, 16);
    public static final Box MAP_LABEL = new Box(484, 325, 160, 15);
    public static final Box MAP_THUMBNAIL = new Box(501, 358, 119, 108);
    public static final Box LEAVE = new Box(680, 568, 110, 25);

    public static Box slot(int i) {
        return new Box(29 + i % 4 * 105, 96 + i / 4 * 144, 95, 137);
    }

    public static Box slotImage(int i) {
        Box b = slot(i);
        return new Box(b.x(), b.y(), 95, 95);
    }

    public static Box slotName(int i) {
        Box b = slot(i);
        return new Box(b.x(), b.y() + 99, 95, 20);
    }

    public static Box slotStatus(int i) {
        Box b = slot(i);
        return new Box(b.x(), b.y() + 120, 95, 17);
    }

    public static Box characterCell(int i) {
        return new Box(488 + i % 4 * 72, 115 + i / 4 * 70, 62, 63);
    }

    public static Box teamSwatch(int i) {
        return new Box(488 + i * 36, 284, 29, 31);
    }

    public static Box mapInfo(int i) {
        return new Box(641, 347 + i * 19, 127, 16);
    }
}

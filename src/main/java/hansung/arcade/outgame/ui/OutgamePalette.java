package hansung.arcade.outgame.ui;

import java.awt.Color;

public final class OutgamePalette {
    public static final Color LOBBY_BACKGROUND = p3(46, 107, 192);
    public static final Color ROOM_LIST = p3(88, 192, 242);
    public static final Color ROOM_HEADER = p3(67, 148, 216);
    public static final Color ROOM_ITEM = p3(103, 219, 251);
    public static final Color PLAYER_COUNT = p3(82, 168, 233);
    public static final Color MAP_PREVIEW = p3(78, 167, 248);
    public static final Color MAP_NAME_STRIP = p3(69, 147, 227);
    public static final Color ROOM_NUMBER_BOX = p3(72, 160, 238);
    public static final Color LOGIN_PANEL_TOP = p3(248, 215, 113);
    public static final Color LOGIN_PANEL_BOTTOM = p3(238, 127, 49);
    public static final Color LOGIN_PANEL_EDGE = p3(183, 90, 34);
    public static final Color LOGIN_TAB_TOP = p3(251, 232, 174);
    public static final Color LOGIN_TAB_BOTTOM = p3(255, 255, 84);
    public static final Color LOGIN_TAB_TEXT = p3(209, 86, 38);
    public static final Color LOGIN_SECTION = p3(98, 96, 98);
    public static final Color LOGIN_LABEL_BOX = p3(48, 111, 192);
    public static final Color WAITING_FRAME = new Color(3, 66, 141);
    public static final Color WAITING_NOTICE = new Color(2, 50, 118);
    public static final Color WAITING_LEFT_PANEL = new Color(1, 194, 250);
    public static final Color WAITING_RIGHT_PANEL = new Color(4, 106, 206);
    public static final Color WAITING_SECTION = new Color(3, 70, 141);
    public static final Color WAITING_HEADER = new Color(2, 146, 216);
    public static final Color SLOT_OCCUPIED = new Color(8, 194, 254);
    public static final Color SLOT_EMPTY = new Color(15, 150, 228);
    public static final Color SLOT_NAME = new Color(9, 116, 214);
    public static final Color SLOT_STATUS = new Color(1, 49, 92);
    public static final Color STATUS_HOST = new Color(20, 225, 245);
    public static final Color STATUS_READY = new Color(255, 182, 31);
    public static final Color STATUS_IDLE = new Color(10, 76, 131);
    public static final Color CHAT_BACKGROUND = new Color(3, 117, 215);
    public static final Color CHAT_INPUT = new Color(0, 78, 172);
    public static final Color CHARACTER_CELL = new Color(35, 139, 211);
    public static final Color MAP_PREVIEW_BACKGROUND = new Color(87, 42, 180);
    public static final Color MAP_INFO = new Color(3, 119, 207);
    public static final Color MAP_SELECT = new Color(9, 148, 247);
    public static final Color READY_TOP = new Color(242, 156, 3);
    public static final Color READY_BOTTOM = new Color(253, 198, 0);
    public static final Color BOTTOM_BAR = new Color(7, 62, 130);

    public static int[] toDisplayP3(Color c) {
        double r = linear(c.getRed() / 255.0),
                g = linear(c.getGreen() / 255.0),
                b = linear(c.getBlue() / 255.0);
        return new int[] {
            encoded(.822593 * r + .177534 * g),
            encoded(.0332 * r + .966783 * g),
            encoded(.017085 * r + .072396 * g + .910302 * b)
        };
    }

    public static Color p3(int r, int g, int b) {
        double x = linear(r / 255.0), y = linear(g / 255.0), z = linear(b / 255.0);
        return new Color(
                encoded(1.224745 * x - .224904 * y),
                encoded(-.042058 * x + 1.042081 * y),
                encoded(-.019642 * x - .078655 * y + 1.098537 * z));
    }

    private static double linear(double c) {
        return c <= .04045 ? c / 12.92 : Math.pow((c + .055) / 1.055, 2.4);
    }

    private static int encoded(double c) {
        c = Math.max(0, Math.min(1, c));
        return (int)
                Math.round(255 * (c <= .0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - .055));
    }
}

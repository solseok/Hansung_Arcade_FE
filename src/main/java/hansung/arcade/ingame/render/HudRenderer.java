package hansung.arcade.ingame.render;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Player;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * 오른쪽 정보 패널(맵 이름, 경과 시간, 플레이어 목록, 조작키).
 */
public final class HudRenderer {

    private static final Color PANEL = new Color(0x26384A);
    private static final Color PANEL_EDGE = new Color(0x3E5871);
    private static final Color CARD = new Color(0x31475D);
    private static final Color TEXT = new Color(0xE8EEF4);
    private static final Color TEXT_DIM = new Color(0x9FB2C4);

    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private static final Font TIME_FONT = new Font(Font.MONOSPACED, Font.BOLD, 22);
    private static final Font BODY_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

    public void draw(Graphics2D g, GameWorld world, int x, int y, int width, int height, int fps) {
        g.setColor(PANEL);
        g.fillRoundRect(x, y, width, height, 14, 14);
        g.setColor(PANEL_EDGE);
        g.drawRoundRect(x, y, width, height, 14, 14);

        int left = x + 12;
        int cursor = y + 26;

        g.setFont(SMALL_FONT);
        g.setColor(TEXT_DIM);
        g.drawString("MAP", left, cursor);
        cursor += 18;
        g.setFont(TITLE_FONT);
        g.setColor(TEXT);
        g.drawString(world.map().name(), left, cursor);

        cursor += 34;
        g.setFont(SMALL_FONT);
        g.setColor(TEXT_DIM);
        g.drawString("TIME", left, cursor);
        cursor += 24;
        g.setFont(TIME_FONT);
        g.setColor(TEXT);
        g.drawString(formatTime(world.elapsedSeconds()), left, cursor);

        cursor += 32;
        g.setFont(SMALL_FONT);
        g.setColor(TEXT_DIM);
        g.drawString("PLAYERS", left, cursor);
        cursor += 8;
        for (Player player : world.players()) {
            drawPlayerCard(g, player, player == world.localPlayer(), left, cursor, width - 24);
            cursor += 40;
        }

        int bottom = y + height - 14;
        g.setFont(BODY_FONT);
        g.setColor(TEXT_DIM);
        g.drawString("방향키  이동", left, bottom - 34);
        g.drawString("Space  물풍선 (예정)", left, bottom - 16);
        g.setFont(SMALL_FONT);
        g.drawString(fps + " FPS", left, bottom);
    }

    private void drawPlayerCard(Graphics2D g, Player player, boolean isLocal, int x, int y, int width) {
        g.setColor(CARD);
        g.fillRoundRect(x, y, width, 32, 10, 10);
        g.setColor(WorldRenderer.playerColor(player.id()));
        g.fillOval(x + 8, y + 8, 16, 16);
        g.setFont(BODY_FONT);
        g.setColor(TEXT);
        g.drawString("P" + player.id() + (isLocal ? "  (나)" : ""), x + 32, y + 21);
    }

    private static String formatTime(double seconds) {
        int total = (int) seconds;
        return "%02d:%02d".formatted(total / 60, total % 60);
    }
}

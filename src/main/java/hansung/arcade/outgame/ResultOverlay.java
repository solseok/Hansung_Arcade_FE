package hansung.arcade.outgame;

import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.outgame.ui.*;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

/** 경기 종료 후 맵 위로 올라오는 결과와 대기실 복귀 암전. 시간은 밀리초다. */
public final class ResultOverlay extends JComponent {
    public static final long RISE_DELAY_MS = 1500,
            RISE_MS = 600,
            FADE_START_MS = 8500,
            FADE_MS = 600,
            END_MS = FADE_START_MS + FADE_MS;
    public static final Color MODAL_BACKGROUND = new Color(15, 28, 52, 160);
    private List<GameResult.Standing> standings = List.of();
    private Set<Integer> localIds = Set.of();
    private long elapsed;
    private boolean finished, active;
    private Runnable onFinished = () -> {};

    public ResultOverlay() {
        setOpaque(false);
    }

    public void onFinished(Runnable action) {
        onFinished = action;
    }

    public void show(GameResult result, int playerId) {
        show(result, Set.of(playerId));
    }

    public void show(GameResult result, Set<Integer> ids) {
        standings =
                result.standings().stream()
                        .sorted(
                                Comparator.comparing(GameResult.Standing::outcome)
                                        .thenComparing(
                                                Comparator.comparingInt(GameResult.Standing::kills)
                                                        .reversed())
                                        .thenComparing(
                                                Comparator.comparingInt(GameResult.Standing::saves)
                                                        .reversed())
                                        .thenComparingInt(GameResult.Standing::playerId))
                        .toList();
        localIds = Set.copyOf(ids);
        elapsed = 0;
        finished = false;
        active = true;
        setVisible(true);
        repaint();
    }

    public Set<Integer> localPlayerIds() {
        return localIds;
    }

    public List<String> headers() {
        return List.of("NAME", "KILL", "SAVE");
    }

    public List<List<String>> rows() {
        return java.util.stream.IntStream.range(0, standings.size())
                .mapToObj(
                        i -> {
                            var s = standings.get(i);
                            return List.of(
                                    "[ " + (i + 1) + " ]",
                                    s.outcome().name(),
                                    s.nickname(),
                                    "%02d".formatted(s.kills()),
                                    "%02d".formatted(s.saves()));
                        })
                .toList();
    }

    public void tick(long time) {
        elapsed = Math.max(0, time);
        repaint();
        if (active && !finished && elapsed >= END_MS) {
            finished = true;
            onFinished.run();
        }
    }

    public boolean panelVisible() {
        return active && elapsed >= RISE_DELAY_MS;
    }

    public int fadeAlpha() {
        return (int) (255 * Math.max(0, Math.min(1, (elapsed - FADE_START_MS) / (double) FADE_MS)));
    }

    public Rectangle panelBounds() {
        var f = HudLayout.FIELD;
        int width = 450, height = 280;
        int target = f.y() + (f.height() - height) / 2;
        double fraction = Math.max(0, Math.min(1, (elapsed - RISE_DELAY_MS) / (double) RISE_MS));
        return new Rectangle(
                f.x() + (f.width() - width) / 2,
                (int) Math.round(f.bottom() + (target - f.bottom()) * fraction),
                width,
                height);
    }

    public Rectangle cellBounds(int row, int column) {
        Rectangle p = panelBounds();
        int[] xs = {10, 60, 125, 305, 370}, ws = {50, 65, 180, 65, 65};
        return new Rectangle(p.x + xs[column], p.y + 38 + row * 28, ws[column], 28);
    }

    public static Font nameFont() {
        return OutgameFonts.gulimChe(14).deriveFont(Font.BOLD);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Shapes.hints(g);
        g.scale(getWidth() / (double) HudLayout.WIDTH, getHeight() / (double) HudLayout.HEIGHT);
        if (panelVisible()) {
            Graphics2D modal = (Graphics2D) g.create();
            var f = HudLayout.FIELD;
            modal.clipRect(f.x(), f.y(), f.width(), f.height());
            Rectangle p = panelBounds();
            modal.setColor(MODAL_BACKGROUND);
            modal.fillRoundRect(p.x, p.y, p.width, p.height, 16, 16);
            modal.setFont(nameFont());
            for (int c = 2; c < 5; c++) {
                Rectangle cell = cellBounds(0, c);
                draw(
                        modal,
                        headers().get(c - 2),
                        new Rectangle(cell.x, p.y + 3, cell.width, 32),
                        Color.WHITE);
            }
            var rows = rows();
            for (int r = 0; r < rows.size(); r++)
                for (int c = 0; c < 5; c++)
                    draw(
                            modal,
                            rows.get(r).get(c),
                            cellBounds(r, c),
                            localIds.contains(standings.get(r).playerId())
                                    ? Color.YELLOW
                                    : Color.WHITE);
            modal.dispose();
        }
        if (fadeAlpha() > 0) {
            g.setColor(new Color(0, 0, 0, fadeAlpha()));
            g.fillRect(0, 0, HudLayout.WIDTH, HudLayout.HEIGHT);
        }
        g.dispose();
    }

    private void draw(Graphics2D g, String text, Rectangle box, Color color) {
        g.setColor(color);
        var fm = g.getFontMetrics();
        g.drawString(
                text,
                box.x + (box.width - fm.stringWidth(text)) / 2,
                box.y + (box.height - fm.getHeight()) / 2 + fm.getAscent());
    }
}

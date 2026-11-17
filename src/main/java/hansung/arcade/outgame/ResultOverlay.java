package hansung.arcade.outgame;

import hansung.arcade.ingame.hud.Box;
import hansung.arcade.ingame.hud.HudLayout;
import hansung.arcade.outgame.ui.Shapes;
import hansung.arcade.outgame.ui.OutgameFonts;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 게임이 끝난 뒤 게임 화면 위에 띄우는 결과 모달과 종료 연출.
 * <ol>
 *   <li>마지막 플레이어가 죽은 때(0초)부터 {@link #RISE_DELAY_MS 1.5초} 동안은 아무것도 없다.</li>
 *   <li>1.5초부터 {@link #RISE_MS}동안 모달이 맵 프레임 안쪽 아래 끝에서 위로 올라온다. 맵 밖으로는 그리지 않는다.</li>
 *   <li>{@link #FADE_START_MS 8.5초}부터 {@link #FADE_MS}동안 화면 전체가 검게 어두워지고,
 *       완전히 검어지면 {@link #onFinished} 한 번 부른다(원래 대기방으로 돌아가기).</li>
 * </ol>
 * 모달은 불투명도를 낮춘 바탕에 | NAME | KILL | SAVE | 머리줄과 [ 순위 ] 승패 이름 | 킬 | 세이브 | 줄만 보여 준다.
 * 크기는 인원과 상관없이 가장 많은 {@link #MAX_ROWS 8명}이 들어가는 고정 크기다. 이 컴퓨터의 플레이어(2P면 둘) 줄은
 * 파란 바탕에 노란 글자로 강조하고, 이름은 굵은 굴림체로 쓴다.
 * 모달은 1배 그림으로 미리 그려 두지 않고 그릴 때마다 화면에 바로 그린다. 미리 그린 그림은 레티나(2배) 화면에서
 * 늘어나 글자가 뭉개지기 때문이다.
 * 시간은 {@link #tick}으로 넘겨받으므로 테스트에서 원하는 시각을 바로 볼 수 있다.
 */
public final class ResultOverlay extends JComponent {

    public static final long RISE_DELAY_MS = 1500;
    public static final long RISE_MS = 600;
    public static final long FADE_START_MS = 8500;
    public static final long FADE_MS = 800;
    public static final long END_MS = FADE_START_MS + FADE_MS;
    /** 모달 바탕: 남색에 알파 150 (약 59%) */
    public static final Color MODAL_BACKGROUND = new Color(10, 20, 45, 150);
    /** 내 플레이어 줄 글자 */
    public static final Color MINE_TEXT = new Color(255, 226, 40);
    /** 모달에 들어가는 줄 수: 가장 많이 들어가는 맵의 인원 */
    public static final int MAX_ROWS = GameMaps.RANDOM.maxPlayers();

    private static final Color HEADER = new Color(0, 0, 0, 170);
    private static final Color MY_ROW = new Color(62, 168, 236);
    private static final Color OTHER_ROW = new Color(255, 255, 255, 40);
    private static final Color WIN = new Color(255, 214, 50);
    private static final Color LOSE = new Color(170, 190, 220);
    private static final Color TEXT = Color.WHITE;
    private static final Color TEXT_SHADOW = new Color(0, 0, 0);
    private static final int PANEL_WIDTH = 520;
    private static final int PADDING = 10;
    private static final int HEADER_HEIGHT = 24;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_GAP = 4;
    /** 다 올라온 모달 위쪽이 맵 영역 위에서 떨어진 거리 */
    private static final int FINAL_TOP = 96;
    // 칸 왼쪽 끝 (모달 기준): [ 순위 ] | 승패 | NAME | KILL | SAVE
    private static final int[] COLUMN_X = {PADDING, 70, 140, 340, 430};
    private static final int[] COLUMN_WIDTH = {56, 64, 196, 86, 80};
    private static final int PANEL_HEIGHT = rowTop(MAX_ROWS) - ROW_GAP + PADDING;

    private GameResult result;
    private Set<Integer> localPlayerIds = Set.of();
    private long elapsed;
    private boolean running;
    private boolean finished;
    private Runnable onFinished = () -> { };

    public ResultOverlay() {
        setOpaque(false);
        // 결과가 떠 있는 동안 뒤의 게임 화면을 누르지 못하게 마우스를 받아 둔다
        addMouseListener(new MouseAdapter() { });
    }

    public void onFinished(Runnable listener) {
        onFinished = listener;
    }

    /** 결과를 띄우기 시작한다(0초). localPlayerId(이 컴퓨터의 플레이어)의 줄을 강조한다. */
    public void show(GameResult result, int localPlayerId) {
        show(result, Set.of(localPlayerId));
    }

    /** 결과를 띄우기 시작한다(0초). 이 컴퓨터의 플레이어들(2P면 둘)의 줄을 강조한다. */
    public void show(GameResult result, Set<Integer> localPlayerIds) {
        this.result = result;
        this.localPlayerIds = Set.copyOf(localPlayerIds);
        elapsed = 0;
        running = true;
        finished = false;
        setVisible(true);
        repaint();
    }

    /** 강조하는 이 컴퓨터의 플레이어 번호들 */
    public Set<Integer> localPlayerIds() {
        return localPlayerIds;
    }

    /** 이름 글꼴: 사용자가 정한 문구라 굴림체, 테두리에 묻히지 않게 굵게 */
    public static Font nameFont() {
        return OutgameFonts.gulimCheBold(14);
    }

    /** 결과를 치운다. 숨겨서 뒤의 게임 화면이 마우스를 다시 받게 한다. */
    public void reset() {
        running = false;
        result = null;
        setVisible(false);
    }

    public boolean isRunning() {
        return running;
    }

    /** 게임이 끝난 뒤 elapsedMs가 지난 모습으로 바꾼다. 완전히 검어지는 때에 한 번 끝났다고 알린다. */
    public void tick(long elapsedMs) {
        if (!running) {
            return;
        }
        elapsed = elapsedMs;
        repaint();
        if (!finished && elapsed >= END_MS) {
            finished = true;
            onFinished.run();
        }
    }

    public boolean panelVisible() {
        return running && elapsed >= RISE_DELAY_MS;
    }

    /** 지금 모달 영역 (게임 화면 기준). 크기는 늘 같다 */
    public Rectangle panelBounds() {
        Box field = HudLayout.FIELD;
        int x = field.x() + (field.width() - PANEL_WIDTH) / 2;
        int finalY = field.y() + FINAL_TOP;
        double t = Math.max(0, Math.min(1, (elapsed - RISE_DELAY_MS) / (double) RISE_MS));
        double eased = 1 - Math.pow(1 - t, 3);
        int y = (int) Math.round(field.bottom() + (finalY - field.bottom()) * eased);
        return new Rectangle(x, y, PANEL_WIDTH, PANEL_HEIGHT);
    }

    /**
     * row번째 줄(0부터)의 column번 칸 (게임 화면 기준).
     * 칸은 0 [ 순위 ], 1 승패, 2 이름, 3 KILL, 4 SAVE 순이다.
     */
    public Rectangle cellBounds(int row, int column) {
        Rectangle panel = panelBounds();
        return new Rectangle(panel.x + COLUMN_X[column], panel.y + rowTop(row), COLUMN_WIDTH[column], ROW_HEIGHT);
    }

    /** 화면 전체를 덮는 검은색의 알파 (0~255) */
    public int fadeAlpha() {
        if (!running || elapsed < FADE_START_MS) {
            return 0;
        }
        return (int) Math.round(255 * Math.min(1, (elapsed - FADE_START_MS) / (double) FADE_MS));
    }

    public List<String> headers() {
        return List.of("NAME", "KILL", "SAVE");
    }

    /** 순위대로 [ 순위 ], 승패, 이름, kill(두 자리), save(두 자리) */
    public List<List<String>> rows() {
        List<List<String>> rows = new ArrayList<>();
        if (result == null) {
            return rows;
        }
        int rank = 1;
        for (GameResult.Standing standing : result.ranked()) {
            rows.add(List.of("[ %d ]".formatted(rank++), standing.outcome().name(), standing.nickname(),
                    "%02d".formatted(standing.kills()), "%02d".formatted(standing.saves())));
        }
        return rows;
    }

    /** row번째 줄 위쪽 (모달 기준) */
    private static int rowTop(int row) {
        return PADDING + HEADER_HEIGHT + ROW_GAP + row * (ROW_HEIGHT + ROW_GAP);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (!running) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            Shapes.hints(g);
            if (panelVisible()) {
                Box field = HudLayout.FIELD;
                Graphics2D panel = (Graphics2D) g.create();
                try {
                    panel.clipRect(field.x(), field.y(), field.width(), field.height());
                    Rectangle bounds = panelBounds();
                    panel.translate(bounds.x, bounds.y);
                    paintPanel(panel);
                } finally {
                    panel.dispose();
                }
            }
            int alpha = fadeAlpha();
            if (alpha > 0) {
                g.setColor(new Color(0, 0, 0, alpha));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        } finally {
            g.dispose();
        }
    }

    /** 모달 기준 좌표로 바탕·머리줄·줄을 그린다. 글자는 그리는 화면의 배율대로 그려진다 */
    private void paintPanel(Graphics2D g) {
        g.setColor(MODAL_BACKGROUND);
        g.fill(Shapes.round(0, 0, PANEL_WIDTH, PANEL_HEIGHT, 6));
        // 이름은 사용자가 정한 문구라 굵은 굴림체, 나머지(머리줄·순위·승패·숫자)는 게임 글꼴
        Font font = OutgameFonts.label(13);
        Font nameFont = nameFont();
        // 머리줄: 이름·KILL·SAVE 칸만
        int headerLeft = COLUMN_X[2] - 6;
        g.setColor(HEADER);
        g.fill(Shapes.round(headerLeft, PADDING, PANEL_WIDTH - PADDING - headerLeft, HEADER_HEIGHT, 4));
        List<String> headers = headers();
        for (int i = 0; i < headers.size(); i++) {
            Shapes.text(g, headers.get(i), font, TEXT, TEXT_SHADOW, 1, null, COLUMN_X[i + 2], PADDING,
                    COLUMN_WIDTH[i + 2], HEADER_HEIGHT, SwingConstants.CENTER);
        }
        List<GameResult.Standing> ranked = result.ranked();
        List<List<String>> rows = rows();
        for (int r = 0; r < rows.size() && r < MAX_ROWS; r++) {
            GameResult.Standing standing = ranked.get(r);
            boolean mine = localPlayerIds.contains(standing.playerId());
            int y = rowTop(r);
            g.setColor(mine ? MY_ROW : OTHER_ROW);
            g.fill(Shapes.round(PADDING, y, PANEL_WIDTH - 2 * PADDING, ROW_HEIGHT, 4));
            List<String> row = rows.get(r);
            for (int c = 0; c < row.size(); c++) {
                Color color;
                if (c == 1) {
                    color = standing.outcome() == GameResult.Outcome.WIN ? WIN : LOSE;
                } else {
                    color = mine ? MINE_TEXT : TEXT;
                }
                boolean name = c == 2;
                Shapes.text(g, row.get(c), name ? nameFont : font, color, TEXT_SHADOW, 1, null,
                        COLUMN_X[c] + (name ? 6 : 0), y, COLUMN_WIDTH[c], ROW_HEIGHT,
                        name ? SwingConstants.LEFT : SwingConstants.CENTER);
            }
        }
    }
}

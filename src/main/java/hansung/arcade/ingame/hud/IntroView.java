package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.match.StartIntro;
import hansung.arcade.ingame.render.IntroSprites;
import hansung.arcade.ingame.render.WorldRenderer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 경기 시작 연출({@link StartIntro}) "Ready... Start!"를 그린다. 크레이지아케이드 플레이 영상의 흐름을 따른다.
 *
 * <ul>
 *   <li>READY: 화면 전체를 검은 막으로 덮는다 ({@link #paintBlackout}). 글자는 없다.</li>
 *   <li>START: 맵 가운데보다 조금 위에 START!! 글자 그림(resources/intro/game_start.bmp의 아랫줄)을 1배로 띄운다.
 *       검은 막이 걷히며 글자와 맵이 함께 밝아지고, 끝나기 {@link StartIntro#START_EXIT_SECONDS}초 전부터 왼쪽 글자부터
 *       한 자씩 위로 튀어 올라 사라진다. 이어진 느낌표 둘은 한 덩어리로 함께 튀어 오른다.</li>
 *   <li>화살표: START!! 글자가 뜬 뒤부터 사라지고 조금 뒤까지, 이 컴퓨터 플레이어의 머리 바로 위에 아래를 가리키는 화살표
 *       (resources/intro/player_pos.bmp)를 팀 색으로 칠해 띄운다. 닉네임 위를 덮는다. 위아래로 {@link #ARROW_BOB}px 흔들린다.
 *       맨 윗줄처럼 닉네임이 발 아래에 있으면 화살표를 뒤집어 발 아래에서 위를 가리킨다.
 *       2인 모드면 두 사람이 같은 팀 색이어도 가릴 수 있게 화살표 위(아래)에 "1P"·"2P"를 적는다.</li>
 * </ul>
 */
final class IntroView implements HudPart {

    /** START!! 글자 한 자가 튀어 올라 사라지는 시간(초) */
    static final double GLYPH_FLY_SECONDS = 0.15;
    /** 튀어 오르는 글자가 사라질 때까지 올라가는 높이(px). 글자 높이쯤 */
    static final int GLYPH_FLY_DISTANCE = 60;
    /** 튀어 오르는 글자가 이만큼(0~1) 올라간 뒤부터 흐려진다 */
    static final double GLYPH_FADE_FROM = 0.6;
    /** START!! 글자 줄 가운데 높이: 맵 영역 위에서 이 비율만큼 아래 */
    static final double START_CENTER_Y = 0.45;
    /** 화살표가 위아래로 흔들리는 폭(px) */
    static final int ARROW_BOB = 4;
    /** 화살표가 한 번 오르내리는 시간(초) */
    static final double ARROW_BOB_SECONDS = 0.5;
    /** 화살표 끝이 캐릭터 위치(충돌 박스 가운데)보다 높은 거리(px): 머리 꼭대기 바로 위 */
    static final int ARROW_TIP_ABOVE_PLAYER = HudLayout.PLAYER_TAG_ABOVE_PLAYER - 2;
    /** 뒤집은 화살표 끝이 캐릭터 위치보다 낮은 거리(px): 발 아래 */
    static final int ARROW_TIP_BELOW_PLAYER = HudLayout.PLAYER_TAG_BELOW_PLAYER - 4;

    /** 화살표와 1P·2P 글자 사이(px). 글자 테두리가 화살표에 겹치지 않게 띄운다 */
    static final int LABEL_GAP = 3;
    private static final Color LABEL_EDGE = new Color(0x1A1A1A);
    private static final List<String> LABEL_FONTS = List.of("Arial Black", "Impact", "Arial Rounded MT Bold");
    /** 고른 글꼴. 설치된 글꼴 목록을 읽는 데 시간이 걸려 한 번만 고른다 */
    private static Font labelBase;

    private final OutlineText nicknameText;
    private final IntroSprites sprites;
    private final Font labelFont;

    /** @param nicknameText 닉네임을 그리는 글자. 화살표가 닉네임 위·아래 어디에 갈지 정할 때 닉네임 칸을 잰다 */
    IntroView(OutlineText nicknameText) {
        this(nicknameText, IntroSprites.shared());
    }

    IntroView(OutlineText nicknameText, IntroSprites sprites) {
        this.nicknameText = nicknameText;
        this.sprites = sprites;
        this.labelFont = labelFont().deriveFont(Font.BOLD, 14f);
    }

    @Override
    public void paint(Graphics2D g, GameWorld world) {
        StartIntro intro = world.intro();
        if (intro.showsArrow()) {
            paintArrows(g, world, intro.elapsedSeconds());
        }
        if (intro.phase() == StartIntro.Phase.START) {
            paintStart(g, intro.phaseSeconds());
        }
    }

    /** READY 동안 화면 전체를 덮는 검은 막. HUD를 모두 그린 뒤 맨 위에 그린다 */
    void paintBlackout(Graphics2D g, GameWorld world) {
        double blackout = world.intro().blackout();
        if (blackout <= 0) {
            return;
        }
        Composite previous = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, blackout)));
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, HudLayout.WIDTH, HudLayout.HEIGHT);
        g.setComposite(previous);
    }

    /** index번째(0부터) 이 컴퓨터 플레이어의 표시. 0번째가 "1P"다 */
    static String tagOf(int index) {
        return (index + 1) + "P";
    }

    /** START!! 줄의 왼쪽 위 (화면 좌표). 맵 영역 가로 가운데, 세로 {@link #START_CENTER_Y} */
    Box startBox() {
        Box field = HudLayout.FIELD;
        int x = field.x() + (field.width() - sprites.startWidth()) / 2;
        int y = field.y() + (int) Math.round(field.height() * START_CENTER_Y) - sprites.startHeight() / 2;
        return new Box(x, y, sprites.startWidth(), sprites.startHeight());
    }

    /**
     * START 단계가 시작된 뒤 t초 때의 START!! 글자. 도트 그림이라 늘리거나 줄이지 않는다.
     * 밝아지는 것은 위에 덮는 검은 막이 맡는다.
     */
    private void paintStart(Graphics2D g, double t) {
        Box line = startBox();
        List<IntroSprites.Glyph> glyphs = sprites.startGlyphs();
        int[] units = flightUnits(glyphs);
        int unitCount = units.length == 0 ? 0 : units[units.length - 1] + 1;
        double stagger = unitCount > 1
                ? (StartIntro.START_EXIT_SECONDS - GLYPH_FLY_SECONDS) / (unitCount - 1) : 0;
        Composite previous = g.getComposite();
        for (int i = 0; i < glyphs.size(); i++) {
            IntroSprites.Glyph glyph = glyphs.get(i);
            double p = Math.max(0, Math.min(1,
                    (t - StartIntro.START_HOLD_SECONDS - units[i] * stagger) / GLYPH_FLY_SECONDS));
            if (p >= 1) {
                continue;
            }
            // 튀어 오르다(처음에 빠르고 끝에서 느리게) 거의 다 올라가면 흐려진다
            int dy = (int) Math.round(-GLYPH_FLY_DISTANCE * (1 - (1 - p) * (1 - p)));
            float alpha = p <= GLYPH_FADE_FROM ? 1f : (float) ((1 - p) / (1 - GLYPH_FADE_FROM));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(glyph.image(), line.x() + glyph.x(), line.y() + dy, null);
        }
        g.setComposite(previous);
    }

    /** 글자마다 몇 번째로 튀어 오르는지 (0부터). 이어진 느낌표들은 앞 느낌표와 같은 차례다 */
    static int[] flightUnits(List<IntroSprites.Glyph> glyphs) {
        int[] units = new int[glyphs.size()];
        int unit = -1;
        for (int i = 0; i < glyphs.size(); i++) {
            boolean joined = i > 0 && glyphs.get(i).exclamation() && glyphs.get(i - 1).exclamation();
            if (!joined) {
                unit++;
            }
            units[i] = unit;
        }
        return units;
    }

    private void paintArrows(Graphics2D g, GameWorld world, double seconds) {
        List<Player> locals = world.localPlayers();
        int bob = (int) Math.round(ARROW_BOB * (0.5 - 0.5 * Math.cos(2 * Math.PI * seconds / ARROW_BOB_SECONDS)));
        int originX = HudLayout.mapOriginX(world.map());
        int originY = HudLayout.mapOriginY(world.map());
        for (int i = 0; i < locals.size(); i++) {
            Player player = locals.get(i);
            if (!player.isAlive()) {
                continue;
            }
            BufferedImage arrow = sprites.arrow(player.profile().color());
            int w = arrow.getWidth();
            int h = arrow.getHeight();
            int playerX = originX + (int) Math.round(player.x());
            int playerY = originY + (int) Math.round(player.y());
            int lift = WorldRenderer.figureLiftOf(player, world.gear(player));
            Box nickname = PlayerTagView.placeOf(nicknameText, world, player, PlayerTagView.nicknameOf(player));
            boolean above = nickname.bottom() <= playerY;
            int x = playerX - w / 2;
            String label = locals.size() > 1 ? tagOf(i) : null;
            if (above) {
                int tip = playerY - lift - ARROW_TIP_ABOVE_PLAYER - bob;
                g.drawImage(arrow, x, tip - h, null);
                if (label != null) {
                    paintLabel(g, label, playerX, tip - h - LABEL_GAP, true);
                }
            } else {
                // 뒤집어 그려 위를 가리킨다
                int tip = playerY + ARROW_TIP_BELOW_PLAYER + bob;
                g.drawImage(arrow, x, tip + h, x + w, tip, 0, 0, w, h, null);
                if (label != null) {
                    paintLabel(g, label, playerX, tip + h + LABEL_GAP, false);
                }
            }
        }
    }

    /** "1P" 글자 (흰 글자에 검은 테두리). above면 글자 아래 끝을, 아니면 위 끝을 edgeY에 맞춘다 */
    private void paintLabel(Graphics2D g, String label, int centerX, int edgeY, boolean above) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            TextLayout layout = new TextLayout(label, labelFont, g2.getFontRenderContext());
            Rectangle2D ink = layout.getBounds();
            double x = centerX - ink.getWidth() / 2 - ink.getX();
            double y = above ? edgeY - ink.getMaxY() : edgeY - ink.getY();
            Shape outline = layout.getOutline(AffineTransform.getTranslateInstance(x, y));
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(LABEL_EDGE);
            g2.draw(outline);
            g2.setColor(Color.WHITE);
            g2.fill(outline);
        } finally {
            g2.dispose();
        }
    }

    /** 굵은 영문 글꼴. 운영체제마다 있는 것을 고르고, 없으면 Java 기본 산세리프 굵게 */
    private static synchronized Font labelFont() {
        if (labelBase == null) {
            Set<String> families;
            try {
                families = Set.copyOf(Arrays.asList(
                        GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            } catch (RuntimeException | Error headless) {
                families = Set.of();
            }
            labelBase = LABEL_FONTS.stream().filter(families::contains).findFirst()
                    .map(family -> new Font(family, Font.BOLD, 12))
                    .orElse(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        }
        return labelBase;
    }
}

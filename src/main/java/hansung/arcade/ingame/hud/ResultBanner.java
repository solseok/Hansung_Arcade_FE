package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerState;
import hansung.arcade.ingame.match.MatchOutcome;
import hansung.arcade.ingame.match.MatchResult;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static hansung.arcade.ingame.hud.HudLayout.RESULT_BAND;
import static hansung.arcade.ingame.hud.HudLayout.RESULT_BAND_EDGE;
import static hansung.arcade.ingame.hud.HudLayout.RESULT_DOT;
import static hansung.arcade.ingame.hud.HudLayout.RESULT_TEXT_HEIGHT;

/**
 * 경기가 끝나면 맵 한가운데에 내 결과(WIN, LOSE, DRAW)를 크게 띄운다. 맵 폭만 한 반투명 띠 위에,
 * 결과마다 다른 색의 글씨를 굵은 테두리와 그림자를 둘러 그린다.
 *
 * <p>한 키보드 2인 모드에서 둘의 결과가 다르면 이긴 쪽을 "1P WIN"처럼 띄운다. 둘이 같으면(비김, 같은 팀) 그 결과만 띄운다.
 * 누군가 탈락 연출(쓰러져 솟았다 떨어지고 깜빡이는 2초) 중이면 띠가 그 모습을 가리지 않게 연출이 끝난 뒤에 띄운다.</p>
 *
 * <p>글씨는 {@link HudLayout#RESULT_DOT}배 작게 도트로 찍어 테두리·색을 입힌 뒤 그만큼 키운다.
 * 그래서 다른 HUD처럼 계단진 도트 가장자리가 되고, 색은 위에서 아래로 몇 단계로만 나뉜다.</p>
 *
 * <p>글꼴은 "내 아이템" 글씨와 같은 HY견고딕이고, 없으면 그 자리에 대신 쓰는 기본 굵은 글꼴이다.
 * 결과 화면 디자인 이미지가 생기면 {@link #render}만 그림으로 바꾸면 된다.</p>
 */
final class ResultBanner implements HudPart {

    /** 테두리 두께(도트) */
    private static final int OUTLINE = 2;
    /** 그림자가 글씨 아래로 내려가는 거리(도트) */
    private static final int SHADOW = 2;
    /** 위에서 아래로 나누는 색 단계 수 */
    private static final int SHADES = 4;
    /** 글자 픽셀로 치는 최소 알파. 글꼴 가장자리의 반투명 픽셀 중 절반 넘게 덮인 것만 도트로 남긴다 */
    private static final int OPAQUE = 0x80;

    private final Font font;
    private final BufferedImage band;
    private final Map<Headline, Word> words = new HashMap<>();

    ResultBanner(HudSkin skin) {
        Font base = skin.itemBarLabel() instanceof FontTextArt art ? art.font() : new Font(Font.SANS_SERIF, Font.BOLD, 12);
        this.font = HudFonts.fitInkHeight(base, "WINLOSEDRAW", RESULT_TEXT_HEIGHT / RESULT_DOT);
        this.band = band();
    }

    /** 결과 글씨 */
    static String textOf(MatchOutcome outcome) {
        return outcome.name();
    }

    /**
     * 이 컴퓨터의 플레이어들(locals, 1P부터)에게 보여 줄 결과 글씨와 색. 혼자거나 모두 같은 결과면 그 결과이고,
     * 한 명만 이겼으면 그 사람의 표시를 붙인 "1P WIN"·"2P WIN"이다.
     */
    static Headline headlineOf(MatchResult result, List<Player> locals) {
        MatchOutcome first = result.outcomeOf(locals.get(0).id());
        boolean same = locals.stream().allMatch(player -> result.outcomeOf(player.id()) == first);
        if (!same) {
            List<Integer> winners = new ArrayList<>();
            for (int i = 0; i < locals.size(); i++) {
                if (result.outcomeOf(locals.get(i).id()) == MatchOutcome.WIN) {
                    winners.add(i);
                }
            }
            if (winners.size() == 1) {
                return new Headline(PlayerTagView.tagOf(winners.get(0)) + " " + textOf(MatchOutcome.WIN),
                        MatchOutcome.WIN);
            }
        }
        return new Headline(textOf(first), first);
    }

    @Override
    public void paint(Graphics2D g, GameWorld world) {
        Optional<MatchResult> result = world.match().result();
        if (result.isEmpty() || world.players().stream().anyMatch(player -> player.state() == PlayerState.DYING)) {
            return;
        }
        Word word = words.computeIfAbsent(headlineOf(result.get(), world.localPlayers()), this::render);
        g.drawImage(band, RESULT_BAND.x(), RESULT_BAND.y(), null);
        int centerX = RESULT_BAND.x() + RESULT_BAND.width() / 2;
        int centerY = RESULT_BAND.y() + RESULT_BAND.height() / 2;
        g.drawImage(word.image(), centerX - word.centerX(), centerY - word.centerY(), null);
    }

    private static BufferedImage band() {
        PixelCanvas canvas = new PixelCanvas(RESULT_BAND.width(), RESULT_BAND.height());
        Box whole = new Box(0, 0, RESULT_BAND.width(), RESULT_BAND.height());
        canvas.fill(whole, Corners.SQUARE, HudPalette.RESULT_BAND);
        canvas.fill(whole.child(0, 0, whole.width(), RESULT_BAND_EDGE), Corners.SQUARE, HudPalette.RESULT_BAND_EDGE);
        canvas.fill(whole.child(0, whole.height() - RESULT_BAND_EDGE, whole.width(), RESULT_BAND_EDGE), Corners.SQUARE,
                HudPalette.RESULT_BAND_EDGE);
        return canvas.image();
    }

    /** 결과 글씨 한 장. 글씨와 테두리를 담은 영역의 가운데를 함께 돌려준다 (그림자는 가운데를 잡을 때 빼고 본다). */
    Word render(Headline headline) {
        MatchOutcome outcome = headline.style();
        Color top = switch (outcome) {
            case WIN -> HudPalette.WIN_TOP;
            case LOSE -> HudPalette.LOSE_TOP;
            case DRAW -> HudPalette.DRAW_TOP;
        };
        Color bottom = switch (outcome) {
            case WIN -> HudPalette.WIN_BOTTOM;
            case LOSE -> HudPalette.LOSE_BOTTOM;
            case DRAW -> HudPalette.DRAW_BOTTOM;
        };
        Color outline = switch (outcome) {
            case WIN -> HudPalette.WIN_OUTLINE;
            case LOSE -> HudPalette.LOSE_OUTLINE;
            case DRAW -> HudPalette.DRAW_OUTLINE;
        };
        boolean[][] glyph = glyphDots(headline.text());
        int height = glyph.length;
        int width = glyph[0].length;
        boolean[][] outlined = dilate(glyph, OUTLINE);
        Box ink = bounds(glyph);

        BufferedImage dots = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (glyph[y][x]) {
                    boolean edgeTop = y == 0 || !glyph[y - 1][x];
                    dots.setRGB(x, y, edgeTop ? HudPalette.RESULT_HIGHLIGHT.getRGB() : shade(top, bottom, ink, y));
                } else if (outlined[y][x]) {
                    dots.setRGB(x, y, outline.getRGB());
                } else if (y >= SHADOW && outlined[y - SHADOW][x]) {
                    dots.setRGB(x, y, HudPalette.RESULT_SHADOW.getRGB());
                }
            }
        }
        Box outlineBox = bounds(outlined);
        BufferedImage image = scale(dots, RESULT_DOT);
        return new Word(image, (2 * outlineBox.x() + outlineBox.width()) * RESULT_DOT / 2,
                (2 * outlineBox.y() + outlineBox.height()) * RESULT_DOT / 2);
    }

    /** 글씨를 도트 크기로 찍어 글자 픽셀이면 true. 둘레에 테두리와 그림자가 들어갈 자리를 비워 둔다 */
    private boolean[][] glyphDots(String text) {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        FontMetrics metrics = pg.getFontMetrics(font);
        pg.dispose();
        int pad = OUTLINE + SHADOW + 1;
        int width = metrics.stringWidth(text) + 2 * pad;
        int height = metrics.getAscent() + metrics.getDescent() + 2 * pad;
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.drawString(text, pad, pad + metrics.getAscent());
        g.dispose();
        boolean[][] dots = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                dots[y][x] = (mask.getRGB(x, y) >>> 24) >= OPAQUE;
            }
        }
        return dots;
    }

    /** 글자 픽셀에서 radius 도트 안(모서리는 둥글게)에 드는 자리 */
    private static boolean[][] dilate(boolean[][] dots, int radius) {
        int height = dots.length;
        int width = dots[0].length;
        boolean[][] out = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!dots[y][x]) {
                    continue;
                }
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int ny = y + dy;
                        int nx = x + dx;
                        if (dx * dx + dy * dy <= radius * radius + 1 && ny >= 0 && nx >= 0 && ny < height && nx < width) {
                            out[ny][nx] = true;
                        }
                    }
                }
            }
        }
        return out;
    }

    private static Box bounds(boolean[][] dots) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < dots.length; y++) {
            for (int x = 0; x < dots[y].length; x++) {
                if (dots[y][x]) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? new Box(0, 0, 0, 0) : new Box(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /** 글씨 윗줄부터 아랫줄까지를 {@link #SHADES}단계로 나눠 y줄이 속한 단계의 색 */
    private static int shade(Color top, Color bottom, Box ink, int y) {
        double t = ink.height() <= 1 ? 0 : (y - ink.y()) / (double) (ink.height() - 1);
        int step = Math.min(SHADES - 1, (int) (t * SHADES));
        double mix = step / (double) (SHADES - 1);
        int r = (int) Math.round(top.getRed() + (bottom.getRed() - top.getRed()) * mix);
        int g = (int) Math.round(top.getGreen() + (bottom.getGreen() - top.getGreen()) * mix);
        int b = (int) Math.round(top.getBlue() + (bottom.getBlue() - top.getBlue()) * mix);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** 도트 하나를 factor x factor 픽셀로 키운다 */
    private static BufferedImage scale(BufferedImage dots, int factor) {
        BufferedImage out = new BufferedImage(dots.getWidth() * factor, dots.getHeight() * factor,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(dots, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }

    /**
     * 결과 띠에 띄울 글씨.
     *
     * @param text  글씨 (WIN, 1P WIN 등)
     * @param style 글씨 색을 고르는 결과
     */
    record Headline(String text, MatchOutcome style) {
    }

    /**
     * 그려 둔 결과 글씨.
     *
     * @param centerX 그림 안에서 글씨와 테두리를 담은 영역의 가로 가운데
     * @param centerY 같은 영역의 세로 가운데
     */
    record Word(BufferedImage image, int centerX, int centerY) {
    }
}

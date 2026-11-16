package hansung.arcade.outgame.lobby;

import hansung.arcade.ingame.map.MapLoader;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.render.Tileset;
import hansung.arcade.ingame.render.TileSprite;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** 기존 게임 타일을 사용하는 로비 미리보기와 버튼 스타일. EDT에서 사용한다. */
public final class LobbyArtwork {
    private static final Map<String, BufferedImage> MAPS = new HashMap<>();

    private LobbyArtwork() { }

    /** 밝은 안쪽 선과 짙은 바깥 선으로 원본 UI의 플라스틱 프레임을 표현한다. */
    public static javax.swing.border.Border frame(int padding) {
        return javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(0x00558F), 2),
                javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createLineBorder(new Color(0x8DF5FF), 2),
                        javax.swing.BorderFactory.createEmptyBorder(padding, padding, padding, padding)));
    }

    public static javax.swing.JPanel panel(java.awt.LayoutManager layout, Color top, Color bottom) {
        return new javax.swing.JPanel(layout) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
    }

    public static javax.swing.JLabel outlined(String text, int size, Color fill, Color outline) {
        return new javax.swing.JLabel(text) {
            { setFont(new Font(Font.SANS_SERIF, Font.BOLD, size)); }
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                java.awt.font.TextLayout layout = new java.awt.font.TextLayout(getText(), getFont(), g.getFontRenderContext());
                java.awt.Shape shape = layout.getOutline(java.awt.geom.AffineTransform.getTranslateInstance(
                        2, (getHeight() + layout.getAscent() - layout.getDescent()) / 2));
                g.setColor(outline);
                g.setStroke(new java.awt.BasicStroke(3, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g.draw(shape);
                g.setColor(fill);
                g.fill(shape);
                g.dispose();
            }
        };
    }

    private static BufferedImage buttonArt(String text) {
        String file = switch (text) {
            case "방만들기" -> "create-room";
            case "바로시작" -> "quick-join";
            default -> null;
        };
        if (file == null) {
            return null;
        }
        try (var input = LobbyArtwork.class.getResourceAsStream("/ui/" + file + ".png")) {
            if (input == null) {
                throw new IllegalStateException("버튼 이미지가 없습니다: " + file);
            }
            return javax.imageio.ImageIO.read(input);
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    static BufferedImage map(String name) {
        return MAPS.computeIfAbsent(name, key -> {
            String id = switch (key) {
                case "캠프 08" -> "camp/camp08";
                case "패트릿 14" -> "patrit/patrit14";
                default -> "village/village10";
            };
            TileMap map = MapLoader.loadResource(id);
            Tileset tiles = Tileset.forTheme(map.theme());
            BufferedImage image = new BufferedImage(map.pixelWidth(), map.pixelHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(new Color(0x87B44B));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            int size = TileMap.TILE_SIZE;
            for (int y = 0; y < map.rows(); y++) {
                for (int x = 0; x < map.columns(); x++) {
                    g.drawImage(tiles.floor(x, y), x * size, y * size, null);
                }
            }
            for (int y = 0; y < map.rows(); y++) {
                for (int x = 0; x < map.columns(); x++) {
                    TileSprite sprite = tiles.sprite(map.tileAt(x, y), x, y);
                    if (sprite != null && map.isTileOrigin(x, y)) {
                        g.drawImage(sprite.image(), x * size + sprite.offsetX(), y * size + sprite.offsetY(), null);
                    }
                }
            }
            g.dispose();
            return image;
        });
    }

    public static JButton button(String text, boolean gold) {
        BufferedImage art = buttonArt(text);
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                if (art != null) {
                    g.drawImage(art, 0, 0, getWidth(), getHeight(), null);
                    if (getModel().isPressed() || !isEnabled()) {
                        g.setColor(new Color(0, 0, 0, 60));
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                    if (isFocusOwner()) {
                        g.setColor(Color.WHITE);
                        g.drawRect(3, 3, getWidth() - 7, getHeight() - 7);
                    }
                    g.dispose();
                    return;
                }
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = gold ? new Color(0xFFF5AB) : new Color(0x9FE8FF);
                Color bottom = gold ? new Color(0xE99B16) : new Color(0x2575CE);
                if (text.equals("메뉴")) {
                    top = new Color(0x93F43C);
                    bottom = new Color(0x249619);
                } else if (text.equals("나가기") || text.equals("종료")) {
                    top = new Color(0xFFA26C);
                    bottom = new Color(0xC6411B);
                }
                if (getModel().isPressed()) {
                    top = bottom;
                }
                g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                g.fillRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 12, 12);
                g.setColor(new Color(0x163E67));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                g.setColor(isFocusOwner() ? Color.WHITE : new Color(255, 255, 255, 170));
                g.drawRoundRect(4, 4, getWidth() - 9, getHeight() - 9, 9, 9);
                if (!isEnabled()) {
                    g.setColor(new Color(30, 50, 70, 95));
                    g.fillRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 12, 12);
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setMargin(new java.awt.Insets(2, 8, 2, 8));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setForeground(gold ? new Color(0x743400) : Color.WHITE);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, gold ? 25 : 14));
        button.addActionListener(event -> hansung.arcade.outgame.audio.Sound.click());
        return button;
    }
}

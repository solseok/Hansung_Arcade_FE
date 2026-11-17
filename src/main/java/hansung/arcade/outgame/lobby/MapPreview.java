package hansung.arcade.outgame.lobby;

import hansung.arcade.outgame.ui.LobbyLayout;
import hansung.arcade.outgame.ui.OutgameFonts;
import hansung.arcade.outgame.ui.OutgamePalette;
import hansung.arcade.outgame.ui.Shapes;
import hansung.arcade.outgame.ui.TextLabel;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 맵 미리보기. 맵 그림을 꽉 채워 부드럽게 줄여 그리고, 아래 띠(높이의 22%)에 맵 이름을 쓴다.
 * 그림이 없는 스켈레톤은 미리보기 색과 띠 색만 칠한다. 모서리는 매끈하게 깎고 가장자리에 얇은 테두리를 두른다.
 */
public final class MapPreview extends JComponent {

    private final BufferedImage source;
    private final int radius;
    private final TextLabel name;
    private BufferedImage thumbnail;
    private BufferedImage rendered;

    /** @param source 맵 그림. 스켈레톤이면 null */
    public MapPreview(String mapName, BufferedImage source, int radius) {
        this.source = source;
        this.radius = radius;
        setName("map-preview");
        setLayout(null);
        name = new TextLabel(mapName == null ? "" : mapName, SwingConstants.CENTER);
        name.setName("map-name");
        name.setFont(OutgameFonts.label(11));
        name.setForeground(Color.WHITE);
        name.setOutline(OutgamePalette.MAP_PREVIEW_EDGE, 1);
        add(name);
    }

    public static MapPreview skeleton(int radius) {
        return new MapPreview(null, null, radius);
    }

    public int radius() {
        return radius;
    }

    public int stripHeight() {
        return LobbyLayout.stripHeight(getHeight());
    }

    /** 미리보기 크기로 맞춘 맵 그림(띠·모서리 전). 스켈레톤이거나 크기가 없으면 null */
    public BufferedImage thumbnail() {
        if (source == null || getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }
        if (thumbnail == null || thumbnail.getWidth() != getWidth() || thumbnail.getHeight() != getHeight()) {
            thumbnail = Shapes.cover(source, getWidth(), getHeight());
            rendered = null;
        }
        return thumbnail;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        int strip = LobbyLayout.stripHeight(height);
        name.setBounds(0, height - strip, width, strip);
        rendered = null;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (rendered == null || rendered.getWidth() != w || rendered.getHeight() != h) {
            BufferedImage map = thumbnail();
            int strip = LobbyLayout.stripHeight(h);
            rendered = Shapes.paint(w, h, m -> m.fill(Shapes.round(0, 0, w, h, radius)),
                    OutgamePalette.MAP_PREVIEW, d -> {
                        if (map != null) {
                            d.drawImage(map, 0, 0, null);
                            d.setColor(OutgamePalette.MAP_NAME_STRIP_OVERLAY);
                        } else {
                            d.setColor(OutgamePalette.MAP_NAME_STRIP);
                        }
                        d.fillRect(0, h - strip, w, strip);
                        Shapes.edge(d, w, h, radius, OutgamePalette.MAP_PREVIEW_EDGE, 1f);
                    });
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.drawImage(rendered, 0, 0, null);
        } finally {
            g.dispose();
        }
    }
}

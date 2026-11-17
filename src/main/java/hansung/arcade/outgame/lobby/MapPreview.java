package hansung.arcade.outgame.lobby;

import hansung.arcade.outgame.*;
import hansung.arcade.outgame.ui.*;

import java.awt.*;

import javax.swing.*;

public class MapPreview extends RoundPanel {
    private final String map;
    private final JLabel name;

    public MapPreview(String map) {
        super(OutgamePalette.MAP_PREVIEW, 4);
        this.map = map;
        name = new TextLabel(map == null ? "" : map);
        name.setName("map-name");
        name.setHorizontalAlignment(JLabel.CENTER);
        name.setFont(OutgameFonts.label(10));
        add(name);
    }

    public int stripHeight() {
        return Math.max(16, (int) (getHeight() * .22));
    }

    @Override
    public void doLayout() {
        name.setBounds(0, getHeight() - stripHeight(), getWidth(), stripHeight());
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(getBackground());
        g.fillRect(0, 0, w, h);
        if (map != null && !map.equals("랜덤 맵"))
            g.drawImage(LobbyArtwork.map(map), 0, 0, w, h - stripHeight(), null);
        else if (map != null) {
            g.setColor(Color.WHITE);
            g.setFont(OutgameFonts.label(40));
            g.drawString("?", w / 2 - 12, h / 2);
        }
        g.setColor(OutgamePalette.MAP_NAME_STRIP);
        g.fillRect(0, h - stripHeight(), w, stripHeight());
        g.setComposite(AlphaComposite.DstIn);
        Shapes.hints(g);
        java.awt.image.BufferedImage mask =
                new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D m = mask.createGraphics();
        Shapes.hints(m);
        m.setColor(Color.WHITE);
        m.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, 8, 8));
        m.dispose();
        g.drawImage(mask, 0, 0, null);
        g.dispose();
        graphics.drawImage(image, 0, 0, null);
    }
}

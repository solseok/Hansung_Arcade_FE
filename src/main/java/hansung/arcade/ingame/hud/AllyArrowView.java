package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.render.Concealment;
import hansung.arcade.ingame.render.WorldRenderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;

/** 이 컴퓨터의 참가자와 같은 편인 살아 있는 캐릭터를 팀 색 화살표로 표시한다. */
final class AllyArrowView implements HudPart {
    @Override
    public void paint(Graphics2D graphics, GameWorld world) {
        Concealment concealment = Concealment.of(world);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            for (Player player : world.players()) {
                if (!player.isAlive() || concealment.hides(player)
                        || world.speechBubbles().textOf(player.id()) != null
                        || world.localPlayers().stream().noneMatch(local -> world.match().isAlly(local, player))) {
                    continue;
                }
                int centerX = HudLayout.mapOriginX(world.map()) + (int) Math.round(player.x());
                int centerY = HudLayout.mapOriginY(world.map()) + (int) Math.round(player.y());
                int lift = WorldRenderer.figureLiftOf(player, world.gear(player));
                // 기존 1P·2P 글자 오른쪽에 붙여 두 표시를 함께 읽을 수 있게 한다.
                Box tag = PlayerTagView.place(14, 14, centerX, centerY, lift);
                int x = tag.x() + 20;
                int y = tag.y();
                Polygon arrow = new Polygon(
                        new int[] {x + 4, x + 10, x + 10, x + 14, x + 7, x, x + 4},
                        new int[] {y, y, y + 6, y + 6, y + 14, y + 6, y + 6}, 7);
                g.setColor(new Color(0x102E49));
                g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolygon(arrow);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolygon(arrow);
                var color = player.profile().color();
                g.setColor(color == null ? new Color(0xFFE65B) : new Color(color.rgb()));
                g.fillPolygon(arrow);
            }
        } finally {
            g.dispose();
        }
    }
}
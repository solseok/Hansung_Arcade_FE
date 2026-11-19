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
    private final hansung.arcade.ingame.render.CharacterSprites sprites = hansung.arcade.ingame.render.CharacterSprites.load();
    private final java.util.Map<java.awt.image.BufferedImage, Box> bounds = new java.util.IdentityHashMap<>();
    @Override
    public void paint(Graphics2D graphics, GameWorld world) {
        Concealment concealment = Concealment.of(world);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            for (Player player : world.players()) {
                if (!player.isAlive() || concealment.hides(player)
                        || world.localPlayers().stream().noneMatch(local -> world.match().isAlly(local, player))) {
                    continue;
                }
                int centerX = HudLayout.mapOriginX(world.map()) + (int) Math.round(player.x());
                int centerY = HudLayout.mapOriginY(world.map()) + (int) Math.round(player.y());
                int lift = WorldRenderer.figureLiftOf(player, world.gear(player));
                var profile = player.profile();
                var frame = switch (player.state()) {
                    case NORMAL -> sprites.frame(profile.mascot(), player.facing(), player.isMoving(), player.walkTime());
                    case TRAPPED -> sprites.trapped(profile.mascot(), null, player.stateTime());
                    case ESCAPING -> sprites.escaping(profile.mascot(), null, player.stateTime());
                    default -> sprites.dying(profile.mascot(), null, player.stateTime());
                };
                Box ink = bounds.computeIfAbsent(frame, PixelImages::opaqueBounds);
                int headY = centerY + WorldRenderer.FEET_BELOW_CENTER
                        - hansung.arcade.ingame.render.CharacterSprites.FEET_Y - lift + ink.y();
                int x = centerX - 7;
                int y = Math.max(2, headY - 18);
                Polygon arrow = new Polygon(
                        new int[] {x + 4, x + 10, x + 10, x + 14, x + 7, x, x + 4},
                        new int[] {y, y, y + 6, y + 6, y + 14, y + 6, y + 6}, 7);
                g.setColor(new Color(0x102E49));
                g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolygon(arrow);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolygon(arrow);
                var palette = hansung.arcade.ingame.entity.CharacterColor.values();
                var color = palette[Math.floorMod(player.profile().team() - 1, palette.length)];
                g.setColor(color == null ? new Color(0xFFE65B) : new Color(color.rgb()));
                g.fillPolygon(arrow);
            }
        } finally {
            g.dispose();
        }
    }
}
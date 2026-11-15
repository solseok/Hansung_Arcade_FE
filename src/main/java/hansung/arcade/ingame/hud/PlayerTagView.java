package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.render.WorldRenderer;

import java.awt.Graphics2D;
import java.util.List;

import static hansung.arcade.ingame.hud.HudLayout.CHAT_OUTLINE;
import static hansung.arcade.ingame.hud.HudLayout.MAP_LABEL;
import static hansung.arcade.ingame.hud.HudLayout.PLAYER_TAG_ABOVE_PLAYER;
import static hansung.arcade.ingame.hud.HudLayout.PLAYER_TAG_BELOW_PLAYER;

/**
 * 한 키보드 2인 모드에서 캐릭터 머리 위에 "1P", "2P"를 띄워 누가 누구인지 가린다. 혼자 할 때는 그리지 않는다.
 * 물풍선에 갇혀 있거나 빠져나오는 동안에도 따라다니고, 탈락하면 거둔다.
 * 글자는 채팅과 같은 도트 글자(흰 글자에 검은 1px 테두리)이고, 캐릭터를 따라다닌다.
 * 스프링 신발로 뛰거나 탱크에 타서 그림이 솟으면({@link WorldRenderer#figureLiftOf}) 표시도 그만큼 올라간다.
 *
 * <p>맨 윗줄에서는 머리 위에 자리가 없거나(창 밖) 맵 이름표에 가려지므로 발 아래에 띄운다.
 * 채팅 말풍선이 떠 있는 동안에는 반투명한 말풍선과 겹치지 않게 그 플레이어의 표시를 그리지 않는다.</p>
 */
final class PlayerTagView implements HudPart {

    private final OutlineText text;

    PlayerTagView(OutlineText text) {
        this.text = text;
    }

    @Override
    public void paint(Graphics2D g, GameWorld world) {
        List<Player> locals = world.localPlayers();
        if (locals.size() < 2) {
            return;
        }
        int originX = HudLayout.mapOriginX(world.map());
        int originY = HudLayout.mapOriginY(world.map());
        for (int i = 0; i < locals.size(); i++) {
            Player player = locals.get(i);
            // 탈락하면 쓰러져 솟았다 사라지는 연출만 보이게 표시를 거둔다
            if (!player.isAlive() || world.speechBubbles().textOf(player.id()) != null) {
                continue;
            }
            String tag = tagOf(i);
            Box place = place(text.width(tag), text.cellHeight(),
                    originX + (int) Math.round(player.x()), originY + (int) Math.round(player.y()),
                    WorldRenderer.figureLiftOf(player, world.gear(player)));
            text.draw(g, tag, place.x(), place.y());
        }
    }

    /** index번째(0부터) 플레이어의 표시. 0번째가 "1P"다 */
    static String tagOf(int index) {
        return (index + 1) + "P";
    }

    /**
     * 화면 좌표 (playerX, playerY)에 있고 그림이 땅에서 lift px 솟은 캐릭터의 표시 글자 칸 자리. 가로는 캐릭터 가운데에 맞춘다.
     * 세로는 머리 위({@link HudLayout#PLAYER_TAG_ABOVE_PLAYER})에서 lift만큼 더 올리되, 땅에 서 있을 때의 머리 위 자리가
     * 테두리째 창 위로 나가거나 맵 이름표에 닿으면 발 아래({@link HudLayout#PLAYER_TAG_BELOW_PLAYER})로 옮긴다.
     *
     * <p>위·아래는 땅에 서 있을 때 자리로 정해서, 뛰는 동안 표시가 머리 위와 발 아래를 오가며 건너뛰지 않는다.
     * 솟은 머리를 따라 올라가다 테두리가 창 위 끝에 닿으면 거기서 멈춘다. 뛰는 잠깐 동안은 솟은 머리처럼
     * 맵 이름표 뒤로 지나가 가려질 수 있다. 발 아래 표시는 땅에 남는 그림자 밑이라 솟아도 그대로 둔다.</p>
     */
    static Box place(int width, int height, int playerX, int playerY, int lift) {
        int x = playerX - width / 2;
        Box standing = new Box(x, playerY - PLAYER_TAG_ABOVE_PLAYER - height, width, height);
        Box outlined = standing.inset(-CHAT_OUTLINE);
        if (outlined.y() < 0 || overlaps(outlined, MAP_LABEL)) {
            return new Box(x, playerY + PLAYER_TAG_BELOW_PLAYER, width, height);
        }
        return new Box(x, Math.max(CHAT_OUTLINE, standing.y() - lift), width, height);
    }

    private static boolean overlaps(Box a, Box b) {
        return a.x() < b.right() && b.x() < a.right() && a.y() < b.bottom() && b.y() < a.bottom();
    }
}

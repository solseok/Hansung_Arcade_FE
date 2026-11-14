package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.entity.Player;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;

import static hansung.arcade.ingame.hud.HudLayout.EDGE;
import static hansung.arcade.ingame.hud.HudLayout.PLAYER_LIST;
import static hansung.arcade.ingame.hud.HudLayout.PLAYER_SLOTS;
import static hansung.arcade.ingame.hud.HudLayout.SIDE_RADIUS;
import static hansung.arcade.ingame.hud.HudLayout.TIMER;

/**
 * 오른쪽 플레이어 목록. 플레이어 칸들 맨 위의 TIMER 박스에 경과 시간을, 그 아래 8칸에
 * 플레이어 번호 순으로 캐릭터 얼굴과 닉네임을 그린다. 칸마다 왼쪽에 얼굴 틀이 있고(칸 안에서 세로 가운데),
 * 닉네임은 얼굴 틀 오른쪽에 칸 높이 기준 세로 가운데로 쓴다.
 */
final class PlayerList implements HudPart {

    /** 99:59 를 넘으면 더 올라가지 않는다 */
    private static final int MAX_SECONDS = 99 * 60 + 59;
    /** 얼굴 틀과 닉네임 사이 */
    private static final int NICKNAME_GAP = 3;

    private final HudSkin skin;

    PlayerList(HudSkin skin) {
        this.skin = skin;
    }

    @Override
    public void paintBase(PixelCanvas canvas) {
        Corners corners = Corners.all(SIDE_RADIUS);
        canvas.fillOutlined(PLAYER_LIST, corners, EDGE, HudPalette.edgeOf(HudPalette.PLAYER_LIST), HudPalette.PLAYER_LIST);
        canvas.fillOutlined(TIMER, Corners.all(HudLayout.TIMER_RADIUS), EDGE, HudPalette.edgeOf(HudPalette.DISPLAY),
                HudPalette.displayGrid(TIMER.x(), TIMER.y()));
        for (int i = 0; i < PLAYER_SLOTS; i++) {
            canvas.fillOutlined(HudLayout.playerSlot(i), Corners.all(HudLayout.PLAYER_SLOT_RADIUS), EDGE,
                    HudPalette.edgeOf(HudPalette.PLAYER_SLOT), HudPalette.PLAYER_SLOT);
            canvas.fillOutlined(HudLayout.portraitBox(i), Corners.all(2), EDGE,
                    HudPalette.edgeOf(HudPalette.PORTRAIT_BOX), HudPalette.PORTRAIT_BOX);
        }
        Graphics2D g = canvas.graphics();
        try {
            skin.timerLabel().draw(g, "TIMER", HudLayout.TIMER_LABEL, TextArt.Align.LEFT);
        } finally {
            g.dispose();
        }
    }

    @Override
    public void paint(Graphics2D g, GameWorld world) {
        // 분의 앞자리는 거의 늘 0이라 왼쪽 정렬이면 숫자가 바뀌어도 시작 위치가 그대로다
        skin.digits().draw(g, formatTime(world.elapsedSeconds()), HudLayout.TIMER_DIGITS, TextArt.Align.LEFT);

        List<Player> players = world.players().stream().sorted(Comparator.comparingInt(Player::id)).toList();
        for (int i = 0; i < Math.min(players.size(), PLAYER_SLOTS); i++) {
            paintPlayer(g, players.get(i), HudLayout.playerSlot(i), HudLayout.portraitBox(i));
        }
    }

    private void paintPlayer(Graphics2D g, Player player, Box slot, Box portraitBox) {
        BufferedImage portrait = skin.portraits().get(player.profile().mascot());
        if (portrait != null) {
            // 얼굴 틀 안쪽 가운데, 아래쪽(어깨)은 틀 바닥에 맞춘다
            Box inside = portraitBox.inset(EDGE);
            g.drawImage(portrait, inside.x() + (inside.width() - portrait.getWidth()) / 2,
                    inside.bottom() - portrait.getHeight(), null);
        }
        // 닉네임은 얼굴 틀 오른쪽, 칸 높이 기준 세로 가운데
        Box nameBox = new Box(portraitBox.right() + NICKNAME_GAP, slot.y(),
                slot.right() - EDGE - (portraitBox.right() + NICKNAME_GAP), slot.height());
        // 긴 닉네임이 칸 밖으로 나가지 않게 자른다
        Shape clip = g.getClip();
        g.clipRect(nameBox.x(), nameBox.y(), nameBox.width(), nameBox.height());
        skin.nickname().draw(g, player.profile().nickname(), nameBox, TextArt.Align.LEFT);
        g.setClip(clip);
    }

    /** 초를 MM:ss 로. 99:59 에서 멈춘다. */
    static String formatTime(double seconds) {
        int total = Math.min(MAX_SECONDS, Math.max(0, (int) seconds));
        return "%02d:%02d".formatted(total / 60, total % 60);
    }
}

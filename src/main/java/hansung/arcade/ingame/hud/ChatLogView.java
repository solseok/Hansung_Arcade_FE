package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.GameWorld;
import hansung.arcade.ingame.chat.ChatMessage;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;

import static hansung.arcade.ingame.hud.HudLayout.CHAT_BAR;
import static hansung.arcade.ingame.hud.HudLayout.CHAT_LINE_PITCH;
import static hansung.arcade.ingame.hud.HudLayout.CHAT_LOG_BOTTOM;
import static hansung.arcade.ingame.hud.HudLayout.CHAT_TEXT_HEIGHT;
import static hansung.arcade.ingame.hud.HudLayout.FIELD;

/**
 * 맵 위에 올라온 채팅 목록. 배경 없이 글자만 그린다. 가장 새 줄을 {@link HudLayout#CHAT_LOG_BOTTOM}에 놓고
 * 먼저 온 줄을 그 위로 쌓으며, 왼쪽 끝은 채팅 입력 바와 같다. 긴 줄은 맵 영역 오른쪽 끝에서 잘린다.
 *
 * <p>다른 플레이어의 줄은 "닉네임 : 내용", 내가 친 줄은 앞에 "> "를 붙이고 색을 달리해서 구분한다.</p>
 */
final class ChatLogView implements HudPart {

    private final OutlineText othersText;
    private final OutlineText myText;

    ChatLogView(OutlineText othersText, OutlineText myText) {
        this.othersText = othersText;
        this.myText = myText;
    }

    /** 채팅 목록에 올리는 한 줄의 모양 */
    static String line(ChatMessage message, boolean mine) {
        return (mine ? "> " : "") + message.sender() + " : " + message.text();
    }

    @Override
    public void paint(Graphics2D g, GameWorld world) {
        List<ChatMessage> messages = world.chatLog().messages();
        if (messages.isEmpty()) {
            return;
        }
        int myId = world.localPlayer().id();
        Shape clip = g.getClip();
        g.clipRect(FIELD.x(), FIELD.y(), FIELD.width(), FIELD.height());
        int top = CHAT_LOG_BOTTOM - CHAT_TEXT_HEIGHT;
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            boolean mine = message.senderId() == myId;
            (mine ? myText : othersText).draw(g, line(message, mine), CHAT_BAR.x(), top);
            top -= CHAT_LINE_PITCH;
        }
        g.setClip(clip);
    }
}

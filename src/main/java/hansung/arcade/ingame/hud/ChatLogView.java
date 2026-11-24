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
    /** {@link #lines}를 만든 채팅 목록. 목록이 바뀌었는지 가릴 때 쓴다 */
    private List<ChatMessage> shown = List.of();
    /** 지금 그리는 줄들. 먼저 온 줄이 앞이다 */
    private List<Line> lines = List.of();

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
        if (messages != shown) {
            // ChatLog는 줄이 올라오거나 내려갈 때만 새 목록을 주므로, 그때만 줄 글을 다시 만든다
            int myId = world.localPlayer().id();
            lines = messages.stream().map(message -> Line.of(message, message.senderId() == myId)).toList();
            shown = messages;
        }
        Shape clip = g.getClip();
        g.clipRect(FIELD.x(), FIELD.y(), FIELD.width(), FIELD.height());
        int top = CHAT_LOG_BOTTOM - CHAT_TEXT_HEIGHT;
        for (int i = lines.size() - 1; i >= 0; i--) {
            Line line = lines.get(i);
            (line.mine() ? myText : othersText).draw(g, line.text(), CHAT_BAR.x(), top);
            top -= CHAT_LINE_PITCH;
        }
        g.setClip(clip);
    }

    /** 화면에 그리는 한 줄: 글과, 내가 친 줄인지 */
    private record Line(String text, boolean mine) {

        static Line of(ChatMessage message, boolean mine) {
            return new Line(line(message, mine), mine);
        }
    }
}

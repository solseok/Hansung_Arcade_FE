package hansung.arcade.ingame;

import hansung.arcade.ingame.chat.ChatLog;
import hansung.arcade.ingame.chat.ChatMessage;
import hansung.arcade.ingame.chat.SpeechBubbles;
import hansung.arcade.ingame.entity.Direction;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.map.TileMap;

import java.awt.Point;
import java.util.List;

/**
 * 인게임 상태(맵 + 플레이어 + 올라온 채팅). Swing에 의존하지 않아서 테스트와 이후 네트워크 동기화에 그대로 쓸 수 있다.
 */
public final class GameWorld {

    private final TileMap map;
    private final Player localPlayer;
    private final ChatLog chatLog = new ChatLog();
    private final SpeechBubbles speechBubbles = new SpeechBubbles();
    private double elapsedSeconds;

    /** @param localProfile 이 컴퓨터에서 조작하는 플레이어의 닉네임·캐릭터 */
    public GameWorld(TileMap map, PlayerProfile localProfile) {
        this.map = map;
        Point spawn = map.spawnPoints().get(0);
        this.localPlayer = new Player(1, localProfile, spawn.x, spawn.y);
    }

    public void update(double dt, Direction localInput) {
        elapsedSeconds += dt;
        localPlayer.update(localInput, dt, map);
        chatLog.expire(elapsedSeconds);
        speechBubbles.expire(elapsedSeconds);
    }

    /**
     * 내가 친 채팅을 올린다. {@link ChatMessage#clean 한 줄로 다듬고} 내용이 없으면 올리지 않는다.
     * 지금은 서버가 없어 바로 내 화면에 올린다. 서버가 붙으면 여기서 서버로 보내고,
     * 서버가 돌려준 채팅(나와 다른 플레이어 모두)을 {@link #receiveChat}으로 올린다.
     */
    public void sendChat(String text) {
        String content = ChatMessage.clean(text);
        if (!content.isEmpty()) {
            receiveChat(new ChatMessage(localPlayer.id(), localPlayer.profile().nickname(), content));
        }
    }

    /**
     * 누군가의 채팅을 채팅 목록에 올리고, 친 플레이어 머리 위에 말풍선으로 띄운다.
     * 다른 상태와 마찬가지로 게임 루프가 도는 스레드에서만 부른다. 네트워크 스레드에서 받은 채팅은 그 스레드로 넘겨서 올린다.
     */
    public void receiveChat(ChatMessage message) {
        chatLog.add(message, elapsedSeconds);
        speechBubbles.show(message, elapsedSeconds);
    }

    public ChatLog chatLog() {
        return chatLog;
    }

    public SpeechBubbles speechBubbles() {
        return speechBubbles;
    }

    public TileMap map() {
        return map;
    }

    public Player localPlayer() {
        return localPlayer;
    }

    public List<Player> players() {
        return List.of(localPlayer);
    }

    public double elapsedSeconds() {
        return elapsedSeconds;
    }
}

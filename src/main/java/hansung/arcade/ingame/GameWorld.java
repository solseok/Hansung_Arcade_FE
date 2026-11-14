package hansung.arcade.ingame;

import hansung.arcade.ingame.chat.ChatLog;
import hansung.arcade.ingame.chat.ChatMessage;
import hansung.arcade.ingame.chat.SpeechBubbles;
import hansung.arcade.ingame.entity.Player;
import hansung.arcade.ingame.entity.PlayerProfile;
import hansung.arcade.ingame.input.PlayerCommand;
import hansung.arcade.ingame.map.TileMap;
import hansung.arcade.ingame.match.Match;
import hansung.arcade.ingame.match.MatchMode;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 인게임 상태(맵 + 플레이어 + 올라온 채팅 + 경기 진행). Swing에 의존하지 않아서 테스트와 이후 네트워크 동기화에 그대로 쓸 수 있다.
 *
 * <p>경기({@link Match})가 끝나면 이동 입력은 더 받지 않지만, 시간({@link #elapsedSeconds()})은 계속 흘러서
 * 채팅이 사라지고 이후 붙을 연출(탈락, 물줄기가 사라지는 모습 등)도 이어진다.</p>
 */
public final class GameWorld {

    private final TileMap map;
    /** 이 컴퓨터의 첫 플레이어(1P). 채팅은 이 플레이어가 친 것으로 올라간다 */
    private final Player localPlayer;
    /** 이 컴퓨터에서 조작하는 플레이어들. 1P, 2P 순서 */
    private final List<Player> localPlayers;
    private final ChatLog chatLog = new ChatLog();
    private final SpeechBubbles speechBubbles = new SpeechBubbles();
    private final Match match;
    private double elapsedSeconds;

    /** 개인전으로 시작한다. */
    public GameWorld(TileMap map, PlayerProfile localProfile) {
        this(map, List.of(localProfile), MatchMode.SOLO);
    }

    /** @param localProfile 이 컴퓨터에서 조작하는 플레이어의 닉네임·캐릭터·팀 */
    public GameWorld(TileMap map, PlayerProfile localProfile, MatchMode mode) {
        this(map, List.of(localProfile), mode);
    }

    /** 한 컴퓨터에서 여럿이 하는 개인전. */
    public GameWorld(TileMap map, List<PlayerProfile> localProfiles) {
        this(map, localProfiles, MatchMode.SOLO);
    }

    /**
     * 한 컴퓨터에서 여럿이 하는 게임(한 키보드 2인 모드).
     *
     * @param localProfiles 이 컴퓨터에서 조작하는 플레이어들의 닉네임·캐릭터·팀. 1P, 2P 순서이고,
     *                      n번째(1부터) 플레이어는 번호 n을 받아 맵의 n번 스폰에서 시작한다
     * @param mode          개인전·팀전
     */
    public GameWorld(TileMap map, List<PlayerProfile> localProfiles, MatchMode mode) {
        List<Point> spawns = map.spawnPoints();
        if (localProfiles.isEmpty()) {
            throw new IllegalArgumentException("플레이어가 한 명은 있어야 합니다.");
        }
        if (localProfiles.size() > spawns.size()) {
            throw new IllegalArgumentException("맵 %s에는 스폰이 %d곳뿐이라 %d명이 시작할 수 없습니다."
                    .formatted(map.name(), spawns.size(), localProfiles.size()));
        }
        this.map = map;
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < localProfiles.size(); i++) {
            Point spawn = spawns.get(i);
            players.add(new Player(i + 1, localProfiles.get(i), spawn.x, spawn.y));
        }
        this.localPlayers = List.copyOf(players);
        this.localPlayer = localPlayers.get(0);
        // 플레이어를 모두 만든 뒤에 경기를 연다 (경기 중에 플레이어를 더하지 않는다)
        this.match = new Match(mode, players());
    }

    /**
     * 게임을 dt초(한 틱) 진행한다.
     *
     * @param commands 플레이어 번호마다 이번 틱에 할 일. 없는 플레이어는 아무것도 하지 않는다 ({@link PlayerCommand#IDLE})
     */
    public void update(double dt, Map<Integer, PlayerCommand> commands) {
        elapsedSeconds += dt;
        for (Player player : players()) {
            // 경기가 끝나면 누르고 있어도 멈춰 선다
            PlayerCommand command = match.isOver() ? PlayerCommand.IDLE
                    : commands.getOrDefault(player.id(), PlayerCommand.IDLE);
            player.update(command.move(), dt, map);
            // 막 눌린 물풍선(command.placeBalloon())·아이템(command.useItem()) 키는 아직 아무 효과가 없다
        }
        // 이 틱의 탈락·kill·save를 모두 반영한 뒤에 시계를 흘리고 승패를 가린다
        match.update(dt);
        chatLog.expire(elapsedSeconds);
        speechBubbles.expire(elapsedSeconds);
    }

    /**
     * 내가 친 채팅을 올린다. 지금은 서버가 없어 바로 내 화면에 올린다. 서버가 붙으면 여기서 서버로 보내고,
     * 서버가 돌려준 채팅(나와 다른 플레이어 모두)을 {@link #receiveChat}으로 올린다.
     */
    public void sendChat(String text) {
        receiveChat(new ChatMessage(localPlayer.id(), localPlayer.profile().nickname(), text));
    }

    /**
     * 누군가의 채팅을 채팅 목록에 올리고, 친 플레이어 머리 위에 말풍선으로 띄운다.
     * 내용은 {@link ChatMessage#clean 한 줄로 다듬어} 올리고, 다듬고 나서 비어 있으면 올리지 않는다.
     * 내가 친 글뿐 아니라 서버가 보내 준 글도 같은 규칙을 거친다 (빈 말풍선이나 화면을 덮는 긴 글을 막는다).
     *
     * <p>다른 상태와 마찬가지로 게임 루프가 도는 스레드에서만 부른다. 네트워크 스레드에서 받은 채팅은 그 스레드로 넘겨서 올린다.</p>
     */
    public void receiveChat(ChatMessage message) {
        String content = ChatMessage.clean(message.text());
        if (content.isEmpty()) {
            return;
        }
        ChatMessage shown = content.equals(message.text())
                ? message
                : new ChatMessage(message.senderId(), message.sender(), content);
        chatLog.add(shown, elapsedSeconds);
        speechBubbles.show(shown, elapsedSeconds);
    }

    public ChatLog chatLog() {
        return chatLog;
    }

    public SpeechBubbles speechBubbles() {
        return speechBubbles;
    }

    /** 이번 판의 경기 시계·kill·save·승패 */
    public Match match() {
        return match;
    }

    public TileMap map() {
        return map;
    }

    /** 이 컴퓨터의 첫 플레이어(1P). 한 키보드 2인 모드에서도 채팅은 1P가 친 것으로 올라간다. */
    public Player localPlayer() {
        return localPlayer;
    }

    /** 이 컴퓨터에서 조작하는 플레이어들. 1P, 2P 순서. 혼자 하면 1P 한 명이다. */
    public List<Player> localPlayers() {
        return localPlayers;
    }

    /** 맵 위의 모든 플레이어. 지금은 이 컴퓨터의 플레이어뿐이다. */
    public List<Player> players() {
        return localPlayers;
    }

    /** 게임을 시작한 뒤 흐른 시간(초). 경기가 끝나도 계속 흐른다. 경기의 남은 시간은 {@link Match#clock()}을 본다 */
    public double elapsedSeconds() {
        return elapsedSeconds;
    }
}

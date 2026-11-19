package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.outgame.lobby.LobbyService;
import hansung.arcade.outgame.lobby.Room;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 한 컴퓨터의 오프라인 대기실 상태. 1P면 한 명, 2P면 두 명이 이 컴퓨터에서 함께 한다.
 * 다른 인원은 준비된 목업 참가자다. 서버 주소·포트는 사용자가 넣지 않고 설정(application.properties)에서 읽는다.
 */
public final class OutgameSession {
    public final LobbyService service;
    private List<String> nicknames = List.of("나");
    private Room room;
    private Mascot character = Mascot.DEFAULT;
    /** ?(랜덤)를 골랐으면 true. 실제 캐릭터는 게임을 시작할 때 정한다 */
    private boolean randomCharacter;
    private final Random random = new Random();
    private Team team = Team.RED;
    private String mapId = GameMaps.DEFAULT_ID;
    private boolean ready;
    /** 목업 참가자(이 컴퓨터 밖의 사람)의 준비 상태. 서버가 붙으면 서버에서 받은 값으로 바꾼다 */
    private final Map<Integer, Boolean> participantReady = new HashMap<>();

    public OutgameSession(LobbyService service) {
        this.service = service;
    }

    /**
     * 1P는 닉네임 하나, 2P는 둘로 로그인한다. 닉네임은 {@link NicknameRule}을 따르고, 2P의 두 닉네임은 달라야 한다.
     */
    public void login(String... nicknames) {
        if (nicknames.length < 1 || nicknames.length > 2) {
            throw new IllegalArgumentException("1P 또는 2P로만 로그인할 수 있습니다.");
        }
        List<String> valid = new ArrayList<>();
        for (String nickname : nicknames) {
            valid.add(NicknameRule.validate(nickname));
        }
        if (valid.size() == 2 && valid.get(0).equals(valid.get(1))) {
            throw new IllegalArgumentException("1P와 2P 닉네임이 같습니다. 서로 다르게 입력해 주세요.");
        }
        this.nicknames = List.copyOf(valid);
    }

    /** 이 컴퓨터의 플레이어 닉네임. 1P부터 */
    public List<String> nicknames() { return nicknames; }

    /** 1P 닉네임 */
    public String nickname() { return nicknames.getFirst(); }

    /** 이 컴퓨터에서 함께 하는 사람 수 (1P면 1, 2P면 2) */
    public int localPlayers() { return nicknames.size(); }

    public boolean isHost() { return service.isHost(); }

    /** 방에 들어간다. 2P면 두 자리를 차지한다. */
    public Room join(int roomNumber) {
        Room joined = service.join(roomNumber, localPlayers());
        enter(joined);
        return joined;
    }

    public Room create(String title, int capacity) {
        Room created = service.create(title, capacity, localPlayers());
        enter(created);
        return created;
    }

    public Room quickJoin() {
        Room joined = service.quickJoin(localPlayers());
        enter(joined);
        return joined;
    }

    /** 방에 들어온 상태로 맞춘다. 목업 참가자는 모두 준비한 채로 시작한다. */
    public void enter(Room room) {
        this.room = room;
        ready = false;
        participantReady.clear();
        for (int id = localPlayers() + 1; id <= room.players(); id++) {
            participantReady.put(id, true);
        }
        mapId = GameMaps.byName(room.map()).id();
    }

    public Room room() { return room; }

    public boolean isSlotClosed(int index) {
        return service.isSlotClosed(index, room);
    }

    public void toggleSlot(int index) {
        room = service.toggleSlot(index);
    }
    /** 고른 캐릭터. 랜덤을 골랐으면 마지막으로 고른 캐릭터이고, 실제 캐릭터는 {@link #start}에서 정한다 */
    public Mascot character() { return character; }
    public boolean randomCharacter() { return randomCharacter; }
    public Team team() { return team; }
    public String mapId() { return mapId; }
    public boolean ready() { return ready; }
    public void setReady(boolean value) { ready = value; }

    /** 캐릭터를 바꾼다. 바꾸면 준비가 풀리고 랜덤도 풀린다. */
    public void select(Mascot character) {
        this.character = character;
        randomCharacter = false;
        ready = false;
    }

    /** ?(랜덤 캐릭터)를 고른다. 게임을 시작할 때 다섯 캐릭터 중 하나로 정한다 */
    public void selectRandomCharacter() {
        randomCharacter = true;
        ready = false;
    }

    /** 팀을 바꾼다. 바꾸면 준비가 풀린다. */
    public void selectTeam(Team team) {
        this.team = team;
        ready = false;
    }

    /**
     * 방장이 맵을 고른다(랜덤 포함). 맵 최대 인원보다 사람이 많으면 고를 수 없고, 고르면 방 정원이 그 맵에 맞게 바뀐다.
     */
    public void selectMap(String mapId) {
        if (!service.isHost()) {
            throw new IllegalStateException("방장만 맵을 선택할 수 있습니다.");
        }
        GameMaps.GameMap map;
        try {
            map = GameMaps.byId(mapId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 맵입니다.", e);
        }
        if (room != null && room.players() > map.maxPlayers()) {
            throw new IllegalStateException("%s은(는) 최대 %d명이라 지금 인원 %d명으로는 고를 수 없습니다."
                    .formatted(map.name(), map.maxPlayers(), room.players()));
        }
        if (room != null) {
            room = service.updateRoom(map.name(), Room.Status.WAITING);
        }
        this.mapId = mapId;
    }

    /** 방장 자리의 순서. 내가 방장이면 0(1P), 아니면 내 다음 첫 목업 참가자다. */
    public int hostIndex() {
        return service.isHost() ? 0 : localPlayers();
    }

    /** 방 인원. 이 컴퓨터의 플레이어가 앞에, 목업 참가자가 뒤에 온다. */
    public List<GameStartInfo.PlayerInfo> players() {
        List<GameStartInfo.PlayerInfo> players = new ArrayList<>();
        for (int i = 0; i < nicknames.size(); i++) {
            players.add(new GameStartInfo.PlayerInfo(i + 1, nicknames.get(i), character, team.label()));
        }
        for (int id = nicknames.size() + 1; id <= room.players(); id++) {
            players.add(new GameStartInfo.PlayerInfo(id, "목업 " + id,
                    Mascot.values()[(id - 1) % Mascot.values().length],
                    (id % 2 == 0 ? Team.BLUE : Team.RED).label()));
        }
        return List.copyOf(players);
    }

    /** 목업 참가자의 준비 상태를 바꾼다. 서버가 붙으면 서버에서 받은 준비 상태도 이 메서드로 반영한다. */
    public void setParticipantReady(int playerId, boolean value) {
        if (!participantReady.containsKey(playerId)) {
            throw new IllegalArgumentException("방에 없는 참가자입니다: " + playerId);
        }
        participantReady.put(playerId, value);
    }

    /** playerId번 사람이 준비했는지. 이 컴퓨터의 플레이어는 함께 준비한다 */
    public boolean isReady(int playerId) {
        return playerId <= localPlayers() ? ready : participantReady.getOrDefault(playerId, false);
    }

    /** 방장이고, 방이 대기 중이고, 다른 참가자가 모두 준비했으면 시작할 수 있다 */
    public boolean canStart() {
        return room != null
                && service.isHost()
                && room.status() == Room.Status.WAITING
                && participantReady.values().stream().allMatch(Boolean::booleanValue);
    }

    /** 경기가 끝나 대기방으로 돌아온다. 방장이면 방을 다시 대기 중으로 돌리고, 준비는 풀린다 */
    public void returnToWaiting() {
        if (room != null && service.isHost()) {
            room = service.updateRoom(room.map(), Room.Status.WAITING);
        }
        ready = false;
    }

    /** 게임을 시작한다. 방은 게임 중이 되고, 랜덤 맵·랜덤 캐릭터는 여기서 실제 맵·캐릭터로 정한다 */
    public GameStartInfo start() {
        if (!service.isHost()) {
            throw new IllegalStateException("방장만 게임을 시작할 수 있습니다.");
        }
        if (!canStart()) {
            throw new IllegalStateException("모든 참가자가 준비해야 시작할 수 있습니다.");
        }
        List<GameStartInfo.PlayerInfo> players = new ArrayList<>(players());
        if (randomCharacter) {
            // 2P면 두 사람이 각자 따로 뽑는다
            for (int i = 0; i < localPlayers(); i++) {
                Mascot picked = Mascot.values()[random.nextInt(Mascot.values().length)];
                GameStartInfo.PlayerInfo local = players.get(i);
                players.set(i, new GameStartInfo.PlayerInfo(local.id(), local.nickname(), picked, local.team()));
            }
        }
        String map = GameMaps.RANDOM_ID.equals(mapId) ? GameMaps.pick(players.size(), random).id() : mapId;
        GameStartInfo info = new GameStartInfo(map, players, localPlayers());
        room = service.updateRoom(room.map(), Room.Status.PLAYING);
        return info;
    }

    public void leave() {
        service.leave();
        room = null;
        participantReady.clear();
        ready = false;
    }
}

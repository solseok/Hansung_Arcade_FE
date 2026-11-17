package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.outgame.lobby.LobbyService;
import hansung.arcade.outgame.lobby.Room;

import java.util.ArrayList;
import java.util.List;

/** 한 사용자의 오프라인 대기실 상태. 다른 인원은 준비된 목업 참가자다. */
public final class OutgameSession {
    public final LobbyService service;
    private String nickname = "나";
    private Room room;
    private Mascot character = Mascot.DEFAULT;
    private Team team = Team.RED;
    private List<String> nicknames = List.of("플레이어");
    private boolean randomCharacter;
    private int configuredCapacity;
    private String mapId = "camp/camp08";
    private boolean ready;
    private final java.util.Map<Integer, Boolean> participantReady = new java.util.HashMap<>();

    public OutgameSession(LobbyService service) {
        this.service = service;
    }

    public void login(String... names) {
        if (names.length < 1 || names.length > 2)
            throw new IllegalArgumentException("1~2명만 로그인할 수 있습니다.");
        List<String> validated =
                java.util.Arrays.stream(names).map(NicknameRule::validate).toList();
        if (validated.stream().distinct().count() != validated.size())
            throw new IllegalArgumentException("같은 닉네임은 사용할 수 없습니다.");
        nicknames = validated;
        nickname = validated.getFirst();
    }

    public List<String> nicknames() {
        return nicknames;
    }

    public int localPlayers() {
        return nicknames.size();
    }

    public Room join(int number) {
        Room joined = service.join(number, localPlayers());
        enter(joined);
        return joined;
    }

    public Room quickJoin() {
        Room joined = service.quickJoin(localPlayers());
        enter(joined);
        return joined;
    }

    public Room create(String title, int capacity) {
        Room created = service.create(title, capacity, localPlayers());
        enter(created);
        return created;
    }

    public void select(Mascot character) {
        this.character = character;
        randomCharacter = false;
        ready = false;
    }

    public void selectTeam(Team team) {
        this.team = team;
        ready = false;
    }

    public void selectRandomCharacter() {
        randomCharacter = true;
        ready = false;
    }

    public boolean randomCharacter() {
        return randomCharacter;
    }

    public void enter(Room room) {
        this.room = room;
        configuredCapacity = room.capacity();
        ready = false;
        participantReady.clear();
        for (int id = localPlayers() + 1; id <= room.players(); id++) {
            participantReady.put(id, true);
        }
        mapId =
                switch (room.map()) {
                    case "패트릿 14" -> "patrit/patrit14";
                    case "빌리지 10" -> "village/village10";
                    default -> "camp/camp08";
                };
    }

    public String nickname() {
        return nickname;
    }

    public Room room() {
        return room;
    }

    public Mascot character() {
        return character;
    }

    public Team team() {
        return team;
    }

    public String mapId() {
        return mapId;
    }

    public boolean ready() {
        return ready;
    }

    public void setReady(boolean value) {
        ready = value;
    }

    public void select(Mascot character, String team) {
        this.character = character;
        this.team = Team.byLabel(team);
        randomCharacter = false;
        ready = false;
    }

    public void selectMap(String mapId) {
        if (!service.isHost()) {
            throw new IllegalStateException("방장만 맵을 선택할 수 있습니다.");
        }
        GameMaps.MapInfo map = GameMaps.byId(mapId);
        int capacity = Math.min(configuredCapacity, map.maxPlayers());
        if (room.players() > capacity)
            throw new IllegalStateException("이 맵은 " + capacity + "명까지 입장할 수 있습니다.");
        room = service.updateRoom(map.label(), Room.Status.WAITING);
        this.mapId = mapId;
    }

    public List<GameStartInfo.PlayerInfo> players() {
        List<GameStartInfo.PlayerInfo> players = new ArrayList<>();
        for (int i = 0; i < localPlayers(); i++)
            players.add(
                    new GameStartInfo.PlayerInfo(i + 1, nicknames.get(i), character, team.label()));
        for (int id = localPlayers() + 1; id <= room.players(); id++) {
            players.add(
                    new GameStartInfo.PlayerInfo(
                            id,
                            "목업 " + id,
                            Mascot.values()[(id - 1) % Mascot.values().length],
                            id % 2 == 0 ? "파랑" : "빨강"));
        }
        return List.copyOf(players);
    }

    /** 서버 연결 후 수신한 준비 상태도 이 메서드로 반영한다. */
    public void setParticipantReady(int playerId, boolean value) {
        if (!participantReady.containsKey(playerId)) {
            throw new IllegalArgumentException("방에 없는 참가자입니다.");
        }
        participantReady.put(playerId, value);
    }

    public boolean isReady(int playerId) {
        return playerId <= localPlayers() ? ready : participantReady.getOrDefault(playerId, false);
    }

    public boolean canStart() {
        return room != null
                && service.isHost()
                && room.status() == Room.Status.WAITING
                && participantReady.values().stream().allMatch(Boolean::booleanValue);
    }

    public void returnToWaiting() {
        if (room != null && service.isHost()) {
            room = service.updateRoom(room.map(), Room.Status.WAITING);
        }
        ready = false;
    }

    public GameStartInfo start() {
        if (!service.isHost()) {
            throw new IllegalStateException("방장만 게임을 시작할 수 있습니다.");
        }
        if (!canStart()) {
            throw new IllegalStateException("모든 참가자가 준비해야 시작할 수 있습니다.");
        }
        String selectedMap = mapId;
        if (GameMaps.RANDOM_ID.equals(mapId)) {
            List<String> available =
                    GameMaps.ids().stream()
                            .filter(
                                    id ->
                                            !id.equals(GameMaps.RANDOM_ID)
                                                    && GameMaps.byId(id).maxPlayers()
                                                            >= room.players())
                            .toList();
            selectedMap =
                    available.get(
                            java.util.concurrent.ThreadLocalRandom.current()
                                    .nextInt(available.size()));
        }
        List<GameStartInfo.PlayerInfo> selectedPlayers = players();
        if (randomCharacter)
            selectedPlayers =
                    selectedPlayers.stream()
                            .map(
                                    p ->
                                            p.id() <= localPlayers()
                                                    ? new GameStartInfo.PlayerInfo(
                                                            p.id(),
                                                            p.nickname(),
                                                            Mascot.values()[
                                                                    java.util.concurrent
                                                                            .ThreadLocalRandom
                                                                            .current()
                                                                            .nextInt(
                                                                                    Mascot.values()
                                                                                            .length)],
                                                            p.team())
                                                    : p)
                            .toList();
        GameStartInfo info = new GameStartInfo(selectedMap, selectedPlayers, localPlayers());
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

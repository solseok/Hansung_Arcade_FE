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
    private String host = "localhost";
    private int port = 9000;
    private Room room;
    private Mascot character = Mascot.DEFAULT;
    private String team = "빨강";
    private String mapId = "camp/camp08";
    private boolean ready;

    public OutgameSession(LobbyService service) {
        this.service = service;
    }

    public void login(String nickname, String host, String port) {
        String name = nickname.strip();
        if (name.isEmpty() || name.length() > 12) {
            throw new IllegalArgumentException("닉네임은 1~12자로 입력해 주세요.");
        }
        if (host.isBlank()) {
            throw new IllegalArgumentException("서버 주소를 입력해 주세요.");
        }
        int number;
        try {
            number = Integer.parseInt(port.strip());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("포트는 1~65535 사이 숫자로 입력해 주세요.");
        }
        if (number < 1 || number > 65535) {
            throw new IllegalArgumentException("포트는 1~65535 사이 숫자로 입력해 주세요.");
        }
        this.nickname = name;
        this.host = host.strip();
        this.port = number;
    }

    public void enter(Room room) {
        this.room = room;
        ready = false;
        mapId = switch (room.map()) {
            case "패트릿 14" -> "patrit/patrit14";
            case "빌리지 10" -> "village/village10";
            default -> "camp/camp08";
        };
    }

    public String nickname() { return nickname; }
    public String host() { return host; }
    public int port() { return port; }
    public Room room() { return room; }
    public Mascot character() { return character; }
    public String team() { return team; }
    public String mapId() { return mapId; }
    public boolean ready() { return ready; }
    public void setReady(boolean value) { ready = value; }

    public void select(Mascot character, String team) {
        this.character = character;
        this.team = team;
        ready = false;
    }

    public void selectMap(String mapId) {
        if (!service.isHost()) {
            throw new IllegalStateException("방장만 맵을 선택할 수 있습니다.");
        }
        if (!List.of("camp/camp08", "patrit/patrit14", "village/village10").contains(mapId)) {
            throw new IllegalArgumentException("지원하지 않는 맵입니다.");
        }
        this.mapId = mapId;
    }

    public List<GameStartInfo.PlayerInfo> players() {
        List<GameStartInfo.PlayerInfo> players = new ArrayList<>();
        players.add(new GameStartInfo.PlayerInfo(1, nickname, character, team));
        for (int id = 2; id <= room.players(); id++) {
            players.add(new GameStartInfo.PlayerInfo(id, "목업 " + id,
                    Mascot.values()[(id - 1) % Mascot.values().length], id % 2 == 0 ? "파랑" : "빨강"));
        }
        return List.copyOf(players);
    }

    public GameStartInfo start() {
        if (!service.isHost()) {
            throw new IllegalStateException("방장만 게임을 시작할 수 있습니다.");
        }
        return new GameStartInfo(mapId, players());
    }

    public void leave() {
        service.leave();
        room = null;
        ready = false;
    }
}

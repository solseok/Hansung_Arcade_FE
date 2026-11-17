package hansung.arcade.outgame.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 로그인·로비·대기실은 각자 전용 BGM을 쓴다.
 * 곡은 sounds/01. login.mp3, 02. lobby.mp3, 03. room.mp3이고, 화면 이름은 SceneManager에 등록하는 이름이다.
 */
class BgmTest {
    private static final List<String> SCREENS = List.of("login", "lobby", "waiting");

    @Test
    void 로그인_화면은_login_곡을_튼다() {
        assertEquals("01. login", Sound.bgmFor("login"));
    }

    @Test
    void 로비는_lobby_곡을_튼다() {
        assertEquals("02. lobby", Sound.bgmFor("lobby"));
    }

    @Test
    void 대기실은_room_곡을_튼다() {
        assertEquals("03. room", Sound.bgmFor("waiting"));
    }

    @Test
    void 화면을_옮기면_곡이_바뀐다() {
        // playBgm은 같은 곡이면 다시 틀지 않으므로, 세 화면의 곡이 모두 달라야 화면을 옮길 때 곡이 바뀐다
        Set<String> songs = new HashSet<>();
        for (String screen : SCREENS) {
            songs.add(Sound.bgmFor(screen));
        }
        assertEquals(SCREENS.size(), songs.size(), "같은 곡을 쓰는 화면이 있습니다: " + songs);
    }

    @Test
    void 세_곡_모두_리소스에_있고_MP3로_읽힌다() throws Exception {
        for (String screen : SCREENS) {
            String key = Sound.bgmFor(screen);
            try (InputStream raw = Sound.class.getResourceAsStream("/sounds/" + key + ".mp3")) {
                assertNotNull(raw, screen + " 화면의 곡 파일이 없습니다: " + key);
                Bitstream bits = new Bitstream(new BufferedInputStream(raw));
                Header first = bits.readFrame();
                bits.close();
                assertNotNull(first, screen + " 화면의 곡을 MP3로 읽지 못했습니다: " + key);
                assertTrue(first.frequency() > 0, key);
            }
        }
    }
}

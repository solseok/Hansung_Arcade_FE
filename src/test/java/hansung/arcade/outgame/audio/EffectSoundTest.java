package hansung.arcade.outgame.audio;

import hansung.arcade.ingame.GameSound;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 인게임 효과음과 아웃게임 버튼 소리 파일이 모두 있고, 겹쳐 낼 수 있게 PCM으로 풀린다 */
class EffectSoundTest {

    /** 아웃게임에서 내는 효과음 (버튼·로그인·준비·시작·나가기·경기 끝) */
    private static final List<String> OUTGAME = List.of(
            Sound.CLICK, "Bt_c_GameStart", "Bt_c_GameReadyOn", "Bt_c_GameReadyOff", "Bt_c_Out",
            "Bt_c_SelectAIBattleChar", "Bt_c_CheckBox", "Bt_c_Success", "Bt_c_GameStartNotAble", "Ntc_GameEnd");

    @Test
    void 인게임_효과음이_모두_리소스에_있고_PCM으로_풀린다() {
        for (String key : GameSound.keys()) {
            assertDecodes(key);
        }
    }

    @Test
    void 아웃게임_버튼_소리가_모두_리소스에_있고_PCM으로_풀린다() {
        for (String key : OUTGAME) {
            assertDecodes(key);
        }
    }

    @Test
    void 버튼_클릭음은_카운트다운_삐_소리가_아니다() {
        assertEquals("Bt_c_Pop", Sound.CLICK);
    }

    @Test
    void 없는_소리는_빈_값이라_게임이_멈추지_않는다() {
        assertTrue(Sound.decode("no-such-sound").isEmpty());
    }

    private static void assertDecodes(String key) {
        Optional<Sound.Pcm> pcm = Sound.decode(key);
        assertTrue(pcm.isPresent(), "소리를 풀지 못했습니다: " + key);
        assertEquals(16, pcm.get().format().getSampleSizeInBits(), key);
        assertTrue(pcm.get().seconds() > 0.05 && pcm.get().seconds() < 5, key + " 길이 " + pcm.get().seconds());
    }
}

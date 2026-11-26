package hansung.arcade.outgame.audio;

import hansung.arcade.ingame.GameSound;
import hansung.arcade.ingame.match.StartIntro;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 경기 시작의 Ready 소리가 다 끝나고 잠깐 쉰 뒤에 Start 소리가 난다. 게임이 실제로 내는 PCM(JLayer로 푼 것)에서
 * 소리가 들리는 구간을 재서 확인한다. Ready 소리는 연출을 시작할 때, Start 소리는 {@link StartIntro#READY_SECONDS} 뒤에 난다.
 */
class StartSoundGapTest {

    /** 10ms씩 잰 크기가 가장 큰 곳보다 이만큼(dB) 넘게 작으면 들리지 않는 것으로 친다 */
    private static final double AUDIBLE_DB = 30;
    /** Ready 소리가 끝나고 Start 소리가 들리기까지 쉬는 시간의 범위(초). 짧으면 이어져 들리고, 길면 늘어진다 */
    private static final double MIN_PAUSE = 0.25;
    private static final double MAX_PAUSE = 0.6;

    @Test
    void Ready_소리가_다_끝나고_잠깐_쉰_뒤에_Start_소리가_들린다() {
        Audible ready = audible(GameSound.READY.key());
        Audible start = audible(GameSound.START.key());

        double pause = (StartIntro.READY_SECONDS + start.from()) - ready.to();

        assertTrue(pause >= MIN_PAUSE, "Ready가 끝나고 %.2f초 만에 Start가 들린다".formatted(pause));
        assertTrue(pause <= MAX_PAUSE, "Ready가 끝나고 %.2f초나 쉰다".formatted(pause));
    }

    @Test
    void 연출이_서버_카운트다운_3초_안에_들어간다() {
        // 온라인은 카운트다운이 끝나는 순간 연출도 끝나도록 남은 시간이 연출 길이가 될 때 시작한다 (ServerRules)
        assertTrue(StartIntro.TOTAL_SECONDS <= 3.0, "연출 " + StartIntro.TOTAL_SECONDS + "초");
    }

    /** 소리가 들리는 구간(초): 처음과 마지막으로 크기가 {@link #AUDIBLE_DB} 안에 드는 10ms 칸 */
    private record Audible(double from, double to) { }

    private static Audible audible(String key) {
        Sound.Pcm pcm = Sound.decode(key).orElseThrow();
        int channels = pcm.format().getChannels();
        float rate = pcm.format().getFrameRate();
        byte[] data = pcm.data();
        int frames = data.length / (2 * channels);
        int window = Math.round(rate / 100);
        int windows = (frames + window - 1) / window;
        double[] rms = new double[windows];
        double loudest = 0;
        for (int w = 0; w < windows; w++) {
            double sum = 0;
            int count = 0;
            for (int f = w * window; f < Math.min(frames, (w + 1) * window); f++) {
                for (int c = 0; c < channels; c++) {
                    int i = (f * channels + c) * 2;
                    short sample = (short) ((data[i] & 0xFF) | (data[i + 1] << 8));
                    sum += (double) sample * sample;
                    count++;
                }
            }
            rms[w] = Math.sqrt(sum / count);
            loudest = Math.max(loudest, rms[w]);
        }
        double threshold = loudest * Math.pow(10, -AUDIBLE_DB / 20);
        int first = -1;
        int last = -1;
        for (int w = 0; w < windows; w++) {
            if (rms[w] >= threshold) {
                if (first < 0) {
                    first = w;
                }
                last = w;
            }
        }
        return new Audible(first * 0.01, (last + 1) * 0.01);
    }
}

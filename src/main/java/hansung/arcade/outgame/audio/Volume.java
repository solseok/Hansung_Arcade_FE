package hansung.arcade.outgame.audio;

/**
 * 전체 음량(%)을 소리에 곱할 진폭 배율로 바꾸고, 16비트 소리에 곱한다. BGM과 효과음이 같은 계산을 쓴다.
 *
 * <p>배율은 음량의 제곱이다(100% → 1, 50% → 1/4 약 -12dB, 0% → 0). 사람 귀는 소리 크기를 로그로 느끼므로,
 * 음량을 그대로 곱하면 슬라이더를 반쯤 내려도 조금밖에 줄지 않다가 0 가까이에서 갑자기 작아진다. 제곱하면 슬라이더를 옮긴 만큼
 * 고르게 작아지게 들린다.</p>
 */
final class Volume {

    private Volume() {
    }

    /** 전체 음량(0~100%)의 진폭 배율 (0~1) */
    static double gain(int percent) {
        double ratio = percent / (double) Sound.MAX_VOLUME;
        return ratio * ratio;
    }

    /** 16비트 소리 한 개에 배율을 곱한다. 배율이 1 이하라 넘치지 않는다 */
    static short apply(short sample, double gain) {
        return (short) Math.round(sample * gain);
    }

    /**
     * 16비트 부호 있는 리틀 엔디언 PCM에 배율을 곱한 새 배열. 배율이 1 이상이면 원래 배열을 그대로 돌려준다
     * (풀어 둔 효과음은 여러 번 쓰므로 바꾸지 않는다).
     */
    static byte[] apply(byte[] pcm, double gain) {
        if (gain >= 1) {
            return pcm;
        }
        byte[] out = new byte[pcm.length];
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            write(out, i, apply(read(pcm, i), gain));
        }
        return out;
    }

    /** 리틀 엔디언 PCM의 i번째 바이트부터 16비트 소리 한 개 */
    static short read(byte[] pcm, int i) {
        return (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
    }

    /** 16비트 소리 한 개를 리틀 엔디언으로 pcm의 i번째 바이트부터 적는다 */
    static void write(byte[] pcm, int i, short sample) {
        pcm[i] = (byte) sample;
        pcm[i + 1] = (byte) (sample >> 8);
    }
}

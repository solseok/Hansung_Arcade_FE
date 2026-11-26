package hansung.arcade.outgame.audio;

import javazoom.jl.player.JavaSoundAudioDevice;

/**
 * BGM을 트는 소리 장치. jlayer가 MP3에서 풀어 준 16비트 소리를 장치에 쓰기 직전에 전체 음량({@link Sound#gain()})을 곱한다.
 * MP3 프레임(약 26ms)마다 음량을 새로 읽으므로, 틀고 있는 곡도 음량을 바꾸면 곧바로 커지거나 작아진다.
 */
class VolumeAudioDevice extends JavaSoundAudioDevice {

    @Override
    protected byte[] toByteArray(short[] samples, int offs, int len) {
        double gain = Sound.gain();
        if (gain >= 1) {
            return super.toByteArray(samples, offs, len);
        }
        byte[] bytes = getByteArray(len * 2);
        for (int i = 0; i < len; i++) {
            short scaled = Volume.apply(samples[offs + i], gain);
            bytes[i * 2] = (byte) scaled;
            bytes[i * 2 + 1] = (byte) (scaled >> 8);
        }
        return bytes;
    }
}

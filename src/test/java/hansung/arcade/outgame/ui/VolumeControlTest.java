package hansung.arcade.outgame.ui;

import hansung.arcade.outgame.SwingTree;
import hansung.arcade.outgame.audio.Sound;
import hansung.arcade.outgame.audio.SoundSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JLabel;
import javax.swing.JSlider;
import java.awt.event.MouseWheelEvent;
import java.nio.file.Path;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 메뉴의 전체 음량 줄: 슬라이더를 움직이면 바로 적용하고, 손을 떼면 저장한다 */
class VolumeControlTest {

    @TempDir
    Path dir;
    private Path file;

    @BeforeEach
    void 저장할_곳을_임시_폴더로() {
        Sound.setMuted(true); // 테스트 중 클릭음이 나지 않게
        Sound.setVolume(Sound.MAX_VOLUME);
        file = dir.resolve("settings.properties");
        Sound.useSettings(SoundSettings.file(file));
    }

    @AfterEach
    void 원래대로() {
        Sound.setVolume(Sound.MAX_VOLUME);
        Sound.useSettings(SoundSettings.inMemory());
        Sound.setMuted(false);
    }

    @Test
    void 지금_음량을_보여_준다() {
        Sound.setVolume(60);
        VolumeControl control = new VolumeControl();

        assertEquals(60, slider(control).getValue());
        assertEquals("60%", value(control).getText());
        assertTrue(SwingTree.texts(control).contains("전체 음량"), "이름이 없습니다: " + SwingTree.texts(control));
    }

    @Test
    void 움직이는_동안_바로_적용하고_손을_떼면_저장한다() {
        VolumeControl control = new VolumeControl();
        JSlider slider = slider(control);

        slider.setValueIsAdjusting(true);
        slider.setValue(40);
        assertEquals(40, Sound.volume(), "움직이는 동안 바로 적용해야 지금 나오는 BGM이 작아진다");
        assertEquals("40%", value(control).getText());
        assertEquals(OptionalInt.empty(), SoundSettings.file(file).volume(), "움직이는 동안에는 파일을 쓰지 않는다");

        slider.setValue(25);
        slider.setValueIsAdjusting(false);
        assertEquals(25, Sound.volume());
        assertEquals(OptionalInt.of(25), SoundSettings.file(file).volume(), "손을 떼면 이 컴퓨터에 저장한다");
    }

    @Test
    void 마우스_휠로도_5퍼센트씩_바꾸고_저장한다() {
        VolumeControl control = new VolumeControl();
        JSlider slider = slider(control);

        // 휠을 아래로 한 칸(+1) 굴리면 작아진다
        slider.dispatchEvent(new MouseWheelEvent(slider, MouseWheelEvent.MOUSE_WHEEL, 0, 0, 10, 10, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 1));

        assertEquals(Sound.MAX_VOLUME - VolumeControl.STEP, Sound.volume());
        assertEquals(OptionalInt.of(95), SoundSettings.file(file).volume());
    }

    @Test
    void 음소거_중에_바꿔도_음소거는_그대로다() {
        VolumeControl control = new VolumeControl();
        slider(control).setValue(10);
        assertTrue(Sound.isMuted());
        assertEquals(10, Sound.volume());
    }

    private static JSlider slider(VolumeControl control) {
        return SwingTree.named(control, "volume-slider", JSlider.class);
    }

    private static JLabel value(VolumeControl control) {
        return SwingTree.named(control, "volume-value", JLabel.class);
    }
}

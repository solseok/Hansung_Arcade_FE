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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 메뉴의 전체 음량 줄: 슬라이더를 움직이면 바로 적용하고, 손을 떼고 잠깐 더 바뀌지 않으면 저장한다.
 * 휠을 잇달아 굴리거나 트랙을 누른 채 끌어도 저장은 마지막 값으로 한 번이고, 저장하기 전에 메뉴가 닫히면 그때 저장한다.
 */
class VolumeControlTest {

    @TempDir
    Path dir;
    private Path file;

    @BeforeEach
    void 저장할_곳을_임시_폴더로() {
        Sound.stopAll();
        Sound.setMuted(true); // 테스트 중 클릭음이 나지 않게
        Sound.setVolume(Sound.MAX_VOLUME);
        file = dir.resolve("settings.properties");
        Sound.useSettings(SoundSettings.file(file));
    }

    @AfterEach
    void 원래대로() {
        Sound.stopAll();
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
    void 움직이는_동안_바로_적용하고_손을_떼고_잠깐_뒤에_저장한다() throws InterruptedException {
        VolumeControl control = new VolumeControl();
        JSlider slider = slider(control);

        slider.setValueIsAdjusting(true);
        slider.setValue(40);
        assertEquals(40, Sound.volume(), "움직이는 동안 바로 적용해야 지금 나오는 BGM이 작아진다");
        assertEquals("40%", value(control).getText());
        assertFalse(control.settling(), "움직이는 동안에는 저장하려고 기다리지 않는다");

        slider.setValue(25);
        slider.setValueIsAdjusting(false);
        assertEquals(25, Sound.volume());
        assertTrue(control.settling());
        assertEquals(OptionalInt.empty(), saved(), "손을 떼자마자 저장하지 않고 잠깐 기다린다");

        assertEquals(OptionalInt.of(25), awaitSaved(), "손을 떼면 이 컴퓨터에 저장한다");
    }

    @Test
    void 트랙을_누른_채_끌면_누를_때가_아니라_뗄_때의_값으로_한_번_저장한다() throws InterruptedException {
        VolumeControl control = new VolumeControl();
        JSlider slider = slider(control);

        // 맥(Aqua)은 트랙을 누르면 adjusting을 켰다 끄며 값을 옮기고, 끌기 시작하면 다시 켠다
        slider.setValueIsAdjusting(true);
        slider.setValue(16);
        slider.setValueIsAdjusting(false);
        slider.setValueIsAdjusting(true);
        slider.setValue(45);
        Thread.sleep(VolumeControl.SETTLE_MILLIS + 200);
        assertEquals(OptionalInt.empty(), saved(), "끄는 중에 누를 때의 값을 저장했습니다");

        slider.setValueIsAdjusting(false);
        assertEquals(OptionalInt.of(45), awaitSaved());
    }

    @Test
    void 휠을_잇달아_굴리면_5퍼센트씩_바꾸고_마지막_값으로_한_번_저장한다() throws InterruptedException {
        VolumeControl control = new VolumeControl();
        JSlider slider = slider(control);

        // 트랙패드로 쓸면 휠 이벤트가 잇달아 온다. 아래로(+1) 굴리면 작아진다
        for (int i = 0; i < 5; i++) {
            slider.dispatchEvent(new MouseWheelEvent(slider, MouseWheelEvent.MOUSE_WHEEL, 0, 0, 10, 10, 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 1));
        }

        assertEquals(Sound.MAX_VOLUME - 5 * VolumeControl.STEP, Sound.volume());
        assertEquals(OptionalInt.empty(), saved(), "휠 한 칸마다 저장하면 안 됩니다");
        assertEquals(OptionalInt.of(75), awaitSaved());
    }

    @Test
    void 저장하기_전에_메뉴가_닫히면_바로_저장한다() {
        VolumeControl control = new VolumeControl();
        JSlider slider = slider(control);

        // 끄는 중에 창을 바꿔 손 뗀 것을 받지 못하고 메뉴가 닫혔다
        slider.setValueIsAdjusting(true);
        slider.setValue(30);
        control.closed();
        assertEquals(OptionalInt.of(30), saved());

        // 손을 떼고 기다리는 중에 닫혔다
        slider.setValueIsAdjusting(false);
        slider.setValue(55);
        assertTrue(control.settling());
        control.closed();
        assertFalse(control.settling());
        assertEquals(OptionalInt.of(55), saved());
    }

    @Test
    void 음소거_중에_바꿔도_음소거는_그대로다() {
        VolumeControl control = new VolumeControl();
        slider(control).setValue(10);
        assertTrue(Sound.isMuted());
        assertEquals(10, Sound.volume());
    }

    private OptionalInt saved() {
        return SoundSettings.file(file).volume();
    }

    /** 저장을 기다리는 타이머(EDT)가 저장할 때까지 기다린 값 */
    private OptionalInt awaitSaved() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (saved().isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        return saved();
    }

    private static JSlider slider(VolumeControl control) {
        return SwingTree.named(control, "volume-slider", JSlider.class);
    }

    private static JLabel value(VolumeControl control) {
        return SwingTree.named(control, "volume-value", JLabel.class);
    }
}

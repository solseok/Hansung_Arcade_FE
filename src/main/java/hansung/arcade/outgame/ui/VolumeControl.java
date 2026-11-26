package hansung.arcade.outgame.ui;

import hansung.arcade.outgame.audio.Sound;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * 메뉴에 넣는 전체 음량 줄: {@code 전체 음량 [────●──] 70%}.
 *
 * <p>슬라이더를 움직이는 동안 {@link Sound#setVolume}으로 지금 나오는 BGM과 이후 효과음에 바로 적용하고, 손을 떼면 이 컴퓨터에
 * 저장한 뒤({@link Sound#saveVolume}) 효과음이 어느 크기로 나는지 클릭음을 한 번 들려준다. 트랙을 누르거나 마우스 휠을 굴려도
 * 같다(휠 한 칸에 {@value #STEP}%). 음소거와는 따로라서, 음소거 중에 바꾼 음량은 음소거를 풀면 그 크기로 난다.</p>
 */
public final class VolumeControl extends JPanel {
    /** 마우스 휠 한 칸에 바뀌는 음량(%) */
    static final int STEP = 5;
    private static final int SLIDER_WIDTH = 130;

    private final JSlider slider = new JSlider(0, Sound.MAX_VOLUME, Sound.volume());
    private final JLabel value = new JLabel(percent(Sound.MAX_VOLUME), SwingConstants.RIGHT);

    public VolumeControl() {
        super(new BorderLayout(8, 0));
        setName("volume");
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 20, 4, 14));
        Font font = UIManager.getFont("MenuItem.font");

        JLabel title = new JLabel("전체 음량");
        title.setFont(font);
        value.setName("volume-value");
        value.setFont(font);
        // 가장 긴 "100%" 폭으로 고정해 숫자가 바뀌어도 슬라이더가 움직이지 않게 한다
        value.setPreferredSize(value.getPreferredSize());
        value.setText(percent(slider.getValue()));

        slider.setName("volume-slider");
        slider.setOpaque(false);
        // 메뉴 안이라 키보드 대신 마우스로만 다룬다 (누를 때 메뉴 밖 화면의 포커스를 가져가지 않게)
        slider.setFocusable(false);
        slider.setPreferredSize(new Dimension(SLIDER_WIDTH, slider.getPreferredSize().height));
        slider.addChangeListener(event -> changed());
        slider.addMouseWheelListener(event -> slider.setValue(slider.getValue() - event.getWheelRotation() * STEP));

        add(title, BorderLayout.WEST);
        add(slider, BorderLayout.CENTER);
        add(value, BorderLayout.EAST);
    }

    private void changed() {
        int percent = slider.getValue();
        Sound.setVolume(percent);
        value.setText(percent(percent));
        if (!slider.getValueIsAdjusting()) {
            Sound.saveVolume();
            Sound.click();
        }
    }

    private static String percent(int value) {
        return value + "%";
    }
}

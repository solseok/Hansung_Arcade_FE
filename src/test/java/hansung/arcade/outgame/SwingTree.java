package hansung.arcade.outgame;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 창에 띄우지 않은 Swing 화면을 이름·종류·글자로 찾고, 그림으로 그려 픽셀을 확인하는 테스트 도구.
 * 아웃게임 화면은 컴포넌트에 {@link Component#setName 이름}을 붙여 두고 테스트가 그 이름으로 찾는다.
 */
public final class SwingTree {

    private SwingTree() {
    }

    /** root 아래(root 제외)에서 type인 컴포넌트를 모두 찾는다. 앞에서부터 깊이 우선 순서다. */
    public static <T> List<T> all(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(root, type, found);
        return found;
    }

    private static <T> void collect(Container parent, Class<T> type, List<T> found) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container container) {
                collect(container, type, found);
            }
        }
    }

    public static Optional<Component> find(Container root, String name) {
        return all(root, Component.class).stream().filter(c -> name.equals(c.getName())).findFirst();
    }

    /** 이름이 name인 컴포넌트. 없거나 종류가 다르면 실패한다. */
    public static <T> T named(Container root, String name, Class<T> type) {
        Component component = find(root, name)
                .orElseThrow(() -> new AssertionError("이름이 '" + name + "'인 컴포넌트가 없습니다."));
        if (!type.isInstance(component)) {
            throw new AssertionError("'" + name + "'은(는) " + type.getSimpleName() + "이(가) 아니라 "
                    + component.getClass().getSimpleName() + "입니다.");
        }
        return type.cast(component);
    }

    public static List<Component> namedStartingWith(Container root, String prefix) {
        return all(root, Component.class).stream()
                .filter(c -> c.getName() != null && c.getName().startsWith(prefix))
                .toList();
    }

    public static Optional<AbstractButton> findButton(Container root, String text) {
        return all(root, AbstractButton.class).stream().filter(b -> text.equals(b.getText())).findFirst();
    }

    public static AbstractButton button(Container root, String text) {
        return findButton(root, text)
                .orElseThrow(() -> new AssertionError("글자가 '" + text + "'인 버튼이 없습니다."));
    }

    /** 라벨·버튼·입력 칸의 글자. 글자가 없는 컴포넌트면 null */
    public static String text(Component component) {
        if (component instanceof JLabel label) {
            return label.getText();
        }
        if (component instanceof AbstractButton button) {
            return button.getText();
        }
        if (component instanceof JTextComponent field) {
            return field.getText();
        }
        return null;
    }

    /** root 아래 라벨·버튼·입력 칸의 글자 전부 (빈 글자 제외) */
    public static List<String> texts(Container root) {
        return all(root, Component.class).stream()
                .map(SwingTree::text)
                .filter(text -> text != null && !text.isBlank())
                .toList();
    }

    /** component와 root까지의 조상이 모두 보이는 상태인지 */
    public static boolean visibleIn(Component component, Container root) {
        for (Component c = component; c != null && c != root; c = c.getParent()) {
            if (!c.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /** root 기준 좌표로 옮긴 component의 영역 */
    public static Rectangle boundsIn(Component component, Container root) {
        return SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), root);
    }

    /** 레이아웃 관리자를 쓰는 컨테이너도 자식 위치가 정해지도록 아래로 내려가며 배치한다. */
    public static void layout(Component component) {
        if (component.getWidth() == 0 || component.getHeight() == 0) {
            component.setSize(component.getPreferredSize());
        }
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                layout(child);
            }
        }
    }

    /** 투명한 바탕에 component를 그린 그림. 크기가 0이면 선호 크기로 맞춘다. */
    public static BufferedImage render(JComponent component) {
        layout(component);
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            component.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    public static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    public static java.awt.Color color(BufferedImage image, int x, int y) {
        return new java.awt.Color(image.getRGB(x, y), true);
    }
}

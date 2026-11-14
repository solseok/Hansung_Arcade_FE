package hansung.arcade.ingame.hud;

/**
 * 화면 위 사각형 영역(픽셀). 불변이라 레이아웃 상수로 공유해도 안전하다.
 * 오른쪽·아래 끝({@link #right()}, {@link #bottom()})은 영역에 포함되지 않는다.
 */
public record Box(int x, int y, int width, int height) {

    public Box {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("크기는 음수일 수 없습니다: %dx%d".formatted(width, height));
        }
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    /** 안쪽으로 여백만큼 줄인 영역 */
    public Box inset(int top, int left, int bottom, int right) {
        return new Box(x + left, y + top, width - left - right, height - top - bottom);
    }

    public Box inset(int all) {
        return inset(all, all, all, all);
    }

    /** 이 영역 왼쪽 위를 원점으로 한 상대 좌표의 영역 */
    public Box child(int dx, int dy, int width, int height) {
        return new Box(x + dx, y + dy, width, height);
    }

    public boolean contains(int px, int py) {
        return px >= x && px < right() && py >= y && py < bottom();
    }

    public boolean contains(Box other) {
        return other.x >= x && other.y >= y && other.right() <= right() && other.bottom() <= bottom();
    }
}

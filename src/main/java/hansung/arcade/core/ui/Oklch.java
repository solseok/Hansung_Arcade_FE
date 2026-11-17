package hansung.arcade.core.ui;

/**
 * OKLCH 색. 사람 눈에 고르게 느껴지도록 만든 OKLab 색 공간(Björn Ottosson, 2020)을 극좌표로 나타낸 것이다.
 * 밝기를 그대로 두고 색상각·채도만 바꿔도 밝기가 거의 같아 보여서, 그림의 명암을 지키며 색을 바꿀 때 쓴다.
 *
 * <p>sRGB(0~255) → 선형 RGB(0~1) → OKLab(L, a, b) → OKLCH(L, C, h) 순서로 바꾸고, 거꾸로 되돌린다.
 * sRGB로 되돌릴 때 표시할 수 없는 색(sRGB 범위 밖)이면 밝기와 색상각은 두고 채도만 줄여서 맞춘다.</p>
 *
 * @param l 밝기. 0(검정)~1(흰색)
 * @param c 채도. 0(회색)부터 커지며 sRGB에서는 0.33 남짓까지 나온다
 * @param h 색상각(도). 0 이상 360 미만. 대략 빨강 25, 노랑 100, 초록 140, 하늘 210, 파랑 265, 마젠타 350
 */
public record Oklch(double l, double c, double h) {

    /** sRGB 범위 검사에서 허용하는 계산 오차. 8비트 sRGB 색을 왕복했을 때 생기는 1e-9 수준의 오차를 범위 안으로 본다 */
    private static final double GAMUT_EPSILON = 1e-7;
    /** 채도를 줄여 sRGB 범위에 맞출 때 반으로 나누는 횟수. 2^-30 이면 채도 오차가 1e-9 아래다 */
    private static final int GAMUT_STEPS = 30;

    public Oklch {
        if (!Double.isFinite(l) || !Double.isFinite(c) || !Double.isFinite(h)) {
            throw new IllegalArgumentException("OKLCH 값이 숫자가 아닙니다: (%s, %s, %s)".formatted(l, c, h));
        }
        if (c < 0) {
            throw new IllegalArgumentException("채도는 0 이상이어야 합니다: " + c);
        }
        h = ((h % 360) + 360) % 360;
    }

    /** 0xRRGGBB (위 8비트는 무시) 색 */
    public static Oklch fromRgb(int rgb) {
        double r = toLinear((rgb >> 16) & 0xFF);
        double g = toLinear((rgb >> 8) & 0xFF);
        double b = toLinear(rgb & 0xFF);

        double lCone = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
        double mCone = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
        double sCone = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);

        double labL = 0.2104542553 * lCone + 0.7936177850 * mCone - 0.0040720468 * sCone;
        double labA = 1.9779984951 * lCone - 2.4285922050 * mCone + 0.4505937099 * sCone;
        double labB = 0.0259040371 * lCone + 0.7827717662 * mCone - 0.8086757660 * sCone;

        return new Oklch(labL, Math.hypot(labA, labB), Math.toDegrees(Math.atan2(labB, labA)));
    }

    /** sRGB 0xRRGGBB. 범위 밖의 색이면 {@link #inGamut 범위에 맞춘} 뒤 바꾼다. */
    public int toRgb() {
        double[] linear = inGamut().toLinear();
        return (toByte(linear[0]) << 16) | (toByte(linear[1]) << 8) | toByte(linear[2]);
    }

    /** sRGB로 그대로 나타낼 수 있는지 */
    public boolean isInGamut() {
        double[] linear = toLinear();
        for (double channel : linear) {
            if (channel < -GAMUT_EPSILON || channel > 1 + GAMUT_EPSILON) {
                return false;
            }
        }
        return true;
    }

    /**
     * sRGB로 나타낼 수 있는 색. 이미 범위 안이면 그대로, 아니면 밝기·색상각은 두고 채도를 범위 안에서 가장 크게 줄인 색이다.
     * 밝기가 0~1 밖이면 검정이나 흰색이 된다.
     */
    public Oklch inGamut() {
        if (l <= 0) {
            return new Oklch(0, 0, h);
        }
        if (l >= 1) {
            return new Oklch(1, 0, h);
        }
        if (isInGamut()) {
            return this;
        }
        double low = 0;
        double high = c;
        for (int i = 0; i < GAMUT_STEPS; i++) {
            double mid = (low + high) / 2;
            if (withChroma(mid).isInGamut()) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return withChroma(low);
    }

    public Oklch withChroma(double chroma) {
        return new Oklch(l, chroma, h);
    }

    /** 선형 RGB(0~1, 범위 밖일 수 있음) */
    private double[] toLinear() {
        double radians = Math.toRadians(h);
        double labA = c * Math.cos(radians);
        double labB = c * Math.sin(radians);

        double lCone = cube(l + 0.3963377774 * labA + 0.2158037573 * labB);
        double mCone = cube(l - 0.1055613458 * labA - 0.0638541728 * labB);
        double sCone = cube(l - 0.0894841775 * labA - 1.2914855480 * labB);

        return new double[]{
                4.0767416621 * lCone - 3.3077115913 * mCone + 0.2309699292 * sCone,
                -1.2684380046 * lCone + 2.6097574011 * mCone - 0.3413193965 * sCone,
                -0.0041960863 * lCone - 0.7034186147 * mCone + 1.7076147010 * sCone};
    }

    /** sRGB 감마를 풀어 빛의 세기에 비례하는 값(0~1)으로 */
    private static double toLinear(int channel) {
        double v = channel / 255.0;
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    /** 선형 값(0~1)을 sRGB 감마를 입힌 0~255로 */
    private static int toByte(double linear) {
        double v = Math.clamp(linear, 0.0, 1.0);
        double encoded = v <= 0.0031308 ? v * 12.92 : 1.055 * Math.pow(v, 1 / 2.4) - 0.055;
        return (int) Math.round(Math.clamp(encoded, 0.0, 1.0) * 255);
    }

    private static double cube(double v) {
        return v * v * v;
    }
}

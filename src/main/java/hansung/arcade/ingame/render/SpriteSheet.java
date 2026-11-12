package hansung.arcade.ingame.render;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 같은 폭의 프레임이 가로 한 줄로 이어 붙은 BMP. 마젠타(255, 0, 255) 배경은 투명하게 바꿔 둔다.
 */
public final class SpriteSheet {

    private final String path;
    private final BufferedImage image;

    private SpriteSheet(String path, BufferedImage image) {
        this.path = path;
        this.image = image;
    }

    /** 클래스패스 리소스(예: /image/camp/01_block_normal.bmp)를 읽는다. 파일이 없으면 빈 값. */
    public static Optional<SpriteSheet> find(String resourcePath) {
        try (InputStream in = SpriteSheet.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return Optional.empty();
            }
            BufferedImage raw = ImageIO.read(in);
            if (raw == null) {
                throw new IllegalArgumentException("이미지 형식을 읽을 수 없습니다: " + resourcePath);
            }
            return Optional.of(new SpriteSheet(resourcePath, withTransparentKey(raw)));
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 파일을 읽지 못했습니다: " + resourcePath, e);
        }
    }

    /** frameWidth 폭으로 왼쪽부터 자른 프레임들. 원본 픽셀을 공유한다. */
    public List<BufferedImage> frames(int frameWidth) {
        if (frameWidth <= 0 || image.getWidth() % frameWidth != 0) {
            throw new IllegalArgumentException("%s 의 폭 %d가 프레임 폭 %d의 배수가 아닙니다."
                    .formatted(path, image.getWidth(), frameWidth));
        }
        List<BufferedImage> frames = new ArrayList<>();
        for (int x = 0; x < image.getWidth(); x += frameWidth) {
            frames.add(image.getSubimage(x, 0, frameWidth, image.getHeight()));
        }
        return frames;
    }

    private static BufferedImage withTransparentKey(BufferedImage raw) {
        int w = raw.getWidth();
        int h = raw.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = raw.getRGB(x, y);
                out.setRGB(x, y, isColorKey(rgb) ? 0 : rgb | 0xFF000000);
            }
        }
        return out;
    }

    /**
     * 투명 처리할 배경색인지. 편집 과정에서 가장자리 마젠타가 (255, 3, 255) 처럼
     * 살짝 틀어지는 경우가 있어서, 빨강·파랑이 높고 초록이 거의 없는 색을 모두 배경으로 본다.
     */
    static boolean isColorKey(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r >= 180 && b >= 180 && g <= 60 && Math.abs(r - b) <= 70;
    }
}

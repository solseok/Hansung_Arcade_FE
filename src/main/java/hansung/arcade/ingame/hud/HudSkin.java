package hansung.arcade.ingame.hud;

import hansung.arcade.ingame.entity.Mascot;
import hansung.arcade.ingame.render.SpriteSheet;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * HUD에 들어가는 글자 그리는 방법과 그림 묶음. 글자를 디자인 이미지로 바꿀 때는 해당 칸의
 * {@link TextArt}만 이미지로 그리는 구현으로 바꿔 새로 만들면 된다 (세로 정렬은 그림의 실제 픽셀 기준으로 맞춘다).
 * 예) TIMER 글씨만 이미지로:
 *
 * <pre>
 * HudSkin skin = HudSkin.load(fonts);
 * TextArt timerImage = ...; // TextArt를 이미지로 구현한 것
 * skin = new HudSkin(timerImage, skin.digits(), skin.itemLabel(), ...);
 * </pre>
 *
 * @param timerLabel   "TIMER" 글씨 (Michroma)
 * @param digits       타이머 MM:ss 와 맵 번호 숫자
 * @param itemLabel    아이템 상자 위 "ITEM" 글씨 (Michroma)
 * @param itemBarLabel 아래 바의 "내 아이템" (HY견고딕)
 * @param keyNumber    아이템 칸 단축키 번호
 * @param exitLabel    "나가기" (HY윤고딕)
 * @param nickname     플레이어 목록 닉네임
 * @param chatText     맵 위 채팅 글자 (돋움체, 흰 글자에 검은 테두리). 입력칸·다른 플레이어의 채팅·말풍선에 쓴다
 * @param myChatText   채팅 목록에서 내가 친 줄 (같은 글꼴, #DFDB86)
 * @param portraits    플레이어 목록에 넣을 캐릭터 얼굴. 그림 파일이 없는 캐릭터는 빠져 있다
 */
public record HudSkin(
        TextArt timerLabel,
        TextArt digits,
        TextArt itemLabel,
        TextArt itemBarLabel,
        TextArt keyNumber,
        TextArt exitLabel,
        TextArt nickname,
        OutlineText chatText,
        OutlineText myChatText,
        Map<Mascot, BufferedImage> portraits) {

    private static final String PORTRAIT_DIR = "/sprites/characters/";
    /** 캐릭터 그림 한 장(70 x 70) */
    private static final int CHARACTER_FRAME = 70;

    public HudSkin {
        portraits = Map.copyOf(portraits);
    }

    /** 디자인 글꼴로 만든 기본 모양과 캐릭터 얼굴 그림 */
    public static HudSkin load(HudFonts fonts) {
        Font timerLabelFont = HudFonts.fitInkWidth(fonts.michroma(10), "TIMER", HudLayout.TIMER_LABEL_WIDTH - 1);
        Font digitFont = HudFonts.fitInkHeight(fonts.michroma(12), "0", 11);
        Font itemLabelFont = HudFonts.fitInkHeight(fonts.michroma(10), "ITEM", HudLayout.ITEM_LABEL_HEIGHT);
        Font itemBarFont = HudFonts.fitInkHeight(fonts.gyeonGothic(12), "내 아이템", 12);
        Font keyFont = HudFonts.fitInkHeight(fonts.gyeonGothic(8), "0", 7);
        Font exitFont = HudFonts.fitInkHeight(fonts.yunGothic(18), "나가기", HudLayout.EXIT_TEXT_HEIGHT);
        Font nicknameFont = HudFonts.fitInkHeight(fonts.gyeonGothic(12), "가", HudLayout.NICKNAME_HEIGHT);
        Font chatFont = fonts.dotumChe(HudLayout.CHAT_TEXT_HEIGHT);

        return new HudSkin(
                new FontTextArt(timerLabelFont, HudPalette.DISPLAY_LABEL),
                new FontTextArt(digitFont, HudPalette.DISPLAY_DIGIT),
                new FontTextArt(itemLabelFont, HudPalette.ITEM_LABEL),
                new FontTextArt(itemBarFont, HudPalette.ITEM_BAR_LABEL),
                new FontTextArt(keyFont, HudPalette.KEY_DIGIT),
                new FontTextArt(exitFont, HudPalette.EXIT_TEXT, HudPalette.EXIT_BUTTON_EDGE),
                new FontTextArt(nicknameFont, HudPalette.NICKNAME, HudPalette.edgeOf(HudPalette.PLAYER_SLOT)),
                new OutlineText(chatFont, HudLayout.CHAT_TEXT_HEIGHT, HudPalette.CHAT_TEXT, HudPalette.CHAT_OUTLINE),
                new OutlineText(chatFont, HudLayout.CHAT_TEXT_HEIGHT, HudPalette.CHAT_TEXT_MINE, HudPalette.CHAT_OUTLINE),
                loadPortraits());
    }

    private static Map<Mascot, BufferedImage> loadPortraits() {
        Map<Mascot, BufferedImage> portraits = new EnumMap<>(Mascot.class);
        for (Mascot mascot : Mascot.values()) {
            SpriteSheet.find(PORTRAIT_DIR + mascot.id() + "_front.bmp")
                    .map(sheet -> sheet.frames(CHARACTER_FRAME).get(0))
                    .map(HudSkin::portraitOf)
                    .ifPresent(portrait -> portraits.put(mascot, portrait));
        }
        return portraits;
    }

    /**
     * 정면 그림에서 머리 쪽을 얼굴 틀 안쪽과 같은 비율로 잘라 틀 크기로 줄인다.
     * 캐릭터 폭보다 조금 넓게 잘라서 얼굴이 틀을 꽉 채우되 양옆이 잘리지 않게 한다.
     */
    static BufferedImage portraitOf(BufferedImage front) {
        int targetWidth = HudLayout.PORTRAIT_WIDTH - 2 * HudLayout.EDGE;
        int targetHeight = HudLayout.PORTRAIT_HEIGHT - 2 * HudLayout.EDGE;
        Box body = PixelImages.opaqueBounds(front);
        int width = Math.min(front.getWidth(), Math.max(targetWidth, body.width() + 4));
        int height = Math.min(front.getHeight() - body.y(), (int) Math.round(width * targetHeight / (double) targetWidth));
        int left = Math.max(0, Math.min(front.getWidth() - width, body.x() + (body.width() - width) / 2));
        BufferedImage head = front.getSubimage(left, body.y(), width, height);
        return PixelImages.shrinkToFit(head, targetWidth, targetHeight);
    }
}

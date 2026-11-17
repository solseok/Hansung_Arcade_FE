package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.CharacterColor;

import java.awt.Color;

public enum Team {
    RED("빨강", 234, 1, 4, CharacterColor.RED),
    YELLOW("노랑", 253, 180, 4, CharacterColor.YELLOW),
    ORANGE("주황", 255, 122, 4, CharacterColor.ORANGE),
    GREEN("초록", 96, 179, 1, CharacterColor.GREEN),
    SKY("청록", 2, 182, 194, CharacterColor.SKY),
    BLUE("파랑", 0, 92, 228, CharacterColor.BLUE),
    PURPLE("보라", 98, 20, 224, CharacterColor.PURPLE),
    MAGENTA("분홍", 227, 0, 136, CharacterColor.MAGENTA);
    private final String label;
    private final Color color;
    private final CharacterColor characterColor;

    Team(String label, int r, int g, int b, CharacterColor characterColor) {
        this.label = label;
        color = new Color(r, g, b);
        this.characterColor = characterColor;
    }

    public String label() {
        return label;
    }

    public Color color() {
        return color;
    }

    public CharacterColor characterColor() {
        return characterColor;
    }

    public static Team byLabel(String label) {
        return java.util.Arrays.stream(values())
                .filter(t -> t.label.equals(label))
                .findFirst()
                .orElseThrow();
    }

    @Override
    public String toString() {
        return label;
    }
}

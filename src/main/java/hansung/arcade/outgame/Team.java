package hansung.arcade.outgame;

import hansung.arcade.ingame.entity.CharacterColor;

import java.awt.Color;

/**
 * 대기실에서 고르는 팀 8색. 순서와 칸 색은 참고 화면의 팀선택 칸을 따른다.
 * 캐릭터는 팀마다 짝이 되는 고정색({@link CharacterColor})으로 옷·모자 같은 고정색 부분을 다시 칠해 보여 준다.
 */
public enum Team {
    RED("빨강", new Color(234, 1, 4), CharacterColor.RED),
    YELLOW("노랑", new Color(253, 180, 4), CharacterColor.YELLOW),
    ORANGE("주황", new Color(255, 122, 4), CharacterColor.ORANGE),
    GREEN("초록", new Color(96, 179, 1), CharacterColor.GREEN),
    MINT("청록", new Color(2, 182, 194), CharacterColor.SKY),
    BLUE("파랑", new Color(0, 92, 228), CharacterColor.BLUE),
    PURPLE("보라", new Color(98, 20, 224), CharacterColor.PURPLE),
    PINK("분홍", new Color(227, 0, 136), CharacterColor.MAGENTA);

    private final String label;
    private final Color color;
    private final CharacterColor characterColor;

    Team(String label, Color color, CharacterColor characterColor) {
        this.label = label;
        this.color = color;
        this.characterColor = characterColor;
    }

    /** 이 팀 캐릭터의 고정색 부분을 칠할 색 */
    public CharacterColor characterColor() {
        return characterColor;
    }

    public String label() {
        return label;
    }

    public Color color() {
        return color;
    }

    /** 이름(빨강 등)에 해당하는 팀. 없으면 빨강 */
    public static Team fromLabel(String label) {
        for (Team team : values()) {
            if (team.label.equals(label)) {
                return team;
            }
        }
        return RED;
    }
}

package hansung.arcade.outgame.lobby;

import hansung.arcade.outgame.GameMaps;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Optional;

/** 방장이 맵선택을 눌렀을 때 맵(또는 랜덤)을 고르는 모달. 테스트에서는 바로 답하는 것으로 바꿔 끼운다. */
@FunctionalInterface
public interface MapPicker {

    /** 고른 맵 id. 취소하면 비어 있다 */
    Optional<String> pick(Component parent, String currentMapId);

    static MapPicker dialog() {
        return (parent, current) -> {
            String[] names = GameMaps.choices().stream().map(GameMaps.GameMap::name).toArray(String[]::new);
            Object chosen = JOptionPane.showInputDialog(parent, "맵을 고르세요.", "맵 선택", JOptionPane.PLAIN_MESSAGE,
                    null, names, GameMaps.byId(current).name());
            return Optional.ofNullable(chosen).map(name -> GameMaps.byName((String) name).id());
        };
    }
}

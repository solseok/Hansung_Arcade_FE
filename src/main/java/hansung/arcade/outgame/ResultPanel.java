package hansung.arcade.outgame;

import hansung.arcade.core.game.Scene;
import hansung.arcade.outgame.lobby.LobbyArtwork;
import hansung.arcade.core.game.SceneManager;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

public final class ResultPanel extends JPanel implements Scene {
    private final JLabel title = new JLabel();
    private final JPanel rankings = new JPanel(new GridLayout(0, 1, 8, 8));

    public ResultPanel(SceneManager scenes) {
        super(new BorderLayout(20, 20));
        setPreferredSize(new Dimension(800, 600));
        setBackground(new java.awt.Color(0x00BCEB));
        rankings.setBackground(new java.awt.Color(0xB4EDFF));
        rankings.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        setBorder(BorderFactory.createEmptyBorder(40, 70, 40, 70));
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        add(title, BorderLayout.NORTH);
        add(rankings, BorderLayout.CENTER);
        JButton back = LobbyArtwork.button("대기실로 돌아가기", false);
        back.addActionListener(event -> scenes.show("waiting", null));
        add(back, BorderLayout.SOUTH);
    }

    @Override
    public void enter(Object data) {
        GameResult result = (GameResult) data;
        String winner = result.standings().stream().filter(row -> row.playerId() == result.winnerId())
                .map(GameResult.Standing::nickname).findFirst().orElse("없음");
        title.setText("목업 결과 · 승자 " + winner);
        rankings.removeAll();
        rankings.add(new JLabel("플레이 시간 " + result.playSeconds() + "초"));
        result.standings().stream().sorted(java.util.Comparator.comparingInt(GameResult.Standing::rank))
                .forEach(row -> rankings.add(new JLabel(row.rank() + "위   " + row.nickname())));
        rankings.revalidate();
        rankings.repaint();
    }

    @Override
    public JComponent view() { return this; }
}

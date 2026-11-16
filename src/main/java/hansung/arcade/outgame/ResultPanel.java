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
    private final JPanel rankings = new JPanel();

    public ResultPanel(SceneManager scenes) {
        super(new BorderLayout(20, 20));
        setPreferredSize(new Dimension(800, 600));
        rankings.setLayout(new javax.swing.BoxLayout(rankings, javax.swing.BoxLayout.Y_AXIS));
        setBackground(new java.awt.Color(0x00BCEB));
        rankings.setBackground(new java.awt.Color(0xB4EDFF));
        rankings.setBorder(BorderFactory.createCompoundBorder(LobbyArtwork.frame(5), BorderFactory.createEmptyBorder(15, 20, 15, 20)));
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
        JLabel time = new JLabel("플레이 시간  " + result.playSeconds() + "초");
        time.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        time.setForeground(new java.awt.Color(0x00558F));
        time.setAlignmentX(LEFT_ALIGNMENT);
        rankings.add(time);
        rankings.add(javax.swing.Box.createVerticalStrut(12));
        result.standings().stream().sorted(java.util.Comparator.comparingInt(GameResult.Standing::rank))
                .forEach(row -> {
                    JPanel line = new JPanel(new BorderLayout(16, 0));
                    line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 37));
                    line.setPreferredSize(new Dimension(500, 37));
                    line.setAlignmentX(LEFT_ALIGNMENT);
                    line.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
                    line.setBackground(new java.awt.Color(row.playerId() == result.winnerId() ? 0xFFF1A1 : 0x78DEFA));
                    JLabel rank = new JLabel(row.rank() + "위");
                    rank.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
                    JLabel name = new JLabel(row.nickname());
                    name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                    line.add(rank, BorderLayout.WEST);
                    line.add(name, BorderLayout.CENTER);
                    if (row.playerId() == result.winnerId()) {
                        line.add(new JLabel("WIN"), BorderLayout.EAST);
                    }
                    rankings.add(line);
                    rankings.add(javax.swing.Box.createVerticalStrut(3));
                });
        rankings.add(javax.swing.Box.createVerticalGlue());
        rankings.revalidate();
        rankings.repaint();
    }

    @Override
    public JComponent view() { return this; }
}

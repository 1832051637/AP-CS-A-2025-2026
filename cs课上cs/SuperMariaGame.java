import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class SuperMariaGame extends JFrame {
    private MenuPanel menuPanel;
    private GamePanel classicPanel;
    private ChaseGamePanel chasePanel;
    private int currentMode = -1; // -1 = menu, 0 = classic, 1 = chase

    public SuperMariaGame() {
        this.initUI();
    }

    private void initUI() {
        this.setTitle("Super Maria - Classic Mode");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        
        // Show menu first
        menuPanel = new MenuPanel(this);
        this.add(menuPanel);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        SwingUtilities.invokeLater(() -> menuPanel.requestFocusInWindow());
        
        try {
            // 默认背景音乐：Never Gonna Give You Up
            MusicPlayer.playBackgroundMusic();
            // 如果你想使用自己的背景音乐，调用下面这个接口：
            // MusicPlayer.playMusicFile("your-background-music.wav");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void startGame(int mode) {
        this.getContentPane().removeAll();
        currentMode = mode;
        
        if (mode == 0) {
            // Classic mode
            classicPanel = new GamePanel();
            this.add(classicPanel);
        } else if (mode == 1) {
            // Chase mode
            chasePanel = new ChaseGamePanel();
            this.add(chasePanel);
        }
        
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        SwingUtilities.invokeLater(() -> {
            if (mode == 0 && classicPanel != null) classicPanel.requestFocusInWindow();
            else if (mode == 1 && chasePanel != null) chasePanel.requestFocusInWindow();
        });
        this.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SuperMariaGame::new);
    }
}

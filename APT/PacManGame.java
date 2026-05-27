import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

public class PacManGame extends JFrame {
    public PacManGame() {
        setTitle("AP CSA Pac-Man - 逃生与捕猎双模式版");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(new GamePanel());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        new PacManGame();
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private final int TILE_SIZE = 24;
    private int ROWS = 35;
    private int COLS = 35;

    private final int STATE_MENU = 0;
    private final int STATE_PLAYING = 1;
    private final int STATE_GAMEOVER = 2;
    private final int STATE_WIN = 3; 
    private int currentState = STATE_MENU;

    private int gameMode = 1; // 1: 逃生模式, 2: 捕猎者模式
    private int difficultyLevel = 2; // 1:简单, 2:普通, 3:困难

    private int[][] map;

    // 玩家数据
    private int pacX, pacY;
    private int pacDX, pacDY;
    private int reqDX, reqDY;
    private final int baseSpeed = 3;
    private final int boostSpeed = 6;
    private int currentSpeed = 3;

    private int invincibleTicks = 0;
    private int attackTicks = 0;
    private int speedTicks = 0;
    private int playerFrozenTicks = 0; 
    private int shootCooldown = 0;
    private int animationTick = 0;
    private int respawnTicks = 0;

    // 生命系统与死亡特效
    private int lives = 3;
    private int maxLives = 3;
    private int deathEffectTicks = 0;
    private int deathX, deathY;

    // 暂停机制
    private JButton pauseButton;
    private boolean isPaused = false;

    private ArrayList<Point> trailHistory = new ArrayList<>();
    private final int MAX_TRAIL_SIZE = 6;

    // 阶段进度机制 (主要用于逃生模式)
    private int totalPellets = 0;
    private int eatenPellets = 0;
    private int activeGhosts = 2;
    private int gamePhase = 1; 
    private boolean ghostsEnraged = false;
    private boolean warningDisplay = false;
    private int warningTicks = 0;

    // 捕猎者模式专属
    private int hunterTicks = 0;
    private final int MAX_HUNTER_TICKS = 180 * 60; // 3分钟

    // 幽灵/NPC数据 (支持最大4个)
    private int[] ghostX = new int[4];
    private int[] ghostY = new int[4];
    private int[] ghostDX = new int[4];
    private int[] ghostDY = new int[4];
    private int[] ghostHP = new int[4];
    private int[] ghostSpeed = new int[4]; 
    private int[] ghostRespawnTimer = new int[4];
    
    // NPC道具状态
    private int[] ghostFrozen = new int[4];
    private int[] ghostInvincible = new int[4];
    private int[] ghostAttack = new int[4];
    private int[] ghostSpeedBoost = new int[4];
    private int[] ghostShootCooldown = new int[4];

    private Color[] ghostColors = {Color.RED, Color.PINK, Color.CYAN, Color.GREEN};

    private ArrayList<Bullet> bullets = new ArrayList<>();
    private int score;
    private Timer timer;

    public GamePanel() {
        setPreferredSize(new Dimension(COLS * TILE_SIZE, ROWS * TILE_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        setLayout(null);
        pauseButton = new JButton("暂停");
        pauseButton.setBounds(120, 14, 65, 22);
        pauseButton.setFocusable(false);
        pauseButton.setBackground(Color.BLACK);
        pauseButton.setForeground(Color.WHITE);
        pauseButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        pauseButton.addActionListener(e -> {
            if (currentState == STATE_PLAYING) {
                isPaused = !isPaused;
                pauseButton.setText(isPaused ? "继续" : "暂停");
            }
        });
        add(pauseButton);
        pauseButton.setVisible(false);

        timer = new Timer(16, this);
        timer.start();
    }

    private void generateRandomMap() {
        map = new int[ROWS][COLS];

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) map[r][c] = 1;
        }

        carveMaze(1, 1);

        for (int r = 1; r < ROWS - 1; r++) {
            for (int c = 1; c < COLS - 1; c++) {
                if (map[r][c] == 1) {
                    int adjacentPaths = 0;
                    if (map[r-1][c] == 0) adjacentPaths++;
                    if (map[r+1][c] == 0) adjacentPaths++;
                    if (map[r][c-1] == 0) adjacentPaths++;
                    if (map[r][c+1] == 0) adjacentPaths++;
                    if (adjacentPaths >= 2 && Math.random() < 0.40) map[r][c] = 0;
                }
            }
        }

        for (int i = 0; i < 12; i++) {
            int pr = 3 + (int)(Math.random() * (ROWS - 7));
            int pc = 3 + (int)(Math.random() * (COLS - 7));
            for (int r = pr; r < pr + 3; r++) {
                for (int c = pc; c < pc + 3; c++) map[r][c] = 0;
            }
        }

        int midR = ROWS / 2;
        for (int c = 0; c < COLS; c++) {
            map[midR][c] = 0;
            if(c < 3 || c > COLS - 4) {
                map[midR - 1][c] = 1;
                map[midR + 1][c] = 1;
            }
        }

        int midC = COLS / 2;
        for (int r = midR - 2; r <= midR + 2; r++) {
            for (int c = midC - 3; c <= midC + 3; c++) map[r][c] = 0;
        }

        totalPellets = 0;
        for (int r = 1; r < ROWS - 1; r++) {
            for (int c = 1; c < COLS - 1; c++) {
                if (r >= midR - 2 && r <= midR + 2 && c >= midC - 3 && c <= midC + 3) continue;
                if (r == midR && (c < 3 || c > COLS - 4)) continue;
                if (map[r][c] == 0) {
                    if (gameMode == 1) {
                        map[r][c] = 2; 
                        totalPellets++;
                    }
                }
            }
        }
        map[1][1] = 0;
    }

    private void carveMaze(int r, int c) {
        map[r][c] = 0;
        int[] dirs = {0, 1, 2, 3};
        for (int i = 0; i < 4; i++) {
            int j = (int)(Math.random() * 4);
            int temp = dirs[i]; dirs[i] = dirs[j]; dirs[j] = temp;
        }

        for (int i = 0; i < 4; i++) {
            int dr = 0, dc = 0;
            if (dirs[i] == 0) dr = -2; else if (dirs[i] == 1) dr = 2;
            else if (dirs[i] == 2) dc = -2; else if (dirs[i] == 3) dc = 2;

            int nr = r + dr, nc = c + dc;
            if (nr > 0 && nr < ROWS - 1 && nc > 0 && nc < COLS - 1) {
                if (map[nr][nc] == 1) {
                    map[r + dr/2][c + dc/2] = 0;
                    carveMaze(nr, nc);
                }
            }
        }
    }

    private void spawnRandomItem(int itemType) {
        ArrayList<Point> validSpots = new ArrayList<>();
        int midR = ROWS / 2, midC = COLS / 2;
        for (int r = 1; r < ROWS - 1; r++) {
            for (int c = 1; c < COLS - 1; c++) {
                if (r >= midR - 2 && r <= midR + 2 && c >= midC - 3 && c <= midC + 3) continue;
                if (r == midR && (c < 3 || c > COLS - 4)) continue;
                if (map[r][c] == 2 || map[r][c] == 0) validSpots.add(new Point(c, r));
            }
        }
        if (!validSpots.isEmpty()) {
            Point choice = validSpots.get((int)(Math.random() * validSpots.size()));
            map[choice.y][choice.x] = itemType;
        }
    }

    private void spawnExit() {
        ArrayList<Point> validSpots = new ArrayList<>();
        for (int r = 1; r < ROWS - 1; r++) {
            for (int c = 1; c < COLS - 1; c++) {
                if (map[r][c] == 0 || map[r][c] == 2) {
                    if (Math.abs(c * TILE_SIZE - pacX) > 10 * TILE_SIZE || Math.abs(r * TILE_SIZE - pacY) > 10 * TILE_SIZE) {
                        validSpots.add(new Point(c, r));
                    }
                }
            }
        }
        if (!validSpots.isEmpty()) {
            Point p = validSpots.get((int)(Math.random() * validSpots.size()));
            map[p.y][p.x] = 9; 
        }
    }

    private int getMapCell(int r, int c) {
        while (c < 0) c += COLS;
        while (c >= COLS) c -= COLS;
        if (r < 0 || r >= ROWS) return 1;
        return map[r][c];
    }

    private void drawHeartShape(Graphics2D g, int x, int y, int size) {
        g.fillArc(x, y, size / 2, size / 2, 0, 180);
        g.fillArc(x + size / 2, y, size / 2, size / 2, 0, 180);
        int[] xs = {x, x + size / 2, x + size};
        int[] ys = {y + size / 4, y + size, y + size / 4};
        g.fillPolygon(xs, ys, 3);
    }
    
    private void drawHeartOutline(Graphics2D g, int x, int y, int size) {
        g.drawArc(x, y, size / 2, size / 2, 0, 180);
        g.drawArc(x + size / 2, y, size / 2, size / 2, 0, 180);
        int[] xs = {x, x + size / 2, x + size};
        int[] ys = {y + size / 4, y + size, y + size / 4};
        g.drawPolygon(xs, ys, 3);
    }

    private void initGame(int diff, int mode) {
        difficultyLevel = diff;
        gameMode = mode;
        generateRandomMap();
        
        spawnRandomItem(3); spawnRandomItem(4); spawnRandomItem(5); spawnRandomItem(6);
        if (gameMode == 2) {
            spawnRandomItem(7);
            spawnRandomItem(7);
        }

        setPreferredSize(new Dimension(COLS * TILE_SIZE, ROWS * TILE_SIZE));
        Container topLevel = getTopLevelAncestor();
        if (topLevel instanceof Window) ((Window) topLevel).pack();

        pacX = 1 * TILE_SIZE; pacY = 1 * TILE_SIZE;
        pacDX = 0; pacDY = 0; reqDX = 0; reqDY = 0;
        
        if (gameMode == 1) {
            maxLives = 3;
            lives = 3;
            activeGhosts = 2;
            gamePhase = 1;
        } else {
            maxLives = 10;
            lives = 10;
            activeGhosts = 4;
            hunterTicks = MAX_HUNTER_TICKS;
        }

        eatenPellets = 0;
        ghostsEnraged = false;
        warningTicks = 0;
        warningDisplay = false;
        deathEffectTicks = 0;
        isPaused = false;
        pauseButton.setText("暂停");

        invincibleTicks = 0; attackTicks = 0; speedTicks = 0; playerFrozenTicks = 0; 
        shootCooldown = 0; respawnTicks = 0;
        bullets.clear(); trailHistory.clear();

        int midR = ROWS / 2, midC = COLS / 2;
        ghostX[0] = (midC - 1) * TILE_SIZE; ghostY[0] = midR * TILE_SIZE;
        ghostX[1] = midC * TILE_SIZE;       ghostY[1] = midR * TILE_SIZE;
        ghostX[2] = (midC + 1) * TILE_SIZE; ghostY[2] = midR * TILE_SIZE;
        ghostX[3] = midC * TILE_SIZE;       ghostY[3] = (midR - 1) * TILE_SIZE;

        for (int i = 0; i < 4; i++) {
            ghostDX[i] = 0; ghostDY[i] = 0;
            ghostHP[i] = 3;
            ghostRespawnTimer[i] = 0;
            ghostSpeed[i] = (gameMode == 2) ? 3 : difficultyLevel; 
            ghostFrozen[i] = 0; ghostInvincible[i] = 0; ghostAttack[i] = 0;
            ghostSpeedBoost[i] = 0; ghostShootCooldown[i] = 0;
        }

        score = 0;
        currentState = STATE_PLAYING;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        pauseButton.setVisible(currentState == STATE_PLAYING);

        if (currentState == STATE_MENU) { drawMenu(g2d); return; }
        if (currentState == STATE_GAMEOVER) { drawGameOver(g2d); return; }
        if (currentState == STATE_WIN) { drawWin(g2d); return; }

        drawMapAndItems(g2d);
        drawPlayer(g2d);
        drawGhosts(g2d);
        drawBullets(g2d);
        drawDeathEffect(g2d);
        drawUI(g2d);

        if (isPaused) {
            g2d.setColor(new Color(0, 0, 0, 170));
            g2d.fillRect(0, 0, COLS * TILE_SIZE, ROWS * TILE_SIZE);
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Microsoft YaHei", Font.BOLD, 36));
            g2d.drawString("游戏已暂停", COLS * TILE_SIZE / 2 - 90, ROWS * TILE_SIZE / 2);
        }
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(Color.YELLOW); g.setFont(new Font("Microsoft YaHei", Font.BOLD, 36));
        g.drawString("双子星计划：逃生与捕猎", 180, 160);
        
        g.setColor(Color.WHITE); g.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        g.drawString("== 模式1: 逃生模式 (躲避幽灵，寻找出口) ==", 180, 240);
        g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 18));
        g.setColor(Color.GREEN); g.drawString("按 1 - 简单 (较慢幽灵, 基础AI)", 220, 280);
        g.setColor(Color.ORANGE); g.drawString("按 2 - 普通 (标准速度, 进阶AI)", 220, 310);
        g.setColor(Color.RED); g.drawString("按 3 - 困难 (极速幽灵, 致命AI)", 220, 340);

        g.setColor(Color.WHITE); g.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        g.drawString("== 模式2: 捕猎者模式 (抓捕4个逃逸NPC) ==", 180, 420);
        g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 18));
        g.setColor(Color.GREEN); g.drawString("按 4 - 简单 (3分钟限时, NPC基础AI)", 220, 460);
        g.setColor(Color.ORANGE); g.drawString("按 5 - 普通 (3分钟限时, NPC狡猾AI)", 220, 490);
        g.setColor(Color.RED); g.drawString("按 6 - 困难 (3分钟限时, NPC极速逃逸)", 220, 520);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(Color.RED); g.setFont(new Font("Arial", Font.BOLD, 45));
        if (gameMode == 2 && hunterTicks <= 0) {
            g.drawString("TIME'S UP!", COLS * TILE_SIZE / 2 - 120, ROWS * TILE_SIZE / 2 - 40);
        } else {
            g.drawString("GAME OVER", COLS * TILE_SIZE / 2 - 130, ROWS * TILE_SIZE / 2 - 40);
        }
        g.setColor(Color.WHITE); g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 20));
        g.drawString("最终得分: " + score, COLS * TILE_SIZE / 2 - 60, ROWS * TILE_SIZE / 2 + 10);
        g.drawString("按 'R' 键返回主菜单", COLS * TILE_SIZE / 2 - 90, ROWS * TILE_SIZE / 2 + 60);
    }

    private void drawWin(Graphics2D g) {
        g.setColor(Color.GREEN); g.setFont(new Font("Arial", Font.BOLD, 45));
        g.drawString(gameMode == 1 ? "YOU ESCAPED!" : "ALL CAUGHT!", COLS * TILE_SIZE / 2 - 160, ROWS * TILE_SIZE / 2 - 40);
        g.setColor(Color.WHITE); g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 20));
        g.drawString("最终得分: " + score, COLS * TILE_SIZE / 2 - 60, ROWS * TILE_SIZE / 2 + 10);
        g.drawString("按 'R' 键进入下一局", COLS * TILE_SIZE / 2 - 100, ROWS * TILE_SIZE / 2 + 60);
    }

    private void drawMapAndItems(Graphics2D g) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int cell = map[r][c];
                int x = c * TILE_SIZE, y = r * TILE_SIZE;

                if (r == ROWS / 2 && (cell == 0 || cell == 2)) {
                    g.setColor(new Color(50, 0, 100, 120));
                    g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                }

                if (cell == 1) {
                    g.setColor(new Color(20, 20, 20)); g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                    g.setColor(new Color(60, 60, 60)); g.drawRect(x, y, TILE_SIZE, TILE_SIZE);
                } else if (cell == 2) {
                    g.setColor(Color.LIGHT_GRAY); g.fillOval(x + 10, y + 10, 4, 4);
                } else if (cell == 3) {
                    g.setColor(Color.GREEN); g.fillOval(x + 6, y + 6, 12, 12);
                } else if (cell == 4) {
                    g.setColor(Color.ORANGE); g.fillOval(x + 6, y + 6, 12, 12);
                } else if (cell == 5) {
                    g.setColor(Color.CYAN); g.fillOval(x + 6, y + 6, 12, 12);
                } else if (cell == 6) {
                    g.setColor(Color.RED); drawHeartShape(g, x + 4, y + 4, 16);
                    g.setColor(new Color(255, 255, 255, 180)); drawHeartOutline(g, x + 4, y + 4, 16);
                } else if (cell == 7) { 
                    g.setColor(new Color(138, 43, 226)); 
                    g.fillOval(x + 5, y + 5, 14, 14);
                    g.setColor(Color.WHITE);
                    g.drawOval(x + 5, y + 5, 14, 14);
                } else if (cell == 9) {
                    g.setColor(new Color(50, 255, 100)); g.fillOval(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4);
                    int pulse = (int) (4 * Math.sin(animationTick * 0.2));
                    g.setColor(Color.WHITE); g.drawOval(x + 4 - pulse, y + 4 - pulse, TILE_SIZE - 8 + pulse*2, TILE_SIZE - 8 + pulse*2);
                    g.setFont(new Font("Arial", Font.BOLD, 9)); g.drawString("EXIT", x + 2, y + 15);
                }
            }
        }
    }

    private void drawPlayer(Graphics2D g) {
        animationTick++;

        if (speedTicks > 0) {
            for (int i = 0; i < trailHistory.size(); i++) {
                Point p = trailHistory.get(i);
                float alpha = (float) (i + 1) / (trailHistory.size() + 1) * 0.4f;
                g.setColor(new Color(0, 255, 255, (int) (alpha * 255)));
                g.fillOval(p.x + 2, p.y + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            }
        }

        if (playerFrozenTicks > 0) {
            g.setColor(new Color(0, 255, 255, 150));
            g.fillRect(pacX - 2, pacY - 2, TILE_SIZE + 4, TILE_SIZE + 4);
            g.setColor(Color.WHITE);
            g.drawRect(pacX - 2, pacY - 2, TILE_SIZE + 4, TILE_SIZE + 4);
        } else if (attackTicks > 0) {
            Graphics2D g4 = (Graphics2D) g.create();
            g4.setColor(Color.ORANGE); g4.setStroke(new BasicStroke(2.0f));
            int pulse = (int) (4 * Math.sin(animationTick * 0.2));
            g4.drawOval(pacX - 4 - pulse, pacY - 4 - pulse, TILE_SIZE + 8 + pulse * 2, TILE_SIZE + 8 + pulse * 2);
            g4.dispose();
        }

        if (invincibleTicks > 0) g.setColor(Color.getHSBColor((animationTick * 5 % 360) / 360.0f, 1.0f, 1.0f));
        else g.setColor(Color.YELLOW);

        int startAngle = 0;
        int extentAngle = ((animationTick / 6) % 2 == 0) ? 300 : 360;
        if (pacDX == 1) startAngle = 30; else if (pacDX == -1) startAngle = 210;
        else if (pacDY == -1) startAngle = 120; else if (pacDY == 1) startAngle = 300; else startAngle = 30;

        if (extentAngle == 360) g.fillOval(pacX + 2, pacY + 2, TILE_SIZE - 4, TILE_SIZE - 4);
        else g.fillArc(pacX + 2, pacY + 2, TILE_SIZE - 4, TILE_SIZE - 4, startAngle, extentAngle);
    }

    private void drawGhosts(Graphics2D g) {
        for (int i = 0; i < activeGhosts; i++) {
            if (ghostRespawnTimer[i] > 0) {
                g.setColor(new Color(ghostColors[i].getRed(), ghostColors[i].getGreen(), ghostColors[i].getBlue(), 120));
                g.setFont(new Font("Arial", Font.BOLD, 14));
                int secondsLeft = (ghostRespawnTimer[i] / 60) + 1;
                g.drawString(secondsLeft + "s", ghostX[i] + 4, ghostY[i] + 16);
                continue;
            }

            if (ghostHP[i] <= 0 && gameMode == 2) continue;

            int gx = ghostX[i], gy = ghostY[i];

            if (ghostFrozen[i] > 0) {
                g.setColor(new Color(0, 255, 255, 150));
                g.fillRect(gx - 2, gy - 2, TILE_SIZE + 4, TILE_SIZE + 4);
            }
            if (ghostAttack[i] > 0) {
                g.setColor(Color.ORANGE);
                g.drawOval(gx - 2, gy - 2, TILE_SIZE + 4, TILE_SIZE + 4);
            }

            if (ghostInvincible[i] > 0) g.setColor(Color.getHSBColor((animationTick * 5 % 360) / 360.0f, 1.0f, 1.0f));
            else if (ghostsEnraged && (animationTick % 10 < 5)) g.setColor(Color.WHITE);
            else g.setColor(ghostColors[i]);

            g.fillOval(gx + 2, gy + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            g.fillRect(gx + 2, gy + TILE_SIZE / 2, TILE_SIZE - 4, TILE_SIZE / 2 - 2);
            
            g.setColor(Color.WHITE);
            g.fillOval(gx + 4, gy + 6, 6, 6); g.fillOval(gx + 14, gy + 6, 6, 6);
            g.setColor(ghostsEnraged ? Color.RED : Color.BLUE);
            g.fillOval(gx + 6, gy + 8, 2, 2); g.fillOval(gx + 16, gy + 8, 2, 2);

            g.setColor(Color.DARK_GRAY); g.fillRect(gx, gy - 6, TILE_SIZE, 4);
            g.setColor(Color.GREEN); g.fillRect(gx, gy - 6, (int) (TILE_SIZE * (ghostHP[i] / 3.0)), 4);
        }
    }

    private void drawBullets(Graphics2D g) {
        for (Bullet b : bullets) {
            g.setColor(b.owner == 0 ? Color.RED : new Color(138, 43, 226)); 
            g.fillOval(b.x + 8, b.y + 8, 8, 8);
        }
    }

    private void drawDeathEffect(Graphics2D g) {
        if (deathEffectTicks > 0) {
            g.setStroke(new BasicStroke(3.0f));
            int radius = (40 - deathEffectTicks) * 3;
            g.setColor(new Color(255, 50, 50, (int) (deathEffectTicks / 40.0 * 255)));
            g.drawOval(deathX + TILE_SIZE / 2 - radius, deathY + TILE_SIZE / 2 - radius, radius * 2, radius * 2);

            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(255, 200, 50, (int) (deathEffectTicks / 40.0 * 255)));
            for (int angle = 0; angle < 360; angle += 30) {
                double rad = Math.toRadians(angle);
                int x1 = (int) (deathX + TILE_SIZE / 2 + (radius * 0.4) * Math.cos(rad));
                int y1 = (int) (deathY + TILE_SIZE / 2 + (radius * 0.4) * Math.sin(rad));
                int x2 = (int) (deathX + TILE_SIZE / 2 + radius * Math.cos(rad));
                int y2 = (int) (deathY + TILE_SIZE / 2 + radius * Math.sin(rad));
                g.drawLine(x1, y1, x2, y2);
            }
        }
    }

    private void drawUI(Graphics2D g) {
        g.setColor(Color.WHITE); g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Score: " + score, 20, 22);
        
        g.setColor(Color.RED);
        int displayLives = Math.min(lives, 15); 
        for (int i = 0; i < displayLives; i++) {
            drawHeartShape(g, 20 + i * 20, 28, 16);
        }

        if (gameMode == 1) {
            g.setColor(Color.LIGHT_GRAY);
            g.drawString(String.format("Phase %d Progress: %d / %d", gamePhase, eatenPellets, totalPellets), 200, 22);
        } else {
            g.setColor(Color.YELLOW);
            int sec = (hunterTicks / 60) % 60;
            int min = (hunterTicks / 60) / 60;
            g.drawString(String.format("Time Left: %02d:%02d", min, sec), 200, 22);
            int alive = 0;
            for(int h : ghostHP) if(h>0) alive++;
            g.setColor(Color.CYAN);
            g.drawString("Targets Left: " + alive + " / 4", 350, 22);
        }

        int uiX = COLS * TILE_SIZE - 220;
        if (warningDisplay) {
            g.setColor(Color.RED);
            if ((warningTicks / 10) % 2 == 0) {
                if (gamePhase == 2) g.drawString("⚠️ PHASE 2: 3 GHOSTS ACTIVE! ⚠️", uiX - 50, 55);
                else if (gamePhase == 3) g.drawString("⚠️ ENRAGED! FIND THE EXIT! ⚠️", uiX - 50, 55);
            }
        }

        int barY = 12;
        if (invincibleTicks > 0) {
            g.setColor(Color.WHITE); g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("INVINC", uiX, barY + 9);
            g.setColor(Color.DARK_GRAY); g.fillRect(uiX + 60, barY, 90, 10);
            g.setColor(Color.getHSBColor((animationTick * 3 % 360) / 360.0f, 0.8f, 1.0f));
            g.fillRect(uiX + 60, barY, (int) (90 * (invincibleTicks / 180.0)), 10);
            barY += 18;
        }
        if (attackTicks > 0) {
            g.setColor(Color.WHITE); g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("WEAPON", uiX, barY + 9);
            g.setColor(Color.DARK_GRAY); g.fillRect(uiX + 60, barY, 90, 10);
            g.setColor(Color.ORANGE); g.fillRect(uiX + 60, barY, (int) (90 * (attackTicks / 360.0)), 10);
            barY += 18;
        }
        if (speedTicks > 0) {
            g.setColor(Color.WHITE); g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("SPEED", uiX, barY + 9);
            g.setColor(Color.DARK_GRAY); g.fillRect(uiX + 60, barY, 90, 10);
            g.setColor(Color.CYAN); g.fillRect(uiX + 60, barY, (int) (90 * (speedTicks / 240.0)), 10);
        }
    }

    private void checkPhaseProgress() {
        if (gameMode != 1) return;
        if (gamePhase == 1 && eatenPellets >= totalPellets / 3) {
            gamePhase = 2; activeGhosts = 3; 
            warningDisplay = true; warningTicks = 180;
        } else if (gamePhase == 2 && eatenPellets >= totalPellets * 2 / 3) {
            gamePhase = 3; ghostsEnraged = true; 
            spawnExit(); 
            warningDisplay = true; warningTicks = 180;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentState != STATE_PLAYING) { repaint(); return; }
        if (isPaused) { repaint(); return; }

        if (deathEffectTicks > 0) deathEffectTicks--;
        if (invincibleTicks > 0) invincibleTicks--;
        if (attackTicks > 0) attackTicks--;
        if (speedTicks > 0) speedTicks--;
        if (shootCooldown > 0) shootCooldown--;
        if (warningTicks > 0) { warningTicks--; if (warningTicks == 0) warningDisplay = false; }

        if (gameMode == 2) {
            hunterTicks--;
            if (hunterTicks <= 0) { currentState = STATE_GAMEOVER; repaint(); return; }
        }

        if (speedTicks > 0) {
            trailHistory.add(new Point(pacX, pacY));
            if (trailHistory.size() > MAX_TRAIL_SIZE) trailHistory.remove(0);
        } else trailHistory.clear();

        respawnTicks++;
        if (respawnTicks >= 500) {
            respawnTicks = 0;
            if (gameMode == 2) {
                // 降低禁锢球刷新率：10%刷禁锢球，90%刷常规属性球
                if (Math.random() < 0.10) {
                    spawnRandomItem(7);
                } else {
                    spawnRandomItem(3 + (int)(Math.random() * 4)); 
                }
            } else {
                spawnRandomItem(3 + (int)(Math.random() * 4)); 
            }
        }

        // ==========================================
        // 1. 玩家移动与状态触发
        // ==========================================
        if (playerFrozenTicks > 0) {
            playerFrozenTicks--;
        } else {
            if ((reqDX == -pacDX && reqDX != 0) || (reqDY == -pacDY && reqDY != 0)) {
                pacDX = reqDX; pacDY = reqDY;
            }

            if (pacX % TILE_SIZE == 0 && pacY % TILE_SIZE == 0) {
                int cX = pacX / TILE_SIZE, cY = pacY / TILE_SIZE;
                currentSpeed = (speedTicks > 0) ? boostSpeed : baseSpeed;

                int tile = getMapCell(cY, cX);
                if (tile == 2) {
                    map[cY][cX] = 0; score += 10; eatenPellets++; checkPhaseProgress(); 
                }
                else if (tile == 3) { map[cY][cX] = 0; invincibleTicks = 300; }
                else if (tile == 4) { map[cY][cX] = 0; attackTicks = 360; }
                else if (tile == 5) { map[cY][cX] = 0; speedTicks = 240; }
                else if (tile == 6) { 
                    map[cY][cX] = 0; 
                    if (lives < maxLives) lives++; 
                    score += 50; 
                }
                else if (tile == 7 && gameMode == 2) {
                    map[cY][cX] = 0;
                    ArrayList<Integer> alive = new ArrayList<>();
                    for(int i=0; i<activeGhosts; i++) if(ghostHP[i]>0) alive.add(i);
                    if(!alive.isEmpty()){
                        int target = alive.get((int)(Math.random()*alive.size()));
                        ghostFrozen[target] = 180;
                    }
                }
                else if (tile == 9 && gameMode == 1) { 
                    currentState = STATE_WIN; repaint(); return;
                }

                if (getMapCell(cY + reqDY, cX + reqDX) != 1) { pacDX = reqDX; pacDY = reqDY; }
                if (getMapCell(cY + pacDY, cX + pacDX) == 1) { pacDX = 0; pacDY = 0; }
            }
            pacX += pacDX * currentSpeed; pacY += pacDY * currentSpeed;
            if (pacX < 0) pacX += COLS * TILE_SIZE;
            else if (pacX >= COLS * TILE_SIZE) pacX -= COLS * TILE_SIZE;
        }

        // ==========================================
        // 2. 子弹逻辑 (增加生命周期)
        // ==========================================
        for (int bIdx = bullets.size() - 1; bIdx >= 0; bIdx--) {
            Bullet b = bullets.get(bIdx); b.move();
            
            // 超出持续时间则消失
            if (b.lifeTicks <= 0) {
                bullets.remove(bIdx); continue;
            }

            if (b.x < 0) b.x += COLS * TILE_SIZE;
            else if (b.x >= COLS * TILE_SIZE) b.x -= COLS * TILE_SIZE;

            int bGridX = b.x / TILE_SIZE, bGridY = b.y / TILE_SIZE;
            if (bGridX >= 0 && bGridX < COLS && bGridY >= 0 && bGridY < ROWS && map[bGridY][bGridX] == 1) {
                bullets.remove(bIdx); continue;
            }

            boolean removed = false;
            // 判定击中NPC
            if (b.owner == 0) {
                for (int i = 0; i < activeGhosts; i++) {
                    if (ghostHP[i] <= 0 || ghostRespawnTimer[i] > 0 || ghostInvincible[i] > 0) continue;

                    if (Math.abs(b.x - ghostX[i]) < 20 && Math.abs(b.y - ghostY[i]) < 20) {
                        ghostHP[i]--; bullets.remove(bIdx); removed = true;
                        if (ghostHP[i] <= 0) {
                            score += 200;
                            if (gameMode == 1) {
                                ghostX[i] = (COLS / 2) * TILE_SIZE; ghostY[i] = (ROWS / 2) * TILE_SIZE;
                                ghostRespawnTimer[i] = 600; // 修改为 10秒复活 (60FPS * 10)
                            }
                        }
                        break;
                    }
                }
            } else if (b.owner == 1) { // 判定击中玩家
                if (Math.abs(b.x - pacX) < 20 && Math.abs(b.y - pacY) < 20) {
                    bullets.remove(bIdx); removed = true;
                    if (invincibleTicks == 0) {
                        lives--; deathX = pacX; deathY = pacY; deathEffectTicks = 40;
                        if (lives <= 0) currentState = STATE_GAMEOVER;
                        else invincibleTicks = 180; // 玩家无敌时间修改为 3 秒 (60FPS * 3)
                    }
                }
            }
            if(removed) continue;
        }

        // ==========================================
        // 3. AI寻路与状态
        // ==========================================
        double smartChanceBase = (difficultyLevel == 1) ? 0.30 : (difficultyLevel == 2 ? 0.50 : 0.70);
        double smartChance = ghostsEnraged ? Math.min(1.0, smartChanceBase + 0.3) : smartChanceBase;

        int aliveCount = 0;
        for (int i = 0; i < activeGhosts; i++) {
            if (ghostRespawnTimer[i] > 0) {
                ghostRespawnTimer[i]--;
                if (ghostRespawnTimer[i] <= 0) ghostHP[i] = 3; 
                continue; 
            }

            if (ghostHP[i] <= 0) continue; 
            aliveCount++;

            if (ghostFrozen[i] > 0) ghostFrozen[i]--;
            if (ghostInvincible[i] > 0) ghostInvincible[i]--;
            if (ghostAttack[i] > 0) ghostAttack[i]--;
            if (ghostSpeedBoost[i] > 0) ghostSpeedBoost[i]--;
            if (ghostShootCooldown[i] > 0) ghostShootCooldown[i]--;

            if (ghostFrozen[i] > 0) continue; 

            // 开火逻辑 (优化：增大索敌范围并确保一直线)
            if (gameMode == 2 && ghostAttack[i] > 0 && ghostShootCooldown[i] == 0) {
                boolean canShoot = false;
                int gdx = 0, gdy = 0;
                // 距离5格之内且同一直线
                if (Math.abs(pacX - ghostX[i]) < 20 && Math.abs(pacY - ghostY[i]) < 5 * TILE_SIZE) {
                    canShoot = true; gdy = (pacY > ghostY[i]) ? 1 : -1;
                } else if (Math.abs(pacY - ghostY[i]) < 20 && Math.abs(pacX - ghostX[i]) < 5 * TILE_SIZE) {
                    canShoot = true; gdx = (pacX > ghostX[i]) ? 1 : -1;
                }
                
                if (canShoot) {
                    bullets.add(new Bullet(ghostX[i], ghostY[i], gdx, gdy, 1));
                    ghostShootCooldown[i] = 60;
                }
            }

            if (ghostX[i] % TILE_SIZE == 0 && ghostY[i] % TILE_SIZE == 0) {
                if (gameMode == 1) ghostSpeed[i] = ghostsEnraged ? difficultyLevel + 1 : difficultyLevel;
                else ghostSpeed[i] = ghostSpeedBoost[i] > 0 ? boostSpeed : baseSpeed;

                int gX = ghostX[i] / TILE_SIZE, gY = ghostY[i] / TILE_SIZE;
                int pGridX = pacX / TILE_SIZE, pGridY = pacY / TILE_SIZE;

                if (gameMode == 2) {
                    int tile = getMapCell(gY, gX);
                    if (tile >= 3 && tile <= 7) {
                        map[gY][gX] = 0;
                        if (tile == 3) ghostInvincible[i] = 300;
                        else if (tile == 4) ghostAttack[i] = 300;
                        else if (tile == 5) ghostSpeedBoost[i] = 240;
                        else if (tile == 6 && ghostHP[i] < 3) ghostHP[i]++;
                        else if (tile == 7) playerFrozenTicks = 180;
                    }
                }

                int nextGDX = 0, nextGDY = 0; boolean moved = false;

                if (Math.random() < smartChance) {
                    int diffX = pGridX - gX, diffY = pGridY - gY;
                    
                    if (gameMode == 2) { 
                        int distToPlayer = Math.abs(diffX) + Math.abs(diffY);
                        if (distToPlayer < 12) { 
                            // 玩家距离较近，优先反向躲避
                            diffX = -diffX; diffY = -diffY; 
                        } else {
                            // 安全时主动寻找最近的道具
                            int closestDist = 999;
                            int targetX = -1, targetY = -1;
                            for (int r = 1; r < ROWS - 1; r++) {
                                for (int c = 1; c < COLS - 1; c++) {
                                    int tile = map[r][c];
                                    if (tile >= 3 && tile <= 7) {
                                        int d = Math.abs(c - gX) + Math.abs(r - gY);
                                        // 寻找 20 格探测范围内的道具
                                        if (d < closestDist && d < 20) {
                                            closestDist = d; targetX = c; targetY = r;
                                        }
                                    }
                                }
                            }
                            if (targetX != -1) { // 找到道具，改变意图去吃道具
                                diffX = targetX - gX; diffY = targetY - gY;
                            } else {
                                diffX = 0; diffY = 0; // 没有道具则随机游走
                            }
                        }
                    }

                    if (diffX != 0 || diffY != 0) {
                        if (Math.abs(diffX) > Math.abs(diffY)) {
                            int tryDX = (diffX > 0) ? 1 : -1;
                            if (getMapCell(gY, gX + tryDX) != 1) { nextGDX = tryDX; moved = true; }
                            else {
                                int tryDY = (diffY > 0) ? 1 : (diffY < 0 ? -1 : 0);
                                if (tryDY != 0 && getMapCell(gY + tryDY, gX) != 1) { nextGDY = tryDY; moved = true; }
                            }
                        } else {
                            int tryDY = (diffY > 0) ? 1 : -1;
                            if (getMapCell(gY + tryDY, gX) != 1) { nextGDY = tryDY; moved = true; }
                            else {
                                int tryDX = (diffX > 0) ? 1 : (diffX < 0 ? -1 : 0);
                                if (tryDX != 0 && getMapCell(gY, gX + tryDX) != 1) { nextGDX = tryDX; moved = true; }
                            }
                        }
                    }
                }

                if (!moved) {
                    int[] dxOptions = {0, 0, -1, 1}, dyOptions = {-1, 1, 0, 0};
                    ArrayList<Integer> validChoices = new ArrayList<>();
                    for (int d = 0; d < 4; d++) {
                        if (getMapCell(gY + dyOptions[d], gX + dxOptions[d]) != 1 && 
                            (dxOptions[d] != -ghostDX[i] || dyOptions[d] != -ghostDY[i])) {
                            validChoices.add(d);
                        }
                    }
                    if (validChoices.isEmpty()) {
                        for (int d = 0; d < 4; d++)
                            if (getMapCell(gY + dyOptions[d], gX + dxOptions[d]) != 1) validChoices.add(d);
                    }
                    if (!validChoices.isEmpty()) {
                        int choice = validChoices.get((int)(Math.random() * validChoices.size()));
                        nextGDX = dxOptions[choice]; nextGDY = dyOptions[choice];
                    }
                }
                ghostDX[i] = nextGDX; ghostDY[i] = nextGDY;
            }

            ghostX[i] += ghostDX[i] * ghostSpeed[i];
            ghostY[i] += ghostDY[i] * ghostSpeed[i];

            if (ghostX[i] < 0) ghostX[i] += COLS * TILE_SIZE;
            else if (ghostX[i] >= COLS * TILE_SIZE) ghostX[i] -= COLS * TILE_SIZE;

            // 碰撞判定
            if (Math.abs(pacX - ghostX[i]) < 18 && Math.abs(pacY - ghostY[i]) < 18) {
                if (gameMode == 1) {
                    if (invincibleTicks == 0) {
                        lives--; deathX = pacX; deathY = pacY; deathEffectTicks = 40;
                        if (lives <= 0) currentState = STATE_GAMEOVER;
                        else invincibleTicks = 180; // 修改为 3秒无敌
                    }
                } else {
                    if (ghostInvincible[i] == 0) {
                        ghostHP[i] = 0; // 捕获
                        score += 500;
                    } else if (invincibleTicks == 0) {
                        lives--; deathX = pacX; deathY = pacY; deathEffectTicks = 40;
                        if (lives <= 0) currentState = STATE_GAMEOVER;
                        else invincibleTicks = 180; // 修改为 3秒无敌
                    }
                }
            }
        }
        
        if (gameMode == 2 && aliveCount == 0 && currentState == STATE_PLAYING) {
            currentState = STATE_WIN;
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (currentState == STATE_MENU) {
            if (key == KeyEvent.VK_1) { initGame(1, 1); }
            else if (key == KeyEvent.VK_2) { initGame(2, 1); }
            else if (key == KeyEvent.VK_3) { initGame(3, 1); }
            else if (key == KeyEvent.VK_4) { initGame(1, 2); }
            else if (key == KeyEvent.VK_5) { initGame(2, 2); }
            else if (key == KeyEvent.VK_6) { initGame(3, 2); }
            return;
        }
        if ((currentState == STATE_GAMEOVER || currentState == STATE_WIN) && key == KeyEvent.VK_R) {
            currentState = STATE_MENU; return;
        }

        if (currentState == STATE_PLAYING) {
            if (key == KeyEvent.VK_P) {
                isPaused = !isPaused;
                pauseButton.setText(isPaused ? "继续" : "暂停");
                return;
            }
            if (isPaused) return; 

            if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) { reqDX = 0; reqDY = -1; }
            if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) { reqDX = 0; reqDY = 1; }
            if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) { reqDX = -1; reqDY = 0; }
            if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) { reqDX = 1; reqDY = 0; }
            if (key == KeyEvent.VK_SPACE && attackTicks > 0 && shootCooldown == 0 && playerFrozenTicks == 0) {
                bullets.add(new Bullet(pacX, pacY, (pacDX == 0 && pacDY == 0) ? 1 : pacDX, pacDY, 0));
                shootCooldown = 12;
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}

class Bullet {
    int x, y, dx, dy, owner, lifeTicks; // 增加 lifeTicks 属性
    public Bullet(int x, int y, int dx, int dy, int owner) { 
        this.x = x; this.y = y; this.dx = dx; this.dy = dy; this.owner = owner; 
        this.lifeTicks = 300; // 5秒存活 (60 FPS * 5)
    }
    public void move() { 
        this.x += dx * 10; 
        this.y += dy * 10; 
        this.lifeTicks--; // 每次移动扣减生命周期
    }
}
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * 森林冰火人 - Java版双人合作平台游戏（多关卡版）
 * 火人(红色): 方向键控制 (← → 移动, ↑ 跳跃)
 * 冰人(蓝色): WASD控制 (A/D 移动, W 跳跃)
 * 按R键重新开始当前关卡，通关后按N键进入下一关
 */
public class ForestFireAndIce extends JFrame {

    public ForestFireAndIce() {
        setTitle("🌲 森林冰火人 🔥💧");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        gamePanel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ForestFireAndIce());
    }
}

// ==================== 游戏面板 ====================
class GamePanel extends JPanel {
    public static final int TILE_SIZE = 40;
    public static final int COLS = 20;
    public static final int ROWS = 15;
    public static final int WIDTH = COLS * TILE_SIZE;
    public static final int HEIGHT = ROWS * TILE_SIZE;

    // ---------- 多关卡地图定义 ----------
    // 0=空, 1=平台, 2=火池(冰人死), 3=水池(火人死), 4=毒液(都死), 5=火门, 6=冰门, 7=钻石
    private final int[][][] levels = {
        // ========== 第1关：初入森林 (原始地图) ==========
        {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6,0,1},
            {1,1,1,1,0,0,0,0,0,0,0,0,7,0,0,0,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,1},
            {1,0,0,1,1,0,0,0,0,7,0,0,0,7,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,1,1,1,0,0,0,0,0,0,0,0,0,0,0,1,1,0,1},
            {1,0,0,0,0,0,0,1,1,0,0,0,1,1,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,2,2,1,1,1,3,3,1,1,1,1,1,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
        },
        // ========== 第2关：分道扬镳 ==========
        {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,6,0,0,0,5,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0,1},
            {1,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,1,0,0,0,1,1,0,0,0,1,1,0,0,0,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,7,0,0,7,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,1,1,0,0,0,0,0,0,1,1,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,1,0,0,1,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,2,2,2,1,4,4,1,3,3,3,1,1,1,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
        },
        // ========== 第3关：冰火九重天 ==========
        {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,1},
            {1,0,0,0,0,0,1,1,0,0,0,0,0,0,1,1,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,7,0,0,7,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,1,1,1,1,1,1,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,1,1,1,0,0,0,0,0,0,0,0,0,1,1,1,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,5,0,0,0,0,0,0,1,0,0,1,0,0,0,0,6,0,1},
            {1,1,1,1,1,0,2,2,0,1,0,0,1,0,3,3,0,1,1,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
        },
        // ========== 第4关：绝命毒池 ==========
        {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0,1},
            {1,0,7,0,0,0,0,0,0,0,4,4,0,0,0,0,0,7,0,1},
            {1,0,0,0,0,0,0,0,0,0,4,4,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,1,1,0,0,0,0,7,0,0,7,0,0,0,0,1,1,0},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,1,1,1,0,0,1,1,1,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,5,0,0,0,0,0,0,1,0,0,1,0,0,0,0,6,0,1},
            {1,1,1,1,1,1,0,2,0,1,0,0,1,0,3,0,1,1,1,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
        },
        // ========== 第5关：终极试炼 ==========
        {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,1,1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,1,1,0,0,0,7,0,0,0,1,1,0,0,0,1},
            {1,0,0,0,0,0,0,0,1,1,1,1,1,1,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,4,4,0,0,0,0,0,0,0,1},
            {1,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,1,1,0,1},
            {1,0,0,0,0,0,0,0,0,7,0,0,7,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,1,1,1,0,0,1,1,1,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,5,0,0,0,0,0,0,1,0,0,1,0,0,0,0,6,0,1},
            {1,1,1,1,1,1,0,2,0,1,0,0,1,0,3,0,1,1,1,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
        }
    };

    // 各关卡角色起始位置（火人x,y ; 冰人x,y）
    private final int[][] fireStarts = {
        {80, 440}, {80, 440}, {80, 440}, {80, 440}, {80, 440}
    };
    private final int[][] iceStarts = {
        {680, 440}, {680, 440}, {680, 440}, {680, 440}, {680, 440}
    };

    private int currentLevel = 0;   // 当前关卡索引
    private int totalLevels = levels.length;

    private int[][] currentMap;
    private Player fireBoy;
    private Player iceGirl;
    private boolean fireBoyAtDoor = false;
    private boolean iceGirlAtDoor = false;
    private boolean gameWon = false;
    private int diamondsCollected = 0;
    private int totalDiamonds = 0;
    private String message = "";
    private int messageTimer = 0;

    // 按键状态
    private final Set<Integer> keysPressed = new HashSet<>();

    // 游戏计时器
    private final Timer gameTimer;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(20, 30, 20));
        setFocusable(true);

        // 初始化当前关卡地图
        currentMap = new int[ROWS][COLS];
        loadLevel(currentLevel);

        // 创建角色
        fireBoy = new Player(fireStarts[currentLevel][0], fireStarts[currentLevel][1], Color.RED, "火人", true);
        iceGirl = new Player(iceStarts[currentLevel][0], iceStarts[currentLevel][1], new Color(50, 150, 255), "冰人", false);

        // 键盘监听
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keysPressed.add(e.getKeyCode());
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    resetGame();
                }
                if (e.getKeyCode() == KeyEvent.VK_N && gameWon && currentLevel < totalLevels - 1) {
                    nextLevel();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keysPressed.remove(e.getKeyCode());
            }
        });

        // 游戏循环 (~60 FPS)
        gameTimer = new Timer(16, e -> gameLoop());
        gameTimer.start();
    }

    private void loadLevel(int levelIndex) {
        for (int row = 0; row < ROWS; row++) {
            System.arraycopy(levels[levelIndex][row], 0, currentMap[row], 0, COLS);
        }
        // 重新统计钻石总数
        totalDiamonds = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (currentMap[row][col] == 7) totalDiamonds++;
            }
        }
    }

    private void resetGame() {
        loadLevel(currentLevel);
        fireBoy.reset(fireStarts[currentLevel][0], fireStarts[currentLevel][1]);
        iceGirl.reset(iceStarts[currentLevel][0], iceStarts[currentLevel][1]);
        fireBoyAtDoor = false;
        iceGirlAtDoor = false;
        gameWon = false;
        diamondsCollected = 0;
        message = "💎 第" + (currentLevel+1) + "关 - 收集所有钻石，然后两人到达各自的门！";
        messageTimer = 180;
    }

    private void nextLevel() {
        currentLevel++;
        loadLevel(currentLevel);
        fireBoy.reset(fireStarts[currentLevel][0], fireStarts[currentLevel][1]);
        iceGirl.reset(iceStarts[currentLevel][0], iceStarts[currentLevel][1]);
        fireBoyAtDoor = false;
        iceGirlAtDoor = false;
        gameWon = false;
        diamondsCollected = 0;
        message = "💎 第" + (currentLevel+1) + "关 - 收集所有钻石，然后两人到达各自的门！";
        messageTimer = 180;
    }

    private void gameLoop() {
        // 胜利后也可以处理输入（R/N）
        if (gameWon) {
            if (messageTimer > 0 && messageTimer < Integer.MAX_VALUE) {
                messageTimer--;
                if (messageTimer == 0) message = "";
            }
            repaint();
            return;
        }

        handleInput();

        fireBoy.update(currentMap);
        iceGirl.update(currentMap);

        checkHazards();
        checkDoors();
        checkDiamondCollection();

        if (fireBoyAtDoor && iceGirlAtDoor && diamondsCollected == totalDiamonds) {
            gameWon = true;
            if (currentLevel < totalLevels - 1) {
                message = "🎉 通关！按 N 进入下一关，按 R 重玩本关";
            } else {
                message = "🏆 恭喜！你已完成所有关卡！按 R 重新开始";
            }
            messageTimer = Integer.MAX_VALUE;
        }

        if (messageTimer > 0 && messageTimer < Integer.MAX_VALUE) {
            messageTimer--;
            if (messageTimer == 0) message = "";
        }

        repaint();
    }

    private void handleInput() {
        if (keysPressed.contains(KeyEvent.VK_LEFT))  fireBoy.moveLeft();
        if (keysPressed.contains(KeyEvent.VK_RIGHT)) fireBoy.moveRight();
        if (keysPressed.contains(KeyEvent.VK_UP))    fireBoy.jump();

        if (keysPressed.contains(KeyEvent.VK_A))     iceGirl.moveLeft();
        if (keysPressed.contains(KeyEvent.VK_D))     iceGirl.moveRight();
        if (keysPressed.contains(KeyEvent.VK_W))     iceGirl.jump();
    }

    private void checkHazards() {
        boolean fireDead = isPlayerOnHazard(fireBoy, true);
        boolean iceDead = isPlayerOnHazard(iceGirl, false);
        if (fireDead || iceDead) {
            String who = fireDead && iceDead ? "两位角色都" : (fireDead ? "火人" : "冰人");
            message = "💀 " + who + "碰到了危险液体！按R重新开始";
            messageTimer = 120;
            resetGame(); // 直接重置本关
        }
    }

    private boolean isPlayerOnHazard(Player player, boolean isFire) {
        int left = player.getX();
        int right = player.getX() + player.getWidth();
        int top = player.getY();
        int bottom = player.getY() + player.getHeight();
        int leftCol = left / TILE_SIZE;
        int rightCol = (right - 1) / TILE_SIZE;
        int topRow = top / TILE_SIZE;
        int bottomRow = (bottom - 1) / TILE_SIZE;
        leftCol = Math.max(0, leftCol);
        rightCol = Math.min(COLS - 1, rightCol);
        topRow = Math.max(0, topRow);
        bottomRow = Math.min(ROWS - 1, bottomRow);
        for (int row = topRow; row <= bottomRow; row++) {
            for (int col = leftCol; col <= rightCol; col++) {
                int tile = currentMap[row][col];
                if (tile == 4) return true;
                if (isFire && tile == 3) return true;
                if (!isFire && tile == 2) return true;
            }
        }
        return false;
    }

    private void checkDoors() {
        fireBoyAtDoor = isPlayerOnTile(fireBoy, 5);
        iceGirlAtDoor = isPlayerOnTile(iceGirl, 6);

        if (diamondsCollected < totalDiamonds) {
            if ((fireBoyAtDoor || iceGirlAtDoor) && messageTimer == 0) {
                int left = totalDiamonds - diamondsCollected;
                message = "💎 还需收集 " + left + " 颗钻石！";
                messageTimer = 60;
            }
            return;
        }

        if (fireBoyAtDoor && !iceGirlAtDoor && messageTimer == 0) {
            message = "🔥 火人已到达出口，等待冰人...";
            messageTimer = 60;
        }
        if (iceGirlAtDoor && !fireBoyAtDoor && messageTimer == 0) {
            message = "💧 冰人已到达出口，等待火人...";
            messageTimer = 60;
        }
    }

    private boolean isPlayerOnTile(Player player, int tileType) {
        int left = player.getX();
        int right = player.getX() + player.getWidth();
        int top = player.getY();
        int bottom = player.getY() + player.getHeight();
        int leftCol = left / TILE_SIZE;
        int rightCol = (right - 1) / TILE_SIZE;
        int topRow = top / TILE_SIZE;
        int bottomRow = bottom / TILE_SIZE;  // 脚底所在行纳入检测
        leftCol = Math.max(0, leftCol);
        rightCol = Math.min(COLS - 1, rightCol);
        topRow = Math.max(0, topRow);
        bottomRow = Math.min(ROWS - 1, bottomRow);
        for (int row = topRow; row <= bottomRow; row++) {
            for (int col = leftCol; col <= rightCol; col++) {
                if (currentMap[row][col] == tileType) return true;
            }
        }
        return false;
    }

    private void checkDiamondCollection() {
        collectDiamond(fireBoy);
        collectDiamond(iceGirl);
    }

    private void collectDiamond(Player player) {
        int cx = (player.getX() + player.getWidth() / 2) / TILE_SIZE;
        int cy = (player.getY() + player.getHeight() / 2) / TILE_SIZE;
        if (cx >= 0 && cx < COLS && cy >= 0 && cy < ROWS) {
            if (currentMap[cy][cx] == 7) {
                currentMap[cy][cx] = 0;
                diamondsCollected++;
            }
        }
    }

    // ================== 绘制部分 (保持不变，只UI添加关卡显示) ==================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(g2d);
        drawMap(g2d);
        fireBoy.draw(g2d);
        iceGirl.draw(g2d);
        drawUI(g2d);
        if (!message.isEmpty()) {
            drawMessage(g2d);
        }
    }

    private void drawBackground(Graphics2D g) {
        GradientPaint sky = new GradientPaint(0,0,new Color(15,25,40),0,HEIGHT,new Color(25,45,30));
        g.setPaint(sky);
        g.fillRect(0,0,WIDTH,HEIGHT);
        g.setColor(new Color(255,255,255,30));
        for (int i=0;i<40;i++) {
            int sx = (i*197+31)%WIDTH;
            int sy = (i*149+67)%(HEIGHT/2);
            g.fillOval(sx,sy,2,2);
        }
    }

    private void drawMap(Graphics2D g) {
        for (int row=0;row<ROWS;row++) {
            for (int col=0;col<COLS;col++) {
                int x=col*TILE_SIZE, y=row*TILE_SIZE;
                int tile=currentMap[row][col];
                switch(tile) {
                    case 1: drawPlatform(g,x,y); break;
                    case 2: drawFirePool(g,x,y); break;
                    case 3: drawWaterPool(g,x,y); break;
                    case 4: drawPoisonPool(g,x,y); break;
                    case 5: drawPlatform(g,x,y); drawFireDoor(g,x,y); break;
                    case 6: drawPlatform(g,x,y); drawIceDoor(g,x,y); break;
                    case 7: drawDiamond(g,x,y); break;
                }
            }
        }
    }

    private void drawPlatform(Graphics2D g, int x, int y) { /* 与原代码相同 */
        g.setColor(new Color(90,70,50));
        g.fillRect(x+1,y+1,TILE_SIZE-2,TILE_SIZE-2);
        g.setColor(new Color(130,105,75));
        g.fillRect(x+2,y+2,TILE_SIZE-4,8);
        g.setColor(new Color(70,50,35));
        g.fillRect(x+5,y+14,TILE_SIZE-10,2);
        g.fillRect(x+8,y+20,TILE_SIZE-16,2);
        g.fillRect(x+3,y+26,TILE_SIZE-6,2);
        g.setColor(new Color(50,35,25));
        g.drawRect(x+1,y+1,TILE_SIZE-3,TILE_SIZE-3);
    }
    private void drawFirePool(Graphics2D g, int x, int y) { /* 同原 */ 
        g.setColor(new Color(180,30,10)); g.fillRect(x+2,y+2,TILE_SIZE-4,TILE_SIZE-4);
        g.setColor(new Color(255,100,20)); for(int i=0;i<4;i++) g.fillOval(x+5+i*8,y+5+(i%2)*10,10,18);
        g.setColor(new Color(255,200,50)); for(int i=0;i<3;i++) g.fillOval(x+8+i*10,y+8+(i%2)*6,6,10);
        g.setColor(new Color(140,20,5)); g.drawRect(x+2,y+2,TILE_SIZE-5,TILE_SIZE-5);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,10));
        g.drawString("火",x+TILE_SIZE/2-6,y+TILE_SIZE/2+4);
    }
    private void drawWaterPool(Graphics2D g, int x, int y) {
        g.setColor(new Color(20,100,200)); g.fillRect(x+2,y+2,TILE_SIZE-4,TILE_SIZE-4);
        g.setColor(new Color(80,160,255)); for(int i=0;i<3;i++) g.fillOval(x+4+i*12,y+10+(i%2)*8,14,6);
        g.setColor(new Color(150,210,255,150)); g.fillRect(x+3,y+3,TILE_SIZE-6,6);
        g.setColor(new Color(10,60,150)); g.drawRect(x+2,y+2,TILE_SIZE-5,TILE_SIZE-5);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,10));
        g.drawString("水",x+TILE_SIZE/2-6,y+TILE_SIZE/2+4);
    }
    private void drawPoisonPool(Graphics2D g, int x, int y) {
        g.setColor(new Color(30,160,30)); g.fillRect(x+2,y+2,TILE_SIZE-4,TILE_SIZE-4);
        g.setColor(new Color(80,220,80)); for(int i=0;i<5;i++) g.fillOval(x+4+(i*7)%32,y+4+(i*11)%28,6,6);
        g.setColor(new Color(20,100,20)); g.drawRect(x+2,y+2,TILE_SIZE-5,TILE_SIZE-5);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,10));
        g.drawString("毒",x+TILE_SIZE/2-6,y+TILE_SIZE/2+4);
    }
    private void drawFireDoor(Graphics2D g, int x, int y) {
        g.setColor(new Color(200,80,20)); g.fillRect(x+6,y+2,TILE_SIZE-12,TILE_SIZE-4);
        g.setColor(new Color(255,200,50)); g.fillRect(x+10,y+6,TILE_SIZE-20,TILE_SIZE-10);
        g.setColor(new Color(255,60,10)); g.fillOval(x+14,y+10,12,16);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,9));
        g.drawString("火",x+16,y+22);
    }
    private void drawIceDoor(Graphics2D g, int x, int y) {
        g.setColor(new Color(30,100,200)); g.fillRect(x+6,y+2,TILE_SIZE-12,TILE_SIZE-4);
        g.setColor(new Color(150,220,255)); g.fillRect(x+10,y+6,TILE_SIZE-20,TILE_SIZE-10);
        g.setColor(new Color(50,150,255)); g.fillOval(x+14,y+10,12,16);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,9));
        g.drawString("冰",x+16,y+22);
    }
    private void drawDiamond(Graphics2D g, int x, int y) {
        int cx=x+TILE_SIZE/2, cy=y+TILE_SIZE/2, size=12;
        g.setColor(new Color(255,255,200,80));
        g.fillOval(cx-size-4,cy-size-4,(size+4)*2,(size+4)*2);
        int[] xp={cx,cx+size,cx,cx-size}, yp={cy-size,cy,cy+size,cy};
        g.setColor(new Color(255,240,100)); g.fillPolygon(xp,yp,4);
        g.setColor(new Color(255,255,220));
        int[] hx={cx,cx+size/2,cx,cx-size/2}, hy={cy-size,cy-size/4,cy,cy-size/4};
        g.fillPolygon(hx,hy,4);
        g.setColor(new Color(200,180,50)); g.drawPolygon(xp,yp,4);
    }

    private void drawUI(Graphics2D g) {
        g.setColor(new Color(0,0,0,150));
        g.fillRect(0,0,WIDTH,36);
        g.setColor(new Color(255,255,255,50));
        g.drawLine(0,36,WIDTH,36);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif",Font.BOLD,16));
        g.drawString("🌲 森林冰火人",10,25);
        g.setColor(new Color(255,240,100));
        g.setFont(new Font("SansSerif",Font.BOLD,14));
        g.drawString("💎 " + diamondsCollected + "/" + totalDiamonds, WIDTH-180,25);
        g.setColor(new Color(200,200,200));
        g.setFont(new Font("SansSerif",Font.PLAIN,11));
        g.drawString("🔥方向键 | 💧WASD | R=重置", WIDTH-270,25);
        // 关卡显示
        g.setColor(Color.ORANGE);
        g.drawString("第"+(currentLevel+1)+"/"+totalLevels+"关", WIDTH/2-30,25);

        int indicatorY=42;
        g.setFont(new Font("SansSerif",Font.BOLD,12));
        // 火人状态
        if (diamondsCollected < totalDiamonds) {
            g.setColor(new Color(100,100,100,180));
            g.fillRoundRect(10,indicatorY,100,20,10,10);
            g.setColor(Color.WHITE); g.drawString("💎 收集钻石",20,indicatorY+15);
        } else if (fireBoyAtDoor) {
            g.setColor(new Color(255,200,50));
            g.fillRoundRect(10,indicatorY,100,20,10,10);
            g.setColor(Color.BLACK); g.drawString("🔥 已到达",20,indicatorY+15);
        } else {
            g.setColor(new Color(100,100,100,180));
            g.fillRoundRect(10,indicatorY,100,20,10,10);
            g.setColor(Color.WHITE); g.drawString("🔥 寻找出口",20,indicatorY+15);
        }
        // 冰人状态
        if (diamondsCollected < totalDiamonds) {
            g.setColor(new Color(100,100,100,180));
            g.fillRoundRect(WIDTH-120,indicatorY,110,20,10,10);
            g.setColor(Color.WHITE); g.drawString("💎 收集钻石",WIDTH-110,indicatorY+15);
        } else if (iceGirlAtDoor) {
            g.setColor(new Color(150,220,255));
            g.fillRoundRect(WIDTH-120,indicatorY,110,20,10,10);
            g.setColor(Color.BLACK); g.drawString("💧 已到达",WIDTH-110,indicatorY+15);
        } else {
            g.setColor(new Color(100,100,100,180));
            g.fillRoundRect(WIDTH-120,indicatorY,110,20,10,10);
            g.setColor(Color.WHITE); g.drawString("💧 寻找出口",WIDTH-110,indicatorY+15);
        }
    }

    private void drawMessage(Graphics2D g) {
        int mw=500, mh=50, mx=(WIDTH-mw)/2, my=HEIGHT/2-mh/2;
        g.setColor(new Color(0,0,0,200));
        g.fillRoundRect(mx,my,mw,mh,20,20);
        g.setColor(new Color(255,255,255,100));
        g.drawRoundRect(mx,my,mw,mh,20,20);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif",Font.BOLD,15));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(message);
        g.drawString(message, (WIDTH-tw)/2, my+mh/2+5);
    }
}

// ==================== 玩家角色类（完整修复版） ====================
class Player {
    private static final int WIDTH = 30;
    private static final int HEIGHT = 38;
    private static final double MOVE_SPEED = 5.5;
    private static final double JUMP_SPEED = -15.5;
    private static final double GRAVITY = 0.75;
    private static final double MAX_FALL_SPEED = 16.0;

    private double x, y;
    private double velX, velY;
    private boolean onGround;
    private final Color color, darkerColor, lighterColor;
    private final String name;
    private final boolean isFireBoy;
    private final int startX, startY;

    public Player(int startX, int startY, Color color, String name, boolean isFireBoy) {
        this.startX = startX; this.startY = startY;
        this.color = color; this.name = name; this.isFireBoy = isFireBoy;
        this.darkerColor = color.darker(); this.lighterColor = color.brighter();
        reset(startX, startY);
    }

    public void reset(int rx, int ry) {
        x = rx; y = ry; velX = 0; velY = 0; onGround = false;
    }

    public int getX() { return (int)x; }
    public int getY() { return (int)y; }
    public int getWidth() { return WIDTH; }
    public int getHeight() { return HEIGHT; }

    public void moveLeft() { velX = -MOVE_SPEED; }
    public void moveRight() { velX = MOVE_SPEED; }

    public void jump() {
        if (onGround) { velY = JUMP_SPEED; onGround = false; }
    }

    public void update(int[][] map) {
        int ts = GamePanel.TILE_SIZE, cols = GamePanel.COLS, rows = GamePanel.ROWS;
        if (!onGround) { velY += GRAVITY; if (velY > MAX_FALL_SPEED) velY = MAX_FALL_SPEED; }
        else velY = 0;

        // 水平
        double nx = x + velX;
        if (velX != 0) {
            int lc = (int)nx/ts, rc = (int)(nx+WIDTH-1)/ts, tr = (int)y/ts, br = (int)(y+HEIGHT-1)/ts;
            lc=Math.max(0,lc); rc=Math.min(cols-1,rc); tr=Math.max(0,tr); br=Math.min(rows-1,br);
            boolean col = false;
            for(int r=tr;r<=br;r++) for(int c=lc;c<=rc;c++) if(isSolid(map[r][c])) { col=true; break; }
            if(col) {
                if(velX>0) nx = rc*ts - WIDTH - 0.01;
                else nx = (lc+1)*ts + 0.01;
                velX = 0;
            }
            x = nx;
        }

        // 垂直
        double ny = y + velY;
        int lc=(int)x/ts, rc=(int)(x+WIDTH-1)/ts, tr=(int)ny/ts, br=(int)(ny+HEIGHT-1)/ts;
        lc=Math.max(0,lc); rc=Math.min(cols-1,rc); tr=Math.max(0,tr); br=Math.min(rows-1,br);
        boolean vcol = false;
        for(int r=tr;r<=br;r++) for(int c=lc;c<=rc;c++) if(isSolid(map[r][c])) { vcol=true; break; }

        if(vcol) {
            if(velY > 0) { // 落地，找精确固体行
                int solidR = tr;
                for(int r=tr;r<=br;r++) {
                    boolean found=false;
                    for(int c=lc;c<=rc;c++) if(isSolid(map[r][c])) { solidR=r; found=true; break; }
                    if(found) break;
                }
                ny = solidR*ts - HEIGHT;
                velY=0; onGround=true;
            } else { // 撞顶
                int solidR = br;
                for(int r=br;r>=tr;r--) {
                    boolean found=false;
                    for(int c=lc;c<=rc;c++) if(isSolid(map[r][c])) { solidR=r; found=true; break; }
                    if(found) break;
                }
                ny = (solidR+1)*ts;
                velY=0;
            }
        } else onGround = false;
        y = ny;

        // 边缘安全
        if(velY==0 && !onGround) {
            int footY = (int)(y+HEIGHT), chkR = footY/ts;
            if(chkR>=0 && chkR<rows)
                for(int c=lc;c<=rc;c++)
                    if(isSolid(map[chkR][c]) && Math.abs(y+HEIGHT-chkR*ts)<2.0) {
                        onGround=true; y=chkR*ts-HEIGHT; break;
                    }
        }

        if(onGround && velX!=0) { velX*=0.85; if(Math.abs(velX)<0.1) velX=0; }

        if(x<0){x=0;velX=0;} if(x+WIDTH>cols*ts){x=cols*ts-WIDTH;velX=0;}
        if(y<0){y=0;velY=0;} if(y+HEIGHT>rows*ts){y=rows*ts-HEIGHT;velY=0;onGround=true;}
    }

    private boolean isSolid(int tile) { return tile==1 || tile==5 || tile==6; }

    public void draw(Graphics2D g) {
        int ix=(int)x, iy=(int)y;
        g.setColor(new Color(0,0,0,80));
        g.fillOval(ix+3,iy+HEIGHT-2,WIDTH-6,6);
        g.setColor(color);
        g.fillRoundRect(ix+3,iy+8,WIDTH-6,HEIGHT-10,8,8);
        g.setColor(lighterColor);
        g.fillOval(ix+5,iy,WIDTH-10,16);
        g.setColor(Color.WHITE);
        g.fillOval(ix+9,iy+4,6,6); g.fillOval(ix+17,iy+4,6,6);
        g.setColor(Color.BLACK);
        g.fillOval(ix+11,iy+5,3,4); g.fillOval(ix+19,iy+5,3,4);
        g.setColor(darkerColor);
        g.drawLine(ix+12,iy+13,ix+18,iy+13);
        g.setColor(color);
        g.fillRoundRect(ix,iy+12,5,14,3,3); g.fillRoundRect(ix+WIDTH-5,iy+12,5,14,3,3);
        g.setColor(darkerColor);
        g.fillRoundRect(ix+6,iy+HEIGHT-10,7,10,3,3); g.fillRoundRect(ix+WIDTH-13,iy+HEIGHT-10,7,10,3,3);
        if(isFireBoy) {
            g.setColor(new Color(255,150,20,180)); g.fillOval(ix+9,iy-5,7,8);
            g.setColor(new Color(255,200,50,150)); g.fillOval(ix+11,iy-7,4,6);
        } else {
            g.setColor(new Color(180,220,255,180)); g.fillOval(ix+10,iy-4,5,5);
            g.setColor(new Color(220,240,255,150)); g.fillOval(ix+13,iy-6,3,4);
        }
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,9));
        FontMetrics fm=g.getFontMetrics(); int nw=fm.stringWidth(name);
        g.drawString(name, ix+(WIDTH-nw)/2, iy-4);
    }
}
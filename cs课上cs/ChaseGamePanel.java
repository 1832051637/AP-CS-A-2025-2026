import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class ChaseGamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int PANEL_W = 1000;
    private static final int PANEL_H = 700;
    private static final int PLAYER_SIZE = 36;
    private static final int GROUND_Y = PANEL_H - 70;

    // Player state
    private double playerX = 150;
    private double playerY = GROUND_Y - PLAYER_SIZE;
    private double playerDx = 0, playerDy = 0;
    private boolean onGround = true;
    private boolean movingLeft = false, movingRight = false;
    private boolean jumpRequested = false;
    private boolean shootRequested = false;
    private int shootCooldown = 0;
    private int currentDamage = 1;
    private int damageBoostTimer = 0;

    // AI Chaser (when player is caught, they become the chaser)
    private double chaserX = 800;
    private double chaserY = GROUND_Y - PLAYER_SIZE;
    private boolean isPlayerCaught = false;

    // Game objects
    private Boss boss;
    private List<Bullet> bullets = new ArrayList<>();
    private List<DamageBoost> boosts = new ArrayList<>();
    private List<Rectangle> platforms;
    private List<Rectangle> groundTiles;

    // Game state
    private boolean gameRunning = true;
    private boolean gameWon = false;
    private int score = 0;
    private Random rand = new Random();
    private Timer gameTimer;
    private int boostSpawnCounter = 0;

    public ChaseGamePanel() {
        setPreferredSize(new Dimension(PANEL_W, PANEL_H));
        setBackground(new Color(80, 180, 255));
        setFocusable(true);
        addKeyListener(this);
        initWorld();
        gameTimer = new Timer(16, this);
        gameTimer.start();
    }

    private void initWorld() {
        platforms = new ArrayList<>();
        groundTiles = new ArrayList<>();
        bullets.clear();
        boosts.clear();

        // Simple ground
        groundTiles.add(new Rectangle(0, GROUND_Y, PANEL_W, 70));

        // Create boss
        boss = new Boss(500, GROUND_Y - 100);

        // Add some platforms
        platforms.add(new Rectangle(300, 500, 150, 24));
        platforms.add(new Rectangle(550, 400, 150, 24));
        platforms.add(new Rectangle(150, 350, 150, 24));
        platforms.add(new Rectangle(700, 300, 150, 24));
    }

    private void updateGame() {
        if (!gameRunning) return;

        if (isPlayerCaught) {
            updateChaser();
            updateAI();
        } else {
            updatePlayerFighting();
            boss.update();
        }

        // Handle shooting
        if (shootRequested && shootCooldown <= 0 && !isPlayerCaught) {
            int dir = (playerDx >= 0) ? 1 : -1;
            int bulletX = (int) playerX + (dir > 0 ? PLAYER_SIZE : 0);
            int bulletY = (int) playerY + PLAYER_SIZE / 2;
            bullets.add(new Bullet(bulletX, bulletY, dir, currentDamage));
            shootCooldown = 8;
        }
        if (shootCooldown > 0) shootCooldown--;

        // Update bullets
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet b = bulletIter.next();
            b.update();
            if (b.x < 0 || b.x > PANEL_W) {
                bulletIter.remove();
            } else if (!isPlayerCaught && boss.getBounds().intersects(b.getBounds())) {
                boss.takeDamage(b.damage);
                bulletIter.remove();
                score += 10 * b.damage;
            }
        }

        // Update boosts
        Iterator<DamageBoost> boostIter = boosts.iterator();
        while (boostIter.hasNext()) {
            DamageBoost boost = boostIter.next();
            if (new Rectangle((int) playerX, (int) playerY, PLAYER_SIZE, PLAYER_SIZE).intersects(boost.getBounds())) {
                currentDamage = boost.multiplier;
                damageBoostTimer = 300;
                boostIter.remove();
                score += 50;
            }
        }

        // Update damage boost timer
        if (damageBoostTimer > 0) {
            damageBoostTimer--;
            if (damageBoostTimer == 0) {
                currentDamage = 1;
            }
        }

        // Spawn boosts randomly
        boostSpawnCounter++;
        if (boostSpawnCounter > 120) {
            if (boosts.size() < 3) {
                int bx = 100 + rand.nextInt(PANEL_W - 200);
                int by = 200 + rand.nextInt(200);
                int mult = 2 + rand.nextInt(3);
                boosts.add(new DamageBoost(bx, by, mult));
                boostSpawnCounter = 0;
            }
        }

        // Check boss defeated
        if (!boss.isAlive() && !gameWon) {
            gameWon = true;
            gameRunning = false;
        }

        // Check if player caught
        if (!isPlayerCaught && boss.getBounds().intersects(new Rectangle((int) playerX, (int) playerY, PLAYER_SIZE, PLAYER_SIZE))) {
            isPlayerCaught = true;
            chaserX = playerX;
            chaserY = playerY;
            playerX = 100;
            playerY = GROUND_Y - PLAYER_SIZE;
        }

        // Check if player catches AI
        if (isPlayerCaught && new Rectangle((int) playerX, (int) playerY, PLAYER_SIZE, PLAYER_SIZE)
                .intersects(new Rectangle((int) chaserX, (int) chaserY, PLAYER_SIZE, PLAYER_SIZE))) {
            isPlayerCaught = false;
            playerX = 100;
            playerY = GROUND_Y - PLAYER_SIZE;
        }
    }

    private void updatePlayerFighting() {
        playerDy += 0.6;
        onGround = false;

        if (movingLeft) playerDx = Math.max(-5.2, playerDx - 0.85);
        else if (movingRight) playerDx = Math.min(5.2, playerDx + 0.85);
        else playerDx *= 0.5;

        playerX += playerDx;
        playerY += playerDy;

        // Collision with ground and platforms
        Rectangle playerRect = new Rectangle((int) playerX, (int) playerY, PLAYER_SIZE, PLAYER_SIZE);
        for (Rectangle g : groundTiles) {
            if (playerRect.intersects(g)) {
                if (playerDy > 0) {
                    playerY = g.y - PLAYER_SIZE;
                    playerDy = 0;
                    onGround = true;
                }
            }
        }
        for (Rectangle p : platforms) {
            if (playerRect.intersects(p)) {
                if (playerDy > 0) {
                    playerY = p.y - PLAYER_SIZE;
                    playerDy = 0;
                    onGround = true;
                }
            }
        }

        // Jump
        if (jumpRequested && onGround) {
            playerDy = -11.5;
            onGround = false;
            jumpRequested = false;
        }

        if (playerX < 0) playerX = 0;
        if (playerX + PLAYER_SIZE > PANEL_W) playerX = PANEL_W - PLAYER_SIZE;
    }

    private void updateChaser() {
        updatePlayerFighting();
    }

    private void updateAI() {
        int aiTargetX = boss.x + boss.width / 2;
        double moveSpeed = 2.0;

        if (chaserX < aiTargetX - 10) {
            chaserX += moveSpeed;
        } else if (chaserX > aiTargetX + 10) {
            chaserX -= moveSpeed;
        }

        if (chaserX < 0) chaserX = 0;
        if (chaserX + PLAYER_SIZE > PANEL_W) chaserX = PANEL_W - PLAYER_SIZE;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning) updateGame();
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        GradientPaint skyGrad = new GradientPaint(0, 0, new Color(80, 180, 255), 0, PANEL_H, new Color(160, 220, 255));
        g2d.setPaint(skyGrad);
        g2d.fillRect(0, 0, PANEL_W, PANEL_H);

        // Ground
        for (Rectangle ground : groundTiles) {
            g2d.setColor(new Color(30, 120, 30));
            g2d.fillRect(ground.x, ground.y, ground.width, ground.height);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(ground.x, ground.y, ground.width, ground.height);
        }

        // Platforms
        for (Rectangle p : platforms) {
            g2d.setColor(new Color(210, 140, 70));
            g2d.fillRect(p.x, p.y, p.width, p.height);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(p.x, p.y, p.width, p.height);
        }

        // Boosts
        for (DamageBoost boost : boosts) {
            boost.draw(g2d);
        }

        // Bullets
        for (Bullet b : bullets) {
            b.draw(g2d);
        }

        // Boss (if not caught)
        if (!isPlayerCaught) {
            boss.draw(g2d);
        }

        // Draw AI chaser (if player is caught)
        if (isPlayerCaught) {
            drawCharacter(g2d, (int) chaserX, (int) chaserY, new Color(100, 200, 100), "AI");
        }

        // Player
        Color playerColor = isPlayerCaught ? new Color(100, 100, 200) : new Color(210, 50, 50);
        drawCharacter(g2d, (int) playerX, (int) playerY, playerColor, isPlayerCaught ? "YOU" : "P");

        // UI
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("SCORE: " + score, 20, 40);

        if (!isPlayerCaught) {
            g2d.setColor(Color.WHITE);
            g2d.drawString("DAMAGE: " + currentDamage + "x", 20, 70);
            if (damageBoostTimer > 0) {
                g2d.setColor(Color.MAGENTA);
                g2d.drawString("BOOST: " + (damageBoostTimer / 10) + "s", 20, 100);
            }
        } else {
            g2d.setColor(Color.CYAN);
            g2d.drawString("CHASING MODE - CATCH THE AI!", 300, 40);
        }

        // Game over / win screen
        if (gameWon) {
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.setColor(Color.GREEN);
            g2d.drawString("YOU WIN!", 350, 350);
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            g2d.drawString("Final Score: " + score, 350, 420);
        } else if (!gameRunning) {
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.setColor(Color.RED);
            g2d.drawString("BOSS STILL ALIVE!", 250, 350);
        }

        // Controls
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(Color.WHITE);
        g2d.drawString("← → to move  |  SPACE to jump  |  CTRL to shoot", 20, PANEL_H - 10);
    }

    private void drawCharacter(Graphics2D g, int x, int y, Color color, String label) {
        g.setColor(color);
        g.fillRect(x, y + PLAYER_SIZE / 3, PLAYER_SIZE, PLAYER_SIZE * 2 / 3);
        g.setColor(new Color(255, 200, 120));
        g.fillOval(x + 2, y - 6, PLAYER_SIZE - 4, PLAYER_SIZE - 8);
        g.setColor(Color.BLACK);
        g.fillOval(x + 8, y, 6, 6);
        g.fillOval(x + PLAYER_SIZE - 14, y, 6, 6);
        g.setColor(Color.RED);
        g.fillRect(x + 4, y - 12, PLAYER_SIZE - 8, 12);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) movingLeft = true;
        else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) movingRight = true;
        else if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_UP) jumpRequested = true;
        else if (code == KeyEvent.VK_CONTROL) shootRequested = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) movingLeft = false;
        else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) movingRight = false;
        else if (code == KeyEvent.VK_CONTROL) shootRequested = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}

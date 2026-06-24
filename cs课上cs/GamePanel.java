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

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    // 常量
    private static final int PANEL_W = 1000;
    private static final int PANEL_H = 700;
    private static final int PLAYER_SIZE = 36;
    private static final double GRAVITY = 0.6;
    private static final double JUMP_STRENGTH = -11.5;
    private static final int GROUND_Y = PANEL_H - 70;
    private static final int CHUNK_W = 800;
    private static final int PRE_GEN_DIST = 8;

    // 玩家属性
    private double playerX = 150;
    private double playerY = GROUND_Y - PLAYER_SIZE;
    private double playerDx = 0, playerDy = 0;
    private boolean onGround = true;
    private boolean movingLeft = false, movingRight = false;
    private int jumpsRemaining = 0;
    private static final int MAX_JUMPS = 2;
    private int coyoteTimer = 0;
    private boolean jumpRequested = false;
    private static final int COYOTE_TIME = 7;

    // 能力状态
    private boolean invincible = false;
    private int invincibleTimer = 0;
    private int invinciblePulse = 0;
    private boolean speedBoostActive = false;
    private int speedBoostTimer = 0;
    private static final int SPEED_BOOST_DURATION = 300;
    private double speedMultiplier = 1.0;
    private boolean giantActive = false;
    private int giantTimer = 0;
    private static final int GIANT_DURATION = 300;

    private boolean facingRight = true;
    private int lives = 3;
    private int score = 0;
    private boolean gameRunning = true;
    private int progressPoints = 0;
    private static final int PROGRESS_THRESHOLD = 1000;

    // 世界物体
    private List<Rectangle> platforms;
    private List<Rectangle> groundTiles;
    private List<MovingPlatform> movingPlatforms;
    private List<Enemy> enemies;
    private List<Bird> birds;
    private List<Spike> spikes;
    private List<Collectible> collectibles;
    private List<SpeedBlock> speedBlocks;
    private List<BounceMushroom> mushrooms;

    private BossMonster boss;

    private int screenShakeX = 0, screenShakeY = 0;
    private int shakeTimer = 0;
    private int furthestGeneratedX = 0;
    private Random rand = new Random();
    private int cameraX = 0;
    private Timer timer;

    private boolean playerJumpedThisFrame = false;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_W, PANEL_H));
        setBackground(new Color(80, 180, 255));
        setFocusable(true);
        addKeyListener(this);
        initWorld();
        timer = new Timer(16, this);
        timer.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void initWorld() {
        platforms = new ArrayList<>();
        groundTiles = new ArrayList<>();
        movingPlatforms = new ArrayList<>();
        enemies = new ArrayList<>();
        birds = new ArrayList<>();
        spikes = new ArrayList<>();
        collectibles = new ArrayList<>();
        speedBlocks = new ArrayList<>();
        mushrooms = new ArrayList<>();

        groundTiles.add(new Rectangle(0, GROUND_Y, CHUNK_W * 3000, 60));

        furthestGeneratedX = 0;
        for (int i = 0; i <= PRE_GEN_DIST * 3; i++) {
            generateChunk(i * CHUNK_W);
            furthestGeneratedX = (i + 1) * CHUNK_W;
        }

        spikes.removeIf(s -> s.x < 300);
        enemies.removeIf(e -> e.x < 300);
        boss = new BossMonster((int) playerX - 350, GROUND_Y - 60, 60, 60);
    }

    private void generateChunk(int chunkX) {
        int startX = chunkX;
        int endX = chunkX + CHUNK_W;

        // 平台
        int numPlats = 3 + rand.nextInt(3);
        for (int i = 0; i < numPlats; i++) {
            for (int attempt = 0; attempt < 12; attempt++) {
                int w = 100 + rand.nextInt(40);
                int x = startX + 70 + rand.nextInt(CHUNK_W - w - 40);
                int y = 250 + rand.nextInt(GROUND_Y - 240);
                Rectangle np = new Rectangle(x, y, w, 24);
                boolean ol = false;
                for (Rectangle p : platforms) if (p.intersects(np)) { ol = true; break; }
                for (MovingPlatform mp : movingPlatforms) if (mp.intersects(np)) { ol = true; break; }
                for (Rectangle g : groundTiles) if (g.intersects(np)) { ol = true; break; }
                if (!ol) {
                    platforms.add(np);
                    if (rand.nextInt(4) == 0) {
                        int mw = 60, mx = x + w + 25;
                        if (mx + mw < endX) {
                            int my = y - 8;
                            int mdx = rand.nextBoolean() ? 2 : -2;
                            int mr = 70 + rand.nextInt(120);
                            MovingPlatform mc = new MovingPlatform(mx, my, mw, 24, mdx, mr);
                            boolean mol = false;
                            Rectangle cpath = new Rectangle(mc.leftBound, mc.y, mc.rightBound - mc.leftBound + mc.width, mc.height);
                            for (Rectangle p : platforms) if (p.intersects(mc) || p.intersects(cpath)) { mol = true; break; }
                            for (MovingPlatform mp : movingPlatforms) {
                                Rectangle epath = new Rectangle(mp.leftBound, mp.y, mp.rightBound - mp.leftBound + mp.width, mp.height);
                                if (epath.intersects(cpath) || mp.intersects(mc)) { mol = true; break; }
                            }
                            if (!mol) movingPlatforms.add(mc);
                        }
                    }
                    break;
                }
            }
        }

        // 尖刺
        int ns = 1 + rand.nextInt(3);
        int sp = 0;
        for (int i = 0; i < ns && sp < ns; i++) {
            for (int a = 0; a < 10 && sp < ns; a++) {
                int sx, sy;
                if (rand.nextBoolean()) { sx = startX + 30 + rand.nextInt(CHUNK_W - 60); sy = GROUND_Y - 15; }
                else {
                    List<Rectangle> cand = new ArrayList<>();
                    for (Rectangle p : platforms) if (p.y < GROUND_Y - 60 && p.x >= startX && p.x < endX) cand.add(p);
                    if (!cand.isEmpty()) { Rectangle pl = cand.get(rand.nextInt(cand.size())); sx = pl.x + 8 + rand.nextInt(pl.width - 20); sy = pl.y - 14; }
                    else { sx = startX + 30 + rand.nextInt(CHUNK_W - 60); sy = GROUND_Y - 15; }
                }
                Spike spike = new Spike(sx, sy, 18, 18);
                boolean ol = false;
                for (Spike s : spikes) if (s.getBounds().intersects(spike.getBounds())) { ol = true; break; }
                for (Rectangle p : platforms) if (p.intersects(spike.getBounds())) { ol = true; break; }
                for (SpeedBlock b : speedBlocks) if (b.getBounds().intersects(spike.getBounds())) { ol = true; break; }
                for (Enemy e : enemies) if (e.getBounds().intersects(spike.getBounds())) { ol = true; break; }
                for (Bird b : birds) if (b.getBounds().intersects(spike.getBounds())) { ol = true; break; }
                if (chunkX == 0 && sx < 300) ol = true;
                if (!ol) { spikes.add(spike); sp++; }
            }
        }

        // 敌人
        int ne = 1 + rand.nextInt(2);
        for (int i = 0; i < ne; i++) {
            for (int a = 0; a < 5; a++) {
                int ex = startX + 50 + rand.nextInt(CHUNK_W - 100);
                Enemy en = new Enemy(ex, GROUND_Y - 36, 32, 32, rand.nextBoolean() ? 2 : -2);
                boolean ol = false;
                for (Enemy e : enemies) if (e.getBounds().intersects(en.getBounds())) { ol = true; break; }
                for (Spike s : spikes) if (s.getBounds().intersects(en.getBounds())) { ol = true; break; }
                if (!ol) { enemies.add(en); break; }
            }
        }

        // 鸟
        int nb = 1 + rand.nextInt(2);
        for (int i = 0; i < nb; i++) {
            for (int a = 0; a < 5; a++) {
                int bx = startX + 60 + rand.nextInt(CHUNK_W - 120);
                int by = 100 + rand.nextInt(220);
                Bird bird = new Bird(bx, by, 32, 28, rand.nextBoolean() ? 2 : -2, 120);
                boolean ol = false;
                for (Bird b : birds) if (b.getBounds().intersects(bird.getBounds())) { ol = true; break; }
                if (!ol) { birds.add(bird); break; }
            }
        }

        // 收集品
        int nc = 5 + rand.nextInt(7);
        for (int i = 0; i < nc; i++) {
            for (int a = 0; a < 6; a++) {
                int cx, cy;
                if (rand.nextBoolean() && !platforms.isEmpty()) {
                    List<Rectangle> cand = new ArrayList<>();
                    for (Rectangle p : platforms) if (p.y < GROUND_Y - 60 && p.x >= startX && p.x < endX) cand.add(p);
                    if (!cand.isEmpty()) { Rectangle pl = cand.get(rand.nextInt(cand.size())); cx = pl.x + 10 + rand.nextInt(pl.width - 25); cy = pl.y - 22; }
                    else { cx = startX + 40 + rand.nextInt(CHUNK_W - 80); cy = 180 + rand.nextInt(220); }
                } else { cx = startX + 40 + rand.nextInt(CHUNK_W - 80); cy = 180 + rand.nextInt(250); }
                Collectible coin = new Collectible(cx, cy, 22, 22, "coin");
                boolean ol = false;
                for (Collectible c : collectibles) if (c.getBounds().intersects(coin.getBounds())) { ol = true; break; }
                for (Spike s : spikes) if (s.getBounds().intersects(coin.getBounds())) { ol = true; break; }
                for (Enemy e : enemies) if (e.getBounds().intersects(coin.getBounds())) { ol = true; break; }
                for (SpeedBlock b : speedBlocks) if (b.getBounds().intersects(coin.getBounds())) { ol = true; break; }
                if (!ol) { collectibles.add(coin); break; }
            }
        }

        // 星星
        if (rand.nextInt(4) == 0) {
            int sx = startX + 50 + rand.nextInt(CHUNK_W - 100);
            int sy = 130 + rand.nextInt(260);
            Collectible star = new Collectible(sx, sy, 26, 26, "star");
            boolean ol = false;
            for (Collectible c : collectibles) if (c.getBounds().intersects(star.getBounds())) { ol = true; break; }
            for (Spike s : spikes) if (s.getBounds().intersects(star.getBounds())) { ol = true; break; }
            for (Enemy e : enemies) if (e.getBounds().intersects(star.getBounds())) { ol = true; break; }
            for (SpeedBlock b : speedBlocks) if (b.getBounds().intersects(star.getBounds())) { ol = true; break; }
            if (!ol) collectibles.add(star);
        }

        // 加速方块
        int blocksToPlace = rand.nextInt(2);
        int placed = 0, att = 0;
        while (placed < blocksToPlace && att < 8) {
            att++;
            int bx = startX + 80 + rand.nextInt(CHUNK_W - 160);
            int by = GROUND_Y - 120 - rand.nextInt(40);
            SpeedBlock block = new SpeedBlock(bx, by, 40, 40);
            boolean ol = false;
            for (SpeedBlock b : speedBlocks) if (b.getBounds().intersects(block.getBounds())) { ol = true; break; }
            for (Rectangle p : platforms) if (p.intersects(block.getBounds())) { ol = true; break; }
            for (Rectangle g : groundTiles) if (g.intersects(block.getBounds())) { ol = true; break; }
            for (Spike s : spikes) if (s.getBounds().intersects(block.getBounds())) { ol = true; break; }
            for (Enemy e : enemies) if (e.getBounds().intersects(block.getBounds())) { ol = true; break; }
            if (!ol) { speedBlocks.add(block); placed++; }
        }

        // 蘑菇
        int nm = 1 + rand.nextInt(2);
        for (int i = 0; i < nm; i++) {
            for (int a = 0; a < 6; a++) {
                int mx = startX + 50 + rand.nextInt(CHUNK_W - 100);
                int my = GROUND_Y - 24;
                BounceMushroom mush = new BounceMushroom(mx, my, 36, 24);
                boolean ol = false;
                for (BounceMushroom m : mushrooms) if (m.getBounds().intersects(mush.getBounds())) { ol = true; break; }
                for (Spike s : spikes) if (s.getBounds().intersects(mush.getBounds())) { ol = true; break; }
                for (Enemy e : enemies) if (e.getBounds().intersects(mush.getBounds())) { ol = true; break; }
                if (!ol) { mushrooms.add(mush); break; }
            }
        }
    }

    // ==================== 碰撞检测 ====================
    private void checkHorizontalCollisions() {
        Rectangle pr = new Rectangle((int) playerX, (int) playerY, getPlayerSize(), getPlayerSize());
        for (Rectangle g : groundTiles) {
            if (pr.intersects(g)) {
                if (playerDx > 0) playerX = g.x - getPlayerSize();
                else if (playerDx < 0) playerX = g.x + g.width;
                pr.setLocation((int) playerX, (int) playerY);
            }
        }
        for (Rectangle p : platforms) {
            if (pr.intersects(p)) {
                if (playerDx > 0) playerX = p.x - getPlayerSize();
                else if (playerDx < 0) playerX = p.x + p.width;
                pr.setLocation((int) playerX, (int) playerY);
            }
        }
        for (MovingPlatform mp : movingPlatforms) {
            if (pr.intersects(mp)) {
                if (playerDx > 0) playerX = mp.x - getPlayerSize();
                else if (playerDx < 0) playerX = mp.x + mp.width;
                pr.setLocation((int) playerX, (int) playerY);
            }
        }
    }

    private void checkVerticalCollisions() {
        Rectangle pr = new Rectangle((int) playerX, (int) playerY, getPlayerSize(), getPlayerSize());
        for (Rectangle g : groundTiles) {
            if (pr.intersects(g)) {
                if (playerDy > 0) { playerY = g.y - getPlayerSize(); playerDy = 0; onGround = true; }
                else if (playerDy < 0) { playerY = g.y + g.height; playerDy = 0; }
                pr.setLocation((int) playerX, (int) playerY);
            }
        }
        for (Rectangle p : platforms) {
            if (pr.intersects(p)) {
                if (playerDy > 0) { playerY = p.y - getPlayerSize(); playerDy = 0; onGround = true; }
                else if (playerDy < 0) { playerY = p.y + p.height; playerDy = 0; }
                pr.setLocation((int) playerX, (int) playerY);
            }
        }
        for (BounceMushroom m : mushrooms) {
            if (pr.intersects(m.getBounds())) {
                if (playerDy > 0) {
                    playerY = m.y - getPlayerSize();
                    playerDy = -16;
                    onGround = false;
                    score += 30;
                    m.bounceAnim = 10;
                }
                pr.setLocation((int) playerX, (int) playerY);
            }
        }
        for (SpeedBlock b : speedBlocks) {
            if (pr.intersects(b.getBounds())) {
                if (playerDy > 0) {
                    playerY = b.y - getPlayerSize(); playerDy = 0; onGround = true;
                    if (!b.used) {
                        b.used = true;
                        if (rand.nextBoolean()) { giantActive = true; giantTimer = GIANT_DURATION; }
                        else { activateSpeedBoost(); }
                    }
                } else if (playerDy < 0) {
                    playerY = b.y + b.height; playerDy = 0;
                    if (!b.used) {
                        b.used = true;
                        if (rand.nextBoolean()) { giantActive = true; giantTimer = GIANT_DURATION; }
                        else { activateSpeedBoost(); }
                    }
                }
                pr.setLocation((int) playerX, (int) playerY);
            }
        }
        for (MovingPlatform mp : movingPlatforms) {
            if (pr.intersects(mp)) {
                if (playerDy > 0) { playerY = mp.y - getPlayerSize(); playerDy = 0; onGround = true; playerX += mp.dx; }
                else if (playerDy < 0) { playerY = mp.y + mp.height; playerDy = 0; }
                pr.setLocation((int) playerX, (int) playerY);
            }
        }
    }

    private void checkSpikeCollisions() {
        if (invincible) return;
        Rectangle pr = new Rectangle((int) playerX, (int) playerY, getPlayerSize(), getPlayerSize());
        for (Spike s : spikes) if (pr.intersects(s.getBounds())) { loseLife(); return; }
    }

    private void checkEnemyCollisions() {
        if (invincible) return;
        Rectangle pr = new Rectangle((int) playerX, (int) playerY, getPlayerSize(), getPlayerSize());
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (pr.intersects(e.getBounds())) {
                if (playerDy > 0 && playerY + getPlayerSize() - playerDy <= e.y + 12) {
                    it.remove(); score += 50; playerDy = -9;
                } else { loseLife(); return; }
            }
        }
    }

    private void checkBirdCollisions() {
        if (invincible) return;
        Rectangle pr = new Rectangle((int) playerX, (int) playerY, getPlayerSize(), getPlayerSize());
        for (Bird b : birds) if (pr.intersects(b.getBounds())) { loseLife(); return; }
    }

    // ★★★ 修复 Boss 碰撞：不再依赖 playerDy > 0 ★★★
    private void checkBossCollision() {
        if (boss == null || boss.dead) return;
        if (invincible) return;
        Rectangle pr = new Rectangle((int) playerX, (int) playerY, getPlayerSize(), getPlayerSize());
        Rectangle br = boss.getBounds();
        if (pr.intersects(br)) {
            // 踩踏判定：玩家脚底高于 Boss 半身 → 踩
            int playerFeet = (int) playerY + getPlayerSize();
            int bossMidY = br.y + br.height / 2;
            boolean isStomping = playerFeet - 10 < bossMidY;
            if (isStomping) {
                playerDy = -10;
                boss.hurt();
                score += 200;
                triggerShake(4);
            } else {
                loseLife();
            }
        }
    }

    private void checkCollectibleCollisions() {
        Rectangle pr = new Rectangle((int) playerX, (int) playerY, getPlayerSize(), getPlayerSize());
        Iterator<Collectible> it = collectibles.iterator();
        while (it.hasNext()) {
            Collectible c = it.next();
            if (pr.intersects(c.getBounds())) {
                if (c.type.equals("coin")) { score += 100; progressPoints += 100; }
                else if (c.type.equals("star")) {
                    invincible = true; invincibleTimer = Math.max(invincibleTimer, 350);
                    score += 250; progressPoints += 250; updateSpeedMultiplier();
                }
                it.remove();
            }
        }
    }

    private void loseLife() {
        if (invincible) return;
        lives--;
        invincible = true; invincibleTimer = Math.max(invincibleTimer, 300);
        speedBoostActive = true; speedBoostTimer = Math.max(speedBoostTimer, 300);
        updateSpeedMultiplier();
        triggerShake(10);
        boss.retreatFrom(playerX);
        if (lives <= 0) { gameRunning = false; triggerShake(30); }
        if (playerY > PANEL_H + 100) { playerY = GROUND_Y - getPlayerSize(); playerDy = 0; onGround = true; }
    }

    private void restartGame() {
        gameRunning = true; lives = 3; score = 0;
        invincible = false; speedBoostActive = false; speedBoostTimer = 0;
        invincibleTimer = 0; speedMultiplier = 1.0;
        giantActive = false; giantTimer = 0; progressPoints = 0;
        initWorld();
        playerX = 150; playerY = GROUND_Y - PLAYER_SIZE;
        playerDx = 0; playerDy = 0; onGround = true;
        movingLeft = false; movingRight = false;
        cameraX = 0; screenShakeX = 0; screenShakeY = 0; shakeTimer = 0;
        playerJumpedThisFrame = false;
        requestFocusInWindow();
    }

    private int getPlayerSize() { return giantActive ? (int)(PLAYER_SIZE * 1.6) : PLAYER_SIZE; }
    private void activateSpeedBoost() { speedBoostActive = true; speedBoostTimer = SPEED_BOOST_DURATION; updateSpeedMultiplier(); }
    private void updateSpeedMultiplier() {
        if (invincible && speedBoostActive) speedMultiplier = 2.0;
        else if (speedBoostActive) speedMultiplier = 1.8;
        else if (invincible) speedMultiplier = 1.5;
        else speedMultiplier = 1.0;
    }
    private void triggerShake(int duration) { shakeTimer = duration; }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning) updateGame();
        repaint();
    }

    private void updateGame() {
        // 屏幕震动
        if (shakeTimer > 0) { shakeTimer--; screenShakeX = rand.nextInt(6) - 3; screenShakeY = rand.nextInt(6) - 3; }
        else { screenShakeX = 0; screenShakeY = 0; }

        // ★★★ 动态区块生成：前方不够就生成 ★★★
        int playerRightEdge = (int) playerX + PANEL_W;
        while (playerRightEdge + CHUNK_W * 3 > furthestGeneratedX) {
            generateChunk(furthestGeneratedX);
            furthestGeneratedX += CHUNK_W;
        }

        // ★★★ 清理远处实体，防止内存溢出 ★★★
        int cleanupX = (int) playerX - PANEL_W * 3;
        if (cleanupX > 1000) {
            platforms.removeIf(p -> p.x + p.width < cleanupX);
            movingPlatforms.removeIf(mp -> mp.x + mp.width < cleanupX);
            spikes.removeIf(s -> s.x < cleanupX);
            enemies.removeIf(e -> e.x < cleanupX);
            birds.removeIf(b -> b.x < cleanupX);
            collectibles.removeIf(c -> c.x < cleanupX);
            speedBlocks.removeIf(b -> b.x < cleanupX);
            mushrooms.removeIf(m -> m.x < cleanupX);
        }

        // 应急平台
        boolean eg = false;
        int ck = (int) playerX + 300;
        for (Rectangle g : groundTiles) if (g.y == GROUND_Y && g.x <= ck && g.x + g.width >= ck) { eg = true; break; }
        if (!eg) platforms.add(new Rectangle((int) playerX + 250, GROUND_Y - 80, 120, 20));

        // 土狼时间
        if (coyoteTimer > 0) coyoteTimer--;
        if (onGround) { coyoteTimer = COYOTE_TIME; jumpsRemaining = MAX_JUMPS; }

        // 跳跃
        playerJumpedThisFrame = false;
        if (jumpRequested && (onGround || coyoteTimer > 0 || jumpsRemaining > 0)) {
            playerDy = JUMP_STRENGTH;
            if (!onGround && coyoteTimer <= 0) jumpsRemaining--;
            onGround = false; coyoteTimer = 0; jumpRequested = false;
            playerJumpedThisFrame = true;
        }

        // 水平移动
        double ms = 5.2 * speedMultiplier;
        if (movingLeft) { playerDx = Math.max(-ms, playerDx - 0.85); facingRight = false; }
        else if (movingRight) { playerDx = Math.min(ms, playerDx + 0.85); facingRight = true; }
        else playerDx *= 0.5;

        // 物理
        playerDy += GRAVITY; onGround = false;
        playerX += playerDx; checkHorizontalCollisions();
        playerY += playerDy; checkVerticalCollisions();

        // 移动平台 & 鸟更新
        for (MovingPlatform mp : movingPlatforms) mp.update();
        for (Bird bird : birds) bird.update();

        // 掉落死亡
        if (playerY > PANEL_H + 120) { loseLife(); if (!gameRunning) return; }

        // Boss 更新
        boolean playerInAir = playerDy < -1 || !onGround;
        boss.update(playerX, playerY, speedMultiplier, giantActive, playerJumpedThisFrame || playerInAir);
        checkBossCollision();
        if (boss != null && !boss.dead && boss.distanceTo(playerX) < 350 && gameRunning) triggerShake(2);

        // 敌人更新
        for (Enemy enemy : enemies) enemy.update(platforms, movingPlatforms);

        // 能力计时
        if (invincible) {
            invincibleTimer--; invinciblePulse = (invinciblePulse + 1) % 60;
            if (invincibleTimer <= 0) { invincible = false; invinciblePulse = 0; updateSpeedMultiplier(); }
        } else invinciblePulse = 0;

        if (giantActive) {
            giantTimer--;
            if (giantTimer <= 0) giantActive = false;
            else {
                int front = (int) playerX + getPlayerSize() / 2;
                int cr = 350, cl = facingRight ? front : front - cr;
                Rectangle ca = new Rectangle(cl, 0, cr, PANEL_H);
                spikes.removeIf(s -> ca.intersects(s.getBounds()));
                enemies.removeIf(e -> ca.intersects(e.getBounds()));
                birds.removeIf(b -> ca.intersects(b.getBounds()));
                platforms.removeIf(p -> ca.intersects(p));
                movingPlatforms.removeIf(mp -> ca.intersects(mp));
                speedBlocks.removeIf(b -> ca.intersects(b.getBounds()));
            }
        }
        if (speedBoostActive) { speedBoostTimer--; if (speedBoostTimer <= 0) { speedBoostActive = false; updateSpeedMultiplier(); } }

        // 碰撞
        checkSpikeCollisions(); checkEnemyCollisions(); checkBirdCollisions(); checkCollectibleCollisions();

        // 进度奖励
        while (progressPoints >= PROGRESS_THRESHOLD) {
            progressPoints -= PROGRESS_THRESHOLD;
            invincible = true; invincibleTimer = Math.max(invincibleTimer, 350);
            updateSpeedMultiplier();
        }

        // 边界 & 镜头
        if (playerX < 0) playerX = 0;
        cameraX = (int) playerX + getPlayerSize() / 2 - PANEL_W / 2;
        if (cameraX < 0) cameraX = 0;
    }

    // ==================== 绘制 ====================
    private void drawStatusCard(Graphics2D g2d, int x, int y, String icon, String label, Color fillColor, int currentTime, int maxTime) {
        int cardW = 150, cardH = 36;
        g2d.setColor(new Color(20, 20, 20, 200));
        g2d.fillRoundRect(x, y, cardW, cardH, 16, 16);
        g2d.setColor(fillColor.darker().darker());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x, y, cardW, cardH, 16, 16);
        g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        g2d.setColor(Color.WHITE);
        g2d.drawString(icon, x + 10, y + 22);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.drawString(label, x + 30, y + 16);
        int barX = x + 30, barY = y + 20, barW = 110, barH = 6;
        g2d.setColor(new Color(60, 60, 60));
        g2d.fillRoundRect(barX, barY, barW, barH, 4, 4);
        if (maxTime > 0 && currentTime > 0) {
            double p = Math.min(1.0, (double) currentTime / (double) maxTime);
            GradientPaint grad = new GradientPaint(barX, 0, fillColor.brighter(), barX + barW, 0, fillColor);
            g2d.setPaint(grad);
            g2d.fillRoundRect(barX, barY, (int)(barW * p), barH, 4, 4);
        }
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        if (maxTime > 0) { double sec = currentTime / 60.0; g2d.drawString(String.format("%.1fs", sec), barX + barW + 4, barY + 10); }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.translate(screenShakeX, screenShakeY);
        g2d.translate(-cameraX, 0);

        // 天空
        GradientPaint skyG = new GradientPaint(0, 0, new Color(80, 180, 255), 0, PANEL_H, new Color(160, 220, 255));
        g2d.setPaint(skyG); g2d.fillRect(0, 0, 20000, PANEL_H);

        // 地面
        for (Rectangle gr : groundTiles) {
            int d = 12;
            g2d.setColor(new Color(30, 120, 30)); g2d.fillRect(gr.x, gr.y - d, gr.width, gr.height);
            g2d.setColor(new Color(20, 90, 20));
            Polygon s = new Polygon(); s.addPoint(gr.x, gr.y - d); s.addPoint(gr.x + gr.width, gr.y - d); s.addPoint(gr.x + gr.width, gr.y); s.addPoint(gr.x, gr.y);
            g2d.fillPolygon(s);
            g2d.setColor(new Color(50, 170, 50)); g2d.fillRect(gr.x, gr.y, gr.width, gr.height);
            g2d.setColor(Color.BLACK); g2d.drawRect(gr.x, gr.y, gr.width, gr.height);
        }

        // 平台
        for (Rectangle p : platforms) {
            int d = 8;
            g2d.setColor(new Color(210, 140, 70)); g2d.fillRect(p.x, p.y - d, p.width, p.height);
            g2d.setColor(new Color(130, 80, 40));
            Polygon s = new Polygon(); s.addPoint(p.x, p.y - d); s.addPoint(p.x + p.width, p.y - d); s.addPoint(p.x + p.width, p.y); s.addPoint(p.x, p.y);
            g2d.fillPolygon(s);
            g2d.setColor(new Color(180, 110, 60)); g2d.fillRect(p.x, p.y, p.width, p.height);
            g2d.setColor(Color.BLACK); g2d.drawRect(p.x, p.y, p.width, p.height);
        }

        // 移动平台
        for (MovingPlatform mp : movingPlatforms) {
            int d = 8;
            g2d.setColor(new Color(180, 120, 50)); g2d.fillRect(mp.x, mp.y - d, mp.width, mp.height);
            g2d.setColor(new Color(100, 70, 30));
            Polygon s = new Polygon(); s.addPoint(mp.x, mp.y - d); s.addPoint(mp.x + mp.width, mp.y - d); s.addPoint(mp.x + mp.width, mp.y); s.addPoint(mp.x, mp.y);
            g2d.fillPolygon(s);
            g2d.setColor(new Color(150, 100, 40)); g2d.fillRect(mp.x, mp.y, mp.width, mp.height);
            g2d.setColor(Color.BLACK); g2d.drawRect(mp.x, mp.y, mp.width, mp.height);
        }

        // 蘑菇
        for (BounceMushroom m : mushrooms) {
            int mx = m.x, my = m.y + (m.bounceAnim > 0 ? -m.bounceAnim * 2 : 0);
            g2d.setColor(new Color(220, 60, 60)); g2d.fillOval(mx, my + m.height / 2, m.width, m.height / 2);
            g2d.setColor(new Color(255, 200, 200)); g2d.fillOval(mx + 4, my, m.width - 8, m.height / 2 + 4);
            g2d.setColor(Color.WHITE); for (int i = 0; i < 5; i++) g2d.fillOval(mx + 6 + i * 5, my + 6, 4, 4);
            g2d.setColor(Color.BLACK); g2d.drawOval(mx, my + m.height / 2, m.width, m.height / 2);
            if (m.bounceAnim > 0) m.bounceAnim--;
        }

        // 加速方块
        for (SpeedBlock b : speedBlocks) {
            g2d.setColor(b.used ? new Color(150, 150, 150) : new Color(230, 190, 60));
            g2d.fillRect(b.x, b.y, b.width, b.height);
            g2d.setColor(new Color(170, 130, 30)); g2d.fillRect(b.x, b.y, b.width, 10);
            g2d.setColor(Color.BLACK); g2d.drawRect(b.x, b.y, b.width, b.height);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.setColor(b.used ? Color.DARK_GRAY : Color.WHITE);
            g2d.drawString("?", b.x + b.width / 2 - 6, b.y + b.height / 2 + 8);
        }

        // 尖刺
        for (Spike spike : spikes) {
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillPolygon(new int[]{spike.x, spike.x + spike.width / 2, spike.x + spike.width}, new int[]{spike.y + spike.height, spike.y, spike.y + spike.height}, 3);
            g2d.setColor(Color.GRAY);
            g2d.fillPolygon(new int[]{spike.x + 2, spike.x + spike.width / 2, spike.x + spike.width - 2}, new int[]{spike.y + spike.height - 2, spike.y + 4, spike.y + spike.height - 2}, 3);
            g2d.setColor(Color.BLACK);
            g2d.drawPolygon(new int[]{spike.x, spike.x + spike.width / 2, spike.x + spike.width}, new int[]{spike.y + spike.height, spike.y, spike.y + spike.height}, 3);
        }

        // 敌人
        for (Enemy en : enemies) {
            g2d.setColor(new Color(120, 70, 40)); g2d.fillOval(en.x, en.y, en.width, en.height);
            g2d.setColor(Color.BLACK); g2d.fillOval(en.x + 8, en.y + 10, 5, 5); g2d.fillOval(en.x + en.width - 13, en.y + 10, 5, 5);
            g2d.setColor(new Color(80, 50, 30)); g2d.fillArc(en.x + 5, en.y + 18, en.width - 10, 8, 0, -180);
        }

        // 鸟
        for (Bird b : birds) {
            g2d.setColor(Color.CYAN); g2d.fillOval(b.x, b.y, b.width, b.height);
            g2d.setColor(Color.ORANGE); g2d.fillOval(b.x + b.width - 10, b.y + 10, 10, 10);
            g2d.setColor(Color.BLACK); g2d.fillOval(b.x + 6, b.y + 12, 4, 4); g2d.fillOval(b.x + b.width - 14, b.y + 12, 4, 4);
            g2d.setColor(new Color(200, 100, 0)); g2d.fillOval(b.x - 8, b.y + 8, 12, 10); g2d.fillOval(b.x + b.width - 4, b.y + 8, 12, 10);
        }

        // 收集品
        for (Collectible c : collectibles) {
            if (c.type.equals("coin")) {
                g2d.setColor(Color.YELLOW); g2d.fillOval(c.x, c.y, c.width, c.height);
                g2d.setColor(Color.ORANGE); g2d.drawString("★", c.x + 6, c.y + 16);
                g2d.setColor(Color.WHITE); g2d.drawOval(c.x + 2, c.y + 2, c.width - 4, c.height - 4);
            } else {
                g2d.setColor(Color.MAGENTA); g2d.fillOval(c.x, c.y, c.width, c.height);
                g2d.setColor(Color.YELLOW); g2d.drawString("⭐", c.x + 6, c.y + 18);
                g2d.setColor(Color.WHITE); g2d.drawOval(c.x + 3, c.y + 3, c.width - 6, c.height - 6);
            }
        }

        // ===== Boss =====
        if (boss != null && !boss.dead) {
            int bx = boss.x, by = boss.y + boss.bobOffset, bw = boss.width, bh = boss.height;
            boolean angry = boss.distanceTo(playerX) < 300;
            if (angry) {
                float alpha = 0.4f + 0.2f * (float) Math.sin(System.currentTimeMillis() / 80.0);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.setColor(new Color(255, 60, 0)); g2d.setStroke(new BasicStroke(8));
                g2d.drawOval(bx - 8, by - 8, bw + 16, bh + 16);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
            if (boss.hurtTimer > 0) {
                float blink = 0.5f + 0.4f * (float) Math.abs(Math.sin(boss.hurtTimer * 1.2));
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, blink));
            }
            g2d.setColor(new Color(180, 20, 20)); g2d.fillRect(bx, by, bw, bh);
            g2d.setColor(new Color(140, 0, 0));
            for (int i = 0; i < 5; i++) { int sx = bx + i * (bw / 4); g2d.fillPolygon(new int[]{sx, sx + bw / 8, sx + bw / 4}, new int[]{by, by - 12, by}, 3); }
            g2d.setColor(Color.WHITE); int es = 18;
            g2d.fillOval(bx + 12, by + 14, es, es); g2d.fillOval(bx + bw - 12 - es, by + 14, es, es);
            g2d.setColor(angry ? Color.RED : Color.BLACK);
            double ang = Math.atan2(playerY - by, playerX - bx);
            int pox = (int)(Math.cos(ang) * 4), poy = (int)(Math.sin(ang) * 4), ps = 8;
            g2d.fillOval(bx + 12 + es / 2 - ps / 2 + pox, by + 14 + es / 2 - ps / 2 + poy, ps, ps);
            g2d.fillOval(bx + bw - 12 - es + es / 2 - ps / 2 + pox, by + 14 + es / 2 - ps / 2 + poy, ps, ps);
            g2d.setColor(Color.BLACK); g2d.fillRect(bx + 20, by + bh - 15, bw - 40, 10);
            g2d.setColor(Color.RED); g2d.fillRect(bx + 22, by + bh - 13, bw - 44, 6);
            if (boss.inAir) { g2d.setColor(new Color(0, 0, 0, 60)); g2d.fillOval(bx, GROUND_Y - 6, bw, 10); }
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            // Boss 血条
            int barW = 56, barH = 8;
            int barX = bx + bw / 2 - barW / 2, barY = by - 18;
            g2d.setColor(new Color(30, 30, 30, 220)); g2d.fillRoundRect(barX - 2, barY - 2, barW + 4, barH + 4, 8, 8);
            double hpPct = (double) boss.hp / boss.maxHp;
            Color hpColor = hpPct > 0.5 ? new Color(220, 40, 40) : (hpPct > 0.25 ? new Color(255, 120, 30) : new Color(255, 30, 30));
            g2d.setColor(hpColor); g2d.fillRoundRect(barX, barY, (int)(barW * hpPct), barH, 6, 6);
            g2d.setColor(new Color(255, 255, 255, 60)); g2d.fillRoundRect(barX, barY, (int)(barW * hpPct), barH / 2, 6, 6);
            g2d.setColor(new Color(180, 30, 30)); g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(barX - 2, barY - 2, barW + 4, barH + 4, 8, 8);
        }

        // 玩家
        int psz = getPlayerSize();
        if (invincible) { float alpha = 0.35f + 0.25f * (float) Math.sin(invinciblePulse * Math.PI / 30); g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)); g2d.setColor(new Color(75, 200, 255)); g2d.setStroke(new BasicStroke(10)); g2d.drawOval((int)playerX - 7, (int)playerY - 7, psz + 14, psz + 14); g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f)); }
        if (giantActive) { float alpha = 0.45f + 0.2f * (float) Math.sin(System.currentTimeMillis() / 120.0); g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)); g2d.setColor(new Color(255, 200, 80)); g2d.setStroke(new BasicStroke(14)); g2d.drawOval((int)playerX - 14, (int)playerY - 14, psz + 28, psz + 28); g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f)); }
        {
            int x = (int)playerX, y = (int)playerY;
            g2d.setColor(new Color(210, 50, 50)); g2d.fillRect(x, y + psz / 3, psz, psz * 2 / 3);
            g2d.setColor(new Color(160, 30, 30)); g2d.fillRect(x, y + psz / 3 + 8, psz, psz * 2 / 3 - 8);
            g2d.setColor(new Color(40, 80, 180)); g2d.fillRect(x, y + psz * 2 / 3, psz, psz / 3);
            g2d.fillRect(x, y + psz / 3 + 5, 8, psz / 3); g2d.fillRect(x + psz - 8, y + psz / 3 + 5, 8, psz / 3);
            g2d.setColor(new Color(255, 200, 120)); g2d.fillOval(x + 2, y - 6, psz - 4, psz - 8);
            g2d.setColor(Color.BLACK); g2d.fillOval(x + 8, y, 6, 6); g2d.fillOval(x + psz - 14, y, 6, 6);
            g2d.setColor(Color.RED); g2d.fillRect(x + 4, y - 12, psz - 8, 12); g2d.fillRect(x + 8, y - 16, psz - 16, 6);
            g2d.setColor(new Color(139, 69, 19)); g2d.fillOval(x + 6, y + 8, 8, 6); g2d.fillOval(x + psz - 14, y + 8, 8, 6);
        }

        g2d.translate(cameraX, 0);
        g2d.translate(-screenShakeX, -screenShakeY);

        // Boss 方向警告
        double distToBoss = (boss != null && !boss.dead) ? boss.distanceTo(playerX) : 9999;
        if (distToBoss < 450 && gameRunning) {
            float warnAlpha = (float) Math.max(0, (450 - distToBoss) / 450.0) * 0.7f;
            boolean bossOnLeft = boss.x + boss.width / 2 < playerX + psz / 2;
            GradientPaint warnGrad = bossOnLeft ?
                new GradientPaint(0, 0, new Color(255, 0, 0, (int)(warnAlpha * 255)), 120, 0, new Color(255, 0, 0, 0)) :
                new GradientPaint(PANEL_W, 0, new Color(255, 0, 0, (int)(warnAlpha * 255)), PANEL_W - 120, 0, new Color(255, 0, 0, 0));
            g2d.setPaint(warnGrad); g2d.fillRect(0, 0, PANEL_W, PANEL_H);
        }

        // HUD
        int hudY = 18;
        g2d.setColor(new Color(20, 20, 20, 190)); g2d.fillRoundRect(18, hudY, 115, 40, 16, 16);
        g2d.setColor(new Color(200, 50, 50)); g2d.setStroke(new BasicStroke(2)); g2d.drawRoundRect(18, hudY, 115, 40, 16, 16);
        g2d.setFont(new Font("Dialog", Font.BOLD, 22)); g2d.drawString("❤️  x" + lives, 28, hudY + 30);

        g2d.setColor(new Color(20, 20, 20, 190)); g2d.fillRoundRect(148, hudY, 155, 40, 16, 16);
        g2d.setColor(new Color(255, 200, 40)); g2d.setStroke(new BasicStroke(2)); g2d.drawRoundRect(148, hudY, 155, 40, 16, 16);
        g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, 15)); g2d.drawString("⭐ Score: " + score, 158, hudY + 28);

        int barX = 148, barY = hudY + 44, barW = 155, barH = 8;
        g2d.setColor(new Color(30, 30, 30, 200)); g2d.fillRoundRect(barX, barY, barW, barH, 6, 6);
        double prog = Math.min(1.0, progressPoints / (double) PROGRESS_THRESHOLD);
        GradientPaint progGrad = new GradientPaint(barX, 0, new Color(255, 220, 60), barX + barW, 0, new Color(255, 160, 20));
        g2d.setPaint(progGrad); g2d.fillRoundRect(barX, barY, (int)(barW * prog), barH, 6, 6);

        int cardX = 18, cardY = 108;
        if (speedBoostActive) { drawStatusCard(g2d, cardX, cardY, "⚡", "SPEED", new Color(255, 140, 0), speedBoostTimer, SPEED_BOOST_DURATION); cardY += 42; }
        if (invincible) { drawStatusCard(g2d, cardX, cardY, "\uD83D\uDEE1\uFE0F", "INVINCIBLE", new Color(0, 200, 255), invincibleTimer, 350); cardY += 42; }
        if (giantActive) { drawStatusCard(g2d, cardX, cardY, "\uD83D\uDC79", "GIANT", new Color(200, 50, 200), giantTimer, GIANT_DURATION); }

        if (gameRunning && boss != null && !boss.dead && distToBoss < 600) {
            g2d.setColor(new Color(20, 20, 20, 180)); g2d.fillRoundRect(PANEL_W - 145, 14, 130, 30, 12, 12);
            g2d.setColor(new Color(255, 80, 80)); g2d.setStroke(new BasicStroke(1.5f)); g2d.drawRoundRect(PANEL_W - 145, 14, 130, 30, 12, 12);
            g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, 13));
            g2d.drawString(String.format("👹 BOSS: %.0f", distToBoss), PANEL_W - 138, 34);
        }

        g2d.setColor(new Color(200, 200, 200, 130));
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("← → / A D = Move    ↑ / Space = Jump    R = Restart", 18, PANEL_H - 16);

        if (!gameRunning) {
            g2d.setColor(new Color(0, 0, 0, 160)); g2d.fillRect(0, 0, PANEL_W, PANEL_H);
            g2d.setFont(new Font("Arial", Font.BOLD, 52)); g2d.setColor(Color.RED);
            String go = "GAME OVER"; int goW = g2d.getFontMetrics().stringWidth(go);
            g2d.drawString(go, PANEL_W / 2 - goW / 2, PANEL_H / 2 - 10);
            g2d.setFont(new Font("Arial", Font.PLAIN, 22)); g2d.setColor(Color.WHITE);
            String rst = "Press R to restart"; int rstW = g2d.getFontMetrics().stringWidth(rst);
            g2d.drawString(rst, PANEL_W / 2 - rstW / 2, PANEL_H / 2 + 40);
            g2d.setFont(new Font("Arial", Font.PLAIN, 18)); g2d.setColor(new Color(255, 200, 40));
            String sc = "Final Score: " + score; int scW = g2d.getFontMetrics().stringWidth(sc);
            g2d.drawString(sc, PANEL_W / 2 - scW / 2, PANEL_H / 2 + 70);
        }
    }

    // ==================== 键盘 ====================
    @Override public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_R) { restartGame(); return; }
        if (!gameRunning) return;
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) movingLeft = true;
        else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) movingRight = true;
        else if (k == KeyEvent.VK_UP || k == KeyEvent.VK_SPACE) jumpRequested = true;
    }
    @Override public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) movingLeft = false;
        else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) movingRight = false;
    }
    @Override public void keyTyped(KeyEvent e) {}

    // ==================== 内部类 ====================
    class MovingPlatform extends Rectangle {
        int dx, leftBound, rightBound;
        MovingPlatform(int x, int y, int w, int h, int dx, int range) { super(x, y, w, h); this.dx = dx; leftBound = x - range; rightBound = x + range; }
        void update() { x += dx; if (x < leftBound || x + width > rightBound) { dx = -dx; x += dx; } }
    }
    class Enemy {
        int x, y, width, height, dx;
        Enemy(int x, int y, int w, int h, int dx) { this.x = x; this.y = y; this.width = w; this.height = h; this.dx = dx; }
        void update(List<Rectangle> pl, List<MovingPlatform> mp) { x += dx; Rectangle r = new Rectangle(x, y, width, height); for (Rectangle p : pl) if (r.intersects(p)) { x -= dx; dx = -dx; return; } for (MovingPlatform m : mp) if (r.intersects(m)) { x -= dx; dx = -dx; return; } }
        Rectangle getBounds() { return new Rectangle(x, y, width, height); }
    }
    class Bird {
        int x, y, width, height, dx, range, startX;
        Bird(int x, int y, int w, int h, int dx, int range) { this.x = x; this.y = y; this.width = w; this.height = h; this.dx = dx; this.range = range; startX = x; }
        void update() { x += dx; if (x < startX - range || x > startX + range) { dx = -dx; x += dx; } }
        Rectangle getBounds() { return new Rectangle(x, y, width, height); }
    }
    class Spike { int x, y, width, height; Spike(int x, int y, int w, int h) { this.x = x; this.y = y; this.width = w; this.height = h; } Rectangle getBounds() { return new Rectangle(x, y, width, height); } }
    class SpeedBlock { int x, y, width, height; boolean used; SpeedBlock(int x, int y, int w, int h) { this.x = x; this.y = y; this.width = w; this.height = h; used = false; } Rectangle getBounds() { return new Rectangle(x, y, width, height); } }
    class Collectible { int x, y, width, height; String type; Collectible(int x, int y, int w, int h, String t) { this.x = x; this.y = y; this.width = w; this.height = h; this.type = t; } Rectangle getBounds() { return new Rectangle(x, y, width, height); } }
    class BounceMushroom { int x, y, width, height, bounceAnim; BounceMushroom(int x, int y, int w, int h) { this.x = x; this.y = y; this.width = w; this.height = h; bounceAnim = 0; } Rectangle getBounds() { return new Rectangle(x, y, width, height); } }

    // ================= Boss（高速 + 多段跳 + 可击杀）=================
    class BossMonster {
        int x, y, width, height;
        double baseSpeed = 6.5;
        double speed;
        double vy = 0;
        boolean inAir = false;
        int bobOffset = 0;
        int jumpCooldown = 0;
        int frameCount = 0;
        int jumpsRemaining = 0;
        static final int MAX_BOSS_JUMPS = 2;

        int hp = 5;
        int maxHp = 5;
        int hurtTimer = 0;
        boolean dead = false;
        int deathTimer = 0;

        BossMonster(int x, int y, int w, int h) { this.x = x; this.y = y; this.width = w; this.height = h; this.speed = baseSpeed; }

        double distanceTo(double px) { return Math.abs(px - x); }

        void retreatFrom(double playerX) { if (playerX > x + width / 2) x -= 200; else x += 200; }

        void hurt() {
            if (hurtTimer > 0) return;
            hp--;
            hurtTimer = 25;
            triggerShake(8);
            if (hp <= 0) { dead = true; deathTimer = 200; score += 500; }
            retreatFrom(playerX);
        }

        void update(double playerX, double playerY, double playerSpeedMult, boolean playerGiant, boolean playerInAir) {
            if (dead) {
                deathTimer--;
                if (deathTimer <= 0) { hp = maxHp; dead = false; x = (int)playerX - 350; y = GROUND_Y - height; vy = 0; inAir = false; jumpsRemaining = 0; }
                return;
            }
            if (hurtTimer > 0) hurtTimer--;

            frameCount++;
            double dist = distanceTo(playerX);
            double bossGroundY = GROUND_Y - height;

            speed = baseSpeed;
            if (dist < 300) speed += 2.5;
            if (dist > 600) speed += 2.0;
            if (dist > 600) x = (int)playerX - 400;

            if (playerGiant && dist < 250) { if (playerX > x) x -= speed * 1.2; else x += speed * 1.2; }
            else { if (playerX > x + 8) x += speed; else if (playerX < x - 8) x -= speed; }

            if (jumpCooldown > 0) jumpCooldown--;
            boolean playerAbove = (playerY < bossGroundY - 20);

            if (!inAir) {
                jumpsRemaining = MAX_BOSS_JUMPS;
                if (jumpCooldown <= 0 && dist < 500 && dist > 40 && (playerInAir || playerAbove)) {
                    vy = -14; inAir = true; jumpsRemaining--; jumpCooldown = 80;
                }
            } else {
                if (jumpsRemaining > 0 && jumpCooldown <= 0 && vy > -5 && (playerAbove || dist < 200)) {
                    vy = -12; jumpsRemaining--; jumpCooldown = 60;
                }
                if (dist < 250) x += (playerX > x ? 1.5 : -1.5);
            }

            if (inAir) { vy += GRAVITY; y += vy; if (y >= bossGroundY) { y = (int)bossGroundY; vy = 0; inAir = false; jumpsRemaining = 0; if (dist < 400) triggerShake(6); } }
            else y = (int)bossGroundY;

            bobOffset = (int)(Math.sin(frameCount * 0.15) * 3);
        }

        Rectangle getBounds() { return new Rectangle(x, y + bobOffset, width, height); }
    }
}
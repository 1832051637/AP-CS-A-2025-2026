package taggame;

import java.util.*;

public class GameEngine {
    private List<Player> players = new ArrayList<>();
    private List<Platform> platforms = new ArrayList<>();
    private List<Spring> springs = new ArrayList<>();
    private List<Trap> traps = new ArrayList<>();
    private List<Item> items = new ArrayList<>();
    private List<ZapEffect> zapEffects = new ArrayList<>();

    private int timeLeft = Constants.GAME_TIME;
    private boolean running = false;
    private double worldWidth = 800;
    private double worldHeight = 600;
    private double cameraY = 0;

    private long lastTime = 0;
    private int itemSpawnTimer = 0;
    private int trapSpawnTimer = 0;

    private static final String[] CONTROLS_LEFT = {"a", "ArrowLeft", "j", "f"};
    private static final String[] CONTROLS_RIGHT = {"d", "ArrowRight", "l", "h"};
    private static final String[] CONTROLS_JUMP = {"w", "ArrowUp", "i", "t"};

    private Map<String, Boolean> keys = new HashMap<>();

    public void startGame() {
        createPlatforms();

        players.clear();
        for (int i = 0; i < Constants.PLAYER_COUNT; i++) {
            players.add(new Player(i));
        }

        // 随机选择一个玩家是鬼
        int itIndex = new Random().nextInt(Constants.PLAYER_COUNT);
        players.get(itIndex).isIT = true;

        timeLeft = Constants.GAME_TIME;
        running = true;
        cameraY = 0;
        items.clear();
        itemSpawnTimer = 0;
        traps.clear();
        trapSpawnTimer = 0;
    }

    private void createPlatforms() {
        platforms.clear();
        springs.clear();
        traps.clear();

        // 地面
        platforms.add(new Platform(0, worldHeight - 20, worldWidth, 20, "#333333"));

        // 第一层
        platforms.add(new Platform(worldWidth * 0.03, worldHeight - 100, 180, 15, "#ff6b6b"));
        platforms.add(new Platform(worldWidth * 0.30, worldHeight - 100, 220, 15, "#54a0ff"));
        platforms.add(new Platform(worldWidth * 0.60, worldHeight - 100, 160, 15, "#feca57"));
        platforms.add(new Platform(worldWidth * 0.85, worldHeight - 100, 100, 15, "#5f27cd"));

        // 第二层
        platforms.add(new Platform(worldWidth * 0.08, worldHeight - 180, 120, 15, "#ff9ff3"));
        platforms.add(new Platform(worldWidth * 0.35, worldHeight - 180, 180, 15, "#1dd1a1"));
        platforms.add(new Platform(worldWidth * 0.65, worldHeight - 180, 140, 15, "#54a0ff"));
        platforms.add(new Platform(worldWidth * 0.88, worldHeight - 180, 80, 15, "#ff6b6b"));

        // 第三层
        platforms.add(new Platform(worldWidth * 0.15, worldHeight - 260, 150, 15, "#feca57"));
        platforms.add(new Platform(worldWidth * 0.45, worldHeight - 260, 200, 15, "#5f27cd"));
        platforms.add(new Platform(worldWidth * 0.75, worldHeight - 260, 120, 15, "#ff9ff3"));

        // 第四层
        platforms.add(new Platform(worldWidth * 0.05, worldHeight - 340, 100, 15, "#1dd1a1"));
        platforms.add(new Platform(worldWidth * 0.28, worldHeight - 340, 160, 15, "#ffffff"));
        platforms.add(new Platform(worldWidth * 0.55, worldHeight - 340, 140, 15, "#ff6b6b"));
        platforms.add(new Platform(worldWidth * 0.80, worldHeight - 340, 100, 15, "#54a0ff"));

        // 第五层
        platforms.add(new Platform(worldWidth * 0.18, worldHeight - 420, 130, 15, "#ff9ff3"));
        platforms.add(new Platform(worldWidth * 0.48, worldHeight - 420, 180, 15, "#1dd1a1"));
        platforms.add(new Platform(worldWidth * 0.78, worldHeight - 420, 110, 15, "#feca57"));

        // 第六层
        platforms.add(new Platform(worldWidth * 0.08, worldHeight - 500, 90, 15, "#54a0ff"));
        platforms.add(new Platform(worldWidth * 0.35, worldHeight - 500, 150, 15, "#ff6b6b"));
        platforms.add(new Platform(worldWidth * 0.62, worldHeight - 500, 130, 15, "#5f27cd"));
        platforms.add(new Platform(worldWidth * 0.88, worldHeight - 500, 80, 15, "#ff9ff3"));

        // 第七层
        platforms.add(new Platform(worldWidth * 0.20, worldHeight - 580, 120, 15, "#1dd1a1"));
        platforms.add(new Platform(worldWidth * 0.50, worldHeight - 580, 100, 15, "#ffffff"));
        platforms.add(new Platform(worldWidth * 0.75, worldHeight - 580, 90, 15, "#feca57"));

        // 弹簧（底部两侧）
        springs.add(new Spring(30, worldHeight - 35, 50, 15));
        springs.add(new Spring(worldWidth - 80, worldHeight - 35, 50, 15));
    }

    public void update(long dt) {
        if (!running) return;

        double deltaTime = lastTime > 0 ? dt : 16.67;
        lastTime = System.currentTimeMillis();

        // 更新玩家
        for (Player p : players) {
            updatePlayer(p, deltaTime);
        }

        // 更新相机
        updateCamera();

        // 道具刷新
        itemSpawnTimer++;
        if (itemSpawnTimer >= 900) { // 15秒 * 60帧
            if (items.size() < Constants.MAX_ITEMS) {
                spawnItem();
            }
            itemSpawnTimer = 0;
        }

        // 补鼠夹刷新
        trapSpawnTimer++;
        if (trapSpawnTimer >= 1200) { // 20秒 * 60帧
            if (traps.size() < Constants.MAX_TRAPS) {
                spawnTrap();
            }
            trapSpawnTimer = 0;
        }

        // 更新闪电效果
        zapEffects.removeIf(z -> !z.isActive());

        // 碰撞检测和道具拾取
        checkCollisions();
    }

    private void checkCollisions() {
        // 道具拾取检测
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            if (!item.active) continue;

            for (Player p : players) {
                if (item.checkCollision(p)) {
                    applyItemEffect(p, item.type);
                    item.active = false;
                    items.remove(i);
                    break;
                }
            }
        }

        // 玩家碰撞检测
        for (int i = 0; i < players.size(); i++) {
            for (int j = i + 1; j < players.size(); j++) {
                Player p1 = players.get(i);
                Player p2 = players.get(j);

                if (p1.invisible || p2.invisible) continue;

                double dx = p1.x - p2.x;
                double dy = p1.y - p2.y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < p1.size + p2.size) {
                    if (p1.shieldActive) {
                        p1.shieldActive = false;
                        continue;
                    }
                    if (p2.shieldActive) {
                        p2.shieldActive = false;
                        continue;
                    }
                    handleTag(p1, p2);
                }
            }
        }
    }

    private void applyItemEffect(Player p, ItemType type) {
        switch (type) {
            case SMOKE_BOMB:
                p.invisible = true;
                p.invisibleTimer = type.duration;
                break;
            case SPEED_BOOTS:
                p.speedBoost = true;
                p.speedBoostTimer = type.duration;
                p.speedBoostEffect = Constants.PLAYER_SPEED * 2;
                break;
            case TELEPORT_SCROLL:
                p.x += p.facingRight ? 80 : -80;
                p.x = Math.max(p.size, Math.min(worldWidth - p.size, p.x));
                break;
            case TRACKER_SCOPE:
                p.trackerActive = true;
                p.trackerTimer = type.duration;
                break;
            case SPEED_SLOW:
                p.speedSlowActive = true;
                p.speedSlowTimer = type.duration;
                break;
            case CATCH_BOOST:
                p.catchBoostActive = true;
                p.catchBoostTimer = type.duration;
                break;
            case FOG_CLOUD:
                p.fogActive = true;
                p.fogTimer = type.duration;
                p.fogX = p.x;
                p.fogY = p.y;
                break;
            case TIME_STOP:
                p.timeStopActive = true;
                p.timeStopTimer = type.duration;
                break;
            case SHIELD:
                p.shieldActive = true;
                p.shieldTimer = type.duration;
                break;
            case SWAP_CARD:
                Player nearest = null;
                double nearestDist = 100;
                for (Player other : players) {
                    if (other == p) continue;
                    double dx = other.x - p.x;
                    double dy = other.y - p.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = other;
                    }
                }
                if (nearest != null) {
                    double tempX = p.x, tempY = p.y;
                    boolean tempIT = p.isIT;
                    p.x = nearest.x; p.y = nearest.y;
                    p.isIT = nearest.isIT;
                    nearest.x = tempX; nearest.y = tempY;
                    nearest.isIT = tempIT;
                }
                break;
            case ZAP_GUN:
                p.hasZap = true;
                p.zapCooldown = false;
                p.zapCooldownTimer = 0;
                break;
        }
    }

    private void handleTag(Player p1, Player p2) {
        Player itPlayer = p1.isIT ? p1 : (p2.isIT ? p2 : null);
        Player otherPlayer = p1.isIT ? p2 : (p2.isIT ? p1 : null);

        if (itPlayer == null || otherPlayer == null) return;

        double relVx = itPlayer.vx - otherPlayer.vx;
        double dx = itPlayer.x - otherPlayer.x;
        boolean approaching = relVx * dx > 0;

        if (approaching) {
            if (p1.isIT && !p2.isIT) {
                p1.isIT = false;
                p2.isIT = true;
            } else if (!p1.isIT && p2.isIT) {
                p2.isIT = false;
                p1.isIT = true;
            }
        }
    }
    }

    private void updatePlayer(Player p, double dt) {
        // 更新道具效果
        p.updateEffects(dt);

        int ctrlIndex = p.id;

        // 定身状态
        if (p.zapped) {
            p.vx = 0;
            p.vy = 0;
            p.zappedTimer -= dt;
            if (p.zappedTimer <= 0) {
                p.zapped = false;
            }
            p.vy += Constants.GRAVITY;
            p.y += p.vy;
            return;
        }

        // 计算速度
        int actualSpeed = p.getPlayerSpeed();

        // 水平移动
        p.vx = 0;
        if (keys.getOrDefault(CONTROLS_LEFT[ctrlIndex], false)) {
            p.vx = -actualSpeed;
            p.facingRight = false;
        }
        if (keys.getOrDefault(CONTROLS_RIGHT[ctrlIndex], false)) {
            p.vx = actualSpeed;
            p.facingRight = true;
        }

        // 跳跃缓冲
        if (keys.getOrDefault(CONTROLS_JUMP[ctrlIndex], false) && p.jumpBufferTimer == 0) {
            p.jumpBufferTimer = Constants.JUMP_BUFFER;
        }

        // 跳跃判定
        boolean canJump = p.onGround || p.coyoteTimer > 0;
        boolean wantsJump = p.jumpBufferTimer > 0;

        if (canJump && wantsJump) {
            p.vy = -Constants.JUMP_SPEED;
            p.onGround = false;
            p.coyoteTimer = 0;
            p.jumpBufferTimer = 0;
        }

        // 重力（缓降时减半）
        double actualGravity = p.slowFall ? Constants.GRAVITY * 0.4 : Constants.GRAVITY;
        p.vy += actualGravity;

        // 限制下落速度
        int maxFallSpeed = p.slowFall ? 8 : 15;
        if (p.vy > maxFallSpeed) p.vy = maxFallSpeed;

        // 更新位置
        p.x += p.vx;
        p.y += p.vy;

        // 世界边界
        if (p.x < p.size) p.x = p.size;
        if (p.x > worldWidth - p.size) p.x = worldWidth - p.size;

        // 记录跳跃前是否在地上
        boolean wasOnGround = p.onGround;

        // 平台碰撞
        p.onGround = false;
        for (Platform plat : platforms) {
            if (p.vy >= 0 &&
                p.x + p.size > plat.x &&
                p.x - p.size < plat.x + plat.width &&
                p.y + p.size > plat.y &&
                p.y + p.size < plat.y + plat.height + 10) {
                p.y = plat.y - p.size;
                p.vy = 0;
                p.onGround = true;
            }
        }

        // 土狼时间
        if (wasOnGround && !p.onGround && p.vy >= 0) {
            p.coyoteTimer = Constants.COYOTE_TIME;
        }

        // 弹簧碰撞
        for (Spring spring : springs) {
            if (spring.checkCollision(p)) {
                p.vy = -Constants.SPRING_POWER;
                p.onGround = false;
                p.slowFall = true;
                p.slowFallTimer = 60;
            }
        }

        // 补鼠夹碰撞
        for (Trap trap : traps) {
            if (trap.checkCollision(p) && !p.zapped) {
                p.zapped = true;
                p.zappedTimer = Constants.TRAP_STUN_DURATION;
                trap.active = false;
            }
        }
        traps.removeIf(t -> !t.active);

        // 缓降效果
        if (p.slowFall) {
            p.slowFallTimer--;
            if (p.slowFallTimer <= 0) {
                p.slowFall = false;
            }
        }

        // 更新计时器
        if (p.coyoteTimer > 0) p.coyoteTimer--;
        if (p.jumpBufferTimer > 0) p.jumpBufferTimer--;

        // 掉落重生
        if (p.y > worldHeight + 50) {
            p.y = 50;
            p.x = 150 + p.id * 150;
            p.vy = 0;
            p.coyoteTimer = 0;
            p.jumpBufferTimer = 0;
        }
    }

    private void updateCamera() {
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (Player p : players) {
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        double centerY = (minY + maxY) / 2;
        double targetOffsetY = centerY - 250; // 假设画布高度500

        double maxOffsetY = worldHeight - 500;
        double minOffsetY = 0;

        cameraY += (Math.max(minOffsetY, Math.min(maxOffsetY, targetOffsetY)) - cameraY) * 0.1;
    }

    private void spawnItem() {
        Random rand = new Random();
        ItemType[] types = ItemType.values();
        ItemType type = types[rand.nextInt(types.length)];

        Platform plat = platforms.get(rand.nextInt(platforms.size()));
        double x = plat.x + rand.nextDouble() * (plat.width - 40) + 20;
        double y = plat.y - 30;

        items.add(new Item(x, y, type));
    }

    private void spawnTrap() {
        Random rand = new Random();
        Platform plat = platforms.get(rand.nextInt(platforms.size()));
        double x = plat.x + rand.nextDouble() * (plat.width - 30);
        double y = plat.y - 15;

        traps.add(new Trap(x, y));
    }

    public void handleKeyDown(String key) {
        keys.put(key, true);
    }

    public void handleKeyUp(String key) {
        keys.put(key, false);
    }

    // Getters
    public List<Player> getPlayers() { return players; }
    public List<Platform> getPlatforms() { return platforms; }
    public List<Spring> getSprings() { return springs; }
    public List<Trap> getTraps() { return traps; }
    public List<Item> getItems() { return items; }
    public List<ZapEffect> getZapEffects() { return zapEffects; }
    public double getCameraY() { return cameraY; }
    public int getTimeLeft() { return timeLeft; }
    public boolean isRunning() { return running; }
    public double getWorldWidth() { return worldWidth; }
    public double getWorldHeight() { return worldHeight; }

    public void setWorldSize(double width, double height) {
        this.worldWidth = width;
        this.worldHeight = height;
    }
}
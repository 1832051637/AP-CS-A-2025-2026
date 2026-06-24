import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;

@SuppressWarnings({"serial", "this-escape"})
public class Main extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener {

    // =========================
    // 基础配置：窗口、地图尺寸、玩家基础数值
    // =========================
// 【调整1】将窗口分辨率加大到 1024 x 768
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 768;
    private static final int BASE_PLAYER_HP = 100;
    private static final int BASE_MAGAZINE_SIZE = 10;
    private static final int BASE_RESERVE_AMMO = 20;
    private static final int BASE_AMMO_PICKUP = 10;
    private static final int BASE_MEDKIT_HEAL = 40;

    // 中间保留原本 16x16 地图，外圈随机生成更宽的迷宫区域
    private static final int CENTRAL_SIZE = 16;
    private static final int WORLD_SIZE = 48;
    private static final int CENTRAL_OFFSET = (WORLD_SIZE - CENTRAL_SIZE) / 2;
    private static final int CORRIDOR_RADIUS = 1; // 半径 1 意味着随机过道约 3 格宽
    private static final int MAZE_STEP = CORRIDOR_RADIUS * 2 + 2;

    private static final int[][] CENTRAL_MAP = {
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,0,0,0,1,0,0,0,0,0,1,0,0,0,0,1},
        {1,0,1,0,1,0,1,1,1,0,1,0,1,1,0,1},
        {1,0,1,0,0,0,1,0,1,0,0,0,1,0,0,1},
        {1,0,1,1,1,1,1,0,1,1,1,1,1,0,1,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,1,1,0,1,1,1,1,1,1,1,0,1,1,0,1},
        {1,0,1,0,1,0,0,0,0,0,1,0,1,0,0,1},
        {1,0,1,0,1,0,1,1,1,0,1,0,1,1,0,1},
        {1,0,0,0,0,0,1,0,1,0,0,0,0,1,0,1},
        {1,1,1,1,1,0,1,0,1,1,1,1,0,1,0,1},
        {1,0,0,0,1,0,0,0,0,0,0,1,0,0,0,1},
        {1,0,1,0,1,1,1,1,1,1,0,1,1,1,0,1},
        {1,0,1,0,0,0,0,0,0,1,0,0,0,1,0,1},
        {1,0,0,0,1,1,1,1,0,0,0,1,0,0,0,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    // =========================
    // 游戏模式与动作类型枚举
    // =========================
    private enum ScreenState {
        START_MENU,
        PLAYING
    }

    private enum GameMode {
        MAZE,
        SLAUGHTER
    }

    private enum BgmChannel {
        NONE,
        MENU,
        MAZE,
        SLAUGHTER
    }

    private enum EnemyAttackMode {
        MELEE,
        RANGED
    }

    private enum WeaponAttackType {
        HITSCAN,
        FLAME
    }

    private enum BossAction {
        INACTIVE,
        IDLE,
        DASH_CHARGE,
        DASHING,
        LASER_CHARGE,
        LASER_SWEEP,
        BLADE_CHARGE,
        BLADE_ATTACK
    }

    private int[][] map;

    // =========================
    // 地图生成：屠戮竞技场与迷宫模式
    // =========================

    private static int[][] createWorldMap() {
        int[][] map = new int[WORLD_SIZE][WORLD_SIZE];
        for (int x = 0; x < WORLD_SIZE; x++) {
            for (int y = 0; y < WORLD_SIZE; y++) {
                map[x][y] = 0;
            }
        }

        sealBorder(map);
        buildArenaObstacles(map, new Random());
        return map;
    }

    private static void buildArenaObstacles(int[][] map, Random rng) {
        addArenaRect(map, 6, 6, 4, 10);
        addArenaRect(map, 38, 6, 4, 10);
        addArenaRect(map, 6, 32, 4, 10);
        addArenaRect(map, 38, 32, 4, 10);

        addArenaRect(map, 15, 11, 5, 3);
        addArenaRect(map, 28, 11, 5, 3);
        addArenaRect(map, 15, 34, 5, 3);
        addArenaRect(map, 28, 34, 5, 3);

        addArenaRect(map, 11, 21, 7, 4);
        addArenaRect(map, 30, 21, 7, 4);
        addArenaRect(map, 22, 7, 4, 7);
        addArenaRect(map, 22, 34, 4, 7);

        int[][] pillars = {
            {13, 13}, {34, 13}, {13, 34}, {34, 34},
            {18, 18}, {29, 18}, {18, 29}, {29, 29}
        };
        for (int[] pillar : pillars) {
            addArenaRect(map, pillar[0], pillar[1], 2, 2);
        }

        for (int i = 0; i < 10; i++) {
            int w = 2 + rng.nextInt(4);
            int h = 2 + rng.nextInt(5);
            int x = 4 + rng.nextInt(WORLD_SIZE - 8 - w);
            int y = 4 + rng.nextInt(WORLD_SIZE - 8 - h);
            if (Math.abs(x - WORLD_SIZE / 2) < 7 && Math.abs(y - WORLD_SIZE / 2) < 7) {
                continue;
            }
            addArenaRect(map, x, y, w, h);
        }

        clearArenaRect(map, WORLD_SIZE / 2 - 5, WORLD_SIZE / 2 - 5, 10, 10);
        clearArenaRect(map, 2, WORLD_SIZE / 2 - 2, 5, 4);
        clearArenaRect(map, WORLD_SIZE - 7, WORLD_SIZE / 2 - 2, 5, 4);
        sealBorder(map);
    }

    private static void addArenaRect(int[][] map, int startX, int startY, int width, int height) {
        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                if (isInsideWorld(x, y)) {
                    map[x][y] = 1;
                }
            }
        }
    }

    private static void clearArenaRect(int[][] map, int startX, int startY, int width, int height) {
        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                if (isInsideWorld(x, y)) {
                    map[x][y] = 0;
                }
            }
        }
    }

    private static int[][] createMazeModeMap() {
        int[][] map = new int[CENTRAL_SIZE][CENTRAL_SIZE];
        for (int x = 0; x < CENTRAL_SIZE; x++) {
            for (int y = 0; y < CENTRAL_SIZE; y++) {
                map[x][y] = CENTRAL_MAP[x][y];
            }
        }
        return map;
    }

    private static void generateOuterMaze(int[][] map, Random rng) {
        boolean[][] visited = new boolean[WORLD_SIZE][WORLD_SIZE];
        int center = WORLD_SIZE / 2;
        int min = CENTRAL_OFFSET;
        int max = CENTRAL_OFFSET + CENTRAL_SIZE - 1;
        int[][] starts = {
            {snapMazeCenter(min - 2), snapMazeCenter(center)},
            {snapMazeCenter(max + 2), snapMazeCenter(center)},
            {snapMazeCenter(center), snapMazeCenter(min - 2)},
            {snapMazeCenter(center), snapMazeCenter(max + 2)}
        };

        for (int[] start : starts) {
            carveMazeFrom(map, visited, start[0], start[1], rng);
        }

        for (int x = 2; x < WORLD_SIZE - 2; x += MAZE_STEP) {
            for (int y = 2; y < WORLD_SIZE - 2; y += MAZE_STEP) {
                if (isMazeCenter(x, y) && !visited[x][y]) {
                    carveMazeFrom(map, visited, x, y, rng);
                }
            }
        }
    }

    private static void carveMazeFrom(int[][] map, boolean[][] visited, int startX, int startY, Random rng) {
        if (!isMazeCenter(startX, startY)) return;

        List<Point> stack = new ArrayList<>();
        visited[startX][startY] = true;
        carveWide(map, startX, startY, CORRIDOR_RADIUS);
        stack.add(new Point(startX, startY));

        while (!stack.isEmpty()) {
            Point current = stack.get(stack.size() - 1);
            int[][] directions = shuffledDirections(rng);
            boolean moved = false;

            for (int[] dir : directions) {
                int nextX = current.x + dir[0] * MAZE_STEP;
                int nextY = current.y + dir[1] * MAZE_STEP;
                if (!isMazeCenter(nextX, nextY) || visited[nextX][nextY]) {
                    continue;
                }

                carveMazeSegment(map, current.x, current.y, nextX, nextY);
                visited[nextX][nextY] = true;
                stack.add(new Point(nextX, nextY));
                moved = true;
                break;
            }

            if (!moved) {
                stack.remove(stack.size() - 1);
            }
        }
    }

    private static void carveMazeSegment(int[][] map, int fromX, int fromY, int toX, int toY) {
        int steps = Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double)steps;
            int x = (int)Math.round(fromX + (toX - fromX) * t);
            int y = (int)Math.round(fromY + (toY - fromY) * t);
            carveWide(map, x, y, CORRIDOR_RADIUS);
        }
    }

    private static int[][] shuffledDirections(Random rng) {
        int[][] directions = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
        };

        for (int i = directions.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int[] temp = directions[i];
            directions[i] = directions[j];
            directions[j] = temp;
        }
        return directions;
    }

    private static boolean isMazeCenter(int x, int y) {
        return isInsideWorld(x, y) && !isInsideCentralMazeBlock(x, y);
    }

    private static boolean isInsideCentralMazeBlock(int x, int y) {
        int min = CENTRAL_OFFSET - 1;
        int max = CENTRAL_OFFSET + CENTRAL_SIZE;
        return x >= min && x <= max && y >= min && y <= max;
    }

    private static int snapMazeCenter(int value) {
        int center = 2 + Math.round((value - 2) / (float)MAZE_STEP) * MAZE_STEP;
        return clamp(center, 2, WORLD_SIZE - 3);
    }

    private static void openCentralExits(int[][] map) {
        int center = WORLD_SIZE / 2;
        int min = CENTRAL_OFFSET;
        int max = CENTRAL_OFFSET + CENTRAL_SIZE - 1;

        for (int w = 0; w < 2; w++) {
            carveExitLine(map, min, center + w, -1, 0);
            carveExitLine(map, max, center + w, 1, 0);
            carveExitLine(map, center + w, min, 0, -1);
            carveExitLine(map, center + w, max, 0, 1);
        }
    }

    private static void carveExitLine(int[][] map, int x, int y, int dirX, int dirY) {
        for (int i = 0; i < 10; i++) {
            int cx = x + dirX * i;
            int cy = y + dirY * i;
            if (isInsideWorld(cx, cy)) {
                map[cx][cy] = 0;
            }
        }
    }

    private static void carveWide(int[][] map, int centerX, int centerY, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int x = centerX + dx;
                int y = centerY + dy;
                if (isInsideWorld(x, y)) {
                    map[x][y] = 0;
                }
            }
        }
    }

    private static void sealBorder(int[][] map) {
        for (int i = 0; i < WORLD_SIZE; i++) {
            map[0][i] = 1;
            map[WORLD_SIZE - 1][i] = 1;
            map[i][0] = 1;
            map[i][WORLD_SIZE - 1] = 1;
        }
    }

    private static boolean isInsideWorld(int x, int y) {
        return x > 0 && y > 0 && x < WORLD_SIZE - 1 && y < WORLD_SIZE - 1;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    // =========================
    // 数据模型：武器、敌人、Boss、补给、粒子
    // =========================
    private static class WeaponTemplate {
        String name;
        WeaponAttackType attackType;
        int magazineSize;
        int maxReserve;
        int ammoPickupAmount;
        int reloadDuration;
        int fireCooldown;
        int damage;
        double moveSpeedMultiplier;
        BufferedImage idleTexture;
        BufferedImage fireTexture;
        BufferedImage[] reloadTextures;

        WeaponTemplate(String name, WeaponAttackType attackType, int magazineSize, int maxReserve,
                       int ammoPickupAmount, int reloadDuration, int fireCooldown, int damage,
                       double moveSpeedMultiplier, BufferedImage idleTexture, BufferedImage fireTexture,
                       BufferedImage[] reloadTextures) {
            this.name = name;
            this.attackType = attackType;
            this.magazineSize = magazineSize;
            this.maxReserve = maxReserve;
            this.ammoPickupAmount = ammoPickupAmount;
            this.reloadDuration = reloadDuration;
            this.fireCooldown = fireCooldown;
            this.damage = damage;
            this.moveSpeedMultiplier = moveSpeedMultiplier;
            this.idleTexture = idleTexture;
            this.fireTexture = fireTexture;
            this.reloadTextures = reloadTextures;
        }
    }

    private static class WeaponState {
        WeaponTemplate template;
        int currentAmmo;
        int reserveAmmo;
        boolean unlocked;

        WeaponState(WeaponTemplate template, boolean unlocked) {
            this.template = template;
            this.currentAmmo = template.magazineSize;
            this.reserveAmmo = template.maxReserve;
            this.unlocked = unlocked;
        }
    }

    private static class EnemyType {
        String name;
        EnemyAttackMode attackMode;
        int maxHP;
        double speed;
        double sizeScale;
        double hitboxScale;
        double collisionRadius;
        double stopDistance;
        int contactDamage;
        double contactChance;
        int respawnDelay;
        int projectileCooldown;
        double projectileSpeed;
        int projectileDamage;
        int projectileLife;
        int projectileBaseSize;
        double aggroRange;
        Color projectileColor;
        Color fallbackColor;
        BufferedImage texture;

        EnemyType(String name, EnemyAttackMode attackMode, int maxHP, double speed, double sizeScale,
                  double hitboxScale, double collisionRadius, double stopDistance,
                  int contactDamage, double contactChance, int respawnDelay,
                  int projectileCooldown, double projectileSpeed, int projectileDamage,
                  int projectileLife, int projectileBaseSize, double aggroRange, Color projectileColor,
                  Color fallbackColor, BufferedImage texture) {
            this.name = name;
            this.attackMode = attackMode;
            this.maxHP = maxHP;
            this.speed = speed;
            this.sizeScale = sizeScale;
            this.hitboxScale = hitboxScale;
            this.collisionRadius = collisionRadius;
            this.stopDistance = stopDistance;
            this.contactDamage = contactDamage;
            this.contactChance = contactChance;
            this.respawnDelay = respawnDelay;
            this.projectileCooldown = projectileCooldown;
            this.projectileSpeed = projectileSpeed;
            this.projectileDamage = projectileDamage;
            this.projectileLife = projectileLife;
            this.projectileBaseSize = projectileBaseSize;
            this.aggroRange = aggroRange;
            this.projectileColor = projectileColor;
            this.fallbackColor = fallbackColor;
            this.texture = texture;
        }
    }

    private static class Enemy {
        EnemyType type;
        double x, y;
        int hp;
        boolean alive;
        boolean alerted;
        int respawnTimer;
        int attackTimer;
        int lastHitSoundFrame;
        int flameHitsTaken;
        int lastFlameDamageFrame;

        Enemy(EnemyType type, double x, double y) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.hp = type.maxHP;
            this.alive = true;
            this.alerted = true;
            this.respawnTimer = 0;
            this.attackTimer = type.projectileCooldown / 2;
            this.lastHitSoundFrame = -999;
            this.flameHitsTaken = 0;
            this.lastFlameDamageFrame = -999;
        }
    }

    private static class Medkit {
        int x, y;
        boolean active;
        int respawnTimer;
        boolean respawns;

        Medkit(int x, int y, boolean respawns) {
            this.x = x;
            this.y = y;
            this.active = true;
            this.respawnTimer = 0;
            this.respawns = respawns;
        }
    }

    private static class FlameParticle {
        double x, y;
        double velX, velY;
        double startSize;
        double maxSize;
        double screenDrift;
        double travelDistance;
        int age;
        int life;
        int colorSeed;
        boolean smoke;

        FlameParticle(double x, double y, double velX, double velY, double startSize,
                      double maxSize, double screenDrift, int life, int colorSeed, boolean smoke) {
            this.x = x;
            this.y = y;
            this.velX = velX;
            this.velY = velY;
            this.startSize = startSize;
            this.maxSize = maxSize;
            this.screenDrift = screenDrift;
            this.life = life;
            this.colorSeed = colorSeed;
            this.smoke = smoke;
        }

        double progress() {
            return Math.min(1.0, age / (double)Math.max(1, life));
        }

        double currentSize() {
            double t = progress();
            double eased = 1.0 - (1.0 - t) * (1.0 - t);
            return startSize + (maxSize - startSize) * eased;
        }

        boolean canDamage() {
            return !smoke
                && age >= 0
                && travelDistance <= FLAME_DAMAGE_RANGE
                && currentSize() <= FLAME_DAMAGE_MAX_SIZE;
        }

        double damageRadius() {
            double closeBoost = travelDistance < FLAME_CLOSE_DAMAGE_RANGE
                ? (FLAME_CLOSE_DAMAGE_RANGE - travelDistance) * 0.18
                : 0.0;
            return 0.26 + currentSize() * 0.95 + closeBoost;
        }
    }

    private static class AmmoBox {
        int x, y;
        boolean active;
        int respawnTimer;

        AmmoBox(int x, int y) {
            this.x = x;
            this.y = y;
            this.active = true;
            this.respawnTimer = 0;
        }
    }

    private static class EnemyProjectile {
        double x, y;
        double velX, velY;
        int damage;
        int life;
        int baseSize;
        Color color;

        EnemyProjectile(double x, double y, double velX, double velY, int damage, int life,
                        int baseSize, Color color) {
            this.x = x;
            this.y = y;
            this.velX = velX;
            this.velY = velY;
            this.damage = damage;
            this.life = life;
            this.baseSize = baseSize;
            this.color = color;
        }
    }

    private static class BossBladeWave {
        double x, y;
        double dirX, dirY;
        int age;
        int visualFrames;
        boolean hitPlayer;
        int colorSeed;

        BossBladeWave(double x, double y, double dirX, double dirY, int visualFrames, int colorSeed) {
            this.x = x;
            this.y = y;
            this.dirX = dirX;
            this.dirY = dirY;
            this.visualFrames = visualFrames;
            this.colorSeed = colorSeed;
        }

        double progress() {
            return Math.min(1.0, age / (double)Math.max(1, visualFrames));
        }
    }

    private static class Boss {
        double x, y;
        int hp;
        int maxHp;
        int phase;
        boolean spawned;
        boolean alive;
        BossAction action = BossAction.INACTIVE;
        int actionTimer;
        int attackCooldown;
        int lastLaserDamageFrame = -999;
        int lastFlameDamageFrame = -999;
        double dashDirX, dashDirY;
        boolean dashHitPlayer;
        double laserAngle;
        double laserStartAngle;
        double laserTotalSweep;
        double laserSwept;
        double laserSweepDir;
    }

    private static class LaserBeam {
        double startX, startY;
        double endX, endY;
        double dirX, dirY;
        double length;

        LaserBeam(double startX, double startY, double endX, double endY, double dirX, double dirY, double length) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.dirX = dirX;
            this.dirY = dirY;
            this.length = length;
        }
    }

    private static class ExplosionParticle {
        double x, y;
        double velX, velY;
        double height;
        double velHeight;
        int age;
        int life;
        int size;
        Color color;

        ExplosionParticle(double x, double y, double velX, double velY, double height,
                          double velHeight, int life, int size, Color color) {
            this.x = x;
            this.y = y;
            this.velX = velX;
            this.velY = velY;
            this.height = height;
            this.velHeight = velHeight;
            this.life = life;
            this.size = size;
            this.color = color;
        }

        double progress() {
            return Math.min(1.0, age / (double)Math.max(1, life));
        }
    }

    // =========================
    // 游戏状态：玩家、敌人、Boss、输入、资源、UI 计时器
    // =========================
    // 玩家属性
    private ScreenState screenState = ScreenState.START_MENU;
    private GameMode currentMode = GameMode.MAZE;
    private double posX, posY;     
    private double dirX, dirY;   
    private double planeX, planeY; 
    private double playerKnockbackX, playerKnockbackY;
    private int playerHP;
    private int maxPlayerHP = BASE_PLAYER_HP;
    private int killCount;         

    // 敌人系统：复制 EnemyType 模板并改数值，就能添加新敌人类型
    private EnemyType meleeEnemyType;
    private EnemyType rangedEnemyType;
    private List<Enemy> enemies = new ArrayList<>();
    private List<EnemyProjectile> enemyProjectiles = new ArrayList<>();
    private List<BossBladeWave> bossBladeWaves = new ArrayList<>();
    private Boss boss = new Boss();
    private static final int BOSS_SPAWN_KILLS = 200;
    private int nextBossSpawnKills = BOSS_SPAWN_KILLS;
    private int bossesDefeated;
    private static final int BOSS_PHASE_ONE_HP = 55;
    private static final int BOSS_PHASE_TWO_HP = 135;
    private static final int BOSS_DASH_DAMAGE = 250;
    private static final int BOSS_LASER_DAMAGE = 80;
    private static final int BOSS_LASER_DAMAGE_INTERVAL = 12;
    private static final double BOSS_LASER_SWEEP_RADIANS = Math.toRadians(200.0);
    private static final int BOSS_BLADE_DAMAGE = 80;
    private static final double BOSS_BLADE_FAN_RADIANS = Math.toRadians(70.0);
    private static final double BOSS_BLADE_SPEED = 0.155;
    private static final int BOSS_BLADE_VISUAL_FRAMES = 84;
    private static final int[] BOSS_CHEAT_SEQUENCE = {
        KeyEvent.VK_UP, KeyEvent.VK_UP,
        KeyEvent.VK_DOWN, KeyEvent.VK_DOWN,
        KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
        KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
        KeyEvent.VK_B, KeyEvent.VK_A,
        KeyEvent.VK_B, KeyEvent.VK_A
    };
    private int bossCheatProgress;


    // 控制变量
    private boolean moveForward, moveBackward, turnLeft, turnRight;
    private boolean moveLeft, moveRight;
    private boolean firing;
    private int fireCooldownTimer;
    private int isShootingFrame;
    private int meleeSwingTimer;
    private int meleeSwingKills;
    private int meleeHitShakeTimer;
    private boolean meleeKeyDown;
    private boolean sustainedMeleeActive;
    private boolean endMeleeAfterDamageWindow;
    private int meleeHoldFrames;
    private double meleeStance;
    private Set<Enemy> meleeHitEnemies = new HashSet<>();
    private boolean meleeHitBossThisSwing;
    private static final int MELEE_SWING_DURATION = 60;
    private static final double[] MELEE_FRAME_WEIGHTS = {0.14, 0.66, 0.12, 0.08};
    private static final double MELEE_FRONT_RANGE = 1.5;
    private static final double MELEE_BACK_RANGE = 0.5;
    private static final double MELEE_FRONT_DOT = 0.18;
    private static final int MELEE_HIT_SHAKE_DURATION = 14;
    private static final int MELEE_HIT_SHAKE_STRENGTH = 10;
    private static final double MELEE_STANCE_MAX = 100.0;
    private static final double MELEE_STANCE_MIN_ATTACK = MELEE_STANCE_MAX * 0.25;
    private static final double MELEE_STANCE_TAP_COST = MELEE_STANCE_MIN_ATTACK;
    private static final double MELEE_STANCE_DRAIN_PER_FRAME = 0.85;
    private static final double MELEE_STANCE_RECOVER_PER_FRAME = 0.28;
    private static final int MELEE_HOLD_TO_SUSTAIN_FRAMES = 8;
    private int damageEffectTimer;
    private int damageDisplayTimer;
    private int damageDisplayValue;
    private boolean hitEffectIsMelee;
    private int lastMouseX;
    private boolean mouseInitialized;
    private double mouseSensitivity = 0.003;
    private double[] zBuffer = new double[WIDTH]; 
    private Robot robot;
    private boolean grabMouse;
    private Cursor hiddenCursor;
    
    // 暂停系统
    private boolean isPaused;
    private int escPressedTime;
    private int escHoldThreshold = 60; // 长按超过60帧（约1秒）强制退出
    
    // 受击特效和敌人指示器
    private int hitEffectTimer = 0; // 敌人受击特效计时器
    private double hitEffectX = 0, hitEffectY = 0; // 受击特效位置
    private int hitMarkerTimer = 0;
    private boolean hitMarkerKill;
    private final double RADAR_RANGE = 7.0; // 屏幕外敌人指示器范围更短，只有较近的危险会提示
    private BufferedImage enemyTexture;
    private BufferedImage rangedEnemyTexture;
    private BufferedImage enemyHitTexture;
    private BufferedImage meleeHitTexture;
    private BufferedImage medkitTexture;
    private BufferedImage ammoBoxTexture;
    private BufferedImage wallTexture;
    private BufferedImage[] wallTextureColumns;
    private BufferedImage[] wallTextureShadedColumns;
    private BufferedImage weaponIdleTexture;
    private BufferedImage weaponFireTexture;
    private BufferedImage[] weaponReloadTextures = new BufferedImage[3];
    private BufferedImage[] meleeWeaponTextures = new BufferedImage[4];
    private BufferedImage meleeWeaponTexture;
    private static final int WALL_COLUMN_WIDTH = 2;

    // 音频通道：短音效和 BGM 分开，音频文件放到 assets/sounds
    private Clip gunFireSound;
    private Clip reloadSound;
    private Clip meleeAttackSound;
    private Clip enemyHitSound;
    private Clip[] enemyHitSoundVoices = new Clip[8];
    private int enemyHitSoundVoiceIndex;
    private Clip menuBgm;
    private Clip mazeBgm;
    private Clip[] mazeBgmTracks = new Clip[2];
    private Clip[] slaughterBgmTracks = new Clip[4];
    private Clip activeBgm;
    private BgmChannel activeBgmChannel = BgmChannel.NONE;
    private int[] mazeBgmOrder = {0, 1};
    private int mazeBgmOrderPosition = 2;
    private int[] slaughterBgmOrder = {0, 1, 2, 3};
    private int slaughterBgmOrderPosition = 4;
    private Random audioRandom = new Random();
    
    // 弹药与回血补给
    private int currentAmmo;
    private int maxAmmo;
    private int reserveAmmo;
    private int maxReserve = 20;
    private int ammoPickupAmount = 10;
    private boolean isReloading;
    private int reloadTimer; // 计时器，2秒大约120帧
    private int reloadDuration = 60;
    private List<WeaponState> weapons = new ArrayList<>();
    private int currentWeaponIndex = 0;
    private boolean weaponWheelActive;
    private int weaponWheelHoverIndex = -1;
    private boolean flamerUnlockedThisRun;
    private BufferedImage[] bossIdleTextures = new BufferedImage[4];
    private BufferedImage[] bossLaserTextures = new BufferedImage[3];
    private BufferedImage bossDashChargeTexture;
    private BufferedImage bossDashTexture;
    private BufferedImage bossBladeChargeTexture;
    private BufferedImage[] bossBladeTextures = new BufferedImage[3];
    private BufferedImage flamerTexture;
    private BufferedImage flamerFireTexture;
    private BufferedImage[] flamerReloadTextures = new BufferedImage[4];
    private BufferedImage[] flamerFlameFrames = new BufferedImage[3];
    private int flameEffectTimer;
    private int flameEffectSeed;
    private List<FlameParticle> flameParticles = new ArrayList<>();
    private List<ExplosionParticle> explosionParticles = new ArrayList<>();
    private Random flameParticleRandom = new Random();
    private Random explosionRandom = new Random();
    private static final int MAX_FLAME_PARTICLES = 260;
    private static final int FLAME_DAMAGE_INTERVAL = 4;
    private static final int FLAME_HITS_TO_KILL = 5;
    private static final double FLAME_DAMAGE_RANGE = 7.0;
    private static final double FLAME_VISUAL_RANGE = 7.8;
    private static final double FLAME_DAMAGE_MAX_SIZE = 0.52;
    private static final double FLAME_CLOSE_DAMAGE_RANGE = 1.25;

    private int medkitHealAmount = BASE_MEDKIT_HEAL;
    private int medkitRespawnDuration = 600; // 10秒后重生
    private List<Medkit> medkits = new ArrayList<>();

    private static final int AMMO_BOX_COUNT = 5;
    private int ammoBoxCount = AMMO_BOX_COUNT;
    private int maxDroppedAmmoBoxes = 30;
    private int maxDroppedMedkits = 18;
    private List<AmmoBox> ammoBoxes = new ArrayList<>();
    private int ammoBoxRespawnDuration = 600;

    private String pickupMessage = "";
    private int pickupMessageTimer = 0;
    private int hpGainDisplayTimer = 0;
    private int hpGainDisplayValue = 0;
    private int ammoGainDisplayTimer = 0;
    private int ammoGainDisplayValue = 0;
    private int newWeaponAnimationTimer = 0;
    private String newWeaponAnimationName = "";
    private static final int PICKUP_GAIN_DISPLAY_DURATION = 60;
    private int frameCounter = 0;

    public Main() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        setFocusable(true);
        loadResources();
        playBgmForCurrentState();
        map = createMazeModeMap();
        try {
            robot = new Robot();
        } catch (Exception ex) {
            robot = null;
        }
        grabMouse = true;
        BufferedImage cursorImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        hiddenCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "hidden");
        setCursor(Cursor.getDefaultCursor());
    }

    // =========================
    // 游戏初始化与模式切换
    // =========================
    private void startGame(GameMode mode) {
        currentMode = mode;
        initGame();
        screenState = ScreenState.PLAYING;
        playBgmForCurrentState();
        grabMouse = true;
        if (hiddenCursor != null) {
            setCursor(hiddenCursor);
        }
        requestFocusInWindow();
    }

    private int currentModeOffset() {
        return currentMode == GameMode.SLAUGHTER ? CENTRAL_OFFSET : 0;
    }

    private void configureModeStats() {
        if (currentMode == GameMode.SLAUGHTER) {
            maxPlayerHP = BASE_PLAYER_HP * 5;
            maxAmmo = 20;
            maxReserve = BASE_RESERVE_AMMO * 5;
            ammoPickupAmount = 30;
            medkitHealAmount = 150;
            ammoBoxCount = 12;
            reloadDuration = 30;
        } else {
            maxPlayerHP = BASE_PLAYER_HP;
            maxAmmo = BASE_MAGAZINE_SIZE;
            maxReserve = BASE_RESERVE_AMMO;
            ammoPickupAmount = BASE_AMMO_PICKUP;
            medkitHealAmount = BASE_MEDKIT_HEAL;
            ammoBoxCount = AMMO_BOX_COUNT;
            reloadDuration = 60;
        }
    }

    private void initGame() {
        map = currentMode == GameMode.SLAUGHTER ? createWorldMap() : createMazeModeMap();
        int offset = currentModeOffset();

        if (currentMode == GameMode.SLAUGHTER) {
            posX = WORLD_SIZE / 2.0;
            posY = WORLD_SIZE / 2.0;
        } else {
            posX = offset + 1.5;
            posY = offset + 1.5;
        }
        dirX = 1.0; dirY = 0.0;     
        planeX = 0.0; planeY = 0.66; 
        configureModeStats();
        playerHP = maxPlayerHP;
        killCount = 0;
        playerKnockbackX = 0.0;
        playerKnockbackY = 0.0;
        boss = new Boss();
        nextBossSpawnKills = BOSS_SPAWN_KILLS;
        bossesDefeated = 0;
        flamerUnlockedThisRun = false;
        
        createEnemyTypes();
        enemies.clear();
        enemyProjectiles.clear();
        bossBladeWaves.clear();
        flameParticles.clear();
        explosionParticles.clear();
        if (currentMode == GameMode.SLAUGHTER) {
            for (int i = 0; i < 18; i++) {
                addSpawnedEnemy(meleeEnemyType);
            }
            for (int i = 0; i < 8; i++) {
                addSpawnedEnemy(rangedEnemyType);
            }
        } else {
            enemies.add(createEnemy(meleeEnemyType, offset + 5.5, offset + 5.5));
            enemies.add(createEnemy(rangedEnemyType, offset + 12.5, offset + 10.5));
        }

        moveForward = moveBackward = turnLeft = turnRight = false;
        moveLeft = moveRight = false;
        firing = false;
        fireCooldownTimer = 0;
        isShootingFrame = 0;
        enemyHitSoundVoiceIndex = 0;
        stopEnemyHitSoundVoices();
        meleeSwingTimer = 0;
        meleeHitShakeTimer = 0;
        meleeKeyDown = false;
        sustainedMeleeActive = false;
        endMeleeAfterDamageWindow = false;
        meleeHoldFrames = 0;
        meleeStance = MELEE_STANCE_MAX;
        meleeHitEnemies.clear();
        meleeHitBossThisSwing = false;
        bossCheatProgress = 0;
        damageEffectTimer = 0;
        damageDisplayTimer = 0;
        damageDisplayValue = 0;
        mouseInitialized = false;
        isPaused = false;
        escPressedTime = 0;
        hitEffectTimer = 0;
        hitEffectIsMelee = false;
        hitMarkerTimer = 0;
        hitMarkerKill = false;
        createWeapons();
        isReloading = false;
        reloadTimer = 0;
        weaponWheelActive = false;
        weaponWheelHoverIndex = -1;
        flameEffectTimer = 0;
        flameParticles.clear();
        medkits.clear();
        ammoBoxes.clear();
        pickupMessage = "";
        pickupMessageTimer = 0;
        hpGainDisplayTimer = 0;
        hpGainDisplayValue = 0;
        ammoGainDisplayTimer = 0;
        ammoGainDisplayValue = 0;
        newWeaponAnimationTimer = 0;
        newWeaponAnimationName = "";
        spawnMedkits(currentMode == GameMode.SLAUGHTER ? 3 : 1);
        spawnAmmoBoxes(ammoBoxCount);
    }

    private void addSpawnedEnemy(EnemyType type) {
        Enemy enemy = new Enemy(type, posX, posY);
        spawnEnemyRandomly(enemy);
        enemies.add(enemy);
    }

    private Enemy createEnemy(EnemyType type, double x, double y) {
        Enemy enemy = new Enemy(type, x, y);
        enemy.alerted = currentMode != GameMode.SLAUGHTER;
        if (!canEnemyStandAt(enemy, x, y)) {
            spawnEnemyRandomly(enemy);
        }
        return enemy;
    }

    // =========================
    // 敌人与武器模板创建
    // =========================
    private void createEnemyTypes() {
        // 模板参数顺序：
        // 名字, 攻击模式, 血量, 速度, 显示缩放, 命中宽度倍率, 碰撞半径, 停止距离,
        // 近战伤害, 近战触发概率, 复活延迟,
        // 远程冷却, 光弹速度, 光弹伤害, 光弹寿命, 光弹大小, 索敌范围, 光弹颜色,
        // 无贴图备用颜色, 贴图
        boolean slaughterMode = currentMode == GameMode.SLAUGHTER;
        meleeEnemyType = new EnemyType(
            "Melee Demon",
            EnemyAttackMode.MELEE,
            slaughterMode ? 2 : 3,
            slaughterMode ? 0.085 : 0.10,
            slaughterMode ? 0.72 : 0.60,
            1.00,
            0.23,
            0.50,
            slaughterMode ? 10 : 8,
            slaughterMode ? 0.14 : 0.10,
            slaughterMode ? 18 : 30,
            0,
            0.0,
            0,
            0,
            0,
            slaughterMode ? 8.5 : 999.0,
            Color.YELLOW,
            new Color(160, 40, 40),
            enemyTexture
        );

        rangedEnemyType = new EnemyType(
            "Ranged Imp",
            EnemyAttackMode.RANGED,
            slaughterMode ? 1 : 2,
            slaughterMode ? 0.065 : 0.055,
            slaughterMode ? 0.86 : 0.72,
            slaughterMode ? 1.55 : 1.35,
            0.28,
            slaughterMode ? 1.65 : 1.30,
            0,
            0.0,
            slaughterMode ? 55 : 90,
            slaughterMode ? 70 : 95,
            0.055,
            slaughterMode ? 14 : 10,
            slaughterMode ? 280 : 220,
            slaughterMode ? 86 : 70,
            slaughterMode ? 38.0 : 999.0,
            new Color(80, 220, 255),
            new Color(80, 170, 210),
            rangedEnemyTexture
        );
    }

    private void createWeapons() {
        weapons.clear();
        int bolterMagazine = currentMode == GameMode.SLAUGHTER ? 20 : BASE_MAGAZINE_SIZE;
        int bolterReserve = currentMode == GameMode.SLAUGHTER ? BASE_RESERVE_AMMO * 5 : BASE_RESERVE_AMMO;
        int bolterPickup = currentMode == GameMode.SLAUGHTER ? 30 : BASE_AMMO_PICKUP;
        int bolterReload = currentMode == GameMode.SLAUGHTER ? 30 : 60;
        int bolterCooldown = currentMode == GameMode.SLAUGHTER ? 7 : 9;

        WeaponTemplate bolter = new WeaponTemplate(
            "爆弹枪",
            WeaponAttackType.HITSCAN,
            bolterMagazine,
            bolterReserve,
            bolterPickup,
            bolterReload,
            bolterCooldown,
            1,
            1.0,
            weaponIdleTexture,
            weaponFireTexture,
            weaponReloadTextures
        );

        weapons.add(new WeaponState(bolter, true));
        if (flamerUnlockedThisRun) {
            weapons.add(new WeaponState(createFlamerTemplate(bolterMagazine, bolterReserve, bolterPickup, bolterCooldown), true));
        }
        currentWeaponIndex = 0;
        syncAmmoFromCurrentWeapon();
    }

    private WeaponTemplate createFlamerTemplate(int bolterMagazine, int bolterReserve, int bolterPickup, int bolterCooldown) {
        int flamerMagazine = bolterMagazine * 4;
        double magazineRatio = flamerMagazine / (double)Math.max(1, bolterMagazine);
        return new WeaponTemplate(
            "钷素喷火器",
            WeaponAttackType.FLAME,
            flamerMagazine,
            (int)Math.round(bolterReserve * magazineRatio),
            (int)Math.round(bolterPickup * magazineRatio),
            180,
            Math.max(2, bolterCooldown / 2),
            1,
            0.72,
            flamerTexture,
            flamerFireTexture != null ? flamerFireTexture : flamerTexture,
            flamerReloadTextures
        );
    }

    private WeaponState currentWeapon() {
        if (weapons.isEmpty()) {
            return null;
        }
        currentWeaponIndex = clamp(currentWeaponIndex, 0, weapons.size() - 1);
        return weapons.get(currentWeaponIndex);
    }

    private WeaponTemplate currentWeaponTemplate() {
        WeaponState weapon = currentWeapon();
        return weapon != null ? weapon.template : null;
    }

    private void syncAmmoFromCurrentWeapon() {
        WeaponState weapon = currentWeapon();
        if (weapon == null) {
            return;
        }
        maxAmmo = weapon.template.magazineSize;
        maxReserve = weapon.template.maxReserve;
        ammoPickupAmount = weapon.template.ammoPickupAmount;
        reloadDuration = weapon.template.reloadDuration;
        currentAmmo = weapon.currentAmmo;
        reserveAmmo = weapon.reserveAmmo;
    }

    private void syncAmmoToCurrentWeapon() {
        WeaponState weapon = currentWeapon();
        if (weapon == null) {
            return;
        }
        weapon.currentAmmo = currentAmmo;
        weapon.reserveAmmo = reserveAmmo;
    }

    private void switchWeapon(int index) {
        if (index < 0 || index >= weapons.size() || index == currentWeaponIndex || !weapons.get(index).unlocked) {
            return;
        }
        syncAmmoToCurrentWeapon();
        currentWeaponIndex = index;
        isReloading = false;
        reloadTimer = 0;
        fireCooldownTimer = 0;
        isShootingFrame = 0;
        syncAmmoFromCurrentWeapon();
        pickupMessage = weapons.get(index).template.name;
        pickupMessageTimer = 70;
    }

    private void unlockFlamerWeapon() {
        if (flamerUnlockedThisRun) {
            return;
        }
        flamerUnlockedThisRun = true;
        int bolterMagazine = currentMode == GameMode.SLAUGHTER ? 20 : BASE_MAGAZINE_SIZE;
        int bolterReserve = currentMode == GameMode.SLAUGHTER ? BASE_RESERVE_AMMO * 5 : BASE_RESERVE_AMMO;
        int bolterPickup = currentMode == GameMode.SLAUGHTER ? 30 : BASE_AMMO_PICKUP;
        int bolterCooldown = currentMode == GameMode.SLAUGHTER ? 7 : 9;
        weapons.add(new WeaponState(createFlamerTemplate(bolterMagazine, bolterReserve, bolterPickup, bolterCooldown), true));
        switchWeapon(weapons.size() - 1);
        newWeaponAnimationName = "钷素喷火器";
        newWeaponAnimationTimer = 180;
        pickupMessage = "New weapon acquired";
        pickupMessageTimer = 160;
    }

    private void openWeaponWheel() {
        if (screenState != ScreenState.PLAYING || isPaused || weapons.size() <= 1) {
            return;
        }
        weaponWheelActive = true;
        weaponWheelHoverIndex = currentWeaponIndex;
        firing = false;
        if (grabMouse) {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void closeWeaponWheel() {
        if (!weaponWheelActive) {
            return;
        }
        updateWeaponWheelHover();
        if (weaponWheelHoverIndex >= 0) {
            switchWeapon(weaponWheelHoverIndex);
        }
        weaponWheelActive = false;
        weaponWheelHoverIndex = -1;
        mouseInitialized = false;
        if (grabMouse && hiddenCursor != null && !isPaused) {
            setCursor(hiddenCursor);
        }
    }

    private void updateWeaponWheelHover() {
        if (!weaponWheelActive || weapons.isEmpty()) {
            return;
        }
        Point mouse = MouseInfo.getPointerInfo() != null ? MouseInfo.getPointerInfo().getLocation() : null;
        if (mouse == null) {
            weaponWheelHoverIndex = currentWeaponIndex;
            return;
        }
        try {
            Point panel = getLocationOnScreen();
            double dx = mouse.x - (panel.x + WIDTH / 2.0);
            double dy = mouse.y - (panel.y + HEIGHT / 2.0);
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 40) {
                weaponWheelHoverIndex = currentWeaponIndex;
                return;
            }
            double angle = Math.atan2(dy, dx) + Math.PI / 2.0;
            if (angle < 0) angle += Math.PI * 2.0;
            int index = (int)(angle / (Math.PI * 2.0) * weapons.size());
            index = clamp(index, 0, weapons.size() - 1);
            if (weapons.get(index).unlocked) {
                weaponWheelHoverIndex = index;
            }
        } catch (Exception ignored) {
            weaponWheelHoverIndex = currentWeaponIndex;
        }
    }

    // =========================
    // 资源加载：贴图、音效、背景音乐
    // =========================
    private void loadResources() {
        enemyTexture = loadImage(
            "assets/sprites/enemy.png",
            "assets/sprites/demon.png",
            "assets/sprites/monster.png"
        );
        rangedEnemyTexture = loadImage(
            "assets/sprites/ranged_enemy.png",
            "assets/sprites/enemy_ranged.png",
            "assets/sprites/imp.png"
        );
        bossIdleTextures[0] = loadImage("assets/sprites/boss_idle_1.png", "assets/sprites/boss_1.png");
        bossIdleTextures[1] = loadImage("assets/sprites/boss_idle_2.png", "assets/sprites/boss_2.png");
        bossIdleTextures[2] = loadImage("assets/sprites/boss_idle_3.png", "assets/sprites/boss_3.png");
        bossIdleTextures[3] = loadImage("assets/sprites/boss_idle_4.png", "assets/sprites/boss_4.png");
        bossLaserTextures[0] = loadImage("assets/sprites/boss_laser_1.png");
        bossLaserTextures[1] = loadImage("assets/sprites/boss_laser_2.png");
        bossLaserTextures[2] = loadImage("assets/sprites/boss_laser_3.png");
        bossDashChargeTexture = loadImage("assets/sprites/boss_dash_charge.png", "assets/sprites/boss_charge.png");
        bossDashTexture = loadImage("assets/sprites/boss_dash.png", "assets/sprites/boss_dashing.png");
        bossBladeChargeTexture = makeLightBackgroundTransparent(loadImage(
            "assets/sprites/boss_blade_charge.png",
            "assets/sprites/boss_blade_charge.jpg"
        ));
        bossBladeTextures[0] = makeLightBackgroundTransparent(loadImage(
            "assets/sprites/boss_blade_1.png",
            "assets/sprites/boss_blade_1.jpg"
        ));
        bossBladeTextures[1] = makeLightBackgroundTransparent(loadImage(
            "assets/sprites/boss_blade_2.png",
            "assets/sprites/boss_blade_2.jpg"
        ));
        bossBladeTextures[2] = makeLightBackgroundTransparent(loadImage(
            "assets/sprites/boss_blade_3.png",
            "assets/sprites/boss_blade_3.jpg"
        ));
        enemyHitTexture = loadImage(
            "assets/sprites/hit.png",
            "assets/sprites/enemy_hit.png"
        );
        meleeHitTexture = loadImage(
            "assets/sprites/melee_hit.png",
            "assets/sprites/melee_impact.png",
            "assets/sprites/melee_slash_hit.png"
        );
        medkitTexture = loadImage(
            "assets/sprites/medkit.png",
            "assets/sprites/health.png",
            "assets/sprites/health_pack.png"
        );
        ammoBoxTexture = loadImage(
            "assets/sprites/ammo_box.png",
            "assets/sprites/ammo.png",
            "assets/sprites/ammo_pack.png"
        );
        wallTexture = loadImage(
            "assets/sprites/wall_texture.png",
            "assets/sprites/wall.png",
            "assets/sprites/stone_wall.png"
        );
        buildWallTextureColumns();
        weaponIdleTexture = loadImage(
            "assets/sprites/weapon_idle.png",
            "assets/sprites/gun_idle.png"
        );
        weaponFireTexture = loadImage(
            "assets/sprites/weapon_fire.png",
            "assets/sprites/gun_fire.png"
        );
        weaponReloadTextures[0] = loadImage(
            "assets/sprites/weapon_reload_1.png",
            "assets/sprites/gun_reload_1.png"
        );
        weaponReloadTextures[1] = loadImage(
            "assets/sprites/weapon_reload_2.png",
            "assets/sprites/gun_reload_2.png"
        );
        weaponReloadTextures[2] = loadImage(
            "assets/sprites/weapon_reload_3.png",
            "assets/sprites/gun_reload_3.png"
        );
        meleeWeaponTextures[0] = loadImage(
            "assets/sprites/melee_weapon_1.png",
            "assets/sprites/melee_swing_1.png",
            "assets/sprites/chainsword_1.png",
            "assets/sprites/left_melee_1.png"
        );
        meleeWeaponTextures[1] = loadImage(
            "assets/sprites/melee_weapon_2.png",
            "assets/sprites/melee_swing_2.png",
            "assets/sprites/chainsword_2.png",
            "assets/sprites/left_melee_2.png"
        );
        meleeWeaponTextures[2] = loadImage(
            "assets/sprites/melee_weapon_3.png",
            "assets/sprites/melee_swing_3.png",
            "assets/sprites/chainsword_3.png",
            "assets/sprites/left_melee_3.png"
        );
        meleeWeaponTextures[3] = loadImage(
            "assets/sprites/melee_weapon_4.png",
            "assets/sprites/melee_swing_4.png",
            "assets/sprites/chainsword_4.png",
            "assets/sprites/left_melee_4.png"
        );
        meleeWeaponTexture = loadImage(
            "assets/sprites/melee_weapon.png",
            "assets/sprites/chainsword.png",
            "assets/sprites/left_melee.png"
        );
        flamerTexture = loadImage(
            "assets/sprites/flamer.png",
            "assets/sprites/promethium_flamer.png",
            "assets/sprites/flamethrower.png"
        );
        flamerFireTexture = loadImage("assets/sprites/flamer_fire.png");
        flamerReloadTextures[0] = loadImage("assets/sprites/flamer_reload_1.png");
        flamerReloadTextures[1] = loadImage("assets/sprites/flamer_reload_2.png");
        flamerReloadTextures[2] = loadImage("assets/sprites/flamer_reload_3.png");
        flamerReloadTextures[3] = loadImage("assets/sprites/flamer_reload_4.png");
        flamerFlameFrames[0] = loadImage("assets/sprites/flamer_flame_1.png");
        flamerFlameFrames[1] = loadImage("assets/sprites/flamer_flame_2.png");
        flamerFlameFrames[2] = loadImage("assets/sprites/flamer_flame_3.png");
        loadAudioResources();
    }

private static BufferedImage loadImage(String... paths) {
        File baseDir = new File(System.getProperty("user.dir"));
        File sourceDir = getApplicationDirectory();

        for (String path : paths) {
            BufferedImage image = tryReadImage(new File(path));
            if (image != null) return image;

            image = findImageNear(baseDir, path);
            if (image != null) return image;

            image = findImageNear(sourceDir, path);
            if (image != null) return image;

            image = tryReadImageResource(path);
            if (image != null) return image;
        }
        logMissingResource("image", paths);
        return null;
    }

    private static BufferedImage makeLightBackgroundTransparent(BufferedImage source) {
        if (source == null) {
            return null;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();

        boolean[] visited = new boolean[width * height];
        java.util.ArrayDeque<Point> queue = new java.util.ArrayDeque<>();
        for (int x = 0; x < width; x++) {
            queue.add(new Point(x, 0));
            queue.add(new Point(x, height - 1));
        }
        for (int y = 1; y < height - 1; y++) {
            queue.add(new Point(0, y));
            queue.add(new Point(width - 1, y));
        }

        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            if (point.x < 0 || point.y < 0 || point.x >= width || point.y >= height) {
                continue;
            }
            int index = point.y * width + point.x;
            if (visited[index]) {
                continue;
            }
            visited[index] = true;
            int argb = output.getRGB(point.x, point.y);
            if (!isNearWhiteBackground(argb)) {
                continue;
            }
            output.setRGB(point.x, point.y, argb & 0x00FFFFFF);
            queue.add(new Point(point.x + 1, point.y));
            queue.add(new Point(point.x - 1, point.y));
            queue.add(new Point(point.x, point.y + 1));
            queue.add(new Point(point.x, point.y - 1));
        }
        return output;
    }

    private static boolean isNearWhiteBackground(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        return alpha > 0 && red >= 246 && green >= 246 && blue >= 246 && max - min <= 10;
    }

    private static File getApplicationDirectory() {
        try {
            URL location = Main.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return null;
            }
            File source = new File(location.toURI());
            return source.isFile() ? source.getParentFile() : source;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalizeResourcePath(String path) {
        return path.replace('\\', '/');
    }

    private static File findChildDirectoryIgnoreCase(File parent, String name) {
        if (parent == null || !parent.isDirectory()) {
            return null;
        }
        File exact = new File(parent, name);
        if (exact.isDirectory()) {
            return exact;
        }
        File[] children = parent.listFiles();
        if (children == null) {
            return null;
        }
        for (File child : children) {
            if (child.isDirectory() && child.getName().equalsIgnoreCase(name)) {
                return child;
            }
        }
        return null;
    }

    private static void logMissingResource(String kind, String... paths) {
        File appDir = getApplicationDirectory();
        System.err.println("[Resource] Missing " + kind + ": " + String.join(", ", paths));
        System.err.println("[Resource] user.dir=" + System.getProperty("user.dir"));
        System.err.println("[Resource] app.dir=" + (appDir != null ? appDir.getAbsolutePath() : "<unknown>"));
    }

    private static BufferedImage findImageNear(File startDir, String path) {
        File dir = startDir;
        while (dir != null) {
            BufferedImage image = tryReadImageCandidate(dir, path);
            if (image != null) {
                return image;
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    private static BufferedImage tryReadImageCandidate(File dir, String path) {
        if (dir == null) return null;
        String normalized = normalizeResourcePath(path);
        String assetRelative = normalized.startsWith("assets/")
            ? normalized.substring("assets/".length())
            : normalized;

        BufferedImage image = tryReadImage(new File(dir, normalized));
        if (image != null) return image;

        image = tryReadImage(new File(dir, "resources/" + normalized));
        if (image != null) return image;

        image = tryReadImage(new File(dir, "src/main/resources/" + normalized));
        if (image != null) return image;

        if (dir.getName().equalsIgnoreCase("assets")) {
            image = tryReadImage(new File(dir, assetRelative));
            if (image != null) return image;
        }

        File assetsDir = findChildDirectoryIgnoreCase(dir, "assets");
        if (assetsDir != null) {
            image = tryReadImage(new File(assetsDir, assetRelative));
            if (image != null) return image;
        }
        return null;
    }

    private static BufferedImage tryReadImage(File file) {
        if (!file.exists()) return null;
        try {
            return ImageIO.read(file);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BufferedImage tryReadImageResource(String path) {
        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream(normalizeResourcePath(path))) {
            if (stream == null) {
                return null;
            }
            return ImageIO.read(stream);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Clip loadAudio(String... paths) {
        File baseDir = new File(System.getProperty("user.dir"));
        File sourceDir = getApplicationDirectory();

        for (String path : paths) {
            Clip clip = tryReadAudio(new File(path));
            if (clip != null) return clip;

            clip = findAudioNear(baseDir, path);
            if (clip != null) return clip;

            clip = findAudioNear(sourceDir, path);
            if (clip != null) return clip;

            clip = tryReadAudioResource(path);
            if (clip != null) return clip;
        }
        logMissingResource("audio", paths);
        return null;
    }

    private static Clip findAudioNear(File startDir, String path) {
        File dir = startDir;
        while (dir != null) {
            Clip clip = tryReadAudioCandidate(dir, path);
            if (clip != null) {
                return clip;
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    private static Clip tryReadAudioCandidate(File dir, String path) {
        if (dir == null) return null;
        String normalized = normalizeResourcePath(path);
        String assetRelative = normalized.startsWith("assets/")
            ? normalized.substring("assets/".length())
            : normalized;

        Clip clip = tryReadAudio(new File(dir, normalized));
        if (clip != null) return clip;

        clip = tryReadAudio(new File(dir, "resources/" + normalized));
        if (clip != null) return clip;

        clip = tryReadAudio(new File(dir, "src/main/resources/" + normalized));
        if (clip != null) return clip;

        if (dir.getName().equalsIgnoreCase("assets")) {
            clip = tryReadAudio(new File(dir, assetRelative));
            if (clip != null) return clip;
        }

        File assetsDir = findChildDirectoryIgnoreCase(dir, "assets");
        if (assetsDir != null) {
            clip = tryReadAudio(new File(assetsDir, assetRelative));
            if (clip != null) return clip;
        }
        return null;
    }

    private static Clip tryReadAudio(File file) {
        if (!file.exists()) return null;
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Clip tryReadAudioResource(String path) {
        try (InputStream raw = Main.class.getClassLoader().getResourceAsStream(normalizeResourcePath(path))) {
            if (raw == null) {
                return null;
            }
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                return clip;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    // =========================
    // 墙壁贴图预处理与资源读取辅助
    // =========================
    private void buildWallTextureColumns() {
        if (wallTexture == null) {
            wallTextureColumns = null;
            wallTextureShadedColumns = null;
            return;
        }

        int textureWidth = wallTexture.getWidth();
        int textureHeight = wallTexture.getHeight();
        wallTextureColumns = new BufferedImage[textureWidth];
        wallTextureShadedColumns = new BufferedImage[textureWidth];
        for (int x = 0; x < textureWidth; x++) {
            BufferedImage column = new BufferedImage(1, textureHeight, BufferedImage.TYPE_INT_RGB);
            BufferedImage shadedColumn = new BufferedImage(1, textureHeight, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < textureHeight; y++) {
                int rgb = wallTexture.getRGB(x, y);
                column.setRGB(0, y, rgb);

                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                r = (int)(r * 0.68);
                g = (int)(g * 0.68);
                b = (int)(b * 0.68);
                shadedColumn.setRGB(0, y, (r << 16) | (g << 8) | b);
            }
            wallTextureColumns[x] = column;
            wallTextureShadedColumns[x] = shadedColumn;
        }
    }

    private void loadAudioResources() {
        gunFireSound = loadAudio(
            "assets/sounds/gun_fire.wav",
            "assets/sounds/weapon_fire.wav",
            "assets/sounds/fire.wav",
            "assets/sounds/gun_fire.aiff",
            "assets/sounds/gun_fire.au"
        );
        reloadSound = loadAudio(
            "assets/sounds/reload.wav",
            "assets/sounds/weapon_reload.wav",
            "assets/sounds/gun_reload.wav",
            "assets/sounds/reload.aiff",
            "assets/sounds/reload.au"
        );
        meleeAttackSound = loadAudio(
            "assets/sounds/melee_attack.wav",
            "assets/sounds/melee.wav",
            "assets/sounds/chainsword.wav",
            "assets/sounds/melee_attack.aiff",
            "assets/sounds/melee_attack.au"
        );
        enemyHitSound = loadAudio(
            "assets/sounds/enemy_hit.wav",
            "assets/sounds/monster_hit.wav",
            "assets/sounds/hit.wav",
            "assets/sounds/enemy_hit.aiff",
            "assets/sounds/enemy_hit.au"
        );
        for (int i = 0; i < enemyHitSoundVoices.length; i++) {
            enemyHitSoundVoices[i] = loadEnemyHitSound();
        }
        menuBgm = loadAudio(
            "assets/sounds/menu_bgm.wav",
            "assets/sounds/menu.wav",
            "assets/sounds/menu_theme.wav",
            "assets/sounds/menu_bgm.aiff",
            "assets/sounds/menu_bgm.au"
        );
        mazeBgm = loadAudio(
            "assets/sounds/maze_bgm.wav",
            "assets/sounds/maze.wav",
            "assets/sounds/maze_theme.wav",
            "assets/sounds/maze_bgm.aiff",
            "assets/sounds/maze_bgm.au"
        );
        for (int i = 0; i < mazeBgmTracks.length; i++) {
            int trackNumber = i + 1;
            mazeBgmTracks[i] = loadAudio(
                "assets/sounds/maze_bgm_" + trackNumber + ".wav",
                "assets/sounds/maze_" + trackNumber + ".wav",
                "assets/sounds/maze_theme_" + trackNumber + ".wav",
                "assets/sounds/maze_bgm_" + trackNumber + ".aiff",
                "assets/sounds/maze_bgm_" + trackNumber + ".au"
            );
        }
        for (int i = 0; i < slaughterBgmTracks.length; i++) {
            int trackNumber = i + 1;
            slaughterBgmTracks[i] = loadAudio(
                "assets/sounds/slaughter_bgm_" + trackNumber + ".wav",
                "assets/sounds/slaughter_" + trackNumber + ".wav",
                "assets/sounds/slaughter_theme_" + trackNumber + ".wav",
                "assets/sounds/slaughter_bgm_" + trackNumber + ".aiff",
                "assets/sounds/slaughter_bgm_" + trackNumber + ".au"
            );
        }
    }

    private Clip loadEnemyHitSound() {
        return loadAudio(
            "assets/sounds/enemy_hit.wav",
            "assets/sounds/monster_hit.wav",
            "assets/sounds/hit.wav",
            "assets/sounds/enemy_hit.aiff",
            "assets/sounds/enemy_hit.au"
        );
    }

    // =========================
    // 音频播放与 BGM 播放列表
    // =========================
    private void playSound(Clip clip) {
        if (clip == null) return;
        try {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception ignored) {
            // 音频设备不可用时不影响游戏运行
        }
    }

    private void playEnemyHitSound(Enemy enemy) {
        if (enemy != null) {
            enemy.lastHitSoundFrame = frameCounter;
        }
        Clip clip = nextEnemyHitSoundVoice();
        playSound(clip != null ? clip : enemyHitSound);
    }

    private Clip nextEnemyHitSoundVoice() {
        for (int i = 0; i < enemyHitSoundVoices.length; i++) {
            int index = (enemyHitSoundVoiceIndex + i) % enemyHitSoundVoices.length;
            Clip clip = enemyHitSoundVoices[index];
            if (clip != null && !clip.isRunning()) {
                enemyHitSoundVoiceIndex = (index + 1) % enemyHitSoundVoices.length;
                return clip;
            }
        }

        Clip clip = enemyHitSoundVoices[enemyHitSoundVoiceIndex];
        enemyHitSoundVoiceIndex = (enemyHitSoundVoiceIndex + 1) % enemyHitSoundVoices.length;
        return clip;
    }

    private void stopEnemyHitSoundVoices() {
        for (Clip clip : enemyHitSoundVoices) {
            if (clip != null) {
                try {
                    clip.stop();
                    clip.setFramePosition(0);
                } catch (Exception ignored) {
                    // 音频设备不可用时不影响游戏运行
                }
            }
        }
    }

    private void triggerMeleeHitShake() {
        meleeHitShakeTimer = MELEE_HIT_SHAKE_DURATION;
    }

    private void playBgmForCurrentState() {
        if (screenState == ScreenState.START_MENU) {
            playLoopingBgm(menuBgm, BgmChannel.MENU);
        } else if (currentMode == GameMode.SLAUGHTER) {
            startSlaughterPlaylist();
        } else {
            startMazePlaylist();
        }
    }

    private void playLoopingBgm(Clip clip, BgmChannel channel) {
        if (activeBgmChannel == channel && activeBgm == clip && clip != null && clip.isRunning()) {
            return;
        }

        stopActiveBgm();
        activeBgmChannel = channel;
        activeBgm = clip;
        if (activeBgm != null) {
            try {
                activeBgm.setFramePosition(0);
                activeBgm.loop(Clip.LOOP_CONTINUOUSLY);
            } catch (Exception ignored) {
                activeBgm = null;
            }
        }
    }

    private void startSlaughterPlaylist() {
        if (activeBgmChannel == BgmChannel.SLAUGHTER && activeBgm != null && activeBgm.isRunning()) {
            return;
        }

        stopActiveBgm();
        activeBgmChannel = BgmChannel.SLAUGHTER;
        slaughterBgmOrderPosition = slaughterBgmOrder.length;
        playNextSlaughterTrack();
    }

    private void startMazePlaylist() {
        if (activeBgmChannel == BgmChannel.MAZE && activeBgm != null && activeBgm.isRunning()) {
            return;
        }

        if (!hasMazeBgmTracks()) {
            playLoopingBgm(mazeBgm, BgmChannel.MAZE);
            return;
        }

        stopActiveBgm();
        activeBgmChannel = BgmChannel.MAZE;
        mazeBgmOrderPosition = mazeBgmOrder.length;
        playNextMazeTrack();
    }

    private void updateMazePlaylist() {
        if (activeBgmChannel != BgmChannel.MAZE || screenState != ScreenState.PLAYING
            || currentMode != GameMode.MAZE || !hasMazeBgmTracks()) {
            return;
        }
        if (activeBgm == null || !activeBgm.isRunning()) {
            playNextMazeTrack();
        }
    }

    private void playNextMazeTrack() {
        if (!hasMazeBgmTracks()) {
            activeBgm = null;
            return;
        }

        if (activeBgm != null) {
            activeBgm.stop();
            activeBgm.setFramePosition(0);
        }

        for (int i = 0; i < mazeBgmTracks.length * 2; i++) {
            if (mazeBgmOrderPosition >= mazeBgmOrder.length) {
                shuffleMazeBgmOrder();
                mazeBgmOrderPosition = 0;
            }

            int trackIndex = mazeBgmOrder[mazeBgmOrderPosition++];
            Clip nextTrack = mazeBgmTracks[trackIndex];
            if (nextTrack != null) {
                activeBgm = nextTrack;
                try {
                    activeBgm.setFramePosition(0);
                    activeBgm.start();
                } catch (Exception ignored) {
                    activeBgm = null;
                }
                return;
            }
        }
    }

    private void updateSlaughterPlaylist() {
        if (activeBgmChannel != BgmChannel.SLAUGHTER || screenState != ScreenState.PLAYING
            || currentMode != GameMode.SLAUGHTER || !hasSlaughterBgmTracks()) {
            return;
        }
        if (activeBgm == null || !activeBgm.isRunning()) {
            playNextSlaughterTrack();
        }
    }

    private void playNextSlaughterTrack() {
        if (!hasSlaughterBgmTracks()) {
            activeBgm = null;
            return;
        }

        if (activeBgm != null) {
            activeBgm.stop();
            activeBgm.setFramePosition(0);
        }

        for (int i = 0; i < slaughterBgmTracks.length * 2; i++) {
            if (slaughterBgmOrderPosition >= slaughterBgmOrder.length) {
                shuffleSlaughterBgmOrder();
                slaughterBgmOrderPosition = 0;
            }

            int trackIndex = slaughterBgmOrder[slaughterBgmOrderPosition++];
            Clip nextTrack = slaughterBgmTracks[trackIndex];
            if (nextTrack != null) {
                activeBgm = nextTrack;
                try {
                    activeBgm.setFramePosition(0);
                    activeBgm.start();
                } catch (Exception ignored) {
                    activeBgm = null;
                }
                return;
            }
        }
    }

    private void shuffleSlaughterBgmOrder() {
        for (int i = slaughterBgmOrder.length - 1; i > 0; i--) {
            int j = audioRandom.nextInt(i + 1);
            int temp = slaughterBgmOrder[i];
            slaughterBgmOrder[i] = slaughterBgmOrder[j];
            slaughterBgmOrder[j] = temp;
        }
    }

    private void shuffleMazeBgmOrder() {
        for (int i = mazeBgmOrder.length - 1; i > 0; i--) {
            int j = audioRandom.nextInt(i + 1);
            int temp = mazeBgmOrder[i];
            mazeBgmOrder[i] = mazeBgmOrder[j];
            mazeBgmOrder[j] = temp;
        }
    }

    private boolean hasMazeBgmTracks() {
        for (Clip clip : mazeBgmTracks) {
            if (clip != null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSlaughterBgmTracks() {
        for (Clip clip : slaughterBgmTracks) {
            if (clip != null) {
                return true;
            }
        }
        return false;
    }

    private void stopActiveBgm() {
        if (activeBgm != null) {
            try {
                activeBgm.stop();
                activeBgm.setFramePosition(0);
            } catch (Exception ignored) {
                // ignore
            }
        }
        activeBgm = null;
        activeBgmChannel = BgmChannel.NONE;
    }

    // =========================
    // 第一人称武器与屏幕特效渲染
    // =========================
    private BufferedImage getCurrentWeaponTexture() {
        WeaponTemplate weapon = currentWeaponTemplate();
        if (weapon == null) {
            return weaponIdleTexture;
        }
        if (isReloading) {
            int elapsed = reloadDuration - reloadTimer;
            BufferedImage[] reloadTextures = weapon.reloadTextures;
            if (reloadTextures != null && reloadTextures.length > 0) {
                int frameIndex = (int)(elapsed / (double)Math.max(1, reloadDuration) * reloadTextures.length);
                if (frameIndex < 0) frameIndex = 0;
                if (frameIndex >= reloadTextures.length) frameIndex = reloadTextures.length - 1;
                BufferedImage reloadTexture = getReloadWeaponTexture(reloadTextures, frameIndex);
                if (reloadTexture != null) {
                    return reloadTexture;
                }
            }
        }
        if (isShootingFrame > 0 && weapon.fireTexture != null) {
            return weapon.fireTexture;
        }
        return weapon.idleTexture != null ? weapon.idleTexture : weaponIdleTexture;
    }

    private BufferedImage getReloadWeaponTexture(BufferedImage[] reloadTextures, int frameIndex) {
        if (reloadTextures[frameIndex] != null) {
            return reloadTextures[frameIndex];
        }
        for (int i = frameIndex - 1; i >= 0; i--) {
            if (reloadTextures[i] != null) {
                return reloadTextures[i];
            }
        }
        for (int i = frameIndex + 1; i < reloadTextures.length; i++) {
            if (reloadTextures[i] != null) {
                return reloadTextures[i];
            }
        }
        return null;
    }

    private BufferedImage getCurrentMeleeWeaponTexture() {
        if (meleeSwingTimer <= 0) {
            return null;
        }

        int frameIndex = getCurrentMeleeWeaponFrameIndex();
        BufferedImage frameTexture = getMeleeWeaponTexture(frameIndex);
        if (frameTexture != null) {
            return frameTexture;
        }
        return meleeWeaponTexture;
    }

    private int getCurrentMeleeWeaponFrameIndex() {
        if (sustainedMeleeActive) {
            return Math.min(1, meleeWeaponTextures.length - 1);
        }
        int elapsed = MELEE_SWING_DURATION - meleeSwingTimer;
        double progress = elapsed / (double)Math.max(1, MELEE_SWING_DURATION);
        progress = Math.max(0.0, Math.min(0.999, progress));
        double accumulated = 0.0;
        for (int i = 0; i < MELEE_FRAME_WEIGHTS.length; i++) {
            accumulated += MELEE_FRAME_WEIGHTS[i];
            if (progress < accumulated) {
                return Math.min(i, meleeWeaponTextures.length - 1);
            }
        }
        return meleeWeaponTextures.length - 1;
    }

    private BufferedImage getMeleeWeaponTexture(int frameIndex) {
        if (meleeWeaponTextures[frameIndex] != null) {
            return meleeWeaponTextures[frameIndex];
        }
        for (int i = frameIndex - 1; i >= 0; i--) {
            if (meleeWeaponTextures[i] != null) {
                return meleeWeaponTextures[i];
            }
        }
        for (int i = frameIndex + 1; i < meleeWeaponTextures.length; i++) {
            if (meleeWeaponTextures[i] != null) {
                return meleeWeaponTextures[i];
            }
        }
        return null;
    }

    private void drawWeapon(Graphics2D g2d) {
        BufferedImage weaponTexture = getCurrentWeaponTexture();
        if (weaponTexture == null) {
            drawFallbackWeapon(g2d);
            return;
        }

        int recoilX = 0;
        int recoilY = 0;
        if (isShootingFrame > 0 && !isReloading) {
            WeaponTemplate weapon = currentWeaponTemplate();
            boolean flameWeapon = weapon != null && weapon.attackType == WeaponAttackType.FLAME;
            recoilX = (int)(Math.random() * (flameWeapon ? 22 : 10)) - (flameWeapon ? 11 : 4);
            recoilY = (int)(Math.random() * (flameWeapon ? 20 : 12));
        }

        WeaponTemplate weapon = currentWeaponTemplate();
        boolean flameWeapon = weapon != null && weapon.attackType == WeaponAttackType.FLAME;
        double textureAspect = weaponTexture.getWidth() / (double)weaponTexture.getHeight();
        boolean fullFrameWeapon = !flameWeapon && weaponTexture.getWidth() >= WIDTH && textureAspect > 1.5;
        int targetWidth;
        int targetHeight;
        int drawX;
        int drawY;
        if (fullFrameWeapon) {
            targetWidth = WIDTH;
            targetHeight = (int)(targetWidth * weaponTexture.getHeight() / (double)weaponTexture.getWidth());
            drawX = recoilX / 2;
            drawY = HEIGHT - targetHeight + recoilY;
        } else {
            targetWidth = flameWeapon ? 600 : 610;
            targetHeight = (int)(targetWidth * weaponTexture.getHeight() / (double)weaponTexture.getWidth());
            if (targetHeight > HEIGHT - 120) {
                targetHeight = HEIGHT - 120;
                targetWidth = (int)(targetHeight * weaponTexture.getWidth() / (double)weaponTexture.getHeight());
            }
            drawX = WIDTH - targetWidth + (flameWeapon ? 46 : 35) + recoilX;
            drawY = HEIGHT - targetHeight - (flameWeapon ? 0 : 28) + recoilY;
        }
        g2d.drawImage(weaponTexture, drawX, drawY, targetWidth, targetHeight, null);
    }

    private void drawFallbackWeapon(Graphics2D g2d) {
        int gunX = WIDTH - 260;
        int gunY = HEIGHT - 220;
        if (isReloading) {
            int elapsed = reloadDuration - reloadTimer;
            double progress = elapsed / (double)Math.max(1, reloadDuration);
            gunY += (int)(Math.sin(progress * Math.PI) * 55);
            gunX -= (int)(progress * 45);
        } else if (isShootingFrame > 0) {
            gunY += (int)(Math.random() * 15);
            g2d.setColor(new Color(255, 140, 0, 230));
            g2d.fillOval(gunX + 20, gunY - 35, 50, 50);
        }
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(gunX, gunY, 18, 150);
        g2d.fillRect(gunX + 25, gunY, 18, 150);
        g2d.setColor(new Color(80, 45, 20));
        g2d.fillRect(gunX - 5, gunY + 100, 70, 90);
    }

    private void drawMeleeWeapon(Graphics2D g2d) {
        if (currentMode != GameMode.SLAUGHTER || meleeSwingTimer <= 0) {
            return;
        }

        double progress = sustainedMeleeActive ? 0.32 : 1.0 - meleeSwingTimer / (double)MELEE_SWING_DURATION;
        double swing = sustainedMeleeActive ? 1.0 : Math.sin(progress * Math.PI);
        Graphics2D bladeG = (Graphics2D)g2d.create();

        int baseX = 135 + (int)(swing * 170);
        int baseY = HEIGHT - 70 - (int)(swing * 115);
        double rotation = Math.toRadians(-72 + progress * 118);
        bladeG.translate(baseX, baseY);
        bladeG.rotate(rotation);

        BufferedImage meleeTexture = getCurrentMeleeWeaponTexture();
        int meleeFrameIndex = getCurrentMeleeWeaponFrameIndex();
        int shakeX = 0;
        int shakeY = 0;
        if (meleeFrameIndex == 1) {
            shakeX = (int)Math.round(Math.sin(frameCounter * 1.7) * 5 + Math.sin(frameCounter * 0.65) * 2);
            shakeY = (int)Math.round(Math.cos(frameCounter * 1.35) * 4 + Math.sin(frameCounter * 0.9) * 2);
        }
        if (meleeTexture != null) {
            double textureAspect = meleeTexture.getWidth() / (double)meleeTexture.getHeight();
            boolean fullFrameWeapon = meleeTexture.getWidth() >= WIDTH && textureAspect > 1.5;
            if (fullFrameWeapon) {
                bladeG.dispose();
                int targetWidth = WIDTH;
                int targetHeight = (int)(targetWidth * meleeTexture.getHeight() / (double)meleeTexture.getWidth());
                g2d.drawImage(meleeTexture, shakeX, HEIGHT - targetHeight + shakeY, targetWidth, targetHeight, null);
                return;
            }

            int width = 310;
            int height = (int)(width * meleeTexture.getHeight() / (double)meleeTexture.getWidth());
            bladeG.drawImage(meleeTexture, -90 + shakeX, -height + 30 + shakeY, width, height, null);
        } else {
            bladeG.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            bladeG.setColor(new Color(60, 70, 85));
            bladeG.drawLine(0, 0, 185, -185);
            bladeG.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            bladeG.setColor(new Color(220, 225, 230));
            bladeG.drawLine(92, -92, 230, -230);
            bladeG.setStroke(new BasicStroke(5));
            bladeG.setColor(new Color(255, 255, 255, 180));
            bladeG.drawLine(116, -116, 225, -225);
            bladeG.setColor(new Color(35, 105, 210));
            bladeG.fillRect(-35, -18, 72, 34);
            bladeG.setColor(new Color(245, 190, 50));
            bladeG.fillRect(-12, -44, 24, 88);
        }

        int alpha = (int)(130 * swing);
        if (alpha > 0) {
            bladeG.setColor(new Color(255, 245, 190, alpha));
            bladeG.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            bladeG.drawArc(-10, -245, 320, 260, 20, 82);
        }
        bladeG.dispose();
    }

    private void drawFlameEffect(Graphics2D g2d) {
        if (flameParticles.isEmpty()) {
            return;
        }

        Graphics2D flameG = (Graphics2D)g2d.create();
        flameG.setComposite(AlphaComposite.SrcOver);
        flameG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        List<FlameParticle> drawParticles = new ArrayList<>(flameParticles);
        drawParticles.sort((a, b) -> Double.compare(
            (b.x - posX) * (b.x - posX) + (b.y - posY) * (b.y - posY),
            (a.x - posX) * (a.x - posX) + (a.y - posY) * (a.y - posY)
        ));

        double invDet = 1.0 / (planeX * dirY - dirX * planeY);
        for (FlameParticle particle : drawParticles) {
            double relX = particle.x - posX;
            double relY = particle.y - posY;
            double transformX = invDet * (dirY * relX - dirX * relY);
            double transformY = invDet * (-planeY * relX + planeX * relY);

            if (transformY <= 0.08) {
                continue;
            }

            int screenX = (int)((WIDTH / 2) * (1 + transformX / transformY));
            if (screenX < -160 || screenX >= WIDTH + 160) {
                continue;
            }

            int depthX = clamp(screenX, 0, WIDTH - 1);
            if (transformY >= zBuffer[depthX]) {
                continue;
            }

            double progress = particle.progress();
            double flutter = Math.sin((frameCounter + particle.colorSeed) * 0.37) * 10.0 * progress;
            int screenY = HEIGHT / 2
                + (int)Math.round(160.0 * (1.0 - progress) + 32.0 * progress + particle.screenDrift + flutter);
            int size = (int)Math.round(particle.currentSize() * HEIGHT / transformY);
            size = clamp(size, particle.smoke ? 8 : 6, particle.smoke ? 128 : 150);

            int alpha = (int)Math.round((particle.smoke ? 70 : 225) * (1.0 - progress * 0.82));
            alpha = clamp(alpha, 0, 255);
            if (alpha <= 0) {
                continue;
            }

            Color baseColor = flameParticleColor(particle, progress, alpha);
            int glowSize = (int)Math.round(size * (particle.smoke ? 1.35 : 1.65));
            flameG.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha / 4));
            flameG.fillRect(screenX - glowSize / 2, screenY - glowSize / 2, glowSize, glowSize);

            flameG.setColor(baseColor);
            flameG.fillRect(screenX - size / 2, screenY - size / 2, size, size);

            if (!particle.smoke && progress < 0.52) {
                int core = Math.max(3, (int)(size * (0.38 + 0.18 * (1.0 - progress))));
                int coreAlpha = clamp(alpha + 25, 0, 255);
                flameG.setColor(new Color(255, 250, 205, coreAlpha));
                flameG.fillRect(screenX - core / 2, screenY - core / 2, core, core);
            }
        }
        flameG.dispose();
    }

    private void drawExplosionParticles(Graphics2D g2d) {
        if (explosionParticles.isEmpty()) {
            return;
        }
        Graphics2D particleG = (Graphics2D)g2d.create();
        List<ExplosionParticle> drawParticles = new ArrayList<>(explosionParticles);
        drawParticles.sort((a, b) -> Double.compare(
            (b.x - posX) * (b.x - posX) + (b.y - posY) * (b.y - posY),
            (a.x - posX) * (a.x - posX) + (a.y - posY) * (a.y - posY)
        ));
        double invDet = 1.0 / (planeX * dirY - dirX * planeY);
        for (ExplosionParticle particle : drawParticles) {
            double relX = particle.x - posX;
            double relY = particle.y - posY;
            double transformX = invDet * (dirY * relX - dirX * relY);
            double transformY = invDet * (-planeY * relX + planeX * relY);
            if (transformY <= 0.08) {
                continue;
            }
            int screenX = (int)((WIDTH / 2) * (1 + transformX / transformY));
            if (screenX < -120 || screenX >= WIDTH + 120) {
                continue;
            }
            int depthX = clamp(screenX, 0, WIDTH - 1);
            if (transformY >= zBuffer[depthX]) {
                continue;
            }
            int screenY = HEIGHT / 2 - (int)Math.round(particle.height * HEIGHT / transformY);
            double fade = 1.0 - particle.progress();
            int size = (int)Math.round(particle.size * (0.7 + particle.progress() * 1.25) / Math.max(0.35, transformY));
            size = clamp(size, 3, 90);
            int alpha = clamp((int)Math.round(230 * fade), 0, 230);
            if (alpha <= 0) {
                continue;
            }
            Color color = particle.color;
            int glow = Math.max(size + 6, (int)(size * 1.9));
            particleG.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha / 4));
            particleG.fillRect(screenX - glow / 2, screenY - glow / 2, glow, glow);
            particleG.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            particleG.fillRect(screenX - size / 2, screenY - size / 2, size, size);
        }
        particleG.dispose();
    }

    private Color flameParticleColor(FlameParticle particle, double progress, int alpha) {
        if (particle.smoke) {
            int v = clamp((int)Math.round(70 - progress * 35), 18, 80);
            return new Color(v + 18, v, v - 8, alpha);
        }
        if (progress < 0.30) {
            int green = clamp(235 - (int)(particle.colorSeed % 30), 205, 245);
            return new Color(255, green, 78, alpha);
        }
        if (progress < 0.62) {
            int green = clamp(145 - (int)(progress * 55), 70, 150);
            return new Color(255, green, 18, alpha);
        }
        int red = clamp(210 - (int)(progress * 55), 125, 210);
        int green = clamp(48 - (int)(progress * 25), 16, 55);
        return new Color(red, green, 12, alpha);
    }

    private void drawHitMarker(Graphics2D g2d) {
        if (hitMarkerTimer <= 0) {
            return;
        }

        int duration = hitMarkerKill ? 18 : 10;
        double t = hitMarkerTimer / (double)Math.max(1, duration);
        int alpha = clamp((int)Math.round(255 * t), 0, 255);
        int centerX = WIDTH / 2;
        int centerY = HEIGHT / 2;
        int gap = hitMarkerKill ? 16 : 14;
        int spread = (int)Math.round((1.0 - t) * (hitMarkerKill ? 10 : 6));
        int inner = gap + spread;
        int outer = inner + (hitMarkerKill ? 15 : 12);

        Stroke oldStroke = g2d.getStroke();
        Color oldColor = g2d.getColor();
        g2d.setStroke(new BasicStroke(hitMarkerKill ? 3.2f : 2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(hitMarkerKill
            ? new Color(255, 96, 38, alpha)
            : new Color(255, 245, 205, alpha));

        g2d.drawLine(centerX - outer, centerY - outer, centerX - inner, centerY - inner);
        g2d.drawLine(centerX + outer, centerY - outer, centerX + inner, centerY - inner);
        g2d.drawLine(centerX - outer, centerY + outer, centerX - inner, centerY + inner);
        g2d.drawLine(centerX + outer, centerY + outer, centerX + inner, centerY + inner);

        if (hitMarkerKill) {
            int tick = 7 + spread / 2;
            g2d.setColor(new Color(255, 230, 95, alpha));
            g2d.drawLine(centerX - tick, centerY, centerX, centerY + tick);
            g2d.drawLine(centerX, centerY + tick, centerX + tick + 2, centerY - tick);
        }

        g2d.setStroke(oldStroke);
        g2d.setColor(oldColor);
    }

    private void drawNewWeaponAnimation(Graphics2D g2d) {
        if (newWeaponAnimationTimer <= 0) {
            return;
        }
        double progress = 1.0 - newWeaponAnimationTimer / 180.0;
        double intro = Math.min(1.0, progress / 0.22);
        double outro = newWeaponAnimationTimer < 45 ? newWeaponAnimationTimer / 45.0 : 1.0;
        int alpha = clamp((int)Math.round(230 * intro * outro), 0, 230);
        if (alpha <= 0) {
            return;
        }

        Graphics2D animG = (Graphics2D)g2d.create();
        int panelW = 560;
        int panelH = 176;
        int panelX = WIDTH / 2 - panelW / 2;
        int panelY = 120 + (int)Math.round((1.0 - intro) * 34);
        animG.setColor(new Color(0, 0, 0, Math.min(170, alpha)));
        animG.fillRoundRect(panelX, panelY, panelW, panelH, 12, 12);
        animG.setColor(new Color(255, 190, 55, alpha));
        animG.setStroke(new BasicStroke(3));
        animG.drawRoundRect(panelX, panelY, panelW, panelH, 12, 12);

        int sweepX = panelX - 120 + (int)Math.round(progress * (panelW + 240));
        animG.setColor(new Color(255, 245, 180, alpha / 3));
        animG.fillRect(sweepX, panelY + 3, 58, panelH - 6);

        animG.setFont(new Font("Monospaced", Font.BOLD, 18));
        animG.setColor(new Color(255, 235, 140, alpha));
        String title = "NEW WEAPON ACQUIRED";
        animG.drawString(title, WIDTH / 2 - animG.getFontMetrics().stringWidth(title) / 2, panelY + 36);
        animG.setFont(new Font("Dialog", Font.BOLD, 34));
        animG.setColor(new Color(255, 255, 255, alpha));
        String name = newWeaponAnimationName.isEmpty() ? "钷素喷火器" : newWeaponAnimationName;
        animG.drawString(name, WIDTH / 2 - animG.getFontMetrics().stringWidth(name) / 2, panelY + 75);

        BufferedImage weaponImage = flamerFireTexture != null ? flamerFireTexture : flamerTexture;
        if (weaponImage != null) {
            double scalePulse = 1.0 + Math.sin(progress * Math.PI) * 0.08;
            int imageW = (int)Math.round(320 * scalePulse);
            int imageH = (int)Math.round(imageW * weaponImage.getHeight() / (double)Math.max(1, weaponImage.getWidth()));
            int imageX = WIDTH / 2 - imageW / 2;
            int imageY = panelY + 84 - (int)Math.round((1.0 - intro) * 24);
            animG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
            animG.drawImage(weaponImage, imageX, imageY, imageW, imageH, null);
            animG.setComposite(AlphaComposite.SrcOver);
        }
        animG.dispose();
    }

    private void drawWeaponWheel(Graphics2D g2d) {
        if (!weaponWheelActive || weapons.isEmpty()) {
            return;
        }
        Graphics2D wheelG = (Graphics2D)g2d.create();
        wheelG.setColor(new Color(0, 0, 0, 145));
        wheelG.fillRect(0, 0, WIDTH, HEIGHT);

        int centerX = WIDTH / 2;
        int centerY = HEIGHT / 2;
        int radius = 180;
        int innerRadius = 62;
        double arcSize = 360.0 / weapons.size();

        for (int i = 0; i < weapons.size(); i++) {
            WeaponState weapon = weapons.get(i);
            int startAngle = 90 - (int)Math.round(i * arcSize + arcSize);
            int arcAngle = (int)Math.ceil(arcSize);
            boolean hovered = i == weaponWheelHoverIndex;
            boolean selected = i == currentWeaponIndex;

            wheelG.setColor(hovered ? new Color(255, 185, 60, 210)
                    : selected ? new Color(80, 155, 255, 190)
                    : new Color(45, 48, 58, 185));
            wheelG.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, startAngle, arcAngle);
            wheelG.setColor(new Color(0, 0, 0, 185));
            wheelG.fillOval(centerX - innerRadius, centerY - innerRadius, innerRadius * 2, innerRadius * 2);

            double mid = Math.toRadians(i * arcSize + arcSize / 2.0 - 90);
            int labelX = centerX + (int)(Math.cos(mid) * 116);
            int labelY = centerY + (int)(Math.sin(mid) * 116);
            wheelG.setFont(new Font("Monospaced", Font.BOLD, 16));
            wheelG.setColor(Color.WHITE);
            String name = weapon.template.name;
            int textW = wheelG.getFontMetrics().stringWidth(name);
            wheelG.drawString(name, labelX - textW / 2, labelY);
            wheelG.setFont(new Font("Monospaced", Font.BOLD, 13));
            String ammo = weapon.currentAmmo + "/" + weapon.template.magazineSize + " +" + weapon.reserveAmmo;
            int ammoW = wheelG.getFontMetrics().stringWidth(ammo);
            wheelG.setColor(new Color(225, 235, 245));
            wheelG.drawString(ammo, labelX - ammoW / 2, labelY + 20);
        }

        wheelG.setColor(new Color(12, 13, 18, 230));
        wheelG.fillOval(centerX - innerRadius, centerY - innerRadius, innerRadius * 2, innerRadius * 2);
        wheelG.setColor(Color.WHITE);
        wheelG.setFont(new Font("Monospaced", Font.BOLD, 18));
        String label = "·";
        wheelG.drawString(label, centerX - wheelG.getFontMetrics().stringWidth(label) / 2, centerY + 6);
        wheelG.dispose();
    }

    // =========================
    // 主循环：输入、更新、碰撞、计时器
    // =========================
    @Override
    public void run() {
        double baseMoveSpeed = 0.07;
        double rotSpeed = 0.05;

        while (true) {
            // 如果启用鼠标抓取且未暂停，使用 Robot 将鼠标固定在窗口中心并按相对移动旋转视角
            if (screenState == ScreenState.PLAYING && grabMouse && !isPaused && !weaponWheelActive && robot != null) {
                Window win = SwingUtilities.getWindowAncestor(this);
                if (win != null && win.isShowing()) {
                    try {
                        Point winLoc = win.getLocationOnScreen();
                        int centerX = winLoc.x + win.getWidth() / 2;
                        int centerY = winLoc.y + win.getHeight() / 2;
                        Point mousePos = MouseInfo.getPointerInfo().getLocation();
                        int dx = mousePos.x - centerX;
                        double angle = dx * mouseSensitivity; // 取反：鼠标右移视角左转
                        if (angle != 0) {
                            double oldDirX = dirX;
                            dirX = dirX * Math.cos(angle) - dirY * Math.sin(angle);
                            dirY = oldDirX * Math.sin(angle) + dirY * Math.cos(angle);
                            double oldPlaneX = planeX;
                            planeX = planeX * Math.cos(angle) - planeY * Math.sin(angle);
                            planeY = oldPlaneX * Math.sin(angle) + planeY * Math.cos(angle);
                        }
                        robot.mouseMove(centerX, centerY);
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }

            if (screenState == ScreenState.PLAYING && !isPaused && playerHP > 0) {
                if (weaponWheelActive) {
                    updateWeaponWheelHover();
                }
                WeaponTemplate movingWeapon = currentWeaponTemplate();
                double weaponMoveMultiplier = movingWeapon != null ? movingWeapon.moveSpeedMultiplier : 1.0;
                double moveSpeed = (currentMode == GameMode.SLAUGHTER ? baseMoveSpeed * 1.5 : baseMoveSpeed) * weaponMoveMultiplier;
                applyPlayerKnockback();
                if (moveForward) {
                    if (isWalkable(posX + dirX * moveSpeed, posY)) posX += dirX * moveSpeed;
                    if (isWalkable(posX, posY + dirY * moveSpeed)) posY += dirY * moveSpeed;
                }
                if (moveBackward) {
                    if (isWalkable(posX - dirX * moveSpeed, posY)) posX -= dirX * moveSpeed;
                    if (isWalkable(posX, posY - dirY * moveSpeed)) posY -= dirY * moveSpeed;
                }
                if (moveLeft) {
                    if (isWalkable(posX - planeX * moveSpeed, posY)) posX -= planeX * moveSpeed;
                    if (isWalkable(posX, posY - planeY * moveSpeed)) posY -= planeY * moveSpeed;
                }
                if (moveRight) {
                    if (isWalkable(posX + planeX * moveSpeed, posY)) posX += planeX * moveSpeed;
                    if (isWalkable(posX, posY + planeY * moveSpeed)) posY += planeY * moveSpeed;
                }
                if (turnLeft) {
                    double oldDirX = dirX;
                    dirX = dirX * Math.cos(rotSpeed) - dirY * Math.sin(rotSpeed);
                    dirY = oldDirX * Math.sin(rotSpeed) + dirY * Math.cos(rotSpeed);
                    double oldPlaneX = planeX;
                    planeX = planeX * Math.cos(rotSpeed) - planeY * Math.sin(rotSpeed);
                    planeY = oldPlaneX * Math.sin(rotSpeed) + planeY * Math.cos(rotSpeed);
                }
                if (turnRight) {
                    double oldDirX = dirX;
                    dirX = dirX * Math.cos(-rotSpeed) - dirY * Math.sin(-rotSpeed);
                    dirY = oldDirX * Math.sin(-rotSpeed) + dirY * Math.cos(-rotSpeed);
                    double oldPlaneX = planeX;
                    planeX = planeX * Math.cos(-rotSpeed) - planeY * Math.sin(-rotSpeed);
                    planeY = oldPlaneX * Math.sin(-rotSpeed) + planeY * Math.cos(-rotSpeed);
                }

                updateEnemies();
                updateBoss();
                updateEnemyProjectiles();
                updateBossBladeWaves();
                updateMeleeStance();
                applyMeleeDamageWindow();
                if (endMeleeAfterDamageWindow) {
                    endSustainedMelee();
                }
                if (firing) {
                    tryFireWeapon();
                }
                updateFlameParticles();
                updateExplosionParticles();
            }

            if (screenState == ScreenState.PLAYING && !isPaused) {
                if (isReloading) {
                    reloadTimer--;
                    if (reloadTimer <= 0) {
                        isReloading = false;
                        int neededAmmo = maxAmmo - currentAmmo;
                        int loadedAmmo = Math.min(neededAmmo, reserveAmmo);
                        currentAmmo += loadedAmmo;
                        reserveAmmo -= loadedAmmo;
                        syncAmmoToCurrentWeapon();
                        pickupMessage = loadedAmmo > 0 ? "Reloaded" : "No reserve ammo";
                        pickupMessageTimer = 90;
                    }
                }

                for (int i = medkits.size() - 1; i >= 0; i--) {
                    Medkit medkit = medkits.get(i);
                    if (medkit.active) {
                        double dist = Math.sqrt(Math.pow((medkit.x + 0.5) - posX, 2) + Math.pow((medkit.y + 0.5) - posY, 2));
                        if (dist < 0.6) {
                            int oldHP = playerHP;
                            playerHP += medkitHealAmount;
                            if (playerHP > maxPlayerHP) playerHP = maxPlayerHP;
                            int healedAmount = playerHP - oldHP;
                            if (healedAmount > 0) {
                                hpGainDisplayValue = healedAmount;
                                hpGainDisplayTimer = PICKUP_GAIN_DISPLAY_DURATION;
                                pickupMessage = "+" + healedAmount + " HP";
                            } else {
                                pickupMessage = "HP full";
                            }
                            pickupMessageTimer = 120;
                            if (medkit.respawns) {
                                medkit.active = false;
                                medkit.respawnTimer = 0;
                            } else {
                                medkits.remove(i);
                            }
                        }
                    } else if (medkit.respawns) {
                        medkit.respawnTimer++;
                        if (medkit.respawnTimer >= medkitRespawnDuration) {
                            respawnMedkit(medkit);
                            pickupMessage = "Medkit respawned";
                            pickupMessageTimer = 120;
                        }
                    }
                }

                for (AmmoBox ammoBox : ammoBoxes) {
                    if (ammoBox.active) {
                        double dist = Math.sqrt(Math.pow((ammoBox.x + 0.5) - posX, 2) + Math.pow((ammoBox.y + 0.5) - posY, 2));
                        if (dist < 0.6) {
                            if (reserveAmmo < maxReserve) {
                                ammoBox.active = false;
                                ammoBox.respawnTimer = 0;
                                int oldReserveAmmo = reserveAmmo;
                                reserveAmmo += ammoPickupAmount;
                                if (reserveAmmo > maxReserve) reserveAmmo = maxReserve;
                                int pickedAmmo = reserveAmmo - oldReserveAmmo;
                                syncAmmoToCurrentWeapon();
                                ammoGainDisplayValue = pickedAmmo;
                                ammoGainDisplayTimer = PICKUP_GAIN_DISPLAY_DURATION;
                                pickupMessage = "+" + pickedAmmo + " ammo";
                            } else {
                                pickupMessage = "Reserve ammo full";
                            }
                            pickupMessageTimer = 120;
                        }
                    } else {
                        ammoBox.respawnTimer++;
                        if (ammoBox.respawnTimer >= ammoBoxRespawnDuration) {
                            respawnAmmoBox(ammoBox);
                            pickupMessage = "Ammo box respawned";
                            pickupMessageTimer = 120;
                        }
                    }
                }
            }

            if (isShootingFrame > 0) isShootingFrame--;
            if (flameEffectTimer > 0) flameEffectTimer--;
            if (fireCooldownTimer > 0) fireCooldownTimer--;
            if (meleeSwingTimer > 0 && !sustainedMeleeActive) {
                meleeSwingTimer--;
                if (meleeSwingTimer <= 0) {
                    meleeHitEnemies.clear();
                }
            }
            if (meleeHitShakeTimer > 0) meleeHitShakeTimer--;
            if (damageEffectTimer > 0) damageEffectTimer--;
            if (damageDisplayTimer > 0) damageDisplayTimer--;
            if (hitEffectTimer > 0) hitEffectTimer--;
            if (hitMarkerTimer > 0) hitMarkerTimer--;
            if (pickupMessageTimer > 0) pickupMessageTimer--;
            if (hpGainDisplayTimer > 0) hpGainDisplayTimer--;
            if (ammoGainDisplayTimer > 0) ammoGainDisplayTimer--;
            if (newWeaponAnimationTimer > 0) newWeaponAnimationTimer--;
            frameCounter++;
            updateMazePlaylist();
            updateSlaughterPlaylist();

            repaint();
            try { Thread.sleep(16); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    // =========================
    // 敌人、Boss、投射物与技能逻辑
    // =========================
    private void updateEnemies() {
        for (Enemy enemy : enemies) {
            if (enemy.alive) {
                updateAliveEnemy(enemy);
            } else {
                enemy.respawnTimer++;
                if (enemy.respawnTimer > enemy.type.respawnDelay) {
                    spawnEnemyRandomly(enemy);
                }
            }
        }
    }

    private void updateBoss() {
        if (currentMode != GameMode.SLAUGHTER) {
            return;
        }
        if (!boss.alive && killCount >= nextBossSpawnKills) {
            spawnBoss();
        }
        if (!boss.alive) {
            return;
        }

        switch (boss.action) {
            case IDLE:
                updateBossIdle();
                break;
            case DASH_CHARGE:
                updateBossDashCharge();
                break;
            case DASHING:
                updateBossDash();
                break;
            case LASER_CHARGE:
                updateBossLaserCharge();
                break;
            case LASER_SWEEP:
                updateBossLaserSweep();
                break;
            case BLADE_CHARGE:
                updateBossBladeCharge();
                break;
            case BLADE_ATTACK:
                updateBossBladeAttack();
                break;
            default:
                boss.action = BossAction.IDLE;
                break;
        }
    }

    private void spawnBoss() {
        spawnBoss(true);
    }

    private void spawnBoss(boolean advanceSchedule) {
        boss.spawned = true;
        boss.alive = true;
        bossBladeWaves.clear();
        boss.phase = 1;
        boss.maxHp = BOSS_PHASE_ONE_HP;
        boss.hp = boss.maxHp;
        boss.action = BossAction.IDLE;
        boss.attackCooldown = 120;
        Point spawn = findBossSpawnTile();
        boss.x = spawn.x + 0.5;
        boss.y = spawn.y + 0.5;
        if (advanceSchedule) {
            nextBossSpawnKills += BOSS_SPAWN_KILLS;
        }
        pickupMessage = "BOSS AWAKENED";
        pickupMessageTimer = 180;
    }

    private void forceSpawnBossAtCenter() {
        spawnBoss(false);
        pickupMessage = "CHEAT: BOSS SPAWNED";
        pickupMessageTimer = 180;
    }

    private Point findBossSpawnTile() {
        int center = WORLD_SIZE / 2;
        for (int radius = 0; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int x = center + dx;
                    int y = center + dy;
                    if (isValidDropTile(x, y) && canBossStandAt(x + 0.5, y + 0.5)) {
                        return new Point(x, y);
                    }
                }
            }
        }
        return new Point(center, center);
    }

    private void updateBossIdle() {
        boss.attackCooldown--;
        double dx = posX - boss.x;
        double dy = posY - boss.y;
        double dist = Math.max(0.001, Math.sqrt(dx * dx + dy * dy));
        double moveSpeed = boss.phase == 1 ? 0.018 : 0.042;
        if (dist > 1.35) {
            tryMoveBoss(dx / dist * moveSpeed, dy / dist * moveSpeed);
        }
        if (boss.attackCooldown <= 0 && hasLineOfSight(boss.x, boss.y, posX, posY)) {
            chooseBossAttack(dist);
        }
    }

    private void chooseBossAttack(double dist) {
        double roll = Math.random();
        double laserChance = boss.phase == 1 ? 0.18 : 0.34;
        double bladeChance = boss.phase == 2 && dist > 1.45 ? 0.30 : 0.0;
        if (roll < bladeChance) {
            startBossBladeWave();
        } else if (roll < bladeChance + laserChance && dist > 2.2) {
            startBossLaser();
        } else {
            startBossDash();
        }
    }

    private void startBossDash() {
        double dx = posX - boss.x;
        double dy = posY - boss.y;
        double len = Math.max(0.001, Math.sqrt(dx * dx + dy * dy));
        boss.dashDirX = dx / len;
        boss.dashDirY = dy / len;
        boss.dashHitPlayer = false;
        boss.action = BossAction.DASH_CHARGE;
        boss.actionTimer = boss.phase == 1 ? 38 : 18;
        pickupMessage = "BOSS DASH";
        pickupMessageTimer = 45;
    }

    private void updateBossDashCharge() {
        boss.actionTimer--;
        if (boss.actionTimer <= 0) {
            boss.action = BossAction.DASHING;
            boss.actionTimer = boss.phase == 1 ? 54 : 66;
        }
    }

    private void updateBossDash() {
        boss.actionTimer--;
        double speed = boss.phase == 1 ? 0.31 : 0.41;
        double nextX = boss.x + boss.dashDirX * speed;
        double nextY = boss.y + boss.dashDirY * speed;
        if (!canBossStandAt(nextX, nextY)) {
            endBossAttack();
            return;
        }
        boss.x = nextX;
        boss.y = nextY;

        double dx = posX - boss.x;
        double dy = posY - boss.y;
        if (!boss.dashHitPlayer && dx * dx + dy * dy <= 0.85 * 0.85) {
            boss.dashHitPlayer = true;
            applyPlayerDamage(BOSS_DASH_DAMAGE);
            addPlayerKnockback(boss.x - boss.dashDirX, boss.y - boss.dashDirY, 0.55);
            endBossAttack();
            return;
        }

        if (boss.actionTimer <= 0) {
            endBossAttack();
        }
    }

    private void startBossLaser() {
        boss.laserSweepDir = Math.random() < 0.5 ? -1.0 : 1.0;
        boss.laserStartAngle = Math.atan2(posY - boss.y, posX - boss.x);
        boss.laserTotalSweep = BOSS_LASER_SWEEP_RADIANS;
        boss.laserSwept = 0.0;
        boss.laserAngle = boss.laserStartAngle;
        boss.action = BossAction.LASER_CHARGE;
        boss.actionTimer = boss.phase == 1 ? 48 : 24;
        pickupMessage = "BOSS LASER";
        pickupMessageTimer = 55;
    }

    private void updateBossLaserCharge() {
        boss.actionTimer--;
        double target = Math.atan2(posY - boss.y, posX - boss.x);
        boss.laserAngle = approachAngle(boss.laserAngle, target, boss.phase == 1 ? 0.018 : 0.026);
        if (boss.actionTimer <= 0) {
            boss.action = BossAction.LASER_SWEEP;
            boss.actionTimer = 999;
            boss.lastLaserDamageFrame = -999;
            boss.laserAngle = boss.laserStartAngle - boss.laserSweepDir * boss.laserTotalSweep * 0.5;
            boss.laserSwept = 0.0;
        }
    }

    private void updateBossLaserSweep() {
        double sweepSpeed = boss.phase == 1 ? Math.toRadians(0.78) : Math.toRadians(1.05);
        boss.laserAngle += boss.laserSweepDir * sweepSpeed;
        boss.laserSwept += sweepSpeed;
        applyBossLaserDamage();
        if (boss.laserSwept >= boss.laserTotalSweep) {
            endBossAttack();
        }
    }

    private void startBossBladeWave() {
        double dx = posX - boss.x;
        double dy = posY - boss.y;
        double len = Math.max(0.001, Math.sqrt(dx * dx + dy * dy));
        boss.dashDirX = dx / len;
        boss.dashDirY = dy / len;
        boss.action = BossAction.BLADE_CHARGE;
        boss.actionTimer = 20;
        pickupMessage = "BOSS BLADE";
        pickupMessageTimer = 48;
    }

    private void updateBossBladeCharge() {
        boss.actionTimer--;
        double targetX = posX - boss.x;
        double targetY = posY - boss.y;
        double len = Math.max(0.001, Math.sqrt(targetX * targetX + targetY * targetY));
        double targetAngle = Math.atan2(targetY / len, targetX / len);
        double currentAngle = Math.atan2(boss.dashDirY, boss.dashDirX);
        double nextAngle = approachAngle(currentAngle, targetAngle, 0.032);
        boss.dashDirX = Math.cos(nextAngle);
        boss.dashDirY = Math.sin(nextAngle);
        if (boss.actionTimer <= 0) {
            releaseBossBladeWaves();
            boss.action = BossAction.BLADE_ATTACK;
            boss.actionTimer = 28;
        }
    }

    private void updateBossBladeAttack() {
        boss.actionTimer--;
        if (boss.actionTimer <= 0) {
            endBossAttack();
        }
    }

    private void releaseBossBladeWaves() {
        double centerAngle = Math.atan2(boss.dashDirY, boss.dashDirX);
        double[] offsets = {-BOSS_BLADE_FAN_RADIANS * 0.5, 0.0, BOSS_BLADE_FAN_RADIANS * 0.5};
        for (int i = 0; i < offsets.length; i++) {
            double angle = centerAngle + offsets[i];
            double waveDirX = Math.cos(angle);
            double waveDirY = Math.sin(angle);
            bossBladeWaves.add(new BossBladeWave(
                boss.x + waveDirX * 0.88,
                boss.y + waveDirY * 0.88,
                waveDirX,
                waveDirY,
                BOSS_BLADE_VISUAL_FRAMES,
                frameCounter + i * 31
            ));
        }
        while (bossBladeWaves.size() > 18) {
            bossBladeWaves.remove(0);
        }
    }

    private void endBossAttack() {
        boss.action = BossAction.IDLE;
        boss.attackCooldown = boss.phase == 1
            ? 130 + (int)(Math.random() * 85)
            : 62 + (int)(Math.random() * 48);
    }

    private double approachAngle(double current, double target, double maxStep) {
        double diff = normalizeAngle(target - current);
        if (Math.abs(diff) <= maxStep) {
            return target;
        }
        return current + Math.signum(diff) * maxStep;
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= Math.PI * 2.0;
        while (angle < -Math.PI) angle += Math.PI * 2.0;
        return angle;
    }

    private void applyBossLaserDamage() {
        if (frameCounter - boss.lastLaserDamageFrame < BOSS_LASER_DAMAGE_INTERVAL) {
            return;
        }
        LaserBeam beam = bossLaserBeam();
        if (beam == null) {
            return;
        }
        if (isPointInsideLaserBeam(posX, posY, beam, boss.phase == 1 ? 0.42 : 0.55)) {
            boss.lastLaserDamageFrame = frameCounter;
            applyPlayerDamage(BOSS_LASER_DAMAGE);
        }
    }

    private LaserBeam bossLaserBeam() {
        if (!boss.alive) {
            return null;
        }
        double dirX = Math.cos(boss.laserAngle);
        double dirY = Math.sin(boss.laserAngle);
        double startX = boss.x + dirX * 0.58;
        double startY = boss.y + dirY * 0.58;
        double endX = startX;
        double endY = startY;
        double length = 0.0;
        double maxLength = 18.0;
        double step = 0.08;
        for (double d = 0.0; d <= maxLength; d += step) {
            double nx = startX + dirX * d;
            double ny = startY + dirY * d;
            if (!isWalkable(nx, ny)) {
                break;
            }
            endX = nx;
            endY = ny;
            length = d;
        }
        if (length <= 0.12) {
            return null;
        }
        return new LaserBeam(startX, startY, endX, endY, dirX, dirY, length);
    }

    private boolean isPointInsideLaserBeam(double x, double y, LaserBeam beam, double radius) {
        double dx = x - beam.startX;
        double dy = y - beam.startY;
        double forward = dx * beam.dirX + dy * beam.dirY;
        if (forward < 0.0 || forward > beam.length) {
            return false;
        }
        double side = Math.abs(dx * -beam.dirY + dy * beam.dirX);
        return side <= radius && hasLineOfSight(beam.startX, beam.startY, x, y);
    }

    private boolean canBossStandAt(double x, double y) {
        double radius = 0.48;
        return isWalkable(x, y)
            && isWalkable(x - radius, y - radius)
            && isWalkable(x + radius, y - radius)
            && isWalkable(x - radius, y + radius)
            && isWalkable(x + radius, y + radius)
            && isWalkable(x - radius, y)
            && isWalkable(x + radius, y)
            && isWalkable(x, y - radius)
            && isWalkable(x, y + radius);
    }

    private boolean tryMoveBoss(double dx, double dy) {
        double nextX = boss.x + dx;
        double nextY = boss.y + dy;
        if (canBossStandAt(nextX, nextY)) {
            boss.x = nextX;
            boss.y = nextY;
            return true;
        }

        boolean moved = false;
        if (canBossStandAt(boss.x + dx, boss.y)) {
            boss.x += dx;
            moved = true;
        }
        if (canBossStandAt(boss.x, boss.y + dy)) {
            boss.y += dy;
            moved = true;
        }
        if (moved) {
            return true;
        }

        double sideX = -dy * 0.85;
        double sideY = dx * 0.85;
        if (canBossStandAt(boss.x + sideX, boss.y + sideY)) {
            boss.x += sideX;
            boss.y += sideY;
            return true;
        }
        if (canBossStandAt(boss.x - sideX, boss.y - sideY)) {
            boss.x -= sideX;
            boss.y -= sideY;
            return true;
        }
        return false;
    }

    private void updateAliveEnemy(Enemy enemy) {
        if (!canEnemyStandAt(enemy, enemy.x, enemy.y)) {
            moveEnemyToNearestSafeTile(enemy);
        }

        double dx = posX - enemy.x;
        double dy = posY - enemy.y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= 0.0001) return;

        if (currentMode == GameMode.SLAUGHTER && !enemy.alerted) {
            if (dist <= enemy.type.aggroRange && canSeePlayer(enemy.x, enemy.y)) {
                enemy.alerted = true;
            } else {
                return;
            }
        }

        if (enemy.type.attackMode == EnemyAttackMode.RANGED) {
            updateRangedEnemy(enemy, dx, dy, dist);
            return;
        }

        if (dist > enemy.type.stopDistance) {
            moveEnemyToward(enemy, dx / dist, dy / dist);
        } else {
            if (Math.random() < enemy.type.contactChance) {
                applyPlayerDamage(enemy.type.contactDamage);
            }
        }
    }

    private void updateRangedEnemy(Enemy enemy, double dx, double dy, double dist) {
        if (dist > enemy.type.aggroRange) {
            return;
        }

        boolean seesPlayer = canSeePlayer(enemy.x, enemy.y);
        if (seesPlayer) {
            enemy.attackTimer--;
            if (enemy.attackTimer <= 0) {
                fireEnemyProjectile(enemy, dx / dist, dy / dist);
                enemy.attackTimer = enemy.type.projectileCooldown;
            }
            return;
        }

        if (dist > enemy.type.stopDistance) {
            moveEnemyToward(enemy, dx / dist, dy / dist);
        }
    }

    private void moveEnemyToward(Enemy enemy, double moveDirX, double moveDirY) {
        double stepX = moveDirX * enemy.type.speed;
        double stepY = moveDirY * enemy.type.speed;
        if (tryMoveEnemy(enemy, stepX, stepY)) {
            return;
        }

        double sideX = -stepY;
        double sideY = stepX;
        if (tryMoveEnemy(enemy, sideX, sideY)) {
            return;
        }
        tryMoveEnemy(enemy, -sideX, -sideY);
    }

    private boolean tryMoveEnemy(Enemy enemy, double stepX, double stepY) {
        double nextX = enemy.x + stepX;
        double nextY = enemy.y + stepY;
        if (canEnemyStandAt(enemy, nextX, nextY)) {
            enemy.x = nextX;
            enemy.y = nextY;
            return true;
        }

        boolean moved = false;
        if (canEnemyStandAt(enemy, nextX, enemy.y)) {
            enemy.x = nextX;
            moved = true;
        }
        if (canEnemyStandAt(enemy, enemy.x, nextY)) {
            enemy.y = nextY;
            moved = true;
        }
        return moved;
    }

    private void fireEnemyProjectile(Enemy enemy, double dirToPlayerX, double dirToPlayerY) {
        double startX = enemy.x + dirToPlayerX * 0.35;
        double startY = enemy.y + dirToPlayerY * 0.35;
        enemyProjectiles.add(new EnemyProjectile(
            startX,
            startY,
            dirToPlayerX * enemy.type.projectileSpeed,
            dirToPlayerY * enemy.type.projectileSpeed,
            enemy.type.projectileDamage,
            enemy.type.projectileLife,
            enemy.type.projectileBaseSize,
            enemy.type.projectileColor
        ));
    }

    private void updateEnemyProjectiles() {
        for (int i = enemyProjectiles.size() - 1; i >= 0; i--) {
            EnemyProjectile projectile = enemyProjectiles.get(i);
            projectile.x += projectile.velX;
            projectile.y += projectile.velY;
            projectile.life--;

            if (projectile.life <= 0 || projectile.x < 0 || projectile.y < 0 ||
                projectile.x >= map.length || projectile.y >= map[0].length ||
                map[(int)projectile.x][(int)projectile.y] != 0) {
                enemyProjectiles.remove(i);
                continue;
            }

            double dx = projectile.x - posX;
            double dy = projectile.y - posY;
            if (dx * dx + dy * dy < 0.22) {
                applyPlayerDamage(projectile.damage);
                enemyProjectiles.remove(i);
            }
        }
    }

    private void updateBossBladeWaves() {
        for (int i = bossBladeWaves.size() - 1; i >= 0; i--) {
            BossBladeWave wave = bossBladeWaves.get(i);
            wave.age++;
            wave.x += wave.dirX * BOSS_BLADE_SPEED;
            wave.y += wave.dirY * BOSS_BLADE_SPEED;

            if (wave.x < 0 || wave.y < 0
                    || wave.x >= map.length || wave.y >= map[0].length
                    || !isWalkable(wave.x, wave.y)) {
                bossBladeWaves.remove(i);
                continue;
            }

            if (!wave.hitPlayer && isPlayerInsideBossBladeWave(wave)) {
                wave.hitPlayer = true;
                applyPlayerDamage(BOSS_BLADE_DAMAGE);
                bossBladeWaves.remove(i);
            }
        }
    }

    private boolean isPlayerInsideBossBladeWave(BossBladeWave wave) {
        double dx = posX - wave.x;
        double dy = posY - wave.y;
        double forward = dx * wave.dirX + dy * wave.dirY;
        double side = dx * -wave.dirY + dy * wave.dirX;
        double halfWidth = 0.95 + wave.progress() * 0.22;
        return forward > -0.42
            && forward < 0.68
            && Math.abs(side) < halfWidth
            && hasLineOfSight(wave.x, wave.y, posX, posY);
    }

    private boolean canSeePlayer(double fromX, double fromY) {
        return hasLineOfSight(fromX, fromY, posX, posY);
    }

    private boolean hasLineOfSight(double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        int steps = (int)(dist * 12);
        if (steps <= 0) return true;
        for (int i = 1; i < steps; i++) {
            double t = i / (double)steps;
            int mx = (int)(fromX + dx * t);
            int my = (int)(fromY + dy * t);
            if (mx < 0 || my < 0 || mx >= map.length || my >= map[0].length || map[mx][my] != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean canEnemyStandAt(Enemy enemy, double x, double y) {
        double radius = enemy.type.collisionRadius;
        return isWalkable(x, y)
            && isWalkable(x - radius, y - radius)
            && isWalkable(x + radius, y - radius)
            && isWalkable(x - radius, y + radius)
            && isWalkable(x + radius, y + radius)
            && isWalkable(x - radius, y)
            && isWalkable(x + radius, y)
            && isWalkable(x, y - radius)
            && isWalkable(x, y + radius);
    }

    private boolean isWalkable(double x, double y) {
        if (x < 0 || y < 0 || x >= map.length || y >= map[0].length) {
            return false;
        }
        return map[(int)x][(int)y] == 0;
    }

    private void applyPlayerKnockback() {
        if (Math.abs(playerKnockbackX) < 0.002 && Math.abs(playerKnockbackY) < 0.002) {
            playerKnockbackX = 0.0;
            playerKnockbackY = 0.0;
            return;
        }
        if (isWalkable(posX + playerKnockbackX, posY)) {
            posX += playerKnockbackX;
        } else {
            playerKnockbackX = 0.0;
        }
        if (isWalkable(posX, posY + playerKnockbackY)) {
            posY += playerKnockbackY;
        } else {
            playerKnockbackY = 0.0;
        }
        playerKnockbackX *= 0.82;
        playerKnockbackY *= 0.82;
    }

    private void addPlayerKnockback(double fromX, double fromY, double strength) {
        double dx = posX - fromX;
        double dy = posY - fromY;
        double len = Math.max(0.001, Math.sqrt(dx * dx + dy * dy));
        playerKnockbackX += dx / len * strength;
        playerKnockbackY += dy / len * strength;
    }

    private void moveEnemyToNearestSafeTile(Enemy enemy) {
        int baseX = (int)enemy.x;
        int baseY = (int)enemy.y;
        for (int radius = 0; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    double candidateX = baseX + dx + 0.5;
                    double candidateY = baseY + dy + 0.5;
                    if (canEnemyStandAt(enemy, candidateX, candidateY)) {
                        enemy.x = candidateX;
                        enemy.y = candidateY;
                        return;
                    }
                }
            }
        }
        spawnEnemyRandomly(enemy);
    }

    private void spawnEnemyRandomly(Enemy enemy) {
        if (currentMode == GameMode.SLAUGHTER && enemy.type.attackMode == EnemyAttackMode.MELEE
            && spawnEnemyNearPlayer(enemy)) {
            return;
        }

        for (int attempts = 0; attempts < 1500; attempts++) {
            int rx = randomInnerMapCellX();
            int ry = randomInnerMapCellY();
            double spawnX = rx + 0.5;
            double spawnY = ry + 0.5;
            if (canEnemyStandAt(enemy, spawnX, spawnY)) {
                double distToPlayer = Math.sqrt(Math.pow(spawnX - posX, 2) + Math.pow(spawnY - posY, 2));
                double minSpawnDistance = currentMode == GameMode.SLAUGHTER ? 10.0 : 4.0;
                if (distToPlayer > minSpawnDistance) { 
                    enemy.x = spawnX;
                    enemy.y = spawnY;
                    enemy.hp = enemy.type.maxHP;
                    enemy.flameHitsTaken = 0;
                    enemy.lastFlameDamageFrame = -999;
                    enemy.alive = true;
                    enemy.alerted = currentMode != GameMode.SLAUGHTER;
                    enemy.respawnTimer = 0;
                    enemy.attackTimer = enemy.type.projectileCooldown / 2;
                    return;
                }
            }
        }
        moveEnemyToNearestSafeTile(enemy);
        enemy.hp = enemy.type.maxHP;
        enemy.flameHitsTaken = 0;
        enemy.lastFlameDamageFrame = -999;
        enemy.alive = true;
        enemy.alerted = currentMode != GameMode.SLAUGHTER;
        enemy.respawnTimer = 0;
        enemy.attackTimer = enemy.type.projectileCooldown / 2;
    }

    private boolean spawnEnemyNearPlayer(Enemy enemy) {
        int centerX = (int)posX;
        int centerY = (int)posY;
        for (int attempts = 0; attempts < 180; attempts++) {
            int rx = centerX - 13 + (int)(Math.random() * 27);
            int ry = centerY - 13 + (int)(Math.random() * 27);
            if (rx <= 0 || ry <= 0 || rx >= map.length - 1 || ry >= map[0].length - 1) {
                continue;
            }
            double spawnX = rx + 0.5;
            double spawnY = ry + 0.5;
            double dx = spawnX - posX;
            double dy = spawnY - posY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist >= 8.0 && dist <= 14.0 && canEnemyStandAt(enemy, spawnX, spawnY)) {
                enemy.x = spawnX;
                enemy.y = spawnY;
                enemy.hp = enemy.type.maxHP;
                enemy.flameHitsTaken = 0;
                enemy.lastFlameDamageFrame = -999;
                enemy.alive = true;
                enemy.alerted = false;
                enemy.respawnTimer = 0;
                enemy.attackTimer = enemy.type.projectileCooldown / 2;
                return true;
            }
        }
        return false;
    }

    private void spawnMedkits(int count) {
        for (int i = 0; i < count; i++) {
            Medkit medkit = new Medkit(-1, -1, true);
            respawnMedkit(medkit);
            medkits.add(medkit);
        }
    }

    private void respawnMedkit(Medkit medkit) {
        medkit.active = false;
        for (int attempts = 0; attempts < 1000; attempts++) {
            int rx = randomInnerMapCellX();
            int ry = randomInnerMapCellY();
            if (isValidDropTile(rx, ry) && !isMedkitAt(rx, ry)) {
                double distToPlayer = Math.sqrt(Math.pow((rx + 0.5) - posX, 2) + Math.pow((ry + 0.5) - posY, 2));
                if (distToPlayer > 3.0) {
                    medkit.x = rx;
                    medkit.y = ry;
                    medkit.active = true;
                    medkit.respawnTimer = 0;
                    return;
                }
            }
        }
    }

    private void spawnAmmoBoxes(int count) {
        for (int i = 0; i < count; i++) {
            AmmoBox ammoBox = new AmmoBox(-1, -1);
            respawnAmmoBox(ammoBox);
            ammoBoxes.add(ammoBox);
        }
    }

    private void respawnAmmoBox(AmmoBox ammoBox) {
        ammoBox.active = false;
        for (int attempts = 0; attempts < 1000; attempts++) {
            int rx = randomInnerMapCellX();
            int ry = randomInnerMapCellY();
            if (isValidDropTile(rx, ry) && !isAmmoBoxAt(rx, ry)) {
                double distToPlayer = Math.sqrt(Math.pow((rx + 0.5) - posX, 2) + Math.pow((ry + 0.5) - posY, 2));
                if (distToPlayer > 3.0) {
                    ammoBox.x = rx;
                    ammoBox.y = ry;
                    ammoBox.active = true;
                    ammoBox.respawnTimer = 0;
                    break;
                }
            }
        }
    }

    private boolean isAmmoBoxAt(int x, int y) {
        for (AmmoBox ammoBox : ammoBoxes) {
            if (ammoBox.active && ammoBox.x == x && ammoBox.y == y) {
                return true;
            }
        }
        return false;
    }

    private boolean isMedkitAt(int x, int y) {
        for (Medkit medkit : medkits) {
            if (medkit.active && medkit.x == x && medkit.y == y) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidDropTile(int x, int y) {
        return x >= 0 && y >= 0 && x < map.length && y < map[0].length && map[x][y] == 0;
    }

    private int randomInnerMapCellX() {
        return 1 + (int)(Math.random() * (map.length - 2));
    }

    private int randomInnerMapCellY() {
        return 1 + (int)(Math.random() * (map[0].length - 2));
    }

    private void applyPlayerDamage(int damage) {
        if (playerHP <= 0) return;
        playerHP -= damage;
        if (playerHP < 0) playerHP = 0;
        damageEffectTimer = 30;
        damageDisplayTimer = 30;
        damageDisplayValue = damage;
    }

    private void damageEnemy(Enemy enemy, int damage, boolean meleeKill) {
        boolean willKill = enemy.hp - damage <= 0;
        wakeEnemyOnHit(enemy);
        enemy.hp -= damage;
        playEnemyHitSound(enemy);
        triggerHitMarker(willKill);
        if (meleeKill) {
            triggerMeleeHitShake();
        }
        hitEffectTimer = 20;
        hitEffectX = enemy.x;
        hitEffectY = enemy.y;
        hitEffectIsMelee = meleeKill;
        if (enemy.hp <= 0) {
            killEnemy(enemy, meleeKill);
        }
    }

    private void damageBoss(int damage) {
        if (!boss.alive) {
            return;
        }
        boss.hp -= damage;
        boolean willKill = boss.phase == 2 && boss.hp <= 0;
        triggerHitMarker(willKill);
        hitEffectTimer = 16;
        hitEffectX = boss.x;
        hitEffectY = boss.y;
        hitEffectIsMelee = false;

        if (boss.hp > 0) {
            return;
        }

        if (boss.phase == 1) {
            boss.phase = 2;
            boss.maxHp = BOSS_PHASE_TWO_HP;
            boss.hp = boss.maxHp;
            boss.action = BossAction.IDLE;
            boss.attackCooldown = 35;
            pickupMessage = "BOSS PHASE 2";
            pickupMessageTimer = 150;
        } else {
            spawnBossExplosion(boss.x, boss.y);
            bossBladeWaves.clear();
            boss.alive = false;
            boss.action = BossAction.INACTIVE;
            boss.hp = 0;
            bossesDefeated++;
            if (bossesDefeated == 1) {
                unlockFlamerWeapon();
                pickupMessage = "New weapon acquired";
                pickupMessageTimer = 180;
            } else {
                pickupMessage = "BOSS SLAIN";
                pickupMessageTimer = 180;
            }
            triggerHitMarker(true);
        }
    }

    private void triggerHitMarker(boolean kill) {
        hitMarkerTimer = kill ? 18 : Math.max(hitMarkerTimer, 10);
        hitMarkerKill = kill || (hitMarkerKill && hitMarkerTimer > 0);
    }

    private void wakeEnemyOnHit(Enemy enemy) {
        if (currentMode != GameMode.SLAUGHTER || enemy.alerted) {
            return;
        }

        enemy.alerted = true;
        alertNearbyEnemies(enemy);
    }

    private void alertNearbyEnemies(Enemy source) {
        double alertRadius = 7.5;
        double alertRadiusSq = alertRadius * alertRadius;
        for (Enemy other : enemies) {
            if (other == source || !other.alive || other.alerted) {
                continue;
            }

            double dx = other.x - source.x;
            double dy = other.y - source.y;
            if (dx * dx + dy * dy <= alertRadiusSq) {
                other.alerted = true;
            }
        }
    }

    private void killEnemy(Enemy enemy, boolean meleeKill) {
        enemy.alive = false;
        enemy.respawnTimer = 0;
        enemy.hp = 0;
        enemy.flameHitsTaken = 0;
        enemy.lastFlameDamageFrame = -999;
        killCount++;
        maybeDropSupplies(enemy, meleeKill);
    }

    private void updateMeleeStance() {
        if (currentMode != GameMode.SLAUGHTER || playerHP <= 0 || isPaused) {
            cancelMeleeAttack();
            return;
        }

        if (meleeKeyDown) {
            meleeHoldFrames++;
        }

        int meleeFrameIndex = meleeSwingTimer > 0 ? getCurrentMeleeWeaponFrameIndex() : -1;
        if (meleeSwingTimer > 0 && meleeKeyDown && meleeHoldFrames >= MELEE_HOLD_TO_SUSTAIN_FRAMES
            && (sustainedMeleeActive || meleeFrameIndex == 1)) {
            sustainedMeleeActive = true;
            meleeSwingTimer = MELEE_SWING_DURATION - (int)Math.ceil(MELEE_SWING_DURATION * MELEE_FRAME_WEIGHTS[0]);
            meleeStance -= MELEE_STANCE_DRAIN_PER_FRAME;
            if (meleeStance <= 0.0) {
                meleeStance = 0.0;
                endMeleeAfterDamageWindow = true;
            }
            return;
        }

        sustainedMeleeActive = false;
        endMeleeAfterDamageWindow = false;
        if (!meleeKeyDown && meleeStance < MELEE_STANCE_MAX) {
            meleeStance = Math.min(MELEE_STANCE_MAX, meleeStance + MELEE_STANCE_RECOVER_PER_FRAME);
        }

        if (meleeKeyDown && meleeSwingTimer <= 0 && meleeStance >= MELEE_STANCE_MIN_ATTACK) {
            startMeleeAttack();
        }
    }

    private void endSustainedMelee() {
        if (sustainedMeleeActive) {
            double secondFrameEnd = MELEE_FRAME_WEIGHTS[0] + MELEE_FRAME_WEIGHTS[1];
            int timerAtThirdFrame = MELEE_SWING_DURATION - (int)Math.ceil(MELEE_SWING_DURATION * secondFrameEnd);
            meleeSwingTimer = Math.min(meleeSwingTimer, Math.max(0, timerAtThirdFrame));
        }
        sustainedMeleeActive = false;
        endMeleeAfterDamageWindow = false;
    }

    private void cancelMeleeAttack() {
        endSustainedMelee();
        meleeSwingTimer = 0;
        meleeHitEnemies.clear();
        meleeHitBossThisSwing = false;
    }

    private void startMeleeAttack() {
        if (screenState != ScreenState.PLAYING || currentMode != GameMode.SLAUGHTER
            || isPaused || playerHP <= 0 || meleeSwingTimer > 0 || meleeStance < MELEE_STANCE_MIN_ATTACK) {
            if (currentMode == GameMode.SLAUGHTER && meleeStance < MELEE_STANCE_MIN_ATTACK) {
                pickupMessage = "Stance recovering";
                pickupMessageTimer = 45;
            }
            return;
        }

        meleeStance = Math.max(0.0, meleeStance - MELEE_STANCE_TAP_COST);
        meleeSwingTimer = MELEE_SWING_DURATION;
        meleeSwingKills = 0;
        meleeHitEnemies.clear();
        meleeHitBossThisSwing = false;
        playSound(meleeAttackSound);
        pickupMessage = "Melee swing";
        pickupMessageTimer = 70;
    }

    private void applyMeleeDamageWindow() {
        if (meleeSwingTimer <= 0) {
            return;
        }

        int frameIndex = getCurrentMeleeWeaponFrameIndex();
        if (frameIndex != 1) {
            return;
        }

        int killsThisFrame = 0;
        for (Enemy enemy : enemies) {
            if (!enemy.alive) continue;
            if (meleeHitEnemies.contains(enemy)) continue;

            if (isInsideMeleeDamageArea(enemy)) {
                meleeHitEnemies.add(enemy);
                damageEnemy(enemy, Math.max(enemy.hp, 1), true);
                if (!enemy.alive) {
                    meleeSwingKills++;
                    killsThisFrame++;
                }
            }
        }
        if (boss.alive && !meleeHitBossThisSwing && isInsideMeleeDamageArea(boss.x, boss.y, 0.58)) {
            meleeHitBossThisSwing = true;
            damageBoss(8);
        }

        if (killsThisFrame > 0) {
            pickupMessage = "Melee cleave x" + meleeSwingKills;
            pickupMessageTimer = 70;
        }
    }

    private boolean isInsideMeleeDamageArea(Enemy enemy) {
        return isInsideMeleeDamageArea(enemy.x, enemy.y, enemy.type.collisionRadius);
    }

    private boolean isInsideMeleeDamageArea(double targetX, double targetY, double radius) {
        double dx = targetX - posX;
        double dy = targetY - posY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > MELEE_FRONT_RANGE + radius && dist > MELEE_BACK_RANGE + radius) {
            return false;
        }

        if (!hasLineOfSight(posX, posY, targetX, targetY)) {
            return false;
        }

        if (dist <= MELEE_BACK_RANGE + radius) {
            return true;
        }

        double facingDot = dist <= 0.001 ? 1.0 : (dx * dirX + dy * dirY) / dist;
        return facingDot >= MELEE_FRONT_DOT;
    }

    private void startReload() {
        if (weaponWheelActive || isReloading || currentAmmo >= maxAmmo || reserveAmmo <= 0) {
            return;
        }
        isReloading = true;
        reloadTimer = reloadDuration;
        playSound(reloadSound);
        pickupMessage = "Reloading...";
        pickupMessageTimer = 90;
    }

    private void tryFireWeapon() {
        if (screenState != ScreenState.PLAYING || isPaused || weaponWheelActive || playerHP <= 0 || isReloading) {
            return;
        }
        if (fireCooldownTimer > 0) {
            return;
        }
        WeaponTemplate weapon = currentWeaponTemplate();
        if (weapon == null) {
            return;
        }

        if (currentAmmo <= 0) {
            if (reserveAmmo > 0) {
                startReload();
            } else {
                pickupMessage = "Out of ammo";
                pickupMessageTimer = 90;
            }
            fireCooldownTimer = 8;
            return;
        }

        currentAmmo--;
        syncAmmoToCurrentWeapon();
        isShootingFrame = 4;
        fireCooldownTimer = weapon.fireCooldown;
        playSound(gunFireSound);

        if (weapon.attackType == WeaponAttackType.FLAME) {
            fireFlamer();
        } else {
            Enemy target = findShotEnemy();
            double enemyDistance = target != null ? forwardDistanceTo(target.x, target.y) : Double.MAX_VALUE;
            double bossDistance = findShotBossDistance();
            if (bossDistance < enemyDistance) {
                damageBoss(weapon.damage);
            } else if (target != null) {
                damageEnemy(target, weapon.damage, false);
            }
        }
    }

    private void fireFlamer() {
        flameEffectTimer = 8;
        flameEffectSeed = frameCounter;
        double planeLen = Math.max(0.001, Math.sqrt(planeX * planeX + planeY * planeY));
        double rightX = planeX / planeLen;
        double rightY = planeY / planeLen;
        double muzzleX = posX + dirX * 0.34 + rightX * 0.08;
        double muzzleY = posY + dirY * 0.34 + rightY * 0.08;

        int spawnCount = 18 + flameParticleRandom.nextInt(8);
        for (int i = 0; i < spawnCount; i++) {
            double spread = (flameParticleRandom.nextDouble() - 0.5) * 0.42;
            double forwardX = dirX + rightX * spread;
            double forwardY = dirY + rightY * spread;
            double forwardLen = Math.max(0.001, Math.sqrt(forwardX * forwardX + forwardY * forwardY));
            forwardX /= forwardLen;
            forwardY /= forwardLen;

            double speed = 0.24 + flameParticleRandom.nextDouble() * 0.10;
            double sideSpeed = (flameParticleRandom.nextDouble() - 0.5) * 0.034;
            double originForward = flameParticleRandom.nextDouble() * 0.16;
            double originSide = (flameParticleRandom.nextDouble() - 0.5) * 0.14;
            boolean smoke = flameParticleRandom.nextDouble() < 0.10;
            flameParticles.add(new FlameParticle(
                muzzleX + dirX * originForward + rightX * originSide,
                muzzleY + dirY * originForward + rightY * originSide,
                forwardX * speed + rightX * sideSpeed,
                forwardY * speed + rightY * sideSpeed,
                smoke ? 0.075 : 0.042 + flameParticleRandom.nextDouble() * 0.032,
                smoke ? 0.54 + flameParticleRandom.nextDouble() * 0.22
                      : 0.48 + flameParticleRandom.nextDouble() * 0.20,
                (flameParticleRandom.nextDouble() - 0.5) * 24.0,
                smoke ? 38 + flameParticleRandom.nextInt(16) : 30 + flameParticleRandom.nextInt(12),
                flameParticleRandom.nextInt(1000),
                smoke
            ));
        }

        while (flameParticles.size() > MAX_FLAME_PARTICLES) {
            flameParticles.remove(0);
        }
        applyMuzzleFlameDamage();
    }

    private void applyMuzzleFlameDamage() {
        int hits = 0;
        if (boss.alive && frameCounter - boss.lastFlameDamageFrame >= FLAME_DAMAGE_INTERVAL
                && isBossInsideFlame(posX, posY, FLAME_CLOSE_DAMAGE_RANGE + 0.65)) {
            boss.lastFlameDamageFrame = frameCounter;
            damageBoss(1);
            hits++;
        }
        for (Enemy enemy : enemies) {
            if (!enemy.alive || frameCounter - enemy.lastFlameDamageFrame < FLAME_DAMAGE_INTERVAL) {
                continue;
            }
            double dx = enemy.x - posX;
            double dy = enemy.y - posY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > FLAME_CLOSE_DAMAGE_RANGE + enemy.type.collisionRadius) {
                continue;
            }
            double facingDot = dist <= 0.001 ? 1.0 : (dx * dirX + dy * dirY) / dist;
            if (facingDot < -0.25 || !hasLineOfSight(posX, posY, enemy.x, enemy.y)) {
                continue;
            }
            applyFlameHit(enemy);
            hits++;
        }
        if (hits > 0) {
            pickupMessage = "Burn x" + hits;
            pickupMessageTimer = 24;
        }
    }

    private boolean isBossInsideFlame(double originX, double originY, double range) {
        double dx = boss.x - originX;
        double dy = boss.y - originY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > range) {
            return false;
        }
        double facingDot = dist <= 0.001 ? 1.0 : (dx * dirX + dy * dirY) / dist;
        return facingDot >= -0.25 && hasLineOfSight(originX, originY, boss.x, boss.y);
    }

    private void updateFlameParticles() {
        for (int i = flameParticles.size() - 1; i >= 0; i--) {
            FlameParticle particle = flameParticles.get(i);
            particle.age++;
            if (!isWalkable(particle.x, particle.y)) {
                flameParticles.remove(i);
                continue;
            }
            if (particle.canDamage()) {
                applyFlameParticleDamage(particle);
            }

            double stepDistance = Math.sqrt(particle.velX * particle.velX + particle.velY * particle.velY);
            particle.x += particle.velX;
            particle.y += particle.velY;
            particle.travelDistance += stepDistance;
            particle.velX *= 0.992;
            particle.velY *= 0.992;

            if (particle.age >= particle.life
                    || particle.travelDistance > FLAME_VISUAL_RANGE
                    || !isWalkable(particle.x, particle.y)) {
                flameParticles.remove(i);
            }
        }
    }

    private void applyFlameParticleDamage(FlameParticle particle) {
        double radius = particle.damageRadius();
        double radiusSq = radius * radius;
        int hits = 0;
        if (boss.alive && frameCounter - boss.lastFlameDamageFrame >= FLAME_DAMAGE_INTERVAL) {
            double bossDx = boss.x - particle.x;
            double bossDy = boss.y - particle.y;
            double bossHitRadius = radius + 0.55;
            if (bossDx * bossDx + bossDy * bossDy <= bossHitRadius * bossHitRadius
                    && hasLineOfSight(particle.x, particle.y, boss.x, boss.y)) {
                boss.lastFlameDamageFrame = frameCounter;
                damageBoss(1);
                hits++;
            }
        }
        for (Enemy enemy : enemies) {
            if (!enemy.alive || frameCounter - enemy.lastFlameDamageFrame < FLAME_DAMAGE_INTERVAL) {
                continue;
            }

            double dx = enemy.x - particle.x;
            double dy = enemy.y - particle.y;
            double hitRadius = radius + enemy.type.collisionRadius * 0.75;
            if (dx * dx + dy * dy > Math.max(radiusSq, hitRadius * hitRadius)) {
                continue;
            }
            if (!hasLineOfSight(particle.x, particle.y, enemy.x, enemy.y)) {
                continue;
            }

            applyFlameHit(enemy);
            hits++;
        }
        if (hits > 0) {
            pickupMessage = "Burn x" + hits;
            pickupMessageTimer = 24;
        }
    }

    private void applyFlameHit(Enemy enemy) {
        enemy.lastFlameDamageFrame = frameCounter;
        enemy.flameHitsTaken++;
        boolean willKill = enemy.flameHitsTaken >= FLAME_HITS_TO_KILL;
        wakeEnemyOnHit(enemy);
        playEnemyHitSound(enemy);
        triggerHitMarker(willKill);
        hitEffectTimer = 12;
        hitEffectX = enemy.x;
        hitEffectY = enemy.y;
        hitEffectIsMelee = false;
        if (willKill) {
            killEnemy(enemy, false);
        }
    }

    private void spawnBossExplosion(double x, double y) {
        for (int i = 0; i < 120; i++) {
            double angle = explosionRandom.nextDouble() * Math.PI * 2.0;
            double speed = 0.035 + explosionRandom.nextDouble() * 0.18;
            double height = 0.08 + explosionRandom.nextDouble() * 1.0;
            double velHeight = 0.015 + explosionRandom.nextDouble() * 0.075;
            int life = 42 + explosionRandom.nextInt(40);
            int size = 7 + explosionRandom.nextInt(20);
            Color color;
            double roll = explosionRandom.nextDouble();
            if (roll < 0.18) {
                color = new Color(255, 250, 210);
            } else if (roll < 0.74) {
                color = new Color(255, 92 + explosionRandom.nextInt(90), 20);
            } else {
                int v = 42 + explosionRandom.nextInt(58);
                color = new Color(v + 30, v, v - 10);
            }
            explosionParticles.add(new ExplosionParticle(
                x + (explosionRandom.nextDouble() - 0.5) * 0.55,
                y + (explosionRandom.nextDouble() - 0.5) * 0.55,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed,
                height,
                velHeight,
                life,
                size,
                color
            ));
        }
    }

    private void updateExplosionParticles() {
        for (int i = explosionParticles.size() - 1; i >= 0; i--) {
            ExplosionParticle particle = explosionParticles.get(i);
            particle.age++;
            particle.x += particle.velX;
            particle.y += particle.velY;
            particle.height += particle.velHeight;
            particle.velX *= 0.965;
            particle.velY *= 0.965;
            particle.velHeight -= 0.0035;
            if (particle.age >= particle.life || particle.height < -0.1) {
                explosionParticles.remove(i);
            }
        }
        while (explosionParticles.size() > 220) {
            explosionParticles.remove(0);
        }
    }

    private void maybeDropSupplies(Enemy enemy, boolean meleeKill) {
        if (currentMode != GameMode.SLAUGHTER) {
            return;
        }

        double ammoChance = meleeKill ? 0.42 : 0.14;
        double medkitChance = meleeKill ? 0.18 : 0.05;
        Point dropTile = findDropTileNear(enemy.x, enemy.y);
        if (dropTile == null) {
            return;
        }

        if (Math.random() < ammoChance) {
            dropAmmoBoxAt(dropTile.x, dropTile.y);
        }
        if (Math.random() < medkitChance) {
            Point medkitTile = findDropTileNear(enemy.x + 0.35, enemy.y - 0.35);
            if (medkitTile != null) {
                dropMedkitAt(medkitTile.x, medkitTile.y);
            }
        }
    }

    private Point findDropTileNear(double worldX, double worldY) {
        int baseX = (int)worldX;
        int baseY = (int)worldY;
        for (int radius = 0; radius <= 2; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int x = baseX + dx;
                    int y = baseY + dy;
                    if (isValidDropTile(x, y)) {
                        return new Point(x, y);
                    }
                }
            }
        }
        return null;
    }

    private void dropAmmoBoxAt(int x, int y) {
        if (!isValidDropTile(x, y) || isAmmoBoxAt(x, y)) {
            return;
        }

        for (AmmoBox ammoBox : ammoBoxes) {
            if (!ammoBox.active) {
                ammoBox.x = x;
                ammoBox.y = y;
                ammoBox.active = true;
                ammoBox.respawnTimer = 0;
                return;
            }
        }

        if (ammoBoxes.size() < maxDroppedAmmoBoxes) {
            ammoBoxes.add(new AmmoBox(x, y));
        }
    }

    private void dropMedkitAt(int x, int y) {
        if (!isValidDropTile(x, y) || isMedkitAt(x, y) || medkits.size() >= maxDroppedMedkits) {
            return;
        }
        medkits.add(new Medkit(x, y, false));
    }

    // =========================
    // 补给掉落、暂停与通用游戏辅助
    // =========================
    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            firing = false;
            weaponWheelActive = false;
            weaponWheelHoverIndex = -1;
            meleeKeyDown = false;
            cancelMeleeAttack();
            meleeHoldFrames = 0;
            // 进入暂停时，释放鼠标锁定
            if (grabMouse) {
                setCursor(Cursor.getDefaultCursor());
            }
        } else {
            // 退出暂停时，恢复鼠标锁定
            if (grabMouse) {
                setCursor(hiddenCursor);
            }
        }
    }

    private double distanceToPlayerSquared(Enemy enemy) {
        double dx = enemy.x - posX;
        double dy = enemy.y - posY;
        return dx * dx + dy * dy;
    }

    private double forwardDistanceTo(double worldX, double worldY) {
        double relX = worldX - posX;
        double relY = worldY - posY;
        return relX * dirX + relY * dirY;
    }

    private double findShotBossDistance() {
        if (!boss.alive) {
            return Double.MAX_VALUE;
        }
        double relX = boss.x - posX;
        double relY = boss.y - posY;
        double invDet = 1.0 / (planeX * dirY - dirX * planeY);
        double transformX = invDet * (dirY * relX - dirX * relY);
        double transformY = invDet * (-planeY * relX + planeX * relY);
        if (transformY <= 0.1) {
            return Double.MAX_VALUE;
        }

        int screenX = (int)((WIDTH / 2) * (1 + transformX / transformY));
        int spriteWidth = (int)(Math.abs((int)(HEIGHT / transformY)) * bossVisualScale());
        int hitboxWidth = (int)(spriteWidth * 0.72);
        if (WIDTH / 2 >= screenX - hitboxWidth / 2 && WIDTH / 2 <= screenX + hitboxWidth / 2
            && hasVisibleHitStripe(transformY, screenX, hitboxWidth)) {
            return transformY;
        }
        return Double.MAX_VALUE;
    }

    private double bossVisualScale() {
        return boss.phase == 2 ? 1.35 : 1.18;
    }

    // =========================
    // 3D 场景对象渲染：敌人、Boss、激光、刀光
    // =========================
    private void drawEnemy(Graphics2D g2d, Enemy enemy) {
        if (!enemy.alive) return;

        double spriteX_rel = enemy.x - posX;
        double spriteY_rel = enemy.y - posY;
        double invDet = 1.0 / (planeX * dirY - dirX * planeY);
        double transformX = invDet * (dirY * spriteX_rel - dirX * spriteY_rel);
        double transformY = invDet * (-planeY * spriteX_rel + planeX * spriteY_rel);

        if (transformY <= 0.1) return;

        int spriteScreenX = (int) ((WIDTH / 2) * (1 + transformX / transformY));
        int baseSize = Math.abs((int)(HEIGHT / transformY));
        int spriteHeight = (int)(baseSize * enemy.type.sizeScale);
        int spriteWidth = (int)(baseSize * enemy.type.sizeScale);

        int rawDrawStartX = -spriteWidth / 2 + spriteScreenX;
        int drawStartX = rawDrawStartX;
        int drawEndX = spriteWidth / 2 + spriteScreenX;
        int drawStartY = -spriteHeight / 2 + HEIGHT / 2 + (baseSize - spriteHeight) / 2;
        int drawEndY = spriteHeight / 2 + HEIGHT / 2 + (baseSize - spriteHeight) / 2;

        if (drawStartX < 0) drawStartX = 0;
        if (drawEndX >= WIDTH) drawEndX = WIDTH - 1;
        if (drawStartX >= drawEndX) return;

        BufferedImage texture = enemy.type.texture;
        if (texture != null) {
            int textureWidth = texture.getWidth();
            int textureHeight = texture.getHeight();
            for (int stripe = drawStartX; stripe < drawEndX; stripe++) {
                if (transformY < zBuffer[stripe]) {
                    int textureX = (int)((stripe - rawDrawStartX) * textureWidth / (double)Math.max(1, spriteWidth));
                    if (textureX < 0) textureX = 0;
                    if (textureX >= textureWidth) textureX = textureWidth - 1;
                    g2d.drawImage(
                        texture,
                        stripe, drawStartY, stripe + 1, drawEndY,
                        textureX, 0, textureX + 1, textureHeight,
                        null
                    );
                }
            }
            return;
        }

        for (int stripe = drawStartX; stripe < drawEndX; stripe++) {
            if (transformY < zBuffer[stripe]) {
                g2d.setColor(enemy.type.fallbackColor);
                g2d.drawLine(stripe, drawStartY, stripe, drawEndY);

                if (stripe > spriteScreenX - spriteWidth / 12 && stripe < spriteScreenX + spriteWidth / 12) {
                    g2d.setColor(enemy.type.attackMode == EnemyAttackMode.RANGED ? Color.CYAN : Color.YELLOW);
                    g2d.fillRect(stripe, drawStartY + spriteHeight / 4, 1, spriteHeight / 10);
                }
            }
        }
    }

    private void drawBoss(Graphics2D g2d) {
        if (!boss.alive) return;

        double relX = boss.x - posX;
        double relY = boss.y - posY;
        double invDet = 1.0 / (planeX * dirY - dirX * planeY);
        double transformX = invDet * (dirY * relX - dirX * relY);
        double transformY = invDet * (-planeY * relX + planeX * relY);
        if (transformY <= 0.1) return;

        int screenX = (int)((WIDTH / 2) * (1 + transformX / transformY));
        int baseSize = Math.abs((int)(HEIGHT / transformY));
        int width = (int)(baseSize * bossVisualScale());
        int height = (int)(width * 1.08);
        int startX = screenX - width / 2;
        int endX = screenX + width / 2;
        int startY = HEIGHT / 2 - height / 2 + (int)(baseSize * 0.10);
        int endY = startY + height;
        int drawStartX = Math.max(0, startX);
        int drawEndX = Math.min(WIDTH - 1, endX);

        BufferedImage bossTexture = getCurrentBossTexture();
        if (bossTexture != null) {
            drawBossTexture(g2d, bossTexture, transformY, startX, startY, width, height, drawStartX, drawEndX);
            return;
        }

        Color body = boss.phase == 2 ? new Color(140, 28, 42) : new Color(92, 42, 118);
        Color core = boss.phase == 2 ? new Color(255, 92, 34) : new Color(180, 80, 230);
        if (boss.action == BossAction.DASH_CHARGE || boss.action == BossAction.LASER_CHARGE
                || boss.action == BossAction.BLADE_CHARGE) {
            core = new Color(255, 220, 90);
        }

        for (int x = drawStartX; x <= drawEndX; x++) {
            if (transformY >= zBuffer[x]) continue;
            double nx = (x - screenX) / (double)Math.max(1, width / 2);
            double curve = Math.sqrt(Math.max(0.0, 1.0 - nx * nx));
            int columnTop = startY + (int)((1.0 - curve) * height * 0.26);
            int columnBottom = endY - (int)((1.0 - curve) * height * 0.18);
            g2d.setColor(body);
            g2d.drawLine(x, columnTop, x, columnBottom);

            if (Math.abs(nx) < 0.28) {
                g2d.setColor(core);
                int coreTop = startY + height / 3;
                int coreBottom = startY + height * 2 / 3;
                g2d.drawLine(x, coreTop, x, coreBottom);
            }
            if ((nx > -0.58 && nx < -0.38) || (nx > 0.38 && nx < 0.58)) {
                g2d.setColor(new Color(255, 235, 140));
                g2d.drawLine(x, startY + height / 3, x, startY + height / 3 + Math.max(2, height / 14));
            }
        }

        if (boss.action == BossAction.DASH_CHARGE || boss.action == BossAction.BLADE_CHARGE) {
            g2d.setColor(new Color(255, 210, 60, 170));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(screenX - width / 2, startY, width, height);
        }
    }

    private BufferedImage getCurrentBossTexture() {
        if (!boss.alive) {
            return null;
        }
        if (boss.action == BossAction.DASH_CHARGE) {
            return bossDashChargeTexture;
        }
        if (boss.action == BossAction.DASHING) {
            return bossDashTexture;
        }
        if (boss.action == BossAction.BLADE_CHARGE) {
            return bossBladeChargeTexture != null ? bossBladeChargeTexture : bossDashChargeTexture;
        }
        if (boss.action == BossAction.BLADE_ATTACK) {
            BufferedImage bladeTexture = getBossBladeAttackTexture();
            return bladeTexture != null ? bladeTexture : bossDashTexture;
        }
        if (boss.action == BossAction.LASER_CHARGE || boss.action == BossAction.LASER_SWEEP) {
            return getBossLaserTextureForDirection();
        }
        return getAnimationFrame(bossIdleTextures, 8);
    }

    private BufferedImage getBossBladeAttackTexture() {
        if (bossBladeTextures == null || bossBladeTextures.length == 0) {
            return null;
        }
        int[] sequence = {0, 1, 2, 1};
        int sequenceIndex = (frameCounter / 5) % sequence.length;
        for (int i = 0; i < sequence.length; i++) {
            int index = sequence[(sequenceIndex + i) % sequence.length];
            if (index >= 0 && index < bossBladeTextures.length && bossBladeTextures[index] != null) {
                return bossBladeTextures[index];
            }
        }
        return null;
    }

    private BufferedImage getBossLaserTextureForDirection() {
        if (bossLaserTextures == null || bossLaserTextures.length < 3) {
            return getAnimationFrame(bossLaserTextures, 5);
        }
        if (boss.action == BossAction.LASER_CHARGE) {
            return firstAvailableFrame(1, 0, 2);
        }

        double diff = normalizeAngle(boss.laserAngle - boss.laserStartAngle);
        double sideThreshold = Math.toRadians(28.0);
        if (diff < -sideThreshold) {
            return firstAvailableFrame(0, 1, 2);
        }
        if (diff > sideThreshold) {
            return firstAvailableFrame(2, 1, 0);
        }
        return firstAvailableFrame(1, 0, 2);
    }

    private BufferedImage firstAvailableFrame(int... indices) {
        for (int index : indices) {
            if (index >= 0 && bossLaserTextures != null && index < bossLaserTextures.length
                    && bossLaserTextures[index] != null) {
                return bossLaserTextures[index];
            }
        }
        return null;
    }

    private BufferedImage getAnimationFrame(BufferedImage[] frames, int frameDuration) {
        if (frames == null || frames.length == 0) {
            return null;
        }
        int available = 0;
        for (BufferedImage frame : frames) {
            if (frame != null) {
                available++;
            }
        }
        if (available == 0) {
            return null;
        }
        int desired = (frameCounter / Math.max(1, frameDuration)) % frames.length;
        for (int i = 0; i < frames.length; i++) {
            int index = (desired + i) % frames.length;
            if (frames[index] != null) {
                return frames[index];
            }
        }
        return null;
    }

    private void drawBossTexture(Graphics2D g2d, BufferedImage texture, double transformY,
                                 int startX, int startY, int width, int height,
                                 int drawStartX, int drawEndX) {
        int textureWidth = texture.getWidth();
        int textureHeight = texture.getHeight();
        for (int stripe = drawStartX; stripe <= drawEndX; stripe++) {
            if (transformY >= zBuffer[stripe]) {
                continue;
            }
            int textureX = (int)((stripe - startX) * textureWidth / (double)Math.max(1, width));
            textureX = clamp(textureX, 0, textureWidth - 1);
            g2d.drawImage(
                texture,
                stripe, startY, stripe + 1, startY + height,
                textureX, 0, textureX + 1, textureHeight,
                null
            );
            if (boss.phase == 2) {
                int pulse = (int)(Math.sin(frameCounter * 0.18) * 18);
                g2d.setColor(new Color(255, 24, 18, 72 + pulse));
                g2d.drawLine(stripe, startY, stripe, startY + height);
                if ((stripe + frameCounter) % 6 == 0) {
                    g2d.setColor(new Color(255, 118, 70, 88));
                    g2d.drawLine(stripe, startY + height / 6, stripe, startY + height * 5 / 6);
                }
            }
        }
    }

    private void drawBossLaser(Graphics2D g2d) {
        if (!boss.alive || (boss.action != BossAction.LASER_CHARGE && boss.action != BossAction.LASER_SWEEP)) {
            return;
        }
        LaserBeam beam = bossLaserBeam();
        if (beam == null) {
            return;
        }

        Stroke oldStroke = g2d.getStroke();
        Color oldColor = g2d.getColor();
        boolean sweeping = boss.action == BossAction.LASER_SWEEP;
        double beamRadius = sweeping ? (boss.phase == 2 ? 0.55 : 0.42) : 0.18;
        double step = 0.22;
        for (double d = beam.length; d >= 0.0; d -= step) {
            double cx = beam.startX + beam.dirX * d;
            double cy = beam.startY + beam.dirY * d;
            drawLaserSlice(g2d, cx, cy, beamRadius, d / Math.max(0.001, beam.length), sweeping);
        }
        Point start = projectWorldToScreen(beam.startX, beam.startY, 0.0);
        Point end = projectWorldToScreen(beam.endX, beam.endY, 0.0);
        if (start != null && end != null) {
            g2d.setStroke(new BasicStroke(sweeping ? 4.0f : 2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(sweeping ? new Color(255, 250, 220, 190) : new Color(255, 235, 120, 150));
            g2d.drawLine(start.x, start.y, end.x, end.y);
        }
        g2d.setStroke(oldStroke);
        g2d.setColor(oldColor);
    }

    private void drawLaserSlice(Graphics2D g2d, double worldX, double worldY, double radius, double t, boolean sweeping) {
        double relX = worldX - posX;
        double relY = worldY - posY;
        double invDet = 1.0 / (planeX * dirY - dirX * planeY);
        double transformX = invDet * (dirY * relX - dirX * relY);
        double transformY = invDet * (-planeY * relX + planeX * relY);
        if (transformY <= 0.08) {
            return;
        }
        int screenX = (int)((WIDTH / 2) * (1 + transformX / transformY));
        if (screenX < -200 || screenX >= WIDTH + 200) {
            return;
        }
        int depthX = clamp(screenX, 0, WIDTH - 1);
        if (transformY >= zBuffer[depthX]) {
            return;
        }
        int screenY = HEIGHT / 2;
        int size = (int)Math.round(radius * HEIGHT / transformY);
        size = clamp(size, sweeping ? 12 : 5, sweeping ? 210 : 76);
        int pulse = (int)(Math.sin(frameCounter * 0.45 + t * 10.0) * size * 0.08);
        int glow = Math.max(size + pulse, size);
        int alpha = sweeping ? 120 : 72;
        Color glowColor = sweeping ? new Color(255, 45, 65, alpha) : new Color(255, 225, 85, alpha);
        g2d.setColor(glowColor);
        g2d.fillRect(screenX - glow, screenY - glow / 2, glow * 2, glow);
        if (sweeping) {
            int core = Math.max(4, size / 3);
            g2d.setColor(new Color(255, 245, 220, 225));
            g2d.fillRect(screenX - core, screenY - core / 2, core * 2, core);
        }
        if (frameCounter % 2 == 0 || sweeping) {
            int spark = Math.max(3, size / 5);
            int offset = (int)Math.round(Math.sin(t * 24.0 + frameCounter * 0.25) * size * 0.45);
            g2d.setColor(sweeping ? new Color(255, 170, 55, 150) : new Color(255, 245, 160, 110));
            g2d.fillRect(screenX + offset - spark / 2, screenY - size / 2, spark, spark);
        }
    }

    private Point projectWorldToScreen(double worldX, double worldY) {
        return projectWorldToScreen(worldX, worldY, 0.0);
    }

    private Point projectWorldToScreen(double worldX, double worldY, double heightOffset) {
        double relX = worldX - posX;
        double relY = worldY - posY;
        double invDet = 1.0 / (planeX * dirY - dirX * planeY);
        double transformX = invDet * (dirY * relX - dirX * relY);
        double transformY = invDet * (-planeY * relX + planeX * relY);
        if (transformY <= 0.08) {
            return null;
        }
        int screenX = (int)((WIDTH / 2) * (1 + transformX / transformY));
        int screenY = HEIGHT / 2 - (int)Math.round(heightOffset * HEIGHT / transformY);
        return new Point(screenX, screenY);
    }

    private void drawBossHealthBar(Graphics2D g2d) {
        if (!boss.spawned || !boss.alive) {
            return;
        }
        int barW = 430;
        int barH = 16;
        int x = WIDTH / 2 - barW / 2;
        int y = 24;
        double ratio = boss.hp / (double)Math.max(1, boss.maxHp);
        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.fillRoundRect(x - 8, y - 20, barW + 16, 48, 8, 8);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 15));
        g2d.setColor(new Color(255, 230, 150));
        String label = boss.phase == 2 ? "BOSS PHASE 2" : "BOSS PHASE 1";
        g2d.drawString(label, x, y - 5);
        g2d.setColor(new Color(45, 20, 28));
        g2d.fillRect(x, y, barW, barH);
        g2d.setColor(boss.phase == 2 ? new Color(255, 64, 44) : new Color(170, 80, 230));
        g2d.fillRect(x, y, (int)Math.round(barW * ratio), barH);
        g2d.setColor(new Color(255, 255, 255, 210));
        g2d.drawRect(x, y, barW, barH);
    }

    private void drawEnemyProjectiles(Graphics2D g2d) {
        for (EnemyProjectile projectile : enemyProjectiles) {
            double relX = projectile.x - posX;
            double relY = projectile.y - posY;
            double invDet = 1.0 / (planeX * dirY - dirX * planeY);
            double transformX = invDet * (dirY * relX - dirX * relY);
            double transformY = invDet * (-planeY * relX + planeX * relY);

            if (transformY <= 0.1) continue;
            int screenX = (int)((WIDTH / 2) * (1 + transformX / transformY));
            if (screenX < 0 || screenX >= WIDTH || transformY >= zBuffer[screenX]) continue;

            Color projectileColor = projectile.color != null ? projectile.color : new Color(80, 220, 255);
            int size = (int)Math.max(8, projectile.baseSize / transformY);
            int screenY = HEIGHT / 2;
            g2d.setColor(new Color(projectileColor.getRed(), projectileColor.getGreen(), projectileColor.getBlue(), 90));
            g2d.fillOval(screenX - size, screenY - size, size * 2, size * 2);
            g2d.setColor(new Color(
                Math.min(255, projectileColor.getRed() + 100),
                Math.min(255, projectileColor.getGreen() + 35),
                Math.min(255, projectileColor.getBlue() + 35),
                230
            ));
            g2d.fillOval(screenX - size / 2, screenY - size / 2, size, size);
        }
    }

    private void drawBossBladeWaves(Graphics2D g2d) {
        if (bossBladeWaves.isEmpty()) {
            return;
        }

        Graphics2D bladeG = (Graphics2D)g2d.create();
        bladeG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        List<BossBladeWave> drawWaves = new ArrayList<>(bossBladeWaves);
        drawWaves.sort((a, b) -> Double.compare(
            (b.x - posX) * (b.x - posX) + (b.y - posY) * (b.y - posY),
            (a.x - posX) * (a.x - posX) + (a.y - posY) * (a.y - posY)
        ));

        for (BossBladeWave wave : drawWaves) {
            drawBossBladeWave(bladeG, wave);
        }
        bladeG.dispose();
    }

    private void drawBossBladeWave(Graphics2D g2d, BossBladeWave wave) {
        double progress = wave.progress();
        double pulse = Math.sin((frameCounter + wave.colorSeed) * 0.35) * 0.035;
        double heightScale = 1.0 + progress * 0.18 + pulse;
        double thicknessScale = 1.0 + Math.sin((frameCounter + wave.colorSeed) * 0.22) * 0.08;

        for (int i = 0; i <= 14; i++) {
            double t = i / 14.0;
            double lifted = Math.sin(t * Math.PI);
            double outerForward = -0.34 + t * 0.68;
            double outerBack = (0.16 + lifted * 0.36) * thicknessScale;
            double outerHeight = (0.06 + lifted * 1.34) * heightScale;
            drawBladeWaveParticle(g2d, wave, outerForward, -outerBack, outerHeight, t, 0);
            drawBladeWaveParticle(g2d, wave, outerForward, outerBack, outerHeight * 0.98, t, 0);

            if (i > 1 && i < 13 && i % 2 == 0) {
                double innerForward = -0.18 + t * 0.36;
                double innerBack = (0.06 + lifted * 0.18) * thicknessScale;
                double innerHeight = (0.05 + lifted * 0.94) * heightScale;
                drawBladeWaveParticle(g2d, wave, innerForward, -innerBack, innerHeight, t, 1);
                drawBladeWaveParticle(g2d, wave, innerForward, innerBack, innerHeight * 0.96, t, 1);
            }

            if (i == 0 || i == 14) {
                drawBladeWaveParticle(g2d, wave, outerForward, 0.0, 0.03, t, 2);
            }
        }
    }

    private void drawBladeWaveParticle(Graphics2D g2d, BossBladeWave wave, double along, double back,
                                       double height, double arcT, int layer) {
        double worldX = wave.x + wave.dirX * along - wave.dirX * back;
        double worldY = wave.y + wave.dirY * along - wave.dirY * back;
        double relX = worldX - posX;
        double relY = worldY - posY;
        double invDet = 1.0 / (planeX * dirY - dirX * planeY);
        double transformX = invDet * (dirY * relX - dirX * relY);
        double transformY = invDet * (-planeY * relX + planeX * relY);
        if (transformY <= 0.08) {
            return;
        }
        int screenX = (int)((WIDTH / 2) * (1 + transformX / transformY));
        if (screenX < -140 || screenX >= WIDTH + 140) {
            return;
        }
        int depthX = clamp(screenX, 0, WIDTH - 1);
        if (transformY >= zBuffer[depthX]) {
            return;
        }

        int screenY = HEIGHT / 2 - (int)Math.round(height * HEIGHT / transformY);
        int baseSize = layer == 0 ? 15 : (layer == 1 ? 10 : 13);
        int size = (int)Math.round((baseSize + wave.progress() * 7.0) / Math.max(0.35, transformY));
        size = clamp(size, layer == 0 ? 5 : 4, layer == 0 ? 62 : 40);
        double edge = Math.abs(arcT - 0.5) * 2.0;
        int alpha = clamp((int)Math.round((layer == 0 ? 235 : 155) * (1.0 - wave.progress() * 0.25)), 45, 240);

        g2d.setColor(new Color(255, 35, 45, alpha / 3));
        g2d.fillRect(screenX - size, screenY - size, size * 2, size * 2);
        g2d.setColor(layer == 0
            ? new Color(255, 236 - (int)(edge * 76), 130 - (int)(edge * 58), alpha)
            : new Color(210, 50, 255, alpha));
        int drawW = layer == 2 ? Math.max(3, size / 2) : size;
        int drawH = layer == 0 ? Math.max(4, (int)(size * 1.25)) : size;
        g2d.fillRect(screenX - drawW / 2, screenY - drawH / 2, drawW, drawH);
        if (layer == 0 && edge < 0.78) {
            int core = Math.max(3, size / 3);
            g2d.setColor(new Color(255, 250, 230, clamp(alpha + 20, 0, 255)));
            g2d.fillRect(screenX - core / 2, screenY - core / 2, core, core);
        }
    }

    private void drawEnemyIndicator(Graphics2D g2d, Enemy enemy) {
        if (!enemy.alive) return;

        double dx = enemy.x - posX;
        double dy = enemy.y - posY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double radarRange = currentMode == GameMode.SLAUGHTER ? 22.0 : RADAR_RANGE;
        if (distance <= 0.05 || distance > radarRange) {
            return;
        }

        int padding = 30;
        int usableHalfWidth = WIDTH / 2 - padding - 18;
        double rightX = -dirY;
        double rightY = dirX;
        double forwardAmount = (dx * dirX + dy * dirY) / distance;
        double rightAmount = (dx * rightX + dy * rightY) / distance;
        int arrowX = WIDTH / 2 + (int)(rightAmount * usableHalfWidth);
        int arrowY = forwardAmount >= 0 ? padding + 8 : HEIGHT - 88;
        int arrowSize = (int)(8 + (radarRange - distance) / radarRange * 14);
        int[] xPoints;
        int[] yPoints;

        if (forwardAmount >= 0) {
            xPoints = new int[] {arrowX, arrowX - arrowSize, arrowX + arrowSize};
            yPoints = new int[] {arrowY + arrowSize, arrowY - arrowSize, arrowY - arrowSize};
        } else {
            xPoints = new int[] {arrowX, arrowX - arrowSize, arrowX + arrowSize};
            yPoints = new int[] {arrowY - arrowSize, arrowY + arrowSize, arrowY + arrowSize};
        }

        Color color = enemy.type.attackMode == EnemyAttackMode.RANGED ? new Color(80, 220, 255, 210) : new Color(255, 50, 50, 210);
        if (!enemy.alerted && currentMode == GameMode.SLAUGHTER) {
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 120);
        }
        g2d.setColor(color);
        g2d.fillPolygon(xPoints, yPoints, 3);
        g2d.setColor(new Color(255, 255, 255, 160));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawPolygon(xPoints, yPoints, 3);
    }

    // =========================
    // HUD、小地图、菜单与整体画面绘制
    // =========================
    private void drawMinimap(Graphics2D g2d) {
        int size = 172;
        int centerX = WIDTH - size / 2 - 18;
        int centerY = 22 + size / 2;
        double range = currentMode == GameMode.SLAUGHTER ? 15.0 : 8.0;
        double scale = (size * 0.46) / range;

        Composite oldComposite = g2d.getComposite();
        Shape oldClip = g2d.getClip();
        Stroke oldStroke = g2d.getStroke();

        g2d.setComposite(AlphaComposite.SrcOver.derive(0.78f));
        g2d.setColor(new Color(5, 8, 12, 210));
        g2d.fillOval(centerX - size / 2, centerY - size / 2, size, size);
        g2d.setComposite(oldComposite);

        g2d.setClip(new java.awt.geom.Ellipse2D.Double(centerX - size / 2, centerY - size / 2, size, size));

        int minX = Math.max(0, (int)Math.floor(posX - range - 2));
        int maxX = Math.min(map.length - 1, (int)Math.ceil(posX + range + 2));
        int minY = Math.max(0, (int)Math.floor(posY - range - 2));
        int maxY = Math.min(map[0].length - 1, (int)Math.ceil(posY + range + 2));

        g2d.setColor(new Color(58, 66, 76, 220));
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (map[x][y] != 0) {
                    drawMinimapCell(g2d, x, y, centerX, centerY, scale);
                }
            }
        }

        for (AmmoBox ammoBox : ammoBoxes) {
            if (ammoBox.active) {
                Point p = worldToMinimap(ammoBox.x + 0.5, ammoBox.y + 0.5, centerX, centerY, scale);
                if (isPointInsideMinimap(p, centerX, centerY, size)) {
                    g2d.setColor(new Color(90, 150, 255, 230));
                    g2d.fillRect(p.x - 3, p.y - 3, 6, 6);
                }
            }
        }

        for (Medkit medkit : medkits) {
            if (medkit.active) {
                Point p = worldToMinimap(medkit.x + 0.5, medkit.y + 0.5, centerX, centerY, scale);
                if (isPointInsideMinimap(p, centerX, centerY, size)) {
                    g2d.setColor(new Color(110, 255, 130, 230));
                    g2d.fillRect(p.x - 3, p.y - 3, 6, 6);
                }
            }
        }

        if (currentMode == GameMode.SLAUGHTER) {
            for (Enemy enemy : enemies) {
                if (!enemy.alive) continue;
                Point p = worldToMinimap(enemy.x, enemy.y, centerX, centerY, scale);
                if (isPointInsideMinimap(p, centerX, centerY, size)) {
                    Color dotColor = enemy.type.attackMode == EnemyAttackMode.RANGED
                        ? new Color(80, 220, 255, enemy.alerted ? 235 : 150)
                        : new Color(255, 70, 60, enemy.alerted ? 235 : 150);
                    g2d.setColor(dotColor);
                    g2d.fillOval(p.x - 4, p.y - 4, 8, 8);
                }
            }
            if (boss.alive) {
                Point p = worldToMinimap(boss.x, boss.y, centerX, centerY, scale);
                if (isPointInsideMinimap(p, centerX, centerY, size)) {
                    g2d.setColor(boss.phase == 2 ? new Color(255, 70, 35, 245) : new Color(210, 80, 255, 235));
                    g2d.fillRect(p.x - 6, p.y - 6, 12, 12);
                    g2d.setColor(new Color(255, 240, 160, 220));
                    g2d.drawRect(p.x - 7, p.y - 7, 14, 14);
                }
            }
        }

        int[] playerX = {centerX, centerX - 6, centerX + 6};
        int[] playerY = {centerY - 10, centerY + 8, centerY + 8};
        g2d.setColor(new Color(255, 225, 90));
        g2d.fillPolygon(playerX, playerY, 3);

        g2d.setClip(oldClip);
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(new Color(220, 230, 245, 200));
        g2d.drawOval(centerX - size / 2, centerY - size / 2, size, size);
        g2d.setStroke(oldStroke);
    }

    private void drawMinimapCell(Graphics2D g2d, int mapX, int mapY, int centerX, int centerY, double scale) {
        Point p1 = worldToMinimap(mapX, mapY, centerX, centerY, scale);
        Point p2 = worldToMinimap(mapX + 1, mapY, centerX, centerY, scale);
        Point p3 = worldToMinimap(mapX + 1, mapY + 1, centerX, centerY, scale);
        Point p4 = worldToMinimap(mapX, mapY + 1, centerX, centerY, scale);
        int[] xs = {p1.x, p2.x, p3.x, p4.x};
        int[] ys = {p1.y, p2.y, p3.y, p4.y};
        g2d.fillPolygon(xs, ys, 4);
    }

    private Point worldToMinimap(double worldX, double worldY, int centerX, int centerY, double scale) {
        double dx = worldX - posX;
        double dy = worldY - posY;
        double planeLength = Math.max(0.001, Math.sqrt(planeX * planeX + planeY * planeY));
        double rightX = planeX / planeLength;
        double rightY = planeY / planeLength;
        double localRight = dx * rightX + dy * rightY;
        double localForward = dx * dirX + dy * dirY;
        return new Point(
            centerX + (int)Math.round(localRight * scale),
            centerY - (int)Math.round(localForward * scale)
        );
    }

    private boolean isPointInsideMinimap(Point p, int centerX, int centerY, int size) {
        double radius = size / 2.0;
        double dx = p.x - centerX;
        double dy = p.y - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }

    private Enemy getFirstAliveEnemy() {
        for (Enemy enemy : enemies) {
            if (enemy.alive) {
                return enemy;
            }
        }
        return null;
    }

    private Enemy findShotEnemy() {
        Enemy target = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Enemy enemy : enemies) {
            if (!enemy.alive) continue;

            double relX = enemy.x - posX;
            double relY = enemy.y - posY;
            double invDet = 1.0 / (planeX * dirY - dirX * planeY);
            double transformX = invDet * (dirY * relX - dirX * relY);
            double transformY = invDet * (-planeY * relX + planeX * relY);

            if (transformY <= 0 || transformY >= nearestDistance) continue;

            int screenX = (int)((WIDTH / 2) * (1 + transformX / transformY));
            int spriteWidth = (int)(Math.abs((int)(HEIGHT / transformY)) * enemy.type.sizeScale);
            int hitboxWidth = (int)(spriteWidth * enemy.type.hitboxScale);

            if (WIDTH / 2 >= screenX - hitboxWidth / 2 && WIDTH / 2 <= screenX + hitboxWidth / 2
                && hasVisibleHitStripe(transformY, screenX, hitboxWidth)) {
                target = enemy;
                nearestDistance = transformY;
            }
        }

        return target;
    }

    private boolean hasVisibleHitStripe(double transformY, int screenX, int hitboxWidth) {
        int halfWindow = Math.max(8, hitboxWidth / 10);
        int startX = Math.max(0, Math.max(WIDTH / 2 - halfWindow, screenX - hitboxWidth / 2));
        int endX = Math.min(WIDTH - 1, Math.min(WIDTH / 2 + halfWindow, screenX + hitboxWidth / 2));

        for (int x = startX; x <= endX; x++) {
            if (transformY < zBuffer[x]) {
                return true;
            }
        }
        return false;
    }

    private Rectangle mazeModeButtonBounds() {
        return new Rectangle(WIDTH / 2 - 185, HEIGHT / 2 - 18, 370, 62);
    }

    private Rectangle slaughterModeButtonBounds() {
        return new Rectangle(WIDTH / 2 - 185, HEIGHT / 2 + 68, 370, 62);
    }

    private void handleStartMenuClick(int x, int y) {
        if (mazeModeButtonBounds().contains(x, y)) {
            startGame(GameMode.MAZE);
        } else if (slaughterModeButtonBounds().contains(x, y)) {
            startGame(GameMode.SLAUGHTER);
        }
    }

    private void returnToStartMenu() {
        screenState = ScreenState.START_MENU;
        isPaused = false;
        firing = false;
        grabMouse = false;
        moveForward = moveBackward = moveLeft = moveRight = false;
        turnLeft = turnRight = false;
        setCursor(Cursor.getDefaultCursor());
        playBgmForCurrentState();
        repaint();
    }

    private void drawStartMenu(Graphics2D g2d) {
        g2d.setColor(new Color(15, 13, 18));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        g2d.setColor(new Color(45, 18, 18));
        g2d.fillRect(0, HEIGHT / 2, WIDTH, HEIGHT / 2);

        g2d.setFont(new Font("Dialog", Font.BOLD, 62));
        String title = "JAVA DOOM";
        int titleX = WIDTH / 2 - g2d.getFontMetrics().stringWidth(title) / 2;
        g2d.setColor(new Color(255, 70, 45));
        g2d.drawString(title, titleX + 4, 180);
        g2d.setColor(new Color(245, 235, 215));
        g2d.drawString(title, titleX, 176);

        g2d.setFont(new Font("Dialog", Font.BOLD, 26));
        drawMenuButton(g2d, mazeModeButtonBounds(), "迷宫模式", new Color(70, 120, 180));
        drawMenuButton(g2d, slaughterModeButtonBounds(), "屠杀模式", new Color(165, 55, 45));

        g2d.setFont(new Font("Dialog", Font.PLAIN, 17));
        g2d.setColor(new Color(220, 220, 220));
        String hint = "1 / 2 选择模式";
        g2d.drawString(hint, WIDTH / 2 - g2d.getFontMetrics().stringWidth(hint) / 2, HEIGHT - 96);
    }

    private void drawMenuButton(Graphics2D g2d, Rectangle rect, String text, Color color) {
        g2d.setColor(new Color(0, 0, 0, 110));
        g2d.fillRect(rect.x + 6, rect.y + 6, rect.width, rect.height);
        g2d.setColor(color);
        g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
        g2d.setColor(new Color(255, 255, 255, 130));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
        g2d.setColor(Color.WHITE);
        int textX = rect.x + rect.width / 2 - g2d.getFontMetrics().stringWidth(text) / 2;
        int textY = rect.y + 40;
        g2d.drawString(text, textX, textY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        if (screenState == ScreenState.START_MENU) {
            drawStartMenu(g2d);
            return;
        }

        java.awt.geom.AffineTransform originalTransform = g2d.getTransform();
        int screenShakeX = 0;
        int screenShakeY = 0;
        if (damageEffectTimer > 0) {
            screenShakeX += (int) ((Math.random() - 0.5) * 6);
            screenShakeY += (int) ((Math.random() - 0.5) * 6);
        }
        if (meleeHitShakeTimer > 0) {
            double life = meleeHitShakeTimer / (double)MELEE_HIT_SHAKE_DURATION;
            int strength = Math.max(1, (int)Math.round(MELEE_HIT_SHAKE_STRENGTH * life));
            screenShakeX += (int)Math.round(Math.sin(frameCounter * 2.35) * strength
                    + Math.cos(frameCounter * 0.9) * strength * 0.35);
            screenShakeY += (int)Math.round(Math.cos(frameCounter * 2.1) * strength * 0.7
                    + Math.sin(frameCounter * 1.4) * strength * 0.3);
        }
        if (screenShakeX != 0 || screenShakeY != 0) {
            g2d.translate(screenShakeX, screenShakeY);
        }

        // 天空与地板
        g2d.setColor(new Color(20, 15, 30)); 
        g2d.fillRect(0, 0, WIDTH, HEIGHT / 2);
        g2d.setColor(new Color(35, 35, 40)); 
        g2d.fillRect(0, HEIGHT / 2, WIDTH, HEIGHT / 2);

        // 射线追踪迷宫墙壁
        for (int x = 0; x < WIDTH; x += WALL_COLUMN_WIDTH) {
            int stripeEnd = Math.min(WIDTH, x + WALL_COLUMN_WIDTH);
            double cameraX = 2 * (x + (stripeEnd - x) * 0.5) / (double) WIDTH - 1;
            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;

            int mapX = (int) posX;
            int mapY = (int) posY;

            double sideDistX, sideDistY;
            double deltaDistX = (rayDirX == 0) ? Double.MAX_VALUE : Math.abs(1 / rayDirX);
            double deltaDistY = (rayDirY == 0) ? Double.MAX_VALUE : Math.abs(1 / rayDirY);
            double perpWallDist;

            int stepX, stepY, hit = 0, side = 0;

            if (rayDirX < 0) { stepX = -1; sideDistX = (posX - mapX) * deltaDistX; }
            else { stepX = 1; sideDistX = (mapX + 1.0 - posX) * deltaDistX; }
            if (rayDirY < 0) { stepY = -1; sideDistY = (posY - mapY) * deltaDistY; }
            else { stepY = 1; sideDistY = (mapY + 1.0 - posY) * deltaDistY; }

            while (hit == 0) {
                if (sideDistX < sideDistY) { sideDistX += deltaDistX; mapX += stepX; side = 0; }
                else { sideDistY += deltaDistY; mapY += stepY; side = 1; }
                if (map[mapX][mapY] > 0) hit = 1;
            }

            if (side == 0) perpWallDist = (mapX - posX + (1 - stepX) / 2) / rayDirX;
            else           perpWallDist = (mapY - posY + (1 - stepY) / 2) / rayDirY;

            for (int z = x; z < stripeEnd; z++) {
                zBuffer[z] = perpWallDist;
            }

            int lineHeight = (int) (HEIGHT / perpWallDist);
            int rawDrawStart = -lineHeight / 2 + HEIGHT / 2;
            int rawDrawEnd = lineHeight / 2 + HEIGHT / 2;
            int drawStart = rawDrawStart;
            int drawEnd = rawDrawEnd;
            if (drawStart < 0) drawStart = 0;
            if (drawEnd >= HEIGHT) drawEnd = HEIGHT - 1;

            if (wallTextureColumns != null) {
                double wallX;
                if (side == 0) {
                    wallX = posY + perpWallDist * rayDirY;
                } else {
                    wallX = posX + perpWallDist * rayDirX;
                }
                wallX -= Math.floor(wallX);

                int textureWidth = wallTexture.getWidth();
                int textureHeight = wallTexture.getHeight();
                int textureX = (int)(wallX * textureWidth);
                if (side == 0 && rayDirX > 0) textureX = textureWidth - textureX - 1;
                if (side == 1 && rayDirY < 0) textureX = textureWidth - textureX - 1;
                textureX = clamp(textureX, 0, textureWidth - 1);

                int textureStartY = (int)((drawStart - rawDrawStart) * textureHeight / (double)Math.max(1, lineHeight));
                int textureEndY = (int)((drawEnd + 1 - rawDrawStart) * textureHeight / (double)Math.max(1, lineHeight));
                textureStartY = clamp(textureStartY, 0, textureHeight - 1);
                textureEndY = clamp(textureEndY, textureStartY + 1, textureHeight);
                BufferedImage wallColumn = side == 1 ? wallTextureShadedColumns[textureX] : wallTextureColumns[textureX];
                g2d.drawImage(
                    wallColumn,
                    x, drawStart, stripeEnd, drawEnd + 1,
                    0, textureStartY, 1, textureEndY,
                    null
                );
            } else {
                if (side == 1) g2d.setColor(new Color(50, 55, 65));
                else           g2d.setColor(new Color(80, 85, 95));
                g2d.fillRect(x, drawStart, stripeEnd - x, drawEnd - drawStart + 1);
            }
        }

        // 渲染所有敌人和远程光弹
        List<Enemy> drawOrder = new ArrayList<>(enemies);
        drawOrder.sort((a, b) -> Double.compare(distanceToPlayerSquared(b), distanceToPlayerSquared(a)));
        for (Enemy enemy : drawOrder) {
            drawEnemy(g2d, enemy);
        }
        drawBoss(g2d);
        drawExplosionParticles(g2d);
        drawBossLaser(g2d);
        drawBossBladeWaves(g2d);
        drawEnemyProjectiles(g2d);

        // 敌人受击血花特效
        if (hitEffectTimer > 0) {
            double spriteX_rel = hitEffectX - posX;
            double spriteY_rel = hitEffectY - posY;
            double invDet = 1.0 / (planeX * dirY - dirX * planeY);
            double transformX = invDet * (dirY * spriteX_rel - dirX * spriteY_rel);
            double transformY = invDet * (-planeY * spriteX_rel + planeX * spriteY_rel);
            
            if (transformY > 0) {
                int spriteScreenX = (int) ((WIDTH / 2) * (1 + transformX / transformY));
                int spriteHeight = (int) (Math.abs((int) (HEIGHT / transformY)) * 0.6);
                BufferedImage hitTexture = hitEffectIsMelee && meleeHitTexture != null ? meleeHitTexture : enemyHitTexture;
                if (hitTexture != null) {
                    int size = (int)(spriteHeight * 1.4);
                    int screenY = HEIGHT / 2 - size / 2;
                    g2d.drawImage(hitTexture, spriteScreenX - size / 2, screenY, size, size, null);
                } else {
                    int alpha = (int)(150 * (hitEffectTimer / 20.0));
                    g2d.setColor(new Color(255, 50, 50, alpha));
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4;
                        double distance = (20 - hitEffectTimer) * 1.5;
                        int px = spriteScreenX + (int)(Math.cos(angle) * distance);
                        int py = HEIGHT / 2 + (int)(Math.sin(angle) * distance);
                        int size = 15 + (20 - hitEffectTimer);
                        g2d.fillOval(px - size/2, py - size/2, size, size);
                    }
                }
            }
        }

        // 准星
        g2d.setColor(Color.GREEN);
        g2d.drawLine(WIDTH / 2 - 8, HEIGHT / 2, WIDTH / 2 + 8, HEIGHT / 2);
        g2d.drawLine(WIDTH / 2, HEIGHT / 2 - 8, WIDTH / 2, HEIGHT / 2 + 8);
        drawHitMarker(g2d);

        // 屏幕外敌人指示器（近战红色，远程蓝色）
        for (Enemy enemy : enemies) {
            drawEnemyIndicator(g2d, enemy);
        }

        // 补给物体渲染（实体掉落物，仅当玩家从当前视角看到且未被墙遮挡时显示）
        for (Medkit medkit : medkits) {
            if (!medkit.active) continue;

            double itemX = medkit.x + 0.5;
            double itemY = medkit.y + 0.5;
            double itemRelX = itemX - posX;
            double itemRelY = itemY - posY;
            double invDet = 1.0 / (planeX * dirY - dirX * planeY);
            double transformX = invDet * (dirY * itemRelX - dirX * itemRelY);
            double transformY = invDet * (-planeY * itemRelX + planeX * itemRelY);
            if (transformY > 0.1) {
                int screenX = (int)((WIDTH / 2) * (1 + transformX / transformY));
                if (screenX >= 0 && screenX < WIDTH && transformY < zBuffer[screenX]) {
                    int size = (int) (Math.max(22, 220 / transformY));
                    double bob = Math.sin(frameCounter * 0.12) * 10;
                    int screenY = HEIGHT / 2 + (int)((HEIGHT / transformY) * 0.16) - (int) bob;

                    if (medkitTexture != null) {
                        g2d.drawImage(medkitTexture, screenX - size/2, screenY - size/2, size, size, null);
                    } else {
                        g2d.setColor(new Color(245, 245, 245, 220));
                        g2d.fillRect(screenX - size/2, screenY - size/2, size, size);
                        g2d.setColor(new Color(180, 40, 40));
                        g2d.fillRect(screenX - size/4, screenY - size/9, size/2, size/5);
                        g2d.fillRect(screenX - size/9, screenY - size/4, size/5, size/2);
                        g2d.setColor(Color.RED);
                        g2d.setStroke(new BasicStroke(4));
                        g2d.drawRect(screenX - size/2, screenY - size/2, size, size);
                        g2d.setColor(new Color(255, 255, 255, 180));
                        g2d.drawLine(screenX - size/2 + 4, screenY - size/2 + 4, screenX + size/2 - 4, screenY - size/2 + 4);
                        g2d.drawLine(screenX - size/2 + 4, screenY - size/2 + 4, screenX - size/2 + 4, screenY + size/2 - 4);
                    }
                }
            }
        }
        for (AmmoBox ammoBox : ammoBoxes) {
            if (!ammoBox.active) continue;

            double itemX = ammoBox.x + 0.5;
            double itemY = ammoBox.y + 0.5;
            double itemRelX = itemX - posX;
            double itemRelY = itemY - posY;
            double invDet = 1.0 / (planeX * dirY - dirX * planeY);
            double transformX = invDet * (dirY * itemRelX - dirX * itemRelY);
            double transformY = invDet * (-planeY * itemRelX + planeX * itemRelY);
            if (transformY > 0.1) {
                int screenX = (int)((WIDTH / 2) * (1 + transformX / transformY));
                if (screenX >= 0 && screenX < WIDTH && transformY < zBuffer[screenX]) {
                    int size = (int) (Math.max(24, 240 / transformY));
                    double bob = Math.sin(frameCounter * 0.12 + Math.PI) * 10;
                    int screenY = HEIGHT / 2 + (int)((HEIGHT / transformY) * 0.16) - (int) bob;

                    if (ammoBoxTexture != null) {
                        g2d.drawImage(ammoBoxTexture, screenX - size/2, screenY - size/2, size, size, null);
                    } else {
                        g2d.setColor(new Color(130, 170, 230, 220));
                        g2d.fillRect(screenX - size/2, screenY - size/2, size, size);
                        g2d.setColor(new Color(40, 70, 140));
                        g2d.fillRect(screenX - size/2 + 4, screenY - size/2 + 4, size - 8, size - 8);
                        g2d.setColor(new Color(80, 130, 220));
                        g2d.fillRect(screenX - size/2 + 4, screenY - size/2 + 4, size/2, size/3);
                        g2d.fillRect(screenX + size/6 - size/2, screenY + size/6 - size/2, size/3, size/2);
                        g2d.setColor(Color.BLUE);
                        g2d.setStroke(new BasicStroke(3));
                        g2d.drawRect(screenX - size/2, screenY - size/2, size, size);
                    }
                }
            }
        }

        // 武器贴图绘制：待机、开火、装弹三状态
        if (playerHP > 0) {
            drawFlameEffect(g2d);
            drawWeapon(g2d);
            drawMeleeWeapon(g2d);
        }

        // 左上角积分/击杀数
        g2d.setColor(new Color(0, 0, 0, 155));
        g2d.fillRoundRect(18, 16, 172, 56, 10, 10);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2d.setColor(new Color(255, 230, 120));
        g2d.drawString("KILLS", 34, 39);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 26));
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.valueOf(killCount), 34, 65);
        drawBossHealthBar(g2d);

        // HUD 状态栏：血量和弹药拆成独立区域，避免文字压在条上
        int hudY = HEIGHT - 58;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, hudY, WIDTH, HEIGHT - hudY);

        int hpPanelX = 28;
        int ammoPanelX = 370;
        int stancePanelX = 704;
        int panelY = hudY + 12;
        int panelW = 300;
        int stancePanelW = 292;
        int barH = 14;

        g2d.setFont(new Font("Monospaced", Font.BOLD, 16));
        g2d.setColor(new Color(225, 230, 240));
        g2d.drawString("HEALTH", hpPanelX, panelY);
        String hpText = playerHP + " / " + maxPlayerHP;
        g2d.drawString(hpText, hpPanelX + panelW - g2d.getFontMetrics().stringWidth(hpText), panelY);

        int hpBarY = panelY + 10;
        double hpRatio = Math.max(0.0, Math.min(1.0, playerHP / (double)Math.max(1, maxPlayerHP)));
        g2d.setColor(new Color(70, 12, 18));
        g2d.fillRoundRect(hpPanelX, hpBarY, panelW, barH, 8, 8);
        g2d.setColor(new Color(55, 220, 90));
        g2d.fillRoundRect(hpPanelX, hpBarY, (int)(panelW * hpRatio), barH, 8, 8);
        g2d.setColor(new Color(255, 255, 255, 110));
        g2d.drawRoundRect(hpPanelX, hpBarY, panelW, barH, 8, 8);
        if (damageDisplayTimer > 0) {
            int alpha = (int)(180 * (damageDisplayTimer / 30.0));
            if (alpha < 0) alpha = 0;
            g2d.setColor(new Color(255, 90, 90, alpha));
            g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
            g2d.drawString("-" + damageDisplayValue, hpPanelX + panelW + 12, hpBarY + 18);
        }
        if (hpGainDisplayTimer > 0) {
            double life = hpGainDisplayTimer / (double)PICKUP_GAIN_DISPLAY_DURATION;
            int alpha = (int)(240 * life);
            int floatY = hpBarY - 8 - (int)((PICKUP_GAIN_DISPLAY_DURATION - hpGainDisplayTimer) * 0.35);
            g2d.setColor(new Color(120, 255, 150, alpha));
            g2d.setFont(new Font("Monospaced", Font.BOLD, 20));
            g2d.drawString("+" + hpGainDisplayValue + " HP", hpPanelX + 92, floatY);
        }

        g2d.setFont(new Font("Monospaced", Font.BOLD, 16));
        g2d.setColor(new Color(225, 230, 240));
        WeaponTemplate hudWeapon = currentWeaponTemplate();
        g2d.drawString(hudWeapon != null ? hudWeapon.name : "AMMO", ammoPanelX, panelY);
        String ammoText = currentAmmo + " / " + maxAmmo + "  +" + reserveAmmo + (isReloading ? "  RLD" : "");
        g2d.drawString(ammoText, ammoPanelX + panelW - g2d.getFontMetrics().stringWidth(ammoText), panelY);

        int ammoBarY = panelY + 10;
        double ammoRatio = Math.max(0.0, Math.min(1.0, currentAmmo / (double)Math.max(1, maxAmmo)));
        g2d.setColor(new Color(18, 34, 64));
        g2d.fillRoundRect(ammoPanelX, ammoBarY, panelW, barH, 8, 8);
        g2d.setColor(new Color(80, 165, 255));
        g2d.fillRoundRect(ammoPanelX, ammoBarY, (int)(panelW * ammoRatio), barH, 8, 8);
        g2d.setColor(new Color(255, 255, 255, 110));
        g2d.drawRoundRect(ammoPanelX, ammoBarY, panelW, barH, 8, 8);
        if (ammoGainDisplayTimer > 0) {
            double life = ammoGainDisplayTimer / (double)PICKUP_GAIN_DISPLAY_DURATION;
            int alpha = (int)(240 * life);
            int floatY = ammoBarY - 8 - (int)((PICKUP_GAIN_DISPLAY_DURATION - ammoGainDisplayTimer) * 0.35);
            g2d.setColor(new Color(120, 205, 255, alpha));
            g2d.setFont(new Font("Monospaced", Font.BOLD, 20));
            g2d.drawString("+" + ammoGainDisplayValue + " AMMO", ammoPanelX + 82, floatY);
        }

        if (currentMode == GameMode.SLAUGHTER) {
            g2d.setFont(new Font("Monospaced", Font.BOLD, 16));
            g2d.setColor(new Color(225, 230, 240));
            g2d.drawString("STANCE", stancePanelX, panelY);
            String stanceText = (int)Math.round(meleeStance) + "%";
            g2d.drawString(stanceText, stancePanelX + stancePanelW - g2d.getFontMetrics().stringWidth(stanceText), panelY);

            int stanceBarY = panelY + 10;
            double stanceRatio = Math.max(0.0, Math.min(1.0, meleeStance / MELEE_STANCE_MAX));
            g2d.setColor(new Color(56, 26, 18));
            g2d.fillRoundRect(stancePanelX, stanceBarY, stancePanelW, barH, 8, 8);
            g2d.setColor(sustainedMeleeActive ? new Color(255, 85, 45) : new Color(255, 190, 60));
            g2d.fillRoundRect(stancePanelX, stanceBarY, (int)(stancePanelW * stanceRatio), barH, 8, 8);
            int minAttackX = stancePanelX + (int)(stancePanelW * (MELEE_STANCE_MIN_ATTACK / MELEE_STANCE_MAX));
            g2d.setColor(new Color(255, 255, 255, 160));
            g2d.drawLine(minAttackX, stanceBarY - 2, minAttackX, stanceBarY + barH + 2);
            g2d.setColor(new Color(255, 255, 255, 110));
            g2d.drawRoundRect(stancePanelX, stanceBarY, stancePanelW, barH, 8, 8);
        }

        if (pickupMessageTimer > 0) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 22));
            g2d.drawString(pickupMessage, WIDTH/2 - g2d.getFontMetrics().stringWidth(pickupMessage)/2, 40);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
        }

        drawMinimap(g2d);

        if (damageEffectTimer > 0) {
            int alpha = (int)(120 * (damageEffectTimer / 30.0));
            if (alpha < 0) alpha = 0;
            g2d.setColor(new Color(255, 0, 0, alpha));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }

        drawNewWeaponAnimation(g2d);
        drawWeaponWheel(g2d);

        // 暂停菜单
        if (isPaused) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Impact", Font.PLAIN, 60));
            g2d.drawString("PAUSED", WIDTH/2 - 120, HEIGHT/2 - 100);
            
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            
            // 继续游戏按钮
            int buttonY = HEIGHT/2 - 20;
            int buttonWidth = 250;
            int buttonHeight = 50;
            int buttonX = WIDTH/2 - buttonWidth/2;
            
            g2d.setColor(new Color(80, 150, 255));
            g2d.fillRect(buttonX, buttonY, buttonWidth, buttonHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Continue", buttonX + 50, buttonY + 40);
            
            // 重新开始按钮
            buttonY += 80;
            g2d.setColor(new Color(100, 180, 100));
            g2d.fillRect(buttonX, buttonY, buttonWidth, buttonHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Restart", buttonX + 60, buttonY + 40);
            
            // 返回选模式菜单按钮
            buttonY += 80;
            g2d.setColor(new Color(190, 135, 60));
            g2d.fillRect(buttonX, buttonY, buttonWidth, buttonHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Modes", buttonX + 78, buttonY + 40);

            // 退出按钮
            buttonY += 80;
            g2d.setColor(new Color(200, 100, 100));
            g2d.fillRect(buttonX, buttonY, buttonWidth, buttonHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Exit", buttonX + 80, buttonY + 40);
            
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            g2d.drawString("Long press ESC to force quit", WIDTH/2 - 140, HEIGHT - 40);
        }

        // 死亡提示
        if (playerHP <= 0) {
            g2d.setColor(new Color(130, 0, 0, 220));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Impact", Font.PLAIN, 65));
            g2d.drawString("YOU WERE SLAIN", WIDTH/2 - 200, HEIGHT/2 - 20);
            
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.drawString("Final Kills: " + killCount, WIDTH/2 - 70, HEIGHT/2 + 30);
            
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            g2d.drawString("Press 'R' to Restart Game", WIDTH/2 - 170, HEIGHT/2 + 90);
        }
        g2d.setTransform(originalTransform);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (screenState == ScreenState.START_MENU) {
            handleStartMenuClick(e.getX(), e.getY());
            return;
        }

        // 暂停菜单按钮处理
        if (isPaused) {
            int buttonWidth = 250;
            int buttonHeight = 50;
            int buttonX = WIDTH/2 - buttonWidth/2;
            int clickX = e.getX();
            int clickY = e.getY();
            
            // 继续游戏按钮
            int continueY = HEIGHT/2 - 20;
            if (clickX >= buttonX && clickX <= buttonX + buttonWidth && 
                clickY >= continueY && clickY <= continueY + buttonHeight) {
                togglePause();
                return;
            }
            
            // 重新开始按钮
            int restartY = continueY + 80;
            if (clickX >= buttonX && clickX <= buttonX + buttonWidth && 
                clickY >= restartY && clickY <= restartY + buttonHeight) {
                isPaused = false;
                initGame();
                return;
            }
            
            // 返回选模式菜单按钮
            int modesY = restartY + 80;
            if (clickX >= buttonX && clickX <= buttonX + buttonWidth && 
                clickY >= modesY && clickY <= modesY + buttonHeight) {
                returnToStartMenu();
                return;
            }

            // 退出按钮
            int exitY = modesY + 80;
            if (clickX >= buttonX && clickX <= buttonX + buttonWidth && 
                clickY >= exitY && clickY <= exitY + buttonHeight) {
                System.exit(0);
            }
            return;
        }
        
        if (weaponWheelActive) {
            return;
        }
        firing = true;
        tryFireWeapon();
    }

    private void handleBossCheatKey(int keyCode) {
        if (screenState != ScreenState.PLAYING || currentMode != GameMode.SLAUGHTER || isPaused || playerHP <= 0) {
            return;
        }

        if (keyCode == BOSS_CHEAT_SEQUENCE[bossCheatProgress]) {
            bossCheatProgress++;
        } else {
            bossCheatProgress = keyCode == BOSS_CHEAT_SEQUENCE[0] ? 1 : 0;
        }

        if (bossCheatProgress >= BOSS_CHEAT_SEQUENCE.length) {
            bossCheatProgress = 0;
            forceSpawnBossAtCenter();
        }
    }

    // =========================
    // 输入处理：键盘、鼠标、作弊码、武器轮盘
    // =========================
    @Override
    public void keyPressed(KeyEvent e) {
        if (screenState == ScreenState.START_MENU) {
            if (e.getKeyCode() == KeyEvent.VK_1) {
                startGame(GameMode.MAZE);
            } else if (e.getKeyCode() == KeyEvent.VK_2) {
                startGame(GameMode.SLAUGHTER);
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
            return;
        }

        handleBossCheatKey(e.getKeyCode());

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            escPressedTime = (int) System.currentTimeMillis();
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_BACK_QUOTE) {
            openWeaponWheel();
            return;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_R) {
            if (weaponWheelActive) {
                return;
            }
            if (playerHP <= 0) {
                initGame();
            } else if (!isReloading && currentAmmo < maxAmmo && reserveAmmo > 0) {
                startReload();
            } else if (!isReloading && currentAmmo < maxAmmo) {
                pickupMessage = "No reserve ammo";
                pickupMessageTimer = 90;
            }
            return;
        }

        if (isPaused) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: moveForward = true; break;
            case KeyEvent.VK_S: moveBackward = true; break;
            case KeyEvent.VK_A: moveLeft = true; break;
            case KeyEvent.VK_D: moveRight = true; break;
            case KeyEvent.VK_LEFT: turnLeft = true; break;
            case KeyEvent.VK_RIGHT: turnRight = true; break;
            case KeyEvent.VK_SPACE:
                if (!meleeKeyDown) {
                    meleeKeyDown = true;
                    meleeHoldFrames = 0;
                    startMeleeAttack();
                }
                break;
            case KeyEvent.VK_OPEN_BRACKET: mouseSensitivity *= 0.8; break;
            case KeyEvent.VK_CLOSE_BRACKET: mouseSensitivity *= 1.25; break;
            case KeyEvent.VK_G:
                grabMouse = !grabMouse;
                if (grabMouse) {
                    setCursor(hiddenCursor);
                    if (robot != null) {
                        Window win = SwingUtilities.getWindowAncestor(this);
                        if (win != null && win.isShowing()) {
                            Point loc = win.getLocationOnScreen();
                            robot.mouseMove(loc.x + win.getWidth()/2, loc.y + win.getHeight()/2);
                        }
                    }
                } else {
                    setCursor(Cursor.getDefaultCursor());
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (screenState == ScreenState.START_MENU) {
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            int pressDuration = (int) System.currentTimeMillis() - escPressedTime;
            if (pressDuration > 600) {
                // 长按，强制退出
                System.exit(0);
            } else {
                // 短按，切换暂停
                togglePause();
            }
            escPressedTime = 0;
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_BACK_QUOTE) {
            closeWeaponWheel();
            return;
        }
        
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: moveForward = false; break;
            case KeyEvent.VK_S: moveBackward = false; break;
            case KeyEvent.VK_A: moveLeft = false; break;
            case KeyEvent.VK_D: moveRight = false; break;
            case KeyEvent.VK_LEFT: turnLeft = false; break;
            case KeyEvent.VK_RIGHT: turnRight = false; break;
            case KeyEvent.VK_SPACE:
                meleeKeyDown = false;
                endSustainedMelee();
                meleeHoldFrames = 0;
                break;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) { firing = false; }
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {
        if (grabMouse) return;
        if (!mouseInitialized) {
            lastMouseX = e.getX();
            mouseInitialized = true;
            return;
        }
        int dx = e.getX() - lastMouseX;
        lastMouseX = e.getX();
        double angle = dx * mouseSensitivity; // 取反：非抓取模式下鼠标右移视角左转，左移视角右转
        if (angle != 0) {
            double oldDirX = dirX;
            dirX = dirX * Math.cos(angle) - dirY * Math.sin(angle);
            dirY = oldDirX * Math.sin(angle) + dirY * Math.cos(angle);
            double oldPlaneX = planeX;
            planeX = planeX * Math.cos(angle) - planeY * Math.sin(angle);
            planeY = oldPlaneX * Math.sin(angle) + planeY * Math.cos(angle);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Java Doom: HD 1024x768");
        Main game = new Main();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Thread gameThread = new Thread(game);
        gameThread.start();
    }
}

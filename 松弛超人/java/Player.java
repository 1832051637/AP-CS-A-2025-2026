package taggame;

public class Player {
    public int id;
    public double x, y;
    public double vx, vy;
    public String color;
    public boolean isIT;
    public int size;

    // 移动状态
    public boolean onGround;
    public int coyoteTimer;
    public int jumpBufferTimer;
    public boolean facingRight;

    // 道具
    public Item item;
    public boolean hasItem;

    // 道具效果状态
    public boolean invisible;
    public int invisibleTimer;

    public boolean speedBoost;
    public int speedBoostTimer;
    public int speedBoostEffect;
    public int baseSpeed;

    public boolean trackerActive;
    public int trackerTimer;

    public boolean speedSlowActive;
    public int speedSlowTimer;
    public boolean speedSlowed;
    public int speedSlowEffect;

    public boolean catchBoostActive;
    public int catchBoostTimer;
    public boolean catchBoostEffect;

    public boolean fogActive;
    public int fogTimer;
    public double fogX, fogY;

    public boolean timeStopActive;
    public int timeStopTimer;

    public boolean shieldActive;
    public int shieldTimer;

    // 闪电技能
    public boolean hasZap;
    public boolean zapCooldown;
    public int zapCooldownTimer;

    // 定身状态
    public boolean zapped;
    public int zappedTimer;

    // 缓降
    public boolean slowFall;
    public int slowFallTimer;

    public Player(int id) {
        this.id = id;
        this.x = 150 + id * 150;
        this.y = 200;
        this.vx = 0;
        this.vy = 0;
        this.color = Constants.COLORS[id];
        this.isIT = false;
        this.size = Constants.PLAYER_SIZE;
        this.onGround = false;
        this.coyoteTimer = 0;
        this.jumpBufferTimer = 0;
        this.facingRight = true;
        this.baseSpeed = Constants.PLAYER_SPEED;
        this.speedBoostEffect = Constants.PLAYER_SPEED * 2;  // 初始化默认值
        this.speedSlowEffect = (int)(Constants.PLAYER_SPEED * 0.5);  // 初始化默认值
    }

    public int getPlayerSpeed() {
        if (speedBoost) return speedBoostEffect;
        if (speedSlowed) return speedSlowEffect;
        return Constants.PLAYER_SPEED;
    }

    // 更新道具效果（每帧调用）
    public void updateEffects(double dt) {
        if (invisible) {
            invisibleTimer -= dt;
            if (invisibleTimer <= 0) invisible = false;
        }
        if (speedBoost) {
            speedBoostTimer -= dt;
            if (speedBoostTimer <= 0) speedBoost = false;
        }
        if (trackerActive) {
            trackerTimer -= dt;
            if (trackerTimer <= 0) trackerActive = false;
        }
        if (speedSlowActive) {
            speedSlowTimer -= dt;
            if (speedSlowTimer <= 0) speedSlowActive = false;
        }
        if (catchBoostActive) {
            catchBoostTimer -= dt;
            if (catchBoostTimer <= 0) catchBoostActive = false;
        }
        if (fogActive) {
            fogTimer -= dt;
            if (fogTimer <= 0) fogActive = false;
        }
        if (timeStopActive) {
            timeStopTimer -= dt;
            if (timeStopTimer <= 0) timeStopActive = false;
        }
        if (shieldActive) {
            shieldTimer -= dt;
            if (shieldTimer <= 0) shieldActive = false;
        }
    }
}
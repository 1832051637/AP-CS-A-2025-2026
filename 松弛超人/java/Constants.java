package taggame;

public class Constants {
    public static final int PLAYER_COUNT = 4;
    public static final int GAME_TIME = 120;
    public static final int PLAYER_SIZE = 25;
    public static final int PLAYER_SPEED = 5;
    public static final int JUMP_SPEED = 10;
    public static final double GRAVITY = 0.8;

    // 跳跃手感优化
    public static final int COYOTE_TIME = 6;      // 土狼时间（帧数）
    public static final int JUMP_BUFFER = 6;      // 跳跃缓冲（帧数）

    public static final String[] COLORS = {"#00ffff", "#ff00ff", "#ffff00", "#00ff00"};

    // 弹簧配置
    public static final int SPRING_POWER = 14;
    public static final int SPRING_BOOST_HEIGHT = 300;

    // 补鼠夹配置
    public static final int TRAP_STUN_DURATION = 2000;  // 眩晕时间2秒
    public static final int MAX_TRAPS = 2;

    // 道具刷新间隔（毫秒）
    public static final int ITEM_SPAWN_INTERVAL = 45000;
    public static final int TRAP_SPAWN_INTERVAL = 20000;
    public static final int MAX_ITEMS = 3;

    // 电击技能
    public static final int ZAP_COOLDOWN = 25000;
    public static final int ZAP_STUN_DURATION = 2000;

    private Constants() {}
}
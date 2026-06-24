package taggame;

public enum ItemType {
    SMOKE_BOMB("隐身", "escape", 2500, 18000, "#888888", "隐"),
    SPEED_BOOTS("加速", "escape", 5000, 12000, "#ffeb3b", "速"),
    TELEPORT_SCROLL("瞬移", "escape", 0, 25000, "#9c27b0", "闪"),
    TRACKER_SCOPE("透视", "chase", 6000, 15000, "#e91e63", "眼"),
    SPEED_SLOW("减速", "chase", 2000, 20000, "#ff5722", "锁"),
    CATCH_BOOST("冲刺", "chase", 4000, 16000, "#f44336", "冲"),
    FOG_CLOUD("迷雾", "universal", 3000, 20000, "#607d8b", "雾"),
    TIME_STOP("冻结", "universal", 1000, 0, "#00bcd4", "停"),
    SHIELD("护盾", "defense", 3000, 22000, "#2196f3", "盾"),
    SWAP_CARD("互换", "defense", 0, 30000, "#4caf50", "换"),
    ZAP_GUN("电击", "zap", 0, 25000, "#ffd700", "电");

    public final String name;
    public final String type;
    public final int duration;
    public final int cooldown;
    public final String color;
    public final String icon;

    ItemType(String name, String type, int duration, int cooldown, String color, String icon) {
        this.name = name;
        this.type = type;
        this.duration = duration;
        this.cooldown = cooldown;
        this.color = color;
        this.icon = icon;
    }
}
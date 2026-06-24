package taggame;

public class Item {
    public double x, y;
    public ItemType type;
    public int size;
    public double bobOffset;
    public boolean active;

    public Item(double x, double y, ItemType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.size = 20;
        this.bobOffset = Math.random() * Math.PI * 2;
        this.active = true;
    }

    public boolean checkCollision(Player p) {
        double dx = p.x - x;
        double dy = p.y - y;
        return Math.sqrt(dx * dx + dy * dy) < p.size + size;
    }
}
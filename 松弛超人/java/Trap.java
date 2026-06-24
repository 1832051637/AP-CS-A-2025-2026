package taggame;

public class Trap {
    public double x, y;
    public double width, height;
    public boolean active;
    public double bobOffset;

    public Trap(double x, double y) {
        this.x = x;
        this.y = y;
        this.width = 30;
        this.height = 10;
        this.active = true;
        this.bobOffset = Math.random() * Math.PI * 2;
    }

    public boolean checkCollision(Player p) {
        if (!active) return false;
        return p.x + p.size > x &&
               p.x - p.size < x + width &&
               p.y > y - 20 &&
               p.y < y + height + 10;
    }
}
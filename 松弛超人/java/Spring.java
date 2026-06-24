package taggame;

public class Spring {
    public double x, y;
    public double width, height;
    public double bobOffset;

    public Spring(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bobOffset = Math.random() * Math.PI * 2;
    }

    public boolean checkCollision(Player p) {
        return p.x + p.size > x &&
               p.x - p.size < x + width &&
               p.y + p.size >= y - 5 &&
               p.y + p.size <= y + height + 10 &&
               p.vy >= 0;
    }
}
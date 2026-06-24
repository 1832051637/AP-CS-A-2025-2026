import java.awt.*;

public class Bullet {
    public int x, y;
    public int dx; // horizontal direction: -1 or 1
    public int width = 10;
    public int height = 5;
    public int damage = 1;
    public static final int BULLET_SPEED = 8;

    public Bullet(int x, int y, int dir, int dmg) {
        this.x = x;
        this.y = y;
        this.dx = (dir > 0) ? BULLET_SPEED : -BULLET_SPEED;
        this.damage = dmg;
    }

    public void update() {
        x += dx;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics2D g) {
        Color bulletColor;
        if (damage == 1) {
            bulletColor = Color.YELLOW;
        } else if (damage == 2) {
            bulletColor = Color.ORANGE;
        } else if (damage == 3) {
            bulletColor = Color.RED;
        } else {
            bulletColor = Color.MAGENTA;
        }
        g.setColor(bulletColor);
        g.fillOval(x, y, width, height);
        g.setColor(Color.BLACK);
        g.drawOval(x, y, width, height);
    }
}

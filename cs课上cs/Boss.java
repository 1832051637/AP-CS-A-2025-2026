import java.awt.*;

public class Boss {
    public int x, y;
    public int width = 48;
    public int height = 48;
    public int hp = 300;
    public int maxHp = 300;
    public int dx = 2; // movement speed
    public int leftBound = 0;
    public int rightBound = 1000;

    public Boss(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.leftBound = 100;
        this.rightBound = 900;
    }

    public void update() {
        x += dx;
        if (x < leftBound || x + width > rightBound) {
            dx = -dx;
        }
    }

    public void takeDamage(int damage) {
        hp -= damage;
        if (hp < 0) hp = 0;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics2D g) {
        // Boss body - red
        g.setColor(new Color(200, 50, 50));
        g.fillOval(x, y, width, height);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawOval(x, y, width, height);

        // Eyes
        g.setColor(Color.WHITE);
        g.fillOval(x + 10, y + 12, 8, 8);
        g.fillOval(x + 30, y + 12, 8, 8);
        g.setColor(Color.BLACK);
        g.fillOval(x + 12, y + 14, 4, 4);
        g.fillOval(x + 32, y + 14, 4, 4);

        // Health bar
        g.setColor(Color.RED);
        g.fillRect(x, y - 20, width, 8);
        g.setColor(Color.GREEN);
        int healthWidth = (int) ((double) hp / maxHp * width);
        g.fillRect(x, y - 20, healthWidth, 8);
        g.setColor(Color.BLACK);
        g.drawRect(x, y - 20, width, 8);

        // HP text
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        g.drawString("HP: " + hp, x + 10, y - 5);
    }
}

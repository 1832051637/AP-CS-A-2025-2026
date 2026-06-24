import java.awt.*;

public class DamageBoost {
    public int x, y;
    public int width = 24;
    public int height = 24;
    public int multiplier; // 2, 3, or 4

    public DamageBoost(int x, int y, int mult) {
        this.x = x;
        this.y = y;
        this.multiplier = mult;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics2D g) {
        Color color;
        if (multiplier == 2) color = new Color(255, 165, 0);      // Orange for 2x
        else if (multiplier == 3) color = new Color(255, 50, 50); // Red for 3x
        else color = new Color(200, 50, 200);                      // Purple for 4x

        g.setColor(color);
        g.fillRect(x, y, width, height);
        g.setColor(Color.WHITE);
        g.drawRect(x, y, width, height);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString(multiplier + "x", x + 6, y + 18);
    }
}

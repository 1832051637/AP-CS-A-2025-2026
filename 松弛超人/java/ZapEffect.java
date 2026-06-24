package taggame;

import java.util.*;

public class ZapEffect {
    private double startX, endX, y;
    private int lifetime;
    private List<double[]> points;
    private boolean active = true;

    public ZapEffect(double startX, double endX, double y) {
        this.startX = startX;
        this.endX = endX;
        this.y = y;
        this.lifetime = 30;
        generateLightningPoints();
    }

    private void generateLightningPoints() {
        points = new ArrayList<>();
        int segments = 8;
        double xStep = (endX - startX) / segments;
        double currentY = y;

        for (int i = 0; i <= segments; i++) {
            points.add(new double[]{startX + i * xStep, currentY + (Math.random() - 0.5) * 30});
            if (i < segments) {
                currentY += (Math.random() - 0.5) * 40;
            }
        }
    }

    public void update() {
        lifetime--;
        if (lifetime <= 0) {
            active = false;
        }
    }

    public boolean isActive() {
        return active;
    }

    public double getStartX() { return startX; }
    public double getEndX() { return endX; }
    public double getY() { return y; }
    public int getLifetime() { return lifetime; }
    public List<double[]> getPoints() { return points; }
}
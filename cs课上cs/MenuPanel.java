import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MenuPanel extends JPanel implements KeyListener {
    private SuperMariaGame parentWindow;

    public MenuPanel(SuperMariaGame parent) {
        this.parentWindow = parent;
        setPreferredSize(new Dimension(1000, 700));
        setBackground(new Color(40, 40, 100));
        setFocusable(true);
        addKeyListener(this);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        GradientPaint sky = new GradientPaint(0, 0, new Color(15, 30, 90), 0, h * 0.5f, new Color(35, 110, 220));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, w, h);

        GradientPaint topGlow = new GradientPaint(w * 0.2f, h * 0.1f, new Color(255, 230, 120, 180), w * 0.8f, h * 0.5f, new Color(255, 130, 40, 0));
        g2d.setPaint(topGlow);
        g2d.fillOval((int) (w * 0.15), (int) (h * 0.05), (int) (w * 0.7), (int) (h * 0.55));

        g2d.setPaint(new GradientPaint(0, h * 0.5f, new Color(28, 130, 58), 0, h, new Color(12, 62, 28)));
        g2d.fillRect(0, (int) (h * 0.5f), w, (int) (h * 0.5f));

        g2d.setFont(new Font("Press Start 2P", Font.BOLD, 64));
        String title = "SUPER MARIO";
        FontMetrics fm = g2d.getFontMetrics();
        int titleX = (w - fm.stringWidth(title)) / 2;
        int titleY = 130;
        g2d.setColor(new Color(12, 22, 60));
        g2d.drawString(title, titleX + 6, titleY + 6);
        g2d.setColor(new Color(255, 210, 55));
        g2d.drawString(title, titleX, titleY);

        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        String subtitle = "CLASSIC ADVENTURE";
        int subtitleX = (w - g2d.getFontMetrics().stringWidth(subtitle)) / 2;
        g2d.setColor(new Color(240, 240, 240));
        g2d.drawString(subtitle, subtitleX, titleY + 60);

        int buttonW = 380;
        int buttonH = 90;
        int buttonX = (w - buttonW) / 2;
        int buttonY = 320;
        g2d.setPaint(new GradientPaint(buttonX, buttonY, new Color(250, 120, 40), buttonX, buttonY + buttonH, new Color(220, 55, 40)));
        g2d.fillRoundRect(buttonX, buttonY, buttonW, buttonH, 36, 36);
        g2d.setColor(new Color(255, 210, 120));
        g2d.setStroke(new BasicStroke(5));
        g2d.drawRoundRect(buttonX + 2, buttonY + 2, buttonW - 4, buttonH - 4, 36, 36);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        String start = "PRESS ENTER TO START";
        int startX = (w - g2d.getFontMetrics().stringWidth(start)) / 2;
        g2d.setColor(Color.WHITE);
        g2d.drawString(start, startX, buttonY + 56);

        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("One epic mode. One endless world.", (w - g2d.getFontMetrics().stringWidth("One epic mode. One endless world.")) / 2, buttonY + 145);
        g2d.drawString("Run, jump and collect coins in classic style.", (w - g2d.getFontMetrics().stringWidth("Run, jump and collect coins in classic style.")) / 2, buttonY + 175);

        int blockX = w - 240;
        int blockY = h - 220;
        g2d.setColor(new Color(220, 170, 70));
        g2d.fillRect(blockX, blockY, 72, 72);
        g2d.setColor(new Color(190, 140, 45));
        g2d.fillRect(blockX, blockY, 72, 16);
        g2d.setFont(new Font("Arial", Font.BOLD, 44));
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.drawString("?", blockX + 18, blockY + 54);

        g2d.setColor(new Color(80, 110, 170, 180));
        g2d.fillOval(120, h - 230, 40, 40);
        g2d.setColor(new Color(255, 215, 90));
        g2d.fillOval(130, h - 220, 20, 20);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            parentWindow.startGame(0);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}
}

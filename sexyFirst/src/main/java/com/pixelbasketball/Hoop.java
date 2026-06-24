package com.pixelbasketball;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public final class Hoop {
    public enum Side {
        LEFT(-1),
        RIGHT(1);

        private final int scoringDirection;

        Side(int scoringDirection) {
            this.scoringDirection = scoringDirection;
        }

        public int scoringDirection() {
            return scoringDirection;
        }
    }

    public record Circle(Point2D center, double radius) {
    }

    public record ScoreWindow(double planeY, double leftX, double rightX) {
        boolean containsX(double x) {
            return x >= leftX && x <= rightX;
        }
    }

    private static final double SIDE_INSET_RATIO = 0.070;
    private static final double RIM_HEIGHT_RATIO = 0.330;
    private static final double BACKBOARD_WIDTH = 12.0;
    private static final double BACKBOARD_HEIGHT = 146.0;
    private static final double RIM_RADIUS = 28.0;
    private static final double RIM_TUBE_RADIUS = 5.5;
    private static final double BOARD_TO_RIM_GAP = 8.0;
    private static final double NET_DEPTH = 52.0;

    private final Side side;
    private final Rectangle2D backboard;
    private final Point2D rimCenter;
    private final double rimRadius;
    private final double rimTubeRadius;
    private final ScoreWindow scoreWindow;
    private final List<Circle> rimCollisionCircles;
    private final double netBottomY;

    private Hoop(Side side,
                 Rectangle2D backboard,
                 Point2D rimCenter,
                 double rimRadius,
                 double rimTubeRadius,
                 ScoreWindow scoreWindow,
                 List<Circle> rimCollisionCircles,
                 double netBottomY) {
        this.side = side;
        this.backboard = backboard;
        this.rimCenter = rimCenter;
        this.rimRadius = rimRadius;
        this.rimTubeRadius = rimTubeRadius;
        this.scoreWindow = scoreWindow;
        this.rimCollisionCircles = List.copyOf(rimCollisionCircles);
        this.netBottomY = netBottomY;
    }

    public static Hoop forSide(Side side, double courtWidth, double courtHeight) {
        double sideInset = courtWidth * SIDE_INSET_RATIO;
        double rimY = courtHeight * RIM_HEIGHT_RATIO;
        double backboardY = rimY - BACKBOARD_HEIGHT * 0.52;
        double backboardX = side == Side.LEFT
                ? sideInset
                : courtWidth - sideInset - BACKBOARD_WIDTH;

        Rectangle2D backboard = new Rectangle2D(
                backboardX,
                backboardY,
                BACKBOARD_WIDTH,
                BACKBOARD_HEIGHT
        );

        double rimCenterX = side == Side.LEFT
                ? backboard.getMaxX() + BOARD_TO_RIM_GAP + RIM_RADIUS
                : backboard.getMinX() - BOARD_TO_RIM_GAP - RIM_RADIUS;
        Point2D rimCenter = new Point2D(rimCenterX, rimY);

        double rimLeft = rimCenterX - RIM_RADIUS;
        double rimRight = rimCenterX + RIM_RADIUS;
        double scorePadding = RIM_TUBE_RADIUS * 0.95;
        ScoreWindow scoreWindow = new ScoreWindow(
                rimY,
                rimLeft + scorePadding,
                rimRight - scorePadding
        );

        List<Circle> rimCollisionCircles = List.of(
                new Circle(new Point2D(rimLeft, rimY), RIM_TUBE_RADIUS),
                new Circle(new Point2D(rimRight, rimY), RIM_TUBE_RADIUS)
        );

        return new Hoop(
                side,
                backboard,
                rimCenter,
                RIM_RADIUS,
                RIM_TUBE_RADIUS,
                scoreWindow,
                rimCollisionCircles,
                rimY + NET_DEPTH
        );
    }

    public Side side() {
        return side;
    }

    public Rectangle2D backboardBounds() {
        return backboard;
    }

    public Point2D rimCenter() {
        return rimCenter;
    }

    public double rimRadius() {
        return rimRadius;
    }

    public double rimTubeRadius() {
        return rimTubeRadius;
    }

    public ScoreWindow scoreWindow() {
        return scoreWindow;
    }

    public List<Circle> rimCollisionCircles() {
        return rimCollisionCircles;
    }

    public Point2D shotTarget() {
        double entryOffset = side == Side.LEFT ? rimRadius * 0.04 : -rimRadius * 0.04;
        return rimCenter.add(entryOffset, rimTubeRadius * 0.45);
    }

    public boolean isMadeBasket(double previousX,
                                double previousY,
                                double currentX,
                                double currentY,
                                double verticalVelocity) {
        if (verticalVelocity <= 0.0) {
            return false;
        }
        if (previousY > scoreWindow.planeY() || currentY < scoreWindow.planeY()) {
            return false;
        }

        double travelY = currentY - previousY;
        if (Math.abs(travelY) < 0.0001) {
            return false;
        }

        double progress = (scoreWindow.planeY() - previousY) / travelY;
        double xAtPlane = previousX + (currentX - previousX) * progress;
        return scoreWindow.containsX(xAtPlane);
    }

    public void draw(GraphicsContext gc) {
        gc.save();
        gc.setLineWidth(4.0);
        gc.setStroke(Color.rgb(240, 244, 255));
        gc.strokeRect(
                backboard.getMinX(),
                backboard.getMinY(),
                backboard.getWidth(),
                backboard.getHeight()
        );

        gc.setStroke(Color.rgb(247, 96, 43));
        gc.setLineWidth(rimTubeRadius * 1.35);
        double rimDiameter = rimRadius * 2.0;
        gc.strokeOval(
                rimCenter.getX() - rimRadius,
                rimCenter.getY() - rimRadius * 0.28,
                rimDiameter,
                rimRadius * 0.56
        );

        gc.setStroke(Color.rgb(225, 231, 241, 0.55));
        gc.setLineWidth(2.0);
        double netTopLeft = scoreWindow.leftX();
        double netTopRight = scoreWindow.rightX();
        double netBottomLeft = rimCenter.getX() - rimRadius * 0.46;
        double netBottomRight = rimCenter.getX() + rimRadius * 0.46;
        gc.strokeLine(netTopLeft, rimCenter.getY() + rimTubeRadius, netBottomLeft, netBottomY);
        gc.strokeLine(netTopRight, rimCenter.getY() + rimTubeRadius, netBottomRight, netBottomY);
        gc.strokeLine(netBottomLeft, netBottomY, netBottomRight, netBottomY);
        gc.restore();
    }
}

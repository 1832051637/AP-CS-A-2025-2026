package com.crystalbreak.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShotResult {
    private Integer firstCueContactNumber;
    private final List<Ball> pottedBalls = new ArrayList<>();
    private boolean cueBallPotted;
    private int railContacts;

    public Integer getFirstCueContactNumber() {
        return firstCueContactNumber;
    }

    public void setFirstCueContactNumber(Integer firstCueContactNumber) {
        if (this.firstCueContactNumber == null) {
            this.firstCueContactNumber = firstCueContactNumber;
        }
    }

    public List<Ball> getPottedBalls() {
        return Collections.unmodifiableList(pottedBalls);
    }

    public void addPottedBall(Ball ball) {
        if (!pottedBalls.contains(ball)) {
            pottedBalls.add(ball);
        }
    }

    public boolean isCueBallPotted() {
        return cueBallPotted;
    }

    public void setCueBallPotted(boolean cueBallPotted) {
        this.cueBallPotted = cueBallPotted;
    }

    public int getRailContacts() {
        return railContacts;
    }

    public void incrementRailContacts() {
        railContacts++;
    }

    public boolean pottedAnyObjectBall() {
        return pottedBalls.stream().anyMatch(ball -> ball.getGroup() != BallGroup.CUE);
    }

    public boolean pottedEightBall() {
        return pottedBalls.stream().anyMatch(ball -> ball.getGroup() == BallGroup.EIGHT);
    }
}

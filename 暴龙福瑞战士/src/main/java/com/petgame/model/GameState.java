package com.petgame.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private String playerName;
    private int money;
    private int gems;
    private int day;
    private int autoTick;
    private int totalIncome;
    private int totalExpense;
    private int homeLevel;
    private int claimedAchievements;
    private String statusMessage;
    private List<Pet> pets = new ArrayList<>();
    private List<InventoryItem> inventory = new ArrayList<>();
    private List<String> history = new ArrayList<>();

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getMoney() { return money; }
    public void setMoney(int money) { this.money = money; }
    public int getGems() { return gems; }
    public void setGems(int gems) { this.gems = gems; }
    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }
    public int getAutoTick() { return autoTick; }
    public void setAutoTick(int autoTick) { this.autoTick = autoTick; }
    public int getTotalIncome() { return totalIncome; }
    public void setTotalIncome(int totalIncome) { this.totalIncome = totalIncome; }
    public int getTotalExpense() { return totalExpense; }
    public void setTotalExpense(int totalExpense) { this.totalExpense = totalExpense; }
    public int getHomeLevel() { return homeLevel; }
    public void setHomeLevel(int homeLevel) { this.homeLevel = homeLevel; }
    public int getClaimedAchievements() { return claimedAchievements; }
    public void setClaimedAchievements(int claimedAchievements) { this.claimedAchievements = claimedAchievements; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public List<Pet> getPets() { return pets; }
    public void setPets(List<Pet> pets) { this.pets = pets; }
    public List<InventoryItem> getInventory() { return inventory; }
    public void setInventory(List<InventoryItem> inventory) { this.inventory = inventory; }
    public List<String> getHistory() { return history; }
    public void setHistory(List<String> history) { this.history = history; }
}

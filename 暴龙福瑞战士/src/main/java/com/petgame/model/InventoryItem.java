package com.petgame.model;

public class InventoryItem {
    private String name;
    private String type;
    private int quantity;
    private int value;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}

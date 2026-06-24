package com.petgame.model;

public class Pet {
    private long id;
    private String name;
    private String species;
    private int age;
    private int hunger;
    private int mood;
    private int health;
    private int affection;
    private int growth;
    private String form;
    private boolean adopted;
    private int price;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public int getHunger() { return hunger; }
    public void setHunger(int hunger) { this.hunger = hunger; }
    public int getMood() { return mood; }
    public void setMood(int mood) { this.mood = mood; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public int getAffection() { return affection; }
    public void setAffection(int affection) { this.affection = affection; }
    public int getGrowth() { return growth; }
    public void setGrowth(int growth) { this.growth = growth; }
    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }
    public boolean isAdopted() { return adopted; }
    public void setAdopted(boolean adopted) { this.adopted = adopted; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
}

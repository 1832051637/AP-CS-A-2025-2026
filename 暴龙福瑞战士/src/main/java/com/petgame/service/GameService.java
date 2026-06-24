package com.petgame.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petgame.model.GameState;
import com.petgame.model.InventoryItem;
import com.petgame.model.Pet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class GameService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public GameService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void ensureOneTimeSeed() {
        GameState state = loadState();
        if (state.getPets() == null || state.getPets().isEmpty() ||
                state.getInventory() == null || state.getInventory().isEmpty()) {
            resetGame();
        }
    }

    @Scheduled(fixedRate = 60000L)
    public void automaticClock() {
        advanceTurn();
    }

    public synchronized GameState loadState() {
        String json = jdbcTemplate.queryForObject("select state_json from game_state where id = 1", String.class);
        try {
            GameState state = objectMapper.readValue(json, GameState.class);
            if (state.getHomeLevel() <= 0) {
                state.setHomeLevel(3);
            }
            if (state.getPets() != null) {
                for (Pet pet : state.getPets()) {
                    normalizePet(pet);
                }
            }
            return state;
        } catch (Exception ex) {
            throw new IllegalStateException("无法解析游戏存档", ex);
        }
    }

    public synchronized GameState resetGame() {
        GameState state = buildNewGame();
        saveState(state);
        return state;
    }

    public synchronized GameState feed(long petId) {
        GameState state = loadState();
        Pet pet = findPet(state, petId);
        if (pet == null) {
            return state;
        }
        if (pet.getHunger() <= 0) {
            state.setStatusMessage(pet.getName() + " 已经吃饱了。");
            saveState(state);
            return state;
        }

        boolean usedStock = consumeItem(state, "food");
        if (!usedStock && !spendIfPossible(state, 18)) {
            state.setStatusMessage("金币不足，无法给 " + pet.getName() + " 临时采购食物。");
            saveState(state);
            return state;
        }

        pet.setHunger(Math.max(0, pet.getHunger() - 18));
        pet.setMood(Math.min(100, pet.getMood() + 8));
        addGrowth(pet, 10);
        appendHistory(state, "喂食了 " + pet.getName());
        state.setStatusMessage(pet.getName() + (usedStock ? " 吃掉了一份营养粮。" : " 临时采购了一份食物。"));
        evolveIfReady(state, pet);
        saveState(state);
        return state;
    }

    public synchronized GameState wash(long petId) {
        GameState state = loadState();
        Pet pet = findPet(state, petId);
        if (pet == null) {
            return state;
        }
        if (pet.getHealth() >= 100) {
            state.setStatusMessage(pet.getName() + " 现在状态很好，不需要洗护。");
            saveState(state);
            return state;
        }

        boolean usedStock = consumeItem(state, "care");
        if (!usedStock && !spendIfPossible(state, 24)) {
            state.setStatusMessage("金币不足，无法给 " + pet.getName() + " 使用备用清洁服务。");
            saveState(state);
            return state;
        }

        pet.setHealth(Math.min(100, pet.getHealth() + 12));
        pet.setMood(Math.min(100, pet.getMood() + 5));
        addGrowth(pet, 8);
        appendHistory(state, "洗护了 " + pet.getName());
        state.setStatusMessage(pet.getName() + (usedStock ? " 清爽多了。" : " 临时用了店里的备用清洁服务。"));
        evolveIfReady(state, pet);
        saveState(state);
        return state;
    }

    public synchronized GameState interact(long petId) {
        GameState state = loadState();
        Pet pet = findPet(state, petId);
        if (pet != null) {
            boolean usedToy = consumeItem(state, "toy");
            pet.setMood(Math.min(100, pet.getMood() + (usedToy ? 12 : 5)));
            pet.setAffection(Math.min(100, pet.getAffection() + (usedToy ? 10 : 4)));
            addGrowth(pet, usedToy ? 16 : 8);
            appendHistory(state, "与 " + pet.getName() + " 互动");
            state.setStatusMessage(pet.getName() + (usedToy ? " 玩得很开心，对你更亲近了。" : " 没有玩具，只陪伴了一会儿。"));
            evolveIfReady(state, pet);
            saveState(state);
        }
        return state;
    }

    public synchronized GameState adoptPet() {
        GameState state = loadState();
        Pet pet = createRandomPet();
        if (!spendIfPossible(state, pet.getPrice())) {
            state.setStatusMessage("领养 " + pet.getName() + " 需要 " + pet.getPrice() + " 金币，当前余额不足。");
            saveState(state);
            return state;
        }
        pet.setAdopted(true);
        state.getPets().add(pet);
        appendHistory(state, "领养了 " + pet.getName());
        state.setStatusMessage("新宠物 " + pet.getName() + " 加入了店里。");
        saveState(state);
        return state;
    }

    public synchronized GameState sellPet(long petId) {
        GameState state = loadState();
        Pet pet = findPet(state, petId);
        if (pet != null) {
            if (state.getPets().size() <= 1) {
                state.setStatusMessage("至少保留一只宠物，不能卖出最后的伙伴。");
                saveState(state);
                return state;
            }
            state.getPets().remove(pet);
            int gain = pet.getPrice() + pet.getAffection() + pet.getMood();
            earn(state, gain);
            appendHistory(state, "卖出 " + pet.getName());
            state.setStatusMessage(pet.getName() + " 已经成交，获得 " + gain + " 金币。");
            saveState(state);
        }
        return state;
    }

    public synchronized GameState renamePet(long petId, String newName) {
        GameState state = loadState();
        Pet pet = findPet(state, petId);
        if (pet == null) {
            return state;
        }

        String cleanName = newName == null ? "" : newName.trim();
        if (cleanName.isEmpty()) {
            state.setStatusMessage("宠物名字不能为空。");
            saveState(state);
            return state;
        }
        if (cleanName.length() > 8) {
            cleanName = cleanName.substring(0, 8);
        }

        String oldName = pet.getName();
        pet.setName(cleanName);
        appendHistory(state, "将 " + oldName + " 改名为 " + cleanName);
        state.setStatusMessage(oldName + " 现在叫 " + cleanName + "。");
        saveState(state);
        return state;
    }

    public synchronized GameState buyItem(String type) {
        GameState state = loadState();
        InventoryItem template = shopTemplate(type);
        if (template == null) {
            state.setStatusMessage("商店里没有这个商品。");
            saveState(state);
            return state;
        }
        if (!spendIfPossible(state, template.getValue())) {
            state.setStatusMessage("购买 " + template.getName() + " 需要 " + template.getValue() + " 金币，余额不足。");
            saveState(state);
            return state;
        }

        addInventoryItem(state, template);
        appendHistory(state, "购买了 " + template.getName());
        state.setStatusMessage("已购买 1 份 " + template.getName() + "。");
        saveState(state);
        return state;
    }

    public synchronized GameState trainPet(long petId) {
        GameState state = loadState();
        Pet pet = findPet(state, petId);
        if (pet == null) {
            return state;
        }
        if (!spendIfPossible(state, 40)) {
            state.setStatusMessage("训练需要 40 金币，余额不足。");
            saveState(state);
            return state;
        }

        pet.setHunger(Math.min(100, pet.getHunger() + 8));
        pet.setMood(Math.min(100, pet.getMood() + 10));
        pet.setHealth(Math.min(100, pet.getHealth() + 4));
        pet.setAffection(Math.min(100, pet.getAffection() + 6));
        addGrowth(pet, 22);
        appendHistory(state, "训练了 " + pet.getName());
        state.setStatusMessage(pet.getName() + " 完成训练，亲密度和状态提升。");
        evolveIfReady(state, pet);
        saveState(state);
        return state;
    }

    public synchronized GameState restPet(long petId) {
        GameState state = loadState();
        Pet pet = findPet(state, petId);
        if (pet != null) {
            pet.setHunger(Math.min(100, pet.getHunger() + 4));
            pet.setMood(Math.min(100, pet.getMood() + 8));
            pet.setHealth(Math.min(100, pet.getHealth() + 10));
            addGrowth(pet, 6);
            appendHistory(state, "安排 " + pet.getName() + " 休息");
            state.setStatusMessage(pet.getName() + " 休息好了，状态恢复。");
            evolveIfReady(state, pet);
            saveState(state);
        }
        return state;
    }

    public synchronized GameState explorePet(long petId) {
        GameState state = loadState();
        Pet pet = findPet(state, petId);
        if (pet == null) {
            return state;
        }
        if (pet.getHunger() > 90 || pet.getHealth() < 20) {
            state.setStatusMessage(pet.getName() + " 现在不适合探险，先喂食或休息。");
            saveState(state);
            return state;
        }

        int reward = 45 + random.nextInt(56);
        pet.setHunger(Math.min(100, pet.getHunger() + 10));
        pet.setMood(Math.max(0, pet.getMood() - 3));
        pet.setAffection(Math.min(100, pet.getAffection() + 3));
        addGrowth(pet, 18);
        earn(state, reward);
        appendHistory(state, pet.getName() + " 探险归来");
        state.setStatusMessage(pet.getName() + " 探险带回 " + reward + " 金币。");
        evolveIfReady(state, pet);
        saveState(state);
        return state;
    }

    public synchronized GameState saveGame() {
        GameState state = loadState();
        appendHistory(state, "保存当前宠物状态");
        state.setStatusMessage("当前宠物、库存和资金已经保存。");
        saveState(state);
        return state;
    }

    public synchronized GameState autoSaveGame() {
        GameState state = loadState();
        saveState(state);
        return state;
    }

    public synchronized GameState upgradeHome() {
        GameState state = loadState();
        int nextLevel = Math.max(1, state.getHomeLevel()) + 1;
        int cost = 120 + nextLevel * 45;
        if (!spendIfPossible(state, cost)) {
            state.setStatusMessage("升级家园到 Lv." + nextLevel + " 需要 " + cost + " 金币，余额不足。");
            saveState(state);
            return state;
        }

        state.setHomeLevel(nextLevel);
        addInventoryItem(state, item("牧草", "material", 2, 18));
        appendHistory(state, "升级家园到 Lv." + nextLevel);
        state.setStatusMessage("森林小屋升级到 Lv." + nextLevel + "，背包获得 2 份牧草。");
        saveState(state);
        return state;
    }

    public synchronized GameState visitFriend(String friendName) {
        GameState state = loadState();
        int reward = 18 + random.nextInt(23);
        earn(state, reward);
        appendHistory(state, "拜访好友 " + friendName);
        state.setStatusMessage("拜访了 " + friendName + " 的小屋，获得 " + reward + " 金币互访奖励。");
        saveState(state);
        return state;
    }

    public synchronized GameState claimAchievement(String achievementKey) {
        GameState state = loadState();
        int bit = achievementBit(achievementKey);
        if (bit == 0) {
            state.setStatusMessage("这个成就暂时不可领取。");
            saveState(state);
            return state;
        }
        if ((state.getClaimedAchievements() & bit) == bit) {
            state.setStatusMessage("这个成就奖励已经领取过了。");
            saveState(state);
            return state;
        }
        if (!isAchievementReady(state, achievementKey)) {
            state.setStatusMessage("这个成就还没有达成，先继续照护宠物。");
            saveState(state);
            return state;
        }

        int reward = achievementReward(achievementKey);
        state.setClaimedAchievements(state.getClaimedAchievements() | bit);
        earn(state, reward);
        appendHistory(state, "领取成就奖励 " + achievementKey);
        state.setStatusMessage("成就奖励已领取，金币增加 " + reward + "。");
        saveState(state);
        return state;
    }

    public synchronized GameState organizeInventory() {
        GameState state = loadState();
        if (state.getInventory() == null || state.getInventory().isEmpty()) {
            state.setStatusMessage("背包还是空的，暂无可整理物品。");
            saveState(state);
            return state;
        }

        List<InventoryItem> merged = new ArrayList<InventoryItem>();
        for (InventoryItem item : state.getInventory()) {
            InventoryItem existing = null;
            for (InventoryItem candidate : merged) {
                if (candidate.getType().equals(item.getType()) && candidate.getName().equals(item.getName())) {
                    existing = candidate;
                    break;
                }
            }
            if (existing == null) {
                merged.add(item(item.getName(), item.getType(), Math.max(0, item.getQuantity()), item.getValue()));
            } else {
                existing.setQuantity(existing.getQuantity() + Math.max(0, item.getQuantity()));
                existing.setValue(Math.max(existing.getValue(), item.getValue()));
            }
        }
        state.setInventory(merged);
        appendHistory(state, "整理背包");
        state.setStatusMessage("背包已按类型合并整理。");
        saveState(state);
        return state;
    }

    public synchronized GameState advanceTurn() {
        GameState state = loadState();
        state.setAutoTick(state.getAutoTick() + 1);
        state.setDay(state.getDay() + 1);
        for (Pet pet : state.getPets()) {
            pet.setHunger(Math.min(100, pet.getHunger() + 12));
            pet.setMood(Math.max(0, pet.getMood() - 7));
            pet.setHealth(Math.max(0, pet.getHealth() - 2));
        }
        earn(state, Math.max(0, state.getPets().size() * 10 - 5));
        appendHistory(state, "时间推进到第 " + state.getDay() + " 天");
        state.setStatusMessage("第 " + state.getDay() + " 天经营完成。");
        saveState(state);
        return state;
    }

    private GameState buildNewGame() {
        GameState state = new GameState();
        state.setPlayerName("训练师小艾");
        state.setMoney(1200);
        state.setGems(24);
        state.setDay(1);
        state.setAutoTick(0);
        state.setTotalIncome(0);
        state.setTotalExpense(0);
        state.setHomeLevel(3);
        state.setClaimedAchievements(0);
        state.setStatusMessage("开店成功，准备迎接第一批宠物。");
        state.setPets(new ArrayList<Pet>());
        state.setInventory(defaultInventory());
        state.setHistory(new ArrayList<String>());
        for (int i = 0; i < 4; i++) {
            state.getPets().add(createRandomPet());
        }
        return state;
    }

    private List<InventoryItem> defaultInventory() {
        List<InventoryItem> items = new ArrayList<InventoryItem>();
        items.add(item("营养粮", "food", 12, 25));
        items.add(item("玩具球", "toy", 6, 40));
        items.add(item("洗护液", "care", 8, 30));
        items.add(item("木屋", "place", 2, 120));
        return items;
    }

    private InventoryItem item(String name, String type, int quantity, int value) {
        InventoryItem item = new InventoryItem();
        item.setName(name);
        item.setType(type);
        item.setQuantity(quantity);
        item.setValue(value);
        return item;
    }

    private Pet createRandomPet() {
        String[] names = {"泡泡", "团团", "跳跳", "嘟嘟", "朵朵", "米粒", "小枝", "果果"};
        String[] species = {"蓝柴犬", "绿柯基", "粉绒兔", "棕耳兔"};
        Pet pet = new Pet();
        pet.setId(System.currentTimeMillis() + random.nextInt(100000));
        pet.setName(names[random.nextInt(names.length)]);
        pet.setSpecies(species[random.nextInt(species.length)]);
        pet.setAge(1 + random.nextInt(5));
        pet.setHunger(30 + random.nextInt(40));
        pet.setMood(55 + random.nextInt(30));
        pet.setHealth(70 + random.nextInt(20));
        pet.setAffection(20 + random.nextInt(40));
        pet.setGrowth(80 + random.nextInt(80));
        pet.setForm("幼体");
        pet.setPrice(180 + random.nextInt(220));
        pet.setAdopted(true);
        return pet;
    }

    private Pet findPet(GameState state, long petId) {
        for (Pet pet : state.getPets()) {
            normalizePet(pet);
            if (pet.getId() == petId) {
                return pet;
            }
        }
        return null;
    }

    private boolean consumeItem(GameState state, String type) {
        if (state.getInventory() == null) {
            return false;
        }
        for (InventoryItem item : state.getInventory()) {
            if (type.equals(item.getType()) && item.getQuantity() > 0) {
                item.setQuantity(item.getQuantity() - 1);
                return true;
            }
        }
        return false;
    }

    private void normalizePet(Pet pet) {
        if (pet.getForm() == null || pet.getForm().trim().isEmpty()) {
            pet.setForm(pet.getGrowth() >= 360 && pet.getAffection() >= 70 ? "福瑞" : "幼体");
        }
        if (pet.getGrowth() <= 0) {
            int total = 100 - pet.getHunger() + pet.getMood() + pet.getHealth() + pet.getAffection();
            pet.setGrowth(Math.max(80, Math.min(500, Math.round(total * 1.2f))));
        }
    }

    private void addGrowth(Pet pet, int amount) {
        normalizePet(pet);
        pet.setGrowth(Math.min(500, Math.max(0, pet.getGrowth() + amount)));
    }

    private void evolveIfReady(GameState state, Pet pet) {
        normalizePet(pet);
        if (!"福瑞".equals(pet.getForm()) && pet.getGrowth() >= 360 && pet.getAffection() >= 70) {
            pet.setForm("福瑞");
            pet.setMood(Math.min(100, pet.getMood() + 15));
            pet.setHealth(Math.min(100, pet.getHealth() + 8));
            appendHistory(state, pet.getName() + " 成长为福瑞形态");
            state.setStatusMessage(pet.getName() + " 成长到福瑞形态，外观和能力都提升了。");
        }
    }

    private InventoryItem shopTemplate(String type) {
        if ("food".equals(type)) {
            return item("营养粮", "food", 1, 25);
        }
        if ("toy".equals(type)) {
            return item("玩具球", "toy", 1, 40);
        }
        if ("care".equals(type)) {
            return item("洗护液", "care", 1, 30);
        }
        if ("place".equals(type)) {
            return item("木屋券", "place", 1, 120);
        }
        if ("material".equals(type)) {
            return item("牧草", "material", 1, 18);
        }
        return null;
    }

    private void addInventoryItem(GameState state, InventoryItem boughtItem) {
        if (state.getInventory() == null) {
            state.setInventory(new ArrayList<InventoryItem>());
        }
        for (InventoryItem item : state.getInventory()) {
            if (boughtItem.getType().equals(item.getType())) {
                item.setQuantity(item.getQuantity() + 1);
                return;
            }
        }
        state.getInventory().add(boughtItem);
    }

    private boolean spendIfPossible(GameState state, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (state.getMoney() < amount) {
            return false;
        }
        spend(state, amount);
        return true;
    }

    private void spend(GameState state, int amount) {
        state.setMoney(Math.max(0, state.getMoney() - amount));
        state.setTotalExpense(state.getTotalExpense() + amount);
    }

    private void earn(GameState state, int amount) {
        state.setMoney(state.getMoney() + amount);
        state.setTotalIncome(state.getTotalIncome() + amount);
    }

    private int achievementBit(String achievementKey) {
        if ("adopt".equals(achievementKey)) {
            return 1;
        }
        if ("care".equals(achievementKey)) {
            return 2;
        }
        if ("home".equals(achievementKey)) {
            return 4;
        }
        if ("furry".equals(achievementKey)) {
            return 8;
        }
        return 0;
    }

    private int achievementReward(String achievementKey) {
        if ("adopt".equals(achievementKey)) {
            return 80;
        }
        if ("care".equals(achievementKey)) {
            return 120;
        }
        if ("home".equals(achievementKey)) {
            return 160;
        }
        if ("furry".equals(achievementKey)) {
            return 240;
        }
        return 0;
    }

    private boolean isAchievementReady(GameState state, String achievementKey) {
        if ("adopt".equals(achievementKey)) {
            return state.getPets() != null && state.getPets().size() >= 5;
        }
        if ("care".equals(achievementKey)) {
            int careCount = 0;
            for (String line : state.getHistory()) {
                if (line.contains("喂食") || line.contains("互动") || line.contains("洗护")) {
                    careCount++;
                }
            }
            return careCount >= 3;
        }
        if ("home".equals(achievementKey)) {
            return state.getHomeLevel() >= 4;
        }
        if ("furry".equals(achievementKey)) {
            for (Pet pet : state.getPets()) {
                normalizePet(pet);
                if ("福瑞".equals(pet.getForm())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void appendHistory(GameState state, String entry) {
        state.getHistory().add(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " " + entry);
        while (state.getHistory().size() > 12) {
            state.getHistory().remove(0);
        }
    }

    private void saveState(GameState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            jdbcTemplate.update("update game_state set state_json = ?, updated_at = datetime('now') where id = 1", json);
        } catch (Exception ex) {
            throw new IllegalStateException("无法保存游戏状态", ex);
        }
    }
}

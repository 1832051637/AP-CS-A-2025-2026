package com.petgame.web;

import com.petgame.model.GameState;
import com.petgame.service.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public GameState state() {
        return gameService.loadState();
    }

    @PostMapping("/reset")
    public GameState reset() {
        return gameService.resetGame();
    }

    @PostMapping("/advance")
    public GameState advance() {
        return gameService.advanceTurn();
    }

    @PostMapping("/save")
    public GameState save() {
        return gameService.saveGame();
    }

    @PostMapping("/autosave")
    public GameState autoSave() {
        return gameService.autoSaveGame();
    }

    @PostMapping("/home/upgrade")
    public GameState upgradeHome() {
        return gameService.upgradeHome();
    }

    @PostMapping("/friends/{name}/visit")
    public GameState visitFriend(@PathVariable("name") String name) {
        return gameService.visitFriend(name);
    }

    @PostMapping("/achievements/{key}/claim")
    public GameState claimAchievement(@PathVariable("key") String key) {
        return gameService.claimAchievement(key);
    }

    @PostMapping("/inventory/organize")
    public GameState organizeInventory() {
        return gameService.organizeInventory();
    }

    @PostMapping("/pets/{id}/feed")
    public GameState feed(@PathVariable("id") long id) {
        return gameService.feed(id);
    }

    @PostMapping("/pets/{id}/wash")
    public GameState wash(@PathVariable("id") long id) {
        return gameService.wash(id);
    }

    @PostMapping("/pets/{id}/interact")
    public GameState interact(@PathVariable("id") long id) {
        return gameService.interact(id);
    }

    @PostMapping("/pets/{id}/rename/{name}")
    public GameState rename(@PathVariable("id") long id, @PathVariable("name") String name) {
        return gameService.renamePet(id, name);
    }

    @PostMapping("/pets/{id}/train")
    public GameState train(@PathVariable("id") long id) {
        return gameService.trainPet(id);
    }

    @PostMapping("/pets/{id}/rest")
    public GameState rest(@PathVariable("id") long id) {
        return gameService.restPet(id);
    }

    @PostMapping("/pets/{id}/explore")
    public GameState explore(@PathVariable("id") long id) {
        return gameService.explorePet(id);
    }

    @PostMapping("/adopt")
    public GameState adopt() {
        return gameService.adoptPet();
    }

    @PostMapping("/shop/{type}/buy")
    public GameState buy(@PathVariable("type") String type) {
        return gameService.buyItem(type);
    }

    @PostMapping("/pets/{id}/sell")
    public GameState sell(@PathVariable("id") long id) {
        return gameService.sellPet(id);
    }
}

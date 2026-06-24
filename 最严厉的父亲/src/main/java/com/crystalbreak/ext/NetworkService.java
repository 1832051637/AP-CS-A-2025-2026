package com.crystalbreak.ext;

public interface NetworkService {
    void hostGame(String lobbyName);

    void joinGame(String lobbyId);

    void sendShotEvent(Object event);
}

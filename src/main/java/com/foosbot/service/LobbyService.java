package com.foosbot.service;

import com.foosbot.model.LobbyState;
import com.foosbot.model.Team;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class LobbyService {

    private static final Logger LOG = LoggerFactory.getLogger(LobbyService.class);

    private final ConcurrentHashMap<String, LobbyState> lobbies = new ConcurrentHashMap<>();
    private final GameService gameService;

    public LobbyService(GameService gameService) {
        this.gameService = gameService;
    }

    public boolean isChannelBusy(String channelId) {
        return lobbies.containsKey(channelId) || gameService.hasActiveGame(channelId);
    }

    public boolean hasLobby(String channelId) {
        return lobbies.containsKey(channelId);
    }

    public LobbyState getOrCreateLobby(String channelId) {
        return lobbies.computeIfAbsent(channelId, LobbyState::new);
    }

    public LobbyState getLobby(String channelId) {
        return lobbies.get(channelId);
    }

    public void joinLobby(String channelId, String userId, String displayName) {
        LobbyState lobby = getOrCreateLobby(channelId);
        lobby.addPlayer(userId, displayName);
        LOG.info("Player {} joined lobby in channel {}", displayName, channelId);
    }

    public void switchPositions(String channelId, Team team) {
        LobbyState lobby = lobbies.get(channelId);
        if (lobby != null) {
            lobby.switchPositions(team);
        }
    }

    public void shuffleTeams(String channelId) {
        LobbyState lobby = lobbies.get(channelId);
        if (lobby != null) {
            lobby.shuffleTeams();
        }
    }

    public void toggleReady(String channelId, String userId) {
        LobbyState lobby = lobbies.get(channelId);
        if (lobby != null) {
            LobbyState.LobbyPlayer player = lobby.getPlayer(userId);
            if (player != null) {
                player.setReady(!player.isReady());
            }
        }
    }

    public void cancelLobby(String channelId) {
        lobbies.remove(channelId);
        LOG.info("Lobby cancelled in channel {}", channelId);
    }

    public LobbyState removeLobby(String channelId) {
        return lobbies.remove(channelId);
    }
}

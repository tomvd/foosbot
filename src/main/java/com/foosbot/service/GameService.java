package com.foosbot.service;

import com.foosbot.model.*;
import com.foosbot.repository.GamePlayerRepository;
import com.foosbot.repository.GameRepository;
import com.foosbot.repository.PlayerRepository;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class GameService {

    private static final Logger LOG = LoggerFactory.getLogger(GameService.class);

    private final ConcurrentHashMap<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final PlayerRepository playerRepository;

    public GameService(GameRepository gameRepository,
                       GamePlayerRepository gamePlayerRepository,
                       PlayerRepository playerRepository) {
        this.gameRepository = gameRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.playerRepository = playerRepository;
    }

    public boolean hasActiveGame(String channelId) {
        return activeGames.containsKey(channelId);
    }

    public GameState getActiveGame(String channelId) {
        return activeGames.get(channelId);
    }

    public GameState startGame(LobbyState lobby) {
        String channelId = lobby.getChannelId();

        // Create game in DB
        Game game = gameRepository.create(channelId);
        gameRepository.setStartTime(game.getId(), LocalDateTime.now());

        GameState gameState = new GameState(game.getId(), channelId);

        // Persist players and build game state
        for (LobbyState.LobbyPlayer lp : lobby.getPlayers()) {
            Player player = playerRepository.findOrCreate(lp.getUserId(), lp.getDisplayName());
            gamePlayerRepository.create(game.getId(), player.getId(), lp.getTeam(), lp.getPosition());

            // Get the created game_player record to get its ID
            var gamePlayers = gamePlayerRepository.findByGameId(game.getId());
            var gp = gamePlayers.stream()
                    .filter(g -> g.getPlayerId() == player.getId())
                    .findFirst().orElseThrow();

            gameState.getPlayers().add(new GameState.GamePlayerState(
                    gp.getId(), lp.getUserId(), lp.getDisplayName(),
                    lp.getTeam(), lp.getPosition()));
        }

        activeGames.put(channelId, gameState);
        LOG.info("Game {} started in channel {}", game.getId(), channelId);
        return gameState;
    }

    public void addGoal(String channelId, long gamePlayerId) {
        GameState game = activeGames.get(channelId);
        if (game == null) return;

        GameState.GamePlayerState player = game.getPlayerByGamePlayerId(gamePlayerId);
        if (player != null) {
            player.addGoal();
            gamePlayerRepository.addGoal(gamePlayerId);
        }
    }

    public void gameWon(String channelId) {
        GameState game = activeGames.get(channelId);
        if (game == null) return;

        Team winner = game.getLeadingTeam();
        if (winner == Team.BLUE) {
            game.setBlueWins(game.getBlueWins() + 1);
        } else if (winner == Team.RED) {
            game.setRedWins(game.getRedWins() + 1);
        }
    }

    public GameState completeGame(String channelId) {
        GameState game = activeGames.remove(channelId);
        if (game == null) return null;

        gameRepository.setEndTime(game.getGameId(), LocalDateTime.now());
        gameRepository.updateStatus(game.getGameId(), GameStatus.COMPLETED);
        LOG.info("Game {} completed in channel {}", game.getGameId(), channelId);
        return game;
    }

    public GameState cancelGame(String channelId) {
        GameState game = activeGames.remove(channelId);
        if (game != null) {
            gameRepository.updateStatus(game.getGameId(), GameStatus.CANCELLED);
            LOG.info("Game {} cancelled in channel {}", game.getGameId(), channelId);
        }
        return game;
    }
}

package com.foosbot.slack.handler;

import com.foosbot.model.GameState;
import com.foosbot.model.LobbyState;
import com.foosbot.model.Team;
import com.foosbot.service.GameService;
import com.foosbot.service.LobbyService;
import com.foosbot.slack.view.GameView;
import com.foosbot.slack.view.LobbyView;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LobbyActionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LobbyActionHandler.class);

    private final LobbyService lobbyService;
    private final GameService gameService;

    public LobbyActionHandler(LobbyService lobbyService, GameService gameService) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;
    }

    public Response handleSwitchPositions(BlockActionRequest req, ActionContext ctx) {
        try {
            String channelId = req.getPayload().getChannel().getId();
            String userId = req.getPayload().getUser().getId();
            String actionId = req.getPayload().getActions().get(0).getActionId();

            LobbyState lobby = lobbyService.getLobby(channelId);
            if (lobby == null || !lobby.hasPlayer(userId)) return ctx.ack();

            Team team = actionId.contains("blue") ? Team.BLUE : Team.RED;
            lobbyService.switchPositions(channelId, team);

            updateLobbyMessage(ctx, channelId, lobby);
        } catch (Exception e) {
            LOG.error("Error handling switch positions", e);
        }
        return ctx.ack();
    }

    public Response handleShuffle(BlockActionRequest req, ActionContext ctx) {
        try {
            String channelId = req.getPayload().getChannel().getId();
            String userId = req.getPayload().getUser().getId();

            LobbyState lobby = lobbyService.getLobby(channelId);
            if (lobby == null || !lobby.hasPlayer(userId)) return ctx.ack();

            lobbyService.shuffleTeams(channelId);
            updateLobbyMessage(ctx, channelId, lobby);
        } catch (Exception e) {
            LOG.error("Error handling shuffle", e);
        }
        return ctx.ack();
    }

    public Response handleCancel(BlockActionRequest req, ActionContext ctx) {
        try {
            String channelId = req.getPayload().getChannel().getId();
            String userId = req.getPayload().getUser().getId();

            LobbyState lobby = lobbyService.getLobby(channelId);
            if (lobby == null || !lobby.hasPlayer(userId)) return ctx.ack();

            String messageTs = lobby.getMessageTs();
            lobbyService.cancelLobby(channelId);

            if (messageTs != null) {
                ctx.client().chatDelete(r -> r.channel(channelId).ts(messageTs));
            }

            ctx.client().chatPostMessage(r -> r
                    .channel(channelId)
                    .text(":x: Game cancelled."));
        } catch (Exception e) {
            LOG.error("Error handling cancel", e);
        }
        return ctx.ack();
    }

    public Response handleReady(BlockActionRequest req, ActionContext ctx) {
        try {
            String channelId = req.getPayload().getChannel().getId();
            String userId = req.getPayload().getUser().getId();
            String actionId = req.getPayload().getActions().get(0).getActionId();

            String targetUserId = actionId.replace("lobby_ready_", "");

            // Only the player themselves can toggle their ready status
            if (!userId.equals(targetUserId)) return ctx.ack();

            LobbyState lobby = lobbyService.getLobby(channelId);
            if (lobby == null || !lobby.hasPlayer(userId)) return ctx.ack();

            lobbyService.toggleReady(channelId, userId);

            // Check if all players are ready - auto-start the game
            if (lobby.allReady()) {
                return startGame(ctx, channelId);
            }

            updateLobbyMessage(ctx, channelId, lobby);
        } catch (Exception e) {
            LOG.error("Error handling ready", e);
        }
        return ctx.ack();
    }

    private Response startGame(ActionContext ctx, String channelId) throws Exception {
        LobbyState lobby = lobbyService.removeLobby(channelId);
        if (lobby == null) return ctx.ack();

        // Delete lobby message
        if (lobby.getMessageTs() != null) {
            ctx.client().chatDelete(r -> r.channel(channelId).ts(lobby.getMessageTs()));
        }

        // Start the game
        GameState gameState = gameService.startGame(lobby);

        // Post game message
        ChatPostMessageResponse response = ctx.client().chatPostMessage(r -> r
                .channel(channelId)
                .text(GameView.getText(gameState))
                .attachments(GameView.build(gameState)));

        if (response.isOk()) {
            gameState.setMessageTs(response.getTs());
        }

        LOG.info("Game started in channel {}", channelId);
        return ctx.ack();
    }

    private void updateLobbyMessage(ActionContext ctx, String channelId, LobbyState lobby) throws Exception {
        if (lobby.getMessageTs() == null) return;

        int needed = 4 - lobby.getPlayers().size();
        String text;
        if (needed > 0) {
            text = String.format(":soccer: %d more needed to play!", needed);
        } else {
            text = ":soccer: The lobby is full! Everyone hit Ready!";
        }

        ctx.client().chatUpdate(r -> r
                .channel(channelId)
                .ts(lobby.getMessageTs())
                .text(text)
                .attachments(LobbyView.build(lobby)));
    }
}

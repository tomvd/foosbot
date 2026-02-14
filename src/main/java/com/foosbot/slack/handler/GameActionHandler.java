package com.foosbot.slack.handler;

import com.foosbot.model.GameState;
import com.foosbot.service.GameService;
import com.foosbot.slack.view.GameView;
import com.foosbot.slack.view.ScoreboardView;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.bolt.response.Response;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class GameActionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GameActionHandler.class);

    private final GameService gameService;

    public GameActionHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public Response handleGoal(BlockActionRequest req, ActionContext ctx) {
        try {
            String channelId = req.getPayload().getChannel().getId();
            String userId = req.getPayload().getUser().getId();
            String actionId = req.getPayload().getActions().get(0).getActionId();

            GameState game = gameService.getActiveGame(channelId);
            if (game == null) return ctx.ack();

            // Only participants can click
            if (!game.isParticipant(userId)) return ctx.ack();

            long gamePlayerId = Long.parseLong(actionId.replace("game_goal_", ""));
            gameService.addGoal(channelId, gamePlayerId);

            updateGameMessage(ctx, channelId, game);
        } catch (Exception e) {
            LOG.error("Error handling goal", e);
        }
        return ctx.ack();
    }

    public Response handleGameWon(BlockActionRequest req, ActionContext ctx) {
        try {
            String channelId = req.getPayload().getChannel().getId();
            String userId = req.getPayload().getUser().getId();

            GameState game = gameService.getActiveGame(channelId);
            if (game == null) return ctx.ack();
            if (!game.isParticipant(userId)) return ctx.ack();

            // Validate: game must be winnable (11+ goals, 2+ lead)
            if (!game.isGameWinnable()) {
                LOG.warn("Game won attempted but conditions not met in channel {}", channelId);
                return ctx.ack();
            }

            // Record the set win
            gameService.gameWon(channelId);

            // For V1, game won = match over
            return finishGame(ctx, channelId);
        } catch (Exception e) {
            LOG.error("Error handling game won", e);
        }
        return ctx.ack();
    }

    public Response handleMatchOver(BlockActionRequest req, ActionContext ctx) {
        try {
            String channelId = req.getPayload().getChannel().getId();
            String userId = req.getPayload().getUser().getId();

            GameState game = gameService.getActiveGame(channelId);
            if (game == null) return ctx.ack();
            if (!game.isParticipant(userId)) return ctx.ack();

            return finishGame(ctx, channelId);
        } catch (Exception e) {
            LOG.error("Error handling match over", e);
        }
        return ctx.ack();
    }

    public Response handleCancelGame(BlockActionRequest req, ActionContext ctx) {
        try {
            String channelId = req.getPayload().getChannel().getId();

            GameState game = gameService.cancelGame(channelId);
            if (game == null) return ctx.ack();

            if (game.getMessageTs() != null) {
                ctx.client().chatDelete(r -> r.channel(channelId).ts(game.getMessageTs()));
            }

            ctx.client().chatPostMessage(r -> r
                    .channel(channelId)
                    .text(":x: Game cancelled."));
        } catch (Exception e) {
            LOG.error("Error handling cancel game", e);
        }
        return ctx.ack();
    }

    private Response finishGame(ActionContext ctx, String channelId) throws Exception {
        GameState game = gameService.getActiveGame(channelId);
        if (game == null) return ctx.ack();

        // Delete the game message
        if (game.getMessageTs() != null) {
            ctx.client().chatDelete(r -> r.channel(channelId).ts(game.getMessageTs()));
        }

        // Complete the game in DB
        GameState completedGame = gameService.completeGame(channelId);
        if (completedGame == null) return ctx.ack();

        // Post scoreboard
        ctx.client().chatPostMessage(r -> r
                .channel(channelId)
                .text(":trophy: Game Over!")
                .attachments(ScoreboardView.build(completedGame)));

        LOG.info("Game finished and scoreboard posted in channel {}", channelId);
        return ctx.ack();
    }

    private void updateGameMessage(ActionContext ctx, String channelId, GameState game) throws Exception {
        if (game.getMessageTs() == null) return;

        ctx.client().chatUpdate(r -> r
                .channel(channelId)
                .ts(game.getMessageTs())
                .text(GameView.getText(game))
                .attachments(GameView.build(game)));
    }
}

package com.baboon.slack.handler;

import com.baboon.model.LobbyState;
import com.baboon.service.GameService;
import com.baboon.service.LobbyService;
import com.baboon.service.StatsService;
import com.baboon.slack.view.BusyView;
import com.baboon.slack.view.LobbyView;
import com.baboon.slack.view.StatsView;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.event.AppMentionEvent;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class AppMentionHandler implements BoltEventHandler<AppMentionEvent> {

    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("<@([A-Z0-9]+)>");

    private static final Logger LOG = LoggerFactory.getLogger(AppMentionHandler.class);

    private final LobbyService lobbyService;
    private final GameService gameService;
    private final StatsService statsService;

    public AppMentionHandler(LobbyService lobbyService, GameService gameService, StatsService statsService) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;
        this.statsService = statsService;
    }

    @Override
    public Response apply(EventsApiPayload<AppMentionEvent> payload, EventContext ctx) {
        try {
            AppMentionEvent event = payload.getEvent();
            String text = event.getText().toLowerCase();
            String channelId = event.getChannel();
            String userId = event.getUser();

            LOG.info("Received mention from {} in {}: {}", userId, channelId, text);

            if (text.contains("stats reset")) {
                return handleStatsReset(ctx, channelId);
            }

            if (text.contains("stats")) {
                boolean allTime = text.contains("all time") || text.contains("alltime") || text.contains("stats all");
                return handleStats(ctx, channelId, allTime);
            }

            if (text.contains("play")) {
                return handlePlay(ctx, channelId, userId);
            }

            if (text.contains("add")) {
                return handleAdd(ctx, channelId, userId, event.getText());
            }

            // Default: show help
            ctx.say("Hi! I'm baboon :monkey_face:\n" +
                    "*Commands:*\n" +
                    "\u2022 `@baboon play` - Start or join a foosball game\n" +
                    "\u2022 `@baboon add @player` - Add another player to the lobby\n" +
                    "\u2022 `@baboon stats` - Show weekly stats\n" +
                    "\u2022 `@baboon stats all` - Show all time stats\n" +
                    "\u2022 `@baboon stats reset` - Clear all stats\n\n" +
                    "*Stats:*\n" +
                    "\u2022 *Rankings* - Win%, wins, games played and total goals per player\n" +
                    "\u2022 *Top Scorers* - Goals and goals per game (as forward)\n" +
                    "\u2022 *Goalies* - Goals let in and goals let in per game (as goalie)");
        } catch (Exception e) {
            LOG.error("Error handling app mention", e);
        }
        return ctx.ack();
    }

    private Response handlePlay(EventContext ctx, String channelId, String userId)
            throws IOException, SlackApiException {
        // Check if there's an active game
        if (gameService.hasActiveGame(channelId)) {
            ctx.client().chatPostMessage(r -> r
                    .channel(channelId)
                    .text(BusyView.getText())
                    .attachments(BusyView.build()));
            return ctx.ack();
        }

        // Get user info for display name
        String displayName = getUserDisplayName(ctx, userId);

        // Join or create lobby
        lobbyService.joinLobby(channelId, userId, displayName);
        var lobby = lobbyService.getLobby(channelId);

        if (lobby.getMessageTs() == null) {
            // New lobby - post a new message
            int needed = 4 - lobby.getPlayers().size();
            String announcement = String.format(":soccer: %s wants to play foosball! %d more needed.", displayName, needed);

            ChatPostMessageResponse response = ctx.client().chatPostMessage(r -> r
                    .channel(channelId)
                    .text(announcement)
                    .attachments(LobbyView.build(lobby)));

            if (response.isOk()) {
                lobby.setMessageTs(response.getTs());
            }
        } else {
            // Existing lobby - update the message
            int needed = 4 - lobby.getPlayers().size();
            String text;
            if (needed > 0) {
                text = String.format(":soccer: %s wants to join the game. %d more needed.", displayName, needed);
            } else {
                text = ":soccer: The lobby is full! Hit *Start Game* when ready!";
            }

            ctx.client().chatUpdate(r -> r
                    .channel(channelId)
                    .ts(lobby.getMessageTs())
                    .text(text)
                    .attachments(LobbyView.build(lobby)));
        }

        return ctx.ack();
    }

    private Response handleAdd(EventContext ctx, String channelId, String requestingUserId, String rawText)
            throws IOException, SlackApiException {
        if (!lobbyService.hasLobby(channelId)) {
            ctx.say("There's no active lobby to add players to. Use `@baboon play` to start one.");
            return ctx.ack();
        }

        // Find all mentioned user IDs, skip the bot itself (first mention)
        Matcher matcher = USER_MENTION_PATTERN.matcher(rawText);
        String botId = null;
        String targetUserId = null;
        while (matcher.find()) {
            String id = matcher.group(1);
            if (botId == null) {
                botId = id; // first mention is always the bot
            } else {
                targetUserId = id;
                break;
            }
        }

        if (targetUserId == null) {
            ctx.say("Please mention a player to add, e.g. `@baboon add @player`.");
            return ctx.ack();
        }

        var lobby = lobbyService.getLobby(channelId);
        if (lobby.hasPlayer(targetUserId)) {
            ctx.say("That player is already in the lobby.");
            return ctx.ack();
        }

        if (lobby.isFull()) {
            ctx.say("The lobby is already full.");
            return ctx.ack();
        }

        String displayName = getUserDisplayName(ctx, targetUserId);
        lobbyService.joinLobby(channelId, targetUserId, displayName);
        LobbyState updatedLobby = lobbyService.getLobby(channelId);

        int needed = 4 - updatedLobby.getPlayers().size();
        String text = needed > 0
                ? String.format(":soccer: %s was added to the lobby. %d more needed.", displayName, needed)
                : ":soccer: The lobby is full! Hit *Start Game* when ready!";

        ctx.client().chatUpdate(r -> r
                .channel(channelId)
                .ts(updatedLobby.getMessageTs())
                .text(text)
                .attachments(LobbyView.build(updatedLobby)));

        return ctx.ack();
    }

    private Response handleStats(EventContext ctx, String channelId, boolean allTime)
            throws IOException, SlackApiException {
        var stats = allTime ? statsService.getAllTimeStats() : statsService.getWeeklyStats();
        String header = allTime
                ? ":bar_chart: *Stats All Time* :bar_chart:"
                : ":bar_chart: *Stats This Week* :bar_chart:";
        ctx.client().chatPostMessage(r -> r
                .channel(channelId)
                .text(header)
                .attachments(StatsView.build(stats)));
        return ctx.ack();
    }

    private Response handleStatsReset(EventContext ctx, String channelId)
            throws IOException, SlackApiException {
        statsService.resetAll();
        ctx.client().chatPostMessage(r -> r
                .channel(channelId)
                .text(":wastebasket: All stats have been reset."));
        return ctx.ack();
    }

    private String getUserDisplayName(EventContext ctx, String userId) {
        try {
            var userInfo = ctx.client().usersInfo(r -> r.user(userId));
            if (userInfo.isOk()) {
                String displayName = userInfo.getUser().getProfile().getDisplayName();
                if (displayName == null || displayName.isBlank()) {
                    displayName = userInfo.getUser().getName();
                }
                return displayName;
            }
        } catch (Exception e) {
            LOG.warn("Failed to get user info for {}", userId, e);
        }
        return userId;
    }
}

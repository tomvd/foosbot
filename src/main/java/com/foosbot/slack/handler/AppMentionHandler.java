package com.foosbot.slack.handler;

import com.foosbot.service.GameService;
import com.foosbot.service.LobbyService;
import com.foosbot.service.StatsService;
import com.foosbot.slack.view.BusyView;
import com.foosbot.slack.view.LobbyView;
import com.foosbot.slack.view.StatsView;
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

@Singleton
public class AppMentionHandler implements BoltEventHandler<AppMentionEvent> {

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

            // Default: show help
            ctx.say("Hi! I'm foosbot :soccer:\n" +
                    "*Commands:*\n" +
                    "\u2022 `@foosbot play` - Start or join a foosball game\n" +
                    "\u2022 `@foosbot stats` - Show weekly stats\n" +
                    "\u2022 `@foosbot stats all` - Show all time stats\n" +
                    "\u2022 `@foosbot stats reset` - Clear all stats\n\n" +
                    "*Stats Glossary:*\n" +
                    "\u2022 *GPG* - Goals Per Game\n" +
                    "\u2022 *GWG* - Game Winning Goals\n" +
                    "\u2022 *+/-* - Plus/minus differential (team goals scored vs conceded)\n" +
                    "\u2022 *G* - Goals\n" +
                    "\u2022 *GP* - Games Played\n" +
                    "\u2022 *MP* - Matches Played\n" +
                    "\u2022 *MWin%* - Match Win Percentage\n" +
                    "\u2022 *GAA* - Goals Against Average\n" +
                    "\u2022 *SO* - Shutouts (no goals conceded)");
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
                text = ":soccer: The lobby is full! Everyone hit Ready!";
            }

            ctx.client().chatUpdate(r -> r
                    .channel(channelId)
                    .ts(lobby.getMessageTs())
                    .text(text)
                    .attachments(LobbyView.build(lobby)));
        }

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

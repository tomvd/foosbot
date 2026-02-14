package com.foosbot.slack;

import com.foosbot.config.SlackConfig;
import com.foosbot.slack.handler.AppMentionHandler;
import com.foosbot.slack.handler.GameActionHandler;
import com.foosbot.slack.handler.LobbyActionHandler;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.AppMentionEvent;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationStartupEvent;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

@Factory
public class SlackAppFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SlackAppFactory.class);

    @Bean
    @Singleton
    public App slackApp(SlackConfig config,
                        AppMentionHandler mentionHandler,
                        LobbyActionHandler lobbyActionHandler,
                        GameActionHandler gameActionHandler) {
        AppConfig appConfig = AppConfig.builder()
                .singleTeamBotToken(config.getBotToken())
                .signingSecret(config.getSigningSecret())
                .build();

        App app = new App(appConfig);

        // Register event handler
        app.event(AppMentionEvent.class, mentionHandler);

        // Register lobby action handlers
        app.blockAction("lobby_switch_blue", (req, ctx) -> lobbyActionHandler.handleSwitchPositions(req, ctx));
        app.blockAction("lobby_switch_red", (req, ctx) -> lobbyActionHandler.handleSwitchPositions(req, ctx));
        app.blockAction("lobby_shuffle", (req, ctx) -> lobbyActionHandler.handleShuffle(req, ctx));
        app.blockAction("lobby_cancel", (req, ctx) -> lobbyActionHandler.handleCancel(req, ctx));
        app.blockAction(Pattern.compile("lobby_ready_.+"), (req, ctx) -> lobbyActionHandler.handleReady(req, ctx));

        // Register game action handlers
        app.blockAction(Pattern.compile("game_goal_.+"), (req, ctx) -> gameActionHandler.handleGoal(req, ctx));
        app.blockAction("game_won", (req, ctx) -> gameActionHandler.handleGameWon(req, ctx));
        app.blockAction("game_match_over", (req, ctx) -> gameActionHandler.handleMatchOver(req, ctx));
        app.blockAction("game_cancel", (req, ctx) -> gameActionHandler.handleCancelGame(req, ctx));
        app.blockAction("game_more", (req, ctx) -> ctx.ack()); // No-op for now

        return app;
    }

    @Bean
    @Singleton
    public ApplicationEventListener<ApplicationStartupEvent> slackStartupListener(App app, SlackConfig config) {
        return event -> {
            try {
                LOG.info("Starting Slack Socket Mode connection...");
                SocketModeApp socketModeApp = new SocketModeApp(config.getAppToken(), app);
                socketModeApp.startAsync();
                LOG.info("Slack bot connected successfully via Socket Mode!");
            } catch (Exception e) {
                LOG.error("Failed to start Slack Socket Mode", e);
                throw new RuntimeException("Failed to start Slack bot", e);
            }
        };
    }
}

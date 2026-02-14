package com.foosbot.slack.view;

import com.foosbot.model.GameState;
import com.foosbot.model.Position;
import com.foosbot.model.Team;
import com.slack.api.model.Attachment;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ScoreboardView {

    private static final String[] WINNER_EMOJIS = {":sunglasses:", ":grin:", ":muscle:", ":star-struck:", ":fire:"};
    private static final String[] LOSER_EMOJIS = {":neutral_face:", ":money_mouth_face:", ":sweat_smile:", ":grimacing:", ":pensive:"};

    public static List<Attachment> build(GameState game) {
        List<Attachment> attachments = new ArrayList<>();

        Team winningTeam = game.getLeadingTeam();
        String winTeamName = winningTeam == Team.BLUE ? "BLUE" : "RED";
        int totalBlue = game.getBlueScore();
        int totalRed = game.getRedScore();

        // Winner players
        var winners = game.getTeam(winningTeam);
        String winnerNames = winners.stream()
                .map(GameState.GamePlayerState::getDisplayName)
                .reduce((a, b) -> a + " and " + b)
                .orElse("");

        // Trophy header
        String headerText = String.format(":trophy: %s TEAM ARE THE CHAMPIONS! :trophy:\n" +
                "Everyone worship your new winners %s! :clap:", winTeamName, winnerNames);

        attachments.add(Attachment.builder()
                .color(winningTeam == Team.BLUE ? "#0000ff" : "#ff0000")
                .blocks(List.<LayoutBlock>of(
                        SectionBlock.builder()
                                .text(MarkdownTextObject.builder().text(headerText).build())
                                .build()
                ))
                .build());

        // Final score
        String scoreText = String.format("*Final Score: %d-%d*", totalBlue, totalRed);
        attachments.add(Attachment.builder()
                .color("#cccccc")
                .blocks(List.<LayoutBlock>of(
                        SectionBlock.builder()
                                .text(MarkdownTextObject.builder().text(scoreText).build())
                                .build()
                ))
                .build());

        // Winning team players
        attachments.add(buildTeamScoreboard(game, winningTeam, true));

        // Losing team players
        Team losingTeam = winningTeam == Team.BLUE ? Team.RED : Team.BLUE;
        attachments.add(buildTeamScoreboard(game, losingTeam, false));

        // Duration
        Duration duration = Duration.between(game.getStartTime(), Instant.now());
        long totalSeconds = duration.getSeconds();
        String durationStr;
        if (totalSeconds < 60) {
            durationStr = totalSeconds + " seconds";
        } else {
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            durationStr = seconds > 0
                    ? String.format("%d minutes and %d seconds", minutes, seconds)
                    : String.format("%d minutes", minutes);
        }
        String durationText = String.format(":soccer: Game lasted *%s*.", durationStr);

        attachments.add(Attachment.builder()
                .color("#cccccc")
                .blocks(List.<LayoutBlock>of(
                        SectionBlock.builder()
                                .text(MarkdownTextObject.builder().text(durationText).build())
                                .build()
                ))
                .build());

        return attachments;
    }

    private static Attachment buildTeamScoreboard(GameState game, Team team, boolean isWinner) {
        String color = team == Team.BLUE ? "#0000ff" : "#ff0000";
        String[] emojis = isWinner ? WINNER_EMOJIS : LOSER_EMOJIS;

        StringBuilder text = new StringBuilder();
        var teamPlayers = game.getTeam(team);
        int emojiIdx = 0;
        for (var player : teamPlayers) {
            String posLabel = player.getPosition() == Position.GOALIE ? "G" : "F";
            String emoji = emojis[emojiIdx % emojis.length];
            text.append(String.format("[%s] %s: *%d goals* %s\n",
                    posLabel, player.getDisplayName(), player.getGoals(), emoji));
            emojiIdx++;
        }

        return Attachment.builder()
                .color(color)
                .blocks(List.<LayoutBlock>of(
                        SectionBlock.builder()
                                .text(MarkdownTextObject.builder().text(text.toString().trim()).build())
                                .build()
                ))
                .build();
    }
}

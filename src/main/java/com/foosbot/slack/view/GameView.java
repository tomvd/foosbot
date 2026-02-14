package com.foosbot.slack.view;

import com.foosbot.model.GameState;
import com.foosbot.model.Position;
import com.foosbot.model.Team;
import com.slack.api.model.Attachment;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;

import java.util.ArrayList;
import java.util.List;

public class GameView {

    public static String getText(GameState game) {
        return ":monkey_face:";
    }

    public static List<Attachment> build(GameState game) {
        List<Attachment> attachments = new ArrayList<>();

        // Wins tracker
        attachments.add(buildWinsAttachment(game));
        // Blue team
        attachments.add(buildTeamAttachment(game, Team.BLUE, "#0000ff"));
        // Red team
        attachments.add(buildTeamAttachment(game, Team.RED, "#ff0000"));
        // Game actions
        attachments.add(buildGameActionsAttachment(game));

        return attachments;
    }

    private static Attachment buildWinsAttachment(GameState game) {
        String winsText = String.format("*Wins:*    :large_blue_circle: %d   :blue_book: %d   :red_circle: %d   :closed_book: %d",
                game.getBlueWins(), game.getBlueWins(),
                game.getRedWins(), game.getRedWins());

        // Simplified: show blue/red wins
        winsText = String.format("*Wins:*    :large_blue_circle: %d   :red_circle: %d",
                game.getBlueWins(), game.getRedWins());

        return Attachment.builder()
                .color("#cccccc")
                .blocks(List.<LayoutBlock>of(
                        SectionBlock.builder()
                                .text(MarkdownTextObject.builder().text(winsText).build())
                                .build()
                ))
                .build();
    }

    private static Attachment buildTeamAttachment(GameState game, Team team, String color) {
        List<GameState.GamePlayerState> teamPlayers = game.getTeam(team);
        int teamScore = teamPlayers.stream().mapToInt(GameState.GamePlayerState::getGoals).sum();

        String teamName = team == Team.BLUE ? "Blue" : "Red";

        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(SectionBlock.builder()
                .text(MarkdownTextObject.builder()
                        .text(String.format("*%s:    %d*", teamName, teamScore))
                        .build())
                .build());

        // Player goal buttons
        for (GameState.GamePlayerState player : teamPlayers) {
            String posLabel = player.getPosition() == Position.GOALIE ? "G" : "F";
            String buttonText = String.format("[%s] %s: %d", posLabel, player.getDisplayName(), player.getGoals());

            blocks.add(ActionsBlock.builder()
                    .elements(List.<BlockElement>of(
                            ButtonElement.builder()
                                    .text(PlainTextObject.builder().text(buttonText).build())
                                    .actionId("game_goal_" + player.getGamePlayerId())
                                    .build()
                    ))
                    .build());
        }

        return Attachment.builder()
                .color(color)
                .blocks(blocks)
                .build();
    }

    private static Attachment buildGameActionsAttachment(GameState game) {
        List<BlockElement> buttons = new ArrayList<>();

        // Game Won button - only enabled when winnable
        ButtonElement gameWonBtn = ButtonElement.builder()
                .text(PlainTextObject.builder().text("Game Won").build())
                .actionId("game_won")
                .style("primary")
                .build();
        buttons.add(gameWonBtn);

        buttons.add(ButtonElement.builder()
                .text(PlainTextObject.builder().text("Match Over").build())
                .actionId("game_match_over")
                .build());

        buttons.add(ButtonElement.builder()
                .text(PlainTextObject.builder().text("More...").build())
                .actionId("game_more")
                .build());

        return Attachment.builder()
                .color("#cccccc")
                .blocks(List.<LayoutBlock>of(
                        ActionsBlock.builder()
                                .elements(buttons)
                                .build()
                ))
                .build();
    }
}

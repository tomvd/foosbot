package com.baboon.slack.view;

import com.baboon.model.GameState;
import com.baboon.model.Position;
import com.baboon.model.Team;
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

        // Blue team
        attachments.add(buildTeamAttachment(game, Team.BLUE, "#0000ff"));
        // Red team
        attachments.add(buildTeamAttachment(game, Team.RED, "#ff0000"));
        // Game actions
        attachments.add(buildGameActionsAttachment(game));

        return attachments;
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
                .fallback("Foosball game")
                .color(color)
                .blocks(blocks)
                .build();
    }

    private static Attachment buildGameActionsAttachment(GameState game) {
        List<BlockElement> buttons = new ArrayList<>();

        buttons.add(ButtonElement.builder()
                .text(PlainTextObject.builder().text("End Game").build())
                .actionId("game_end")
                .style("primary")
                .build());

        return Attachment.builder()
                .fallback("Foosball game actions")
                .color("#cccccc")
                .blocks(List.<LayoutBlock>of(
                        ActionsBlock.builder()
                                .elements(buttons)
                                .build()
                ))
                .build();
    }
}

package com.baboon.slack.view;

import com.baboon.model.LobbyState;
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

public class LobbyView {

    public static List<Attachment> build(LobbyState lobby) {
        List<Attachment> attachments = new ArrayList<>();

        // Blue team attachment
        attachments.add(buildTeamAttachment(lobby, Team.BLUE, "#0000ff"));
        // Red team attachment
        attachments.add(buildTeamAttachment(lobby, Team.RED, "#ff0000"));
        // Actions attachment (shuffle, cancel, start)
        attachments.add(buildActionsAttachment(lobby));

        return attachments;
    }

    private static Attachment buildTeamAttachment(LobbyState lobby, Team team, String color) {
        List<LobbyState.LobbyPlayer> teamPlayers = lobby.getTeam(team);

        StringBuilder text = new StringBuilder();
        for (LobbyState.LobbyPlayer p : teamPlayers) {
            String posLabel = p.getPosition() == Position.GOALIE ? "G" : "F";
            text.append(posLabel).append(" - ").append(p.getDisplayName()).append("\n");
        }

        // Add empty slots
        int slots = 2 - teamPlayers.size();
        for (int i = 0; i < slots; i++) {
            text.append("_waiting for player..._\n");
        }

        String actionId = team == Team.BLUE ? "lobby_switch_blue" : "lobby_switch_red";

        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(SectionBlock.builder()
                .text(MarkdownTextObject.builder().text(text.toString().trim()).build())
                .build());

        if (teamPlayers.size() == 2) {
            blocks.add(ActionsBlock.builder()
                    .elements(List.<BlockElement>of(
                            ButtonElement.builder()
                                    .text(PlainTextObject.builder().text("Switch Positions").build())
                                    .actionId(actionId)
                                    .build()
                    ))
                    .build());
        }

        return Attachment.builder()
                .fallback("Foosball lobby")
                .color(color)
                .blocks(blocks)
                .build();
    }

    private static Attachment buildActionsAttachment(LobbyState lobby) {
        List<BlockElement> buttons = new ArrayList<>();

        if (lobby.isFull()) {
            buttons.add(ButtonElement.builder()
                    .text(PlainTextObject.builder().text("Start Game").build())
                    .actionId("lobby_start")
                    .style("primary")
                    .build());
        }

        buttons.add(ButtonElement.builder()
                .text(PlainTextObject.builder().text("Shuffle Teams").build())
                .actionId("lobby_shuffle")
                .build());

        buttons.add(ButtonElement.builder()
                .text(PlainTextObject.builder().text("Cancel").build())
                .actionId("lobby_cancel")
                .style("danger")
                .build());

        return Attachment.builder()
                .fallback("Foosball lobby")
                .color("#cccccc")
                .blocks(List.of(
                        ActionsBlock.builder()
                                .elements(buttons)
                                .build()
                ))
                .build();
    }
}

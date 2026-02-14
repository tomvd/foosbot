package com.foosbot.slack.view;

import com.slack.api.model.Attachment;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;

import java.util.List;

public class BusyView {

    public static String getText() {
        return "Sorry I'm busy monitoring a game right now. I can still show you stats if you'd like.";
    }

    public static List<Attachment> build() {
        return List.of(
                Attachment.builder()
                        .color("#cccccc")
                        .blocks(List.<LayoutBlock>of(
                                SectionBlock.builder()
                                        .text(MarkdownTextObject.builder()
                                                .text("Feel free to cancel the current game!")
                                                .build())
                                        .build(),
                                ActionsBlock.builder()
                                        .elements(List.<BlockElement>of(
                                                ButtonElement.builder()
                                                        .text(PlainTextObject.builder().text("Cancel Game").build())
                                                        .actionId("game_cancel")
                                                        .style("danger")
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build()
        );
    }
}

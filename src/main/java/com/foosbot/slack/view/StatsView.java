package com.foosbot.slack.view;

import com.slack.api.model.Attachment;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatsView {

    public static List<Attachment> build(Map<String, List<Map<String, Object>>> allStats) {
        List<Attachment> attachments = new ArrayList<>();

        for (var entry : allStats.entrySet()) {
            String title = entry.getKey();
            List<Map<String, Object>> rows = entry.getValue();

            String table = buildTable(title, rows);

            attachments.add(Attachment.builder()
                    .color("#cccccc")
                    .blocks(List.<LayoutBlock>of(
                            SectionBlock.builder()
                                    .text(MarkdownTextObject.builder().text(table).build())
                                    .build()
                    ))
                    .build());
        }

        return attachments;
    }

    private static String buildTable(String title, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return String.format("*%s*\n_No data available_", title);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("*%s*\n```\n", title));

        // Header
        List<String> columns = new ArrayList<>(rows.get(0).keySet());

        // Column widths
        int[] widths = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = formatColumnName(columns.get(i)).length();
        }
        for (var row : rows) {
            int i = 0;
            for (var col : columns) {
                String val = formatValue(row.get(col));
                widths[i] = Math.max(widths[i], val.length());
                i++;
            }
        }

        // Header row
        sb.append("| ");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(padRight(formatColumnName(columns.get(i)), widths[i]));
            if (i < columns.size() - 1) sb.append(" | ");
        }
        sb.append(" |\n");

        // Separator
        sb.append("|");
        for (int i = 0; i < columns.size(); i++) {
            sb.append("-".repeat(widths[i] + 2));
            sb.append("|");
        }
        sb.append("\n");

        // Data rows
        for (var row : rows) {
            sb.append("| ");
            int i = 0;
            for (var col : columns) {
                String val = formatValue(row.get(col));
                sb.append(padRight(val, widths[i]));
                if (i < columns.size() - 1) sb.append(" | ");
                i++;
            }
            sb.append(" |\n");
        }

        sb.append("```");
        return sb.toString();
    }

    private static String formatColumnName(String name) {
        return switch (name) {
            case "display_name" -> "Player";
            case "gp" -> "GP";
            case "goals" -> "G";
            case "gpg" -> "GPG";
            case "gaa" -> "GAA";
            case "plus_minus" -> "+/-";
            case "gwg" -> "GWG";
            case "shutouts" -> "SO";
            case "mp" -> "MP";
            case "mwin_pct" -> "MWin%";
            default -> name;
        };
    }

    private static String formatValue(Object val) {
        if (val == null) return "-";
        if (val instanceof Double d) {
            if (d == Math.floor(d)) return String.valueOf((int) d.doubleValue());
            return String.valueOf(d);
        }
        return String.valueOf(val);
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }
}

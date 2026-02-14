package com.foosbot.repository;

import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class StatsRepository {

    private final DataSource dataSource;

    public StatsRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Player Stats: GPG, GWG, +/-, G, GP, MP, MWin%
     */
    public List<Map<String, Object>> getPlayerStats(boolean allTime) {
        String timeFilter = allTime ? "" : "AND g.start_time >= datetime('now', '-7 days')";
        String sql = String.format("""
                SELECT p.display_name,
                       ROUND(CAST(SUM(gp.goals) AS REAL) / MAX(COUNT(DISTINCT gp.game_id), 1), 2) as gpg,
                       SUM(CASE WHEN g.status = 'COMPLETED'
                           AND ((gp.team = 'BLUE' AND
                                 (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                                 > (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED'))
                            OR (gp.team = 'RED' AND
                                 (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                                 > (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')))
                           THEN 1 ELSE 0 END) as gwg,
                       SUM(CASE WHEN g.status = 'COMPLETED' THEN
                           CASE WHEN gp.team = 'BLUE' THEN
                               (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                               - (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                           ELSE
                               (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                               - (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                           END
                       ELSE 0 END) as plus_minus,
                       SUM(gp.goals) as goals,
                       COUNT(DISTINCT gp.game_id) as gp,
                       COUNT(DISTINCT gp.game_id) as mp,
                       ROUND(100.0 * SUM(CASE WHEN g.status = 'COMPLETED'
                           AND ((gp.team = 'BLUE' AND
                                 (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                                 > (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED'))
                            OR (gp.team = 'RED' AND
                                 (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                                 > (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')))
                           THEN 1 ELSE 0 END) / MAX(COUNT(DISTINCT gp.game_id), 1)) as mwin_pct
                FROM game_players gp
                JOIN games g ON gp.game_id = g.id
                JOIN players p ON gp.player_id = p.id
                WHERE g.status = 'COMPLETED'
                  %s
                GROUP BY p.id
                ORDER BY gpg DESC
                """, timeFilter);
        return executeQuery(sql);
    }

    /**
     * Forward Stats: GPG, GAA, G, GP, +/-
     */
    public List<Map<String, Object>> getForwardStats(boolean allTime) {
        String timeFilter = allTime ? "" : "AND g.start_time >= datetime('now', '-7 days')";
        String sql = String.format("""
                SELECT p.display_name,
                       ROUND(CAST(SUM(gp.goals) AS REAL) / MAX(COUNT(DISTINCT gp.game_id), 1), 2) as gpg,
                       ROUND(CAST(SUM(CASE WHEN gp.team = 'BLUE' THEN
                           (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                       ELSE
                           (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                       END) AS REAL) / MAX(COUNT(DISTINCT gp.game_id), 1), 2) as gaa,
                       SUM(gp.goals) as goals,
                       COUNT(DISTINCT gp.game_id) as gp,
                       SUM(CASE WHEN g.status = 'COMPLETED' THEN
                           CASE WHEN gp.team = 'BLUE' THEN
                               (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                               - (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                           ELSE
                               (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                               - (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                           END
                       ELSE 0 END) as plus_minus
                FROM game_players gp
                JOIN games g ON gp.game_id = g.id
                JOIN players p ON gp.player_id = p.id
                WHERE g.status = 'COMPLETED'
                  AND gp.position = 'FORWARD'
                  %s
                GROUP BY p.id
                ORDER BY gpg DESC
                """, timeFilter);
        return executeQuery(sql);
    }

    /**
     * Goalie Stats: GAA, GPG, G, GP, +/-, SO
     */
    public List<Map<String, Object>> getGoalieStats(boolean allTime) {
        String timeFilter = allTime ? "" : "AND g.start_time >= datetime('now', '-7 days')";
        String sql = String.format("""
                SELECT p.display_name,
                       ROUND(CAST(SUM(CASE WHEN gp.team = 'BLUE' THEN
                           (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                       ELSE
                           (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                       END) AS REAL) / MAX(COUNT(DISTINCT gp.game_id), 1), 2) as gaa,
                       ROUND(CAST(SUM(gp.goals) AS REAL) / MAX(COUNT(DISTINCT gp.game_id), 1), 2) as gpg,
                       SUM(gp.goals) as goals,
                       COUNT(DISTINCT gp.game_id) as gp,
                       SUM(CASE WHEN g.status = 'COMPLETED' THEN
                           CASE WHEN gp.team = 'BLUE' THEN
                               (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                               - (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                           ELSE
                               (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                               - (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                           END
                       ELSE 0 END) as plus_minus,
                       SUM(CASE WHEN g.status = 'COMPLETED'
                           AND ((gp.team = 'BLUE' AND
                                 (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED') = 0)
                            OR (gp.team = 'RED' AND
                                 (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE') = 0))
                           THEN 1 ELSE 0 END) as shutouts
                FROM game_players gp
                JOIN games g ON gp.game_id = g.id
                JOIN players p ON gp.player_id = p.id
                WHERE g.status = 'COMPLETED'
                  AND gp.position = 'GOALIE'
                  %s
                GROUP BY p.id
                ORDER BY gaa ASC
                """, timeFilter);
        return executeQuery(sql);
    }

    private List<Map<String, Object>> executeQuery(String sql) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            var meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute stats query", e);
        }
        return results;
    }
}

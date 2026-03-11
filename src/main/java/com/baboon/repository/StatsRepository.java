package com.baboon.repository;

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
     * Rankings: Games, Wins, Win%, Goals — sorted by win%
     */
    public List<Map<String, Object>> getPlayerStats(boolean allTime) {
        String timeFilter = allTime ? "" : "AND g.start_time >= datetime('now', '-7 days')";
        String sql = String.format("""
                SELECT p.display_name,
                       COUNT(DISTINCT gp.game_id) as games,
                       SUM(CASE WHEN g.status = 'COMPLETED'
                           AND ((gp.team = 'BLUE' AND
                                 (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                                 > (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED'))
                            OR (gp.team = 'RED' AND
                                 (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                                 > (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')))
                           THEN 1 ELSE 0 END) as wins,
                       ROUND(100.0 * SUM(CASE WHEN g.status = 'COMPLETED'
                           AND ((gp.team = 'BLUE' AND
                                 (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                                 > (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED'))
                            OR (gp.team = 'RED' AND
                                 (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                                 > (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')))
                           THEN 1 ELSE 0 END) / MAX(COUNT(DISTINCT gp.game_id), 1)) as win_pct,
                       SUM(gp.goals) as goals
                FROM game_players gp
                JOIN games g ON gp.game_id = g.id
                JOIN players p ON gp.player_id = p.id
                WHERE g.status = 'COMPLETED'
                  %s
                GROUP BY p.id
                ORDER BY win_pct DESC, goals DESC
                """, timeFilter);
        return executeQuery(sql);
    }

    /**
     * Top Scorers (forwards): Goals, Goals per game — sorted by goals per game
     */
    public List<Map<String, Object>> getForwardStats(boolean allTime) {
        String timeFilter = allTime ? "" : "AND g.start_time >= datetime('now', '-7 days')";
        String sql = String.format("""
                SELECT p.display_name,
                       SUM(gp.goals) as goals,
                       ROUND(CAST(SUM(gp.goals) AS REAL) / MAX(COUNT(DISTINCT gp.game_id), 1), 1) as per_game
                FROM game_players gp
                JOIN games g ON gp.game_id = g.id
                JOIN players p ON gp.player_id = p.id
                WHERE g.status = 'COMPLETED'
                  AND gp.position = 'FORWARD'
                  %s
                GROUP BY p.id
                ORDER BY per_game DESC, goals DESC
                """, timeFilter);
        return executeQuery(sql);
    }

    /**
     * Goalies: Goals let in, Goals let in per game — sorted by per game ASC
     */
    public List<Map<String, Object>> getGoalieStats(boolean allTime) {
        String timeFilter = allTime ? "" : "AND g.start_time >= datetime('now', '-7 days')";
        String sql = String.format("""
                SELECT p.display_name,
                       SUM(CASE WHEN gp.team = 'BLUE' THEN
                           (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                       ELSE
                           (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                       END) as goals_let_in,
                       ROUND(CAST(SUM(CASE WHEN gp.team = 'BLUE' THEN
                           (SELECT SUM(gp3.goals) FROM game_players gp3 WHERE gp3.game_id = g.id AND gp3.team = 'RED')
                       ELSE
                           (SELECT SUM(gp2.goals) FROM game_players gp2 WHERE gp2.game_id = g.id AND gp2.team = 'BLUE')
                       END) AS REAL) / MAX(COUNT(DISTINCT gp.game_id), 1), 1) as per_game
                FROM game_players gp
                JOIN games g ON gp.game_id = g.id
                JOIN players p ON gp.player_id = p.id
                WHERE g.status = 'COMPLETED'
                  AND gp.position = 'GOALIE'
                  %s
                GROUP BY p.id
                ORDER BY per_game ASC
                """, timeFilter);
        return executeQuery(sql);
    }

    public void resetAll() {
        try (Connection conn = dataSource.getConnection();
             var s = conn.createStatement()) {
            s.executeUpdate("DELETE FROM game_players");
            s.executeUpdate("DELETE FROM game_sets");
            s.executeUpdate("DELETE FROM games");
            s.executeUpdate("DELETE FROM players");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset stats", e);
        }
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

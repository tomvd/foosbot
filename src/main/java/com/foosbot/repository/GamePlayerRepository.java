package com.foosbot.repository;

import com.foosbot.model.GamePlayer;
import com.foosbot.model.Position;
import com.foosbot.model.Team;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class GamePlayerRepository {

    private final DataSource dataSource;

    public GamePlayerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(long gameId, long playerId, Team team, Position position) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO game_players (game_id, player_id, team, position, goals) VALUES (?, ?, ?, ?, 0)")) {
            ps.setLong(1, gameId);
            ps.setLong(2, playerId);
            ps.setString(3, team.name());
            ps.setString(4, position.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create game player", e);
        }
    }

    public List<GamePlayer> findByGameId(long gameId) {
        List<GamePlayer> players = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT gp.*, p.display_name, p.slack_user_id " +
                     "FROM game_players gp JOIN players p ON gp.player_id = p.id " +
                     "WHERE gp.game_id = ?")) {
            ps.setLong(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    players.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find game players", e);
        }
        return players;
    }

    public void addGoal(long gamePlayerId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE game_players SET goals = goals + 1 WHERE id = ?")) {
            ps.setLong(1, gamePlayerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add goal", e);
        }
    }

    public GamePlayer findById(long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT gp.*, p.display_name, p.slack_user_id " +
                     "FROM game_players gp JOIN players p ON gp.player_id = p.id " +
                     "WHERE gp.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find game player", e);
        }
        throw new RuntimeException("GamePlayer not found: " + id);
    }

    private GamePlayer mapRow(ResultSet rs) throws SQLException {
        GamePlayer gp = new GamePlayer();
        gp.setId(rs.getLong("id"));
        gp.setGameId(rs.getLong("game_id"));
        gp.setPlayerId(rs.getLong("player_id"));
        gp.setTeam(Team.valueOf(rs.getString("team")));
        gp.setPosition(Position.valueOf(rs.getString("position")));
        gp.setGoals(rs.getInt("goals"));
        gp.setDisplayName(rs.getString("display_name"));
        gp.setSlackUserId(rs.getString("slack_user_id"));
        return gp;
    }
}

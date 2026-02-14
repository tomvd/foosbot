package com.foosbot.repository;

import com.foosbot.model.Player;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

@Singleton
public class PlayerRepository {

    private final DataSource dataSource;

    public PlayerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Player findOrCreate(String slackUserId, String displayName) {
        return findBySlackUserId(slackUserId).orElseGet(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO players (slack_user_id, display_name) VALUES (?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, slackUserId);
                ps.setString(2, displayName);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return new Player(keys.getLong(1), slackUserId, displayName);
                }
            } catch (SQLException e) {
                // Race condition: another thread may have inserted
                return findBySlackUserId(slackUserId)
                        .orElseThrow(() -> new RuntimeException("Failed to create player", e));
            }
        });
    }

    public Optional<Player> findBySlackUserId(String slackUserId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, slack_user_id, display_name FROM players WHERE slack_user_id = ?")) {
            ps.setString(1, slackUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find player", e);
        }
        return Optional.empty();
    }

    public void updateDisplayName(String slackUserId, String displayName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE players SET display_name = ? WHERE slack_user_id = ?")) {
            ps.setString(1, displayName);
            ps.setString(2, slackUserId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update player", e);
        }
    }

    private Player mapRow(ResultSet rs) throws SQLException {
        return new Player(
                rs.getLong("id"),
                rs.getString("slack_user_id"),
                rs.getString("display_name")
        );
    }
}

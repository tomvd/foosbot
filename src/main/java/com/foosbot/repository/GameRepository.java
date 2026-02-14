package com.foosbot.repository;

import com.foosbot.model.Game;
import com.foosbot.model.GameStatus;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Singleton
public class GameRepository {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataSource dataSource;

    public GameRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Game create(String channelId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO games (channel_id, status) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, channelId);
            ps.setString(2, GameStatus.IN_PROGRESS.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                Game game = new Game();
                game.setId(keys.getLong(1));
                game.setChannelId(channelId);
                game.setStatus(GameStatus.IN_PROGRESS);
                return game;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create game", e);
        }
    }

    public Optional<Game> findById(long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM games WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find game", e);
        }
        return Optional.empty();
    }

    public void updateStatus(long gameId, GameStatus status) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE games SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, gameId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update game status", e);
        }
    }

    public void setStartTime(long gameId, LocalDateTime startTime) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE games SET start_time = ? WHERE id = ?")) {
            ps.setString(1, startTime.format(FMT));
            ps.setLong(2, gameId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set start time", e);
        }
    }

    public void setEndTime(long gameId, LocalDateTime endTime) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE games SET end_time = ? WHERE id = ?")) {
            ps.setString(1, endTime.format(FMT));
            ps.setLong(2, gameId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set end time", e);
        }
    }

    private Game mapRow(ResultSet rs) throws SQLException {
        Game game = new Game();
        game.setId(rs.getLong("id"));
        game.setChannelId(rs.getString("channel_id"));
        game.setStatus(GameStatus.valueOf(rs.getString("status")));
        String startTime = rs.getString("start_time");
        if (startTime != null) game.setStartTime(LocalDateTime.parse(startTime, FMT));
        String endTime = rs.getString("end_time");
        if (endTime != null) game.setEndTime(LocalDateTime.parse(endTime, FMT));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) game.setCreatedAt(LocalDateTime.parse(createdAt, FMT));
        return game;
    }
}

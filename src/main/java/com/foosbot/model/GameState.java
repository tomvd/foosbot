package com.foosbot.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GameState {

    private final long gameId;
    private final String channelId;
    private String messageTs;
    private final Instant startTime;
    private int blueWins;
    private int redWins;
    private final List<GamePlayerState> players = new ArrayList<>();

    public GameState(long gameId, String channelId) {
        this.gameId = gameId;
        this.channelId = channelId;
        this.startTime = Instant.now();
    }

    public long getGameId() { return gameId; }
    public String getChannelId() { return channelId; }
    public String getMessageTs() { return messageTs; }
    public void setMessageTs(String messageTs) { this.messageTs = messageTs; }
    public Instant getStartTime() { return startTime; }
    public int getBlueWins() { return blueWins; }
    public void setBlueWins(int blueWins) { this.blueWins = blueWins; }
    public int getRedWins() { return redWins; }
    public void setRedWins(int redWins) { this.redWins = redWins; }
    public List<GamePlayerState> getPlayers() { return players; }

    public int getBlueScore() {
        return players.stream().filter(p -> p.getTeam() == Team.BLUE).mapToInt(GamePlayerState::getGoals).sum();
    }

    public int getRedScore() {
        return players.stream().filter(p -> p.getTeam() == Team.RED).mapToInt(GamePlayerState::getGoals).sum();
    }

    public boolean isGameWinnable() {
        int blue = getBlueScore();
        int red = getRedScore();
        int max = Math.max(blue, red);
        int diff = Math.abs(blue - red);
        return max >= 11 && diff >= 2;
    }

    public Team getLeadingTeam() {
        int blue = getBlueScore();
        int red = getRedScore();
        if (blue > red) return Team.BLUE;
        if (red > blue) return Team.RED;
        return null;
    }

    public List<GamePlayerState> getTeam(Team team) {
        return players.stream().filter(p -> p.getTeam() == team).toList();
    }

    public GamePlayerState getPlayerByGamePlayerId(long gamePlayerId) {
        return players.stream()
                .filter(p -> p.getGamePlayerId() == gamePlayerId)
                .findFirst().orElse(null);
    }

    public boolean isParticipant(String userId) {
        return players.stream().anyMatch(p -> p.getSlackUserId().equals(userId));
    }

    public static class GamePlayerState {
        private final long gamePlayerId;
        private final String slackUserId;
        private final String displayName;
        private final Team team;
        private final Position position;
        private int goals;

        public GamePlayerState(long gamePlayerId, String slackUserId, String displayName,
                               Team team, Position position) {
            this.gamePlayerId = gamePlayerId;
            this.slackUserId = slackUserId;
            this.displayName = displayName;
            this.team = team;
            this.position = position;
        }

        public long getGamePlayerId() { return gamePlayerId; }
        public String getSlackUserId() { return slackUserId; }
        public String getDisplayName() { return displayName; }
        public Team getTeam() { return team; }
        public Position getPosition() { return position; }
        public int getGoals() { return goals; }
        public void addGoal() { this.goals++; }
    }
}

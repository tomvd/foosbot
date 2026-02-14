package com.foosbot.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LobbyState {

    private final String channelId;
    private String messageTs;
    private final List<LobbyPlayer> players = new ArrayList<>();

    public LobbyState(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelId() { return channelId; }
    public String getMessageTs() { return messageTs; }
    public void setMessageTs(String messageTs) { this.messageTs = messageTs; }
    public List<LobbyPlayer> getPlayers() { return players; }

    public boolean hasPlayer(String userId) {
        return players.stream().anyMatch(p -> p.getUserId().equals(userId));
    }

    public LobbyPlayer getPlayer(String userId) {
        return players.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst().orElse(null);
    }

    public boolean isFull() {
        return players.size() >= 4;
    }

    public boolean allReady() {
        return players.size() == 4 && players.stream().allMatch(LobbyPlayer::isReady);
    }

    public List<LobbyPlayer> getTeam(Team team) {
        return players.stream().filter(p -> p.getTeam() == team).toList();
    }

    public void addPlayer(String userId, String displayName) {
        if (hasPlayer(userId) || isFull()) return;

        Team team;
        Position position;
        int blueCount = (int) players.stream().filter(p -> p.getTeam() == Team.BLUE).count();
        int redCount = (int) players.stream().filter(p -> p.getTeam() == Team.RED).count();

        if (blueCount <= redCount) {
            team = Team.BLUE;
            boolean hasGoalie = players.stream()
                    .anyMatch(p -> p.getTeam() == Team.BLUE && p.getPosition() == Position.GOALIE);
            position = hasGoalie ? Position.FORWARD : Position.GOALIE;
        } else {
            team = Team.RED;
            boolean hasGoalie = players.stream()
                    .anyMatch(p -> p.getTeam() == Team.RED && p.getPosition() == Position.GOALIE);
            position = hasGoalie ? Position.FORWARD : Position.GOALIE;
        }

        players.add(new LobbyPlayer(userId, displayName, team, position));
    }

    public void switchPositions(Team team) {
        List<LobbyPlayer> teamPlayers = players.stream()
                .filter(p -> p.getTeam() == team).toList();
        if (teamPlayers.size() == 2) {
            Position temp = teamPlayers.get(0).getPosition();
            teamPlayers.get(0).setPosition(teamPlayers.get(1).getPosition());
            teamPlayers.get(1).setPosition(temp);
        }
    }

    public void shuffleTeams() {
        if (players.size() < 2) return;
        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++) {
            LobbyPlayer p = players.get(i);
            p.setTeam(i < 2 ? Team.BLUE : Team.RED);
            p.setPosition((i % 2 == 0) ? Position.GOALIE : Position.FORWARD);
            p.setReady(false);
        }
    }

    public static class LobbyPlayer {
        private final String userId;
        private final String displayName;
        private Team team;
        private Position position;
        private boolean ready;

        public LobbyPlayer(String userId, String displayName, Team team, Position position) {
            this.userId = userId;
            this.displayName = displayName;
            this.team = team;
            this.position = position;
        }

        public String getUserId() { return userId; }
        public String getDisplayName() { return displayName; }
        public Team getTeam() { return team; }
        public void setTeam(Team team) { this.team = team; }
        public Position getPosition() { return position; }
        public void setPosition(Position position) { this.position = position; }
        public boolean isReady() { return ready; }
        public void setReady(boolean ready) { this.ready = ready; }
    }
}

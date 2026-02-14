package com.foosbot.model;

public class GamePlayer {
    private long id;
    private long gameId;
    private long playerId;
    private Team team;
    private Position position;
    private int goals;

    // Joined fields (not always populated)
    private String displayName;
    private String slackUserId;

    public GamePlayer() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getGameId() { return gameId; }
    public void setGameId(long gameId) { this.gameId = gameId; }
    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
    public int getGoals() { return goals; }
    public void setGoals(int goals) { this.goals = goals; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getSlackUserId() { return slackUserId; }
    public void setSlackUserId(String slackUserId) { this.slackUserId = slackUserId; }
}

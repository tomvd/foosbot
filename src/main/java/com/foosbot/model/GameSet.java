package com.foosbot.model;

public class GameSet {
    private long id;
    private long gameId;
    private int setNumber;
    private int blueScore;
    private int redScore;
    private Team winningTeam;

    public GameSet() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getGameId() { return gameId; }
    public void setGameId(long gameId) { this.gameId = gameId; }
    public int getSetNumber() { return setNumber; }
    public void setSetNumber(int setNumber) { this.setNumber = setNumber; }
    public int getBlueScore() { return blueScore; }
    public void setBlueScore(int blueScore) { this.blueScore = blueScore; }
    public int getRedScore() { return redScore; }
    public void setRedScore(int redScore) { this.redScore = redScore; }
    public Team getWinningTeam() { return winningTeam; }
    public void setWinningTeam(Team winningTeam) { this.winningTeam = winningTeam; }
}
